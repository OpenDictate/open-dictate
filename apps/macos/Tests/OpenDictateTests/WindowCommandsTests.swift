import AppKit
import XCTest
@testable import OpenDictate

final class WindowCommandsTests: XCTestCase {
    @MainActor func testCommandWMenuActionClosesWindowAndAllowsReopening() async throws {
        let application = NSApplication.shared
        let delegate = AppDelegate()
        let (preferences, defaults, suite) = makePreferences()
        let originalMenu = application.mainMenu
        let window = makeWindow()
        defer {
            window.close()
            application.mainMenu = originalMenu
            defaults.removePersistentDomain(forName: suite)
        }
        let menu = delegate.makeMainMenu(preferences: preferences)
        application.mainMenu = menu
        window.makeKeyAndOrderFront(nil)
        let close = try XCTUnwrap(command(in: menu, modifiers: [.command]))
        XCTAssertEqual(close.action, #selector(NSWindow.performClose(_:)))
        XCTAssertNil(close.target)
        // XCTest has no active app event loop/key window. Dispatch the real menu
        // selector through the window responder; physical keys are checked in the smoke app.
        XCTAssertTrue(window.tryToPerform(try XCTUnwrap(close.action), with: close))
        XCTAssertFalse(window.isVisible)
        window.makeKeyAndOrderFront(nil)
        XCTAssertTrue(window.isVisible)
    }

    @MainActor func testCommandShiftWClosesAllWindowsAndPreservesRecordingHUD() async throws {
        let application = NSApplication.shared
        let delegate = AppDelegate()
        let (preferences, defaults, suite) = makePreferences()
        preferences.interfaceLanguage = "ru"
        let originalMenu = application.mainMenu
        let first = makeWindow(), second = makeWindow()
        let indicator = RecordingIndicator()
        defer {
            first.close(); second.close(); indicator.hide()
            application.mainMenu = originalMenu
            defaults.removePersistentDomain(forName: suite)
        }
        let menu = delegate.makeMainMenu(preferences: preferences)
        application.mainMenu = menu
        first.orderFront(nil); second.makeKeyAndOrderFront(nil)
        indicator.show(phase: .recording, style: .waveform, audioLevels: RecordingAudioLevels(),
                       isRussian: true, finish: {})
        let panel = try XCTUnwrap(application.windows.first {
            $0.title == "OpenDictate recording indicator" && $0.isVisible
        })
        let closeAll = try XCTUnwrap(command(in: menu, modifiers: [.command, .shift]))
        XCTAssertEqual(closeAll.title, "Закрыть все окна")
        XCTAssertTrue(application.sendAction(try XCTUnwrap(closeAll.action), to: closeAll.target, from: closeAll))
        XCTAssertFalse(first.isVisible)
        XCTAssertFalse(second.isVisible)
        XCTAssertTrue(panel.isVisible)
        XCTAssertFalse(panel.canBecomeKey)
    }

    @MainActor private func makePreferences() -> (Preferences, UserDefaults, String) {
        let suite = "com.opendictate.tests.window-commands.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        return (Preferences(defaults: defaults), defaults, suite)
    }

    @MainActor private func makeWindow() -> NSWindow {
        let window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: 200, height: 100),
                              styleMask: [.titled, .closable], backing: .buffered, defer: false)
        window.isReleasedWhenClosed = false
        return window
    }

    @MainActor private func command(in menu: NSMenu, modifiers: NSEvent.ModifierFlags) -> NSMenuItem? {
        for item in menu.items {
            if item.keyEquivalent == "w" && item.keyEquivalentModifierMask == modifiers { return item }
            if let submenu = item.submenu, let match = command(in: submenu, modifiers: modifiers) { return match }
        }
        return nil
    }
}
