import Foundation

/// Expected input languages, encoded with ISO 639-1 codes for both transcription paths.
public struct SpeechLanguage: Identifiable, Equatable, Sendable {
    public let id: String
    public let nativeName: String

    public func title(russian: Bool) -> String {
        let locale = Locale(identifier: russian ? "ru" : "en")
        return (locale.localizedString(forLanguageCode: id) ?? nativeName).capitalized(with: locale)
    }

    public func matches(_ query: String) -> Bool {
        let query = query.trimmingCharacters(in: .whitespacesAndNewlines)
        return query.isEmpty || [id, nativeName, title(russian: false), title(russian: true)].contains {
            $0.range(of: query, options: [.caseInsensitive, .diacriticInsensitive],
                     locale: Locale(identifier: "en")) != nil
        }
    }

    public static func normalize<S: Sequence>(_ codes: S) -> [String] where S.Element == String {
        let allowed = Set(all.map(\.id))
        return Set(codes).intersection(allowed).sorted()
    }

    public static func restore(stored: [String]?, legacy: String?) -> [String] {
        if let stored { return normalize(stored) }
        if legacy == "ru-en" { return ["en", "ru"] }
        return normalize(legacy.map { [$0] } ?? [])
    }

    public static let all: [SpeechLanguage] = [
        .init(id: "af", nativeName: "Afrikaans"),
        .init(id: "ar", nativeName: "العربية"),
        .init(id: "az", nativeName: "Azərbaycan dili"),
        .init(id: "be", nativeName: "Беларуская"),
        .init(id: "bg", nativeName: "Български"),
        .init(id: "bn", nativeName: "বাংলা"),
        .init(id: "bs", nativeName: "Bosanski"),
        .init(id: "ca", nativeName: "Català"),
        .init(id: "cs", nativeName: "Čeština"),
        .init(id: "cy", nativeName: "Cymraeg"),
        .init(id: "da", nativeName: "Dansk"),
        .init(id: "de", nativeName: "Deutsch"),
        .init(id: "el", nativeName: "Ελληνικά"),
        .init(id: "en", nativeName: "English"),
        .init(id: "es", nativeName: "Español"),
        .init(id: "et", nativeName: "Eesti"),
        .init(id: "eu", nativeName: "Euskara"),
        .init(id: "fa", nativeName: "فارسی"),
        .init(id: "fi", nativeName: "Suomi"),
        .init(id: "fr", nativeName: "Français"),
        .init(id: "gl", nativeName: "Galego"),
        .init(id: "gu", nativeName: "ગુજરાતી"),
        .init(id: "he", nativeName: "עברית"),
        .init(id: "hi", nativeName: "हिन्दी"),
        .init(id: "hr", nativeName: "Hrvatski"),
        .init(id: "hu", nativeName: "Magyar"),
        .init(id: "hy", nativeName: "Հայերեն"),
        .init(id: "id", nativeName: "Bahasa Indonesia"),
        .init(id: "is", nativeName: "Íslenska"),
        .init(id: "it", nativeName: "Italiano"),
        .init(id: "ja", nativeName: "日本語"),
        .init(id: "ka", nativeName: "ქართული"),
        .init(id: "kk", nativeName: "Қазақша"),
        .init(id: "kn", nativeName: "ಕನ್ನಡ"),
        .init(id: "ko", nativeName: "한국어"),
        .init(id: "lt", nativeName: "Lietuvių"),
        .init(id: "lv", nativeName: "Latviešu"),
        .init(id: "mk", nativeName: "Македонски"),
        .init(id: "ml", nativeName: "മലയാളം"),
        .init(id: "mr", nativeName: "मराठी"),
        .init(id: "ms", nativeName: "Bahasa Melayu"),
        .init(id: "ne", nativeName: "नेपाली"),
        .init(id: "nl", nativeName: "Nederlands"),
        .init(id: "no", nativeName: "Norsk"),
        .init(id: "pa", nativeName: "ਪੰਜਾਬੀ"),
        .init(id: "pl", nativeName: "Polski"),
        .init(id: "pt", nativeName: "Português"),
        .init(id: "ro", nativeName: "Română"),
        .init(id: "ru", nativeName: "Русский"),
        .init(id: "sk", nativeName: "Slovenčina"),
        .init(id: "sl", nativeName: "Slovenščina"),
        .init(id: "sq", nativeName: "Shqip"),
        .init(id: "sr", nativeName: "Српски"),
        .init(id: "sv", nativeName: "Svenska"),
        .init(id: "sw", nativeName: "Kiswahili"),
        .init(id: "ta", nativeName: "தமிழ்"),
        .init(id: "te", nativeName: "తెలుగు"),
        .init(id: "th", nativeName: "ไทย"),
        .init(id: "tr", nativeName: "Türkçe"),
        .init(id: "uk", nativeName: "Українська"),
        .init(id: "ur", nativeName: "اردو"),
        .init(id: "uz", nativeName: "Oʻzbekcha"),
        .init(id: "vi", nativeName: "Tiếng Việt"),
        .init(id: "zh", nativeName: "中文")
    ]
}
