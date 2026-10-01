import Foundation

/// A short, in-memory envelope of input levels, ordered from oldest to newest.
public struct AudioLevelHistory: Equatable, Sendable {
    public static let count = 10
    public private(set) var samples = Array(repeating: Float.zero, count: count)

    public init() {}

    public mutating func append(_ level: Float) {
        samples.removeFirst()
        samples.append(level.isFinite ? min(1, max(0, level)) : 0)
    }

    public mutating func reset() { self = Self() }
}
