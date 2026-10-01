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
    var delayedWrites = false
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
        if delayedWrites { DispatchQueue.main.asyncAfter(deadline: .now() + 0.03) { self.range = range } }
        else { self.range = range }
        if losesFocusWhenSelecting { focused = false }
        return true
    }
    func setSelectedText(_ text: String, in element: AXUIElement) -> Bool {
        guard supportsSelectedText else { return false }
        selectedTextWrites += 1
        let replacement = (self.text as NSString).replacingCharacters(in: range, with: text)
        let cursor = NSRange(location: range.location + (text as NSString).length, length: 0)
        if delayedWrites {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.03) { self.text = replacement; self.range = cursor }
        } else { self.text = replacement; range = cursor }
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
    private func assertResult(_ actual: Bool, _ expected: Bool, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertEqual(actual, expected, file: file, line: line)
    }

    @MainActor func testDelayedEditorReadbackDoesNotRejectOrDuplicateTextAndCanRestore() async {
        let snapshot = EditableTextSnapshot(original: "before old after", selection: NSRange(location: 7, length: 3))
        let editor = Editor(snapshot); editor.delayedWrites = true
        let target = editor.target(snapshot); target.preserveOriginal()
        assertResult(await target.apply("new words"), true)
        assertResult(await target.apply("new words 🙂"), true)
        XCTAssertEqual(editor.text, snapshot.compose("new words 🙂"))
        XCTAssertEqual(editor.range, snapshot.cursor(after: "new words 🙂"))
        XCTAssertEqual(editor.selectedTextWrites, 2)
        await target.restore()
        XCTAssertEqual(editor.text, snapshot.original)
        XCTAssertEqual(editor.range, snapshot.selection)
    }

    @MainActor func testCancellationWaitsForAcknowledgedWriteBeforeRollback() async throws {
        let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 0, length: 3))
        let editor = Editor(snapshot); editor.delayedWrites = true
        let target = editor.target(snapshot); target.preserveOriginal()
        let task = Task { await target.apply("new") }
        try await Task.sleep(for: .milliseconds(10))
        task.cancel()
        _ = await task.value
        await target.restore()
        XCTAssertEqual(editor.text, "old")
        XCTAssertEqual(editor.range, snapshot.selection)
    }

    @MainActor func testCancellationDuringCumulativeSelectionRestoresOriginalTextAndCursor() async throws {
        let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 0, length: 3))
        let editor = Editor(snapshot); editor.delayedWrites = true
        let target = editor.target(snapshot); target.preserveOriginal()
        assertResult(await target.apply("first partial"), true)
        let task = Task { await target.apply("later partial") }
        try await Task.sleep(for: .milliseconds(10))
        task.cancel()
        _ = await task.value
        await target.restore()
        XCTAssertEqual(editor.text, "old")
        XCTAssertEqual(editor.range, snapshot.selection)
        XCTAssertEqual(editor.selectedTextWrites, 2) // First partial and rollback; no cancelled write.
    }

    @MainActor func testNativeTextViewSelectionReplacementAndRollback() async {
            let editor = NativeEditor()
            editor.view.string = "🙂 old after"
            editor.view.setSelectedRange(NSRange(location: 3, length: 3))
            let snapshot = EditableTextSnapshot(original: editor.view.string, selection: editor.view.selectedRange())
            let target = TextTarget(element: AXUIElementCreateApplication(getpid()), application: .current,
                                    snapshot: snapshot, writable: true, accessibility: editor)
            target.preserveOriginal()
            assertResult(await target.apply("Проверка"), true)
            assertResult(await target.apply("Проверка 🎤"), true)
            XCTAssertEqual(editor.view.string, "🙂 Проверка 🎤 after")
            XCTAssertEqual(editor.view.selectedRange(), snapshot.cursor(after: "Проверка 🎤"))
            await target.restore()
            XCTAssertEqual(editor.view.string, snapshot.original)
            XCTAssertEqual(editor.view.selectedRange(), snapshot.selection)
    }

    @MainActor func testSelectionInsertionLeavesCaretAfterCumulativeUTF16TextAndRestoresOriginal() async {
            let snapshot = EditableTextSnapshot(original: "🙂 old after", selection: NSRange(location: 3, length: 3))
            let editor = Editor(snapshot); editor.resetsCaretOnValueRead = true
            let target = editor.target(snapshot); target.preserveOriginal()
            assertResult(await target.apply("Новое"), true)
            assertResult(await target.apply("Новое 🎤"), true)
            XCTAssertEqual(editor.text, "🙂 Новое 🎤 after")
            XCTAssertEqual(editor.range, snapshot.cursor(after: "Новое 🎤"))
            XCTAssertEqual(editor.valueWrites, 0)
            await target.restore()
            XCTAssertEqual(editor.text, snapshot.original)
            XCTAssertEqual(editor.range, snapshot.selection)
    }

    @MainActor func testValueOnlyEditorFinishesTextUpdateBeforeSettingCaret() async {
            let snapshot = EditableTextSnapshot(original: "before old after", selection: NSRange(location: 7, length: 3))
            let editor = Editor(snapshot)
            editor.supportsSelectedText = false; editor.resetsCaretOnValueRead = true
            let target = editor.target(snapshot); target.preserveOriginal()
            assertResult(await target.apply("new words"), true)
            XCTAssertEqual(editor.text, "before new words after")
            XCTAssertEqual(editor.range, NSRange(location: 16, length: 0))
            await target.restore()
            XCTAssertEqual(editor.text, snapshot.original)
            XCTAssertEqual(editor.range, snapshot.selection)
    }

    @MainActor func testFocusChangeDuringSelectionPreventsTextWrite() async {
            let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 0, length: 3))
            let editor = Editor(snapshot); editor.losesFocusWhenSelecting = true
            assertResult(await editor.target(snapshot).apply("new"), false)
            XCTAssertEqual(editor.text, "old")
            XCTAssertEqual(editor.valueWrites, 0)
            XCTAssertEqual(editor.selectedTextWrites, 0)
    }

    @MainActor func testInsertionAndRollbackRejectUserEditsAndFocusChanges() async {
            let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 0, length: 3))
            let editor = Editor(snapshot); let target = editor.target(snapshot); target.preserveOriginal()
            assertResult(await target.apply("new"), true)
            editor.range = NSRange(location: 0, length: 0)
            assertResult(await target.apply("later"), false)
            await target.restore(); XCTAssertEqual(editor.text, "new")
            editor.range = NSRange(location: 3, length: 0); editor.focused = false
            assertResult(await target.apply("later"), false)
            await target.restore(); XCTAssertEqual(editor.text, "new")
            editor.focused = true; editor.text = "new!"
            assertResult(await target.apply("later"), false)
            await target.restore(); XCTAssertEqual(editor.text, "new!")
    }
}
