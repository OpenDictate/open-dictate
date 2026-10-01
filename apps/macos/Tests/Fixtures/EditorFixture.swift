import AppKit

@main
struct EditorFixture {
    @MainActor static func main() {
        let app = NSApplication.shared
        let delegate = Delegate(); app.delegate = delegate
        app.setActivationPolicy(.regular); app.run()
        withExtendedLifetime(delegate) {}
    }
}

@MainActor private final class Delegate: NSObject, NSApplicationDelegate {
    var window: NSWindow!
    func applicationDidFinishLaunching(_ notification: Notification) {
        window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: 560, height: 270),
                          styleMask: [.titled, .closable], backing: .buffered, defer: false)
        window.title = "OpenDictate disposable editor fixture"
        let content = NSView(frame: NSRect(x: 0, y: 0, width: 560, height: 270))
        let label = NSTextField(labelWithString: "Synthetic fields for local Accessibility checks. No network.")
        label.frame = NSRect(x: 24, y: 220, width: 510, height: 24); content.addSubview(label)
        let activate = NSButton(title: "Activate test editor", target: self, action: #selector(activateEditor))
        activate.frame = NSRect(x: 24, y: 190, width: 210, height: 26); content.addSubview(activate)
        let first = NSTextField(string: "before old after")
        first.frame = NSRect(x: 24, y: 162, width: 510, height: 28)
        first.setAccessibilityLabel("First test field"); content.addSubview(first)
        let second = NSTextField(string: "second field")
        second.frame = NSRect(x: 24, y: 104, width: 510, height: 28)
        second.setAccessibilityLabel("Second test field"); content.addSubview(second)
        let secure = NSSecureTextField(string: "synthetic-password")
        secure.frame = NSRect(x: 24, y: 46, width: 510, height: 28)
        secure.setAccessibilityLabel("Secure test field"); content.addSubview(secure)
        window.contentView = content; window.center(); window.makeKeyAndOrderFront(nil)
        window.makeFirstResponder(first); NSApp.activate(ignoringOtherApps: true)
        let menu = NSMenu(), item = NSMenuItem(), submenu = NSMenu()
        item.submenu = submenu; menu.addItem(item)
        submenu.addItem(NSMenuItem(title: "Quit", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q"))
        let editItem = NSMenuItem(), edit = NSMenu(title: "Edit")
        editItem.submenu = edit; menu.addItem(editItem)
        edit.addItem(NSMenuItem(title: "Cut", action: #selector(NSText.cut(_:)), keyEquivalent: "x"))
        edit.addItem(NSMenuItem(title: "Copy", action: #selector(NSText.copy(_:)), keyEquivalent: "c"))
        edit.addItem(NSMenuItem(title: "Paste", action: #selector(NSText.paste(_:)), keyEquivalent: "v"))
        edit.addItem(NSMenuItem(title: "Select All", action: #selector(NSText.selectAll(_:)), keyEquivalent: "a"))
        NSApp.mainMenu = menu
    }
    @objc private func activateEditor() { NSApp.activate(ignoringOtherApps: true) }
}
