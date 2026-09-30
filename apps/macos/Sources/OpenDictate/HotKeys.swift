import AppKit
import Carbon

@MainActor
final class HotKeys {
    enum Action: UInt32 { case dictate = 1, transform, cancel }
    private var registrations = [Action: EventHotKeyRef]()
    private var handler: EventHandlerRef?
    private var pressed = Set<Action>()
    var onAction: ((Action) -> Void)?

    init() {
        var types = [EventTypeSpec(eventClass: OSType(kEventClassKeyboard), eventKind: UInt32(kEventHotKeyPressed)),
                     EventTypeSpec(eventClass: OSType(kEventClassKeyboard), eventKind: UInt32(kEventHotKeyReleased))]
        InstallEventHandler(GetApplicationEventTarget(), { _, event, context in
            guard let event, let context else { return noErr }
            var id = EventHotKeyID()
            guard GetEventParameter(event, EventParamName(kEventParamDirectObject), EventParamType(typeEventHotKeyID),
                                    nil, MemoryLayout<EventHotKeyID>.size, nil, &id) == noErr,
                  let action = Action(rawValue: id.id) else { return noErr }
            let keys = Unmanaged<HotKeys>.fromOpaque(context).takeUnretainedValue()
            MainActor.assumeIsolated {
                if GetEventKind(event) == UInt32(kEventHotKeyReleased) { keys.pressed.remove(action) }
                else if keys.pressed.insert(action).inserted { keys.onAction?(action) }
            }
            return noErr
        }, 2, &types, Unmanaged.passUnretained(self).toOpaque(), &handler)
    }

    func configure(modifiers: String, key: String) throws {
        unregister(.dictate); unregister(.transform)
        let flags = UInt32(optionKey) | (modifiers == "control-option" ? UInt32(controlKey) : 0) |
            (modifiers == "command-option" ? UInt32(cmdKey) : 0)
        let code: UInt32 = key == "d" ? 2 : (key == "r" ? 15 : 49)
        do {
            try register(.dictate, key: code, modifiers: flags)
            try register(.transform, key: code, modifiers: flags | UInt32(shiftKey))
        } catch { unregister(.dictate); unregister(.transform); throw error }
    }

    func setCancelEnabled(_ enabled: Bool) {
        unregister(.cancel)
        if enabled { try? register(.cancel, key: 53, modifiers: 0) }
    }

    func shutdown() {
        for action in [Action.dictate, .transform, .cancel] { unregister(action) }
        if let handler { RemoveEventHandler(handler) }; handler = nil
    }

    private func register(_ action: Action, key: UInt32, modifiers: UInt32) throws {
        var reference: EventHotKeyRef?
        let id = EventHotKeyID(signature: 0x4f444354, id: action.rawValue)
        guard RegisterEventHotKey(key, modifiers, id, GetApplicationEventTarget(), 0, &reference) == noErr,
              let reference else { throw DictationError.shortcut }
        registrations[action] = reference
    }
    private func unregister(_ action: Action) {
        pressed.remove(action)
        if let reference = registrations.removeValue(forKey: action) { UnregisterEventHotKey(reference) }
    }
}
