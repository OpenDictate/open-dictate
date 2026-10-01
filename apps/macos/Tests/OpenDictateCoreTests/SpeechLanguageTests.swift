import XCTest
@testable import OpenDictateCore

final class SpeechLanguageTests: XCTestCase {
    func testLegacySelectionsMigrateWithoutLosingBilingualChoice() {
        XCTAssertEqual(SpeechLanguage.restore(stored: nil, legacy: "ru-en"), ["en", "ru"])
        for code in ["ru", "en", "uk"] {
            XCTAssertEqual(SpeechLanguage.restore(stored: nil, legacy: code), [code])
        }
        for legacy in [nil, "auto", "unknown"] {
            XCTAssertEqual(SpeechLanguage.restore(stored: nil, legacy: legacy), [])
        }
    }

    func testNewSelectionOverridesLegacyIncludingAutomaticDetection() {
        XCTAssertEqual(SpeechLanguage.restore(stored: [], legacy: "ru-en"), [])
        XCTAssertEqual(SpeechLanguage.restore(stored: ["de", "fr", "de", "unknown"], legacy: "ru"), ["de", "fr"])
    }

    func testCatalogUsesUniqueISO6391CodesAndSearchesAllDisplayLanguages() throws {
        let ids = SpeechLanguage.all.map(\.id)
        XCTAssertEqual(ids.count, 64)
        XCTAssertEqual(Set(ids).count, ids.count)
        XCTAssertTrue(ids.allSatisfy { $0.count == 2 && Locale.isoLanguageCodes.contains($0) })
        let german = try XCTUnwrap(SpeechLanguage.all.first { $0.id == "de" })
        for query in ["de", "GERMAN", "Deutsch", "немец", "  German  "] {
            XCTAssertTrue(german.matches(query), query)
        }
        let french = try XCTUnwrap(SpeechLanguage.all.first { $0.id == "fr" })
        XCTAssertTrue(french.matches("francais"))
        XCTAssertFalse(german.matches("not-a-language"))
        XCTAssertTrue(german.matches("  "))
        XCTAssertTrue(ids.contains("ka"))
    }
}
