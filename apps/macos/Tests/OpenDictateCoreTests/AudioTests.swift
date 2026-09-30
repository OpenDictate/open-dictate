import XCTest
@testable import OpenDictateCore

final class AudioTests: XCTestCase {
    func testResampledBuffersBecome40MillisecondChunksWithoutLosingTail() {
        var chunker = PCMChunker()
        let input = Data((0..<5000).map { UInt8($0 % 251) })
        var chunks = chunker.append(input.prefix(1200))
        XCTAssertTrue(chunks.isEmpty)
        chunks += chunker.append(input.dropFirst(1200))
        XCTAssertEqual(chunks.map(\.count), [1920, 1920])
        let tail = chunker.finish()
        XCTAssertEqual(tail.count, 1160)
        XCTAssertEqual(chunks.reduce(Data(), +) + tail, input)
        XCTAssertTrue(chunker.finish().isEmpty)
    }
    func testWavContainsCorrectPCMFormatAndSizes() {
        let pcm = Data([0x00, 0x80, 0xff, 0x7f])
        let wav = WAV.encode(pcm: pcm)
        func u32(_ offset: Int) -> UInt32 { wav[offset..<offset + 4].enumerated().reduce(0) { $0 | UInt32($1.element) << ($1.offset * 8) } }
        XCTAssertEqual(String(decoding: wav[0..<4], as: UTF8.self), "RIFF")
        XCTAssertEqual(u32(4), 40)
        XCTAssertEqual(String(decoding: wav[8..<16], as: UTF8.self), "WAVEfmt ")
        XCTAssertEqual(wav[20], 1); XCTAssertEqual(wav[22], 1)
        XCTAssertEqual(u32(24), 24000); XCTAssertEqual(u32(28), 48000)
        XCTAssertEqual(wav[32], 2); XCTAssertEqual(wav[34], 16)
        XCTAssertEqual(u32(40), 4); XCTAssertEqual(wav.suffix(4), pcm)
    }
}
