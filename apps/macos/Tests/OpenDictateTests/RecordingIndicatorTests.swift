import AppKit
import SwiftUI
import XCTest
@testable import OpenDictate

final class RecordingIndicatorTests: XCTestCase {
    func testPreparingAlreadyShowsFinishControlAtHalfSize() async throws {
        try await MainActor.run {
            let image = try render(.preparing)
            XCTAssertEqual(image.pixelsWide, 44)
            XCTAssertEqual(image.pixelsHigh, 22)
            XCTAssertNotNil(redBounds(image))
        }
    }

    func testRecordingFinishCircleHasExplicitTrailingInset() async throws {
        try await MainActor.run {
            let image = try render(.recording)
            XCTAssertEqual(image.pixelsWide, 44)
            XCTAssertEqual(image.pixelsHigh, 22)
            let bounds = try XCTUnwrap(redBounds(image))
            XCTAssertEqual(CGFloat(image.pixelsWide) - bounds.maxX, 7, accuracy: 1)
            XCTAssertEqual(bounds.width, 14, accuracy: 1)
            XCTAssertEqual(bounds.midY, 11, accuracy: 1)
        }
    }

    @MainActor private func render(_ phase: AppModel.Phase) throws -> NSBitmapImageRep {
        let renderer = ImageRenderer(content: RecordingStatusView(phase: phase, isRussian: false, finish: {}))
        renderer.scale = 1
        return NSBitmapImageRep(cgImage: try XCTUnwrap(renderer.cgImage))
    }

    private func redBounds(_ image: NSBitmapImageRep) -> CGRect? {
        var bounds: CGRect?
        for y in 0..<image.pixelsHigh {
            for x in 0..<image.pixelsWide {
                guard let color = image.colorAt(x: x, y: y)?.usingColorSpace(.deviceRGB),
                      color.redComponent > 0.7, color.greenComponent < 0.45,
                      color.blueComponent < 0.45, color.alphaComponent > 0.8 else { continue }
                let pixel = CGRect(x: x, y: y, width: 1, height: 1)
                bounds = bounds.map { $0.union(pixel) } ?? pixel
            }
        }
        return bounds
    }
}
