import SwiftUI

enum AppTheme: String, CaseIterable {
    case system, light, dark

    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }

    var symbol: String {
        switch self {
        case .system: return "circle.lefthalf.filled"
        case .light: return "sun.max"
        case .dark: return "moon"
        }
    }

    func title(russian: Bool) -> String {
        switch self {
        case .system: return russian ? "Системная тема" : "System theme"
        case .light: return russian ? "Светлая тема" : "Light theme"
        case .dark: return russian ? "Тёмная тема" : "Dark theme"
        }
    }
}
