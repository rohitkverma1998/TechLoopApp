from __future__ import annotations

import json
import re
import shutil
import sys
from pathlib import Path

from PIL import Image

from dice_pack_config import (
    BOOK_ID,
    BOOK_PATH,
    BOOK_TITLE_EN,
    BOOK_TITLE_HI,
    CATALOG_PATH,
    IMAGES_DIR,
    LEGACY_PACK_NAMES,
    SUBJECT_TITLE_EN,
    SUBJECT_TITLE_HI,
    DicePdfConfig,
    selected_configs,
)
from subject_pack_io import save_book, save_json


def localized(english: str, hindi: str) -> dict[str, str]:
    return {"english": english.strip(), "hindi": hindi.strip()}


def split_solution(text: str) -> list[str]:
    cleaned = text.strip().replace("\r\n", "\n").replace("\r", "\n")
    if not cleaned:
        return []
    parts = [part.strip() for part in re.split(r"\n\s*\n", cleaned) if part.strip()]
    return parts or [cleaned]


def slug(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", value.lower()).strip("_")


def strip_inline_options(prompt: str) -> str:
    return re.sub(r"\s*(options|विकल्प)\s*:\s*.*$", "", prompt, flags=re.IGNORECASE).strip()


def dedupe_preserve_order(values: list[str]) -> list[str]:
    seen: set[str] = set()
    ordered: list[str] = []
    for raw_value in values:
        value = raw_value.strip()
        if not value:
            continue
        marker = value.casefold()
        if marker in seen:
            continue
        seen.add(marker)
        ordered.append(value)
    return ordered


def question_image_name(config: DicePdfConfig, sequence_number: int) -> str:
    return f"{config.image_prefix}_q{sequence_number:02d}.jpg"


def source_page_path(config: DicePdfConfig, page_number: int) -> Path:
    return config.render_dir / f"page_{page_number:02d}.png"


def export_question_image(config: DicePdfConfig, page_number: int, sequence_number: int) -> str:
    source = source_page_path(config, page_number)
    if not source.exists():
        raise FileNotFoundError(f"Missing rendered page for {config.key} page {page_number}: {source}")

    IMAGES_DIR.mkdir(parents=True, exist_ok=True)
    output_name = question_image_name(config, sequence_number)
    output_path = IMAGES_DIR / output_name

    with Image.open(source) as image:
        image = image.convert("RGB")
        width, height = image.size
        target_width = min(width, 1600)
        if target_width < width:
            target_height = round(height * target_width / width)
            image = image.resize((target_width, target_height), Image.Resampling.LANCZOS)
        image.save(output_path, format="JPEG", quality=84, optimize=True, progressive=True)

    return output_name


def clear_images_dir() -> None:
    IMAGES_DIR.mkdir(parents=True, exist_ok=True)
    for child in IMAGES_DIR.iterdir():
        if child.is_file():
            child.unlink()


def remove_legacy_packs() -> None:
    subject_packs_dir = BOOK_PATH.parent
    for pack_name in LEGACY_PACK_NAMES:
        manifest_path = subject_packs_dir / f"{pack_name}.json"
        if manifest_path.exists():
            manifest_path.unlink()

        pack_dir = subject_packs_dir / pack_name
        if pack_dir.exists():
            shutil.rmtree(pack_dir)


def load_payloads(config: DicePdfConfig) -> list[tuple[int, dict[str, object]]]:
    payloads: list[tuple[int, dict[str, object]]] = []
    for page_number in config.candidate_pages:
        path = config.codex_dir / f"page_{page_number:02d}.json"
        if not path.exists():
            raise FileNotFoundError(f"Missing generated question file: {path}")
        payload = json.loads(path.read_text(encoding="utf-8"))
        if payload.get("is_question"):
            payloads.append((page_number, payload))
    return payloads


def build_topic(
    config: DicePdfConfig,
    page_number: int,
    sequence_number: int,
    payload: dict[str, object],
) -> dict[str, object]:
    visible_question_number = payload.get("question_number")
    topic_title_en = str(payload["topic_title_en"]).strip() or f"{config.chapter_title_en} Question"
    topic_title_hi = str(payload["topic_title_hi"]).strip() or f"{config.chapter_title_hi} प्रश्न"
    prompt_en = strip_inline_options(str(payload["question_prompt_en"]).strip())
    prompt_hi = strip_inline_options(str(payload["question_prompt_hi"]).strip())
    question_type = str(payload["question_type"]).strip() or "TEXT_INPUT"
    options = [str(option).strip() for option in payload.get("options") or [] if str(option).strip()]
    correct_option_index = payload.get("correct_option_index")
    final_answer = str(payload.get("final_answer") or "").strip()
    support_en = str(payload.get("support_example_en") or "").strip()
    support_hi = str(payload.get("support_example_hi") or "").strip()
    solution_en = str(payload.get("solution_en") or "").strip()
    solution_hi = str(payload.get("solution_hi") or "").strip()

    if not support_en and not support_hi:
        support_en = "Use the given figure step by step."
        support_hi = "दिए गए चित्र को चरण-दर-चरण देखें।"

    if question_type != "MULTIPLE_CHOICE" or not options or correct_option_index is None:
        question_type = "TEXT_INPUT"
        options = []
        correct_option_index = None

    accepted_answers = dedupe_preserve_order(
        [str(value) for value in (payload.get("accepted_answers") or [])] + [final_answer]
    )

    subtopic_en = f"Question {sequence_number:02d}: {topic_title_en}"
    subtopic_hi = f"प्रश्न {sequence_number:02d}: {topic_title_hi}"
    explanation_en = [f"Question: {prompt_en}"]
    explanation_hi = [f"प्रश्न: {prompt_hi}"]
    explanation_en.extend(split_solution(solution_en))
    explanation_hi.extend(split_solution(solution_hi))

    reteach_paragraphs = [
        localized(english or hindi or "", hindi or english or "")
        for english, hindi in zip(split_solution(solution_en), split_solution(solution_hi), strict=False)
    ]
    if not reteach_paragraphs:
        reteach_paragraphs = [localized(solution_en, solution_hi)]

    image_name = export_question_image(config, page_number, sequence_number)

    return {
        "id": f"{config.key}_q{sequence_number:02d}_{slug(topic_title_en) or 'question'}",
        "sourceLessonId": config.source_lesson_id,
        "chapterNumber": config.chapter_number,
        "chapterTitle": localized(config.chapter_title_en, config.chapter_title_hi),
        "lessonTitle": localized(config.chapter_title_en, config.chapter_title_hi),
        "subtopicTitle": localized(subtopic_en, subtopic_hi),
        "knowPrompt": localized(
            f"Solve Question {sequence_number:02d} from {config.chapter_title_en}.",
            f"{config.chapter_title_hi} का प्रश्न {sequence_number:02d} हल कीजिए।",
        ),
        "explanationTitle": localized(topic_title_en, topic_title_hi),
        "explanationParagraphs": [
            localized(english or hindi or "", hindi or english or "")
            for english, hindi in zip(explanation_en, explanation_hi, strict=False)
        ],
        "examples": [localized(support_en, support_hi)],
        "visuals": [],
        "questions": [
            {
                "id": f"{config.key}_q{sequence_number:02d}",
                "prompt": localized(prompt_en, prompt_hi),
                "type": question_type,
                "options": [localized(option, option) for option in options],
                "correctOptionIndex": correct_option_index,
                "acceptedAnswers": accepted_answers,
                "hint": localized("", ""),
                "wrongReason": localized(solution_en, solution_hi),
                "supportExample": localized(support_en, support_hi),
                "mistakeType": "PATTERN_RULE",
                "reteachTitle": localized("Solution", "हल"),
                "reteachParagraphs": reteach_paragraphs,
                "questionImageAsset": image_name,
            }
        ],
        "tags": [
            localized(SUBJECT_TITLE_EN, SUBJECT_TITLE_HI),
            localized(config.chapter_title_en, config.chapter_title_hi),
            localized(f"Question {sequence_number:02d}", f"प्रश्न {sequence_number:02d}"),
            localized(
                f"Source page {page_number:02d}",
                f"स्रोत पेज {page_number:02d}",
            ),
        ],
        "mistakeFocus": "PATTERN_RULE",
    }


def build_book(configs: list[DicePdfConfig]) -> dict[str, object]:
    clear_images_dir()
    topics: list[dict[str, object]] = []
    for config in configs:
        for sequence_number, (page_number, payload) in enumerate(load_payloads(config), start=1):
            topics.append(build_topic(config, page_number, sequence_number, payload))

    return {
        "id": BOOK_ID,
        "subjectTitle": localized(SUBJECT_TITLE_EN, SUBJECT_TITLE_HI),
        "bookTitle": localized(BOOK_TITLE_EN, BOOK_TITLE_HI),
        "teacherNote": localized(
            "Each chapter comes from a separate PDF. Use the screenshot, compare faces carefully, and then check the bilingual solution.",
            "हर अध्याय अलग PDF से लिया गया है। पहले स्क्रीनशॉट देखकर फलकों की तुलना कराइए, फिर द्विभाषी हल जाँचिए।",
        ),
        "topics": topics,
    }


def write_catalog() -> None:
    save_json(
        CATALOG_PATH,
        [
            {
                "id": BOOK_ID,
                "title": localized(BOOK_TITLE_EN, BOOK_TITLE_HI),
                "assetPath": f"subject_packs/{BOOK_PATH.name}",
            }
        ],
    )


def main(argv: list[str]) -> int:
    configs = selected_configs(argv)
    remove_legacy_packs()
    book = build_book(configs)
    save_book(BOOK_PATH, book)
    write_catalog()
    print(f"Wrote {len(book['topics'])} Dice and Cube topics to {BOOK_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
