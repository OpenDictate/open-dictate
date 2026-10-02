import AppKit
import ApplicationServices

/// Enables an application's lazy Accessibility tree without inspecting its contents.
@MainActor
struct ApplicationAccessibility {
    private let read: (String) -> CFTypeRef?
    private let enable: (String) -> Void

    init(read: @escaping (String) -> CFTypeRef?, enable: @escaping (String) -> Void) {
        self.read = read; self.enable = enable
    }

    init(pid: pid_t) {
        let application = AXUIElementCreateApplication(pid)
        AXUIElementSetMessagingTimeout(application, 0.15)
        read = { attribute in
            var result: CFTypeRef?
            guard AXUIElementCopyAttributeValue(application, attribute as CFString, &result) == .success else { return nil }
            return result
        }
        enable = { attribute in
            // Some Electron versions return notImplemented even though
            // the flag was applied. The focused-element read remains authoritative.
            _ = AXUIElementSetAttributeValue(application, attribute as CFString, kCFBooleanTrue)
        }
    }

    func prepare() {
        // Electron documents AXManualAccessibility. Its Chromium tree can also
        // require AXEnhancedUserInterface (including T3 Code). Probe support
        // instead of keeping an application allowlist or resetting existing flags.
        for attribute in ["AXManualAccessibility", "AXEnhancedUserInterface"] {
            guard let value = read(attribute), CFGetTypeID(value) == CFBooleanGetTypeID(),
                  !CFBooleanGetValue(unsafeBitCast(value, to: CFBoolean.self)) else { continue }
            enable(attribute)
        }
    }

    func focusedElement() -> AXUIElement? {
        guard let result = read(kAXFocusedUIElementAttribute),
              CFGetTypeID(result) == AXUIElementGetTypeID() else { return nil }
        let element = unsafeBitCast(result, to: AXUIElement.self)
        AXUIElementSetMessagingTimeout(element, 0.15)
        return element
    }

    static func prepareFrontmost(exclusions: [String]) {
        guard AXIsProcessTrusted(), let app = NSWorkspace.shared.frontmostApplication,
              app.processIdentifier != ProcessInfo.processInfo.processIdentifier,
              !exclusions.contains(app.bundleIdentifier ?? "") else { return }
        ApplicationAccessibility(pid: app.processIdentifier).prepare()
    }
}
