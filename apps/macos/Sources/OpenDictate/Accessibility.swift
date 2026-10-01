import AppKit
import ApplicationServices
import OpenDictateCore

@MainActor
protocol TextAccessibility {
    func isFocused(_ element: AXUIElement, pid: pid_t) -> Bool
    func value(_ element: AXUIElement) -> String?
    func selection(_ element: AXUIElement) -> NSRange?
    func setValue(_ text: String, in element: AXUIElement) -> Bool
    func setSelection(_ range: NSRange, in element: AXUIElement) -> Bool
    func setSelectedText(_ text: String, in element: AXUIElement) -> Bool
}

@MainActor
final class TextTarget {
    let element: AXUIElement
    let pid: pid_t
    let applicationName: String
    let bundleID: String
    var delivery: TextDeliveryGuard
    private(set) var didInsert = false
    let writable: Bool
    private let capturedSnapshot: EditableTextSnapshot
    private let accessibility: any TextAccessibility

    init(element: AXUIElement, application: NSRunningApplication, snapshot: EditableTextSnapshot, writable: Bool,
         accessibility: (any TextAccessibility)? = nil) {
        self.accessibility = accessibility ?? SystemAccessibility()
        self.element = element; pid = application.processIdentifier
        applicationName = application.localizedName ?? "App"
        bundleID = application.bundleIdentifier ?? ""
        capturedSnapshot = snapshot
        delivery = TextDeliveryGuard(snapshot: snapshot); self.writable = writable
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
        var settable = DarwinBoolean(false)
        AXUIElementIsAttributeSettable(element, kAXValueAttribute as CFString, &settable)
        let target = TextTarget(element: element, application: app, snapshot: captured, writable: settable.boolValue)
        // Transformation may replace the whole field, but the original caret remains the delivery guard.
        target.delivery = TextDeliveryGuard(snapshot: captured)
        if transform { target.replacementSnapshot = snapshot }
        return target
    }

    private var replacementSnapshot: EditableTextSnapshot?
    var snapshot: EditableTextSnapshot { replacementSnapshot ?? capturedSnapshot }

    var isCurrent: Bool {
        guard accessibility.isFocused(element, pid: pid),
              let text = accessibility.value(element), let selection = accessibility.selection(element) else { return false }
        return delivery.accepts(text: text, selection: selection)
    }

    private var insertedRange: NSRange?
    private(set) var isUpdating = false

    /// Cumulative partials replace exactly the original selection; never append previous partials.
    func apply(_ transcript: String) async -> Bool {
        guard !isUpdating, writable, isCurrent, !Task.isCancelled else { return false }
        isUpdating = true
        defer { isUpdating = false }
        let cursor = snapshot.cursor(after: transcript)
        guard await replace(transcript, range: insertedRange ?? snapshot.selection,
                      composed: snapshot.compose(transcript), selection: cursor) else { return false }
        insertedRange = NSRange(location: snapshot.selection.location, length: (transcript as NSString).length)
        return true
    }

    func restore() async {
        guard !isUpdating, didInsert, isCurrent, let insertedRange else { return }
        isUpdating = true
        defer { isUpdating = false }
        if await replace(snapshot.selectedText, range: insertedRange,
                   composed: deliveryOriginal.original, selection: deliveryOriginal.selection) {
            didInsert = false; self.insertedRange = nil
        }
    }

    private func replace(_ text: String, range: NSRange, composed: String, selection: NSRange) async -> Bool {
        let original = delivery.expectedText
        let originalSelection = delivery.expectedSelection
        guard setSelection(range), await settle(text: original, selection: range, previousText: original,
                                                previousSelection: originalSelection, finishAcknowledgedWrite: true) else { return false }
        if Task.isCancelled {
            if accessibility.isFocused(element, pid: pid), accessibility.value(element) == original,
               accessibility.selection(element) == range, setSelection(originalSelection) {
                _ = await settle(text: original, selection: originalSelection, previousText: original,
                                 previousSelection: range, finishAcknowledgedWrite: true)
            }
            return false
        }
        if !accessibility.setSelectedText(text, in: element) {
            guard accessibility.isFocused(element, pid: pid),
                  accessibility.value(element) == original, accessibility.selection(element) == range,
                  accessibility.setValue(composed, in: element) else { return false }
        }
        didInsert = true // An acknowledged write may complete on the editor's next run-loop turn.
        guard await settle(text: composed, previousText: original, finishAcknowledgedWrite: true),
              let previousSelection = accessibility.selection(element),
              setSelection(selection),
              await settle(text: composed, selection: selection, previousText: composed,
                           previousSelection: previousSelection, finishAcknowledgedWrite: true) else { return false }
        delivery = TextDeliveryGuard(snapshot: EditableTextSnapshot(original: composed, selection: selection))
        return true
    }

    /// AX setters can acknowledge before editor text/caret updates are readable.
    /// Retry only the readback, never the write; reject focus changes or unrelated edits.
    private func settle(text: String, selection: NSRange? = nil, previousText: String,
                        previousSelection: NSRange? = nil, finishAcknowledgedWrite: Bool = false) async -> Bool {
        let deadline = ContinuousClock.now.advanced(by: .milliseconds(300))
        while finishAcknowledgedWrite || !Task.isCancelled {
            guard accessibility.isFocused(element, pid: pid) else { return false }
            let actual = accessibility.value(element)
            guard actual == text || actual == previousText else { return false }
            let range = accessibility.selection(element)
            if actual == text && (selection == nil || range == selection) { return true }
            if let selection, let range, range != selection && range != previousSelection { return false }
            guard ContinuousClock.now < deadline else { return false }
            if finishAcknowledgedWrite {
                await withCheckedContinuation { continuation in
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.01) { continuation.resume() }
                }
            } else {
                do { try await Task.sleep(for: .milliseconds(10)) } catch { return false }
            }
        }
        return false
    }

    private var deliveryOriginal: EditableTextSnapshot { originalSnapshot ?? snapshot }
    private var originalSnapshot: EditableTextSnapshot?
    func preserveOriginal() { originalSnapshot = delivery.snapshot }

    /// Used only for editors that don't support AXValue writes. Clipboard is restored if untouched.
    func paste(_ transcript: String) -> Bool {
        guard !didInsert, isCurrent else { return false }
        if replacementSnapshot != nil {
            setSelection(snapshot.selection)
            guard Self.range(element) == snapshot.selection else { return false }
        }
        let clipboard = NSPasteboard.general
        let backup = (clipboard.pasteboardItems ?? []).map { item -> NSPasteboardItem in
            let copy = NSPasteboardItem()
            for type in item.types { if let data = item.data(forType: type) { copy.setData(data, forType: type) } }
            return copy
        }
        clipboard.clearContents(); clipboard.setString(transcript, forType: .string)
        let changeCount = clipboard.changeCount
        guard NSWorkspace.shared.frontmostApplication?.processIdentifier == pid,
              let focused = Self.focused(in: pid), CFEqual(focused, element),
              Self.value(element) == delivery.expectedText,
              Self.range(element) == (replacementSnapshot == nil ? delivery.expectedSelection : snapshot.selection),
              let source = CGEventSource(stateID: .combinedSessionState),
              let down = CGEvent(keyboardEventSource: source, virtualKey: 9, keyDown: true),
              let up = CGEvent(keyboardEventSource: source, virtualKey: 9, keyDown: false) else {
            restoreClipboard(backup, changeCount: changeCount); return false
        }
        down.flags = .maskCommand; up.flags = .maskCommand
        down.postToPid(pid); up.postToPid(pid)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
            self.restoreClipboard(backup, changeCount: changeCount)
        }
        return true
    }

    private func restoreClipboard(_ backup: [NSPasteboardItem], changeCount: Int) {
        let clipboard = NSPasteboard.general
        guard clipboard.changeCount == changeCount else { return }
        clipboard.clearContents()
        if !backup.isEmpty { clipboard.writeObjects(backup) }
    }

    @discardableResult private func setSelection(_ range: NSRange) -> Bool {
        accessibility.setSelection(range, in: element)
    }

    struct SystemAccessibility: TextAccessibility {
        func isFocused(_ element: AXUIElement, pid: pid_t) -> Bool {
            AXIsProcessTrusted() && NSWorkspace.shared.frontmostApplication?.processIdentifier == pid
                && TextTarget.focused(in: pid).map { CFEqual($0, element) } == true
                && TextTarget.string(element, kAXSubroleAttribute) != kAXSecureTextFieldSubrole
        }
        func value(_ element: AXUIElement) -> String? { TextTarget.value(element) }
        func selection(_ element: AXUIElement) -> NSRange? { TextTarget.range(element) }
        func setValue(_ text: String, in element: AXUIElement) -> Bool {
            AXUIElementSetAttributeValue(element, kAXValueAttribute as CFString, text as CFString) == .success
        }
        func setSelection(_ range: NSRange, in element: AXUIElement) -> Bool {
            var range = CFRange(location: range.location, length: range.length)
            guard let value = AXValueCreate(.cfRange, &range) else { return false }
            return AXUIElementSetAttributeValue(element, kAXSelectedTextRangeAttribute as CFString, value) == .success
        }
        func setSelectedText(_ text: String, in element: AXUIElement) -> Bool {
            AXUIElementSetAttributeValue(element, kAXSelectedTextAttribute as CFString, text as CFString) == .success
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
