#!/usr/bin/env python3
"""
extract_pdf_questions.py

Comprehensive script to:
1. Extract all exercise questions (1-80) from RS Aggarwal Class V Maths PDF
2. Render figure images for image-based questions
3. Compare extracted questions against existing JSON chapter files
4. Save detailed report to pdf_extraction_report.json
5. Print summary to stdout
"""

import sys
import os
import re
import json

sys.stdout.reconfigure(encoding='utf-8')

import fitz  # PyMuPDF

# ─────────────────────────────────────────────────────────────────────────────
# CONFIGURATION
# ─────────────────────────────────────────────────────────────────────────────

BASE_DIR        = "C:/Users/abhil/OneDrive/Desktop/Book/TechLoopApp"
PDF_PATH        = os.path.join(BASE_DIR, "RS_Aggarwal_Class_V_Maths.pdf")
ASSETS_DIR      = os.path.join(BASE_DIR, "app/src/main/assets/subject_packs/class5_rs_aggarwal_math")
JSON_DIR        = ASSETS_DIR
IMAGES_DIR      = os.path.join(ASSETS_DIR, "images")
REPORT_PATH     = os.path.join(BASE_DIR, "pdf_extraction_report.json")

# Exercise → (start_page, end_page) — 0-indexed PDF pages
exercise_pages = {
    1:(6,13),   2:(14,19),  3:(20,23),  4:(24,25),  5:(26,31),
    6:(32,33),  7:(34,36),  8:(37,38),  9:(39,41),  10:(42,42),
    11:(43,43), 12:(44,48), 13:(49,50), 14:(51,55), 15:(56,56),
    16:(57,62), 17:(63,66), 18:(67,70), 19:(71,74), 20:(75,75),
    21:(76,84), 22:(85,87), 23:(88,88), 24:(89,91), 25:(92,95),
    26:(96,96), 27:(97,97), 28:(98,98), 29:(99,100), 30:(101,101),
    31:(102,104),32:(105,109),33:(110,110),34:(111,111),35:(112,114),
    36:(115,117),37:(118,119),38:(120,127),39:(128,130),40:(131,133),
    41:(134,137),42:(138,140),43:(141,142),44:(143,144),45:(145,146),
    46:(147,148),47:(149,150),48:(151,152),49:(153,153),50:(154,165),
    51:(166,169),52:(170,175),53:(176,177),54:(178,181),55:(182,183),
    56:(184,187),57:(188,189),58:(190,192),59:(193,198),60:(199,202),
    61:(203,206),62:(207,210),63:(211,215),64:(216,218),65:(219,223),
    66:(224,224),67:(225,227),68:(228,231),69:(232,234),70:(235,240),
    71:(241,243),72:(244,250),73:(251,258),74:(259,262),75:(263,272),
    76:(273,280),77:(281,288),78:(289,296),79:(298,303),80:(304,327),
}

# ─────────────────────────────────────────────────────────────────────────────
# EXERCISE → CHAPTER MAPPING
# RS Aggarwal Class 5 exercises are distributed across 25 chapters.
# This mapping was derived from chapter JSON sourceLessonIds.
# ─────────────────────────────────────────────────────────────────────────────

# We'll auto-detect from JSON files themselves.
# But also define a fallback order-based mapping.
# exercises 1..80 appear in chapters 1..25 in sequence.
# We derive it from the JSON data loaded below.

# ─────────────────────────────────────────────────────────────────────────────
# STEP 1: SETUP
# ─────────────────────────────────────────────────────────────────────────────

os.makedirs(IMAGES_DIR, exist_ok=True)
print(f"[INFO] Images directory: {IMAGES_DIR}")
print(f"[INFO] Opening PDF: {PDF_PATH}")
doc = fitz.open(PDF_PATH)
print(f"[INFO] PDF pages: {doc.page_count}")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 2: LOAD ALL JSON CHAPTER FILES
# ─────────────────────────────────────────────────────────────────────────────

print("\n[INFO] Loading JSON chapter files ...")
json_files = sorted([f for f in os.listdir(JSON_DIR) if f.startswith("chapter_") and f.endswith(".json")])
print(f"[INFO] Found {len(json_files)} chapter files")

# Build a map: exercise_num → list of exercise-question topics
# Also build: exercise_num → chapter_num
exercise_topics_map: dict[int, list[dict]] = {}  # ex_num → list of topic dicts
exercise_to_chapter: dict[int, int] = {}

for jf in json_files:
    jpath = os.path.join(JSON_DIR, jf)
    with open(jpath, encoding='utf-8') as fh:
        chapter_data = json.load(fh)
    ch_num = chapter_data.get("chapterNumber", 0)
    topics = chapter_data.get("topics", [])
    for topic in topics:
        tid = topic.get("id", "")
        # Pattern: rsexch{CH:02d}ex{EX:02d}q{Q:02d}...
        m = re.match(r'rsexch(\d+)ex(\d+)q(\d+)', tid)
        if m:
            ex_num = int(m.group(2))
            if ex_num not in exercise_topics_map:
                exercise_topics_map[ex_num] = []
                exercise_to_chapter[ex_num] = ch_num
            exercise_topics_map[ex_num].append(topic)

print(f"[INFO] Exercises found in JSON: {sorted(exercise_topics_map.keys())}")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 3: EXTRACT TEXT FROM PDF FOR EACH EXERCISE
# ─────────────────────────────────────────────────────────────────────────────

def get_exercise_text(doc, start_page, end_page):
    """Extract raw text from PDF pages start_page..end_page (inclusive, 0-indexed)."""
    parts = []
    for pg in range(start_page, end_page + 1):
        if pg < doc.page_count:
            parts.append(doc[pg].get_text())
    return "\n".join(parts)


def parse_questions_from_text(raw_text):
    """
    Parse numbered questions from raw PDF text.
    Returns list of dicts:
      {num, text, sub_parts: [{label, text}], has_image, raw}
    """
    # Normalize whitespace but preserve structure
    # Remove page headers/footers (common patterns like page numbers)
    lines = raw_text.split('\n')
    # Filter out standalone page numbers and short noise lines (e.g. "7", "N", "C")
    filtered = []
    for line in lines:
        stripped = line.strip()
        # Skip lines that are just page numbers (1-3 digits) or known header noise
        if re.match(r'^\d{1,3}$', stripped):
            continue
        if re.match(r'^[A-Z]$', stripped):  # single capital letters (layout artifacts)
            continue
        filtered.append(line)
    text = '\n'.join(filtered)

    # Split by question number at line start: "  1.\t", "  2.\t", etc.
    # Pattern: optional whitespace, digit(s), dot, whitespace
    q_pattern = re.compile(r'(?m)^\s{0,6}(\d{1,3})\.\s+')

    splits = list(q_pattern.finditer(text))
    questions = []

    for i, match in enumerate(splits):
        q_num = int(match.group(1))
        start = match.end()
        end = splits[i+1].start() if i+1 < len(splits) else len(text)
        q_text_raw = text[start:end].strip()

        # Parse sub-parts: (a), (b), (c), ...
        sub_parts = parse_sub_parts(q_text_raw)
        has_image = detect_image_question(q_text_raw, sub_parts)

        questions.append({
            "num": q_num,
            "text": q_text_raw[:300].strip(),  # truncate for readability
            "full_text": q_text_raw,
            "sub_parts": sub_parts,
            "has_image": has_image,
        })

    return questions


def parse_sub_parts(q_text):
    """
    Parse sub-parts like (a), (b), (c) from question text.
    Returns list of dicts: {label, text}
    """
    # Pattern: (a), (b), ... or \t(a)\t, etc.
    sub_pattern = re.compile(r'\(([a-zA-Z])\)\s*')
    parts = []
    matches = list(sub_pattern.finditer(q_text))

    for i, m in enumerate(matches):
        label = m.group(1).lower()
        text_start = m.end()
        text_end = matches[i+1].start() if i+1 < len(matches) else len(q_text)
        part_text = q_text[text_start:text_end].strip()
        parts.append({"label": label, "text": part_text})

    return parts


def detect_image_question(q_text, sub_parts):
    """
    Detect if a question likely has image-only sub-parts.
    Heuristic: sub-part text is empty or very short (just whitespace/tabs),
    or question text mentions 'figure', 'fig.', 'shape', 'diagram'.
    """
    image_keywords = re.compile(r'\b(fig|figure|diagram|shape|draw|given below|shown)\b', re.IGNORECASE)
    if image_keywords.search(q_text):
        return True

    # Check for sub-parts with no text (image-only)
    if sub_parts:
        empty_count = sum(1 for sp in sub_parts if len(sp['text'].strip()) < 5)
        if empty_count >= len(sub_parts) // 2 and len(sub_parts) > 0:
            return True

    return False


# ─────────────────────────────────────────────────────────────────────────────
# STEP 4: RENDER FIGURE IMAGES
# ─────────────────────────────────────────────────────────────────────────────

def render_exercise_images(doc, ex_num, start_page, end_page):
    """
    For an exercise, find pages with significant non-text content (images/figures)
    and render them. Save to IMAGES_DIR.
    Returns list of saved image paths.
    """
    saved = []
    for pg_idx in range(start_page, end_page + 1):
        if pg_idx >= doc.page_count:
            continue
        page = doc[pg_idx]

        # Check if page has image objects
        img_list = page.get_images(full=False)
        blocks = page.get_text('blocks')  # (x0, y0, x1, y1, text, block_no, block_type)

        # block_type=1 means image block
        image_blocks = [b for b in blocks if b[6] == 1]

        if img_list or image_blocks:
            # Render full page at 2x scale
            mat = fitz.Matrix(2, 2)
            pix = page.get_pixmap(matrix=mat)
            img_filename = f"ex{ex_num:02d}_pg{pg_idx:03d}.png"
            img_path = os.path.join(IMAGES_DIR, img_filename)
            pix.save(img_path)
            saved.append(img_filename)

    return saved


# ─────────────────────────────────────────────────────────────────────────────
# STEP 5: BUILD JSON QUESTIONS MAP FOR AN EXERCISE
# ─────────────────────────────────────────────────────────────────────────────

def get_json_questions_for_exercise(ex_num):
    """
    From exercise_topics_map, collect all question prompts and answers
    grouped by question number.
    Returns list of dicts: {topic_id, q_num, sub_label, prompt_en, accepted_answers}
    """
    topics = exercise_topics_map.get(ex_num, [])
    result = []
    for topic in topics:
        tid = topic.get("id", "")
        m = re.match(r'rsexch(\d+)ex(\d+)q(\d+)', tid)
        if not m:
            continue
        q_num = int(m.group(3))
        # Get sub-label from id suffix (e.g., _a, _b, _q1)
        sub_m = re.search(r'_([a-zA-Z0-9]+)$', tid)
        sub_label = sub_m.group(1) if sub_m else ''

        questions = topic.get("questions", [])
        for q in questions:
            prompt_obj = q.get("prompt", {})
            prompt_en = prompt_obj.get("english", "") if isinstance(prompt_obj, dict) else str(prompt_obj)
            accepted = q.get("acceptedAnswers", [])
            result.append({
                "topic_id": tid,
                "q_num": q_num,
                "sub_label": sub_label,
                "prompt_en": prompt_en,
                "accepted_answers": accepted,
            })
    return result


# ─────────────────────────────────────────────────────────────────────────────
# STEP 6: COMPARE PDF vs JSON
# ─────────────────────────────────────────────────────────────────────────────

def compare_exercise(ex_num, pdf_questions, json_questions):
    """
    Compare PDF questions vs JSON questions.
    Returns list of issue strings.
    """
    issues = []

    # Group JSON questions by q_num
    json_by_qnum: dict[int, list] = {}
    for jq in json_questions:
        qn = jq['q_num']
        json_by_qnum.setdefault(qn, []).append(jq)

    # Group PDF questions by num
    pdf_by_qnum = {pq['num']: pq for pq in pdf_questions}

    pdf_nums = set(pdf_by_qnum.keys())
    json_nums = set(json_by_qnum.keys())

    missing_in_json = pdf_nums - json_nums
    extra_in_json   = json_nums - pdf_nums

    if missing_in_json:
        issues.append(f"Missing in JSON (in PDF only): Q{sorted(missing_in_json)}")
    if extra_in_json:
        issues.append(f"Extra in JSON (not in PDF): Q{sorted(extra_in_json)}")

    # For common questions, check sub-parts
    for qn in sorted(pdf_nums & json_nums):
        pq = pdf_by_qnum[qn]
        jqs = json_by_qnum[qn]

        pdf_sub_labels = set(sp['label'] for sp in pq.get('sub_parts', []))
        json_sub_labels = set(jq['sub_label'].lower().lstrip('_') for jq in jqs
                               if re.match(r'^[a-z]$', jq['sub_label'].lower()))

        # Check if JSON has no sub-parts but PDF does
        if pdf_sub_labels and not json_sub_labels and len(jqs) == 1:
            issues.append(f"Q{qn}: PDF has sub-parts {sorted(pdf_sub_labels)} but JSON has single entry (needs split)")

        # Check for missing sub-parts in JSON
        missing_subs = pdf_sub_labels - json_sub_labels
        if missing_subs and json_sub_labels:
            issues.append(f"Q{qn}: Missing sub-parts in JSON: {sorted(missing_subs)}")

        # Check for empty answers
        for jq in jqs:
            if not jq['accepted_answers']:
                issues.append(f"Q{qn}({jq['sub_label']}): No accepted answers in JSON (topic: {jq['topic_id']})")

    return issues


# ─────────────────────────────────────────────────────────────────────────────
# STEP 7: MAIN PROCESSING LOOP
# ─────────────────────────────────────────────────────────────────────────────

print("\n" + "="*70)
print("PROCESSING ALL EXERCISES")
print("="*70)

report = {"exercises": {}}

exercises_with_images = []
exercises_with_unsplit = []
exercises_with_missing_answers = []
exercises_with_wrong_missing = []

total_pdf_questions = 0
total_json_questions = 0
total_issues = 0

for ex_num in range(1, 81):
    start_pg, end_pg = exercise_pages[ex_num]

    # Extract text
    raw_text = get_exercise_text(doc, start_pg, end_pg)
    pdf_questions = parse_questions_from_text(raw_text)

    # Render images if needed
    image_files = render_exercise_images(doc, ex_num, start_pg, end_pg)

    has_image_qs = any(pq['has_image'] for pq in pdf_questions)
    if has_image_qs or image_files:
        exercises_with_images.append(ex_num)

    # Get JSON questions
    json_questions = get_json_questions_for_exercise(ex_num)

    # Compare
    issues = compare_exercise(ex_num, pdf_questions, json_questions)

    # Track issue types
    has_unsplit = any('needs split' in iss for iss in issues)
    has_missing_ans = any('No accepted answers' in iss for iss in issues)
    has_missing_q = any('Missing in JSON' in iss or 'wrong answer' in iss for iss in issues)

    if has_unsplit:
        exercises_with_unsplit.append(ex_num)
    if has_missing_ans:
        exercises_with_missing_answers.append(ex_num)
    if has_missing_q or issues:
        exercises_with_wrong_missing.append(ex_num)

    total_pdf_questions += len(pdf_questions)
    total_json_questions += len(json_questions)
    total_issues += len(issues)

    # Build report entry
    report["exercises"][str(ex_num)] = {
        "pages": f"{start_pg}-{end_pg}",
        "page_count": end_pg - start_pg + 1,
        "pdf_questions": [
            {
                "num": pq["num"],
                "text": pq["text"],
                "sub_parts": [sp["label"] for sp in pq["sub_parts"]],
                "has_image": pq["has_image"],
            }
            for pq in pdf_questions
        ],
        "json_questions": [
            {
                "topic_id": jq["topic_id"],
                "q_num": jq["q_num"],
                "sub_label": jq["sub_label"],
                "prompt_en": jq["prompt_en"][:150],
                "accepted_answers": jq["accepted_answers"],
            }
            for jq in json_questions
        ],
        "image_files": image_files,
        "issues": issues,
    }

    # Progress print
    q_count_pdf = len(pdf_questions)
    q_count_json = len(json_questions)
    issue_marker = f" [!{len(issues)} issues]" if issues else ""
    img_marker = f" [img:{len(image_files)}]" if image_files else ""
    print(f"  Ex{ex_num:02d} (pg {start_pg}-{end_pg}): PDF={q_count_pdf}q  JSON={q_count_json}entries{img_marker}{issue_marker}")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 8: SAVE REPORT
# ─────────────────────────────────────────────────────────────────────────────

report["summary"] = {
    "total_exercises": 80,
    "exercises_with_images": exercises_with_images,
    "exercises_with_unsplit_subquestions": exercises_with_unsplit,
    "exercises_with_missing_answers": exercises_with_missing_answers,
    "exercises_with_issues": exercises_with_wrong_missing,
    "total_pdf_questions": total_pdf_questions,
    "total_json_question_entries": total_json_questions,
    "total_issues_found": total_issues,
}

with open(REPORT_PATH, 'w', encoding='utf-8') as fh:
    json.dump(report, fh, ensure_ascii=False, indent=2)

print(f"\n[INFO] Report saved to: {REPORT_PATH}")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 9: PRINT SUMMARY
# ─────────────────────────────────────────────────────────────────────────────

print("\n" + "="*70)
print("SUMMARY")
print("="*70)
print(f"Total exercises processed       : 80")
print(f"Total PDF questions extracted   : {total_pdf_questions}")
print(f"Total JSON question entries     : {total_json_questions}")
print(f"Total issues found              : {total_issues}")
print()
print(f"Exercises WITH image questions  : {len(exercises_with_images)}")
if exercises_with_images:
    print(f"  => {exercises_with_images}")
print()
print(f"Exercises with UNSPLIT sub-Qs   : {len(exercises_with_unsplit)}")
if exercises_with_unsplit:
    print(f"  => {exercises_with_unsplit}")
print()
print(f"Exercises with MISSING answers  : {len(exercises_with_missing_answers)}")
if exercises_with_missing_answers:
    print(f"  => {exercises_with_missing_answers}")
print()
print(f"Exercises with ANY issues       : {len(exercises_with_wrong_missing)}")
if exercises_with_wrong_missing:
    print(f"  => {exercises_with_wrong_missing}")
print()

# Detailed issue printout
print("="*70)
print("DETAILED ISSUES PER EXERCISE")
print("="*70)
for ex_num in range(1, 81):
    entry = report["exercises"][str(ex_num)]
    if entry["issues"]:
        print(f"\nExercise {ex_num} (pages {entry['pages']}):")
        for iss in entry["issues"]:
            print(f"  - {iss}")

print("\n[DONE] extract_pdf_questions.py completed.")
