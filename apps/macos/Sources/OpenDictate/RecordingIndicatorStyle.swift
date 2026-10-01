import Combine
import OpenDictateCore

enum RecordingIndicatorStyle: String, CaseIterable {
    case compact, waveform

    func title(russian: Bool) -> String {
        switch self {
        case .compact: return russian ? "Микрофон" : "Microphone"
        case .waveform: return russian ? "Полоски" : "Waveform"
        }
    }
}

/// Only the HUD observes microphone levels, so audio updates never rebuild menu state.
@MainActor
final class RecordingAudioLevels: ObservableObject {
    @Published private(set) var history = AudioLevelHistory()

    func append(_ level: Float) { history.append(level) }
    func reset() { history.reset() }
}
