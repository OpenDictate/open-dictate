import AppKit

let folder = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
for size in [16, 32, 128, 256, 512] {
    for scale in [1, 2] {
        let pixels = size * scale
        let bitmap = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: pixels, pixelsHigh: pixels,
            bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
            colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
        NSGraphicsContext.saveGraphicsState()
        NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: bitmap)
        let unit = CGFloat(pixels) / 1024
        let transform = NSAffineTransform()
        transform.scale(by: unit)
        transform.concat()
        NSColor(calibratedWhite: 0.93, alpha: 1).setFill()
        NSBezierPath(roundedRect: NSRect(x: 64, y: 64, width: 896, height: 896),
                     xRadius: 200, yRadius: 200).fill()
        NSColor(calibratedWhite: 0.22, alpha: 1).setFill()
        let triangle = NSBezierPath()
        triangle.move(to: NSPoint(x: 512, y: 744))
        triangle.line(to: NSPoint(x: 264, y: 432))
        triangle.line(to: NSPoint(x: 760, y: 432))
        triangle.close()
        triangle.fill()
        NSBezierPath(roundedRect: NSRect(x: 264, y: 280, width: 496, height: 88),
                     xRadius: 12, yRadius: 12).fill()
        NSGraphicsContext.restoreGraphicsState()
        let suffix = scale == 2 ? "@2x" : ""
        try bitmap.representation(using: .png, properties: [:])!.write(
            to: folder.appendingPathComponent("icon_\(size)x\(size)\(suffix).png"))
    }
}
