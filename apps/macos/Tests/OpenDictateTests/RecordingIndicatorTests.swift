import AppKit
import SwiftUI
import XCTest
@testable import OpenDictate

final class RecordingIndicatorTests: XCTestCase {
    func testWaveformPanelNeverTakesFocusAndIgnoresProcessingClicks() async throws {
        try await MainActor.run {
            let application = NSApplication.shared
            let originalKeyWindow = application.keyWindow
            let originalProcess = NSWorkspace.shared.frontmostApplication?.processIdentifier
            let indicator = RecordingIndicator()
            defer { indicator.hide() }
            indicator.show(phase: .preparing, style: .waveform, audioLevels: RecordingAudioLevels(),
                           isRussian: false, finish: {})
            let panel = try XCTUnwrap(application.windows.first {
                $0.title == "OpenDictate recording indicator" && $0.isVisible
            } as? NSPanel)
            XCTAssertEqual(panel.contentView?.frame.size, NSSize(width: 50, height: 22))
            XCTAssertFalse(panel.canBecomeKey)
            XCTAssertFalse(panel.canBecomeMain)
            XCTAssertFalse(panel.ignoresMouseEvents)
            XCTAssertTrue(application.keyWindow === originalKeyWindow)
            XCTAssertEqual(NSWorkspace.shared.frontmostApplication?.processIdentifier, originalProcess)
            indicator.show(phase: .processing, style: .waveform, audioLevels: RecordingAudioLevels(),
                           isRussian: false, finish: {})
            XCTAssertTrue(panel.ignoresMouseEvents)
            XCTAssertEqual(panel.contentView?.frame.size, NSSize(width: 50, height: 22))
            indicator.show(phase: .idle, style: .waveform, audioLevels: RecordingAudioLevels(),
                           isRussian: false, finish: {})
            XCTAssertFalse(panel.isVisible)
        }
    }

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

    func testWaveformHasTenBarsAndNewestInputChangesRightmostHeight() async throws {
        try await MainActor.run {
            let levels = RecordingAudioLevels()
            let quiet = try render(.preparing, style: .waveform, levels: levels)
            XCTAssertEqual(quiet.pixelsWide, 50)
            XCTAssertEqual(quiet.pixelsHigh, 22)
            XCTAssertNil(redBounds(quiet))
            let quietBars = whiteBars(quiet)
            XCTAssertEqual(quietBars.count, 10)
            levels.append(1)
            let loud = try render(.recording, style: .waveform, levels: levels)
            let loudBars = whiteBars(loud)
            XCTAssertEqual(loudBars.count, 10)
            XCTAssertEqual(Array(loudBars.dropLast()), Array(quietBars.dropLast()))
            XCTAssertGreaterThan(try XCTUnwrap(loudBars.last).height, try XCTUnwrap(quietBars.last).height + 5)
            XCTAssertEqual(RecordingStatusView.size(phase: .processing, style: .waveform), NSSize(width: 50, height: 22))
        }
    }

    @MainActor private func render(_ phase: AppModel.Phase, style: RecordingIndicatorStyle = .compact,
                                   levels: RecordingAudioLevels? = nil) throws -> NSBitmapImageRep {
        let renderer = ImageRenderer(content: RecordingStatusView(phase: phase, style: style, audioLevels: levels,
                                                                  isRussian: false, finish: {}))
        renderer.scale = 1
        return NSBitmapImageRep(cgImage: try XCTUnwrap(renderer.cgImage))
    }

    private func whiteBars(_ image: NSBitmapImageRep) -> [CGRect] {
        var bars = [CGRect]()
        for x in 0..<image.pixelsWide {
            var column: CGRect?
            for y in 0..<image.pixelsHigh {
                guard let color = image.colorAt(x: x, y: y)?.usingColorSpace(.deviceRGB),
                      min(color.redComponent, color.greenComponent, color.blueComponent) > 0.5 else { continue }
                let pixel = CGRect(x: x, y: y, width: 1, height: 1)
                column = column.map { $0.union(pixel) } ?? pixel
            }
            if let column {
                if let previous = bars.last, previous.maxX == column.minX {
                    bars[bars.count - 1] = previous.union(column)
                } else { bars.append(column) }
            }
        }
        return bars
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
