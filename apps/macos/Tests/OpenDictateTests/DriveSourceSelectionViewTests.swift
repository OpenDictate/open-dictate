import AppKit
import SwiftUI
import XCTest
@testable import OpenDictate

final class DriveSourceSelectionViewTests: XCTestCase {
    @MainActor func testNativeSourceChoiceRendersInBothLanguagesAndAppearances() throws {
        let suite = "drive-view-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let preferences = Preferences(defaults: defaults)
        for language in ["en", "ru"] {
            preferences.interfaceLanguage = language
            for appearanceName in [NSAppearance.Name.aqua, .darkAqua] {
                let host = NSHostingView(rootView: DriveSourceSelectionView(preferences: preferences,
                    onChoose: { _ in }, onCancel: {}).background(Color(nsColor: .windowBackgroundColor)))
                host.appearance = NSAppearance(named: appearanceName)
                host.setFrameSize(host.fittingSize)
                host.layoutSubtreeIfNeeded()
                XCTAssertEqual(host.frame.width, 480, accuracy: 1)
                XCTAssertGreaterThan(host.frame.height, 200)
                XCTAssertLessThan(host.frame.height, 650)
                // Optional local visual evidence; never emitted by CI or the shipped application.
                if let output = ProcessInfo.processInfo.environment["OPENDICTATE_VIEW_CAPTURES"] {
                    let bitmap = try XCTUnwrap(host.bitmapImageRepForCachingDisplay(in: host.bounds))
                    host.cacheDisplay(in: host.bounds, to: bitmap)
                    let data = try XCTUnwrap(bitmap.representation(using: .png, properties: [:]))
                    let directory = URL(fileURLWithPath: output)
                    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
                    try data.write(to: directory.appendingPathComponent("drive-source-\(language)-\(appearanceName.rawValue).png"))
                }
            }
        }
    }
}
