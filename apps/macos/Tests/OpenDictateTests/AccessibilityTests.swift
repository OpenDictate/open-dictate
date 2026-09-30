import AppKit
import ApplicationServices
import XCTest
import OpenDictateCore
@testable import OpenDictate

@MainActor
private final class Editor: TextAccessibility {
    var text: String
    var range: NSRange
    var focused = true
    var losesFocusWhenSelecting = false
    var supportsSelectedText = true
    var resetsCaretOnValueRead = false
    var pendingValue: String?
    var valueWrites = 0
    var selectedTextWrites = 0

    init(_ snapshot: EditableTextSnapshot) { text = snapshot.original; range = snapshot.selection }
    func isFocused(_ element: AXUIElement, pid: pid_t) -> Bool { focused }
    func value(_ element: AXUIElement) -> String? {
        // Some editors acknowledge AXValue before updating their text and resetting the caret.
        if let pendingValue {
            text = pendingValue; range = NSRange(location: 0, length: 0); self.pendingValue = nil
        }
        return text
    }
    func selection(_ element: AXUIElement) -> NSRange? { range }
    func setValue(_ text: String, in element: AXUIElement) -> Bool {
        valueWrites += 1
        if resetsCaretOnValueRead { pendingValue = text }
        else { self.text = text; range = NSRange(location: 0, length: 0) }
        return true
    }
    func setSelection(_ range: NSRange, in element: AXUIElement) -> Bool {
        self.range = range
        if losesFocusWhenSelecting { focused = false }
        return true
    }
    func setSelectedText(_ text: String, in element: AXUIElement) -> Bool {
        guard supportsSelectedText else { return false }
        selectedTextWrites += 1
        self.text = (self.text as NSString).replacingCharacters(in: range, with: text)
        range = NSRange(location: range.location + (text as NSString).length, length: 0)
        return true
    }
    func target(_ snapshot: EditableTextSnapshot) -> TextTarget {
        TextTarget(element: AXUIElementCreateApplication(getpid()), application: .current,
                   snapshot: snapshot, writable: true, accessibility: self)
    }
}

@MainActor
private final class NativeEditor: TextAccessibility {
    let view = NSTextView()
    func isFocused(_ element: AXUIElement, pid: pid_t) -> Bool { true }
    func value(_ element: AXUIElement) -> String? { view.string }
    func selection(_ element: AXUIElement) -> NSRange? { view.selectedRange() }
    func setValue(_ text: String, in element: AXUIElement) -> Bool { view.setAccessibilityValue(text); return true }
    func setSelection(_ range: NSRange, in element: AXUIElement) -> Bool {
        view.setAccessibilitySelectedTextRange(range); return true
    }
    func setSelectedText(_ text: String, in element: AXUIElement) -> Bool {
        view.setAccessibilitySelectedText(text); return true
    }
}

final class AccessibilityTests: XCTestCase {
    func testNativeTextViewSelectionReplacementAndRollback() async {
        await MainActor.run {
            let editor = NativeEditor()
            editor.view.string = "🙂 old after"
            editor.view.setSelectedRange(NSRange(location: 3, length: 3))
            let snapshot = EditableTextSnapshot(original: editor.view.string, selection: editor.view.selectedRange())
            let target = TextTarget(element: AXUIElementCreateApplication(getpid()), application: .current,
                                    snapshot: snapshot, writable: true, accessibility: editor)
            target.preserveOriginal()
            XCTAssertTrue(target.apply("Проверка"))
            XCTAssertTrue(target.apply("Проверка 🎤"))
            XCTAssertEqual(editor.view.string, "🙂 Проверка 🎤 after")
            XCTAssertEqual(editor.view.selectedRange(), snapshot.cursor(after: "Проверка 🎤"))
            target.restore()
            XCTAssertEqual(editor.view.string, snapshot.original)
            XCTAssertEqual(editor.view.selectedRange(), snapshot.selection)
        }
    }

    func testSelectionInsertionLeavesCaretAfterCumulativeUTF16TextAndRestoresOriginal() async {
        await MainActor.run {
            let snapshot = EditableTextSnapshot(original: "🙂 old after", selection: NSRange(location: 3, length: 3))
            let editor = Editor(snapshot); editor.resetsCaretOnValueRead = true
            let target = editor.target(snapshot); target.preserveOriginal()
            XCTAssertTrue(target.apply("Новое"))
            XCTAssertTrue(target.apply("Новое 🎤"))
            XCTAssertEqual(editor.text, "🙂 Новое 🎤 after")
            XCTAssertEqual(editor.range, snapshot.cursor(after: "Новое 🎤"))
            XCTAssertEqual(editor.valueWrites, 0)
            target.restore()
            XCTAssertEqual(editor.text, snapshot.original)
            XCTAssertEqual(editor.range, snapshot.selection)
        }
    }

    func testValueOnlyEditorFinishesTextUpdateBeforeSettingCaret() async {
        await MainActor.run {
            let snapshot = EditableTextSnapshot(original: "before old after", selection: NSRange(location: 7, length: 3))
            let editor = Editor(snapshot)
            editor.supportsSelectedText = false; editor.resetsCaretOnValueRead = true
            let target = editor.target(snapshot); target.preserveOriginal()
            XCTAssertTrue(target.apply("new words"))
            XCTAssertEqual(editor.text, "before new words after")
            XCTAssertEqual(editor.range, NSRange(location: 16, length: 0))
            target.restore()
            XCTAssertEqual(editor.text, snapshot.original)
            XCTAssertEqual(editor.range, snapshot.selection)
        }
    }

    func testFocusChangeDuringSelectionPreventsTextWrite() async {
        await MainActor.run {
            let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 0, length: 3))
            let editor = Editor(snapshot); editor.losesFocusWhenSelecting = true
            XCTAssertFalse(editor.target(snapshot).apply("new"))
            XCTAssertEqual(editor.text, "old")
            XCTAssertEqual(editor.valueWrites, 0)
            XCTAssertEqual(editor.selectedTextWrites, 0)
        }
    }

    func testInsertionAndRollbackRejectUserEditsAndFocusChanges() async {
        await MainActor.run {
            let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 0, length: 3))
            let editor = Editor(snapshot); let target = editor.target(snapshot); target.preserveOriginal()
            XCTAssertTrue(target.apply("new"))
            editor.range = NSRange(location: 0, length: 0)
            XCTAssertFalse(target.apply("later"))
            target.restore(); XCTAssertEqual(editor.text, "new")
            editor.range = NSRange(location: 3, length: 0); editor.focused = false
            XCTAssertFalse(target.apply("later"))
            target.restore(); XCTAssertEqual(editor.text, "new")
            editor.focused = true; editor.text = "new!"
            XCTAssertFalse(target.apply("later"))
            target.restore(); XCTAssertEqual(editor.text, "new!")
        }
    }
}
