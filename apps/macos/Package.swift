// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "OpenDictate",
    platforms: [.macOS(.v14)],
    products: [
        .executable(name: "OpenDictate", targets: ["OpenDictate"]),
        .executable(name: "OpenDictateEject", targets: ["OpenDictateEject"])
    ],
    targets: [
        .target(name: "OpenDictateCore"),
        .executableTarget(name: "OpenDictate", dependencies: ["OpenDictateCore"],
                          linkerSettings: [.linkedFramework("Carbon")]),
        .executableTarget(name: "OpenDictateEject"),
        .testTarget(name: "OpenDictateEjectTests", dependencies: ["OpenDictateEject"]),
        .testTarget(name: "OpenDictateCoreTests", dependencies: ["OpenDictateCore"]),
        .testTarget(name: "OpenDictateTests", dependencies: ["OpenDictate", "OpenDictateCore"])
    ]
)
