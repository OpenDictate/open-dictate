import AppKit
import SwiftUI

/// Keeps recording controls above the editor without activating or focusing the app.
@MainActor
final class RecordingIndicator {
    private let panel: NSPanel
    private let content: NSHostingView<RecordingStatusView>

    init() {
        panel = IndicatorPanel(contentRect: .zero, styleMask: [.borderless, .nonactivatingPanel],
                               backing: .buffered, defer: false)
        content = NSHostingView(rootView: RecordingStatusView(phase: .idle, isRussian: false, finish: {}))
        panel.title = "OpenDictate recording indicator"
        panel.level = .statusBar
        panel.isOpaque = false
        panel.backgroundColor = .clear
        panel.hasShadow = true
        panel.hidesOnDeactivate = false
        panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
        panel.contentView = content
    }

    func show(phase: AppModel.Phase, isRussian: Bool, finish: @escaping () -> Void) {
        guard phase != .idle else { hide(); return }
        content.rootView = RecordingStatusView(phase: phase, isRussian: isRussian, finish: finish)
        let size = NSSize(width: phase.showsFinishControl ? 44 : 22, height: 22)
        content.sizingOptions = []
        panel.setContentSize(size)
        panel.ignoresMouseEvents = !phase.showsFinishControl
        let screen = NSScreen.screens.first { NSMouseInRect(NSEvent.mouseLocation, $0.frame, false) } ?? NSScreen.main
        if let screen {
            panel.setFrameOrigin(NSPoint(x: screen.visibleFrame.midX - size.width / 2,
                                        y: screen.visibleFrame.minY + 28))
        }
        panel.orderFrontRegardless()
    }

    func hide() { panel.orderOut(nil) }
}

private final class IndicatorPanel: NSPanel {
    override var canBecomeKey: Bool { false }
    override var canBecomeMain: Bool { false }
}

struct RecordingStatusView: View {
    let phase: AppModel.Phase
    let isRussian: Bool
    let finish: () -> Void

    var body: some View {
        Group {
            if phase.showsFinishControl {
                HStack(spacing: 6) {
                    Image(systemName: "mic.fill")
                        .font(.system(size: 9, weight: .medium)).frame(width: 10)
                        .accessibilityHidden(true)
                    Button(action: finish) {
                        ZStack {
                            Circle().fill(Color.red).frame(width: 14, height: 14)
                            RoundedRectangle(cornerRadius: 1).fill(Color.white).frame(width: 4.5, height: 4.5)
                        }.frame(width: 16, height: 16).contentShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .help(isRussian ? "Завершить и отправить" : "Finish and submit")
                    .accessibilityLabel(isRussian ? "Завершить диктовку" : "Finish dictation")
                    .accessibilityIdentifier("finishDictation")
                }
                .fixedSize()
                .padding(.horizontal, 6)
                .frame(height: 22)
            } else {
                ProgressView().progressViewStyle(.circular).controlSize(.mini)
                    .frame(width: 22, height: 22)
                    .accessibilityLabel(phase == .preparing
                        ? (isRussian ? "Запуск диктовки" : "Starting dictation")
                        : (isRussian ? "Распознавание" : "Transcribing"))
            }
        }
        .background(Color(nsColor: .windowBackgroundColor), in: Capsule())
        .preferredColorScheme(.dark)
    }
}
