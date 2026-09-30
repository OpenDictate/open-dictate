import Foundation
import Combine
import Security
import OpenDictateCore

@MainActor
final class Preferences: ObservableObject {
    private let defaults: UserDefaults
    @Published var mode: DictationMode { didSet { defaults.set(mode.rawValue, forKey: "mode") } }
    @Published var speechLanguage: String { didSet { defaults.set(speechLanguage, forKey: "speechLanguage") } }
    @Published var interfaceLanguage: String { didSet { defaults.set(interfaceLanguage, forKey: "interfaceLanguage") } }
    @Published var dictionary: String { didSet { defaults.set(dictionary, forKey: "dictionary") } }
    @Published var keepTrailingPeriod: Bool { didSet { defaults.set(keepTrailingPeriod, forKey: "keepTrailingPeriod") } }
    @Published var saveHistory: Bool { didSet { defaults.set(saveHistory, forKey: "saveHistory") } }
    @Published var shortcutModifiers: String { didSet { defaults.set(shortcutModifiers, forKey: "shortcutModifiers") } }
    @Published var shortcutKey: String { didSet { defaults.set(shortcutKey, forKey: "shortcutKey") } }
    @Published var textModel: String { didSet { defaults.set(textModel, forKey: "textModel") } }
    @Published var timeout: Int { didSet { defaults.set(timeout, forKey: "timeout") } }
    @Published var excludedApps: [String] { didSet { defaults.set(excludedApps, forKey: "excludedApps") } }
    @Published var showStatus: Bool { didSet { defaults.set(showStatus, forKey: "showStatus") } }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        mode = DictationMode(rawValue: defaults.string(forKey: "mode") ?? "") ?? .live
        speechLanguage = defaults.string(forKey: "speechLanguage") ?? "auto"
        interfaceLanguage = defaults.string(forKey: "interfaceLanguage") ?? "auto"
        dictionary = defaults.string(forKey: "dictionary") ?? "OpenDictate"
        keepTrailingPeriod = defaults.object(forKey: "keepTrailingPeriod") as? Bool ?? true
        saveHistory = defaults.object(forKey: "saveHistory") as? Bool ?? true
        shortcutModifiers = defaults.string(forKey: "shortcutModifiers") ?? "option"
        shortcutKey = defaults.string(forKey: "shortcutKey") ?? "space"
        textModel = defaults.string(forKey: "textModel") ?? "gpt-6-luna"
        timeout = [30, 60, 120].contains(defaults.integer(forKey: "timeout")) ? defaults.integer(forKey: "timeout") : 60
        excludedApps = defaults.stringArray(forKey: "excludedApps") ?? []
        showStatus = defaults.object(forKey: "showStatus") as? Bool ?? true
    }

    var isRussian: Bool {
        interfaceLanguage == "ru" || (interfaceLanguage == "auto" && Locale.preferredLanguages.first?.hasPrefix("ru") == true)
    }
    func t(_ english: String, _ russian: String) -> String { isRussian ? russian : english }
    var context: TranscriptionContext {
        let languages = speechLanguage == "ru-en" ? ["ru", "en"] : (speechLanguage == "auto" ? [] : [speechLanguage])
        return TranscriptionContext(languages: languages, dictionary: DictionaryTerms.normalize(dictionary))
    }
    var shortcutLabel: String {
        let modifiers = shortcutModifiers == "control-option" ? "⌃⌥" : (shortcutModifiers == "command-option" ? "⌥⌘" : "⌥")
        return modifiers + (shortcutKey == "space" ? "Space" : shortcutKey.uppercased())
    }
    var editShortcutLabel: String { "⇧" + shortcutLabel }
}

enum APIKeyStore {
    private static var query: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: "com.opendictate.mac",
         kSecAttrAccount as String: "openai-api-key", kSecAttrSynchronizable as String: false]
    }
    static func load() throws -> String? {
        var query = query
        query[kSecReturnData as String] = true; query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess, let data = result as? Data else { throw DictationError.keychain }
        return String(data: data, encoding: .utf8)
    }
    static func save(_ key: String) throws {
        let value = key.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty, !value.contains(where: { $0.isWhitespace }) else { throw DictationError.missingKey }
        let attributes: [String: Any] = [kSecValueData as String: Data(value.utf8),
                                       kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly]
        var status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            var item = query; item.merge(attributes) { _, new in new }
            status = SecItemAdd(item as CFDictionary, nil)
        }
        guard status == errSecSuccess else { throw DictationError.keychain }
    }
    static func delete() throws {
        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else { throw DictationError.keychain }
    }
}

@MainActor
final class HistoryStore: ObservableObject {
    @Published private(set) var entries = [HistoryEntry]()
    @Published private(set) var error: String?
    private let url: URL?
    init(inMemory: Bool = false) {
        if inMemory { url = nil; return }
        let folder = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("OpenDictate", isDirectory: true)
        let historyURL = folder.appendingPathComponent("history.json")
        url = historyURL
        do {
            try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true,
                                                    attributes: [.posixPermissions: 0o700])
            if FileManager.default.fileExists(atPath: historyURL.path) {
                entries = try JSONDecoder().decode([HistoryEntry].self, from: Data(contentsOf: historyURL))
            }
        } catch { self.error = DictationError.storage.localizedDescription }
    }
    func add(_ text: String, mode: String) {
        guard !text.isEmpty else { return }
        entries.insert(HistoryEntry(text: text, mode: mode), at: 0)
        entries = Array(entries.prefix(500)); persist()
    }
    func delete(_ id: UUID) { entries.removeAll { $0.id == id }; persist() }
    func clear() { entries.removeAll(); persist() }
    private func persist() {
        guard let url else { return }
        do {
            try JSONEncoder().encode(entries).write(to: url, options: [.atomic])
            try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: url.path)
            error = nil
        } catch { self.error = DictationError.storage.localizedDescription }
    }
}
