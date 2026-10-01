import XCTest
@testable import OpenDictateCore

final class WordReplacementTests: XCTestCase {
    private struct Fixture: Decodable {
        struct Rule: Decodable { let source: String; let replacement: String; let enabled: Bool? }
        let name: String; let rules: [Rule]; let text: String; let expected: String; let final: Bool?
    }
    func testSharedPlatformCases() throws {
        var root = URL(fileURLWithPath: #filePath)
        for _ in 0..<5 { root.deleteLastPathComponent() }
        let cases = try JSONDecoder().decode([Fixture].self, from: Data(contentsOf: root.appendingPathComponent("shared/word-replacement-cases.json")))
        for fixture in cases {
            let rules = fixture.rules.map { WordReplacement(source: $0.source, replacement: $0.replacement, enabled: $0.enabled ?? true) }
            XCTAssertEqual(WordReplacementEngine(rules: rules).apply(fixture.text, final: fixture.final ?? true), fixture.expected, fixture.name)
        }
    }
    func testSessionSnapshotAndCumulativeUpdates() {
        var rule = WordReplacement(source: "cat", replacement: "dog")
        let session = WordReplacementEngine(rules: [rule])
        rule.replacement = "bird"
        XCTAssertEqual(session.apply("cat ", final: false), "dog ")
        XCTAssertEqual(session.apply("cat cat.", final: false), "dog dog.")
        XCTAssertEqual(WordReplacementEngine(rules: [rule]).apply("cat"), "bird")
    }
    func testDocumentRoundTripAndValidation() throws {
        let rule = WordReplacement(source: "тест", replacement: "Test")
        let document = ReplacementDocument(rules: [rule])
        XCTAssertEqual(try JSONDecoder().decode(ReplacementDocument.self, from: JSONEncoder().encode(document)), document)
        XCTAssertFalse(ReplacementDocument(rules: [rule, rule]).isValid)
        XCTAssertFalse(WordReplacement(source: " ", replacement: "Test").isValid)
        XCTAssertFalse(WordReplacement(source: "test", replacement: "").isValid)
        XCTAssertFalse(WordReplacement(source: String(repeating: "x", count: 257), replacement: "Test").isValid)
        var future = document; future.schemaVersion = 2
        XCTAssertFalse(future.isValid)
    }
}
