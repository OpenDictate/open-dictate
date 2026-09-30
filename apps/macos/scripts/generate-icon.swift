import AppKit

// Native vector adaptation of the established white microphone on black.
// Origin: docs/mark.svg and the OpenDictate brand; no generated artwork.
let folder = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
for size in [16, 32, 128, 256, 512] {
    for scale in [1, 2] {
        let pixels = size * scale
        let image = NSImage(size: NSSize(width: pixels, height: pixels))
        image.lockFocus()
        let context = NSGraphicsContext.current!.cgContext
        context.scaleBy(x: CGFloat(pixels) / 1024, y: CGFloat(pixels) / 1024)
        NSColor(calibratedWhite: 0.03, alpha: 1).setFill()
        NSBezierPath(roundedRect: NSRect(x: 32, y: 32, width: 960, height: 960), xRadius: 210, yRadius: 210).fill()
        NSColor.white.setFill()
        NSBezierPath(roundedRect: NSRect(x: 407, y: 405, width: 210, height: 375), xRadius: 105, yRadius: 105).fill()
        let cradle = NSBezierPath()
        cradle.move(to: NSPoint(x: 309, y: 475))
        cradle.curve(to: NSPoint(x: 715, y: 475), controlPoint1: NSPoint(x: 309, y: 202), controlPoint2: NSPoint(x: 715, y: 202))
        cradle.lineWidth = 53; cradle.lineCapStyle = .round
        NSColor.white.setStroke(); cradle.stroke()
        NSBezierPath(roundedRect: NSRect(x: 486, y: 242, width: 52, height: 118), xRadius: 10, yRadius: 10).fill()
        NSBezierPath(roundedRect: NSRect(x: 407, y: 218, width: 210, height: 52), xRadius: 26, yRadius: 26).fill()
        image.unlockFocus()
        let representation = NSBitmapImageRep(data: image.tiffRepresentation!)!
        let png = representation.representation(using: .png, properties: [:])!
        let suffix = scale == 2 ? "@2x" : ""
        try png.write(to: folder.appendingPathComponent("icon_\(size)x\(size)\(suffix).png"))
    }
}
