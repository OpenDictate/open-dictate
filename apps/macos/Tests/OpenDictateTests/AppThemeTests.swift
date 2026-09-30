import SwiftUI
import XCTest
@testable import OpenDictate

final class AppThemeTests: XCTestCase {
    func testSystemThemeLeavesAppearanceToMacOS() {
        XCTAssertNil(AppTheme.system.colorScheme)
        XCTAssertEqual(AppTheme.light.colorScheme, .light)
        XCTAssertEqual(AppTheme.dark.colorScheme, .dark)
    }

    func testExistingPreferencesKeepDarkAppearanceAndUnknownValuesRecover() async {
        await MainActor.run {
            let suite = "com.opendictate.tests.theme.\(UUID().uuidString)"
            let defaults = UserDefaults(suiteName: suite)!
            defer { defaults.removePersistentDomain(forName: suite) }
            XCTAssertEqual(Preferences(defaults: defaults).theme, .dark)
            defaults.set("unknown", forKey: "theme")
            XCTAssertEqual(Preferences(defaults: defaults).theme, .dark)
        }
    }

    func testThemeChoiceSurvivesPreferencesReload() async {
        await MainActor.run {
            let suite = "com.opendictate.tests.theme.\(UUID().uuidString)"
            let defaults = UserDefaults(suiteName: suite)!
            defer { defaults.removePersistentDomain(forName: suite) }
            let preferences = Preferences(defaults: defaults)
            for theme in AppTheme.allCases {
                preferences.theme = theme
                XCTAssertEqual(Preferences(defaults: defaults).theme, theme)
            }
        }
    }
}
