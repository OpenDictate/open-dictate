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
    private var schedule = SettingsSyncSchedule()
    private var now: Int64 { Int64(ProcessInfo.processInfo.systemUptime * 1000) }
    private let localOnly: Bool

    init(preferences: Preferences, localOnly: Bool = false) {
        self.preferences = preferences; self.localOnly = localOnly
        guard !localOnly else { return }
        status = preferences.driveSyncEnabled ? .waiting : .disconnected
        subscription = preferences.syncChanges.sink { [weak self] in
            self?.settingsChanged()
        }
        polling = Task { [weak self] in
            while !Task.isCancelled {
                self?.syncNow()
                do { try await Task.sleep(nanoseconds: 60_000_000_000) } catch { return }
            }
        }
    }

    private func settingsChanged() {
        guard preferences.driveSyncEnabled else { return }
        schedule.changed(now: now)
        pump()
    }

    private func pump() {
        guard !localOnly, preferences.driveSyncEnabled, task == nil else { return }
        debounce?.cancel()
        guard schedule.begin(now: now) else {
            if let delay = schedule.delayUntilReady(now: now) {
                debounce = Task { [weak self] in
                    do { try await Task.sleep(nanoseconds: UInt64(delay) * 1_000_000) } catch { return }
                    self?.pump()
                }
            }
            return
        }
        status = .busy
        let run = generation
        task = Task { [weak self] in
            guard let self else { return }
            defer { finish(run: run) }
            do { try await synchronize(token: authorization.refresh(client: client), run: run) }
            catch { if generation == run { show(error) } }
        }
    }

    private func finish(run: Int) {
        guard generation == run else { return }
        task = nil; schedule.finish(); pump()
    }

    func connect(clientSecret: String) {
        guard !localOnly, task == nil else { return }
        status = .busy
        let run = generation
        let clientID = preferences.driveClientID.trimmingCharacters(in: .whitespacesAndNewlines)
        task = Task { [weak self] in
            guard let self else { return }
            defer { finish(run: run) }
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
        guard !localOnly, preferences.driveSyncEnabled else { return }
        schedule.refresh()
        pump()
    }

    private func synchronize(token: String, run: Int) async throws {
        let remote = try await client.download(token: token, deviceID: preferences.syncDeviceID)
        try Task.checkCancellation()
        guard generation == run else { return }
        // Do not apply or publish a dictionary edit until its quiet period has elapsed.
        if schedule.hasPendingChange {
            schedule.refresh(); status = .waiting
            return
        }
        let merged = try preferences.mergeSync(remote.document)
        if remote.needsUpload(merged) {
            try await client.upload(token: token, deviceID: preferences.syncDeviceID, fileID: remote.ownFileID, document: merged)
        }
        try Task.checkCancellation()
        guard generation == run else { return }
        lastSyncedAt = Date(); status = .synced
    }

    func disconnect() {
        generation += 1
        task?.cancel(); task = nil; debounce?.cancel(); authorization.cancel()
        schedule = SettingsSyncSchedule()
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
