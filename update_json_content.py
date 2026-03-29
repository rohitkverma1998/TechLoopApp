#!/usr/bin/env python3
"""
Parses BOOK_CONTENT_GENERATED.md and updates all 25 chapter JSON files
with bilingual (English + Hindi) explanations and solutions.
"""

import json
import re
import os
import glob

MD_FILE = os.path.join(os.path.dirname(__file__), "BOOK_CONTENT_GENERATED.md")
JSON_DIR = os.path.join(
    os.path.dirname(__file__),
    "app", "src", "main", "assets",
    "subject_packs", "class5_rs_aggarwal_math"
)


def clean(text: str) -> str:
    """Strip leading/trailing whitespace and markdown bold markers."""
    return text.strip()


def split_paragraphs(text: str) -> list:
    """Split text into non-empty paragraphs on blank lines."""
    paras = [p.strip() for p in re.split(r'\n{2,}', text)]
    return [p for p in paras if p]


def parse_markdown(md_path: str) -> dict:
    """
    Returns:
        chapters: {chapter_num (int): [topic_dict, ...]}
        topic_dict keys: title, explanation_en, explanation_hi, examples_en, examples_hi, questions
        question keys: prompt_en, solution_en, solution_hi
    """
    with open(md_path, encoding="utf-8") as f:
        raw = f.read()

    chapters = {}

    # Split by top-level chapter headings  "# Chapter N:" or "# Chapter N :"
    ch_split = re.split(r'\n# Chapter\s+(\d+)\s*[:\-–]', raw)
    # ch_split[0] = preamble, then pairs (num, content)

    for i in range(1, len(ch_split), 2):
        ch_num = int(ch_split[i])
        ch_body = ch_split[i + 1] if i + 1 < len(ch_split) else ""

        topics = []

        # Split by "## Topic:" lines
        topic_split = re.split(r'\n## Topic\s*[:\-–]?\s*(.+)', ch_body)
        # topic_split[0] = intro, then pairs (title, content)

        for j in range(1, len(topic_split), 2):
            topic_title = topic_split[j].strip()
            # Remove inline Hindi (e.g. "Foo / फू" → keep the whole thing as title)
            topic_body = topic_split[j + 1] if j + 1 < len(topic_split) else ""

            # ---- English explanation ----
            en_match = re.search(
                r'###\s*Explanation\s*\(English\)\s*\n(.*?)(?=###\s*Explanation\s*\(Hindi\)|###\s*Q[:\s]|^---|\Z)',
                topic_body, re.DOTALL | re.MULTILINE
            )
            explanation_en = clean(en_match.group(1)) if en_match else ""

            # ---- Hindi explanation ----
            hi_match = re.search(
                r'###\s*(?:Explanation\s*\(Hindi\)|व्याख्या)\s*\n(.*?)(?=###\s*Q[:\s]|^---|\Z)',
                topic_body, re.DOTALL | re.MULTILINE
            )
            explanation_hi = clean(hi_match.group(1)) if hi_match else ""

            # ---- Questions ----
            questions = []
            q_split = re.split(r'\n###\s*Q\s*[:\-–]?\s*(.+)', topic_body)
            for k in range(1, len(q_split), 2):
                q_prompt = q_split[k].strip()
                q_body = q_split[k + 1] if k + 1 < len(q_split) else ""

                sol_en_match = re.search(
                    r'####\s*Solution\s*\(English\)\s*\n(.*?)(?=####\s*Solution\s*\(Hindi\)|^---|\Z)',
                    q_body, re.DOTALL | re.MULTILINE
                )
                sol_hi_match = re.search(
                    r'####\s*Solution\s*\(Hindi\)\s*\n(.*?)(?=^---|\Z)',
                    q_body, re.DOTALL | re.MULTILINE
                )
                sol_en = clean(sol_en_match.group(1)) if sol_en_match else ""
                sol_hi = clean(sol_hi_match.group(1)) if sol_hi_match else ""

                questions.append({
                    "prompt_en": q_prompt,
                    "solution_en": sol_en,
                    "solution_hi": sol_hi,
                })

            topics.append({
                "title": topic_title,
                "explanation_en": explanation_en,
                "explanation_hi": explanation_hi,
                "questions": questions,
            })

        chapters[ch_num] = topics

    return chapters


def make_localized(english: str, hindi: str) -> dict:
    return {"english": english, "hindi": hindi if hindi else english}


def paragraphs_to_localized(en_text: str, hi_text: str) -> list:
    """
    Pair English and Hindi paragraphs. If counts match, pair 1-to-1.
    Otherwise, create one combined localized object.
    """
    en_paras = split_paragraphs(en_text)
    hi_paras = split_paragraphs(hi_text)

    if not en_paras:
        return []

    if len(en_paras) == len(hi_paras):
        return [make_localized(e, h) for e, h in zip(en_paras, hi_paras)]

    # Counts don't match — use full blocks as single items
    return [make_localized(
        "\n\n".join(en_paras),
        "\n\n".join(hi_paras) if hi_paras else "\n\n".join(en_paras)
    )]


def update_json_file(json_path: str, md_topics: list):
    with open(json_path, encoding="utf-8") as f:
        data = json.load(f)

    json_topics = data.get("topics", [])

    # Match by position (markdown was generated in same order as JSON)
    for idx, json_topic in enumerate(json_topics):
        if idx >= len(md_topics):
            print(f"  WARNING: more JSON topics than markdown topics at index {idx}")
            break

        md_topic = md_topics[idx]

        # Always overwrite explanationParagraphs with generated content
        paras = paragraphs_to_localized(
            md_topic["explanation_en"],
            md_topic["explanation_hi"]
        )
        if paras:
            json_topic["explanationParagraphs"] = paras

        # Always overwrite examples
        if True:
            # Extract example paragraphs that mention "Example"
            en_paras = split_paragraphs(md_topic["explanation_en"])
            hi_paras = split_paragraphs(md_topic["explanation_hi"])
            en_examples = [p for p in en_paras if re.search(r'\bexample\b', p, re.I)]
            hi_examples = [p for p in hi_paras if re.search(r'\bउदाहरण\b|example', p, re.I)]

            if en_examples:
                if len(en_examples) == len(hi_examples):
                    json_topic["examples"] = [make_localized(e, h) for e, h in zip(en_examples, hi_examples)]
                else:
                    json_topic["examples"] = [make_localized(
                        "\n".join(en_examples),
                        "\n".join(hi_examples) if hi_examples else "\n".join(en_examples)
                    )]

        # Update questions: add reteachParagraphs, wrongReason, and supportExample from solutions
        json_questions = json_topic.get("questions", [])
        md_questions = md_topic.get("questions", [])

        for qi, jq in enumerate(json_questions):
            if qi >= len(md_questions):
                break
            mq = md_questions[qi]

            sol_en = mq.get("solution_en", "")
            sol_hi = mq.get("solution_hi", "") or sol_en
            if not sol_en:
                continue

            en_paras = split_paragraphs(sol_en)
            hi_paras = split_paragraphs(sol_hi)

            # Always overwrite all solution fields
            jq["reteachTitle"] = make_localized("Solution", "हल")

            if len(en_paras) == len(hi_paras):
                jq["reteachParagraphs"] = [make_localized(e, h) for e, h in zip(en_paras, hi_paras)]
            else:
                jq["reteachParagraphs"] = [make_localized(
                    "\n".join(en_paras),
                    "\n".join(hi_paras) if hi_paras else "\n".join(en_paras)
                )]

            jq["wrongReason"] = make_localized(sol_en, sol_hi)

            support_en = "\n".join(en_paras[:2])
            support_hi = "\n".join(hi_paras[:2]) if hi_paras else support_en
            jq["supportExample"] = make_localized(support_en, support_hi)

    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def main():
    print("Parsing markdown...")
    chapters = parse_markdown(MD_FILE)
    print(f"  Found {len(chapters)} chapters in markdown.")

    json_files = sorted(glob.glob(os.path.join(JSON_DIR, "chapter_*.json")))
    print(f"  Found {len(json_files)} JSON files.\n")

    updated = 0
    for json_path in json_files:
        fname = os.path.basename(json_path)
        # Extract chapter number from filename e.g. chapter_01_revision.json → 1
        m = re.match(r'chapter_(\d+)_', fname)
        if not m:
            print(f"  Skipping (unrecognised filename): {fname}")
            continue

        ch_num = int(m.group(1))
        md_topics = chapters.get(ch_num)

        if not md_topics:
            print(f"  WARNING: No markdown content for Chapter {ch_num} ({fname})")
            continue

        print(f"  Updating Chapter {ch_num}: {fname}  ({len(md_topics)} md topics)")
        update_json_file(json_path, md_topics)
        updated += 1

    print(f"\nDone. Updated {updated}/{len(json_files)} JSON files.")


if __name__ == "__main__":
    main()
