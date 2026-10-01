import AppKit

// Run on macOS when docs/app-icon.png changes; Android builds use the checked-in PNGs.
let sourceURL = URL(fileURLWithPath: CommandLine.arguments[1])
let resources = URL(fileURLWithPath: CommandLine.arguments[2], isDirectory: true)
guard let source = NSBitmapImageRep(data: try Data(contentsOf: sourceURL))?.cgImage else {
    fatalError("Cannot read app icon: \(sourceURL.path)")
}
let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!
let bitmapInfo = CGImageAlphaInfo.premultipliedLast.rawValue

// The adaptive background belongs to the launcher. Extract only the white glyph
// from the approved artwork, retaining edge antialiasing, for both foreground
// and Android 13+ themed icons. Dark background pixels must be transparent.
let context = CGContext(data: nil, width: source.width, height: source.height,
                        bitsPerComponent: 8, bytesPerRow: source.width * 4,
                        space: colorSpace, bitmapInfo: bitmapInfo)!
context.draw(source, in: CGRect(x: 0, y: 0, width: source.width, height: source.height))
let bytes = context.data!.assumingMemoryBound(to: UInt8.self)
for pixel in 0..<(source.width * source.height) {
    let offset = pixel * 4
    let brightness = Int(max(bytes[offset], bytes[offset + 1], bytes[offset + 2]))
    let alpha = UInt8(min(255, max(0, (brightness - 32) * 255 / (220 - 32))))
    // Premultiplied white: RGB equals alpha.
    for channel in 0..<4 { bytes[offset + channel] = alpha }
}
let foreground = context.makeImage()!

func write(_ image: CGImage, pixels: Int, folder: String, name: String, round: Bool = false) throws {
    let directory = resources.appendingPathComponent(folder, isDirectory: true)
    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    let output = CGContext(data: nil, width: pixels, height: pixels,
                           bitsPerComponent: 8, bytesPerRow: pixels * 4,
                           space: colorSpace, bitmapInfo: bitmapInfo)!
    output.interpolationQuality = .high
    let bounds = CGRect(x: 0, y: 0, width: pixels, height: pixels)
    if round {
        output.setFillColor(CGColor(gray: 8.0 / 255, alpha: 1))
        output.fillEllipse(in: bounds)
    }
    output.draw(image, in: bounds)
    let png = NSBitmapImageRep(cgImage: output.makeImage()!)
        .representation(using: .png, properties: [:])!
    try png.write(to: directory.appendingPathComponent(name))
}

for (density, scale) in [("mdpi", 1.0), ("hdpi", 1.5), ("xhdpi", 2.0),
                         ("xxhdpi", 3.0), ("xxxhdpi", 4.0)] {
    try write(foreground, pixels: Int(108 * scale), folder: "drawable-\(density)",
              name: "ic_launcher_foreground.png")
    try write(source, pixels: Int(48 * scale), folder: "mipmap-\(density)", name: "ic_launcher.png")
    try write(foreground, pixels: Int(48 * scale), folder: "mipmap-\(density)",
              name: "ic_launcher_round.png", round: true)
}
