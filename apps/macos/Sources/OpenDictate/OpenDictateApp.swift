import AppKit
import SwiftUI
import OpenDictateCore

@main
struct OpenDictateApp {
    @MainActor static func main() {
        let application = NSApplication.shared
        let delegate = AppDelegate()
        application.delegate = delegate
        application.setActivationPolicy(.accessory)
        application.run()
        withExtendedLifetime(delegate) {}
    }
}

@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate, NSMenuDelegate {
    private var model: AppModel!
    private var statusItem: NSStatusItem!
    private var settingsWindow: NSWindow?
    private var recordingPanel: NSPanel?
    #if DEBUG
    private var localChecks: LocalSmokeChecks?
    #endif

    func applicationDidFinishLaunching(_ notification: Notification) {
        #if DEBUG
        model = AppModel(localOnly: CommandLine.arguments.contains("--local-smoke-test"))
        #else
        model = AppModel()
        #endif
        #if DEBUG
        if CommandLine.arguments.contains("--local-smoke-test") {
            let checks = LocalSmokeChecks(model: model); localChecks = checks
            model.hotKeys.onAction = { action in
                switch action {
                case .dictate, .transform: checks.toggle()
                case .cancel: checks.cancel()
                }
            }
        }
        #endif
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        model.showSettings = { [weak self] in self?.openSettings() }
        model.onStateChange = { [weak self] in self?.updateState() }
        let menu = NSMenu(); menu.delegate = self
        statusItem.menu = menu
        updateState()
        if !model.hasKey || !model.microphoneAllowed || !model.accessibilityAllowed { openSettings() }
        #if DEBUG
        if localChecks != nil { openSettings() }
        #endif
    }

    func applicationShouldHandleReopen(_ sender: NSApplication, hasVisibleWindows flag: Bool) -> Bool {
        openSettings(); return true
    }
    func applicationWillTerminate(_ notification: Notification) { model.shutdown() }

    func menuNeedsUpdate(_ menu: NSMenu) {
        menu.removeAllItems()
        let p = model.preferences
        let status = NSMenuItem(title: model.isActive ? model.stateLabel : "OpenDictate", action: nil, keyEquivalent: "")
        menu.addItem(status)
        if !model.message.isEmpty {
            let item = NSMenuItem(title: String(model.message.prefix(110)), action: nil, keyEquivalent: "")
            menu.addItem(item)
        }
        menu.addItem(.separator())
        add(menu, model.isActive ? p.t("Finish dictation", "Завершить диктовку") : p.t("Start dictation", "Начать диктовку"),
            #selector(dictate), key: p.shortcutLabel, enabled: model.phase != .processing)
        add(menu, p.t("Edit text by voice", "Изменить текст голосом"), #selector(transform), key: p.editShortcutLabel, enabled: !model.isActive)
        if model.isActive { add(menu, p.t("Cancel", "Отменить"), #selector(cancel), key: "Esc") }
        menu.addItem(.separator())
        add(menu, p.t("Copy last transcript", "Скопировать последний текст"), #selector(copyLast), enabled: !model.lastTranscript.isEmpty)
        add(menu, p.t("Paste last transcript", "Вставить последний текст"), #selector(pasteLast), enabled: !model.isActive && !model.lastTranscript.isEmpty)
        add(menu, p.t("Add selection to dictionary", "Добавить выделение в словарь"), #selector(addSelection), enabled: !model.isActive)
        menu.addItem(.separator())
        let modes = NSMenu(title: p.t("Transcription", "Распознавание"))
        for mode in DictationMode.allCases {
            let item = NSMenuItem(title: mode == .live ? "Live" : "Accurate", action: #selector(setMode(_:)), keyEquivalent: "")
            item.target = self; item.representedObject = mode.rawValue
            item.state = p.mode == mode ? .on : .off; item.isEnabled = !model.isActive
            modes.addItem(item)
        }
        let modeItem = NSMenuItem(title: p.t("Transcription", "Распознавание") + ": " + (p.mode == .live ? "Live" : "Accurate"), action: nil, keyEquivalent: "")
        modeItem.submenu = modes; menu.addItem(modeItem)
        add(menu, p.t("Settings…", "Настройки…"), #selector(settings))
        add(menu, p.t("Quit OpenDictate", "Завершить OpenDictate"), #selector(quit))
        #if DEBUG
        if localChecks != nil {
            menu.addItem(.separator())
            add(menu, "Local check: record microphone (2 seconds)", #selector(checkMicrophone))
        }
        #endif
    }

    private func add(_ menu: NSMenu, _ title: String, _ action: Selector, key: String = "", enabled: Bool = true) {
        let item = NSMenuItem(title: title + (key.isEmpty ? "" : "    \(key)"), action: action, keyEquivalent: "")
        item.target = self; item.isEnabled = enabled; menu.addItem(item)
    }

    private func updateState() {
        let main = NSMenu()
        let applicationItem = NSMenuItem(); main.addItem(applicationItem)
        let applicationMenu = NSMenu(title: "OpenDictate")
        let settings = NSMenuItem(title: model.preferences.t("Settings…", "Настройки…"), action: #selector(settings), keyEquivalent: ",")
        settings.target = self; applicationMenu.addItem(settings)
        #if DEBUG
        if localChecks != nil {
            let microphone = NSMenuItem(title: "Local check: record microphone (2 seconds)", action: #selector(checkMicrophone), keyEquivalent: "")
            microphone.target = self; applicationMenu.addItem(microphone)
            let insertion = NSMenuItem(title: "Local check: insert in 3 seconds", action: #selector(checkInsertion), keyEquivalent: "")
            insertion.target = self; applicationMenu.addItem(insertion)
            let cancellation = NSMenuItem(title: "Local check: cancel in 3 seconds", action: #selector(checkCancellation), keyEquivalent: "")
            cancellation.target = self; applicationMenu.addItem(cancellation)
            let sequence = NSMenuItem(title: "Local check: insertion and rollback in 3 seconds", action: #selector(checkSequence), keyEquivalent: "")
            sequence.target = self; applicationMenu.addItem(sequence)
        }
        #endif
        applicationMenu.addItem(.separator())
        let quit = NSMenuItem(title: model.preferences.t("Quit OpenDictate", "Завершить OpenDictate"), action: #selector(quit), keyEquivalent: "q")
        quit.target = self; applicationMenu.addItem(quit)
        applicationItem.submenu = applicationMenu
        let editItem = NSMenuItem(), editMenu = NSMenu(title: model.preferences.t("Edit", "Правка"))
        main.addItem(editItem); editItem.submenu = editMenu
        editMenu.addItem(NSMenuItem(title: model.preferences.t("Undo", "Отменить"), action: NSSelectorFromString("undo:"), keyEquivalent: "z"))
        let redo = NSMenuItem(title: model.preferences.t("Redo", "Повторить"), action: NSSelectorFromString("redo:"), keyEquivalent: "z")
        redo.keyEquivalentModifierMask = [.command, .shift]; editMenu.addItem(redo)
        editMenu.addItem(.separator())
        editMenu.addItem(NSMenuItem(title: model.preferences.t("Cut", "Вырезать"), action: #selector(NSText.cut(_:)), keyEquivalent: "x"))
        editMenu.addItem(NSMenuItem(title: model.preferences.t("Copy", "Скопировать"), action: #selector(NSText.copy(_:)), keyEquivalent: "c"))
        editMenu.addItem(NSMenuItem(title: model.preferences.t("Paste", "Вставить"), action: #selector(NSText.paste(_:)), keyEquivalent: "v"))
        editMenu.addItem(NSMenuItem(title: model.preferences.t("Select All", "Выбрать всё"), action: #selector(NSText.selectAll(_:)), keyEquivalent: "a"))
        NSApp.mainMenu = main
        statusItem.button?.image = NSImage(systemSymbolName: model.isActive ? "waveform" : "mic", accessibilityDescription: "OpenDictate")
        statusItem.button?.title = model.phase == .processing ? " …" : ""
        statusItem.button?.toolTip = "OpenDictate · \(model.stateLabel) · \(model.preferences.shortcutLabel)"
        if model.isActive && model.preferences.showStatus {
            if recordingPanel == nil {
                let panel = NSPanel(contentRect: NSRect(x: 0, y: 0, width: 316, height: 94),
                                    styleMask: [.borderless, .nonactivatingPanel], backing: .buffered, defer: false)
                panel.level = .statusBar; panel.isOpaque = false; panel.backgroundColor = .clear
                panel.hasShadow = true; panel.ignoresMouseEvents = true
                panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
                panel.contentView = NSHostingView(rootView: RecordingStatusView(model: model))
                recordingPanel = panel
            }
            let screen = NSScreen.screens.first { NSMouseInRect(NSEvent.mouseLocation, $0.frame, false) } ?? NSScreen.main
            if let screen {
                recordingPanel?.setFrameOrigin(NSPoint(x: screen.visibleFrame.midX - 158, y: screen.visibleFrame.minY + 28))
            }
            recordingPanel?.orderFrontRegardless()
        } else { recordingPanel?.orderOut(nil) }
    }

    private func openSettings() {
        model.refreshPermissions()
        if settingsWindow == nil {
            #if DEBUG
            let compact = localChecks != nil && CommandLine.arguments.contains("--compact-ui-check")
            #else
            let compact = false
            #endif
            let window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: compact ? 740 : 790, height: compact ? 590 : 650),
                                  styleMask: [.titled, .closable, .miniaturizable, .resizable], backing: .buffered, defer: false)
            window.title = "OpenDictate"; window.isReleasedWhenClosed = false
            window.contentView = NSHostingView(rootView: SettingsView(model: model, preferences: model.preferences, history: model.history))
            window.minSize = NSSize(width: 740, height: 620)
            window.center(); settingsWindow = window
        }
        NSApp.activate(ignoringOtherApps: true)
        settingsWindow?.makeKeyAndOrderFront(nil)
    }

    // Menu actions run after the menu releases focus, before capturing the frontmost field.
    private func afterMenu(_ action: @escaping @MainActor @Sendable () -> Void) {
        Task { try? await Task.sleep(nanoseconds: 120_000_000); action() }
    }
    @objc private func dictate() { afterMenu { self.model.toggle(transform: false) } }
    @objc private func transform() { afterMenu { self.model.toggle(transform: true) } }
    @objc private func cancel() { model.cancel() }
    @objc private func copyLast() { model.copyLast() }
    @objc private func pasteLast() { afterMenu { self.model.pasteLast() } }
    @objc private func addSelection() { afterMenu { self.model.addSelectionToDictionary() } }
    @objc private func setMode(_ sender: NSMenuItem) {
        if let raw = sender.representedObject as? String, let mode = DictationMode(rawValue: raw) { model.preferences.mode = mode }
    }
    @objc private func settings() { openSettings() }
    @objc private func quit() { NSApp.terminate(nil) }
    #if DEBUG
    @objc private func checkMicrophone() { localChecks?.record() }
    @objc private func checkInsertion() {
        Task { try? await Task.sleep(nanoseconds: 3_000_000_000); localChecks?.toggle() }
    }
    @objc private func checkCancellation() {
        Task { try? await Task.sleep(nanoseconds: 3_000_000_000); localChecks?.cancel() }
    }
    @objc private func checkSequence() {
        Task { try? await Task.sleep(nanoseconds: 3_000_000_000); await localChecks?.verifyInsertionAndRollback() }
    }
    #endif
}
