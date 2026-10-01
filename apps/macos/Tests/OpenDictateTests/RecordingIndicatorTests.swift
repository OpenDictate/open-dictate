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
            XCTAssertEqual(panel.contentLayoutRect.size, NSSize(width: 63, height: 28))
            XCTAssertFalse(panel.canBecomeKey)
            XCTAssertFalse(panel.canBecomeMain)
            XCTAssertFalse(panel.ignoresMouseEvents)
            XCTAssertTrue(application.keyWindow === originalKeyWindow)
            XCTAssertEqual(NSWorkspace.shared.frontmostApplication?.processIdentifier, originalProcess)
            indicator.show(phase: .processing, style: .waveform, audioLevels: RecordingAudioLevels(),
                           isRussian: false, finish: {})
            XCTAssertTrue(panel.ignoresMouseEvents)
            XCTAssertEqual(panel.contentLayoutRect.size, NSSize(width: 63, height: 28))
            indicator.show(phase: .idle, style: .waveform, audioLevels: RecordingAudioLevels(),
                           isRussian: false, finish: {})
            XCTAssertFalse(panel.isVisible)
        }
    }

    @MainActor func testFinishControlCannotTakeFocusAndDefersActionUntilAfterClick() async throws {
        let view = IndicatorFinishView(frame: NSRect(x: 0, y: 0, width: 20, height: 20))
        let originalWindow = NSApplication.shared.keyWindow
        let originalProcess = NSWorkspace.shared.frontmostApplication?.processIdentifier
        let finished = expectation(description: "Finish runs after the mouse event")
        var called = false
        view.finish = { called = true; finished.fulfill() }
        XCTAssertFalse(view.acceptsFirstResponder)
        XCTAssertTrue(view.acceptsFirstMouse(for: nil))
        XCTAssertTrue(view.accessibilityPerformPress())
        XCTAssertFalse(called)
        await fulfillment(of: [finished], timeout: 1)
        XCTAssertTrue(NSApplication.shared.keyWindow === originalWindow)
        XCTAssertEqual(NSWorkspace.shared.frontmostApplication?.processIdentifier, originalProcess)
    }

    func testPreparingAlreadyShowsFinishControlAtLargerSize() async throws {
        try await MainActor.run {
            let image = try render(.preparing)
            XCTAssertEqual(image.pixelsWide, 55)
            XCTAssertEqual(image.pixelsHigh, 28)
            XCTAssertNotNil(redBounds(image))
        }
    }

    func testFinishCircleHasEqualTopBottomAndTrailingInsets() async throws {
        try await MainActor.run {
            for phase in [AppModel.Phase.preparing, .recording] {
                let image = try render(phase)
                XCTAssertEqual(image.pixelsWide, 55)
                XCTAssertEqual(image.pixelsHigh, 28)
                let bounds = try XCTUnwrap(redBounds(image))
                let trailingInset = CGFloat(image.pixelsWide) - bounds.maxX
                XCTAssertEqual(trailingInset, 5, accuracy: 1)
                XCTAssertEqual(bounds.minY, trailingInset, accuracy: 1)
                XCTAssertEqual(CGFloat(image.pixelsHigh) - bounds.maxY, trailingInset, accuracy: 1)
                XCTAssertEqual(bounds.width, 17.5, accuracy: 2)
            }
        }
    }

    func testWaveformHasTenBarsAndNewestInputChangesRightmostHeight() async throws {
        try await MainActor.run {
            let levels = RecordingAudioLevels()
            let quiet = try render(.preparing, style: .waveform, levels: levels)
            XCTAssertEqual(quiet.pixelsWide, 63)
            XCTAssertEqual(quiet.pixelsHigh, 28)
            XCTAssertNil(redBounds(quiet))
            let quietBars = whiteBars(quiet)
            XCTAssertEqual(quietBars.count, 10)
            levels.append(1)
            let loud = try render(.recording, style: .waveform, levels: levels)
            let loudBars = whiteBars(loud)
            XCTAssertEqual(loudBars.count, 10)
            XCTAssertEqual(Array(loudBars.dropLast()), Array(quietBars.dropLast()))
            XCTAssertGreaterThan(try XCTUnwrap(loudBars.last).height, try XCTUnwrap(quietBars.last).height + 5)
            XCTAssertEqual(RecordingStatusView.size(phase: .processing, style: .waveform), NSSize(width: 62.5, height: 27.5))
        }
    }

    @MainActor private func render(_ phase: AppModel.Phase, style: RecordingIndicatorStyle = .compact,
                                   levels: RecordingAudioLevels? = nil) throws -> NSBitmapImageRep {
        let size = RecordingStatusView.size(phase: phase, style: style)
        let view = NSHostingView(rootView: RecordingStatusView(phase: phase, style: style, audioLevels: levels,
                                                              isRussian: false, finish: {}))
        view.appearance = NSAppearance(named: .darkAqua)
        view.frame = NSRect(origin: .zero, size: size)
        view.layoutSubtreeIfNeeded()
        let bitmap = try XCTUnwrap(view.bitmapImageRepForCachingDisplay(in: view.bounds))
        view.cacheDisplay(in: view.bounds, to: bitmap)
        let source = try XCTUnwrap(bitmap.cgImage)
        let width = Int(ceil(size.width)), height = Int(ceil(size.height))
        let context = try XCTUnwrap(CGContext(data: nil, width: width, height: height, bitsPerComponent: 8,
                                             bytesPerRow: 0, space: CGColorSpaceCreateDeviceRGB(),
                                             bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue))
        context.draw(source, in: CGRect(x: 0, y: 0, width: width, height: height))
        let result = NSBitmapImageRep(cgImage: try XCTUnwrap(context.makeImage()))
        if ProcessInfo.processInfo.environment["OPENDICTATE_RENDER_TESTS"] == "1", let png = result.representation(using: .png, properties: [:]) {
            try png.write(to: URL(fileURLWithPath: "/tmp/opendictate-\(style.rawValue)-\(phase).png"))
        }
        return result
    }

    private func whiteBars(_ image: NSBitmapImageRep) -> [CGRect] {
        var bars = [CGRect]()
        for x in 10..<(image.pixelsWide - 10) {
            var column: CGRect?
            for y in 5..<(image.pixelsHigh - 5) {
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
