import AppKit

// Render the original Android vector directly, keeping one source of glyph geometry.
// This path reader supports the absolute M/L/C/H/V/Z commands used by that asset.
func path(_ data: String) -> CGPath {
    let expression = try! NSRegularExpression(pattern: "[A-Za-z]|[-+]?(?:[0-9]*\\.[0-9]+|[0-9]+)(?:[eE][-+]?[0-9]+)?")
    let text = data as NSString
    let tokens = expression.matches(in: data, range: NSRange(location: 0, length: text.length))
        .map { text.substring(with: $0.range) }
    let result = CGMutablePath()
    var index = 0, command = ""
    var current = CGPoint.zero
    func number() -> CGFloat {
        guard index < tokens.count, let value = Double(tokens[index]) else {
            fatalError("Invalid coordinate in Android icon path")
        }
        index += 1
        return CGFloat(value)
    }
    func point() -> CGPoint { CGPoint(x: number(), y: number()) }
    while index < tokens.count {
        if tokens[index].first!.isLetter { command = tokens[index]; index += 1 }
        switch command {
        case "M": current = point(); result.move(to: current); command = "L"
        case "L": current = point(); result.addLine(to: current)
        case "C":
            let first = point(), second = point(); current = point()
            result.addCurve(to: current, control1: first, control2: second)
        case "H": current.x = number(); result.addLine(to: current)
        case "V": current.y = number(); result.addLine(to: current)
        case "Z": result.closeSubpath(); current = result.currentPoint; command = ""
        default: fatalError("Unsupported Android icon path command: \(command)")
        }
    }
    return result
}

let sourceURL = URL(fileURLWithPath: CommandLine.arguments[1])
let folder = URL(fileURLWithPath: CommandLine.arguments[2], isDirectory: true)
let document = try XMLDocument(contentsOf: sourceURL)
let vector = document.rootElement()!
let group = vector.elements(forName: "group").first!
func value(_ element: XMLElement, _ name: String) -> CGFloat {
    CGFloat(Double(element.attribute(forName: "android:\(name)")!.stringValue!)!)
}
let width = value(vector, "viewportWidth"), height = value(vector, "viewportHeight")
let transform = CGAffineTransform(a: value(group, "scaleX"), b: 0, c: 0,
                                 d: value(group, "scaleY"),
                                 tx: value(group, "translateX"), ty: value(group, "translateY"))
let paths = group.elements(forName: "path").map {
    path($0.attribute(forName: "android:pathData")!.stringValue!)
}
try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
for size in [16, 32, 128, 256, 512] {
    for scale in [1, 2] {
        let pixels = size * scale
        let context = CGContext(data: nil, width: pixels, height: pixels,
                                bitsPerComponent: 8, bytesPerRow: pixels * 4,
                                space: CGColorSpace(name: CGColorSpace.sRGB)!,
                                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        let unit = CGFloat(pixels) / 1024
        context.setFillColor(red: 8.0 / 255, green: 8.0 / 255, blue: 8.0 / 255, alpha: 1)
        context.addPath(CGPath(roundedRect: CGRect(x: 32 * unit, y: 32 * unit,
                                                 width: 960 * unit, height: 960 * unit),
                               cornerWidth: 210 * unit, cornerHeight: 210 * unit, transform: nil))
        context.fillPath()
        // VectorDrawable coordinates run downwards from the top-left.
        context.translateBy(x: 0, y: CGFloat(pixels))
        context.scaleBy(x: CGFloat(pixels) / width, y: -CGFloat(pixels) / height)
        context.concatenate(transform)
        context.setFillColor(red: 1, green: 1, blue: 1, alpha: 1)
        for path in paths { context.addPath(path); context.fillPath() }
        let png = NSBitmapImageRep(cgImage: context.makeImage()!)
            .representation(using: .png, properties: [:])!
        let suffix = scale == 2 ? "@2x" : ""
        try png.write(to: folder.appendingPathComponent("icon_\(size)x\(size)\(suffix).png"))
    }
}
