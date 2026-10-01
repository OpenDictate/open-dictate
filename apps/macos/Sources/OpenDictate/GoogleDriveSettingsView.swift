import SwiftUI

struct GoogleDriveSettingsView: View {
    @ObservedObject var sync: GoogleDriveSync
    @ObservedObject var preferences: Preferences
    @State private var clientSecret = ""
    private func t(_ en: String, _ ru: String) -> String { preferences.t(en, ru) }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Google Drive").fontWeight(.medium)
            Text(t("Sync your dictionary, word replacements, dictation mode and selected models across Android and macOS.",
                   "Синхронизация словаря, автозамен, режима диктовки и выбранных моделей между Android и macOS."))
                .font(.callout).foregroundStyle(.secondary)
            if !preferences.driveSyncEnabled || sync.needsSignIn {
                TextField(t("Desktop OAuth client ID", "ID OAuth-клиента для ПК"), text: $preferences.driveClientID)
                    .textFieldStyle(.roundedBorder).disabled(sync.status == .busy)
                SecureField(t("Desktop OAuth client secret", "Секрет OAuth-клиента для ПК"), text: $clientSecret)
                    .textFieldStyle(.roundedBorder).disabled(sync.status == .busy)
                Text(t("Use a Desktop client from the same Google Cloud project as the Android app. Sign-in opens your browser.",
                       "Используйте клиент типа Desktop из того же проекта Google Cloud, что и Android-приложение. Вход откроется в браузере."))
                    .font(.caption).foregroundStyle(.secondary)
            }
            Text(statusText).font(.caption).foregroundStyle(.secondary).accessibilityLabel(statusText)
            HStack {
                if sync.status == .busy { ProgressView().controlSize(.small) }
                Button(preferences.driveSyncEnabled && !sync.needsSignIn
                       ? t("Sync now", "Синхронизировать") : t("Connect Google Drive", "Подключить Google Drive")) {
                    if preferences.driveSyncEnabled && !sync.needsSignIn { sync.syncNow() }
                    else { sync.connect(clientSecret: clientSecret); clientSecret = "" }
                }.disabled(sync.status == .busy || (!preferences.driveSyncEnabled && preferences.driveClientID.isEmpty))
                if preferences.driveSyncEnabled || sync.status == .busy {
                    Button(sync.status == .busy && !preferences.driveSyncEnabled ? t("Cancel", "Отмена") : t("Disconnect", "Отключить"),
                           action: sync.disconnect)
                }
            }
            Text(t("Only these settings go to Google. API keys, audio and history are never synced. Disconnecting keeps local and cloud settings.",
                   "В Google отправляются только эти настройки. API-ключи, звук и история не синхронизируются. Отключение сохраняет локальные и облачные настройки."))
                .font(.caption).foregroundStyle(.secondary)
        }
    }
    private var statusText: String {
        switch sync.status {
        case .disconnected: return t("Not connected", "Не подключено")
        case .waiting: return t("Waiting to synchronize", "Ожидание синхронизации")
        case .busy: return t("Synchronizing or waiting for Google sign-in…", "Синхронизация или ожидание входа в Google…")
        case .synced:
            let date = sync.lastSyncedAt?.formatted(date: .abbreviated, time: .shortened) ?? ""
            return t("Last synced: \(date). Changes sync automatically while the app is running.",
                     "Последняя синхронизация: \(date). Изменения синхронизируются автоматически, пока приложение работает.")
        case .failed: return t("Could not sync. Local settings are saved. Check your connection and try again.",
                               "Не удалось синхронизировать. Локальные настройки сохранены. Проверьте соединение и повторите.")
        case .authorization: return t("Sign in again to resume synchronization.", "Войдите снова, чтобы возобновить синхронизацию.")
        case .signInTimedOut: return t("Google sign-in timed out. Connect again and finish sign-in within 10 minutes.",
                                      "Время ожидания входа в Google истекло. Подключитесь снова и завершите вход в течение 10 минут.")
        case .configuration: return t("Enter a valid Desktop OAuth client ID.", "Введите корректный ID OAuth-клиента типа Desktop.")
        }
    }
}
