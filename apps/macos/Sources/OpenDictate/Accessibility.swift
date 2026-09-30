import AppKit
import ApplicationServices
import OpenDictateCore

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

    private init(element: AXUIElement, application: NSRunningApplication, snapshot: EditableTextSnapshot, writable: Bool) {
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
        guard AXIsProcessTrusted(), NSWorkspace.shared.frontmostApplication?.processIdentifier == pid,
              let current = Self.focused(in: pid), CFEqual(current, element),
              Self.string(current, kAXSubroleAttribute) != kAXSecureTextFieldSubrole,
              let text = Self.value(current), let selection = Self.range(current) else { return false }
        return delivery.accepts(text: text, selection: selection)
    }

    /// Cumulative partials replace exactly the original selection; never append previous partials.
    func apply(_ transcript: String) -> Bool {
        guard writable, isCurrent else { return false }
        let composed = snapshot.compose(transcript)
        guard AXUIElementSetAttributeValue(element, kAXValueAttribute as CFString, composed as CFString) == .success else { return false }
        let cursor = snapshot.cursor(after: transcript)
        setSelection(cursor)
        didInsert = true
        // Do not continue overwriting an editor that silently rejected or reformatted our update.
        guard Self.value(element) == composed, Self.range(element) == cursor else { return false }
        delivery = TextDeliveryGuard(snapshot: EditableTextSnapshot(original: composed, selection: cursor))
        return true
    }

    func restore() {
        guard didInsert, isCurrent else { return }
        if AXUIElementSetAttributeValue(element, kAXValueAttribute as CFString, deliveryOriginal.original as CFString) == .success {
            setSelection(deliveryOriginal.selection)
        }
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

    private func setSelection(_ range: NSRange) {
        var range = CFRange(location: range.location, length: range.length)
        if let value = AXValueCreate(.cfRange, &range) {
            AXUIElementSetAttributeValue(element, kAXSelectedTextRangeAttribute as CFString, value)
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
