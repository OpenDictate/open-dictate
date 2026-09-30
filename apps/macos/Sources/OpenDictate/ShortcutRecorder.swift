import SwiftUI

struct ShortcutRecorder: NSViewRepresentable {
    let shortcut: KeyboardShortcut
    let title: String
    let prompt: String
    let enabled: Bool
    let onRecording: (Bool) -> Void
    let onSave: (KeyboardShortcut) -> Void

    func makeNSView(context: Context) -> ShortcutRecorderButton {
        let button = ShortcutRecorderButton()
        button.bezelStyle = .rounded
        button.target = button; button.action = #selector(ShortcutRecorderButton.toggleRecording)
        return button
    }
    func updateNSView(_ button: ShortcutRecorderButton, context: Context) {
        button.shortcutLabel = shortcut.label; button.prompt = prompt
        button.onRecording = onRecording; button.onSave = onSave
        button.isEnabled = enabled
        button.setAccessibilityLabel(title)
        button.setAccessibilityHelp(prompt)
        button.refreshTitle()
    }
    static func dismantleNSView(_ button: ShortcutRecorderButton, coordinator: ()) { button.finish() }
}

@MainActor
final class ShortcutRecorderButton: NSButton {
    var shortcutLabel = ""
    var prompt = ""
    var onRecording: ((Bool) -> Void)?
    var onSave: ((KeyboardShortcut) -> Void)?
    private(set) var recording = false
    private var monitor: Any?
    private var windowObserver: NSObjectProtocol?
    private var modifierCandidate: NSEvent.ModifierFlags = []
    override var acceptsFirstResponder: Bool { true }

    func refreshTitle() { title = recording ? prompt : shortcutLabel }

    @objc func toggleRecording() {
        if recording { finish(); return }
        guard window?.makeFirstResponder(self) == true else { return }
        modifierCandidate = []
        recording = true; onRecording?(true); refreshTitle()
        // Local and temporary: captures menu equivalents (e.g. Command+Q) only
        // while this control is focused. The rest of the app keeps native editing.
        monitor = NSEvent.addLocalMonitorForEvents(matching: [.keyDown, .flagsChanged]) { [weak self] event in
            let consumed = MainActor.assumeIsolated {
                guard let self, self.recording, self.window?.isKeyWindow == true else { return false }
                self.capture(event)
                return true
            }
            return consumed ? nil : event
        }
        windowObserver = NotificationCenter.default.addObserver(forName: NSWindow.didResignKeyNotification,
                                                               object: window, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.finish() }
        }
    }

    private func capture(_ event: NSEvent) {
        if event.type == .keyDown {
            guard !event.isARepeat else { return }
            if event.keyCode == 53 && event.modifierFlags.intersection(KeyboardShortcut.modifierMask).isEmpty {
                finish(); return
            }
            let shortcut = KeyboardShortcut.recorded(event)
            finish(); onSave?(shortcut)
        } else {
            let flags = event.modifierFlags.intersection(KeyboardShortcut.modifierMask)
            if !modifierCandidate.isEmpty && flags.isSubset(of: modifierCandidate) && flags != modifierCandidate {
                let shortcut = KeyboardShortcut(keyCode: nil, flags: modifierCandidate)
                finish(); onSave?(shortcut)
            } else { modifierCandidate = flags }
        }
    }

    func finish() {
        guard recording else { return }
        recording = false
        if let monitor { NSEvent.removeMonitor(monitor) }; monitor = nil
        if let windowObserver { NotificationCenter.default.removeObserver(windowObserver) }; windowObserver = nil
        modifierCandidate = []; refreshTitle(); onRecording?(false)
    }
    override func resignFirstResponder() -> Bool { finish(); return super.resignFirstResponder() }
    override func viewWillMove(toWindow newWindow: NSWindow?) {
        if newWindow == nil { finish() }
        super.viewWillMove(toWindow: newWindow)
    }
}
