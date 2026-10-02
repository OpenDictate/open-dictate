import AppKit
import XCTest
@testable import OpenDictate

final class MenuBarIconTests: XCTestCase {
    func testOnlyRecordingUsesAColoredMicrophone() async throws {
        try await MainActor.run {
            let recording = try XCTUnwrap(MenuBarIcon.image(for: .recording))
            XCTAssertFalse(recording.isTemplate)
            for phase in [AppModel.Phase.idle, .preparing, .processing] {
                let image = try XCTUnwrap(MenuBarIcon.image(for: phase))
                XCTAssertTrue(image.isTemplate)
                XCTAssertEqual(image.size, recording.size)
                XCTAssertEqual(image.accessibilityDescription, "OpenDictate")
            }
        }
    }

    func testRecordingFillsOnlyTheHeadRedInBothAppearances() async throws {
        try await MainActor.run {
            for name in [NSAppearance.Name.aqua, .darkAqua] {
                let appearance = try XCTUnwrap(NSAppearance(named: name))
                let image = try XCTUnwrap(MenuBarIcon.image(for: .recording))
                let canvas = NSImage(size: NSSize(width: image.size.width * 10, height: image.size.height * 10))
                appearance.performAsCurrentDrawingAppearance {
                    canvas.lockFocus()
                    image.draw(in: NSRect(origin: .zero, size: canvas.size))
                    canvas.unlockFocus()
                }
                let bitmap = try XCTUnwrap(NSBitmapImageRep(data: XCTUnwrap(canvas.tiffRepresentation)))
                var redBounds: CGRect?
                var foregroundBounds: CGRect?
                var redPixels = 0
                for y in 0..<bitmap.pixelsHigh {
                    for x in 0..<bitmap.pixelsWide {
                        guard let color = bitmap.colorAt(x: x, y: y)?.usingColorSpace(.deviceRGB),
                              color.alphaComponent > 0.8 else { continue }
                        let pixel = CGRect(x: x, y: y, width: 1, height: 1)
                        if color.redComponent > 0.7, color.greenComponent < 0.5, color.blueComponent < 0.5 {
                            redBounds = redBounds.map { $0.union(pixel) } ?? pixel
                            redPixels += 1
                        } else {
                            foregroundBounds = foregroundBounds.map { $0.union(pixel) } ?? pixel
                        }
                    }
                }
                let head = try XCTUnwrap(redBounds)
                let cradle = try XCTUnwrap(foregroundBounds)
                XCTAssertLessThan(head.width, cradle.width * 0.65, "Red must stay inside the head")
                XCTAssertLessThan(head.maxY, cradle.maxY, "The stem must keep its foreground color")
                XCTAssertEqual(head.midX, cradle.midX, accuracy: 2)
                XCTAssertGreaterThan(CGFloat(redPixels), head.width * head.height * 0.8, "The head must be filled")
            }
        }
    }
}
