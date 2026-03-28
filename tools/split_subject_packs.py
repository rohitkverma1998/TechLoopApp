from __future__ import annotations

from pathlib import Path

from subject_pack_io import load_book, save_book


ROOT = Path(__file__).resolve().parents[1]
PACK_PATHS = [
    ROOT / "app" / "src" / "main" / "assets" / "subject_packs" / "class5_rs_aggarwal_math.json",
    ROOT / "app" / "src" / "main" / "assets" / "subject_packs" / "class5_science_story_lab.json",
    ROOT / "app" / "src" / "main" / "assets" / "subject_packs" / "class5_english_reading_trails.json",
]


def main() -> None:
    for pack_path in PACK_PATHS:
        book = load_book(pack_path)
        manifest = save_book(pack_path, book)
        print(
            f"Split {pack_path.name} into {len(manifest.get('chapterAssetPaths', []))} chapter file(s)."
        )


if __name__ == "__main__":
    main()
