from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def save_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def load_book(path: Path) -> dict[str, Any]:
    manifest = load_json(path)
    topics = list(manifest.get("topics") or [])
    for chapter_asset_path in manifest.get("chapterAssetPaths") or []:
        chapter_path = resolve_book_path(path, str(chapter_asset_path))
        chapter_payload = load_json(chapter_path)
        topics.extend(chapter_payload.get("topics") or [])

    book = dict(manifest)
    book["topics"] = topics
    return book


def save_book(path: Path, book: dict[str, Any]) -> dict[str, Any]:
    topics = list(book.get("topics") or [])
    chapter_dir = path.with_suffix("")
    chapter_dir.mkdir(parents=True, exist_ok=True)

    chapters = group_topics_by_chapter(topics)
    chapter_asset_paths: list[str] = []
    expected_files: set[str] = set()
    for chapter_number, chapter_topics in chapters:
        chapter_title = chapter_title_english(chapter_topics)
        file_name = build_chapter_file_name(chapter_number, chapter_title)
        expected_files.add(file_name)
        chapter_asset_paths.append(f"{path.stem}/{file_name}")
        save_json(
            chapter_dir / file_name,
            {
                "chapterNumber": chapter_number,
                "chapterTitle": chapter_topics[0].get("chapterTitle"),
                "topics": chapter_topics,
            },
        )

    for existing_file in chapter_dir.glob("*.json"):
        if existing_file.name not in expected_files:
            existing_file.unlink()

    manifest = {
        key: value
        for key, value in book.items()
        if key not in {"topics", "chapterAssetPaths"}
    }
    manifest["chapterAssetPaths"] = chapter_asset_paths
    save_json(path, manifest)
    return manifest


def resolve_book_path(base_path: Path, asset_path: str) -> Path:
    normalized = asset_path.replace("\\", "/").lstrip("/")
    return (base_path.parent / normalized).resolve()


def group_topics_by_chapter(topics: list[dict[str, Any]]) -> list[tuple[int, list[dict[str, Any]]]]:
    grouped: dict[int, list[dict[str, Any]]] = {}
    for topic in topics:
        raw_number = topic.get("chapterNumber")
        if raw_number is None:
            raise ValueError(f"Topic is missing chapterNumber: {topic.get('id', '<unknown>')}")
        chapter_number = int(raw_number)
        grouped.setdefault(chapter_number, []).append(topic)
    return sorted(grouped.items())


def chapter_title_english(chapter_topics: list[dict[str, Any]]) -> str:
    first_topic = chapter_topics[0]
    chapter_title = first_topic.get("chapterTitle") or {}
    if isinstance(chapter_title, dict):
        return str(chapter_title.get("english") or chapter_title.get("hindi") or "").strip()
    return ""


def build_chapter_file_name(chapter_number: int, chapter_title: str) -> str:
    chapter_slug = slugify(chapter_title) or f"chapter_{chapter_number:02d}"
    return f"chapter_{chapter_number:02d}_{chapter_slug}.json"


def slugify(value: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "_", value.lower()).strip("_")
    return slug
