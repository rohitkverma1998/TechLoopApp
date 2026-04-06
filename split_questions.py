#!/usr/bin/env python3
"""
Split multi-sub-question entries into individual questions.
Each (a), (b), (c)... sub-part becomes its own question entry.
"""
import json
import os

BASE = os.path.join(os.path.dirname(__file__),
    "app", "src", "main", "assets", "subject_packs", "class5_rs_aggarwal_math")

def loc(en, hi=None):
    return {"english": en, "hindi": hi if hi else en}

def mcq(qid, prompt_en, prompt_hi, options_en, options_hi, correct_idx,
        wrong_en, wrong_hi, support_en, support_hi, reteach_en, reteach_hi,
        mistake="GENERAL"):
    return {
        "id": qid,
        "prompt": loc(prompt_en, prompt_hi),
        "type": "MULTIPLE_CHOICE",
        "options": [loc(options_en[i], options_hi[i]) for i in range(len(options_en))],
        "correctOptionIndex": correct_idx,
        "acceptedAnswers": [],
        "hint": None,
        "wrongReason": loc(wrong_en, wrong_hi),
        "supportExample": loc(support_en, support_hi),
        "mistakeType": mistake,
        "reteachTitle": loc("Solution", "हल"),
        "reteachParagraphs": [loc(reteach_en, reteach_hi)],
    }

def text_q(qid, prompt_en, prompt_hi, accepted, wrong_en, wrong_hi,
           support_en, support_hi, reteach_en, reteach_hi, mistake="GENERAL"):
    return {
        "id": qid,
        "prompt": loc(prompt_en, prompt_hi),
        "type": "TEXT_INPUT",
        "options": [],
        "correctOptionIndex": None,
        "acceptedAnswers": accepted,
        "hint": None,
        "wrongReason": loc(wrong_en, wrong_hi),
        "supportExample": loc(support_en, support_hi),
        "mistakeType": mistake,
        "reteachTitle": loc("Solution", "हल"),
        "reteachParagraphs": [loc(reteach_en, reteach_hi)],
    }


# ─── Chapter 07 Fractions ──────────────────────────────────────────────────

CH07_SPLITS = {
    "rsexch07ex22q03_q1": [
        mcq("rsexch07ex22q03_qa",
            "Are 3/5 and 4/5 like fractions or unlike fractions?",
            "3/5 और 4/5 — सजातीय भिन्न हैं या असजातीय?",
            ["Like fractions", "Unlike fractions"],
            ["सजातीय भिन्न", "असजातीय भिन्न"],
            0,
            "3/5 and 4/5 have the same denominator (5), so they are like fractions.",
            "3/5 और 4/5 का हर एक ही (5) है, इसलिए ये सजातीय भिन्न हैं।",
            "Like fractions share the same denominator.",
            "सजातीय भिन्नों का हर एक जैसा होता है।",
            "3/5 and 4/5 → both have denominator 5 → Like fractions.",
            "3/5 और 4/5 दोनों का हर 5 है → सजातीय भिन्न।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch07ex22q03_qb",
            "Are 2/5 and 2/7 like fractions or unlike fractions?",
            "2/5 और 2/7 — सजातीय भिन्न हैं या असजातीय?",
            ["Like fractions", "Unlike fractions"],
            ["सजातीय भिन्न", "असजातीय भिन्न"],
            1,
            "2/5 and 2/7 have different denominators (5 and 7), so they are unlike fractions.",
            "2/5 और 2/7 का हर अलग-अलग (5 और 7) है, इसलिए ये असजातीय भिन्न हैं।",
            "Unlike fractions have different denominators.",
            "असजातीय भिन्नों के हर अलग-अलग होते हैं।",
            "2/5 and 2/7 → denominators 5 and 7 (different) → Unlike fractions.",
            "2/5 और 2/7 के हर 5 और 7 (अलग) → असजातीय भिन्न।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch07ex22q03_qc",
            "Are 5/9 and 7/9 like fractions or unlike fractions?",
            "5/9 और 7/9 — सजातीय भिन्न हैं या असजातीय?",
            ["Like fractions", "Unlike fractions"],
            ["सजातीय भिन्न", "असजातीय भिन्न"],
            0,
            "5/9 and 7/9 have the same denominator (9), so they are like fractions.",
            "5/9 और 7/9 का हर एक ही (9) है, इसलिए ये सजातीय भिन्न हैं।",
            "Like fractions share the same denominator.",
            "सजातीय भिन्नों का हर एक जैसा होता है।",
            "5/9 and 7/9 → both denominator 9 → Like fractions.",
            "5/9 और 7/9 दोनों का हर 9 → सजातीय भिन्न।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch07ex22q03_qd",
            "Are 6/7 and 6/11 like fractions or unlike fractions?",
            "6/7 और 6/11 — सजातीय भिन्न हैं या असजातीय?",
            ["Like fractions", "Unlike fractions"],
            ["सजातीय भिन्न", "असजातीय भिन्न"],
            1,
            "6/7 and 6/11 have different denominators (7 and 11), so they are unlike fractions.",
            "6/7 और 6/11 का हर अलग-अलग (7 और 11) है, इसलिए ये असजातीय भिन्न हैं।",
            "Unlike fractions have different denominators.",
            "असजातीय भिन्नों के हर अलग-अलग होते हैं।",
            "6/7 and 6/11 → denominators 7 and 11 (different) → Unlike fractions.",
            "6/7 और 6/11 के हर 7 और 11 (अलग) → असजातीय भिन्न।",
            "CONCEPT_CONFUSION"),
    ],
    "rsexch07ex22q04_q1": [
        mcq("rsexch07ex22q04_qa",
            "Are 1/4 and 1/7 like fractions or unlike fractions?",
            "1/4 और 1/7 — सजातीय भिन्न हैं या असजातीय?",
            ["Like fractions", "Unlike fractions"],
            ["सजातीय भिन्न", "असजातीय भिन्न"],
            1,
            "1/4 and 1/7 have different denominators (4 and 7), so they are unlike fractions.",
            "1/4 और 1/7 का हर अलग-अलग (4 और 7) है → असजातीय भिन्न।",
            "Different denominators → Unlike fractions.",
            "अलग हर → असजातीय भिन्न।",
            "1/4 and 1/7 → denominators 4 and 7 → Unlike fractions.",
            "1/4 और 1/7 के हर 4 और 7 → असजातीय भिन्न।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch07ex22q04_qb",
            "Are 3/4 and 3/5 like fractions or unlike fractions?",
            "3/4 और 3/5 — सजातीय भिन्न हैं या असजातीय?",
            ["Like fractions", "Unlike fractions"],
            ["सजातीय भिन्न", "असजातीय भिन्न"],
            1,
            "3/4 and 3/5 have different denominators (4 and 5), so they are unlike fractions.",
            "3/4 और 3/5 का हर अलग-अलग (4 और 5) है → असजातीय भिन्न।",
            "Different denominators → Unlike fractions.",
            "अलग हर → असजातीय भिन्न।",
            "3/4 and 3/5 → denominators 4 and 5 → Unlike fractions.",
            "3/4 और 3/5 के हर 4 और 5 → असजातीय भिन्न।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch07ex22q04_qc",
            "Are 4/9 and 7/9 like fractions or unlike fractions?",
            "4/9 और 7/9 — सजातीय भिन्न हैं या असजातीय?",
            ["Like fractions", "Unlike fractions"],
            ["सजातीय भिन्न", "असजातीय भिन्न"],
            0,
            "4/9 and 7/9 have the same denominator (9), so they are like fractions — not unlike.",
            "4/9 और 7/9 का हर एक ही (9) है → ये सजातीय भिन्न हैं, असजातीय नहीं।",
            "Same denominator → Like fractions.",
            "एक ही हर → सजातीय भिन्न।",
            "4/9 and 7/9 → both denominator 9 → Like fractions.",
            "4/9 और 7/9 दोनों का हर 9 → सजातीय भिन्न।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch07ex22q04_qd",
            "Are 5/11 and 7/11 like fractions or unlike fractions?",
            "5/11 और 7/11 — सजातीय भिन्न हैं या असजातीय?",
            ["Like fractions", "Unlike fractions"],
            ["सजातीय भिन्न", "असजातीय भिन्न"],
            0,
            "5/11 and 7/11 have the same denominator (11), so they are like fractions — not unlike.",
            "5/11 और 7/11 का हर एक ही (11) है → ये सजातीय भिन्न हैं, असजातीय नहीं।",
            "Same denominator → Like fractions.",
            "एक ही हर → सजातीय भिन्न।",
            "5/11 and 7/11 → both denominator 11 → Like fractions.",
            "5/11 और 7/11 दोनों का हर 11 → सजातीय भिन्न।",
            "CONCEPT_CONFUSION"),
    ],
    "rsexch07ex22q05_q1": [
        mcq("rsexch07ex22q05_qa",
            "Is 5/3 a proper fraction or an improper fraction?",
            "5/3 — उचित भिन्न है या विषम भिन्न?",
            ["Proper fraction", "Improper fraction"],
            ["उचित भिन्न", "विषम भिन्न"],
            1,
            "5/3 is an improper fraction because the numerator (5) is greater than the denominator (3).",
            "5/3 विषम भिन्न है क्योंकि अंश (5) हर (3) से बड़ा है।",
            "Improper fraction: numerator ≥ denominator.",
            "विषम भिन्न: अंश ≥ हर।",
            "5/3 → numerator 5 > denominator 3 → Improper fraction.",
            "5/3 → अंश 5 > हर 3 → विषम भिन्न।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch07ex22q05_qb",
            "Is 6/7 a proper fraction or an improper fraction?",
            "6/7 — उचित भिन्न है या विषम भिन्न?",
            ["Proper fraction", "Improper fraction"],
            ["उचित भिन्न", "विषम भिन्न"],
            0,
            "6/7 is a proper fraction because the numerator (6) is less than the denominator (7).",
            "6/7 उचित भिन्न है क्योंकि अंश (6) हर (7) से छोटा है।",
            "Proper fraction: numerator < denominator.",
            "उचित भिन्न: अंश < हर।",
            "6/7 → numerator 6 < denominator 7 → Proper fraction.",
            "6/7 → अंश 6 < हर 7 → उचित भिन्न।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch07ex22q05_qc",
            "Is 3/3 a proper fraction or an improper fraction?",
            "3/3 — उचित भिन्न है या विषम भिन्न?",
            ["Proper fraction", "Improper fraction"],
            ["उचित भिन्न", "विषम भिन्न"],
            1,
            "3/3 is an improper fraction because the numerator equals the denominator (3 = 3). Proper fractions must have numerator strictly less than denominator.",
            "3/3 विषम भिन्न है क्योंकि अंश और हर बराबर हैं (3 = 3)। उचित भिन्न में अंश हर से कम होना चाहिए।",
            "Improper: numerator ≥ denominator.",
            "विषम: अंश ≥ हर।",
            "3/3 → numerator 3 = denominator 3 → Improper fraction.",
            "3/3 → अंश 3 = हर 3 → विषम भिन्न।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch07ex22q05_qd",
            "Is 8/11 a proper fraction or an improper fraction?",
            "8/11 — उचित भिन्न है या विषम भिन्न?",
            ["Proper fraction", "Improper fraction"],
            ["उचित भिन्न", "विषम भिन्न"],
            0,
            "8/11 is a proper fraction because the numerator (8) is less than the denominator (11).",
            "8/11 उचित भिन्न है क्योंकि अंश (8) हर (11) से छोटा है।",
            "Proper fraction: numerator < denominator.",
            "उचित भिन्न: अंश < हर।",
            "8/11 → numerator 8 < denominator 11 → Proper fraction.",
            "8/11 → अंश 8 < हर 11 → उचित भिन्न।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch07ex22q05_qe",
            "Is 6/1 a proper fraction or an improper fraction?",
            "6/1 — उचित भिन्न है या विषम भिन्न?",
            ["Proper fraction", "Improper fraction"],
            ["उचित भिन्न", "विषम भिन्न"],
            1,
            "6/1 is an improper fraction because the numerator (6) is much greater than the denominator (1).",
            "6/1 विषम भिन्न है क्योंकि अंश (6) हर (1) से बहुत बड़ा है।",
            "Improper: numerator ≥ denominator.",
            "विषम: अंश ≥ हर।",
            "6/1 → numerator 6 > denominator 1 → Improper fraction.",
            "6/1 → अंश 6 > हर 1 → विषम भिन्न।",
            "CONCEPT_CONFUSION"),
    ],
}

# ─── Chapter 20 Triangles ─────────────────────────────────────────────────

CH20_SPLITS = {
    "rsexch20ex75q01_q1": [
        mcq("rsexch20ex75q01_qa",
            "Classify the triangle with angles 30°, 110°, 40° by its angles.",
            "30°, 110°, 40° कोणों वाले त्रिभुज को उसके कोणों के आधार पर वर्गीकृत करें।",
            ["Acute-angled", "Right-angled", "Obtuse-angled"],
            ["न्यून कोण त्रिभुज", "समकोण त्रिभुज", "अधिक कोण त्रिभुज"],
            2,
            "One angle is 110° which is greater than 90°, so it is an obtuse-angled triangle.",
            "110° कोण 90° से बड़ा है, इसलिए यह अधिक कोण त्रिभुज है।",
            "If any angle > 90° → Obtuse-angled triangle.",
            "यदि कोई कोण > 90° → अधिक कोण त्रिभुज।",
            "30° + 110° + 40° = 180°. Angle 110° > 90° → Obtuse-angled triangle.",
            "30° + 110° + 40° = 180°। कोण 110° > 90° → अधिक कोण त्रिभुज।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch20ex75q01_qb",
            "Classify the triangle with angles 90°, 45°, 45° by its angles.",
            "90°, 45°, 45° कोणों वाले त्रिभुज को वर्गीकृत करें।",
            ["Acute-angled", "Right-angled", "Obtuse-angled"],
            ["न्यून कोण त्रिभुज", "समकोण त्रिभुज", "अधिक कोण त्रिभुज"],
            1,
            "One angle is exactly 90°, so it is a right-angled triangle.",
            "एक कोण ठीक 90° है, इसलिए यह समकोण त्रिभुज है।",
            "If one angle = 90° → Right-angled triangle.",
            "यदि एक कोण = 90° → समकोण त्रिभुज।",
            "90°, 45°, 45° → one angle is 90° → Right-angled triangle.",
            "90°, 45°, 45° → एक कोण 90° → समकोण त्रिभुज।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch20ex75q01_qc",
            "Classify the triangle with angles 25°, 130°, 25° by its angles.",
            "25°, 130°, 25° कोणों वाले त्रिभुज को वर्गीकृत करें।",
            ["Acute-angled", "Right-angled", "Obtuse-angled"],
            ["न्यून कोण त्रिभुज", "समकोण त्रिभुज", "अधिक कोण त्रिभुज"],
            2,
            "One angle is 130° which is greater than 90°, so it is an obtuse-angled triangle.",
            "130° कोण 90° से बड़ा है → अधिक कोण त्रिभुज।",
            "If any angle > 90° → Obtuse-angled triangle.",
            "यदि कोई कोण > 90° → अधिक कोण त्रिभुज।",
            "25° + 130° + 25° = 180°. Angle 130° > 90° → Obtuse-angled.",
            "25° + 130° + 25° = 180°। कोण 130° > 90° → अधिक कोण त्रिभुज।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch20ex75q01_qd",
            "Classify the triangle with angles 60°, 70°, 50° by its angles.",
            "60°, 70°, 50° कोणों वाले त्रिभुज को वर्गीकृत करें।",
            ["Acute-angled", "Right-angled", "Obtuse-angled"],
            ["न्यून कोण त्रिभुज", "समकोण त्रिभुज", "अधिक कोण त्रिभुज"],
            0,
            "All three angles (60°, 70°, 50°) are less than 90°, so it is an acute-angled triangle.",
            "तीनों कोण (60°, 70°, 50°) 90° से कम हैं → न्यून कोण त्रिभुज।",
            "If all angles < 90° → Acute-angled triangle.",
            "सभी कोण < 90° → न्यून कोण त्रिभुज।",
            "60° + 70° + 50° = 180°. All < 90° → Acute-angled.",
            "60° + 70° + 50° = 180°। सभी < 90° → न्यून कोण त्रिभुज।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch20ex75q01_qe",
            "Classify the triangle with angles 90°, 60°, 30° by its angles.",
            "90°, 60°, 30° कोणों वाले त्रिभुज को वर्गीकृत करें।",
            ["Acute-angled", "Right-angled", "Obtuse-angled"],
            ["न्यून कोण त्रिभुज", "समकोण त्रिभुज", "अधिक कोण त्रिभुज"],
            1,
            "One angle is exactly 90°, so it is a right-angled triangle.",
            "एक कोण ठीक 90° है → समकोण त्रिभुज।",
            "If one angle = 90° → Right-angled triangle.",
            "यदि एक कोण = 90° → समकोण त्रिभुज।",
            "90° + 60° + 30° = 180°. One angle 90° → Right-angled.",
            "90° + 60° + 30° = 180°। एक कोण 90° → समकोण त्रिभुज।",
            "CONCEPT_CONFUSION"),
        mcq("rsexch20ex75q01_qf",
            "Classify the triangle with angles 70°, 60°, 50° by its angles.",
            "70°, 60°, 50° कोणों वाले त्रिभुज को वर्गीकृत करें।",
            ["Acute-angled", "Right-angled", "Obtuse-angled"],
            ["न्यून कोण त्रिभुज", "समकोण त्रिभुज", "अधिक कोण त्रिभुज"],
            0,
            "All three angles (70°, 60°, 50°) are less than 90°, so it is an acute-angled triangle.",
            "तीनों कोण (70°, 60°, 50°) 90° से कम हैं → न्यून कोण त्रिभुज।",
            "If all angles < 90° → Acute-angled triangle.",
            "सभी कोण < 90° → न्यून कोण त्रिभुज।",
            "70° + 60° + 50° = 180°. All < 90° → Acute-angled.",
            "70° + 60° + 50° = 180°। सभी < 90° → न्यून कोण त्रिभुज।",
            "CONCEPT_CONFUSION"),
    ],
    "rsexch20ex75q02_q1": [
        text_q("rsexch20ex75q02_qa",
            "A triangle has two angles: 70° and 60°. Find the third angle.",
            "एक त्रिभुज के दो कोण 70° और 60° हैं। तीसरा कोण ज्ञात करें।",
            ["50", "50°"],
            "Third angle = 180° − (70° + 60°) = 180° − 130° = 50°.",
            "तीसरा कोण = 180° − (70° + 60°) = 180° − 130° = 50°।",
            "Sum of angles in a triangle = 180°. Third = 180° − (70° + 60°).",
            "त्रिभुज के कोणों का योग = 180°। तीसरा = 180° − (70° + 60°)।",
            "70° + 60° = 130°. Third angle = 180° − 130° = 50°.",
            "70° + 60° = 130°। तीसरा कोण = 180° − 130° = 50°।",
            "CONCEPT_CONFUSION"),
        text_q("rsexch20ex75q02_qb",
            "A triangle has two angles: 25° and 40°. Find the third angle.",
            "एक त्रिभुज के दो कोण 25° और 40° हैं। तीसरा कोण ज्ञात करें।",
            ["115", "115°"],
            "Third angle = 180° − (25° + 40°) = 180° − 65° = 115°.",
            "तीसरा कोण = 180° − (25° + 40°) = 180° − 65° = 115°।",
            "Sum of angles = 180°. Third = 180° − (25° + 40°).",
            "कोणों का योग = 180°। तीसरा = 180° − (25° + 40°)।",
            "25° + 40° = 65°. Third angle = 180° − 65° = 115°.",
            "25° + 40° = 65°। तीसरा कोण = 180° − 65° = 115°।",
            "CONCEPT_CONFUSION"),
        text_q("rsexch20ex75q02_qc",
            "A triangle has two angles: 55° and 42°. Find the third angle.",
            "एक त्रिभुज के दो कोण 55° और 42° हैं। तीसरा कोण ज्ञात करें।",
            ["83", "83°"],
            "Third angle = 180° − (55° + 42°) = 180° − 97° = 83°.",
            "तीसरा कोण = 180° − (55° + 42°) = 180° − 97° = 83°।",
            "Sum of angles = 180°. Third = 180° − (55° + 42°).",
            "कोणों का योग = 180°। तीसरा = 180° − (55° + 42°)।",
            "55° + 42° = 97°. Third angle = 180° − 97° = 83°.",
            "55° + 42° = 97°। तीसरा कोण = 180° − 97° = 83°।",
            "CONCEPT_CONFUSION"),
        text_q("rsexch20ex75q02_qd",
            "A triangle has two angles: 90° and 57°. Find the third angle.",
            "एक त्रिभुज के दो कोण 90° और 57° हैं। तीसरा कोण ज्ञात करें।",
            ["33", "33°"],
            "Third angle = 180° − (90° + 57°) = 180° − 147° = 33°.",
            "तीसरा कोण = 180° − (90° + 57°) = 180° − 147° = 33°।",
            "Sum of angles = 180°. Third = 180° − (90° + 57°).",
            "कोणों का योग = 180°। तीसरा = 180° − (90° + 57°)।",
            "90° + 57° = 147°. Third angle = 180° − 147° = 33°.",
            "90° + 57° = 147°। तीसरा कोण = 180° − 147° = 33°।",
            "CONCEPT_CONFUSION"),
        text_q("rsexch20ex75q02_qe",
            "A triangle has two angles: 60° and 40°. Find the third angle.",
            "एक त्रिभुज के दो कोण 60° और 40° हैं। तीसरा कोण ज्ञात करें।",
            ["80", "80°"],
            "Third angle = 180° − (60° + 40°) = 180° − 100° = 80°.",
            "तीसरा कोण = 180° − (60° + 40°) = 180° − 100° = 80°।",
            "Sum of angles = 180°. Third = 180° − (60° + 40°).",
            "कोणों का योग = 180°। तीसरा = 180° − (60° + 40°)।",
            "60° + 40° = 100°. Third angle = 180° − 100° = 80°.",
            "60° + 40° = 100°। तीसरा कोण = 180° − 100° = 80°।",
            "CONCEPT_CONFUSION"),
        text_q("rsexch20ex75q02_qf",
            "A triangle has two angles: 100° and 45°. Find the third angle.",
            "एक त्रिभुज के दो कोण 100° और 45° हैं। तीसरा कोण ज्ञात करें।",
            ["35", "35°"],
            "Third angle = 180° − (100° + 45°) = 180° − 145° = 35°.",
            "तीसरा कोण = 180° − (100° + 45°) = 180° − 145° = 35°।",
            "Sum of angles = 180°. Third = 180° − (100° + 45°).",
            "कोणों का योग = 180°। तीसरा = 180° − (100° + 45°)।",
            "100° + 45° = 145°. Third angle = 180° − 145° = 35°.",
            "100° + 45° = 145°। तीसरा कोण = 180° − 145° = 35°।",
            "CONCEPT_CONFUSION"),
    ],
}

# ─── Chapter 22 Perimeters ────────────────────────────────────────────────

CH22_SPLITS = {
    "rsexch22ex77q01_q1": [
        text_q("rsexch22ex77q01_qa",
            "Find the perimeter of a rectilinear figure with sides: 6 cm, 5 cm, 4 cm, 8 cm.",
            "एक आयताकार आकृति जिसकी भुजाएँ 6 सेमी, 5 सेमी, 4 सेमी, 8 सेमी हैं, उसका परिमाप ज्ञात करें।",
            ["23", "23 cm", "23cm"],
            "Perimeter = 6 + 5 + 4 + 8 = 23 cm.",
            "परिमाप = 6 + 5 + 4 + 8 = 23 सेमी।",
            "Perimeter = sum of all sides.",
            "परिमाप = सभी भुजाओं का योग।",
            "6 + 5 + 4 + 8 = 23 cm.",
            "6 + 5 + 4 + 8 = 23 सेमी।",
            "MEASUREMENT_ESTIMATE"),
        text_q("rsexch22ex77q01_qb",
            "Find the perimeter of a rectilinear figure with sides: 5 cm, 9 cm, 5 cm, 8 cm.",
            "एक आयताकार आकृति जिसकी भुजाएँ 5 सेमी, 9 सेमी, 5 सेमी, 8 सेमी हैं, उसका परिमाप ज्ञात करें।",
            ["27", "27 cm", "27cm"],
            "Perimeter = 5 + 9 + 5 + 8 = 27 cm.",
            "परिमाप = 5 + 9 + 5 + 8 = 27 सेमी।",
            "Perimeter = sum of all sides.",
            "परिमाप = सभी भुजाओं का योग।",
            "5 + 9 + 5 + 8 = 27 cm.",
            "5 + 9 + 5 + 8 = 27 सेमी।",
            "MEASUREMENT_ESTIMATE"),
        text_q("rsexch22ex77q01_qc",
            "Find the perimeter of a rectilinear figure with sides: 6 cm, 7 cm, 9 cm, 6 cm.",
            "एक आयताकार आकृति जिसकी भुजाएँ 6 सेमी, 7 सेमी, 9 सेमी, 6 सेमी हैं, उसका परिमाप ज्ञात करें।",
            ["28", "28 cm", "28cm"],
            "Perimeter = 6 + 7 + 9 + 6 = 28 cm.",
            "परिमाप = 6 + 7 + 9 + 6 = 28 सेमी।",
            "Perimeter = sum of all sides.",
            "परिमाप = सभी भुजाओं का योग।",
            "6 + 7 + 9 + 6 = 28 cm.",
            "6 + 7 + 9 + 6 = 28 सेमी।",
            "MEASUREMENT_ESTIMATE"),
    ],
}

# ─── Chapter 23 Area ──────────────────────────────────────────────────────

CH23_SPLITS = {
    # Intentionally omitted because this exercise was removed from the pack.
    "rsexch23ex78q17_q1": [],
}


def split_in_file(json_path, splits_dict):
    with open(json_path, encoding="utf-8") as f:
        data = json.load(f)

    changed = 0
    new_topics = []
    for topic in data["topics"]:
        topic_changed = False
        questions = topic["questions"]
        new_questions = []
        for q in questions:
            if q["id"] in splits_dict:
                replacements = splits_dict[q["id"]]
                new_questions.extend(replacements)
                print(f"  Replaced {q['id']} with {len(replacements)} questions")
                topic_changed = True
            else:
                new_questions.append(q)
        topic["questions"] = new_questions
        if new_questions:
            new_topics.append(topic)
        else:
            print(f"  Removed topic {topic['id']} because it has no remaining questions")
            topic_changed = True

        if topic_changed:
            changed += 1

    data["topics"] = new_topics

    if changed:
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    return changed


def main():
    tasks = [
        ("chapter_07_fractions.json", CH07_SPLITS),
        ("chapter_20_triangles.json", CH20_SPLITS),
        ("chapter_22_perimeters_of_rectilinear_figures.json", CH22_SPLITS),
        ("chapter_23_area.json", CH23_SPLITS),
    ]
    total = 0
    for fname, splits in tasks:
        path = os.path.join(BASE, fname)
        print(f"\nProcessing {fname}...")
        n = split_in_file(path, splits)
        total += n
    print(f"\nDone. Split {total} multi-sub-question entries.")


if __name__ == "__main__":
    main()
