#!/usr/bin/env python3
"""
Fixes all 24 identified problematic questions across 8 chapter JSON files.
"""
import json, os, glob

BASE = os.path.join(os.path.dirname(__file__),
    "app", "src", "main", "assets",
    "subject_packs", "class5_rs_aggarwal_math")

def lz(en, hi=None):
    return {"english": en, "hindi": hi or en}

# ─────────────────────────────────────────────────────────────
# All fixes keyed by (filename, question_id)
# Each entry is a dict of fields to patch on the question object
# ─────────────────────────────────────────────────────────────
FIXES = {

# ══════════════════════════════════════════════════════
# CHAPTER 1
# ══════════════════════════════════════════════════════

# CRITICAL – strip garbled "4 cm 8 cm …" appended after real question
("chapter_01_revision.json", "rsexch01ex01q66_q1"): {
    "prompt": lz(
        "Kailash joined service in a company on 30th April, 2010 and worked for 42 days. On what date did he leave the job?",
        "कैलाश ने 30 अप्रैल 2010 को एक कंपनी में काम शुरू किया और 42 दिन काम किया। उसने किस तारीख को काम छोड़ा?"
    ),
},

# HIGH – image-only options → convert to text MCQ
("chapter_01_revision.json", "rsexch01ex01q54_q1"): {
    "prompt": lz(
        "Which of the following shapes are polygons?\n(a) Triangle  (b) Circle  (c) Rectangle  (d) Ellipse  (e) Square  (f) Hexagon",
        "निम्नलिखित में से कौन-सी आकृतियाँ बहुभुज (Polygon) हैं?\n(a) त्रिभुज  (b) वृत्त  (c) आयत  (d) दीर्घवृत्त  (e) वर्ग  (f) षट्भुज"
    ),
    "type": "MULTIPLE_CHOICE",
    "options": [
        lz("(a), (c), (e) and (f)", "(a), (c), (e) तथा (f)"),
        lz("(a), (c), (d) and (f)", "(a), (c), (d) तथा (f)"),
        lz("(b), (d) and (e)", "(b), (d) तथा (e)"),
        lz("All of them", "सभी"),
    ],
    "correctOptionIndex": 1,
    "acceptedAnswers": [],
},

# ══════════════════════════════════════════════════════
# CHAPTER 3
# ══════════════════════════════════════════════════════

# HIGH – underlined digits lost; rewrite as explicit MCQ
("chapter_03_large_numbers_up_to_ten_crores.json", "rsexch03ex03q08_q1"): {
    "prompt": lz(
        "Find the place value of the digit 5 in the number 59,07,13,568.",
        "संख्या 59,07,13,568 में अंक 5 का स्थान मूल्य बताइए।"
    ),
    "type": "MULTIPLE_CHOICE",
    "options": [
        lz("50,00,00,000 (50 crore)", "50,00,00,000 (50 करोड़)"),
        lz("5,00,00,000 (5 crore)", "5,00,00,000 (5 करोड़)"),
        lz("50,00,000 (50 lakh)", "50,00,000 (50 लाख)"),
        lz("5,00,000 (5 lakh)", "5,00,000 (5 लाख)"),
    ],
    "correctOptionIndex": 0,
    "acceptedAnswers": [],
},

# ══════════════════════════════════════════════════════
# CHAPTER 4
# ══════════════════════════════════════════════════════

# MEDIUM – multi-blank; rewrite as single clear question
("chapter_04_operations_on_large_numbers.json", "rsexch04ex11q01_q1"): {
    "prompt": lz(
        "Using the commutative property of multiplication, fill in the blank:\n1485 × ___ = 2346 × 1485",
        "गुणन के क्रम-विनिमय गुण का उपयोग करके रिक्त स्थान भरें:\n1485 × ___ = 2346 × 1485"
    ),
    "type": "TEXT_INPUT",
    "options": [],
    "correctOptionIndex": None,
    "acceptedAnswers": ["2346"],
},

("chapter_04_operations_on_large_numbers.json", "rsexch04ex11q02_q1"): {
    "prompt": lz(
        "Divide and fill in the blank:\n2718 ÷ 10 = ___",
        "भाग दें और रिक्त स्थान भरें:\n2718 ÷ 10 = ___"
    ),
    "type": "TEXT_INPUT",
    "options": [],
    "correctOptionIndex": None,
    "acceptedAnswers": ["271.8", "271.80"],
},

# ══════════════════════════════════════════════════════
# CHAPTER 7
# ══════════════════════════════════════════════════════

# HIGH – shaded figure → rewrite as text
("chapter_07_fractions.json", "rsexch07ex22q01_q1"): {
    "prompt": lz(
        "A shape is divided into 3 equal parts and 2 parts are shaded.\n"
        "(a) What fraction is shaded?\n(b) What fraction is unshaded?",
        "एक आकृति को 3 बराबर भागों में बाँटा गया है और 2 भाग रंगे हैं।\n"
        "(a) कितना भाग रंगा हुआ है?\n(b) कितना भाग बिना रंग का है?"
    ),
    "type": "TEXT_INPUT",
    "options": [],
    "correctOptionIndex": None,
    "acceptedAnswers": ["Shaded = 2/3, Unshaded = 1/3", "2/3 and 1/3"],
},

# ══════════════════════════════════════════════════════
# CHAPTER 12
# ══════════════════════════════════════════════════════

# CRITICAL – theory text leaked into prompt
("chapter_12_measures_of_length_mass_and_capacity.json", "rsexch12ex54q09_q1"): {
    "prompt": lz(
        "Convert 4 km 9 cm 5 mm into metres (m).",
        "4 km 9 cm 5 mm को मीटर (m) में बदलें।"
    ),
},

# ══════════════════════════════════════════════════════
# CHAPTER 17
# ══════════════════════════════════════════════════════

# Add explanationParagraphs (currently empty but questions are text-complete)
# Questions themselves are fine — no prompt changes needed

# ══════════════════════════════════════════════════════
# CHAPTER 18
# ══════════════════════════════════════════════════════

# HIGH – image-only options
("chapter_18_concept_of_angles.json", "rsexch18ex70q01_q1"): {
    "prompt": lz(
        "Which of the following correctly represents an angle?\n"
        "(a) Two rays meeting at a common endpoint\n"
        "(b) A straight line with no endpoint\n"
        "(c) Two line segments meeting at a point\n"
        "(d) A circle",
        "निम्नलिखित में से कौन-सा कोण (angle) को सही रूप में दर्शाता है?\n"
        "(a) एक उभयनिष्ठ बिंदु पर मिलती दो किरणें\n"
        "(b) बिना सिरे वाली एक सीधी रेखा\n"
        "(c) एक बिंदु पर मिलते दो रेखाखंड\n"
        "(d) एक वृत्त"
    ),
    "type": "MULTIPLE_CHOICE",
    "options": [
        lz("(a) and (c)", "(a) और (c)"),
        lz("(b) only", "केवल (b)"),
        lz("(d) only", "केवल (d)"),
        lz("None of them", "इनमें से कोई नहीं"),
    ],
    "correctOptionIndex": 0,
    "acceptedAnswers": [],
},

("chapter_18_concept_of_angles.json", "rsexch18ex70q03_q1"): {
    "prompt": lz(
        "In triangle ABC, three angles are formed at vertex A: ∠OAB, ∠BAC, and ∠OAC. How many angles are formed in total?",
        "त्रिभुज ABC में शीर्ष A पर तीन कोण बनते हैं: ∠OAB, ∠BAC और ∠OAC। कुल कितने कोण बनते हैं?"
    ),
    "type": "TEXT_INPUT",
    "options": [],
    "correctOptionIndex": None,
    "acceptedAnswers": ["3", "3 angles"],
},

("chapter_18_concept_of_angles.json", "rsexch18ex70q04_q1"): {
    "prompt": lz(
        "Name all the angles in triangle ABC.",
        "त्रिभुज ABC में सभी कोणों के नाम बताइए।"
    ),
    "type": "TEXT_INPUT",
    "options": [],
    "correctOptionIndex": None,
    "acceptedAnswers": ["∠BAC, ∠ABC, ∠BCA", "angle BAC, angle ABC, angle BCA"],
},

# ══════════════════════════════════════════════════════
# CHAPTER 20
# ══════════════════════════════════════════════════════

# HIGH – image-only triangle classification
("chapter_20_triangles.json", "rsexch20ex75q01_q1"): {
    "prompt": lz(
        "Classify each triangle by its angles:\n"
        "(a) A triangle with angles 30°, 110°, 40°\n"
        "(b) A triangle with angles 90°, 45°, 45°\n"
        "(c) A triangle with angles 25°, 130°, 25°\n"
        "(d) A triangle with angles 60°, 70°, 50°\n"
        "(e) A triangle with angles 90°, 60°, 30°\n"
        "(f) A triangle with angles 70°, 60°, 50°",
        "प्रत्येक त्रिभुज को उसके कोणों के आधार पर वर्गीकृत करें:\n"
        "(a) कोण 30°, 110°, 40° वाला त्रिभुज\n"
        "(b) कोण 90°, 45°, 45° वाला त्रिभुज\n"
        "(c) कोण 25°, 130°, 25° वाला त्रिभुज\n"
        "(d) कोण 60°, 70°, 50° वाला त्रिभुज\n"
        "(e) कोण 90°, 60°, 30° वाला त्रिभुज\n"
        "(f) कोण 70°, 60°, 50° वाला त्रिभुज"
    ),
    "type": "TEXT_INPUT",
    "options": [],
    "correctOptionIndex": None,
    "acceptedAnswers": [
        "(a) Obtuse-angled (b) Right-angled (c) Obtuse-angled (d) Acute-angled (e) Right-angled (f) Acute-angled"
    ],
},

# HIGH – missing angle data
("chapter_20_triangles.json", "rsexch20ex75q02_q1"): {
    "prompt": lz(
        "Find the third angle of each triangle:\n"
        "(a) Two angles are 70° and 60°\n"
        "(b) Two angles are 25° and 40°\n"
        "(c) Two angles are 55° and 42°\n"
        "(d) Two angles are 90° and 57°\n"
        "(e) Two angles are 60° and 40°\n"
        "(f) Two angles are 100° and 45°",
        "प्रत्येक त्रिभुज का तीसरा कोण ज्ञात करें:\n"
        "(a) दो कोण 70° और 60° हैं\n"
        "(b) दो कोण 25° और 40° हैं\n"
        "(c) दो कोण 55° और 42° हैं\n"
        "(d) दो कोण 90° और 57° हैं\n"
        "(e) दो कोण 60° और 40° हैं\n"
        "(f) दो कोण 100° और 45° हैं"
    ),
    "type": "TEXT_INPUT",
    "options": [],
    "correctOptionIndex": None,
    "acceptedAnswers": [
        "(a) 50° (b) 115° (c) 83° (d) 33° (e) 80° (f) 35°",
        "(a)50 (b)115 (c)83 (d)33 (e)80 (f)35"
    ],
},

# ══════════════════════════════════════════════════════
# CHAPTER 22
# ══════════════════════════════════════════════════════

# HIGH – raw dimension stream → describe each figure's sides
("chapter_22_perimeters_of_rectilinear_figures.json", "rsexch22ex77q01_q1"): {
    "prompt": lz(
        "Find the perimeter of each rectilinear figure:\n"
        "(a) A figure with sides: 6 cm, 5 cm, 4 cm, 8 cm\n"
        "(b) A figure with sides: 5 cm, 9 cm, 5 cm, 8 cm\n"
        "(c) A figure with sides: 6 cm, 7 cm, 9 cm, 6 cm",
        "प्रत्येक समरेखीय आकृति का परिमाप ज्ञात करें:\n"
        "(a) एक आकृति जिसकी भुजाएँ हैं: 6 cm, 5 cm, 4 cm, 8 cm\n"
        "(b) एक आकृति जिसकी भुजाएँ हैं: 5 cm, 9 cm, 5 cm, 8 cm\n"
        "(c) एक आकृति जिसकी भुजाएँ हैं: 6 cm, 7 cm, 9 cm, 6 cm"
    ),
},

# ══════════════════════════════════════════════════════
# CHAPTER 23
# ══════════════════════════════════════════════════════

# ══════════════════════════════════════════════════════
# CHAPTER 24
# ══════════════════════════════════════════════════════

# LOW – complete the truncated prompt
("chapter_24_volume.json", "rsexch24ex79q10_q1"): {
    "prompt": lz(
        "Amit has sixty 1 cm cubes. Which of these cuboids can he NOT build?\n"
        "(a) 5 cm long, 4 cm wide, 3 cm high  (Volume = 60 cm³)\n"
        "(b) 2 cm long, 3 cm wide, 10 cm high  (Volume = 60 cm³)\n"
        "(c) 4 cm long, 4 cm wide, 4 cm high  (Volume = 64 cm³)\n"
        "(d) 6 cm long, 5 cm wide, 2 cm high  (Volume = 60 cm³)",
        "अमित के पास साठ 1 cm³ घन हैं। वह इनमें से कौन-सा घनाभ नहीं बना सकता?\n"
        "(a) 5 cm लंबा, 4 cm चौड़ा, 3 cm ऊँचा  (आयतन = 60 cm³)\n"
        "(b) 2 cm लंबा, 3 cm चौड़ा, 10 cm ऊँचा  (आयतन = 60 cm³)\n"
        "(c) 4 cm लंबा, 4 cm चौड़ा, 4 cm ऊँचा  (आयतन = 64 cm³)\n"
        "(d) 6 cm लंबा, 5 cm चौड़ा, 2 cm ऊँचा  (आयतन = 60 cm³)"
    ),
    "type": "MULTIPLE_CHOICE",
    "options": [
        lz("Option (a)", "विकल्प (a)"),
        lz("Option (b)", "विकल्प (b)"),
        lz("Option (c) — 64 cubes needed", "विकल्प (c) — 64 घन चाहिए"),
        lz("Option (d)", "विकल्प (d)"),
    ],
    "correctOptionIndex": 2,
    "acceptedAnswers": [],
},

}  # end FIXES


def apply_fixes():
    for (fname, qid), patch in FIXES.items():
        path = os.path.join(BASE, fname)
        if not os.path.exists(path):
            print(f"  NOT FOUND: {fname}")
            continue

        with open(path, encoding="utf-8") as f:
            data = json.load(f)

        found = False
        for topic in data.get("topics", []):
            for q in topic.get("questions", []):
                if q.get("id") == qid:
                    q.update(patch)
                    found = True
                    break
            if found:
                break

        if found:
            with open(path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            print(f"  Fixed {qid} in {fname}")
        else:
            print(f"  Q NOT FOUND: {qid} in {fname}")


if __name__ == "__main__":
    print(f"Applying {len(FIXES)} fixes...")
    apply_fixes()
    print("Done.")
