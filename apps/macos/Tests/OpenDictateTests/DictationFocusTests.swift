import AppKit
import ApplicationServices
import XCTest
import OpenDictateCore
@testable import OpenDictate

private actor FocusRecorder: AudioRecording {
    private(set) var stops = 0
    func start(keepAudio: Bool, onChunk: @escaping @Sendable (Data) -> Void,
               onLevel: @escaping @Sendable (Float) -> Void,
               onError: @escaping @Sendable (DictationError) -> Void) async throws {}
    func stop() async -> (Data, Int) {
        stops += 1
        return (Data(repeating: 0, count: 4_800), 4_800)
    }
}

@MainActor private final class FocusEditor: TextAccessibility, TextPasting {
    var text: String
    var range: NSRange
    var focused = true
    var sends = 0
    private var staged = ""
    init(_ text: String) { self.text = text; range = NSRange(location: (text as NSString).length, length: 0) }
    func isFocused(_ element: AXUIElement, pid: pid_t) -> Bool { focused }
    func value(_ element: AXUIElement) -> String? { text }
    func selection(_ element: AXUIElement) -> NSRange? { range }
    func setSelection(_ range: NSRange, in element: AXUIElement) -> Bool { self.range = range; return true }
    func stage(_ text: String) -> Bool { staged = text; return true }
    func send(to pid: pid_t) -> Bool {
        sends += 1
        text = (text as NSString).replacingCharacters(in: range, with: staged)
        range = NSRange(location: range.location + (staged as NSString).length, length: 0)
        return true
    }
    func restore() {}
    func target() -> TextTarget {
        TextTarget(element: AXUIElementCreateApplication(getpid()), application: .current,
                   snapshot: EditableTextSnapshot(original: text, selection: range), accessibility: self, clipboard: self)
    }
}

final class DictationFocusTests: XCTestCase {
    @MainActor private func waitFor(_ phase: AppModel.Phase, in model: AppModel) async {
        let deadline = ContinuousClock.now + .seconds(2)
        while model.phase != phase && ContinuousClock.now < deadline { await Task.yield() }
        XCTAssertEqual(model.phase, phase)
    }

    @MainActor func testSwitchingWindowsKeepsRecordingAndPastesIntoFieldAtStop() async {
        let first = FocusEditor("first "), second = FocusEditor("second ")
        var active = first
        var captures = 0
        let recorder = FocusRecorder()
        let model = AppModel(localOnly: true, captureTarget: { _, _ in
            captures += 1; return active.target()
        }, makeRecorder: { recorder })
        defer { model.shutdown() }
        model.toggleLocalRecording()
        await waitFor(.recording, in: model)
        first.focused = false; active = second
        model.tick()
        XCTAssertEqual(model.phase, .recording)
        XCTAssertFalse(model.detached)
        let stops = await recorder.stops
        XCTAssertEqual(stops, 0)
        model.stop()
        await waitFor(.idle, in: model)
        XCTAssertEqual(first.text, "first ")
        XCTAssertEqual(first.sends, 0)
        XCTAssertEqual(second.text, "second " + model.lastTranscript)
        XCTAssertEqual(second.sends, 1)
        XCTAssertEqual(captures, 2)
    }

    @MainActor func testReturningToOriginalWindowDoesNotPermanentlyDetachDelivery() async {
        let editor = FocusEditor("original ")
        let model = AppModel(localOnly: true, captureTarget: { _, _ in editor.target() }, makeRecorder: { FocusRecorder() })
        defer { model.shutdown() }
        model.toggleLocalRecording()
        await waitFor(.recording, in: model)
        editor.focused = false; model.tick()
        editor.focused = true; model.tick()
        XCTAssertEqual(model.phase, .recording)
        model.stop()
        await waitFor(.idle, in: model)
        XCTAssertEqual(editor.sends, 1)
    }

    @MainActor func testSwitchingDuringPreparationUsesFieldAtEarlyStop() async {
        let first = FocusEditor("first "), second = FocusEditor("second ")
        var active = first
        let model = AppModel(localOnly: true, captureTarget: { _, _ in active.target() }, makeRecorder: { FocusRecorder() })
        defer { model.shutdown() }
        model.toggleLocalRecording()
        XCTAssertEqual(model.phase, .preparing)
        first.focused = false; active = second; model.tick()
        XCTAssertEqual(model.phase, .preparing)
        model.stop()
        // A repeated finish action must not recapture a different destination.
        active = first; model.stop()
        await waitFor(.idle, in: model)
        XCTAssertEqual(first.sends, 0)
        XCTAssertEqual(second.sends, 1)
    }

    @MainActor func testSwitchingAfterStopNeverRetargetsPendingTranscript() async {
        let first = FocusEditor("first "), second = FocusEditor("second ")
        var active = first
        let model = AppModel(localOnly: true, captureTarget: { _, _ in active.target() }, makeRecorder: { FocusRecorder() })
        defer { model.shutdown() }
        model.toggleLocalRecording()
        await waitFor(.recording, in: model)
        model.stop()
        first.focused = false; active = second; model.tick()
        await waitFor(.idle, in: model)
        XCTAssertEqual(first.sends, 0)
        XCTAssertEqual(second.sends, 0)
        XCTAssertFalse(model.lastTranscript.isEmpty)
    }

    @MainActor func testIneligibleFieldAtStopKeepsResultWithoutFallbackPaste() async {
        for error in [DictationError.noField, .secureField, .excluded, .accessibility] {
            let editor = FocusEditor("original ")
            var denyCapture = false
            let model = AppModel(localOnly: true, captureTarget: { _, _ in
                if denyCapture { throw error }; return editor.target()
            }, makeRecorder: { FocusRecorder() })
            model.toggleLocalRecording()
            await waitFor(.recording, in: model)
            denyCapture = true; model.stop()
            await waitFor(.idle, in: model)
            XCTAssertEqual(editor.sends, 0)
            XCTAssertFalse(model.lastTranscript.isEmpty)
            model.shutdown()
        }
    }

    @MainActor func testCancellationAfterWindowSwitchNeverPastesOrKeepsTranscript() async {
        let first = FocusEditor("first "), second = FocusEditor("second ")
        var active = first
        let model = AppModel(localOnly: true, captureTarget: { _, _ in active.target() }, makeRecorder: { FocusRecorder() })
        defer { model.shutdown() }
        model.toggleLocalRecording()
        await waitFor(.recording, in: model)
        first.focused = false; active = second; model.tick()
        model.cancel()
        await waitFor(.idle, in: model)
        XCTAssertEqual(first.sends + second.sends, 0)
        XCTAssertTrue(model.lastTranscript.isEmpty)
    }

    @MainActor func testRevokingAccessibilityCancelsRecordingWithoutDelivery() async {
        let editor = FocusEditor("original ")
        var trusted = true
        let model = AppModel(localOnly: true, captureTarget: { _, _ in editor.target() },
                             makeRecorder: { FocusRecorder() }, accessibilityTrust: { trusted })
        defer { model.shutdown() }
        model.toggleLocalRecording()
        await waitFor(.recording, in: model)
        trusted = false; model.tick()
        await waitFor(.idle, in: model)
        XCTAssertEqual(editor.sends, 0)
        XCTAssertTrue(model.lastTranscript.isEmpty)
    }

    @MainActor func testVoiceEditingStillDetachesAndStopsWhenOriginalFieldLosesFocus() async {
        let editor = FocusEditor("original")
        let model = AppModel(localOnly: true, captureTarget: { _, _ in editor.target() }, makeRecorder: { FocusRecorder() })
        defer { model.shutdown() }
        model.toggleLocalRecording(transform: true)
        await waitFor(.recording, in: model)
        editor.focused = false; model.tick()
        XCTAssertTrue(model.detached)
        XCTAssertEqual(model.phase, .processing)
        // Cancel before completion; local checks never make a voice-edit provider request.
        model.cancel()
        await waitFor(.idle, in: model)
        XCTAssertEqual(editor.sends, 0)
    }

}
