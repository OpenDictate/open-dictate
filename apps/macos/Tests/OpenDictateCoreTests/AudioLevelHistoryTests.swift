import XCTest
@testable import OpenDictateCore

final class AudioLevelHistoryTests: XCTestCase {
    func testRecentLevelsStayBoundedAndMoveFromRightToLeft() {
        var history = AudioLevelHistory()
        XCTAssertEqual(history.samples, Array(repeating: 0, count: 10))
        for value in 1...15 { history.append(Float(value) / 20) }
        XCTAssertEqual(history.samples, (6...15).map { Float($0) / 20 })
        history.append(0)
        XCTAssertEqual(history.samples, (7...15).map { Float($0) / 20 } + [0])
    }

    func testInvalidLevelsCannotProduceInvalidBarHeights() {
        var history = AudioLevelHistory()
        for value: Float in [-1, 2, .nan, .infinity, -.infinity] { history.append(value) }
        XCTAssertEqual(Array(history.samples.suffix(5)), [0, 1, 0, 0, 0])
    }

    func testEndingSessionClearsPreviousInputEnvelope() {
        var history = AudioLevelHistory()
        for _ in 0..<1_000 { history.append(0.8) }
        XCTAssertEqual(history.samples.count, 10)
        history.reset()
        XCTAssertEqual(history.samples, Array(repeating: 0, count: 10))
    }
}
