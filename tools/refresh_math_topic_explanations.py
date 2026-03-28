from __future__ import annotations

import argparse
import json
import subprocess
import tempfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from shutil import which
from typing import Any

from subject_pack_io import load_book, save_book


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_PACK_PATH = ROOT / "app" / "src" / "main" / "assets" / "subject_packs" / "class5_rs_aggarwal_math.json"
DEFAULT_STATE_PATH = ROOT / "tools" / ".refresh_math_topic_explanations_state.json"

SCHEMA: dict[str, Any] = {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "additionalProperties": False,
    "properties": {
        "english_paragraphs": {
            "type": "array",
            "items": {"type": "string"},
            "minItems": 6,
            "maxItems": 12,
        },
        "hindi_paragraphs": {
            "type": "array",
            "items": {"type": "string"},
            "minItems": 6,
            "maxItems": 12,
        },
    },
    "required": ["english_paragraphs", "hindi_paragraphs"],
}


def load_json_file(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def save_json_file(path: Path, payload: dict[str, Any]) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def ensure_state(path: Path, *, reset: bool) -> dict[str, Any]:
    if reset or not path.exists():
        state = {"completed_topic_ids": [], "failed_topic_ids": {}, "pack_path": str(DEFAULT_PACK_PATH)}
        save_json_file(path, state)
        return state
    return load_json_file(path)


def topic_title(topic: dict[str, Any]) -> str:
    for key in ("subtopicTitle", "explanationTitle", "lessonTitle"):
        block = topic.get(key)
        if isinstance(block, dict):
            value = str(block.get("english", "")).strip()
            if value:
                return value
    return str(topic.get("id", "Unknown Topic"))


def topic_context_prompt(topic: dict[str, Any]) -> str:
    questions = topic.get("questions") or []
    if not questions:
        return ""
    prompt_block = questions[0].get("prompt")
    if isinstance(prompt_block, dict):
        return str(prompt_block.get("english", "")).strip()
    return str(prompt_block or "").strip()


def build_prompt(title: str, context_prompt: str) -> str:
    exact_request = (
        f"I am a beginner at maths. Explain {title} in detail with the formula, real-life examples, "
        "and 3 worked solutions from easy to hard. Show every step clearly and point out common mistakes"
    )
    lines = [
        f'Write a JSON object with "english_paragraphs" and "hindi_paragraphs" for the maths topic "{title}".',
        f'Use this exact English teaching request to shape the explanation: "{exact_request}"',
    ]
    if context_prompt:
        lines.append(f'Use this practice question only as extra context when helpful: "{context_prompt}"')
    lines.extend(
        [
            "Keep both languages beginner-friendly and natural.",
            "Keep the explanation strictly suitable for class 5 students.",
            "No bullets, no headings, no labels, and no markdown.",
            "Ensure the English and Hindi arrays have the same number of paragraphs and correspond in order.",
            "Use formulas only when the topic truly has one. If a topic is conceptual, explain the key rule or idea clearly instead of inventing a formula.",
            "The worked solutions should stay accurate for the topic and move from easy to hard.",
            "Do not use coordinate geometry, slope, equations of lines, x-axis, y-axis, algebraic proofs, negative reciprocals, or any advanced ideas beyond class 5.",
            "Return only the JSON object that matches the schema.",
        ]
    )
    return "\n".join(lines)


def normalize_paragraphs(values: list[str]) -> list[str]:
    normalized: list[str] = []
    for value in values:
        text = " ".join(str(value).split()).strip()
        if text:
            normalized.append(text)
    return normalized


def run_codex_prompt(
    *,
    prompt: str,
    schema_path: Path,
    model: str,
    workdir: Path,
    timeout_seconds: int,
) -> dict[str, list[str]]:
    codex_bin = which("codex")
    if not codex_bin:
        raise RuntimeError("codex CLI was not found in PATH.")

    with tempfile.NamedTemporaryFile(prefix="topic_explanation_", suffix=".json", delete=False) as output_file:
        output_path = Path(output_file.name)

    try:
        command = [
            codex_bin,
            "exec",
            "-C",
            str(workdir),
            "--sandbox",
            "read-only",
            "--ephemeral",
            "--color",
            "never",
            "--model",
            model,
            "--output-schema",
            str(schema_path),
            "-o",
            str(output_path),
            prompt,
        ]
        completed = subprocess.run(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=timeout_seconds,
            check=False,
        )
        if completed.returncode != 0:
            raise RuntimeError(
                f"codex exec failed with exit code {completed.returncode}\nSTDOUT:\n{completed.stdout}\nSTDERR:\n{completed.stderr}"
            )

        payload = json.loads(output_path.read_text(encoding="utf-8"))
        english = normalize_paragraphs(list(payload.get("english_paragraphs", [])))
        hindi = normalize_paragraphs(list(payload.get("hindi_paragraphs", [])))
        if not english or not hindi:
            raise RuntimeError(f"codex returned empty paragraphs: {payload}")
        count = min(len(english), len(hindi))
        return {
            "english_paragraphs": english[:count],
            "hindi_paragraphs": hindi[:count],
        }
    finally:
        output_path.unlink(missing_ok=True)


def build_explanation_paragraphs(result: dict[str, list[str]]) -> list[dict[str, str]]:
    return [
        {"english": english, "hindi": hindi}
        for english, hindi in zip(result["english_paragraphs"], result["hindi_paragraphs"])
    ]


def selected_topic_entries(
    pack: dict[str, Any],
    *,
    include_exercises: bool,
    only_topic_ids: set[str] | None,
    limit: int | None,
) -> list[tuple[int, dict[str, Any]]]:
    selected: list[tuple[int, dict[str, Any]]] = []
    for index, topic in enumerate(pack.get("topics", [])):
        topic_id = str(topic.get("id", ""))
        if not include_exercises and topic_id.startswith("rs_ex_"):
            continue
        if only_topic_ids is not None and topic_id not in only_topic_ids:
            continue
        selected.append((index, topic))
    if limit is not None:
        return selected[:limit]
    return selected


def worker_payload(
    topic: dict[str, Any],
    *,
    schema_path: Path,
    model: str,
    workdir: Path,
    timeout_seconds: int,
) -> tuple[str, list[dict[str, str]]]:
    title = topic_title(topic)
    prompt = build_prompt(title, topic_context_prompt(topic))
    result = run_codex_prompt(
        prompt=prompt,
        schema_path=schema_path,
        model=model,
        workdir=workdir,
        timeout_seconds=timeout_seconds,
    )
    return title, build_explanation_paragraphs(result)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Refresh beginner-friendly bilingual explanations for math topics.")
    parser.add_argument("--pack", type=Path, default=DEFAULT_PACK_PATH)
    parser.add_argument("--state", type=Path, default=DEFAULT_STATE_PATH)
    parser.add_argument("--model", default="gpt-5.4")
    parser.add_argument("--max-workers", type=int, default=3)
    parser.add_argument("--timeout-seconds", type=int, default=600)
    parser.add_argument("--limit", type=int, default=None)
    parser.add_argument("--include-exercises", action="store_true")
    parser.add_argument("--only-topic-id", action="append", dest="only_topic_ids")
    parser.add_argument("--reset-state", action="store_true")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    pack_path: Path = args.pack.resolve()
    state_path: Path = args.state.resolve()
    state_path.parent.mkdir(parents=True, exist_ok=True)

    state = ensure_state(state_path, reset=args.reset_state)
    pack = load_book(pack_path)
    only_topic_ids = set(args.only_topic_ids) if args.only_topic_ids else None
    pending_entries = selected_topic_entries(
        pack,
        include_exercises=args.include_exercises,
        only_topic_ids=only_topic_ids,
        limit=args.limit,
    )
    completed_ids = set(state.get("completed_topic_ids", []))
    pending_entries = [
        (index, topic)
        for index, topic in pending_entries
        if str(topic.get("id", "")) not in completed_ids
    ]

    if not pending_entries:
        print("No pending topics to refresh.")
        return

    with tempfile.NamedTemporaryFile(prefix="topic_explanation_schema_", suffix=".json", mode="w", encoding="utf-8", delete=False) as schema_file:
        schema_path = Path(schema_file.name)
        schema_file.write(json.dumps(SCHEMA))

    print(f"Refreshing {len(pending_entries)} topic explanations from {pack_path.name}")
    print(f"Workers: {args.max_workers} | Model: {args.model}")

    try:
        futures = {}
        with ThreadPoolExecutor(max_workers=args.max_workers) as executor:
            for index, topic in pending_entries:
                future = executor.submit(
                    worker_payload,
                    topic,
                    schema_path=schema_path,
                    model=args.model,
                    workdir=ROOT,
                    timeout_seconds=args.timeout_seconds,
                )
                futures[future] = (index, topic)

            processed = 0
            total = len(futures)
            for future in as_completed(futures):
                index, topic = futures[future]
                topic_id = str(topic.get("id", ""))
                try:
                    title, explanation_paragraphs = future.result()
                    pack["topics"][index]["explanationParagraphs"] = explanation_paragraphs
                    completed_list = list(state.get("completed_topic_ids", []))
                    if topic_id not in completed_list:
                        completed_list.append(topic_id)
                    state["completed_topic_ids"] = completed_list
                    failed = dict(state.get("failed_topic_ids", {}))
                    failed.pop(topic_id, None)
                    state["failed_topic_ids"] = failed
                    save_book(pack_path, pack)
                    save_json_file(state_path, state)
                    processed += 1
                    print(f"[{processed}/{total}] refreshed {topic_id} -> {title}")
                except Exception as exc:  # pragma: no cover - long-running external command errors
                    failed = dict(state.get("failed_topic_ids", {}))
                    failed[topic_id] = str(exc)
                    state["failed_topic_ids"] = failed
                    save_json_file(state_path, state)
                    print(f"[error] {topic_id}: {exc}")

        if state.get("failed_topic_ids"):
            raise SystemExit(f"Finished with failures. See {state_path}")
    finally:
        schema_path.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
