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
    var delayedSelection = false
    var losesFocusWhenSelecting = false
    var selectionWrites = 0

    init(_ snapshot: EditableTextSnapshot) { text = snapshot.original; range = snapshot.selection }
    func isFocused(_ element: AXUIElement, pid: pid_t) -> Bool { focused }
    func value(_ element: AXUIElement) -> String? { text }
    func selection(_ element: AXUIElement) -> NSRange? { range }
    func setSelection(_ range: NSRange, in element: AXUIElement) -> Bool {
        selectionWrites += 1
        if delayedSelection { DispatchQueue.main.asyncAfter(deadline: .now() + 0.03) { self.range = range } }
        else { self.range = range }
        if losesFocusWhenSelecting { focused = false }
        return true
    }
    func insert(_ text: String) {
        self.text = (self.text as NSString).replacingCharacters(in: range, with: text)
        range = NSRange(location: range.location + (text as NSString).length, length: 0)
    }
    func target(_ snapshot: EditableTextSnapshot, clipboard: any TextPasting, transform: Bool = false) -> TextTarget {
        TextTarget(element: AXUIElementCreateApplication(getpid()), application: .current,
                   snapshot: snapshot, transform: transform, accessibility: self, clipboard: clipboard)
    }
}

@MainActor
private final class Paste: TextPasting {
    let editor: Editor
    var text = ""
    var sends = 0
    var stages = 0
    var restores = 0
    var delayed = false
    var ignored = false
    var beforeSend: (() -> Void)?
    init(_ editor: Editor) { self.editor = editor }
    func stage(_ text: String) -> Bool { stages += 1; self.text = text; beforeSend?(); return true }
    func send(to pid: pid_t) -> Bool {
        sends += 1
        if ignored { return true }
        if delayed { DispatchQueue.main.asyncAfter(deadline: .now() + 0.03) { self.editor.insert(self.text) } }
        else { editor.insert(text) }
        return true
    }
    func restore() { restores += 1 }
}

final class AccessibilityTests: XCTestCase {
    @MainActor func testEveryEditorUsesOneFinalClipboardPasteWithoutAXTextWrites() async {
        let snapshot = EditableTextSnapshot(original: "🙂 old after", selection: NSRange(location: 3, length: 3))
        let editor = Editor(snapshot), paste = Paste(editor)
        let target = editor.target(snapshot, clipboard: paste)
        let result = await target.paste("Новое 🎤")
        XCTAssertTrue(result)
        XCTAssertEqual(editor.text, snapshot.compose("Новое 🎤"))
        XCTAssertEqual(editor.range, snapshot.cursor(after: "Новое 🎤"))
        XCTAssertEqual(editor.selectionWrites, 0)
        XCTAssertEqual(paste.sends, 1)
        XCTAssertEqual(paste.restores, 1)
    }

    @MainActor func testDelayedPasteReadbackIsVerifiedWithoutRepeatingPaste() async {
        let snapshot = EditableTextSnapshot(original: "before old after", selection: NSRange(location: 7, length: 3))
        let editor = Editor(snapshot), paste = Paste(editor); paste.delayed = true
        let result = await editor.target(snapshot, clipboard: paste).paste("new words")
        XCTAssertTrue(result)
        XCTAssertEqual(editor.text, snapshot.compose("new words"))
        XCTAssertEqual(paste.sends, 1)
        XCTAssertEqual(paste.restores, 1)
    }

    @MainActor func testIgnoredPasteIsNotReportedAsDeliveredOrRetried() async {
        let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 0, length: 3))
        let editor = Editor(snapshot), paste = Paste(editor); paste.ignored = true
        let result = await editor.target(snapshot, clipboard: paste).paste("new")
        XCTAssertFalse(result)
        XCTAssertEqual(editor.text, "old")
        XCTAssertEqual(paste.sends, 1)
        XCTAssertEqual(paste.restores, 1)
    }

    @MainActor func testTypingLettersDigitsAndSpacesKeepsDictationTargetAndPastesAtLatestCaret() async {
        let snapshot = EditableTextSnapshot(original: "before old after", selection: NSRange(location: 7, length: 3))
        let editor = Editor(snapshot), paste = Paste(editor)
        let target = editor.target(snapshot, clipboard: paste)
        for text in ["a", "7", " ", "🙂"] {
            editor.insert(text)
            // AppModel.tick uses this exact check to decide whether to stop/detach.
            XCTAssertTrue(target.isCurrent)
        }
        let result = await target.paste("spoken words")
        XCTAssertTrue(result)
        XCTAssertEqual(editor.text, "before a7 🙂spoken words after")
        XCTAssertEqual(paste.sends, 1)
        XCTAssertEqual(editor.selectionWrites, 0)
    }

    @MainActor func testDictationUsesLatestSelectionEvenWithoutPollingBeforePaste() async {
        let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 0, length: 3))
        let editor = Editor(snapshot), paste = Paste(editor)
        let target = editor.target(snapshot, clipboard: paste)
        editor.insert("typed 🙂 text")
        editor.range = NSRange(location: 6, length: 2)
        let result = await target.paste("spoken")
        XCTAssertTrue(result)
        XCTAssertEqual(editor.text, "typed spoken text")
        XCTAssertEqual(paste.sends, 1)
    }

    @MainActor func testVoiceEditFocusCaretAndUserEditsRejectPasteBeforeClipboardChanges() async {
        let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 0, length: 3))
        for change in 0..<3 {
            let editor = Editor(snapshot), paste = Paste(editor)
            let target = editor.target(snapshot, clipboard: paste, transform: true)
            if change == 0 { editor.focused = false }
            if change == 1 { editor.range = NSRange(location: 3, length: 0) }
            if change == 2 { editor.text = "user edit" }
            let result = await target.paste("new")
            XCTAssertFalse(result)
            XCTAssertEqual(paste.stages, 0)
            XCTAssertEqual(paste.sends, 0)
        }
    }

    @MainActor func testDictationFocusLossAndInvalidSelectionRejectDelivery() async {
        let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 3, length: 0))
        for change in 0..<4 {
            let editor = Editor(snapshot), paste = Paste(editor)
            let target = editor.target(snapshot, clipboard: paste)
            if change == 0 { editor.focused = false }
            if change == 1 { editor.range = NSRange(location: NSNotFound, length: 0) }
            if change == 2 { editor.range = NSRange(location: 2, length: 2) }
            if change == 3 { editor.range = NSRange(location: 0, length: -1) }
            XCTAssertFalse(target.isCurrent)
            let result = await target.paste("new")
            XCTAssertFalse(result)
            XCTAssertEqual(paste.stages, 0)
        }
    }

    @MainActor func testTypingDuringClipboardStagingRejectsPasteWithoutOverwritingUserText() async {
        let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 3, length: 0))
        let editor = Editor(snapshot), paste = Paste(editor)
        paste.beforeSend = { editor.insert("7 ") }
        let result = await editor.target(snapshot, clipboard: paste).paste("new")
        XCTAssertFalse(result)
        XCTAssertEqual(editor.text, "old7 ")
        XCTAssertEqual(paste.sends, 0)
        XCTAssertEqual(paste.restores, 1)
    }

    @MainActor func testFocusChangeDuringClipboardStagingPreventsPasteAndRestoresClipboard() async {
        let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 0, length: 3))
        let editor = Editor(snapshot), paste = Paste(editor)
        paste.beforeSend = { editor.focused = false }
        let result = await editor.target(snapshot, clipboard: paste).paste("new")
        XCTAssertFalse(result)
        XCTAssertEqual(paste.sends, 0)
        XCTAssertEqual(paste.restores, 1)
        XCTAssertEqual(editor.text, "old")
    }

    @MainActor func testCancelledDeliveryLeavesOriginalTextAndClipboardUntouched() async {
        let snapshot = EditableTextSnapshot(original: "old", selection: NSRange(location: 0, length: 3))
        let editor = Editor(snapshot), paste = Paste(editor)
        let target = editor.target(snapshot, clipboard: paste)
        let task = Task { await target.paste("new") }; task.cancel()
        let result = await task.value
        XCTAssertFalse(result)
        XCTAssertEqual(editor.text, "old")
        XCTAssertEqual(paste.stages, 0)
    }

    @MainActor func testCancellationDuringVoiceEditSelectionRestoresOriginalCaretWithoutPasting() async throws {
        let snapshot = EditableTextSnapshot(original: "original", selection: NSRange(location: 3, length: 0))
        let editor = Editor(snapshot), paste = Paste(editor); editor.delayedSelection = true
        let target = editor.target(snapshot, clipboard: paste, transform: true)
        let task = Task { await target.paste("new") }
        try await Task.sleep(for: .milliseconds(10)); task.cancel()
        let result = await task.value
        try await Task.sleep(for: .milliseconds(50))
        XCTAssertFalse(result)
        XCTAssertEqual(editor.text, snapshot.original)
        XCTAssertEqual(editor.range, snapshot.selection)
        XCTAssertEqual(paste.sends, 0)
        XCTAssertEqual(paste.stages, 0)
    }

    @MainActor func testVoiceEditAtCaretSelectsAndReplacesWholeFieldAfterReadback() async {
        let snapshot = EditableTextSnapshot(original: "🙂 original", selection: NSRange(location: 3, length: 0))
        let editor = Editor(snapshot), paste = Paste(editor); editor.delayedSelection = true
        let target = editor.target(snapshot, clipboard: paste, transform: true)
        let result = await target.paste("Новое 🎤")
        XCTAssertTrue(result)
        XCTAssertEqual(editor.text, "Новое 🎤")
        XCTAssertEqual(editor.range, NSRange(location: 8, length: 0))
        XCTAssertEqual(paste.sends, 1)
    }

    @MainActor func testFocusLossDuringVoiceEditSelectionPreventsPaste() async {
        let snapshot = EditableTextSnapshot(original: "original", selection: NSRange(location: 3, length: 0))
        let editor = Editor(snapshot), paste = Paste(editor); editor.losesFocusWhenSelecting = true
        let target = editor.target(snapshot, clipboard: paste, transform: true)
        let result = await target.paste("new")
        XCTAssertFalse(result)
        XCTAssertEqual(editor.text, "original")
        XCTAssertEqual(paste.sends, 0)
    }

    @MainActor func testClipboardPreservesAllTypesAndDoesNotOverwriteNewUserCopy() {
        let board = NSPasteboard.withUniqueName(); defer { board.releaseGlobally() }
        let custom = NSPasteboard.PasteboardType("com.opendictate.test")
        let item = NSPasteboardItem(); item.setString("existing", forType: .string); item.setData(Data([1, 2, 3]), forType: custom)
        board.writeObjects([item])
        let paste = ClipboardPaste(pasteboard: board, post: { _ in true })
        XCTAssertTrue(paste.stage("transcript"))
        XCTAssertEqual(board.string(forType: .string), "transcript")
        XCTAssertNotNil(board.data(forType: NSPasteboard.PasteboardType("org.nspasteboard.ConcealedType")))
        paste.restore()
        XCTAssertEqual(board.string(forType: .string), "existing")
        XCTAssertEqual(board.data(forType: custom), Data([1, 2, 3]))
        XCTAssertTrue(paste.stage("another transcript"))
        board.clearContents(); board.setString("new user copy", forType: .string)
        XCTAssertFalse(paste.send(to: getpid()))
        paste.restore()
        XCTAssertEqual(board.string(forType: .string), "new user copy")
    }

    @MainActor func testNativeTextViewReceivesClipboardReplacement() async {
        let board = NSPasteboard.withUniqueName(); defer { board.releaseGlobally() }
        let snapshot = EditableTextSnapshot(original: "🙂 old after", selection: NSRange(location: 3, length: 3))
        let editor = Editor(snapshot)
        let view = NSTextView(); view.string = snapshot.original; view.setSelectedRange(snapshot.selection)
        let paste = ClipboardPaste(pasteboard: board, post: { _ in
            view.insertText(board.string(forType: .string)!, replacementRange: view.selectedRange())
            editor.text = view.string; editor.range = view.selectedRange(); return true
        })
        let result = await editor.target(snapshot, clipboard: paste).paste("Проверка 🎤")
        XCTAssertTrue(result)
        XCTAssertEqual(view.string, "🙂 Проверка 🎤 after")
    }
}
