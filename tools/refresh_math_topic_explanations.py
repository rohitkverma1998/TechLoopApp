from __future__ import annotations

import argparse
import json
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any

from codex_math_content import generate_existing_topic_content
from subject_pack_io import load_book, save_book


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_PACK_PATH = ROOT / "app" / "src" / "main" / "assets" / "subject_packs" / "class5_rs_aggarwal_math.json"
DEFAULT_STATE_PATH = ROOT / "tools" / ".refresh_math_topic_explanations_state.json"


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
    model: str,
    timeout_seconds: int,
) -> tuple[str, list[dict[str, str]], list[dict[str, str]]]:
    title = topic_title(topic)
    content = generate_existing_topic_content(
        topic,
        model=model,
        timeout_seconds=timeout_seconds,
    )
    return title, content["teaching_paragraphs"], content["solution_paragraphs"]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Refresh bilingual math topic teaching and solution paragraphs with Codex.")
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

    print(f"Refreshing {len(pending_entries)} topic content from {pack_path.name}")
    print(f"Workers: {args.max_workers} | Model: {args.model}")

    futures = {}
    with ThreadPoolExecutor(max_workers=args.max_workers) as executor:
        for index, topic in pending_entries:
            future = executor.submit(
                worker_payload,
                topic,
                model=args.model,
                timeout_seconds=args.timeout_seconds,
            )
            futures[future] = (index, topic)

        processed = 0
        total = len(futures)
        for future in as_completed(futures):
            index, topic = futures[future]
            topic_id = str(topic.get("id", ""))
            try:
                title, teaching_paragraphs, solution_paragraphs = future.result()
                pack["topics"][index]["explanationParagraphs"] = teaching_paragraphs
                questions = pack["topics"][index].get("questions") or []
                if questions:
                    questions[0]["reteachParagraphs"] = solution_paragraphs
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


if __name__ == "__main__":
    main()
