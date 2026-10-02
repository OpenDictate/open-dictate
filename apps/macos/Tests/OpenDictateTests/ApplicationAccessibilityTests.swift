import ApplicationServices
import XCTest
@testable import OpenDictate

final class ApplicationAccessibilityTests: XCTestCase {
    @MainActor func testElectronFocusIsExposedBeforeCaptureEvenWhenManualFlagDoesNotEnableIt() {
        let field = AXUIElementCreateApplication(getpid())
        var enhanced = false
        var writes: [String] = []
        let accessibility = ApplicationAccessibility(read: { attribute in
            switch attribute {
            case "AXManualAccessibility": return kCFBooleanFalse
            case "AXEnhancedUserInterface": return enhanced ? kCFBooleanTrue : kCFBooleanFalse
            case kAXFocusedUIElementAttribute: return enhanced ? field : nil
            default: return nil
            }
        }, enable: { attribute in
            writes.append(attribute)
            if attribute == "AXEnhancedUserInterface" { enhanced = true }
        })
        XCTAssertNil(accessibility.focusedElement())
        accessibility.prepare()
        XCTAssertTrue(accessibility.focusedElement().map { CFEqual($0, field) } == true)
        XCTAssertEqual(writes, ["AXManualAccessibility", "AXEnhancedUserInterface"])
    }

    @MainActor func testUnsupportedAndAlreadyEnabledAttributesAreLeftAlone() {
        var writes: [String] = []
        for value: CFTypeRef? in [nil, kCFBooleanTrue, "false" as CFString] {
            let accessibility = ApplicationAccessibility(read: { _ in value }, enable: { writes.append($0) })
            accessibility.prepare()
        }
        XCTAssertTrue(writes.isEmpty)
    }

    @MainActor func testPreparationNeverReadsFieldContentsOrChangesFocus() {
        var reads: [String] = [], writes: [String] = []
        let accessibility = ApplicationAccessibility(read: { attribute in
            reads.append(attribute); return kCFBooleanFalse
        }, enable: { writes.append($0) })
        accessibility.prepare()
        XCTAssertEqual(reads, ["AXManualAccessibility", "AXEnhancedUserInterface"])
        XCTAssertEqual(writes, reads)
    }

    @MainActor func testInvalidFocusedElementIsRejected() {
        let accessibility = ApplicationAccessibility(read: { _ in kCFBooleanTrue }, enable: { _ in })
        XCTAssertNil(accessibility.focusedElement())
    }
}
