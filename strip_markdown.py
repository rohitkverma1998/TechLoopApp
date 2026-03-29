#!/usr/bin/env python3
"""
Strips markdown formatting from all text fields in all 25 chapter JSON files.
Converts: **bold** → bold, *italic* → italic, `code` → code,
          ### Header → Header, | table | → cleaned, etc.
"""

import json
import re
import glob
import os

JSON_DIR = os.path.join(
    os.path.dirname(__file__),
    "app", "src", "main", "assets",
    "subject_packs", "class5_rs_aggarwal_math"
)


def strip_md(text: str) -> str:
    if not isinstance(text, str):
        return text

    # Remove markdown table rows entirely (lines that start/end with |)
    lines = text.split("\n")
    cleaned_lines = []
    for line in lines:
        stripped = line.strip()
        # Skip table separator rows like |---|---|
        if re.match(r'^\|[-| :]+\|$', stripped):
            continue
        # Convert table rows to simple bullet lines
        if stripped.startswith("|") and stripped.endswith("|"):
            cells = [c.strip() for c in stripped.strip("|").split("|") if c.strip()]
            if cells:
                cleaned_lines.append("  ".join(cells))
            continue
        cleaned_lines.append(line)
    text = "\n".join(cleaned_lines)

    # Remove bold: **text** or __text__
    text = re.sub(r'\*\*(.+?)\*\*', r'\1', text)
    text = re.sub(r'__(.+?)__', r'\1', text)

    # Remove italic: *text* or _text_
    text = re.sub(r'\*(.+?)\*', r'\1', text)
    text = re.sub(r'_(.+?)_', r'\1', text)

    # Remove inline code: `text`
    text = re.sub(r'`(.+?)`', r'\1', text)

    # Remove heading markers at start of line: ### text → text
    text = re.sub(r'^#{1,6}\s+', '', text, flags=re.MULTILINE)

    # Clean up excessive blank lines (max 1 blank line between paragraphs)
    text = re.sub(r'\n{3,}', '\n\n', text)

    return text.strip()


def clean_localized(obj):
    """Recursively strip markdown from any dict with 'english'/'hindi' keys, or any string."""
    if isinstance(obj, str):
        return strip_md(obj)
    if isinstance(obj, list):
        return [clean_localized(item) for item in obj]
    if isinstance(obj, dict):
        return {k: clean_localized(v) for k, v in obj.items()}
    return obj


def process_file(json_path: str):
    with open(json_path, encoding="utf-8") as f:
        data = json.load(f)

    data = clean_localized(data)

    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def main():
    json_files = sorted(glob.glob(os.path.join(JSON_DIR, "chapter_*.json")))
    print(f"Stripping markdown from {len(json_files)} JSON files...")
    for path in json_files:
        process_file(path)
        print(f"  Cleaned: {os.path.basename(path)}")
    print("Done.")


if __name__ == "__main__":
    main()
