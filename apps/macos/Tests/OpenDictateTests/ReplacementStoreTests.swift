import XCTest
@testable import OpenDictate

final class ReplacementStoreTests: XCTestCase {
    func testEditsPersistAndSessionRulesAreImmutable() async {
        await MainActor.run {
            let suite = "com.opendictate.tests.replacements.\(UUID().uuidString)"
            let defaults = UserDefaults(suiteName: suite)!
            defer { defaults.removePersistentDomain(forName: suite) }
            let store = ReplacementStore(defaults: defaults)
            XCTAssertTrue(store.save(source: " cat ", replacement: " dog "))
            let rule = store.rules[0], active = store.engine()
            XCTAssertFalse(store.save(source: "CAT", replacement: "duplicate"))
            XCTAssertTrue(store.save(id: rule.id, source: "cat", replacement: "bird"))
            XCTAssertEqual(active.apply("cat"), "dog")
            XCTAssertEqual(ReplacementStore(defaults: defaults).engine().apply("cat"), "bird")
            store.enabled = false
            XCTAssertEqual(ReplacementStore(defaults: defaults).engine().apply("cat"), "cat")
            store.enabled = true; store.update(store.rules[0], enabled: false)
            XCTAssertEqual(ReplacementStore(defaults: defaults).engine().apply("cat"), "cat")
            store.delete(rule)
            XCTAssertTrue(ReplacementStore(defaults: defaults).rules.isEmpty)
        }
    }
    func testCorruptDocumentCannotBeOverwritten() async {
        await MainActor.run {
            let suite = "com.opendictate.tests.replacements.\(UUID().uuidString)"
            let defaults = UserDefaults(suiteName: suite)!
            defer { defaults.removePersistentDomain(forName: suite) }
            let data = Data("not-json".utf8)
            defaults.set(data, forKey: "wordReplacementDocument")
            let store = ReplacementStore(defaults: defaults)
            XCTAssertEqual(store.status, .storageError)
            XCTAssertFalse(store.save(source: "cat", replacement: "dog"))
            XCTAssertEqual(defaults.data(forKey: "wordReplacementDocument"), data)
        }
    }
}
