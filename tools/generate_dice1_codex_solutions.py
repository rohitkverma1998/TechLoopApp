from __future__ import annotations

import json
import subprocess
import sys

import fitz

from dice_pack_config import ROOT, SCHEMA_PATH, DicePdfConfig, selected_configs


RENDER_SCALE = 1.8


def render_candidate_pages(config: DicePdfConfig) -> None:
    config.render_dir.mkdir(parents=True, exist_ok=True)
    document = fitz.open(config.pdf_path)
    try:
        for page_number in config.candidate_pages:
            output_path = config.render_dir / f"page_{page_number:02d}.png"
            if output_path.exists():
                continue

            page = document.load_page(page_number - 1)
            pixmap = page.get_pixmap(matrix=fitz.Matrix(RENDER_SCALE, RENDER_SCALE), alpha=False)
            pixmap.save(output_path)
            print(f"rendered {config.key} page {page_number:02d}")
    finally:
        document.close()


def build_prompt(config: DicePdfConfig, page_number: int) -> str:
    return (
        f"Analyze the attached scanned page from {config.chapter_title_en} in a verbal reasoning dice and cube pack. "
        "If the page does not contain exactly one standalone exercise question, return JSON with is_question=false, "
        "question_number=null, blank strings for text fields, an empty options list, an empty accepted_answers list, "
        "correct_option_index=null, and question_type='TEXT_INPUT'. "
        "If it does contain one question, solve it for beginners in both English and Hindi and return only JSON matching the schema. "
        "Rules: extract the visible question number; write clean topic_title_en/topic_title_hi; "
        "question_prompt_en/question_prompt_hi should be the clean question without option labels; "
        "set question_type to MULTIPLE_CHOICE only when answer choices are visibly present on the page, otherwise TEXT_INPUT; "
        "options must contain only the option values in order; "
        "correct_option_index must be 0-based for MULTIPLE_CHOICE and null for TEXT_INPUT; "
        "accepted_answers must include short valid answer variants; "
        "final_answer must be the shortest correct answer; "
        "support_example_en/support_example_hi should be one short concept reminder; "
        "solution_en/solution_hi should explain the reasoning clearly using the shown figure or formula; "
        "do not add hint text; do not mention the teacher or that the page is already solved; "
        f"this is page {page_number} from {config.pdf_path.name}."
    )


def run_codex_for_page(config: DicePdfConfig, page_number: int) -> None:
    image_path = config.render_dir / f"page_{page_number:02d}.png"
    output_path = config.codex_dir / f"page_{page_number:02d}.json"
    output_path.parent.mkdir(parents=True, exist_ok=True)

    if output_path.exists():
        print(f"skip {config.key} page {page_number:02d}")
        return

    prompt = build_prompt(config, page_number)
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
    print(f"codex {config.key} page {page_number:02d}")
    completed = subprocess.run(
        command,
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if completed.returncode != 0:
        stderr_tail = completed.stderr.strip().splitlines()[-20:]
        stdout_tail = completed.stdout.strip().splitlines()[-20:]
        tail = "\n".join(line for line in stdout_tail + stderr_tail if line.strip())
        raise RuntimeError(
            f"Codex extraction failed for {config.key} page {page_number:02d}.\n{tail}"
        )

    payload = json.loads(output_path.read_text(encoding="utf-8"))
    status = "question" if payload.get("is_question") else "skip"
    print(f"done {config.key} page {page_number:02d}: {status}")


def main(argv: list[str]) -> int:
    configs = selected_configs(argv)
    for config in configs:
        render_candidate_pages(config)
        for page_number in config.candidate_pages:
            run_codex_for_page(config, page_number)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
