import Foundation

/// Wire format shared with Android. New sections add keys without replacing old clients' data.
public struct SettingsSyncDocument: Codable, Equatable, Sendable {
    public struct Entry: Codable, Equatable, Sendable {
        public var value: String
        public var modifiedAt: Int64
        public var deviceId: String
        public init(value: String, modifiedAt: Int64, deviceId: String) {
            self.value = value; self.modifiedAt = modifiedAt; self.deviceId = deviceId
        }
    }
    public var schemaVersion = 1
    public var entries: [String: Entry] = [:]
    public init() {}
    public static let keys = ["dictionary", "mode", "liveModel", "accurateModel", "textModel", "wordReplacements", "wordReplacementsEnabled", "accuratePunctuationEnabled"]
    public static let maximumBytes = 1_048_576

    public static func decode(_ data: Data) throws -> Self {
        guard data.count <= maximumBytes else { throw SyncFormatError.invalid }
        let document = try JSONDecoder().decode(Self.self, from: data)
        guard document.schemaVersion == 1, document.entries.count <= 1024,
              document.entries.allSatisfy({ $0.key.utf8.count <= 128 && $0.value.value.utf8.count <= 262_144 &&
                  $0.value.modifiedAt >= 0 && $0.value.modifiedAt < Int64.max - 1 && $0.value.deviceId.utf8.count <= 128 })
        else { throw SyncFormatError.invalid }
        return document
    }

    public mutating func merge(_ other: Self) {
        for (key, incoming) in other.entries {
            guard let existing = entries[key] else { entries[key] = incoming; continue }
            if incoming.modifiedAt > existing.modifiedAt ||
                (incoming.modifiedAt == existing.modifiedAt && (incoming.deviceId > existing.deviceId ||
                    (incoming.deviceId == existing.deviceId && incoming.value > existing.value))) {
                entries[key] = incoming
            }
        }
    }

    public mutating func record(_ values: [String: String], deviceId: String, now: Int64, seed: Bool = false) {
        let clock = max(now, (entries.values.map(\.modifiedAt).max() ?? 0) + 1)
        for key in Self.keys {
            guard let value = values[key], entries[key]?.value != value else { continue }
            entries[key] = Entry(value: value, modifiedAt: seed ? 0 : clock, deviceId: seed ? "" : deviceId)
        }
    }

    /// Defaults on a newly linked device must not supersede an existing cloud preference.
    public mutating func promoteSeeds(deviceId: String, now: Int64) {
        let clock = max(now, (entries.values.map(\.modifiedAt).max() ?? 0) + 1)
        for key in Self.keys where entries[key]?.modifiedAt == 0 {
            entries[key]?.modifiedAt = clock; entries[key]?.deviceId = deviceId
        }
    }
}

public enum SyncFormatError: Error { case invalid }
