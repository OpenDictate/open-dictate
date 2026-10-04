import Foundation
import Combine
import OpenDictateCore

@MainActor
final class GoogleDriveSync: ObservableObject {
    enum Status { case disconnected, waiting, busy, synced, failed, sourceSelection, authorization, signInTimedOut, configuration }
    @Published private(set) var status: Status = .disconnected
    @Published private(set) var lastSyncedAt: Date?
    var needsSignIn: Bool { status == .authorization || status == .signInTimedOut }
    private let preferences: Preferences
    private let client: GoogleDriveClient
    private let deleteCredentials: () throws -> Void
    private let refreshToken: (() async throws -> String)?
    private struct ConnectionChoice {
        let local: SettingsSyncDocument
        let cloud: SettingsSyncDocument
        var source: SettingsSyncDocument.ConnectionSource?
    }
    private var connectionChoice: ConnectionChoice?
    @Published private(set) var needsSourceSelection = false
    private let authorization = GoogleDriveAuthorization()
    private var task: Task<Void, Never>?
    private var debounce: Task<Void, Never>?
    private var polling: Task<Void, Never>?
    private var subscription: AnyCancellable?
    private var generation = 0
    private var schedule = SettingsSyncSchedule()
    private var now: Int64 { Int64(ProcessInfo.processInfo.systemUptime * 1000) }
    private let localOnly: Bool

    init(preferences: Preferences, localOnly: Bool = false, client: GoogleDriveClient = GoogleDriveClient(),
         refreshToken: (() async throws -> String)? = nil, automaticallySync: Bool = true,
         deleteCredentials: @escaping () throws -> Void = DriveCredentialStore.delete) {
        self.preferences = preferences; self.localOnly = localOnly
        self.client = client; self.refreshToken = refreshToken; self.deleteCredentials = deleteCredentials
        guard !localOnly else { return }
        status = preferences.driveSyncEnabled ? .waiting : .disconnected
        subscription = preferences.syncChanges.sink { [weak self] in
            self?.settingsChanged()
        }
        guard automaticallySync else { return }
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
        guard !localOnly, preferences.driveSyncEnabled, !needsSourceSelection, task == nil else { return }
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
            do {
                let token: String
                if let refreshToken { token = try await refreshToken() }
                else { token = try await authorization.refresh(client: client) }
                try await synchronize(token: token, run: run)
            }
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
                preferences.driveConnectionPending = true
                connectionChoice = nil; needsSourceSelection = false
                preferences.driveSyncEnabled = true
                try await synchronize(token: token, run: run)
            } catch { if generation == run { show(error) } }
        }
    }

    func chooseSource(_ source: SettingsSyncDocument.ConnectionSource) {
        guard needsSourceSelection, connectionChoice != nil else { return }
        connectionChoice?.source = source
        needsSourceSelection = false
        syncNow()
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
        var source: SettingsSyncDocument.ConnectionSource?
        if preferences.driveConnectionPending {
            try preferences.validateSync(remote.document)
            let local = try preferences.syncDocument()
            if local.needsConnectionChoice(with: remote.document) {
                if let choice = connectionChoice, let selected = choice.source,
                   choice.local.hasSameSettings(as: local), choice.cloud.hasSameSettings(as: remote.document) {
                    source = selected
                } else {
                    connectionChoice = ConnectionChoice(local: local, cloud: remote.document)
                    needsSourceSelection = true; status = .sourceSelection
                    return
                }
            }
        }
        let merged = try preferences.mergeSync(remote.document, source: source)
        if remote.needsUpload(merged) {
            try await client.upload(token: token, deviceID: preferences.syncDeviceID, fileID: remote.ownFileID, document: merged)
        }
        try Task.checkCancellation()
        guard generation == run else { return }
        preferences.driveConnectionPending = false
        connectionChoice = nil
        lastSyncedAt = Date(); status = .synced
    }

    func disconnect() {
        generation += 1
        task?.cancel(); task = nil; debounce?.cancel(); authorization.cancel()
        schedule = SettingsSyncSchedule()
        preferences.driveSyncEnabled = false
        preferences.driveConnectionPending = false
        connectionChoice = nil; needsSourceSelection = false
        do { try deleteCredentials(); status = .disconnected; lastSyncedAt = nil }
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
