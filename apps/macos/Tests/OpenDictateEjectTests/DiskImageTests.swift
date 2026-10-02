import Foundation
import XCTest
@testable import OpenDictateEject

final class DiskImageTests: XCTestCase {
    func testSelectsOnlyTheImageContainingTheHelperIncludingSpacesAndUnicode() throws {
        let images = try decode(["/Volumes/Unrelated", "/Volumes/OpenDictate 1.2 — тест"])
        XCTAssertEqual(images.installerVolume(containing: URL(fileURLWithPath:
            "/Volumes/OpenDictate 1.2 — тест/Eject.app", isDirectory: true))?.path, "/Volumes/OpenDictate 1.2 — тест")
    }

    func testRejectsCopiedHelperAndSimilarVolumeNames() throws {
        let images = try decode(["/Volumes/OpenDictate", "/"])
        for path in ["/Applications/Eject.app", "/Volumes/OpenDictate other/Eject.app",
                     "/Volumes/OpenDictate/nested/Eject.app", "/Eject.app"] {
            XCTAssertNil(images.installerVolume(containing: URL(fileURLWithPath: path)))
        }
    }

    func testIgnoresUnmountedEntities() throws {
        let data = try PropertyListSerialization.data(fromPropertyList:
            ["images": [["system-entities": [["dev-entry": "/dev/disk9"]]]]], format: .xml, options: 0)
        let images = try PropertyListDecoder().decode(DiskImages.self, from: data)
        XCTAssertNil(images.installerVolume(containing: URL(fileURLWithPath: "/Volumes/OpenDictate/Eject.app")))
    }

    private func decode(_ mounts: [String]) throws -> DiskImages {
        let data = try PropertyListSerialization.data(fromPropertyList:
            ["images": mounts.map { ["system-entities": [["mount-point": $0]]] }], format: .xml, options: 0)
        return try PropertyListDecoder().decode(DiskImages.self, from: data)
    }
}
