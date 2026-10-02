import AppKit

enum EjectError: Error { case outsideInstaller, handoffFailed }

func installerVolume(containing bundle: URL) throws -> URL {
    let process = Process()
    let output = Pipe()
    process.executableURL = URL(fileURLWithPath: "/usr/bin/hdiutil")
    process.arguments = ["info", "-plist"]
    process.standardOutput = output
    process.standardError = FileHandle.nullDevice
    try process.run()
    let data = output.fileHandleForReading.readDataToEndOfFile()
    process.waitUntilExit()
    guard process.terminationStatus == 0,
          let volume = try PropertyListDecoder().decode(DiskImages.self, from: data)
            .installerVolume(containing: bundle.resolvingSymlinksInPath())
    else { throw EjectError.outsideInstaller }
    return volume
}

func finishInstallation() throws {
    let files = FileManager.default
    let arguments = CommandLine.arguments
    if arguments.count == 4, arguments[1] == "--finish", let parent = Int32(arguments[3]) {
        // The worker runs from a temporary copy so its executable cannot hold the DMG busy.
        let temporary = Bundle.main.bundleURL.deletingLastPathComponent()
        guard temporary.deletingLastPathComponent().resolvingSymlinksInPath().path
                == files.temporaryDirectory.resolvingSymlinksInPath().path,
              temporary.lastPathComponent.hasPrefix("OpenDictateEject-")
        else { throw EjectError.handoffFailed }
        defer { try? files.removeItem(at: temporary) }
        // Wait for the original helper to release the volume before requesting a normal eject.
        for _ in 0..<250 {
            if kill(parent, 0) != 0 { break }
            Thread.sleep(forTimeInterval: 0.02)
        }
        guard kill(parent, 0) != 0 else { throw EjectError.handoffFailed }
        let source = URL(fileURLWithPath: arguments[2], isDirectory: true)
        let volume = try installerVolume(containing: source)
        try NSWorkspace.shared.unmountAndEjectDevice(at: volume)
    } else {
        let source = Bundle.main.bundleURL
        _ = try installerVolume(containing: source)
        let temporary = files.temporaryDirectory.appendingPathComponent("OpenDictateEject-\(UUID().uuidString)")
        try files.createDirectory(at: temporary, withIntermediateDirectories: false)
        do {
            let copy = temporary.appendingPathComponent("Eject.app")
            try files.copyItem(at: source, to: copy)
            let worker = Process()
            worker.executableURL = copy.appendingPathComponent("Contents/MacOS/OpenDictateEject")
            worker.arguments = ["--finish", source.path, String(ProcessInfo.processInfo.processIdentifier)]
            worker.currentDirectoryURL = files.temporaryDirectory
            try worker.run()
        } catch {
            try? files.removeItem(at: temporary)
            throw error
        }
    }
}

let application = NSApplication.shared
application.setActivationPolicy(.accessory)
DispatchQueue.global(qos: .userInitiated).async {
    do {
        try finishInstallation()
        DispatchQueue.main.async { application.terminate(nil) }
    } catch {
        DispatchQueue.main.async {
            let russian = Locale.preferredLanguages.first?.hasPrefix("ru") == true
            let alert = NSAlert()
            alert.messageText = russian ? "Не удалось извлечь образ" : "Couldn’t eject the disk image"
            alert.informativeText = error is EjectError
                ? (russian ? "Откройте Eject внутри примонтированного образа OpenDictate."
                           : "Open Eject inside the mounted OpenDictate disk image.")
                : (russian ? "Закройте приложения и файлы, открытые с этого образа, и запустите Eject ещё раз. Запускайте OpenDictate из папки «Программы»."
                           : "Close apps and files opened from this disk image, then open Eject again. Run OpenDictate from Applications.")
            alert.addButton(withTitle: "OK")
            application.activate(ignoringOtherApps: true)
            alert.runModal()
            application.terminate(nil)
        }
    }
}
application.run()
