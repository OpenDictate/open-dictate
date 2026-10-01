import XCTest
@testable import OpenDictateCore

final class SettingsSyncScheduleTests: XCTestCase {
    func testTypingIsBatchedAndMinutePullCannotBypassQuietPeriod() {
        var queue = SettingsSyncSchedule()
        queue.changed(now: 0); queue.changed(now: 2_000); queue.refresh()
        XCTAssertFalse(queue.begin(now: 3_000))
        XCTAssertEqual(queue.delayUntilReady(now: 3_000), 2_000)
        XCTAssertTrue(queue.begin(now: 5_000))
        queue.finish()
        XCTAssertFalse(queue.begin(now: 6_000))
    }

    func testChangesDuringDownloadOrUploadRemainQueuedUntilTypingStops() {
        var queue = SettingsSyncSchedule()
        queue.refresh(); XCTAssertTrue(queue.begin(now: 0))
        queue.changed(now: 100)
        XCTAssertTrue(queue.hasPendingChange)
        XCTAssertFalse(queue.begin(now: 4_000))
        queue.finish()
        XCTAssertFalse(queue.begin(now: 2_000))
        XCTAssertTrue(queue.begin(now: 3_100))
        XCTAssertFalse(queue.hasPendingChange)
        queue.changed(now: 3_200); queue.finish()
        XCTAssertTrue(queue.begin(now: 6_200))
    }

    func testReopenAndPollingCoalesceWhileBusyAndErrorsRetryOnNextRequest() {
        var queue = SettingsSyncSchedule()
        queue.refresh(); XCTAssertTrue(queue.begin(now: 0))
        for _ in 0..<3 { queue.refresh() }
        XCTAssertFalse(queue.begin(now: 10))
        queue.finish(); XCTAssertTrue(queue.begin(now: 20))
        queue.finish()
        XCTAssertFalse(queue.begin(now: 30))
        queue.refresh(); XCTAssertTrue(queue.begin(now: 60_000))
    }
}
