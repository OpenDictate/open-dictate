import Foundation

public enum AudioConstants {
    public static let sampleRate = 24_000
    public static let chunkBytes = 1_920 // mono PCM16, 40 ms; same as Android
    public static let maximumRecordingBytes = 23_000_000 // safely below the 25 MB upload limit
    public static let maximumQueuedChunks = 300
}

public struct PCMChunker: Sendable {
    private var pending = Data()
    public init() {}
    public mutating func append(_ data: Data) -> [Data] {
        pending.append(data)
        var chunks = [Data]()
        while pending.count >= AudioConstants.chunkBytes {
            chunks.append(Data(pending.prefix(AudioConstants.chunkBytes)))
            pending.removeFirst(AudioConstants.chunkBytes)
        }
        return chunks
    }
    public mutating func finish() -> Data {
        defer { pending.removeAll() }
        return pending
    }
}

public enum WAV {
    public static func encode(pcm: Data) -> Data {
        precondition(pcm.count % 2 == 0 && pcm.count <= AudioConstants.maximumRecordingBytes)
        var result = Data()
        func ascii(_ value: String) { result.append(contentsOf: value.utf8) }
        func u32(_ value: UInt32) { var v = value.littleEndian; withUnsafeBytes(of: &v) { result.append(contentsOf: $0) } }
        func u16(_ value: UInt16) { var v = value.littleEndian; withUnsafeBytes(of: &v) { result.append(contentsOf: $0) } }
        ascii("RIFF"); u32(UInt32(pcm.count + 36)); ascii("WAVEfmt ")
        u32(16); u16(1); u16(1); u32(24_000); u32(48_000); u16(2); u16(16)
        ascii("data"); u32(UInt32(pcm.count)); result.append(pcm)
        return result
    }
}
