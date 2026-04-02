#!/usr/bin/env python3
"""
Generate missing question solutions by launching a fresh Codex shell session per question.

This updates `reteachTitle` and `reteachParagraphs` in the subject-pack JSON files so the
Android app can show a `See solution` button for wrong answers without relying on a live API.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import tempfile
import textwrap
import time
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parent.parent
PACK_ROOT = REPO_ROOT / "app" / "src" / "main" / "assets" / "subject_packs"
PROMPT_FILE = REPO_ROOT / "MASTER_BOOK_CONTENT_PROMPT.md"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--path",
        action="append",
        help="One or more JSON files to update. Defaults to all chapter_*.json files.",
    )
    parser.add_argument(
        "--all",
        action="store_true",
        help="Regenerate even when reteachParagraphs already exist.",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Stop after generating this many question solutions.",
    )
    parser.add_argument(
        "--match",
        default=None,
        help="Only process questions whose prompt/topic/id contains this text.",
    )
    parser.add_argument(
        "--model",
        default=None,
        help="Optional Codex model override, for example gpt-5.4-mini.",
    )
    parser.add_argument(
        "--delay-seconds",
        type=float,
        default=0.0,
        help="Optional delay between Codex calls.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print matching questions without calling Codex or changing files.",
    )
    parser.add_argument(
        "--max-errors",
        type=int,
        default=25,
        help="Abort after this many Codex generation failures.",
    )
    return parser.parse_args()


def load_solution_prompt_template() -> str:
    text = PROMPT_FILE.read_text(encoding="utf-8")
    marker = "## Generate Solution Prompt"
    if marker not in text:
        return "Solve [Question] for beginners in both English and Hindi"
    section = text.split(marker, 1)[1]
    for line in section.splitlines():
        stripped = line.strip().strip("`")
        if stripped:
            return stripped
    return "Solve [Question] for beginners in both English and Hindi"


def chapter_paths(selected_paths: list[str] | None) -> list[Path]:
    if selected_paths:
        return [Path(path).resolve() for path in selected_paths]
    return sorted(PACK_ROOT.glob("**/chapter_*.json"))


def split_paragraphs(text: str) -> list[str]:
    parts = [part.strip() for part in text.replace("\r\n", "\n").split("\n\n")]
    return [part for part in parts if part]


def localized_paragraphs(english: str, hindi: str) -> list[dict[str, str]]:
    english_parts = split_paragraphs(english)
    hindi_parts = split_paragraphs(hindi)
    if not english_parts:
        return []
    if len(english_parts) == len(hindi_parts):
        return [
            {"english": en, "hindi": hi}
            for en, hi in zip(english_parts, hindi_parts)
        ]
    return [{
        "english": "\n\n".join(english_parts),
        "hindi": "\n\n".join(hindi_parts) if hindi_parts else "\n\n".join(english_parts),
    }]


def question_matches(match_text: str | None, topic: dict, question: dict) -> bool:
    if not match_text:
        return True
    haystack = "\n".join(
        [
            topic.get("id", ""),
            topic.get("subtopicTitle", {}).get("english", ""),
            topic.get("subtopicTitle", {}).get("hindi", ""),
            question.get("id", ""),
            question.get("prompt", {}).get("english", ""),
            question.get("prompt", {}).get("hindi", ""),
        ]
    ).lower()
    return match_text.lower() in haystack


def build_question_text(topic: dict, question: dict) -> str:
    lines = [
        f"Topic (English): {topic.get('subtopicTitle', {}).get('english', '').strip()}",
        f"Topic (Hindi): {topic.get('subtopicTitle', {}).get('hindi', '').strip()}",
        f"Question (English): {question.get('prompt', {}).get('english', '').strip()}",
        f"Question (Hindi): {question.get('prompt', {}).get('hindi', '').strip()}",
        f"Question type: {question.get('type', '').strip()}",
    ]

    options = question.get("options") or []
    if options:
        lines.append("Options:")
        for index, option in enumerate(options):
            label = chr(ord("A") + index)
            lines.append(
                f"{label}. {option.get('english', '').strip()} || {option.get('hindi', '').strip()}"
            )

    accepted_answers = question.get("acceptedAnswers") or []
    if accepted_answers:
        lines.append("Accepted answer(s): " + " | ".join(answer.strip() for answer in accepted_answers))

    hint = question.get("hint")
    if hint:
        lines.append(f"Hint (English): {hint.get('english', '').strip()}")
        lines.append(f"Hint (Hindi): {hint.get('hindi', '').strip()}")

    if question.get("questionImageAsset"):
        lines.append(f"Image asset: {question['questionImageAsset']}")

    return "\n".join(line for line in lines if line.strip())


def codex_solution_prompt(template: str, topic: dict, question: dict) -> str:
    question_text = build_question_text(topic, question)
    style_goal = template.replace("[Question]", question.get("prompt", {}).get("english", "").strip())
    return textwrap.dedent(
        f"""
        Use this exact project style goal: {style_goal}

        Solve the following classroom question for a child. Keep the explanation correct, beginner-friendly, and step by step.
        Return JSON that matches the provided schema only.
        Put the full English solution in `english` and the full Hindi solution in `hindi`.
        End both with the final answer.

        {question_text}
        """
    ).strip()


def run_codex(prompt: str, model: str | None) -> dict[str, str]:
    schema = {
        "type": "object",
        "additionalProperties": False,
        "properties": {
            "english": {"type": "string"},
            "hindi": {"type": "string"},
        },
        "required": ["english", "hindi"],
    }

    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as schema_file:
        json.dump(schema, schema_file)
        schema_path = Path(schema_file.name)
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as output_file:
        output_path = Path(output_file.name)

    cmd = [
        "codex",
        "exec",
        "--ephemeral",
        "--skip-git-repo-check",
        "-C",
        str(REPO_ROOT),
        "-s",
        "read-only",
        "--output-schema",
        str(schema_path),
        "-o",
        str(output_path),
        "-",
    ]
    if model:
        cmd[2:2] = ["-m", model]

    try:
        completed = subprocess.run(
            cmd,
            input=prompt,
            text=True,
            capture_output=True,
            check=False,
            timeout=240,
        )
        if completed.returncode != 0:
            raise RuntimeError(completed.stderr.strip() or completed.stdout.strip() or "codex exec failed")
        return json.loads(output_path.read_text(encoding="utf-8"))
    finally:
        schema_path.unlink(missing_ok=True)
        output_path.unlink(missing_ok=True)


def process_file(path: Path, args: argparse.Namespace, template: str) -> tuple[int, bool, int]:
    data = json.loads(path.read_text(encoding="utf-8"))
    changed = False
    generated = 0
    errors = 0

    for topic in data.get("topics", []):
        for question in topic.get("questions", []):
            if not question_matches(args.match, topic, question):
                continue
            if not args.all and question.get("reteachParagraphs"):
                continue

            generated += 1
            prompt_preview = question.get("prompt", {}).get("english", "").strip()
            print(
                f"[{generated}] {path.name} :: {question.get('id')} :: {prompt_preview}",
                flush=True,
            )

            if args.dry_run:
                continue

            try:
                solution = run_codex(codex_solution_prompt(template, topic, question), args.model)
            except Exception as error:
                errors += 1
                print(
                    f"ERROR {path.name} :: {question.get('id')} :: {error}",
                    file=sys.stderr,
                    flush=True,
                )
                if errors >= args.max_errors:
                    if changed:
                        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
                    raise RuntimeError(f"Stopped after {errors} errors while processing {path}")
                continue

            question["reteachTitle"] = {"english": "Solution", "hindi": "हल"}
            question["reteachParagraphs"] = localized_paragraphs(
                solution["english"].strip(),
                solution["hindi"].strip(),
            )
            changed = True
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

            if args.delay_seconds > 0:
                time.sleep(args.delay_seconds)

            if args.limit is not None and generated >= args.limit:
                return generated, changed, errors

    return generated, changed, errors


def main() -> int:
    args = parse_args()
    template = load_solution_prompt_template()
    total_generated = 0
    changed_files = 0
    total_errors = 0

    for path in chapter_paths(args.path):
        generated_here, changed, errors_here = process_file(path, args, template)
        total_generated += generated_here
        total_errors += errors_here
        if changed:
            changed_files += 1
        if args.limit is not None and total_generated >= args.limit:
            break

    print(
        f"Processed {total_generated} question(s). Updated {changed_files} file(s). Errors: {total_errors}.",
        flush=True,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
