from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TMP_DIR = ROOT / "tmp_pages"
RENDER_ROOT = TMP_DIR / "dice_pages"
CODEX_ROOT = TMP_DIR / "dice_codex"

SUBJECT_PACKS_DIR = ROOT / "app" / "src" / "main" / "assets" / "subject_packs"
BOOK_PATH = SUBJECT_PACKS_DIR / "class5_rs_aggarwal_math.json"
BOOK_DIR = BOOK_PATH.with_suffix("")
IMAGES_DIR = BOOK_DIR / "images"
CATALOG_PATH = SUBJECT_PACKS_DIR / "catalog.json"
SCHEMA_PATH = ROOT / "tools" / "dice_page_solution_schema.json"

BOOK_ID = "verbal_reasoning_dice_and_cube"
SUBJECT_TITLE_EN = "Verbal Reasoning"
SUBJECT_TITLE_HI = "वर्बल रीजनिंग"
BOOK_TITLE_EN = "Dice and Cube"
BOOK_TITLE_HI = "डाइस और क्यूब"

LEGACY_PACK_NAMES = (
    "class5_science_story_lab",
    "class5_english_reading_trails",
)


@dataclass(frozen=True)
class DicePdfConfig:
    key: str
    pdf_path: Path
    chapter_number: int
    chapter_title_en: str
    chapter_title_hi: str
    source_lesson_id: str
    candidate_pages: tuple[int, ...]

    @property
    def render_dir(self) -> Path:
        return RENDER_ROOT / self.key

    @property
    def codex_dir(self) -> Path:
        return CODEX_ROOT / self.key

    @property
    def image_prefix(self) -> str:
        return self.key


DICE_PDF_CONFIGS: tuple[DicePdfConfig, ...] = (
    DicePdfConfig(
        key="dice1",
        pdf_path=ROOT / "Verbal_Reasoning" / "Dice_and_Cube" / "Dice1.pdf",
        chapter_number=1,
        chapter_title_en="Dice 1",
        chapter_title_hi="डाइस 1",
        source_lesson_id="verbal_reasoning_dice1",
        candidate_pages=tuple(range(11, 37)),
    ),
    DicePdfConfig(
        key="dice2",
        pdf_path=ROOT / "Verbal_Reasoning" / "Dice_and_Cube" / "Dice2.pdf",
        chapter_number=2,
        chapter_title_en="Dice 2",
        chapter_title_hi="डाइस 2",
        source_lesson_id="verbal_reasoning_dice2",
        candidate_pages=tuple(range(3, 36)),
    ),
    DicePdfConfig(
        key="dice3",
        pdf_path=ROOT / "Verbal_Reasoning" / "Dice_and_Cube" / "Dice3.pdf",
        chapter_number=3,
        chapter_title_en="Dice 3",
        chapter_title_hi="डाइस 3",
        source_lesson_id="verbal_reasoning_dice3",
        candidate_pages=tuple(range(13, 27)),
    ),
)

DICE_PDF_CONFIG_BY_KEY = {config.key: config for config in DICE_PDF_CONFIGS}


def selected_configs(argv: list[str]) -> list[DicePdfConfig]:
    if len(argv) <= 1:
        return list(DICE_PDF_CONFIGS)

    selected: list[DicePdfConfig] = []
    for raw_value in argv[1:]:
        key = raw_value.strip().lower()
        config = DICE_PDF_CONFIG_BY_KEY.get(key)
        if config is None:
            known = ", ".join(sorted(DICE_PDF_CONFIG_BY_KEY))
            raise ValueError(f"Unknown dice PDF key '{raw_value}'. Expected one of: {known}")
        if config not in selected:
            selected.append(config)
    return selected
