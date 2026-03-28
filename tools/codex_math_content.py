from __future__ import annotations

import hashlib
import json
import os
import subprocess
import tempfile
from pathlib import Path
from shutil import which
from threading import Lock
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MODEL = os.getenv("CODEX_CONTENT_MODEL", "gpt-5.4")
DEFAULT_TIMEOUT_SECONDS = int(os.getenv("CODEX_CONTENT_TIMEOUT_SECONDS", "600"))
CACHE_PATH = ROOT / "tools" / ".codex_math_content_cache.json"
SCHEMA_VERSION = 1

SCHEMA: dict[str, Any] = {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "additionalProperties": False,
    "properties": {
        "teaching_english_paragraphs": {
            "type": "array",
            "items": {"type": "string"},
            "minItems": 5,
            "maxItems": 10,
        },
        "teaching_hindi_paragraphs": {
            "type": "array",
            "items": {"type": "string"},
            "minItems": 5,
            "maxItems": 10,
        },
        "solution_english_paragraphs": {
            "type": "array",
            "items": {"type": "string"},
            "minItems": 5,
            "maxItems": 10,
        },
        "solution_hindi_paragraphs": {
            "type": "array",
            "items": {"type": "string"},
            "minItems": 5,
            "maxItems": 10,
        },
    },
    "required": [
        "teaching_english_paragraphs",
        "teaching_hindi_paragraphs",
        "solution_english_paragraphs",
        "solution_hindi_paragraphs",
    ],
}

_CACHE_LOCK = Lock()
_CACHE: dict[str, Any] | None = None


def loc(english: str, hindi: str) -> dict[str, str]:
    return {
        "english": english,
        "hindi": hindi,
    }


def generate_math_content(
    *,
    content_kind: str,
    chapter_title_en: str,
    chapter_title_hi: str,
    topic_title_en: str,
    topic_title_hi: str,
    question_prompt_en: str,
    question_prompt_hi: str,
    authoritative_answer_en: str,
    authoritative_answer_hi: str,
    question_type: str = "TEXT_INPUT",
    concept_en: str = "",
    concept_hi: str = "",
    hint_en: str = "",
    hint_hi: str = "",
    example_en: str = "",
    example_hi: str = "",
    support_en: str = "",
    support_hi: str = "",
    options: list[dict[str, str]] | None = None,
    notebook_task: bool = False,
    computed_steps: list[str] | None = None,
    model: str = DEFAULT_MODEL,
    timeout_seconds: int = DEFAULT_TIMEOUT_SECONDS,
) -> dict[str, list[dict[str, str]]]:
    payload = {
        "content_kind": content_kind,
        "chapter_title_en": chapter_title_en.strip(),
        "chapter_title_hi": chapter_title_hi.strip(),
        "topic_title_en": topic_title_en.strip(),
        "topic_title_hi": topic_title_hi.strip(),
        "question_prompt_en": question_prompt_en.strip(),
        "question_prompt_hi": question_prompt_hi.strip(),
        "authoritative_answer_en": authoritative_answer_en.strip(),
        "authoritative_answer_hi": authoritative_answer_hi.strip(),
        "question_type": question_type.strip(),
        "concept_en": concept_en.strip(),
        "concept_hi": concept_hi.strip(),
        "hint_en": hint_en.strip(),
        "hint_hi": hint_hi.strip(),
        "example_en": example_en.strip(),
        "example_hi": example_hi.strip(),
        "support_en": support_en.strip(),
        "support_hi": support_hi.strip(),
        "options": options or [],
        "notebook_task": notebook_task,
        "computed_steps": [step.strip() for step in computed_steps or [] if step.strip()],
    }
    prompt = build_prompt(payload)
    result = run_cached_codex_prompt(
        prompt=prompt,
        payload=payload,
        model=model,
        timeout_seconds=timeout_seconds,
    )
    return {
        "teaching_paragraphs": build_localized_paragraphs(
            result["teaching_english_paragraphs"],
            result["teaching_hindi_paragraphs"],
        ),
        "solution_paragraphs": build_localized_paragraphs(
            result["solution_english_paragraphs"],
            result["solution_hindi_paragraphs"],
        ),
    }


def generate_existing_topic_content(
    topic: dict[str, Any],
    *,
    model: str = DEFAULT_MODEL,
    timeout_seconds: int = DEFAULT_TIMEOUT_SECONDS,
) -> dict[str, list[dict[str, str]]]:
    question = first_question(topic)
    answer_en, answer_hi = authoritative_answer_from_question(question)
    return generate_math_content(
        content_kind="existing_topic",
        chapter_title_en=localized_value(topic.get("chapterTitle"), "english"),
        chapter_title_hi=localized_value(topic.get("chapterTitle"), "hindi"),
        topic_title_en=localized_value(topic.get("subtopicTitle"), "english"),
        topic_title_hi=localized_value(topic.get("subtopicTitle"), "hindi"),
        question_prompt_en=localized_value(question.get("prompt"), "english"),
        question_prompt_hi=localized_value(question.get("prompt"), "hindi"),
        authoritative_answer_en=answer_en,
        authoritative_answer_hi=answer_hi,
        question_type=str(question.get("type", "TEXT_INPUT")),
        concept_en=localized_value(topic.get("explanationTitle"), "english"),
        concept_hi=localized_value(topic.get("explanationTitle"), "hindi"),
        hint_en=localized_value(question.get("hint"), "english"),
        hint_hi=localized_value(question.get("hint"), "hindi"),
        example_en=localized_value(question.get("supportExample"), "english"),
        example_hi=localized_value(question.get("supportExample"), "hindi"),
        support_en=localized_value(question.get("wrongReason"), "english"),
        support_hi=localized_value(question.get("wrongReason"), "hindi"),
        options=question_options(question),
        notebook_task=False,
        model=model,
        timeout_seconds=timeout_seconds,
    )


def build_prompt(payload: dict[str, Any]) -> str:
    lines = [
        'Write a JSON object with "teaching_english_paragraphs", "teaching_hindi_paragraphs", "solution_english_paragraphs", and "solution_hindi_paragraphs".',
        "You are creating offline bilingual mathematics teaching content for class 5 students.",
        "Use this structured context exactly as the source of truth:",
        json.dumps(payload, ensure_ascii=False, indent=2),
        "Rules for the output:",
        "Teaching paragraphs must explain the topic or question in a calm, beginner-friendly way before the learner answers.",
        "Solution paragraphs must solve the exact question using the authoritative final answer from the context. Treat that answer as correct and do not contradict it.",
        "If the context includes computed_steps, use them closely but rewrite them into natural teaching language.",
        "If notebook_task is true, explain the notebook or construction process clearly and tell the learner to type done only after finishing neatly.",
        "If the question is multiple choice, explain why the authoritative answer is the correct option in simple terms.",
        "Keep both languages aligned paragraph by paragraph.",
        "Keep the content strictly suitable for class 5 mathematics and avoid advanced methods or terminology.",
        "Do not use bullets, headings, markdown, or labels like Step 1.",
        "Keep units, place value, and fraction forms accurate whenever they matter.",
        "Return only the JSON object that matches the schema.",
    ]
    return "\n".join(lines)


def run_cached_codex_prompt(
    *,
    prompt: str,
    payload: dict[str, Any],
    model: str,
    timeout_seconds: int,
) -> dict[str, list[str]]:
    cache_key = build_cache_key(payload=payload, model=model)
    cached = get_cached_result(cache_key)
    if cached is not None:
        return cached

    result = run_codex_prompt(
        prompt=prompt,
        model=model,
        timeout_seconds=timeout_seconds,
    )
    set_cached_result(cache_key, result)
    return result


def build_cache_key(*, payload: dict[str, Any], model: str) -> str:
    raw = json.dumps(
        {
            "schema_version": SCHEMA_VERSION,
            "model": model,
            "payload": payload,
        },
        ensure_ascii=False,
        sort_keys=True,
    )
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def get_cached_result(cache_key: str) -> dict[str, list[str]] | None:
    with _CACHE_LOCK:
        cache = load_cache()
        payload = cache.get(cache_key)
        if not isinstance(payload, dict):
            return None
        return payload


def set_cached_result(cache_key: str, result: dict[str, list[str]]) -> None:
    with _CACHE_LOCK:
        cache = load_cache()
        cache[cache_key] = result
        save_cache(cache)


def load_cache() -> dict[str, Any]:
    global _CACHE
    if _CACHE is not None:
        return _CACHE
    if CACHE_PATH.exists():
        _CACHE = json.loads(CACHE_PATH.read_text(encoding="utf-8"))
    else:
        _CACHE = {}
    return _CACHE


def save_cache(cache: dict[str, Any]) -> None:
    CACHE_PATH.write_text(json.dumps(cache, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def run_codex_prompt(
    *,
    prompt: str,
    model: str,
    timeout_seconds: int,
) -> dict[str, list[str]]:
    codex_bin = which("codex")
    if not codex_bin:
        raise RuntimeError("codex CLI was not found in PATH.")

    with tempfile.NamedTemporaryFile(prefix="codex_math_schema_", suffix=".json", mode="w", encoding="utf-8", delete=False) as schema_file:
        schema_path = Path(schema_file.name)
        schema_file.write(json.dumps(SCHEMA))

    with tempfile.NamedTemporaryFile(prefix="codex_math_output_", suffix=".json", delete=False) as output_file:
        output_path = Path(output_file.name)

    try:
        command = [
            codex_bin,
            "exec",
            "-C",
            str(ROOT),
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
            encoding="utf-8",
            errors="replace",
            timeout=timeout_seconds,
            check=False,
        )
        if completed.returncode != 0:
            raise RuntimeError(
                f"codex exec failed with exit code {completed.returncode}\nSTDOUT:\n{completed.stdout}\nSTDERR:\n{completed.stderr}"
            )

        payload = json.loads(output_path.read_text(encoding="utf-8"))
        teaching_english = normalize_paragraphs(payload.get("teaching_english_paragraphs", []))
        teaching_hindi = normalize_paragraphs(payload.get("teaching_hindi_paragraphs", []))
        solution_english = normalize_paragraphs(payload.get("solution_english_paragraphs", []))
        solution_hindi = normalize_paragraphs(payload.get("solution_hindi_paragraphs", []))
        if not teaching_english or not teaching_hindi or not solution_english or not solution_hindi:
            raise RuntimeError(f"codex returned empty content: {payload}")

        teaching_count = min(len(teaching_english), len(teaching_hindi))
        solution_count = min(len(solution_english), len(solution_hindi))
        return {
            "teaching_english_paragraphs": teaching_english[:teaching_count],
            "teaching_hindi_paragraphs": teaching_hindi[:teaching_count],
            "solution_english_paragraphs": solution_english[:solution_count],
            "solution_hindi_paragraphs": solution_hindi[:solution_count],
        }
    finally:
        schema_path.unlink(missing_ok=True)
        output_path.unlink(missing_ok=True)


def build_localized_paragraphs(english_values: list[str], hindi_values: list[str]) -> list[dict[str, str]]:
    return [
        loc(english, hindi)
        for english, hindi in zip(english_values, hindi_values)
    ]


def normalize_paragraphs(values: list[str]) -> list[str]:
    normalized: list[str] = []
    for value in values:
        text = " ".join(str(value).split()).strip()
        if text:
            normalized.append(text)
    return normalized


def first_question(topic: dict[str, Any]) -> dict[str, Any]:
    questions = topic.get("questions") or []
    return questions[0] if questions else {}


def question_options(question: dict[str, Any]) -> list[dict[str, str]]:
    options = question.get("options") or []
    return [
        {
            "english": localized_value(option, "english"),
            "hindi": localized_value(option, "hindi"),
        }
        for option in options
    ]


def authoritative_answer_from_question(question: dict[str, Any]) -> tuple[str, str]:
    question_type = str(question.get("type", "TEXT_INPUT"))
    if question_type == "MULTIPLE_CHOICE":
        options = question_options(question)
        try:
            correct_index = int(question.get("correctOptionIndex"))
        except (TypeError, ValueError):
            correct_index = 0
        option = options[correct_index] if 0 <= correct_index < len(options) else {"english": "", "hindi": ""}
        return option["english"], option["hindi"]

    accepted_answers = question.get("acceptedAnswers") or []
    answer = str(accepted_answers[0]).strip() if accepted_answers else ""
    return answer, answer


def localized_value(block: Any, key: str) -> str:
    if isinstance(block, dict):
        return str(block.get(key, "")).strip()
    return str(block or "").strip()
