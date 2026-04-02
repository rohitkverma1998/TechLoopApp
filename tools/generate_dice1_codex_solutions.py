from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OCR_DIR = ROOT / "tmp_pages" / "dice1_ocr"
SCHEMA_PATH = ROOT / "tmp_pages" / "dice_solution_schema.json"
OUTPUT_DIR = ROOT / "tmp_pages" / "dice1_codex"

FIRST_QUESTION_PAGE = 11
LAST_QUESTION_PAGE = 36


NOISE_PATTERNS = (
    "reasoningby",
    "reasoning by",
    "zero tohero",
    "zero to hero",
    "zerohero",
    "classes",
    "dice",
)


def cleaned_lines(page_number: int) -> list[str]:
    text = (OCR_DIR / f"page_{page_number:02d}.txt").read_text(encoding="utf-8")
    lines = []
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        compact = re.sub(r"\s+", " ", line)
        lowered = compact.lower()
        if any(pattern in lowered for pattern in NOISE_PATTERNS):
            continue
        if re.fullmatch(r"[\W_]+", compact):
            continue
        if re.fullmatch(r"[A-Da-d][.)]?", compact):
            continue
        lines.append(compact)
    return lines


def prompt_seed(page_number: int) -> str:
    lines = cleaned_lines(page_number)
    question_lines: list[str] = []
    for line in lines:
        if re.fullmatch(r"\d+[.)]?", line):
            continue
        question_lines.append(line)
        joined = " ".join(question_lines)
        if "?" in joined or len(joined) > 90:
            break

    seed = " ".join(question_lines).strip()
    seed = re.sub(r"\s+", " ", seed)
    seed = re.sub(r"^[\d.)\s-]+", "", seed)
    return seed or f"Question from Dice1 page {page_number}"


def build_prompt(page_number: int, seed: str) -> str:
    question_number = page_number - FIRST_QUESTION_PAGE + 1
    return (
        f'Solve "{seed}" for beginners in both English and Hindi. '
        "Use the attached image to read the exact dice question and solve it. "
        "Return only JSON matching the schema with these rules: "
        f"set question_number to {question_number}; "
        "write clean topic_title_en/topic_title_hi; "
        "write clean question_prompt_en/question_prompt_hi; "
        "accepted_answers must include short valid answer variants; "
        "final_answer must be the shortest final answer; "
        "hint_en/hint_hi should be one short helpful line; "
        "support_example_en/support_example_hi should be one short concept reminder; "
        "solution_en/solution_hi should explain the reasoning clearly using the shown dice positions."
    )


def run_codex(page_number: int) -> None:
    question_number = page_number - FIRST_QUESTION_PAGE + 1
    image_path = OCR_DIR / f"page_{page_number:02d}.png"
    output_path = OUTPUT_DIR / f"question_{question_number:02d}.json"
    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists():
        print(f"skip q{question_number:02d} (already exists)")
        return

    seed = prompt_seed(page_number)
    prompt = build_prompt(page_number, seed)
    command = [
        "codex",
        "exec",
        "--skip-git-repo-check",
        "--dangerously-bypass-approvals-and-sandbox",
        "-C",
        str(ROOT),
        "-i",
        str(image_path),
        "--output-schema",
        str(SCHEMA_PATH),
        "-o",
        str(output_path),
        prompt,
    ]
    print(f"run q{question_number:02d}: {seed}")
    subprocess.run(command, check=True)

    payload = json.loads(output_path.read_text(encoding="utf-8"))
    print(f"done q{question_number:02d}: {payload['final_answer']}")


def main(argv: list[str]) -> int:
    if len(argv) == 1:
        pages = range(FIRST_QUESTION_PAGE, LAST_QUESTION_PAGE + 1)
    else:
        pages = [int(value) for value in argv[1:]]

    for page_number in pages:
        run_codex(page_number)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
