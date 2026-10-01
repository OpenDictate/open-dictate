import XCTest
@testable import OpenDictateCore

final class SettingsSyncTests: XCTestCase {
    private func document(_ key: String, _ value: String, _ time: Int64, _ device: String) -> SettingsSyncDocument {
        var result = SettingsSyncDocument()
        result.entries[key] = .init(value: value, modifiedAt: time, deviceId: device)
        return result
    }
    func testIndependentChangesAndConcurrentConflictsConverge() {
        var android = document("dictionary", "Android\nНикита", 100, "android")
        android.merge(document("textModel", "gpt-6-sol", 120, "android"))
        var mac = document("dictionary", "Mac", 100, "mac")
        mac.merge(document("mode", "live", 130, "mac"))
        var left = android; left.merge(mac)
        var right = mac; right.merge(android)
        XCTAssertEqual(left, right)
        left.merge(android); left.merge(mac); XCTAssertEqual(left, right)
        XCTAssertEqual(left.entries["dictionary"]?.value, "Mac")
        XCTAssertEqual(left.entries["mode"]?.value, "live")
        XCTAssertEqual(left.entries["textModel"]?.value, "gpt-6-sol")
    }
    func testEmptyDictionarySurvivesStaleReplica() {
        let old = document("dictionary", "OpenDictate", 1, "mac")
        var cleared = old; cleared.record(["dictionary": ""], deviceId: "android", now: 2)
        cleared.merge(old); XCTAssertEqual(cleared.entries["dictionary"]?.value, "")
    }
    func testNewDeviceDefaultsAndClockRollback() {
        var seed = SettingsSyncDocument()
        seed.record(["mode": "accurate"], deviceId: "android", now: 0, seed: true)
        var merged = seed; merged.merge(document("mode", "live", 1000, "mac"))
        XCTAssertEqual(merged.entries["mode"]?.value, "live")
        merged.record(["mode": "accurate"], deviceId: "android", now: 10)
        XCTAssertEqual(merged.entries["mode"]?.modifiedAt, 1001)
        let unchanged = merged; merged.record(["mode": "accurate"], deviceId: "android", now: 2000)
        XCTAssertEqual(merged, unchanged)
        seed.promoteSeeds(deviceId: "android", now: 100)
        XCTAssertGreaterThan(seed.entries["mode"]!.modifiedAt, 0)
    }
    func testSharedWireFormatAndAllowlist() throws {
        let json = #"{"schemaVersion":1,"entries":{"dictionary":{"value":"Никита\nOpenDictate 📝","modifiedAt":1760000000000,"deviceId":"mac"},"future.color":{"value":"dark","modifiedAt":2,"deviceId":"mac"}}}"#
        var parsed = try SettingsSyncDocument.decode(Data(json.utf8))
        XCTAssertEqual(parsed, try SettingsSyncDocument.decode(JSONEncoder().encode(parsed)))
        parsed.record(["textModel": "gpt-6-sol", "apiKey": "private-test-key", "history": "private-test-history"], deviceId: "android", now: 1)
        XCTAssertEqual(parsed.entries["future.color"]?.value, "dark")
        XCTAssertFalse(String(decoding: try JSONEncoder().encode(parsed), as: UTF8.self).contains("private-test"))
    }
    func testUnsupportedAndMalformedDocumentsFailClosed() {
        for json in [#"{"schemaVersion":2,"entries":{}}"#,
                     #"{"schemaVersion":1,"entries":{"mode":{"value":false,"modifiedAt":1,"deviceId":"a"}}}"#,
                     #"{"schemaVersion":1,"entries":{"mode":{"value":"live","modifiedAt":-1,"deviceId":"a"}}}"#,
                     String(repeating: "x", count: 1_048_577)] {
            XCTAssertThrowsError(try SettingsSyncDocument.decode(Data(json.utf8)))
        }
    }
    func testSelectedModelsReachBothTranscriptionRequests() throws {
        var context = TranscriptionContext()
        context.liveModel = "custom-live-model"; context.accurateModel = "custom-file-model"
        let session = OpenAIRequest.sessionUpdate(context: context)["session"] as! [String: Any]
        let input = ((session["audio"] as! [String: Any])["input"] as! [String: Any])
        XCTAssertEqual((input["transcription"] as! [String: Any])["model"] as? String, "custom-live-model")
        let multipart = OpenAIRequest.multipart(wav: Data(), context: context, boundary: "test")
        XCTAssertTrue(String(decoding: multipart, as: UTF8.self).contains("name=\"model\"\r\n\r\ncustom-file-model"))
    }
}
