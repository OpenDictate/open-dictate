import Foundation

struct DiskImages: Decodable {
    struct Image: Decodable {
        struct Entity: Decodable {
            let mountPoint: String?
            enum CodingKeys: String, CodingKey { case mountPoint = "mount-point" }
        }
        let entities: [Entity]
        enum CodingKeys: String, CodingKey { case entities = "system-entities" }
    }
    let images: [Image]

    // Match the helper at the image root, never a name/prefix or another mounted image.
    func installerVolume(containing bundle: URL) -> URL? {
        images.flatMap(\.entities).compactMap(\.mountPoint).compactMap { path in
            let volume = URL(fileURLWithPath: path, isDirectory: true).standardizedFileURL
            guard volume.path != "/",
                  volume.appendingPathComponent("Eject.app").standardizedFileURL.path == bundle.standardizedFileURL.path
            else { return nil }
            return volume
        }.first
    }
}
