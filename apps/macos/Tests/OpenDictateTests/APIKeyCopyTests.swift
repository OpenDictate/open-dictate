import AppKit
import XCTest
@testable import OpenDictate

final class APIKeyCopyTests: XCTestCase {
    func testEnteredKeyIsCopiedAsConfidentialWithoutAppearingInStatus() async {
        await MainActor.run {
            let model = AppModel(localOnly: true)
            defer { model.shutdown() }
            let pasteboard = NSPasteboard.withUniqueName()
            defer { pasteboard.releaseGlobally() }
            let language = model.preferences.interfaceLanguage
            defer { model.preferences.interfaceLanguage = language }
            model.preferences.interfaceLanguage = "en"

            model.copyKey("  sk-test-copy-only\n", to: pasteboard)

            XCTAssertEqual(pasteboard.string(forType: .string), "sk-test-copy-only")
            XCTAssertNotNil(pasteboard.data(forType: NSPasteboard.PasteboardType("org.nspasteboard.ConcealedType")))
            XCTAssertEqual(model.message, "API key copied to clipboard.")
            XCTAssertFalse(model.hasKey)
        }
    }

    func testEmptyFieldInLocalModePreservesClipboardWithoutReadingKeychain() async {
        await MainActor.run {
            let model = AppModel(localOnly: true)
            defer { model.shutdown() }
            let pasteboard = NSPasteboard.withUniqueName()
            defer { pasteboard.releaseGlobally() }
            pasteboard.setString("existing clipboard", forType: .string)

            model.copyKey(" \n", to: pasteboard)

            XCTAssertEqual(pasteboard.string(forType: .string), "existing clipboard")
            XCTAssertTrue(model.message.isEmpty)
        }
    }
}
