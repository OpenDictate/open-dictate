import Foundation
import Combine
import OpenDictateCore

@MainActor
final class GoogleDriveSync: ObservableObject {
    enum Status { case disconnected, waiting, busy, synced, failed, authorization, signInTimedOut, configuration }
    @Published private(set) var status: Status = .disconnected
    @Published private(set) var lastSyncedAt: Date?
    var needsSignIn: Bool { status == .authorization || status == .signInTimedOut }
    private let preferences: Preferences
    private let client = GoogleDriveClient()
    private let authorization = GoogleDriveAuthorization()
    private var task: Task<Void, Never>?
    private var debounce: Task<Void, Never>?
    private var polling: Task<Void, Never>?
    private var subscription: AnyCancellable?
    private var generation = 0
    private let localOnly: Bool

    init(preferences: Preferences, localOnly: Bool = false) {
        self.preferences = preferences; self.localOnly = localOnly
        guard !localOnly else { return }
        status = preferences.driveSyncEnabled ? .waiting : .disconnected
        subscription = preferences.objectWillChange.sink { [weak self] in
            Task { @MainActor in self?.schedule() }
        }
        polling = Task { [weak self] in
            while !Task.isCancelled {
                self?.syncNow()
                do { try await Task.sleep(nanoseconds: 60_000_000_000) } catch { return }
            }
        }
    }

    private func schedule() {
        guard preferences.driveSyncEnabled else { return }
        debounce?.cancel()
        debounce = Task { [weak self] in
            do { try await Task.sleep(nanoseconds: 1_500_000_000) } catch { return }
            self?.syncNow()
        }
    }

    func connect(clientSecret: String) {
        guard !localOnly, task == nil else { return }
        status = .busy
        let run = generation
        let clientID = preferences.driveClientID.trimmingCharacters(in: .whitespacesAndNewlines)
        task = Task { [weak self] in
            guard let self else { return }
            defer { if generation == run { task = nil } }
            do {
                let token = try await authorization.connect(clientID: clientID, clientSecret: clientSecret, client: client)
                try Task.checkCancellation()
                guard generation == run else { return }
                preferences.driveSyncEnabled = true
                try await synchronize(token: token, run: run)
            } catch { if generation == run { show(error) } }
        }
    }

    func syncNow() {
        guard !localOnly, preferences.driveSyncEnabled, task == nil else { return }
        status = .busy
        let run = generation
        task = Task { [weak self] in
            guard let self else { return }
            defer { if generation == run { task = nil } }
            do { try await synchronize(token: authorization.refresh(client: client), run: run) }
            catch { if generation == run { show(error) } }
        }
    }

    private func synchronize(token: String, run: Int) async throws {
        let remote = try await client.download(token: token, deviceID: preferences.syncDeviceID)
        try Task.checkCancellation()
        guard generation == run else { return }
        let merged = try preferences.mergeSync(remote.document)
        try await client.upload(token: token, deviceID: preferences.syncDeviceID, fileID: remote.ownFileID, document: merged)
        try Task.checkCancellation()
        guard generation == run else { return }
        lastSyncedAt = Date(); status = .synced
    }

    func disconnect() {
        generation += 1
        task?.cancel(); task = nil; debounce?.cancel(); authorization.cancel()
        preferences.driveSyncEnabled = false
        do { try DriveCredentialStore.delete(); status = .disconnected; lastSyncedAt = nil }
        catch { status = .authorization }
    }

    private func show(_ error: Error) {
        if error is CancellationError { status = preferences.driveSyncEnabled ? .waiting : .disconnected }
        else if let error = error as? DriveError, error == .signInTimedOut { status = .signInTimedOut }
        else if let error = error as? DriveError, error == .authorization { status = .authorization }
        else if let error = error as? DriveError, error == .configuration { status = .configuration }
        else { status = .failed }
    }
}
