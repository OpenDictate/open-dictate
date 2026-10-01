import AppKit
import ApplicationServices
import OpenDictateCore

@MainActor
protocol TextAccessibility {
    func isFocused(_ element: AXUIElement, pid: pid_t) -> Bool
    func value(_ element: AXUIElement) -> String?
    func selection(_ element: AXUIElement) -> NSRange?
    func setSelection(_ range: NSRange, in element: AXUIElement) -> Bool
}

@MainActor
final class TextTarget {
    let element: AXUIElement
    let pid: pid_t
    let applicationName: String
    private let delivery: TextDeliveryGuard
    private let capturedSnapshot: EditableTextSnapshot
    private let accessibility: any TextAccessibility
    private let clipboard: any TextPasting

    init(element: AXUIElement, application: NSRunningApplication, snapshot: EditableTextSnapshot, transform: Bool = false,
         accessibility: (any TextAccessibility)? = nil, clipboard: (any TextPasting)? = nil) {
        self.accessibility = accessibility ?? SystemAccessibility()
        self.clipboard = clipboard ?? ClipboardPaste()
        self.element = element; pid = application.processIdentifier
        applicationName = application.localizedName ?? "App"
        capturedSnapshot = snapshot
        replacementSnapshot = transform ? snapshot.transformationTarget : nil
        delivery = TextDeliveryGuard(snapshot: snapshot)
    }

    static func capture(exclusions: [String], transform: Bool = false) throws -> TextTarget {
        guard AXIsProcessTrusted() else { throw DictationError.accessibility }
        guard let app = NSWorkspace.shared.frontmostApplication,
              app.processIdentifier != ProcessInfo.processInfo.processIdentifier else { throw DictationError.noField }
        guard !exclusions.contains(app.bundleIdentifier ?? "") else { throw DictationError.excluded }
        guard let element = focused(in: app.processIdentifier) else { throw DictationError.noField }
        let role = string(element, kAXRoleAttribute)
        let subrole = string(element, kAXSubroleAttribute)
        guard subrole != kAXSecureTextFieldSubrole else { throw DictationError.secureField }
        guard [kAXTextFieldRole, kAXTextAreaRole, kAXComboBoxRole].contains(role) || range(element) != nil else {
            throw DictationError.noField
        }
        guard let text = value(element), let selection = range(element),
              selection.location >= 0, selection.length >= 0,
              selection.location <= (text as NSString).length,
              selection.length <= (text as NSString).length - selection.location else { throw DictationError.noField }
        let captured = EditableTextSnapshot(original: text, selection: selection)
        let snapshot = transform ? captured.transformationTarget : captured
        if transform && snapshot.selectedText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            throw DictationError.noField
        }
        // Whole-field voice edits retain the captured caret as their delivery guard.
        return TextTarget(element: element, application: app, snapshot: captured, transform: transform)
    }

    private let replacementSnapshot: EditableTextSnapshot?
    var snapshot: EditableTextSnapshot { replacementSnapshot ?? capturedSnapshot }

    var isCurrent: Bool {
        guard accessibility.isFocused(element, pid: pid),
              let text = accessibility.value(element), let selection = accessibility.selection(element) else { return false }
        return delivery.accepts(text: text, selection: selection)
    }

    private(set) var isUpdating = false

    /// All editors receive one final paste, after validating the original target.
    func paste(_ transcript: String) async -> Bool {
        guard !isUpdating, isCurrent, !Task.isCancelled else { return false }
        isUpdating = true
        defer { isUpdating = false }
        let selection = snapshot.selection
        let originalSelection = delivery.expectedSelection
        var posted = false
        defer {
            clipboard.restore()
            // A failed whole-field voice edit must not leave our temporary selection behind.
            if !posted, selection != originalSelection,
               matches(text: delivery.expectedText, selection: selection) {
                _ = accessibility.setSelection(originalSelection, in: element)
            }
        }
        if selection != originalSelection {
            guard accessibility.setSelection(selection, in: element),
                  await settle(text: delivery.expectedText, selection: selection,
                               previousSelection: originalSelection, finishAcknowledgedWrite: true) else { return false }
        }
        guard !Task.isCancelled, matches(text: delivery.expectedText, selection: selection),
              clipboard.stage(transcript),
              matches(text: delivery.expectedText, selection: selection), !Task.isCancelled,
              clipboard.send(to: pid) else { return false }
        posted = true
        // Posting a key event is not proof of insertion. Never repeat an unacknowledged paste.
        return await settle(text: snapshot.compose(transcript), selection: snapshot.cursor(after: transcript),
                            previousText: delivery.expectedText, previousSelection: selection,
                            finishAcknowledgedWrite: true)
    }

    private func matches(text: String, selection: NSRange) -> Bool {
        accessibility.isFocused(element, pid: pid)
            && accessibility.value(element) == text && accessibility.selection(element) == selection
    }

    private func settle(text: String, selection: NSRange, previousText: String? = nil,
                        previousSelection: NSRange, finishAcknowledgedWrite: Bool = false) async -> Bool {
        let deadline = ContinuousClock.now.advanced(by: .milliseconds(800))
        while finishAcknowledgedWrite || !Task.isCancelled {
            guard accessibility.isFocused(element, pid: pid) else { return false }
            let actual = accessibility.value(element)
            let range = accessibility.selection(element)
            if actual == text && range == selection { return true }
            guard actual == text || actual == (previousText ?? text),
                  range == selection || range == previousSelection,
                  ContinuousClock.now < deadline else { return false }
            // Finish acknowledged selection/paste readback even when cancelled; never repeat the write.
            await withCheckedContinuation { continuation in
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.01) { continuation.resume() }
            }
        }
        return false
    }

    struct SystemAccessibility: TextAccessibility {
        func isFocused(_ element: AXUIElement, pid: pid_t) -> Bool {
            AXIsProcessTrusted() && NSWorkspace.shared.frontmostApplication?.processIdentifier == pid
                && TextTarget.focused(in: pid).map { CFEqual($0, element) } == true
                && TextTarget.string(element, kAXSubroleAttribute) != kAXSecureTextFieldSubrole
        }
        func value(_ element: AXUIElement) -> String? { TextTarget.value(element) }
        func selection(_ element: AXUIElement) -> NSRange? { TextTarget.range(element) }
        func setSelection(_ range: NSRange, in element: AXUIElement) -> Bool {
            var range = CFRange(location: range.location, length: range.length)
            guard let value = AXValueCreate(.cfRange, &range) else { return false }
            return AXUIElementSetAttributeValue(element, kAXSelectedTextRangeAttribute as CFString, value) == .success
        }
    }

    private static func focused(in pid: pid_t) -> AXUIElement? {
        let application = AXUIElementCreateApplication(pid)
        AXUIElementSetMessagingTimeout(application, 0.15)
        var result: CFTypeRef?
        guard AXUIElementCopyAttributeValue(application, kAXFocusedUIElementAttribute as CFString, &result) == .success,
              let result, CFGetTypeID(result) == AXUIElementGetTypeID() else { return nil }
        let element = unsafeBitCast(result, to: AXUIElement.self)
        AXUIElementSetMessagingTimeout(element, 0.15)
        return element
    }
    private static func string(_ element: AXUIElement, _ attribute: String) -> String {
        var result: CFTypeRef?
        AXUIElementCopyAttributeValue(element, attribute as CFString, &result)
        return result as? String ?? ""
    }
    private static func value(_ element: AXUIElement) -> String? {
        var result: CFTypeRef?
        guard AXUIElementCopyAttributeValue(element, kAXValueAttribute as CFString, &result) == .success else { return nil }
        return result as? String
    }
    private static func range(_ element: AXUIElement) -> NSRange? {
        var result: CFTypeRef?
        guard AXUIElementCopyAttributeValue(element, kAXSelectedTextRangeAttribute as CFString, &result) == .success,
              let result, CFGetTypeID(result) == AXValueGetTypeID() else { return nil }
        let value = unsafeBitCast(result, to: AXValue.self)
        var range = CFRange()
        guard AXValueGetValue(value, .cfRange, &range) else { return nil }
        return NSRange(location: range.location, length: range.length)
    }
}

@MainActor
protocol TextPasting {
    func stage(_ text: String) -> Bool
    func send(to pid: pid_t) -> Bool
    func restore()
}

/// Owns one temporary clipboard replacement; preserves every previous item/type.
@MainActor
final class ClipboardPaste: TextPasting {
    private let pasteboard: NSPasteboard
    private let post: (pid_t) -> Bool
    private var backup: [NSPasteboardItem] = []
    private var changeCount: Int?

    init(pasteboard: NSPasteboard = .general, post: ((pid_t) -> Bool)? = nil) {
        self.pasteboard = pasteboard
        self.post = post ?? Self.postPaste
    }

    func stage(_ text: String) -> Bool {
        guard changeCount == nil else { return false }
        backup = (pasteboard.pasteboardItems ?? []).map { item in
            let copy = NSPasteboardItem()
            for type in item.types {
                if let data = item.data(forType: type) { copy.setData(data, forType: type) }
            }
            return copy
        }
        let item = NSPasteboardItem()
        item.setString(text, forType: .string)
        item.setData(Data(), forType: NSPasteboard.PasteboardType("org.nspasteboard.ConcealedType"))
        pasteboard.clearContents()
        let written = pasteboard.writeObjects([item])
        changeCount = pasteboard.changeCount
        return written
    }

    func send(to pid: pid_t) -> Bool {
        guard let changeCount, pasteboard.changeCount == changeCount else { return false }
        return post(pid)
    }

    func restore() {
        defer { backup = []; changeCount = nil }
        guard let changeCount, pasteboard.changeCount == changeCount else { return }
        pasteboard.clearContents()
        if !backup.isEmpty { pasteboard.writeObjects(backup) }
    }

    private static func postPaste(to pid: pid_t) -> Bool {
        guard let source = CGEventSource(stateID: .combinedSessionState),
              let down = CGEvent(keyboardEventSource: source, virtualKey: 9, keyDown: true),
              let up = CGEvent(keyboardEventSource: source, virtualKey: 9, keyDown: false) else { return false }
        down.flags = .maskCommand; up.flags = .maskCommand
        down.postToPid(pid); up.postToPid(pid)
        return true
    }
}
