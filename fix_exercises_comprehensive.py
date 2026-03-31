#!/usr/bin/env python3
"""
Comprehensive fix for all exercise path JSON questions:
1. Fix Ex75 Q1/Q2 angle values to match PDF
2. Add questionImageAsset to image-based questions
3. Split Ex77 Q2/Q7/Q10, Ex79 remaining sub-parts
4. Fix Ex74 Q2 with text descriptions
"""
import json, os, glob, re, sys
sys.stdout.reconfigure(encoding='utf-8')
os.chdir('c:/Users/abhil/OneDrive/Desktop/Book/TechLoopApp')

BASE = 'app/src/main/assets/subject_packs/class5_rs_aggarwal_math'

def load(fname):
    with open(os.path.join(BASE, fname), encoding='utf-8') as f:
        return json.load(f)

def save(fname, data):
    with open(os.path.join(BASE, fname), 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

def loc(en, hi=None):
    return {'english': en, 'hindi': hi if hi else en}

def text_q(qid, en, hi, accepted, sol_en, sol_hi, image=None):
    q = {
        'id': qid,
        'prompt': loc(en, hi),
        'type': 'TEXT_INPUT',
        'options': [],
        'correctOptionIndex': None,
        'acceptedAnswers': accepted,
        'hint': None,
        'wrongReason': loc(sol_en, sol_hi),
        'supportExample': loc(sol_en, sol_hi),
        'mistakeType': 'GENERAL',
        'reteachTitle': loc('Solution', 'हल'),
        'reteachParagraphs': [loc(sol_en, sol_hi)],
    }
    if image:
        q['questionImageAsset'] = image
    return q

def mcq(qid, en, hi, opts_en, opts_hi, correct_idx, sol_en, sol_hi, image=None):
    q = {
        'id': qid,
        'prompt': loc(en, hi),
        'type': 'MULTIPLE_CHOICE',
        'options': [loc(opts_en[i], opts_hi[i]) for i in range(len(opts_en))],
        'correctOptionIndex': correct_idx,
        'acceptedAnswers': [],
        'hint': None,
        'wrongReason': loc(sol_en, sol_hi),
        'supportExample': loc(sol_en, sol_hi),
        'mistakeType': 'CONCEPT_CONFUSION',
        'reteachTitle': loc('Solution', 'हल'),
        'reteachParagraphs': [loc(sol_en, sol_hi)],
    }
    if image:
        q['questionImageAsset'] = image
    return q

def make_topic(tid, ch_num, ch_title_en, ch_title_hi, lesson_en, subtopic_en, subtopic_hi, question):
    return {
        'id': tid,
        'sourceLessonId': tid,
        'chapterNumber': ch_num,
        'chapterTitle': loc(ch_title_en, ch_title_hi),
        'lessonTitle': loc(lesson_en, lesson_en),
        'subtopicTitle': loc(subtopic_en, subtopic_hi),
        'knowPrompt': loc(f'Can you solve {subtopic_en}?', f'क्या आप {subtopic_hi} हल कर सकते हैं?'),
        'explanationTitle': loc(subtopic_en, subtopic_hi),
        'explanationParagraphs': [],
        'examples': [],
        'visuals': [],
        'questions': [question],
        'tags': [],
        'mistakeFocus': 'GENERAL',
    }


# ═══════════════════════════════════════════════════════════
# FIX 1: Exercise 75 Q1 and Q2 — correct angles from PDF
# ═══════════════════════════════════════════════════════════

def fix_ex75():
    data = load('chapter_20_triangles.json')

    # Q1 correct angles from PDF page 264:
    q1_data = [
        # (part, angles, classification, correct_idx)
        # (a) triangle ABC: 25°, 30°, 125° → obtuse
        ('qa', '25°, 30°, 125°', 'A,B,C', 2,
         'Triangle with 25°, 30°, 125°: angle 125° > 90° → Obtuse-angled.',
         '25°, 30°, 125° वाला त्रिभुज: 125° > 90° → अधिक कोण त्रिभुज।'),
        # (b) triangle NLM: 45°, 45°, 90° → right
        ('qb', '45°, 45°, 90°', 'N,L,M', 1,
         'Triangle with 45°, 45°, 90°: one angle = 90° → Right-angled.',
         '45°, 45°, 90° वाला त्रिभुज: एक कोण = 90° → समकोण त्रिभुज।'),
        # (c) triangle PQR: 25°, 60°, 95° → obtuse
        ('qc', '25°, 60°, 95°', 'P,Q,R', 2,
         'Triangle with 25°, 60°, 95°: angle 95° > 90° → Obtuse-angled.',
         '25°, 60°, 95° वाला त्रिभुज: 95° > 90° → अधिक कोण त्रिभुज।'),
        # (d) triangle RST: 55°, 70°, 55° → acute
        ('qd', '55°, 70°, 55°', 'R,S,T', 0,
         'Triangle with 55°, 70°, 55°: all angles < 90° → Acute-angled.',
         '55°, 70°, 55° वाला त्रिभुज: सभी कोण < 90° → न्यून कोण त्रिभुज।'),
        # (e) triangle XYZ: 40°, 90°, 50° → right
        ('qe', '40°, 90°, 50°', 'X,Y,Z', 1,
         'Triangle with 40°, 90°, 50°: one angle = 90° → Right-angled.',
         '40°, 90°, 50° वाला त्रिभुज: एक कोण = 90° → समकोण त्रिभुज।'),
        # (f) triangle DEF: 40°, 52°, 88° → acute
        ('qf', '40°, 52°, 88°', 'D,E,F', 0,
         'Triangle with 40°, 52°, 88°: all angles < 90° → Acute-angled.',
         '40°, 52°, 88° वाला त्रिभुज: सभी कोण < 90° → न्यून कोण त्रिभुज।'),
    ]

    # Q2 correct angle pairs from PDF:
    q2_data = [
        # (a) ABC: 40°, 90° → third = 50°
        ('qa', '40° and 90°', 50, '40° + 90° = 130°; third = 180° − 130° = 50°.',
         '40° + 90° = 130°; तीसरा = 180° − 130° = 50°।'),
        # (b) PQR: 35°, 30° → third = 115°
        ('qb', '35° and 30°', 115, '35° + 30° = 65°; third = 180° − 65° = 115°.',
         '35° + 30° = 65°; तीसरा = 180° − 65° = 115°।'),
        # (c) LMN: 35°, 62° → third = 83°
        ('qc', '35° and 62°', 83, '35° + 62° = 97°; third = 180° − 97° = 83°.',
         '35° + 62° = 97°; तीसरा = 180° − 97° = 83°।'),
        # (d) XYZ: 22°, 125° → third = 33°
        ('qd', '22° and 125°', 33, '22° + 125° = 147°; third = 180° − 147° = 33°.',
         '22° + 125° = 147°; तीसरा = 180° − 147° = 33°।'),
        # (e) DEF: 47°, 53° → third = 80°
        ('qe', '47° and 53°', 80, '47° + 53° = 100°; third = 180° − 100° = 80°.',
         '47° + 53° = 100°; तीसरा = 180° − 100° = 80°।'),
        # (f) GHK: 90°, 55° → third = 35°
        ('qf', '90° and 55°', 35, '90° + 55° = 145°; third = 180° − 145° = 35°.',
         '90° + 55° = 145°; तीसरा = 180° − 145° = 35°।'),
    ]

    opts_en = ['Acute-angled', 'Right-angled', 'Obtuse-angled']
    opts_hi = ['न्यून कोण त्रिभुज', 'समकोण त्रिभुज', 'अधिक कोण त्रिभुज']

    # Build replacement questions for Q1 and Q2 sub-parts
    q1_replacements = {}
    for part, angles, vertices, cidx, sol_en, sol_hi in q1_data:
        qid = f'rsexch20ex75q01_{part}'
        en = f'Classify the triangle ({vertices}) with angles {angles} by its angles. (See figure)'
        hi = f'त्रिभुज ({vertices}) जिसके कोण {angles} हैं, उसे वर्गीकृत करें।'
        q1_replacements[qid] = mcq(qid, en, hi, opts_en, opts_hi, cidx, sol_en, sol_hi, 'ex75_q01.png')

    q2_replacements = {}
    for part, angle_pair, third, sol_en, sol_hi in q2_data:
        qid = f'rsexch20ex75q02_{part}'
        en = f'A triangle has two angles: {angle_pair}. Find the third angle. (See figure)'
        hi = f'एक त्रिभुज के दो कोण {angle_pair} हैं। तीसरा कोण ज्ञात करें।'
        ans_str = str(third)
        q2_replacements[qid] = text_q(qid, en, hi, [ans_str, f'{ans_str}°'], sol_en, sol_hi, 'ex75_q02.png')

    changed = 0
    for topic in data['topics']:
        new_qs = []
        for q in topic['questions']:
            if q['id'] in q1_replacements:
                new_qs.append(q1_replacements[q['id']])
                changed += 1
            elif q['id'] in q2_replacements:
                new_qs.append(q2_replacements[q['id']])
                changed += 1
            else:
                new_qs.append(q)
        topic['questions'] = new_qs

    save('chapter_20_triangles.json', data)
    print(f'  Ex75: fixed {changed} questions with correct PDF angles + images')


# ═══════════════════════════════════════════════════════════
# FIX 2: Add images to Ex70, Ex71, Ex73, Ex74, Ex76 questions
# ═══════════════════════════════════════════════════════════

def add_images_to_ex70():
    data = load('chapter_18_concept_of_angles.json')

    # Image assignments for ex70 questions
    image_map = {
        'rsexch18ex70q01': 'ex70_q01.png',
        'rsexch18ex70q01_q1': 'ex70_q01.png',
        'rsexch18ex70q02': 'ex70_q02.png',
        'rsexch18ex70q02_q1': 'ex70_q02.png',
        'rsexch18ex70q03': 'ex70_q03.png',
        'rsexch18ex70q03_q1': 'ex70_q03.png',
        'rsexch18ex70q04': 'ex70_q04.png',
        'rsexch18ex70q04_q1': 'ex70_q04.png',
    }

    changed = 0
    for topic in data['topics']:
        for q in topic['questions']:
            # Match by prefix
            for prefix, img in image_map.items():
                if q['id'].startswith(prefix.rstrip('_q1').rstrip('q1')):
                    if q.get('questionImageAsset') != img:
                        q['questionImageAsset'] = img
                        changed += 1
                    break

    save('chapter_18_concept_of_angles.json', data)
    print(f'  Ex70: added images to {changed} questions')


def add_images_to_ex71():
    data = load('chapter_18_concept_of_angles.json')

    changed = 0
    for topic in data['topics']:
        for q in topic['questions']:
            qid = q['id']
            img = None
            if 'ex71q01' in qid:
                img = 'ex71_q01.png'
            elif 'ex71q02' in qid:
                img = 'ex71_q02.png'
            if img and q.get('questionImageAsset') != img:
                q['questionImageAsset'] = img
                changed += 1

    save('chapter_18_concept_of_angles.json', data)
    print(f'  Ex71: added images to {changed} questions')


def add_images_to_geometry():
    # Ex73, Ex74, Ex76 are in different chapter files

    # chapter_19 = parallel lines = ex73
    data19 = load('chapter_19_parallel_and_perpendicular_lines.json')
    changed19 = 0
    for topic in data19['topics']:
        for q in topic['questions']:
            if 'ex73q01' in q['id'] and not q.get('questionImageAsset'):
                q['questionImageAsset'] = 'ex73_q01.png'
                changed19 += 1
    save('chapter_19_parallel_and_perpendicular_lines.json', data19)
    print(f'  Ex73: added images to {changed19} questions')

    # chapter_20 = triangles = ex74, ex75
    data20 = load('chapter_20_triangles.json')
    changed20 = 0
    for topic in data20['topics']:
        for q in topic['questions']:
            qid = q['id']
            img = None
            if 'ex74q01' in qid:
                img = 'ex74_q01.png'
            elif 'ex74q02' in qid:
                if '_qa' in qid or '_qb' in qid or '_qc' in qid:
                    img = 'ex74_q02_abc.png'
                elif '_qd' in qid or '_qe' in qid or '_qf' in qid:
                    img = 'ex74_q02_def.png'
            if img and not q.get('questionImageAsset'):
                q['questionImageAsset'] = img
                changed20 += 1
    save('chapter_20_triangles.json', data20)
    print(f'  Ex74: added images to {changed20} questions')

    # chapter_21 = circles = ex76
    data21 = load('chapter_21_circles.json')
    changed21 = 0
    for topic in data21['topics']:
        for q in topic['questions']:
            if 'ex76q01' in q['id'] or 'ex76q02' in q['id']:
                if not q.get('questionImageAsset'):
                    q['questionImageAsset'] = 'ex76_q01_q02.png'
                    changed21 += 1
    save('chapter_21_circles.json', data21)
    print(f'  Ex76: added images to {changed21} questions')


# ═══════════════════════════════════════════════════════════
# FIX 3: Ex77 Q2, Q7, Q10 — split sub-parts
# ═══════════════════════════════════════════════════════════

def fix_ex77():
    data = load('chapter_22_perimeters_of_rectilinear_figures.json')

    # Find topic data for chapter/exercise info
    ch_num = 22
    ch_en = 'Perimeters of Rectilinear Figures'
    ch_hi = 'आयताकार आकृतियों का परिमाप'
    lesson = 'Exercise 77'

    # Q2 sub-parts: perimeter of triangles
    q2_splits = [
        # (suffix, sides_en, sides_hi, ans_str, sol_en, sol_hi)
        ('qa', '8 cm, 6 cm, 5 cm', '8 सेमी, 6 सेमी, 5 सेमी', ['19', '19 cm'],
         '8 + 6 + 5 = 19 cm.', '8 + 6 + 5 = 19 सेमी।'),
        ('qb', '7.6 cm, 6.4 cm, 5.2 cm', '7.6 सेमी, 6.4 सेमी, 5.2 सेमी', ['19.2', '19.2 cm'],
         '7.6 + 6.4 + 5.2 = 19.2 cm.', '7.6 + 6.4 + 5.2 = 19.2 सेमी।'),
        ('qc', '4.8 cm, 5.3 cm, 8.2 cm', '4.8 सेमी, 5.3 सेमी, 8.2 सेमी', ['18.3', '18.3 cm'],
         '4.8 + 5.3 + 8.2 = 18.3 cm.', '4.8 + 5.3 + 8.2 = 18.3 सेमी।'),
        ('qd', '6 cm, 7.3 cm, 8.5 cm', '6 सेमी, 7.3 सेमी, 8.5 सेमी', ['21.8', '21.8 cm'],
         '6 + 7.3 + 8.5 = 21.8 cm.', '6 + 7.3 + 8.5 = 21.8 सेमी।'),
    ]

    # Q7 sub-parts: perimeter of rectangles
    q7_splits = [
        ('qa', 'Length = 36 cm, Breadth = 29 cm', 'लंबाई = 36 सेमी, चौड़ाई = 29 सेमी',
         ['130', '130 cm'], 'P = 2(36+29) = 2×65 = 130 cm.', 'P = 2(36+29) = 130 सेमी।'),
        ('qb', 'Length = 7 m, Breadth = 5 m', 'लंबाई = 7 मी, चौड़ाई = 5 मी',
         ['24', '24 m'], 'P = 2(7+5) = 2×12 = 24 m.', 'P = 2(7+5) = 24 मी।'),
        ('qc', 'Length = 2 m 60 cm, Breadth = 1 m 90 cm', 'लंबाई = 2 मी 60 सेमी, चौड़ाई = 1 मी 90 सेमी',
         ['9', '9 m', '900 cm'], 'P = 2(260+190) = 2×450 = 900 cm = 9 m.', 'P = 2(260+190) = 900 सेमी = 9 मी।'),
        ('qd', 'Length = 12.6 m, Breadth = 9.4 m', 'लंबाई = 12.6 मी, चौड़ाई = 9.4 मी',
         ['44', '44 m'], 'P = 2(12.6+9.4) = 2×22 = 44 m.', 'P = 2(12.6+9.4) = 44 मी।'),
    ]

    # Q10 sub-parts: perimeter of squares
    q10_splits = [
        ('qa', '45 cm', '45 सेमी', ['180', '180 cm'],
         'P = 4×45 = 180 cm.', 'P = 4×45 = 180 सेमी।'),
        ('qb', '16 m', '16 मी', ['64', '64 m'],
         'P = 4×16 = 64 m.', 'P = 4×16 = 64 मी।'),
        ('qc', '2 m 10 cm', '2 मी 10 सेमी', ['8.4', '8.4 m', '840 cm'],
         'P = 4×210 cm = 840 cm = 8.4 m.', 'P = 4×210 = 840 सेमी = 8.4 मी।'),
        ('qd', '3 m 25 cm', '3 मी 25 सेमी', ['13', '13 m', '1300 cm'],
         'P = 4×325 cm = 1300 cm = 13 m.', 'P = 4×325 = 1300 सेमी = 13 मी।'),
    ]

    # Build replacement maps
    q2_new = []
    for sfx, sides_en, sides_hi, ans, sol_en, sol_hi in q2_splits:
        qid = f'rsexch22ex77q02_{sfx}'
        en = f'Find the perimeter of a triangle with sides: {sides_en}.'
        hi = f'एक त्रिभुज की भुजाएँ {sides_hi} हैं। परिमाप ज्ञात करें।'
        q2_new.append((qid, text_q(qid, en, hi, ans, sol_en, sol_hi)))

    q7_new = []
    for sfx, dim_en, dim_hi, ans, sol_en, sol_hi in q7_splits:
        qid = f'rsexch22ex77q07_{sfx}'
        en = f'Find the perimeter of the rectangle with {dim_en}.'
        hi = f'{dim_hi} वाले आयत का परिमाप ज्ञात करें।'
        q7_new.append((qid, text_q(qid, en, hi, ans, sol_en, sol_hi)))

    q10_new = []
    for sfx, side_en, side_hi, ans, sol_en, sol_hi in q10_splits:
        qid = f'rsexch22ex77q10_{sfx}'
        en = f'Find the perimeter of the square whose side is {side_en}.'
        hi = f'उस वर्ग का परिमाप ज्ञात करें जिसकी भुजा {side_hi} है।'
        q10_new.append((qid, text_q(qid, en, hi, ans, sol_en, sol_hi)))

    # Also add image to Q1 sub-parts
    changed = 0
    for topic in data['topics']:
        for q in topic['questions']:
            if 'ex77q01' in q['id'] and not q.get('questionImageAsset'):
                q['questionImageAsset'] = 'ex77_q01.png'
                changed += 1

    # Insert new topic entries after their parent topics
    topics = data['topics']
    new_topics = []

    def insert_after(parent_id_contains, new_items):
        nonlocal topics
        result = []
        added = {}
        for t in topics:
            result.append(t)
            if parent_id_contains in t['id'] and parent_id_contains not in added:
                added[parent_id_contains] = True
                ch_data = t
                for qid_new, q_new in new_items:
                    if not any(tt['id'] == qid_new for tt in topics):
                        new_t = make_topic(
                            qid_new, ch_num, ch_en, ch_hi, lesson,
                            f'Question Ex77 {qid_new.split("_")[-1].upper()}',
                            f'प्रश्न Ex77 {qid_new.split("_")[-1].upper()}',
                            q_new
                        )
                        new_t['chapterTitle'] = ch_data.get('chapterTitle', loc(ch_en, ch_hi))
                        result.append(new_t)
                        changed2 = 1
        return result

    # Apply splits
    topics = insert_after('rsexch22ex77q02', q2_new)
    data['topics'] = topics
    topics = insert_after('rsexch22ex77q07', q7_new)
    data['topics'] = topics
    topics = insert_after('rsexch22ex77q10', q10_new)
    data['topics'] = topics

    save('chapter_22_perimeters_of_rectilinear_figures.json', data)
    print(f'  Ex77: split Q2/Q7/Q10, added images to Q1. Total new topics: {len(q2_new)+len(q7_new)+len(q10_new)}')


# ═══════════════════════════════════════════════════════════
# FIX 4: Ex74 Q2 — add text descriptions from PDF side data
# ═══════════════════════════════════════════════════════════

def fix_ex74():
    data = load('chapter_20_triangles.json')

    # Q2 side data from PDF page 260:
    # (a) LN triangle: 3.2, 3.2, 4 → isosceles (two equal sides)
    # (b) PQR: 3.4, 3.4, 3.4 → equilateral
    # (c) XYZ: 4, 2.8, 4 → isosceles
    # (d) DEF: 5, 3, 4 → scalene
    # (e) ABC: 3.6, 3.6, 4.2 → isosceles
    # (f) GHK: 4.8, 3.2, 3.5 → scalene

    opts_en = ['Equilateral', 'Isosceles', 'Scalene']
    opts_hi = ['समबाहु', 'समद्विबाहु', 'विषमबाहु']

    q2_fixes = {
        # qid → (new_prompt_en, new_prompt_hi, correct_idx, sol_en, sol_hi)
        'rsexch20ex74q02_qa': (
            'Triangle with sides 3.2 cm, 3.2 cm, 4 cm. Classify by sides. (See figure)',
            'भुजाएँ 3.2 सेमी, 3.2 सेमी, 4 सेमी वाला त्रिभुज। भुजाओं के आधार पर वर्गीकृत करें।',
            1, 'Two sides equal (3.2=3.2) → Isosceles.', 'दो भुजाएँ बराबर → समद्विबाहु।', 'ex74_q02_abc.png'),
        'rsexch20ex74q02_qb': (
            'Triangle with sides 3.4 cm, 3.4 cm, 3.4 cm. Classify by sides. (See figure)',
            'भुजाएँ 3.4 सेमी, 3.4 सेमी, 3.4 सेमी वाला त्रिभुज। वर्गीकृत करें।',
            0, 'All three sides equal → Equilateral.', 'तीनों भुजाएँ बराबर → समबाहु।', 'ex74_q02_abc.png'),
        'rsexch20ex74q02_qc': (
            'Triangle with sides 4 cm, 2.8 cm, 4 cm. Classify by sides. (See figure)',
            'भुजाएँ 4 सेमी, 2.8 सेमी, 4 सेमी वाला त्रिभुज। वर्गीकृत करें।',
            1, 'Two sides equal (4=4) → Isosceles.', 'दो भुजाएँ बराबर → समद्विबाहु।', 'ex74_q02_abc.png'),
        'rsexch20ex74q02_qd': (
            'Triangle with sides 5 cm, 3 cm, 4 cm. Classify by sides. (See figure)',
            'भुजाएँ 5 सेमी, 3 सेमी, 4 सेमी वाला त्रिभुज। वर्गीकृत करें।',
            2, 'All sides different → Scalene.', 'सभी भुजाएँ अलग → विषमबाहु।', 'ex74_q02_def.png'),
        'rsexch20ex74q02_qe': (
            'Triangle with sides 3.6 cm, 3.6 cm, 4.2 cm. Classify by sides. (See figure)',
            'भुजाएँ 3.6 सेमी, 3.6 सेमी, 4.2 सेमी वाला त्रिभुज। वर्गीकृत करें।',
            1, 'Two sides equal (3.6=3.6) → Isosceles.', 'दो भुजाएँ बराबर → समद्विबाहु।', 'ex74_q02_def.png'),
        'rsexch20ex74q02_qf': (
            'Triangle with sides 4.8 cm, 3.2 cm, 3.5 cm. Classify by sides. (See figure)',
            'भुजाएँ 4.8 सेमी, 3.2 सेमी, 3.5 सेमी वाला त्रिभुज। वर्गीकृत करें।',
            2, 'All sides different → Scalene.', 'सभी भुजाएँ अलग → विषमबाहु।', 'ex74_q02_def.png'),
    }

    changed = 0
    for topic in data['topics']:
        for q in topic['questions']:
            if q['id'] in q2_fixes:
                en, hi, cidx, sol_en, sol_hi, img = q2_fixes[q['id']]
                new_q = mcq(q['id'], en, hi, opts_en, opts_hi, cidx, sol_en, sol_hi, img)
                topic['questions'] = [new_q if qq['id'] == q['id'] else qq for qq in topic['questions']]
                changed += 1
        # Add image to Q1
        for q in topic['questions']:
            if 'ex74q01' in q['id'] and not q.get('questionImageAsset'):
                q['questionImageAsset'] = 'ex74_q01.png'
                changed += 1

    save('chapter_20_triangles.json', data)
    print(f'  Ex74: fixed {changed} Q2 questions with correct side data + images')


# ═══════════════════════════════════════════════════════════
# FIX 5: Add image to Ex77 Q1 sub-parts (already in JSON)
# ═══════════════════════════════════════════════════════════

def fix_ex77_q1_image():
    data = load('chapter_22_perimeters_of_rectilinear_figures.json')
    changed = 0
    for topic in data['topics']:
        for q in topic['questions']:
            if 'ex77q01' in q['id'] and not q.get('questionImageAsset'):
                q['questionImageAsset'] = 'ex77_q01.png'
                changed += 1
    save('chapter_22_perimeters_of_rectilinear_figures.json', data)
    print(f'  Ex77 Q1: added image to {changed} existing questions')


# ═══════════════════════════════════════════════════════════
# RUN ALL FIXES
# ═══════════════════════════════════════════════════════════

print('Applying comprehensive exercise fixes...')
print()
fix_ex75()
add_images_to_ex70()
add_images_to_ex71()
add_images_to_geometry()
fix_ex74()
fix_ex77()
fix_ex77_q1_image()
print()
print('All fixes applied.')
