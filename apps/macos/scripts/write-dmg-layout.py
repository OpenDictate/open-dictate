"""Write the installer row directly, without Finder automation during packaging."""

from pathlib import Path
import sys

from ds_store import DSStore


def write_layout(folder: Path) -> None:
    items = (
        "Applications",
        "Install OpenDictate.txt",
        "OpenDictate.app",
    )
    for name in items:
        if not (folder / name).exists():
            raise FileNotFoundError(f"Missing installer item: {name}")

    with DSStore.open(str(folder / ".DS_Store"), "w+") as store:
        store["."]["vSrn"] = ("long", 1)
        store["."]["icvl"] = ("type", b"icnv")
        store["."]["bwsp"] = {
            "WindowBounds": "{{100, 100}, {660, 200}}",
            "ShowToolbar": False,
            "ShowSidebar": False,
            "ShowStatusBar": False,
            "ShowPathbar": False,
            "ShowTabView": False,
            "ContainerShowSidebar": False,
            "PreviewPaneVisibility": False,
        }
        store["."]["icvp"] = {
            "viewOptionsVersion": 1,
            "backgroundType": 0,
            "arrangeBy": "none",
            "iconSize": 64.0,
            "textSize": 14.0,
            "labelOnBottom": True,
            "showIconPreview": True,
            "showItemInfo": False,
            "gridSpacing": 220.0,
            "gridOffsetX": 0.0,
            "gridOffsetY": 0.0,
            "scrollPositionX": 0.0,
            "scrollPositionY": 0.0,
        }
        for index, name in enumerate(items):
            store[name]["Iloc"] = (100 + index * 220, 95)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Usage: write-dmg-layout.py STAGING_FOLDER")
    write_layout(Path(sys.argv[1]))
