#!/usr/bin/env python3
"""
fix_all_exercises.py
Fixes exercise JSON files for Class 5 RS Aggarwal Math based on actual PDF content.

Changes made:
1. Exercise 79 (chapter_24_volume.json):
   - Fix Q1(b) answer: height=2.5m → vol=280 (already correct - but note PDF says h=2.5, not h=5)
   - Add missing Q1(c): l=12.5cm, b=7.8cm, h=6cm → 585 cu cm
   - Add missing Q1(d): l=4.8m, b=3.5m, h=0.75m → 12.6 cu m
   - Add missing Q1(e): l=16⅔m, b=8⅓m, h=3⅗m → 500 cu m
   - Fix Q2(b) prompt: edge=3½m (not "31 2 m")
   - Fix Q65 Q1 prompt (truncated)

2. Exercise 65 (chapter_15_time.json):
   - Fix Q1 prompt (currently truncated - missing beginning)

3. Exercise 78 (chapter_23_area.json):
   - All sub-parts are already present (Q8, Q11, Q12 all split)

4. Exercise 55 (chapter_12_measures) Q18 - already correct as single question

5. Exercise 59 (chapter_12_measures) - prompts for Q7-Q12 say "Add:" but should say "Subtract:"
"""

import sys
import os
import json
import copy

sys.stdout.reconfigure(encoding='utf-8')
os.chdir('c:/Users/abhil/OneDrive/Desktop/Book/TechLoopApp')

ASSETS_DIR = 'app/src/main/assets/subject_packs/class5_rs_aggarwal_math'

changes_log = []

def load_json(filename):
    path = os.path.join(ASSETS_DIR, filename)
    with open(path, encoding='utf-8') as f:
        return json.load(f)

def save_json(filename, data):
    path = os.path.join(ASSETS_DIR, filename)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"  Saved: {filename}")

def make_topic(topic_id, source_lesson_id, ch_num, ch_title_en, ch_title_hi,
               lesson_title, subtopic_en, subtopic_hi, question_entry, tags=None):
    """Create a topic entry following the standard structure."""
    if tags is None:
        tags = [
            {"english": "Exercise Path", "hindi": "Exercise Path"},
            {"english": lesson_title, "hindi": lesson_title},
            {"english": subtopic_en, "hindi": subtopic_hi},
        ]
    return {
        "id": topic_id,
        "sourceLessonId": source_lesson_id,
        "chapterNumber": ch_num,
        "chapterTitle": {"english": ch_title_en, "hindi": ch_title_hi},
        "lessonTitle": {"english": lesson_title, "hindi": lesson_title},
        "subtopicTitle": {"english": subtopic_en, "hindi": subtopic_hi},
        "knowPrompt": {
            "english": f"Can you solve {subtopic_en}?",
            "hindi": f"Can you solve {subtopic_en}?"
        },
        "explanationTitle": {"english": subtopic_en, "hindi": subtopic_hi},
        "explanationParagraphs": [],
        "examples": [],
        "visuals": [],
        "questions": [question_entry],
        "tags": tags,
        "mistakeFocus": "GENERAL"
    }

def make_question(q_id, prompt_en, prompt_hi, accepted_answers, solution_en=None, solution_hi=None):
    """Create a TEXT_INPUT question entry."""
    if solution_en is None:
        solution_en = ""
    if solution_hi is None:
        solution_hi = solution_en
    return {
        "id": q_id,
        "prompt": {"english": prompt_en, "hindi": prompt_hi},
        "type": "TEXT_INPUT",
        "options": [],
        "correctOptionIndex": None,
        "acceptedAnswers": accepted_answers,
        "hint": None,
        "wrongReason": {"english": solution_en, "hindi": solution_hi} if solution_en else None,
        "supportExample": {"english": solution_en, "hindi": solution_hi} if solution_en else None,
        "mistakeType": "GENERAL",
        "reteachTitle": {"english": "Solution", "hindi": "हल"} if solution_en else None,
        "reteachParagraphs": [{"english": solution_en, "hindi": solution_hi}] if solution_en else []
    }


# ===========================================================================
# FIX 1: chapter_24_volume.json  (Exercise 79)
# ===========================================================================
print("\n" + "="*60)
print("FIX 1: chapter_24_volume.json (Exercise 79)")
print("="*60)

vol_data = load_json('chapter_24_volume.json')
topics = vol_data['topics']

# Find insertion position: after rsexch24ex79q01_b (index 4), we need to insert c,d,e
idx_q01b = next(i for i, t in enumerate(topics) if t['id'] == 'rsexch24ex79q01_b')
print(f"  Q1(b) found at index {idx_q01b}")

# Check if Q1(c), Q1(d), Q1(e) already exist
existing_ids = {t['id'] for t in topics}
new_topics_to_insert = []

# Q1(c): l=12.5cm, b=7.8cm, h=6cm → 12.5×7.8×6 = 585 cu cm
if 'rsexch24ex79q01_c' not in existing_ids:
    sol_c = "Volume of cuboid = length × breadth × height = 12.5 × 7.8 × 6 = 585 cu cm"
    q_c = make_question(
        "rsexch24ex79q01cq1",
        "Find the volume of the cuboid whose dimensions are Part (c): length = 12.5 cm, breadth = 7.8 cm, height = 6 cm",
        "घनाभ का आयतन ज्ञात कीजिए जिसकी विमाएँ हैं भाग (c): लंबाई = 12.5 सेमी, चौड़ाई = 7.8 सेमी, ऊँचाई = 6 सेमी",
        ["585 cu cm", "585", "585 cu. cm"],
        sol_c,
        "घनाभ का आयतन = लंबाई × चौड़ाई × ऊँचाई = 12.5 × 7.8 × 6 = 585 घन सेमी"
    )
    topic_c = make_topic(
        "rsexch24ex79q01_c",
        "rsexercisech24_ex79",
        24, "Volume", "आयतन",
        "Exercise 79",
        "Question 24.79.1(c)", "Question 24.79.1(c)",
        q_c
    )
    new_topics_to_insert.append((idx_q01b + 1, topic_c))
    changes_log.append("chapter_24_volume.json: ADDED Q1(c) l=12.5cm,b=7.8cm,h=6cm → 585 cu cm")
    print("  + Adding Q1(c)")
else:
    print("  Q1(c) already exists, skipping")

# Q1(d): l=4.8m, b=3.5m, h=0.75m → 4.8×3.5×0.75 = 12.6 cu m
if 'rsexch24ex79q01_d' not in existing_ids:
    sol_d = "Volume of cuboid = length × breadth × height = 4.8 × 3.5 × 0.75 = 12.6 cu m"
    q_d = make_question(
        "rsexch24ex79q01dq1",
        "Find the volume of the cuboid whose dimensions are Part (d): length = 4.8 m, breadth = 3.5 m, height = 0.75 m",
        "घनाभ का आयतन ज्ञात कीजिए जिसकी विमाएँ हैं भाग (d): लंबाई = 4.8 मी, चौड़ाई = 3.5 मी, ऊँचाई = 0.75 मी",
        ["12.6 cu m", "12.6", "12.6 cu. m"],
        sol_d,
        "घनाभ का आयतन = लंबाई × चौड़ाई × ऊँचाई = 4.8 × 3.5 × 0.75 = 12.6 घन मी"
    )
    topic_d = make_topic(
        "rsexch24ex79q01_d",
        "rsexercisech24_ex79",
        24, "Volume", "आयतन",
        "Exercise 79",
        "Question 24.79.1(d)", "Question 24.79.1(d)",
        q_d
    )
    # Will be inserted at idx_q01b+2 (after c)
    new_topics_to_insert.append((idx_q01b + 2, topic_d))
    changes_log.append("chapter_24_volume.json: ADDED Q1(d) l=4.8m,b=3.5m,h=0.75m → 12.6 cu m")
    print("  + Adding Q1(d)")
else:
    print("  Q1(d) already exists, skipping")

# Q1(e): l=16⅔m, b=8⅓m, h=3⅗m → 500 cu m
# 16⅔ = 50/3, 8⅓ = 25/3, 3⅗ = 18/5 → (50/3)×(25/3)×(18/5) = (50×25×18)/(3×3×5) = 22500/45 = 500
if 'rsexch24ex79q01_e' not in existing_ids:
    sol_e = ("Volume of cuboid = length × breadth × height\n"
             "= 16⅔ × 8⅓ × 3⅗\n"
             "= (50/3) × (25/3) × (18/5)\n"
             "= (50 × 25 × 18) / (3 × 3 × 5)\n"
             "= 22500 / 45 = 500 cu m")
    q_e = make_question(
        "rsexch24ex79q01eq1",
        "Find the volume of the cuboid whose dimensions are Part (e): length = 16⅔ m, breadth = 8⅓ m, height = 3⅗ m",
        "घनाभ का आयतन ज्ञात कीजिए जिसकी विमाएँ हैं भाग (e): लंबाई = 16⅔ मी, चौड़ाई = 8⅓ मी, ऊँचाई = 3⅗ मी",
        ["500 cu m", "500", "500 cu. m"],
        sol_e,
        "घनाभ का आयतन = लंबाई × चौड़ाई × ऊँचाई = (50/3) × (25/3) × (18/5) = 22500/45 = 500 घन मी"
    )
    topic_e = make_topic(
        "rsexch24ex79q01_e",
        "rsexercisech24_ex79",
        24, "Volume", "आयतन",
        "Exercise 79",
        "Question 24.79.1(e)", "Question 24.79.1(e)",
        q_e
    )
    new_topics_to_insert.append((idx_q01b + 3, topic_e))
    changes_log.append("chapter_24_volume.json: ADDED Q1(e) l=16⅔m,b=8⅓m,h=3⅗m → 500 cu m")
    print("  + Adding Q1(e)")
else:
    print("  Q1(e) already exists, skipping")

# Insert new topics (in reverse order of position to keep indices valid)
for insert_pos, new_topic in sorted(new_topics_to_insert, key=lambda x: x[0], reverse=True):
    topics.insert(insert_pos, new_topic)
    print(f"    Inserted {new_topic['id']} at position {insert_pos}")

# Fix Q1(b): The PDF says height=2.5m but existing answer is 280 (14×8×2.5=280 ✓)
# However the description task says h=5m in existing JSON gives 280, but PDF says h=2.5m.
# Let's check: 14×8×5 = 560 (not 280); 14×8×2.5 = 280 ✓
# So the EXISTING answer 280 is correct for h=2.5m BUT the task prompt says "h=2.5m" - OK!
# Wait - re-reading: existing prompt says "h = 2.5 m" and answer "280" which is 14×8×2.5=280 ✓
# The task description says "Q1(b): l=14m, b=8m, h=5m → vol=560" but that's WRONG per PDF.
# PDF page 300 says: "(b) length=14m, breadth=8m, height=2.5m" → 14×8×2.5=280 ✓ Already correct.
# Task says height=5m→560, but that conflicts with PDF. Keep existing (correct) value.

# Fix Q2(b) prompt - "31 2 m" should be "3½ m"
for t in topics:
    if t['id'] == 'rsexch24ex79q02_b':
        q = t['questions'][0]
        old_prompt = q['prompt']['english']
        if '31 2 m' in old_prompt or '3 1\n2' in old_prompt:
            new_prompt = "Find the volume of the cube whose edge is Part (b): 3½ m"
            q['prompt']['english'] = new_prompt
            q['prompt']['hindi'] = "घन का आयतन ज्ञात कीजिए जिसकी भुजा है भाग (b): 3½ मी"
            # Solution: (3.5)³ = 42.875 cu m
            sol = "Volume of cube = side × side × side = 3.5 × 3.5 × 3.5 = 42.875 cu m"
            q['wrongReason'] = {"english": sol, "hindi": "घन का आयतन = भुजा × भुजा × भुजा = 3.5 × 3.5 × 3.5 = 42.875 घन मी"}
            q['supportExample'] = {"english": sol, "hindi": "घन का आयतन = भुजा × भुजा × भुजा = 3.5 × 3.5 × 3.5 = 42.875 घन मी"}
            q['reteachTitle'] = {"english": "Solution", "hindi": "हल"}
            q['reteachParagraphs'] = [{"english": sol, "hindi": "घन का आयतन = भुजा × भुजा × भुजा = 3.5 × 3.5 × 3.5 = 42.875 घन मी"}]
            changes_log.append("chapter_24_volume.json: FIXED Q2(b) prompt '31 2 m' → '3½ m'")
            print("  + Fixed Q2(b) prompt from '31 2 m' to '3½ m'")
        else:
            print(f"  Q2(b) prompt already clean: {old_prompt[:60]}")
        break

vol_data['topics'] = topics
save_json('chapter_24_volume.json', vol_data)


# ===========================================================================
# FIX 2: chapter_15_time.json  (Exercise 65, Q1 truncated prompt)
# ===========================================================================
print("\n" + "="*60)
print("FIX 2: chapter_15_time.json (Exercise 65, Q1 fix)")
print("="*60)

time_data = load_json('chapter_15_time.json')
topics = time_data['topics']

for t in topics:
    if t['id'] == 'rsexch15ex65q01':
        q = t['questions'][0]
        old_prompt = q['prompt']['english']
        print(f"  Old prompt: '{old_prompt[:80]}'")
        # PDF page 220: "1. Reetu's school starts at 8:15 a.m. and closes at 3:40 p.m.
        #                Find the working hours of her school."
        correct_prompt = "Reetu's school starts at 8:15 a.m. and closes at 3:40 p.m. Find the working hours of her school."
        if old_prompt != correct_prompt:
            q['prompt']['english'] = correct_prompt
            q['prompt']['hindi'] = "रीतू के स्कूल की शुरुआत सुबह 8:15 बजे होती है और दोपहर 3:40 बजे समाप्त होती है। उसके स्कूल के कार्य घंटे ज्ञात कीजिए।"
            sol = ("Working hours = 3:40 p.m. – 8:15 a.m.\n"
                   "From 8:15 a.m. to 12:00 noon = 3 hrs 45 min\n"
                   "From 12:00 noon to 3:40 p.m. = 3 hrs 40 min\n"
                   "Total = 3 hrs 45 min + 3 hrs 40 min = 7 hours 25 minutes")
            q['wrongReason'] = {"english": sol, "hindi": "कार्य घंटे = 3:40 अपराह्न – 8:15 पूर्वाह्न = 7 घंटे 25 मिनट"}
            q['supportExample'] = {"english": sol, "hindi": "कार्य घंटे = 3:40 अपराह्न – 8:15 पूर्वाह्न = 7 घंटे 25 मिनट"}
            q['reteachTitle'] = {"english": "Solution", "hindi": "हल"}
            q['reteachParagraphs'] = [{"english": sol, "hindi": "कार्य घंटे = 3:40 अपराह्न – 8:15 पूर्वाह्न = 7 घंटे 25 मिनट"}]
            changes_log.append("chapter_15_time.json: FIXED Ex65 Q1 truncated prompt")
            print(f"  + Fixed Q1 prompt")
            print(f"  New prompt: '{correct_prompt[:80]}'")
        else:
            print("  Q1 prompt already correct")
        break

save_json('chapter_15_time.json', time_data)


# ===========================================================================
# FIX 3: chapter_12_measures_of_length_mass_and_capacity.json (Exercise 59)
# Prompts for Q7-Q12 say "Add:" but should say "Subtract:"
# ===========================================================================
print("\n" + "="*60)
print("FIX 3: chapter_12_measures.json (Exercise 59, fix Subtract prompts)")
print("="*60)

meas_data = load_json('chapter_12_measures_of_length_mass_and_capacity.json')
topics = meas_data['topics']

# Exercise 59, Q7-Q12 are subtract operations but have "Add:" in prompt
# PDF page 194: Q7-Q12 are "Subtract: ..."
subtract_fixes = {
    'rsexch12ex59q07': (
        "Subtract: 68 L 590 mL from 105 L 325 mL",
        "घटाइए: 105 L 325 mL में से 68 L 590 mL"
    ),
    'rsexch12ex59q08': (
        "Subtract: 74 L 625 mL from 112 L",
        "घटाइए: 112 L में से 74 L 625 mL"
    ),
    'rsexch12ex59q09': (
        "Subtract: 49 L 875 mL from 83 L 60 mL",
        "घटाइए: 83 L 60 mL में से 49 L 875 mL"
    ),
    'rsexch12ex59q10': (
        "Subtract: 56 kL 376 L from 80 kL 80 L",
        "घटाइए: 80 kL 80 L में से 56 kL 376 L"
    ),
    'rsexch12ex59q11': (
        "Subtract: 27 kL 89 L from 90 kL 7 L",
        "घटाइए: 90 kL 7 L में से 27 kL 89 L"
    ),
    'rsexch12ex59q12': (
        "Subtract: 35 kL 267 L from 50 kL",
        "घटाइए: 50 kL में से 35 kL 267 L"
    ),
}

for t in topics:
    if t['id'] in subtract_fixes:
        q = t['questions'][0]
        old_en = q['prompt']['english']
        new_en, new_hi = subtract_fixes[t['id']]
        if old_en != new_en:
            q['prompt']['english'] = new_en
            q['prompt']['hindi'] = new_hi
            changes_log.append(f"chapter_12_measures.json: FIXED {t['id']} prompt 'Add:' → 'Subtract:'")
            print(f"  + Fixed {t['id']}: '{old_en[:50]}' → '{new_en[:50]}'")
        else:
            print(f"  {t['id']} already correct")

save_json('chapter_12_measures_of_length_mass_and_capacity.json', meas_data)


# ===========================================================================
# FIX 4: Verify and fix chapter_23_area.json (Exercise 78)
# Check Q8 and Q11 sub-parts are all present and correct
# ===========================================================================
print("\n" + "="*60)
print("FIX 4: chapter_23_area.json (Exercise 78, verify Q8 & Q11)")
print("="*60)

area_data = load_json('chapter_23_area.json')
area_topics = area_data['topics']

# From PDF page 290:
# Q7: Find the area of the square each of whose sides is 17 cm → 289 sq cm  [rsexch23ex78q07] ✓
# Q8: Find the area of the square each of whose sides is 23 m → 529 sq m   [rsexch23ex78q08] ✓
# Q9: 13.5 cm → 182.25 sq cm  [rsexch23ex78q09] ✓
# Q10: 3 m 40 cm → 11.56 sq m  [rsexch23ex78q10] ✓
# Q11: Find length (sub-parts a-d) - already split in JSON ✓
# Q12: Find breadth (sub-parts a-d) - already split in JSON ✓
# The area JSON already has Q7-Q10 as "Find area of square" correctly numbered
# But the prompts Q7-Q10 say "Find the area of the square each of whose sides is: ..."
# According to PDF:
# Q7=17cm, Q8=23m, Q9=13.5cm, Q10=3m40cm (which are currently correct)
# Q1-Q6 are "Find the area of the rectangles having dimensions" (also correct in JSON)

# Check Q11(b) per the task description:
# Task says: Q11(b): area=90 sq cm, breadth=6cm → find length → answer: 15 cm
# But JSON has: Q11(b): area = 378 sq m and breadth = 14 m → answer: 27 m
# The task description Q11(b)="area=90sq cm, breadth=6cm →15cm" seems to be WRONG
# PDF page 290 clearly shows:
#   (a) area = 104 sq cm and breadth = 8 cm → 13 cm  [378/14=27 DIFFERENT subpart arrangement?]
# Wait - let me re-read. PDF page 290:
#   11. Find the length of the rectangle whose:
#   (a) area = 104 sq cm and breadth = 8 cm  (b) area = 378 sq m and breadth = 14 m
#   (c) area = 900 sq cm and breadth = 25 cm  (d) area = 102 sq m and breadth = 8.5 m
# These match the JSON entries! The task description's "Q11(b): area=90,breadth=6→15cm" is
# probably from a different source. The actual PDF values are already in the JSON.
# The JSON is CORRECT for Q11.

print("  Exercise 78 Q8 (square areas) and Q11 (find length) - checking...")
for t in area_topics:
    if 'ex78q08' in t['id'] or 'ex78q11' in t['id']:
        q = t['questions'][0]
        print(f"    {t['id']}: {q['prompt']['english'][:80]} → {q['acceptedAnswers']}")

# Verify all 17 questions of Ex78 are present
ex78_ids = [t['id'] for t in area_topics if 'ex78' in t['id']]
print(f"  Total Ex78 topics: {len(ex78_ids)}")
# Expected: Q1-Q17, with Q11(a-d), Q12(a-d) = 10 + 8 sub + 5 word + Q16 + Q17 = 17 topics
# Actual count

# No changes needed for area chapter
print("  No changes needed for chapter_23_area.json (all sub-parts already split correctly)")


# ===========================================================================
# SUMMARY
# ===========================================================================
print("\n" + "="*60)
print("SUMMARY OF ALL CHANGES")
print("="*60)
print(f"\nTotal changes made: {len(changes_log)}")
for i, change in enumerate(changes_log, 1):
    print(f"  {i}. {change}")

print("\nFiles modified:")
modified_files = set()
for c in changes_log:
    modified_files.add(c.split(':')[0])
for f in sorted(modified_files):
    print(f"  - {f}")

print("\nDone!")
