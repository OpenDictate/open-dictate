import AppKit
import AVFoundation
import Combine
import ServiceManagement
import OpenDictateCore

@MainActor
final class AppModel: ObservableObject {
    enum Phase {
        case idle, preparing, recording, processing
        var showsFinishControl: Bool { self == .preparing || self == .recording }
    }
    @Published private(set) var phase = Phase.idle
    let audioLevels = RecordingAudioLevels()
    @Published private(set) var elapsed = 0
    @Published private(set) var partial = ""
    @Published private(set) var lastTranscript = ""
    @Published private(set) var message = ""
    @Published private(set) var hasKey = false
    @Published private(set) var microphoneAllowed = false
    @Published private(set) var accessibilityAllowed = false
    @Published private(set) var shortcutError = ""
    @Published var isSearching = false
    @Published var searchMatches: [UUID]?
    @Published var loginEnabled = false
    let preferences: Preferences
    let history: HistoryStore
    let replacements: ReplacementStore
    let driveSync: GoogleDriveSync
    private let localOnly: Bool
    let hotKeys = HotKeys()
    private let client = OpenAIClient()
    private var current: Session?
    private var timer: Timer?
    private var subscriptions = Set<AnyCancellable>()
    private var searchTask: Task<Void, Never>?
    private var pasteTask: Task<Void, Never>?
    private var searchID: UUID?
    private var recordingShortcut = false
    var showSettings: (() -> Void)?
    var onStateChange: (() -> Void)?

    @MainActor private final class Session {
        let id = UUID()
        let target: TextTarget
        let recorder = AudioRecorder()
        let mode: DictationMode
        let transform: Bool
        let key: String
        let context: TranscriptionContext
        let textModel: String
        let keepTrailingPeriod: Bool
        let timeout: Int
        let replacements: WordReplacementEngine
        let started = Date()
        var acceptsPartials = true
        var cancelling = false
        var detached = false
        var stopRequested = false
        var live: LiveTranscriptionSession?
        var continuation: AsyncStream<Data>.Continuation?
        var pump: Task<Void, Never>?
        var setup: Task<Void, Never>?
        var completion: Task<Void, Never>?
        init(target: TextTarget, transform: Bool, key: String, preferences: Preferences, replacements: WordReplacementEngine) {
            self.replacements = replacements
            self.target = target; self.transform = transform; self.key = key
            mode = transform ? .accurate : preferences.mode
            context = preferences.context; textModel = preferences.textModel
            keepTrailingPeriod = preferences.keepTrailingPeriod; timeout = preferences.timeout
        }
    }

    init(localOnly: Bool = false) {
        self.localOnly = localOnly
        replacements = ReplacementStore(defaults: localOnly ? UserDefaults(suiteName: "com.opendictate.mac.local-tests")! : .standard)
        preferences = Preferences(defaults: localOnly ? UserDefaults(suiteName: "com.opendictate.mac.local-tests")! : .standard, replacements: replacements)
        history = HistoryStore(inMemory: localOnly)
        driveSync = GoogleDriveSync(preferences: preferences, localOnly: localOnly)
        refreshPermissions()
        loginEnabled = SMAppService.mainApp.status == .enabled
        hotKeys.onAction = { [weak self] action in
            switch action {
            case .dictate: self?.toggle(transform: false)
            case .transform: self?.toggle(transform: true)
            case .cancel: self?.cancel()
            }
        }
        configureHotKeys()
        preferences.$shortcuts.dropFirst().sink { [weak self] _ in
            DispatchQueue.main.async { self?.configureHotKeys() }
        }.store(in: &subscriptions)
        preferences.objectWillChange.sink { [weak self] in
            self?.objectWillChange.send()
            DispatchQueue.main.async { self?.onStateChange?() }
        }.store(in: &subscriptions)
        timer = Timer.scheduledTimer(withTimeInterval: 0.2, repeats: true) { [weak self] _ in
            MainActor.assumeIsolated { self?.tick() }
        }
        NSWorkspace.shared.notificationCenter.addObserver(forName: NSWorkspace.willSleepNotification,
                                                         object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.cancel() }
        }
    }

    var isActive: Bool { phase != .idle }
    var stateLabel: String {
        switch phase {
        case .idle: return preferences.t("Ready", "Готово")
        case .preparing: return preferences.t("Starting…", "Запуск…")
        case .recording: return current?.transform == true ? preferences.t("Listening to your edit", "Слушаю команду") : preferences.t("Listening", "Слушаю")
        case .processing: return preferences.t("Finishing…", "Обработка…")
        }
    }
    var targetName: String { current?.target.applicationName ?? "" }
    var detached: Bool { current?.detached ?? false }

    func refreshPermissions() {
        microphoneAllowed = AVCaptureDevice.authorizationStatus(for: .audio) == .authorized
        accessibilityAllowed = AXIsProcessTrusted()
        guard !localOnly else { hasKey = false; return }
        do { hasKey = try APIKeyStore.load()?.isEmpty == false }
        catch { present(error) }
    }
    func requestMicrophone() {
        Task {
            if AVCaptureDevice.authorizationStatus(for: .audio) == .notDetermined {
                _ = await AVCaptureDevice.requestAccess(for: .audio)
            } else if !microphoneAllowed { openPrivacy("Privacy_Microphone") }
            refreshPermissions()
        }
    }
    func requestAccessibility() {
        AXIsProcessTrustedWithOptions([kAXTrustedCheckOptionPrompt.takeUnretainedValue() as String: true] as CFDictionary)
        openPrivacy("Privacy_Accessibility")
    }
    private func openPrivacy(_ anchor: String) {
        if let url = URL(string: "x-apple.systempreferences:com.apple.preference.security?\(anchor)") {
            NSWorkspace.shared.open(url)
        }
    }
    @discardableResult func saveKey(_ key: String) -> Bool {
        guard !localOnly else { return false }
        do {
            try APIKeyStore.save(key); hasKey = true
            message = preferences.t("API key saved in Keychain.", "Ключ сохранён в Keychain.")
            return true
        } catch { present(error); return false }
    }
    func deleteKey() {
        guard !localOnly else { return }
        guard !isActive else { return }
        do { try APIKeyStore.delete(); hasKey = false; message = "" }
        catch { present(error) }
    }

    func copyKey(_ enteredKey: String, to pasteboard: NSPasteboard = .general) {
        do {
            var key = enteredKey.trimmingCharacters(in: .whitespacesAndNewlines)
            if key.isEmpty {
                guard !localOnly else { return }
                guard let savedKey = try APIKeyStore.load(), !savedKey.isEmpty else {
                    hasKey = false
                    throw DictationError.missingKey
                }
                key = savedKey
            }
            let item = NSPasteboardItem()
            item.setString(key, forType: .string)
            item.setData(Data(), forType: NSPasteboard.PasteboardType("org.nspasteboard.ConcealedType"))
            pasteboard.clearContents()
            if pasteboard.writeObjects([item]) {
                message = preferences.t("API key copied to clipboard.", "Ключ API скопирован в буфер обмена.")
            } else {
                message = preferences.t("Couldn't copy the API key. Try again.", "Не удалось скопировать ключ API. Попробуйте ещё раз.")
            }
        } catch { present(error) }
    }

    func toggle(transform: Bool) {
        guard !localOnly else { return }
        if phase == .preparing { stop(); return }
        if phase == .recording { stop(); return }
        guard phase == .idle, pasteTask == nil else { return }
        do {
            guard let key = try APIKeyStore.load(), !key.isEmpty else { throw DictationError.missingKey }
            guard AVCaptureDevice.authorizationStatus(for: .audio) == .authorized else { throw DictationError.microphone }
            let target = try TextTarget.capture(exclusions: preferences.excludedApps, transform: transform)
            let session = Session(target: target, transform: transform, key: key, preferences: preferences, replacements: replacements.engine())
            current = session; phase = .preparing; partial = ""; audioLevels.reset(); elapsed = 0; message = ""
            hotKeys.setCancelEnabled(true); onStateChange?()
            session.setup = Task { await start(session) }
        } catch { present(error); showSettings?() }
    }

    #if DEBUG
    /// Exercises production capture, focus monitoring, stop and delivery without credentials/network.
    func toggleLocalRecording(autoStop: Bool = false) {
        guard localOnly else { return }
        if isActive { stop(); return }
        do {
            preferences.mode = .accurate
            let target = try TextTarget.capture(exclusions: [])
            let session = Session(target: target, transform: false, key: "", preferences: preferences, replacements: replacements.engine())
            current = session; phase = .preparing; audioLevels.reset(); message = ""
            hotKeys.setCancelEnabled(true); onStateChange?()
            session.setup = Task { await start(session) }
            if autoStop {
                Task {
                    try? await Task.sleep(for: .seconds(3))
                    if current?.id == session.id { stop() }
                }
            }
        } catch { present(error) }
    }
    #endif

    private func start(_ session: Session) async {
        let id = session.id
        do {
            if session.mode == .live {
                let live = LiveTranscriptionSession(key: session.key, context: session.context) { [weak self] text in
                    Task { @MainActor in self?.receive(text, id: id) }
                }
                session.live = live
                let (stream, continuation) = AsyncStream<Data>.makeStream(bufferingPolicy: .bufferingOldest(AudioConstants.maximumQueuedChunks))
                session.continuation = continuation
                session.pump = Task { [weak self] in
                    do { for await chunk in stream { try Task.checkCancellation(); try await live.append(chunk) } }
                    catch { if self?.current?.id == id { self?.fail(error, session: session) } }
                }
                try await live.start()
            }
            try Task.checkCancellation()
            let continuation = session.continuation
            try await session.recorder.start(keepAudio: session.mode == .accurate, onChunk: { [weak self] chunk in
                if case .dropped = continuation?.yield(chunk) {
                    Task { @MainActor [weak self] in if self?.current?.id == id { self?.fail(DictationError.queueFull, session: session) } }
                }
            }, onLevel: { [weak self] level in
                Task { @MainActor in
                    guard let self, self.current?.id == id, self.phase.showsFinishControl else { return }
                    self.audioLevels.append(level)
                }
            }, onError: { [weak self] error in
                Task { @MainActor in
                    guard self?.current?.id == id else { return }
                    if case .tooLong = error { self?.stop() } else { self?.fail(error, session: session) }
                }
            })
            try Task.checkCancellation()
            guard current?.id == id else { return }
            if session.stopRequested { beginCompletion(session) }
            else { phase = .recording; onStateChange?() }
        } catch { if current?.id == id { fail(error, session: session) } }
    }

    func stop() {
        guard let session = current else { return }
        if phase == .preparing {
            session.stopRequested = true
            phase = .processing; onStateChange?()
            return
        }
        guard phase == .recording else { return }
        beginCompletion(session)
    }

    private func beginCompletion(_ session: Session) {
        phase = .processing; onStateChange?()
        session.completion = Task { await complete(session) }
    }

    private func complete(_ session: Session) async {
        do {
            let (pcm, bytes) = await session.recorder.stop()
            session.continuation?.finish()
            await session.pump?.value
            try Task.checkCancellation()
            guard current?.id == session.id else { return }
            guard bytes >= 4_800 else { throw DictationError.tooShort }
            let transcript: String
            if localOnly {
                transcript = "Локальная проверка 🙂"
            } else if let live = session.live {
                transcript = try await live.finish(timeout: session.timeout)
            } else {
                transcript = try await client.transcribe(pcm: pcm, key: session.key, context: session.context, timeout: session.timeout)
            }
            try Task.checkCancellation()
            guard current?.id == session.id else { return }
            var result = TranscriptFormatter.format(transcript, keepTrailingPeriod: session.keepTrailingPeriod)
            if session.transform {
                guard !result.isEmpty else { throw DictationError.tooShort }
                let (transformed, responseMessage) = try await client.transform(source: session.target.snapshot.selectedText,
                    instruction: result, key: session.key, model: session.textModel)
                try Task.checkCancellation()
                guard current?.id == session.id else { return }
                if let responseMessage, !responseMessage.isEmpty {
                    message = responseMessage; finish(session); return
                }
                result = transformed
            }
            if !session.transform { result = session.replacements.apply(result) }
            guard !result.isEmpty else { throw DictationError.tooShort }
            session.acceptsPartials = false
            try Task.checkCancellation()
            guard current?.id == session.id else { return }
            var delivered = false
            if !session.detached {
                delivered = await session.target.paste(result)
            }
            try Task.checkCancellation()
            guard current?.id == session.id else { return }
            lastTranscript = result
            if preferences.saveHistory { history.add(result, mode: session.transform ? "edit" : session.mode.rawValue) }
            message = delivered ? preferences.t("Text inserted.", "Текст вставлен.") :
                preferences.t("Focus changed or the editor refused the update. Your text is ready to copy from the menu.",
                              "Фокус изменился или редактор отклонил вставку. Скопируйте текст из меню.")
            finish(session)
        } catch { if current?.id == session.id { fail(error, session: session) } }
    }

    private func receive(_ text: String, id: UUID) {
        guard let session = current, session.id == id, !session.transform, session.acceptsPartials else { return }
        let text = session.replacements.apply(text, final: false)
        partial = text
    }

    func cancel() {
        guard let session = current else { return }
        terminate(session, error: nil)
    }

    private func fail(_ error: Error, session: Session) { terminate(session, error: error) }

    private func terminate(_ session: Session, error: Error?) {
        guard current?.id == session.id, !session.cancelling else { return }
        session.cancelling = true; session.acceptsPartials = false
        session.setup?.cancel(); session.completion?.cancel()
        session.continuation?.finish(); session.pump?.cancel()
        phase = .processing; onStateChange?()
        Task {
            await session.setup?.value
            _ = await session.recorder.stop()
            await session.live?.close()
            await session.completion?.value
            guard current?.id == session.id else { return }
            finish(session)
            if let error {
                if !(error is CancellationError) { present(error) }
            } else {
                message = preferences.t("Dictation cancelled.", "Диктовка отменена.")
                onStateChange?()
            }
        }
    }

    private func finish(_ session: Session) {
        guard current?.id == session.id else { return }
        current = nil; phase = .idle; audioLevels.reset(); partial = ""
        hotKeys.setCancelEnabled(false); onStateChange?()
        session.continuation?.finish(); session.pump?.cancel()
        Task {
            await session.setup?.value
            _ = await session.recorder.stop()
            await session.live?.close()
        }
    }

    private func tick() {
        if let session = current {
            if phase == .recording { elapsed = Int(Date().timeIntervalSince(session.started)) }
            if !session.detached && !session.target.isUpdating && !session.target.isCurrent {
                session.detached = true
                if phase == .recording { stop() }
                else if phase == .preparing { stop() }
            }
        }
        let allowed = AXIsProcessTrusted()
        if accessibilityAllowed != allowed {
            accessibilityAllowed = allowed
            if !recordingShortcut { configureHotKeys() }
        }
    }

    private func configureHotKeys() {
        do {
            try hotKeys.configure(preferences.shortcuts)
            shortcutError = ""
        } catch { shortcutError = (error as? DictationError)?.message(russian: preferences.isRussian) ?? error.localizedDescription }
        onStateChange?()
    }

    func recordShortcut(_ recording: Bool) {
        recordingShortcut = recording
        if recording { hotKeys.suspend() }
        else { configureHotKeys() }
    }

    @discardableResult func setShortcuts(_ shortcuts: ShortcutBindings) -> Bool {
        guard !isActive else { return false }
        guard shortcuts.isValid else {
            shortcutError = preferences.t("Dictation and voice editing need different shortcuts.",
                                         "Для диктовки и голосовой правки нужны разные сочетания.")
            return false
        }
        do {
            try hotKeys.configure(shortcuts)
            preferences.shortcuts = shortcuts
            shortcutError = ""
            return true
        } catch {
            shortcutError = (error as? DictationError)?.message(russian: preferences.isRussian) ?? error.localizedDescription
            return false
        }
    }

    func copyLast() {
        guard !lastTranscript.isEmpty else { return }
        copy(lastTranscript)
    }
    func copy(_ text: String) {
        NSPasteboard.general.clearContents(); NSPasteboard.general.setString(text, forType: .string)
        message = preferences.t("Copied to clipboard.", "Скопировано в буфер обмена.")
    }
    func pasteLast() {
        guard !isActive, pasteTask == nil, !lastTranscript.isEmpty else { return }
        do {
            let target = try TextTarget.capture(exclusions: preferences.excludedApps)
            let text = lastTranscript
            pasteTask = Task {
                defer { pasteTask = nil }
                if !(await target.paste(text)) { present(DictationError.noField) }
            }
        } catch { present(error) }
    }
    func addSelectionToDictionary() {
        guard !isActive else { return }
        do {
            let target = try TextTarget.capture(exclusions: preferences.excludedApps)
            guard !target.snapshot.selectedText.isEmpty else { throw DictationError.noField }
            preferences.dictionary = DictionaryTerms.normalize(preferences.dictionary + "\n" + target.snapshot.selectedText).joined(separator: "\n")
            message = preferences.t("Selection added to the dictionary.", "Выделение добавлено в словарь.")
        } catch { present(error) }
    }

    func searchHistory(_ query: String) {
        guard !localOnly else { return }
        cancelSearch()
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        do {
            guard let key = try APIKeyStore.load(), !key.isEmpty else { throw DictationError.missingKey }
            let entries = history.entries, model = preferences.textModel
            let id = UUID(); searchID = id
            isSearching = true
            searchTask = Task {
                defer { if searchID == id { isSearching = false } }
                do {
                    let matches = try await client.search(query: query, entries: entries, key: key, model: model)
                    try Task.checkCancellation()
                    if searchID == id { searchMatches = matches }
                } catch { if !Task.isCancelled && searchID == id { present(error) } }
            }
        } catch { present(error) }
    }
    func cancelSearch() { searchID = nil; searchTask?.cancel(); searchTask = nil; searchMatches = nil; isSearching = false }

    func setLoginEnabled(_ enabled: Bool) {
        do {
            if enabled { try SMAppService.mainApp.register() } else { try SMAppService.mainApp.unregister() }
            loginEnabled = SMAppService.mainApp.status == .enabled
            if SMAppService.mainApp.status == .requiresApproval { SMAppService.openSystemSettingsLoginItems() }
        } catch {
            loginEnabled = SMAppService.mainApp.status == .enabled
            message = preferences.t("Could not change login items. Move OpenDictate to Applications and try again.",
                                    "Не удалось изменить автозапуск. Переместите OpenDictate в Программы и повторите.")
        }
    }
    func present(_ error: Error) {
        message = (error as? DictationError)?.message(russian: preferences.isRussian) ?? error.localizedDescription
        onStateChange?()
    }
    func clearMessage() { message = "" }
    func shutdown() { cancel(); cancelSearch(); pasteTask?.cancel(); timer?.invalidate(); hotKeys.shutdown() }
}
