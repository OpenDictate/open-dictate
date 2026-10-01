#if DEBUG
import AppKit
import OpenDictateCore

/// Explicit development-only checks. No credentials, provider calls or audio files.
@MainActor
final class LocalSmokeChecks {
    private var target: TextTarget?
    private var replacements: WordReplacementEngine?
    private let model: AppModel
    init(model: AppModel) { self.model = model }

    func toggle() async {
        if let target {
            let accepted = await target.apply(replacements?.apply("Проверка 🙂") ?? "Проверка 🙂")
            model.present(CheckMessage(accepted ? "LOCAL CHECK: cumulative final inserted" : "LOCAL CHECK: focus guard rejected final"))
            self.target = nil; replacements = nil; model.hotKeys.setCancelEnabled(false)
        } else {
            do {
                let target = try TextTarget.capture(exclusions: [])
                target.preserveOriginal()
                let replacements = model.replacements.engine()
                guard await target.apply(replacements.apply("Проверка", final: false)) else { throw DictationError.noField }
                self.target = target; self.replacements = replacements
                model.hotKeys.setCancelEnabled(true)
                model.present(CheckMessage("LOCAL CHECK: partial inserted; press shortcut again or Escape"))
            } catch { model.present(error) }
        }
    }
    func cancel() async {
        await target?.restore(); target = nil; replacements = nil; model.hotKeys.setCancelEnabled(false)
        model.present(CheckMessage("LOCAL CHECK: cancellation restored unchanged field"))
    }
    func verifyInsertionAndRollback() async {
        do {
            let target = try TextTarget.capture(exclusions: [])
            let original = target.snapshot
            let replacements = model.replacements.engine()
            target.preserveOriginal()
            guard await target.apply(replacements.apply("Проверка", final: false)) else { throw CheckMessage("LOCAL CHECK: partial delivery rejected") }
            try await Task.sleep(nanoseconds: 100_000_000)
            guard await target.apply(replacements.apply("Проверка 🙂")) else { throw CheckMessage("LOCAL CHECK: cumulative delivery rejected") }
            try await Task.sleep(nanoseconds: 100_000_000)
            await target.restore()
            try await Task.sleep(nanoseconds: 100_000_000)
            let restored = try TextTarget.capture(exclusions: []).snapshot
            guard restored == original else { throw CheckMessage("LOCAL CHECK: original selection was not restored") }
            model.present(CheckMessage("LOCAL CHECK: cumulative insertion and cancellation verified; original restored"))
        } catch { model.present(error) }
    }
    func record() {
        Task { [model] in
            let recorder = AudioRecorder()
            do {
                model.audioLevels.reset()
                try await recorder.start(keepAudio: true, onChunk: { _ in }, onLevel: { [weak model] level in
                    Task { @MainActor in model?.audioLevels.append(level) }
                }, onError: { _ in })
                try await Task.sleep(nanoseconds: 2_000_000_000)
                let (data, bytes) = await recorder.stop()
                model.present(CheckMessage(bytes > 48_000 && data.count == bytes
                    ? "LOCAL CHECK: microphone captured 24 kHz PCM; discarded in memory"
                    : "LOCAL CHECK: microphone check failed"))
            } catch { _ = await recorder.stop(); model.present(error) }
            model.audioLevels.reset()
        }
    }
    private struct CheckMessage: LocalizedError {
        let errorDescription: String?
        init(_ message: String) { errorDescription = message }
    }
}
#endif
