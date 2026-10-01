import Foundation

public struct WordReplacement: Codable, Equatable, Identifiable, Sendable {
    public var id: String
    public var source: String
    public var replacement: String
    public var enabled: Bool

    public init(id: String = UUID().uuidString.lowercased(), source: String, replacement: String,
                enabled: Bool = true) {
        self.id = id; self.source = source; self.replacement = replacement
        self.enabled = enabled
    }
    public var isValid: Bool {
        UUID(uuidString: id) != nil && id == id.lowercased() &&
        !source.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && source.utf16.count <= 256 &&
        !replacement.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && replacement.utf16.count <= 2048
    }
}

public struct ReplacementDocument: Codable, Equatable, Sendable {
    public var schemaVersion = 1
    public var rules: [WordReplacement]
    public init(rules: [WordReplacement] = []) { self.rules = rules }
    public var isValid: Bool {
        schemaVersion == 1 && rules.count <= 500 && rules.allSatisfy(\.isValid) &&
        Set(rules.map(\.id)).count == rules.count
    }

}

/// Compiled once per session. Matches the original transcript once; replacements never cascade.
public struct WordReplacementEngine: @unchecked Sendable {
    private let rules: [WordReplacement]
    private let regex: NSRegularExpression?
    public init(rules: [WordReplacement]) {
        self.rules = rules.filter { $0.isValid && $0.enabled }.sorted {
            if $0.source.utf16.count != $1.source.utf16.count { return $0.source.utf16.count > $1.source.utf16.count }
            return $0.id < $1.id
        }
        let alternatives = self.rules.map { "(" + NSRegularExpression.escapedPattern(for: $0.source.precomposedStringWithCanonicalMapping) + ")" }.joined(separator: "|")
        regex = alternatives.isEmpty ? nil : try? NSRegularExpression(pattern: "(?<![\\p{L}\\p{M}\\p{N}_])(?:" + alternatives + ")(?![\\p{L}\\p{M}\\p{N}_])", options: [.caseInsensitive])
    }
    public func apply(_ text: String, final: Bool = true) -> String {
        guard let regex else { return text }
        let normalized = text.precomposedStringWithCanonicalMapping
        let value = normalized as NSString
        var result = "", cursor = 0
        for match in regex.matches(in: normalized, range: NSRange(location: 0, length: value.length)) {
            // A live trailing token may still grow (e.g. cat → catalog).
            if !final && NSMaxRange(match.range) == value.length { continue }
            guard let index = rules.indices.first(where: { match.range(at: $0 + 1).location != NSNotFound }) else { continue }
            result += value.substring(with: NSRange(location: cursor, length: match.range.location - cursor))
            result += rules[index].replacement
            cursor = NSMaxRange(match.range)
        }
        return result + value.substring(from: cursor)
    }
}
