import AppKit

@MainActor
enum MenuBarIcon {
    static func image(for phase: AppModel.Phase) -> NSImage? {
        guard phase == .recording else {
            return NSImage(systemSymbolName: "mic", accessibilityDescription: "OpenDictate")
        }
        guard let microphone = NSImage(systemSymbolName: "mic.fill", accessibilityDescription: "OpenDictate") else {
            return nil
        }
        let size = microphone.size
        let image = NSImage(size: size, flipped: false) { bounds in
            // mic.fill has one palette layer. Tint its central capsule separately
            // so the surrounding cradle and stem retain the menu bar foreground.
            microphone.draw(in: bounds)
            NSColor.labelColor.setFill()
            bounds.fill(using: .sourceIn)
            NSGraphicsContext.saveGraphicsState()
            NSRect(x: bounds.minX + bounds.width * 0.30,
                   y: bounds.minY + bounds.height * 0.35,
                   width: bounds.width * 0.36, height: bounds.height * 0.65).clip()
            NSColor.systemRed.setFill()
            bounds.fill(using: .sourceAtop)
            NSGraphicsContext.restoreGraphicsState()
            return true
        }
        image.isTemplate = false
        image.accessibilityDescription = "OpenDictate"
        return image
    }
}
