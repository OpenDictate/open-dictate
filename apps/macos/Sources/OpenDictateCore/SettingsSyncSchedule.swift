/// Serializes sync requests and batches edits after three seconds without changes.
/// The owner supplies monotonic milliseconds, making scheduling independent of wall-clock changes.
public struct SettingsSyncSchedule {
    private var changeDueAt: Int64?
    private var refreshRequested = false
    public private(set) var busy = false
    public var hasPendingChange: Bool { changeDueAt != nil }
    public init() {}

    public mutating func changed(now: Int64) { changeDueAt = now + 3_000 }
    public mutating func refresh() { refreshRequested = true }
    public func delayUntilReady(now: Int64) -> Int64? { changeDueAt.map { max(0, $0 - now) } }

    public mutating func begin(now: Int64) -> Bool {
        guard !busy, changeDueAt.map({ $0 <= now }) ?? true else { return false }
        guard refreshRequested || changeDueAt != nil else { return false }
        changeDueAt = nil; refreshRequested = false; busy = true
        return true
    }

    public mutating func finish() { busy = false }
}
