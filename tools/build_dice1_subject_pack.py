from __future__ import annotations

import json
import re
import shutil
import sys
from itertools import zip_longest
from pathlib import Path

from PIL import Image

from subject_pack_io import save_book, save_json


ROOT = Path(__file__).resolve().parents[1]
CODEX_DIR = ROOT / "tmp_pages" / "dice1_codex"
OCR_DIR = ROOT / "tmp_pages" / "dice1_ocr"
SUBJECT_PACKS_DIR = ROOT / "app" / "src" / "main" / "assets" / "subject_packs"
BOOK_PATH = SUBJECT_PACKS_DIR / "class5_rs_aggarwal_math.json"
BOOK_DIR = BOOK_PATH.with_suffix("")
IMAGES_DIR = BOOK_DIR / "images"
CATALOG_PATH = SUBJECT_PACKS_DIR / "catalog.json"
FIRST_QUESTION_PAGE = 11
ALL_QUESTION_NUMBERS = tuple(range(1, 27))

BOOK_ID = "verbal_reasoning_dice_and_cube"
CHAPTER_TITLE_EN = "Dice and Cube"
CHAPTER_TITLE_HI = "डाइस और क्यूब"
SUBJECT_TITLE_EN = "Verbal Reasoning"
SUBJECT_TITLE_HI = "वर्बल रीजनिंग"

LEGACY_PACK_NAMES = (
    "class5_science_story_lab",
    "class5_english_reading_trails",
)

ACCEPTED_ANSWERS: dict[int, list[str]] = {
    1: ["5", "face 5", "option b", "b", "पाँच", "५"],
    2: ["4", "option a", "a", "चार", "४"],
    3: ["2", "two", "option c", "c", "दो", "२"],
    4: ["5", "five", "option b", "b", "पाँच", "५"],
    5: ["L", "l", "face l", "option b", "b", "एल"],
    6: ["6", "face 6", "number 6", "छह", "६"],
    7: ["CND", "cannot be determined", "cannot determine", "निर्धारित नहीं किया जा सकता"],
    8: ["Blue", "blue", "option a", "a", "नीला"],
    9: ["M", "m", "face m", "एम"],
    10: ["C", "c", "option c", "pentagon", "blue pentagon", "पंचभुज"],
    11: ["4", "four", "number 4", "चार", "४"],
    12: ["N", "n", "face n", "एन"],
    13: ["3", "three", "face 3", "option d", "d", "तीन", "३"],
    14: ["4", "four", "option a", "a", "चार", "४"],
    15: ["3", "three", "3 dots", "option c", "c", "तीन", "३"],
    16: ["O", "o", "option a", "a", "ओ"],
    17: ["(d)", "d", "option d", "statement d", "D is adjacent to F", "D and F are adjacent"],
    18: ["4", "four", "option d", "d", "चार", "४"],
    19: ["3", "three", "face 3", "option c", "c", "तीन", "३"],
    20: ["3", "three", "option c", "c", "face opposite to 1 is 3", "3 is opposite to 1", "तीन", "३"],
    21: ["3", "three", "option a", "a", "तीन", "३"],
    22: ["5", "five", "option b", "b", "पाँच", "५"],
    23: ["3", "three", "option d", "d", "तीन", "३"],
    24: ["U", "u", "option c", "c", "यू"],
    25: ["(d)", "d", "option d", "statement d", "D is adjacent to F", "D and F are adjacent"],
    26: ["1 or 6", "either 1 or 6", "1/6", "option c", "c", "1 या 6"],
}

OBJECTIVE_QUESTION_CONFIG: dict[int, dict[str, object]] = {
    1: {
        "options": ["4", "5", "2", "1"],
        "correct_option_index": 1,
    },
    2: {
        "options": ["4", "6", "2", "5"],
        "correct_option_index": 0,
    },
}


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


def question_image_name(question_number: int) -> str:
    return f"dice1_q{question_number:02d}.jpg"


def source_page(question_number: int) -> Path:
    page_number = FIRST_QUESTION_PAGE + question_number - 1
    return OCR_DIR / f"page_{page_number:02d}.png"


def export_question_image(question_number: int) -> str:
    source = source_page(question_number)
    if not source.exists():
        raise FileNotFoundError(f"Missing OCR image for question {question_number}: {source}")

    IMAGES_DIR.mkdir(parents=True, exist_ok=True)
    output_name = question_image_name(question_number)
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


def parse_selected_questions(argv: list[str]) -> list[int]:
    if len(argv) == 1:
        return list(ALL_QUESTION_NUMBERS)

    selected: list[int] = []
    for raw_value in argv[1:]:
        question_number = int(raw_value)
        if question_number not in ALL_QUESTION_NUMBERS:
            raise ValueError(f"Question number out of range: {question_number}")
        if question_number not in selected:
            selected.append(question_number)
    if not selected:
        raise ValueError("No valid question numbers were provided.")
    return selected


def load_question_payloads(selected_questions: list[int]) -> list[dict[str, object]]:
    payloads: list[dict[str, object]] = []
    expected_paths = [CODEX_DIR / f"question_{question_number:02d}.json" for question_number in selected_questions]
    for path in expected_paths:
        if not path.exists():
            raise FileNotFoundError(f"Missing generated question file: {path}")
        payloads.append(json.loads(path.read_text(encoding="utf-8")))
    return payloads


def build_topic(payload: dict[str, object]) -> dict[str, object]:
    question_number = int(payload["question_number"])
    topic_title_en = str(payload["topic_title_en"]).strip()
    topic_title_hi = str(payload["topic_title_hi"]).strip()
    prompt_en = str(payload["question_prompt_en"]).strip()
    prompt_hi = str(payload["question_prompt_hi"]).strip()
    hint_en = ""
    hint_hi = ""
    support_en = str(payload["support_example_en"]).strip()
    support_hi = str(payload["support_example_hi"]).strip()
    solution_en = str(payload["solution_en"]).strip()
    solution_hi = str(payload["solution_hi"]).strip()
    objective_config = OBJECTIVE_QUESTION_CONFIG.get(question_number)

    if objective_config is not None:
        prompt_en = strip_inline_options(prompt_en)
        prompt_hi = strip_inline_options(prompt_hi)

    subtopic_en = f"Question {question_number:02d}: {topic_title_en}"
    subtopic_hi = f"प्रश्न {question_number:02d}: {topic_title_hi}"
    explanation_en = [f"Question: {prompt_en}"]
    explanation_hi = [f"प्रश्न: {prompt_hi}"]
    if hint_en:
        explanation_en.append(f"Hint: {hint_en}")
    if hint_hi:
        explanation_hi.append(f"संकेत: {hint_hi}")
    explanation_en.extend(split_solution(solution_en))
    explanation_hi.extend(split_solution(solution_hi))

    reteach_paragraphs = [
        localized(english or hindi or "", hindi or english or "")
        for english, hindi in zip_longest(split_solution(solution_en), split_solution(solution_hi))
    ]
    if not reteach_paragraphs:
        reteach_paragraphs = [localized(solution_en, solution_hi)]

    image_name = export_question_image(question_number)
    accepted_answers = ACCEPTED_ANSWERS[question_number]

    return {
        "id": f"dice1_q{question_number:02d}_{slug(topic_title_en) or 'question'}",
        "sourceLessonId": "verbal_reasoning_dice1",
        "chapterNumber": 1,
        "chapterTitle": localized(CHAPTER_TITLE_EN, CHAPTER_TITLE_HI),
        "lessonTitle": localized(CHAPTER_TITLE_EN, CHAPTER_TITLE_HI),
        "subtopicTitle": localized(subtopic_en, subtopic_hi),
        "knowPrompt": localized(
            f"Can you solve Question {question_number:02d} from Dice and Cube?",
            f"क्या आप डाइस और क्यूब का प्रश्न {question_number:02d} हल कर सकते हैं?",
        ),
        "explanationTitle": localized(topic_title_en, topic_title_hi),
        "explanationParagraphs": [
            localized(english or hindi or "", hindi or english or "")
            for english, hindi in zip_longest(explanation_en, explanation_hi)
        ],
        "examples": [localized(support_en, support_hi)],
        "visuals": [],
        "questions": [
            {
                "id": f"dice1_q{question_number:02d}",
                "prompt": localized(prompt_en, prompt_hi),
                "type": "MULTIPLE_CHOICE" if objective_config is not None else "TEXT_INPUT",
                "options": [
                    localized(str(option), str(option))
                    for option in objective_config.get("options", [])
                ] if objective_config is not None else [],
                "correctOptionIndex": objective_config.get("correct_option_index") if objective_config is not None else None,
                "acceptedAnswers": accepted_answers,
                "hint": localized(hint_en, hint_hi),
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
            localized(CHAPTER_TITLE_EN, CHAPTER_TITLE_HI),
            localized(f"Question {question_number:02d}", f"प्रश्न {question_number:02d}"),
        ],
        "mistakeFocus": "PATTERN_RULE",
    }


def build_book(selected_questions: list[int]) -> dict[str, object]:
    clear_images_dir()
    payloads = load_question_payloads(selected_questions)
    topics = [build_topic(payload) for payload in payloads]
    return {
        "id": BOOK_ID,
        "subjectTitle": localized(SUBJECT_TITLE_EN, SUBJECT_TITLE_HI),
        "bookTitle": localized(CHAPTER_TITLE_EN, CHAPTER_TITLE_HI),
        "teacherNote": localized(
            "Use one question at a time. Ask the learner to compare adjacent and opposite faces before checking the bilingual solution.",
            "एक समय में एक प्रश्न कराइए। हल देखने से पहले विद्यार्थी से आसन्न और विपरीत फलकों की तुलना करवाइए।",
        ),
        "topics": topics,
    }


def write_catalog() -> None:
    save_json(
        CATALOG_PATH,
        [
            {
                "id": BOOK_ID,
                "title": localized(CHAPTER_TITLE_EN, CHAPTER_TITLE_HI),
                "assetPath": f"subject_packs/{BOOK_PATH.name}",
            }
        ],
    )


def remove_legacy_packs() -> None:
    for pack_name in LEGACY_PACK_NAMES:
        manifest_path = SUBJECT_PACKS_DIR / f"{pack_name}.json"
        if manifest_path.exists():
            manifest_path.unlink()

        pack_dir = SUBJECT_PACKS_DIR / pack_name
        if pack_dir.exists():
            shutil.rmtree(pack_dir)


def main() -> int:
    selected_questions = parse_selected_questions(sys.argv)
    remove_legacy_packs()
    book = build_book(selected_questions)
    save_book(BOOK_PATH, book)
    write_catalog()
    print(f"Wrote {len(book['topics'])} Dice and Cube topics to {BOOK_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
