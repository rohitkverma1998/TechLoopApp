from __future__ import annotations

import re
from fractions import Fraction as _Frac
from functools import reduce as _reduce
from math import gcd as _gcd
from pathlib import Path

import fitz


ROOT = Path(__file__).resolve().parents[1]
PDF_PATH = ROOT.parent / "Class_V_Maths_Book" / "RS-Aggarwal_Math_Book_Class_V.pdf.pdf"
ANSWER_PAGE_START_INDEX = 310

CHAPTER_PAGE_RANGES: dict[int, tuple[int, int]] = {
    1: (7, 12),
    2: (13, 17),
    3: (18, 30),
    4: (31, 55),
    5: (56, 59),
    6: (60, 82),
    7: (83, 96),
    8: (97, 109),
    9: (110, 126),
    10: (127, 162),
    11: (163, 173),
    12: (174, 197),
    13: (198, 200),
    14: (201, 205),
    15: (206, 223),
    16: (224, 229),
    17: (230, 234),
    18: (235, 248),
    19: (249, 256),
    20: (257, 269),
    21: (270, 279),
    22: (280, 285),
    23: (286, 296),
    24: (297, 302),
    25: (303, 310),
}

CHAPTER_EXERCISES: dict[int, list[int]] = {
    1: [1],
    2: [2],
    3: [3, 4, 5],
    4: [6, 7, 8, 9, 10, 11, 12, 13, 14],
    5: [15, 16],
    6: [17, 18, 19, 20, 21],
    7: [22, 23, 24, 25],
    8: [26, 27, 28, 29, 30, 31, 32],
    9: [33, 34, 35, 36, 37, 38],
    10: [39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50],
    11: [51, 52],
    12: [53, 54, 55, 56, 57, 58, 59],
    13: [60],
    14: [61],
    15: [62, 63, 64, 65],
    16: [66, 67, 68],
    17: [69],
    18: [70, 71, 72],
    19: [73],
    20: [74, 75],
    21: [76],
    22: [77],
    23: [78],
    24: [79],
    25: [80, 81],
}

EXERCISE_TO_CHAPTER = {
    exercise_number: chapter_number
    for chapter_number, exercise_numbers in CHAPTER_EXERCISES.items()
    for exercise_number in exercise_numbers
}

SOURCE_CUT_MARKERS = (
    "ASSESSMENT",
    "Assessment",
    "QUESTION BAG",
    "Question bag",
    "Solved Examples",
)
ANSWER_CUT_MARKERS = (
    "ASSESSMENT",
    "Assessment",
    "QUESTION BAG",
    "Question bag",
)
QUESTION_PATTERN = re.compile(r"(?m)^\s*([1-9]\d?)\.\s+")
PART_PATTERN = re.compile(r"\(([a-z])\)", re.IGNORECASE)
PART_KEY_PATTERN = re.compile(r"\(([a-z])\)", re.IGNORECASE)
EXAMPLE_PATTERN = re.compile(r"\bExample\s+\d+\b", re.IGNORECASE)
ACTIVITY_PATTERN = re.compile(r"\bActivity(?:\s+Time|\s+\d+)\b", re.IGNORECASE)

PROMPT_TRIM_MARKERS = (
    "Things to Remember",
    "Summary",
    "Question bag",
    "Question Bag",
    "ASSESSMENT",
    "Assessment",
    "Solved Examples",
)

NOTEBOOK_ACCEPTED_ANSWERS = ["done", "completed", "drawn", "ready", "finished"]
QUESTION_VERBS = (
    "add",
    "answer",
    "arrange",
    "classify",
    "compare",
    "construct",
    "convert",
    "convey",
    "divide",
    "draw",
    "express",
    "fill",
    "find",
    "give",
    "identify",
    "look",
    "mark",
    "match",
    "measure",
    "multiply",
    "name",
    "observe",
    "read",
    "reduce",
    "rewrite",
    "round",
    "simplify",
    "state",
    "subtract",
    "think",
    "tick",
    "use",
    "verify",
    "what",
    "which",
    "write",
)
QUESTION_START_PATTERN = re.compile(
    rf"\b(?:{'|'.join(re.escape(verb) for verb in QUESTION_VERBS)})\b",
    re.IGNORECASE,
)
HEADING_OPENERS = (
    "find ",
    "which of the following",
    "by using suitable grouping",
    "fill in the blanks",
    "identify ",
    "mark ",
    "match ",
    "draw ",
    "measure ",
    "convert ",
    "express ",
    "write ",
    "state ",
    "tick ",
    "use ",
)

MANUAL_SOURCE_OVERRIDES: dict[int, dict[int, str]] = {
    3: {
        13: (
            "Answer the following. "
            "(a) What comes just after 9536999? "
            "(b) What comes just before 9900000? "
            "(c) What comes just after 13700899? "
            "(d) What comes just before 10000000?"
        ),
    },
    4: {
        4: (
            "Encircle the largest number in each of the following. "
            "(a) 31650829, 307482134, 4536794, 41035106, 238590746 "
            "(b) 102234102, 93645753, 27810591, 102240003, 93646800 "
            "(c) 9037848, 12345716, 101010706, 91537964, 100718967 "
            "(d) 9000009, 90000001, 9935469, 87590909, 88888888"
        ),
    },
    7: {
        9: "A number exceeds 35637844 by 7674156. What is that number?",
    },
    9: {
        11: (
            "In an examination, 506212 candidates could get through. Out of these, 197538 passed "
            "in first division, 238604 passed in second division. How many passed in third division?"
        ),
    },
    17: {
        15: "Write all odd numbers between (a) 64 and 80 (b) 624 and 640.",
    },
    18: {
        3: "List all prime numbers between 1 and 50.",
        4: "List all prime numbers between 51 and 100.",
        5: "List all twin-primes between 1 and 50.",
        6: "List seven consecutive composite numbers less than 100.",
        7: "Give four examples of pairs of co-primes.",
        10: "Give examples of 4 pairs of prime numbers which have only one composite number between them.",
        11: (
            "Fill in the blanks. "
            "(a) ..................... is a factor of every number. "
            "(b) The least prime number is ..................... . "
            "(c) The smallest composite number is ..................... . "
            "(d) Each prime number has exactly ..................... factors. "
            "(e) ..................... is neither prime nor composite. "
            "(f) ..................... is the only even prime number. "
            "(g) The largest 2-digit prime number is ..................... ."
        ),
    },
    19: {
        6: (
            "Find the HCF of the following numbers, using division method. "
            "(a) 60, 96 and 150 "
            "(b) 75, 100 and 140 "
            "(c) 270, 945 and 2175 "
            "(d) 902, 1394 and 3321"
        ),
    },
    23: {
        18: "Reduce each of the following fractions to simplest form: 51/68.",
    },
    24: {
        4: (
            "Arrange the following fractions in descending order. "
            "(a) 3/7, 3/5, 3/11, 3/8, 3/14 "
            "(b) 8/9, 8/15, 8/11, 8/17, 8/13 "
            "(c) 5/8, 5/6, 5/11, 5/14, 5/12 "
            "(d) 10/21, 10/17, 10/19, 10/23, 10/11"
        ),
    },
    27: {
        17: "Add the fractions: 1/2 + 1/3 + 1/4 + 1/6.",
        18: "Add the fractions: 1 + 2/3 + 3/4 + 5/8.",
    },
    28: {
        10: "Find the sum: 3 2/5 + 1 1/10 + 4/15.",
        12: "Find the sum: 7 1/2 + 5/9 + 2/3.",
    },
    31: {
        10: "Find the result: 10 - 3 1/5 - 5 3/10.",
    },
    34: {
        18: "Multiply: 0.0325 by 0.09.",
    },
    36: {
        18: (
            "3/8 of the population of a village consists of women. If 2/3 of the women of that village "
            "are illiterate, what fraction of the population in that village consists of illiterate women?"
        ),
    },
    39: {
        5: (
            "Write each of the following in short form. "
            "(a) 20 + 7 + 3/10 + 6/100 "
            "(b) 400 + 30 + 6 + 1/10 + 3/100 + 5/1000 "
            "(c) 40 + 8/10 + 3/100 + 4/1000 "
            "(d) 500 + 7 + 2/100 + 3/1000 "
            "(e) 3000 + 200 + 1 + 6/10 + 7/1000 "
            "(f) 6000 + 9 + 7/10 + 6/1000"
        ),
    },
    40: {
        4: (
            "Arrange the following decimals in descending order. "
            "(a) 0.1, 0.01, 0.001, 1.1, 1.01 "
            "(b) 3.1, 0.75, 3.01, 0.57, 2.3, 2.03 "
            "(c) 1.93, 2.01, 2.1, 1.9, 2.13, 1.87 "
            "(d) 55.5, 5.55, 55.05, 5.5, 5.05, 55.55 "
            "(e) 6.06, 6.6, 6.006, 0.66, 0.06, 0.6 "
            "(f) 2.002, 2.22, 2.02, 2.2, 2.012, 2.021 "
            "(g) 1.9, 2.6, 1.09, 2.06, 1.009, 2.006"
        ),
    },
    41: {
        12: (
            "Find the perimeter of a rectangular park whose length and breadth are "
            "45 1/2 m and 34 3/4 m respectively."
        ),
        14: (
            "A drum contained 60 litres of milk. Out of this, 15.75 litres of milk was taken out "
            "in one bucket and 8.5 litres in another bucket. How much milk is left in the drum?"
        ),
    },
    44: {
        29: "Verify that: 9.8 × 6.4 = 0.98 × 64.",
        30: "Verify that: 35.079 × 8.5 = 350.79 × 0.85.",
        31: "Verify that: 6.9 × 5.8 = 5.8 × 6.9.",
    },
    35: {
        20: "The cost of one pencil is 3 13/20 rupees. What is the cost of 12 such pencils?",
    },
    48: {
        22: (
            "Fill in the blanks. "
            "(a) 168.84 ÷ 14 = 1688.4 ÷ .............. "
            "(b) 32.87 ÷ 1.9 = 3.287 ÷ .............. "
            "(c) 3.288 ÷ 60 = .............. ÷ 5 "
            "(d) 36 ÷ 150 = 0.036 ÷ .............."
        ),
    },
    51: {
        12: (
            "Think of rounding to the nearest thousand. What numbers could be rounded to "
            "(a) 9000? (b) 18000? (c) 27000?"
        ),
    },
    53: {
        8: (
            "Convert: "
            "(a) 12 km 34 m into m "
            "(b) 50 km 9 m into m "
            "(c) 62 cm 5 mm into mm "
            "(d) 7 m 8 cm into cm"
        ),
    },
    55: {
        18: "A man covers 255 km in 6 hours on scooter at a uniform speed. Find his speed in km per hour.",
    },
    56: {
        9: "Subtract: 8 kg 25 g from 10 kg 10 g.",
        10: (
            "Nisha bought 8 kg 260 g apples, 6 kg 325 g chikoos and 9 kg 85 g guavas. "
            "What is the total weight of fruits bought by her?"
        ),
        11: "Ashu weighs 36 kg 540 g while his sister Nisha weighs 40 kg 125 g. Who weighs more and by how much?",
        12: "Shashi and Sakshi together weigh 73 kg 250 g. If Shashi weighs 38 kg 675 g, what is Shaksi's weight?",
        13: "If each bag of wheat weighs 25 kg 650 g, what is the total weight of 8 such bags?",
        14: "The total weight of 6 bags of rice is 52 kg 500 g and all the bags weigh equally. Find the weight of each bag.",
    },
    57: {
        1: (
            "Convert: "
            "(a) 8 L into mL "
            "(b) 3 L 360 mL into mL "
            "(c) 6 kL 250 L into L "
            "(d) 5 daL 6 L into L"
        ),
        2: (
            "Convert: "
            "(a) 65 kL 345 L into L "
            "(b) 5 kL 35 L into L "
            "(c) 36 kL 5 L into L"
        ),
        3: (
            "Convert: "
            "(a) 8 L 375 mL into mL "
            "(b) 37 L 65 mL into mL "
            "(c) 15 L 6 mL into mL"
        ),
    },
    60: {
        4: "Find the average of first nine counting numbers.",
    },
    64: {
        12: "Find the interval between 7:25 a.m. and 3:10 p.m.",
    },
    70: {
        1: "Which of the following figures represents an angle? (a) (b) (c) (d)",
        2: "Name the angles in each of the following figures. Also, name the vertex and the arms in each case. (a) (b)",
        3: "How many angles are formed in each of the following figures? Name them. (a) (b) (c) (d)",
        5: (
            "In the adjoining figure, name the points: "
            "(a) on the angle "
            "(b) in the interior of the angle "
            "(c) in the exterior of the angle."
        ),
    },
    71: {
        1: (
            "Measure the six angles shown in the book with a protractor and fill in the blanks: "
            "(a) angle PQR, (b) angle LMN, (c) angle XYZ, (d) angle DEF, (e) angle ABC, "
            "(f) angle RTS. Write the measurements in your notebook, then type done."
        ),
        8: (
            "Fill in the blanks. "
            "(a) When two rays form an angle, their common end point is called the ............ of the angle. "
            "(b) Angles are measured in ............ . "
            "(c) Angles are measured with the help of a ............ . "
            "(d) The measure of a right angle is ............ . "
            "(e) The measure of a ............ angle is 180 degrees."
        ),
    },
    76: {
        1: "In the given figure, O is the centre of a circle. Name the radii and the diameters in the circle.",
        2: "In the figure given here, O is the centre of the circle. Name the chords of the circle.",
        3: (
            "Fill in the blanks. "
            "(a) If A and B are two points on a circle, then the line segment AB is called a ............ of the circle. "
            "(b) A line segment passing through the centre of a circle with its end points on the circle is a ............ of the circle. "
            "(c) A diameter is the ............ chord of the circle."
        ),
    },
    79: {
        10: (
            "Amit has sixty 1 cm cubes. Which of these cuboids can he not build? "
            "(a) 5 cm long, 4 cm wide, 3 cm high "
            "(b) 2 cm long, 3 cm wide, 10 cm high "
            "(c) 4 cm long, 4 cm wide, 4 cm high "
            "(d) 6 cm long, 5 cm wide, 2 cm high"
        ),
        11: "Volume of a cuboid is 1/8 cu m. What is its volume in cu cm?",
    },
    51: {
        9: (
            "The population of an Indian state is 85642574. Write the population "
            "(a) to the nearest crore. "
            "(b) to the nearest lakh. "
            "(c) to the nearest thousand."
        ),
    },
    80: {
        1: (
            "Students present during the week are Monday 36, Tuesday 30, Wednesday 33, "
            "Thursday 39, Friday 30 and Saturday 27. Take one symbol to represent 3 students "
            "and draw a pictograph. Then type done."
        ),
        2: (
            "A village has 6500 men, 4500 women and 1500 children. Take one men symbol = 500 men, "
            "one women symbol = 500 women and one children symbol = 500 children, and draw a "
            "pictograph to show the population. Then type done."
        ),
        3: (
            "Vehicles running in a day on the roads of Meerut city are: cycles 800, scooters 500, "
            "cars 300 and buses 200. Convey the information through a pictograph. Then type done."
        ),
        4: (
            "A pictograph shows students travelling to school: on foot = 4 pictures, on bicycle = "
            "5 pictures, by car = 1 picture and by bus = 1 picture. Each picture represents 100 "
            "students. Answer the following questions. (a) How many students go to school on foot? "
            "(b) How many students use school bus? (c) How many students use bicycle to go to their "
            "school? (d) How many students in all are there in the school? (e) What mode is adopted "
            "by maximum number of students?"
        ),
        5: (
            "In a colony of Delhi, persons knowing different languages are Hindi 500, Tamil 450, "
            "Bengali 250 and Malayalam 150. Draw a pictograph which conveys the information. "
            "Then type done."
        ),
    },
    81: {
        1: (
            "The number of absentees during the first five days of a week is: day I = 30, day II = 45, "
            "day III = 20, day IV = 50 and day V = 15. Draw a bar graph to convey the information. "
            "Then type done."
        ),
        2: (
            "Amit's marks in five consecutive tests are: I = 60, II = 80, III = 40, IV = 70, V = 90. "
            "Draw a bar graph showing the marks obtained by Amit in these tests. Then type done."
        ),
        3: (
            "There are 300 students in a school. Tamil = 125, Telugu = 75, Malayalam = 50, "
            "Hindi = 25 and Bengali = 25. Draw a bar graph to represent the data. Then type done."
        ),
        4: (
            "The number of different books in a library is: English 450, Mathematics 600, General "
            "Knowledge 150, Science 250 and Tamil 200. Draw a bar graph to represent the data. "
            "Then type done."
        ),
        5: (
            "Rainfall in cm for six months is: June 5, July 12, August 6, September 3, October 2 "
            "and November 4. Answer the following questions. (a) In which month was the rainfall "
            "maximum? (b) How much was the rainfall in September? (c) Which month was the driest?"
        ),
        6: (
            "A bar graph gives the number of families by number of members: 1 member = 5 families, "
            "2 members = 10 families, 3 members = 40 families, 4 members = 45 families, "
            "5 members = 30 families and 6 members = 20 families. Answer the following questions. "
            "(a) What information does the bar graph give? (b) How many families have 3 members? "
            "(c) How many families have 6 members?"
        ),
    },
}

ADDITIONAL_MANUAL_SOURCE_OVERRIDES: dict[int, dict[int, str]] = {
    1: {
        11: (
            "Write the smallest number of different digits formed by using the digits 5, 9, 3, 1 and 0. "
            "Also write the greatest number of different digits formed by using the digits 2, 0, 8, 7 and 5."
        ),
        31: (
            "Fill in the missing numerals. "
            "(a) 9/16 = 27/____ "
            "(b) 9/13 = ____/78 "
            "(c) 11/17 = ____/51"
        ),
        34: (
            "Arrange the following fractions in ascending order. "
            "(a) 2/7, 3/7, 6/7, 5/7 "
            "(b) 13/19, 15/19, 2/19, 10/19 "
            "(c) 1/7, 1/4, 1/2, 1/5, 1/3 "
            "(d) 5/6, 5/10, 5/8, 5/11, 5/9"
        ),
        35: (
            "Add. "
            "(a) 3/7 + 2/7 "
            "(b) 2/9 + 5/9 "
            "(c) 3/8 + 4/8 "
            "(d) 3/11 + 4/11 + 2/11"
        ),
        36: (
            "Find the difference. "
            "(a) 4/5 - 2/5 "
            "(b) 5/7 - 2/7 "
            "(c) 9/13 - 7/13 "
            "(d) 11/15 - 7/15"
        ),
        37: (
            "Convert each of the following mixed numerals into an improper fraction. "
            "(a) 6 5/7 "
            "(b) 9 3/8 "
            "(c) 5 11/17"
        ),
        38: (
            "Convert the following improper fractions into mixed numerals. "
            "(a) 107/9 "
            "(b) 189/11 "
            "(c) 212/15"
        ),
        28: (
            "Circle the prime numbers: 2, 5, 9, 13, 17, 21, 27, 31, 37, 43, 49, 54, 63, 68, "
            "69, 71, 73, 75, 77, 83, 85, 87, 91, 93, 95, 97, 99."
        ),
        33: (
            "Put the correct symbol > or < in the placeholders. "
            "(a) 5/9 ... 8/9 "
            "(b) 19/20 ... 17/20 "
            "(c) 3/8 ... 7/8 "
            "(d) 7/11 ... 7/15 "
            "(e) 15/23 ... 15/19 "
            "(f) 21/20 ... 21/29"
        ),
        47: (
            "Change: "
            "(a) 2 hm 3 dam into metres "
            "(b) 8 m 56 mm into mm "
            "(c) 3 quintals 65 kg into kg "
            "(d) 2 kL 5 L into L "
            "(e) 15 L 730 mL into mL "
            "(f) 12 kg 220 g into g"
        ),
        48: (
            "Change: "
            "(a) 5530 mm into m and cm "
            "(b) 2685 mL into L and mL "
            "(c) 565 kg into quintals and kg "
            "(d) 8760 g into kg and g"
        ),
    },
    3: {
        4: "Using Indian place value system, write the place value of each of the digits in the numeral 64,19,70,528.",
    },
    6: {
        5: "Add: 43268974 + 6794347 + 316554.",
        6: "Add: 16875309 + 23426793 + 4231518.",
        7: "Add: 134526729 + 243647394 + 69318453.",
        8: "Add: 245719563 + 463267478 + 71932345.",
        9: "Add: 5764239 + 43075786 + 139608945 + 96578.",
        10: "Add: 546271285 + 173827493 + 10374678 + 2992789.",
        13: "Find the sum: 474361279 + 236554385 + 53168837 + 20716314.",
    },
    8: {
        7: "Subtract: 231629547 - 192739789.",
        10: "Subtract: 302415206 - 203516438.",
        14: "Find the difference: 12034504 - 8075698.",
    },
    9: {
        5: "The sum of two numbers is 13604050. If one of the numbers is 7824361, find the other number.",
    },
    10: {
        17: "By using suitable grouping, find the product: 2 x 5726 x 500.",
    },
    17: {
        13: (
            "Separate even and odd numbers from the following. "
            "(a) 23 (b) 36 (c) 41 (d) 87 (e) 60 (f) 74 (g) 258 (h) 605."
        ),
    },
    21: {
        11: (
            "Six bells commence tolling together and toll at intervals of 2, 4, 6, 8, 10 and 12 seconds "
            "respectively. After how much time will they toll together again?"
        ),
    },
    22: {
        2: "Circle each one of the unit fractions given below: 6/1, 1/4, 3/6, 1/7, 9/1, 1/10, 5/5, 1/11.",
        3: (
            "Circle each pair of like fractions given below. "
            "(a) 3/5, 4/5 (b) 2/5, 2/7 (c) 5/9, 7/9 (d) 6/7, 6/11."
        ),
        4: (
            "Circle each pair of unlike fractions given below. "
            "(a) 1/4, 1/7 (b) 3/4, 3/5 (c) 4/9, 7/9 (d) 5/11, 7/11."
        ),
        5: (
            "Circle each one of the proper fractions given below. "
            "(a) 5/3 (b) 6/7 (c) 3/3 (d) 8/11 (e) 6/1."
        ),
        6: (
            "Circle each one of the improper fractions given below. "
            "(a) 7/8 (b) 4/4 (c) 11/6 (d) 8/1 (e) 1/1."
        ),
        10: (
            "Replace the blank in each of the following by the correct numeral. "
            "(a) 3/5 = 12/____ "
            "(b) 6/13 = ____/52 "
            "(c) 7/17 = 35/____ "
            "(d) 11/27 = 33/____"
        ),
        15: (
            "Check whether the given fractions are equivalent or not. "
            "(a) 3/4 and 15/20 "
            "(b) 4/5 and 12/20 "
            "(c) 2/3 and 10/15 "
            "(d) 5/8 and 15/24 "
            "(e) 7/11 and 28/44 "
            "(f) 3/10 and 12/50"
        ),
    },
    26: {
        12: "Find the sum of the following: 12/19 + 8/19 + 6/19.",
    },
    27: {
        13: "Find the sum: 5/9 + 7/12 + 1/3.",
        14: "Find the sum: 3/4 + 5/8 + 7/12.",
        15: "Find the sum: 3/8 + 5/16 + 13/24.",
        16: "Find the sum: 5/6 + 7/12 + 11/18.",
    },
    30: {
        24: "Subtract: 1 11/24 - 7/8.",
    },
    32: {
        6: "A drum full of rice weighs 40 1/6 kg. If the empty drum weighs 13 3/4 kg, find the weight of rice in the drum.",
    },
    33: {
        16: "Multiply: 2 10/21 x 28.",
    },
    34: {
        20: "Multiply: 2 1/4 x 1 1/5 x 3 1/3.",
    },
    37: {
        17: "Express 25 minutes as a fraction of an hour.",
    },
    38: {
        9: "The area of a rectangle is 37 4/5 sq. cm. If its length is 6 3/4 cm, find its breadth.",
    },
    45: {
        15: "Amit weighs 50.84 kg. His father is 1.5 times heavier than he is. Calculate his father's weight.",
    },
    46: {
        9: "Divide: 217.44 by 18.",
    },
    47: {
        24: "Find the quotient: 182.5 / 5000.",
    },
    49: {
        22: "Convert the following fraction into decimal: 15/32.",
    },
    67: {
        5: "Add: Rs. 372.56, Rs. 168.68 and Rs. 37.86.",
    },
    74: {
        1: (
            "In triangle LMN, name the following. "
            "(a) the vertices of the triangle "
            "(b) the sides of the triangle "
            "(c) the angles of the triangle."
        ),
        2: (
            "Classify the following triangles with respect to their sides. "
            "(a) Triangle LMN with sides 3.2 cm, 3.2 cm and 4 cm "
            "(b) Triangle PQR with sides 3.4 cm, 3.4 cm and 3.4 cm "
            "(c) Triangle XYZ with sides 4 cm, 2.8 cm and 4 cm "
            "(d) Triangle DEF with sides 5 cm, 3 cm and 4 cm "
            "(e) Triangle ABC with sides 3.6 cm, 3.6 cm and 4.2 cm "
            "(f) Triangle GHK with sides 4.8 cm, 3.2 cm and 3.5 cm."
        ),
        4: (
            "Is it possible to form a triangle by three line segments of the following lengths? "
            "(a) 8 cm, 5 cm, 15 cm "
            "(b) 6 cm, 7 cm, 13 cm "
            "(c) 7 cm, 6 cm, 11 cm "
            "(d) 6.5 cm, 16.5 cm, 5.6 cm "
            "(e) 6 cm, 6 cm, 6 cm "
            "(f) 7 cm, 8 cm, 10 cm"
        ),
        5: (
            "Fill in the blanks. "
            "(a) A scalene triangle has all sides of ......................... lengths. "
            "(b) .................... sides of an isosceles triangle are ........................ in length. "
            "(c) A triangle with all sides of the same length is called an .................... triangle. "
            "(d) The sum of the lengths of any two sides of a triangle is always ................. than the length of the third side. "
            "(e) Each angle of an equilateral triangle measures ............................ . "
            "(f) All the three angles of a scalene triangle are of .......................... measures. "
            "(g) The measures of the angles opposite to equal sides of an isosceles triangle are ................... ."
        ),
        6: (
            "State whether the given statement is true or false. "
            "(a) A triangle having only two sides equal, is called an equilateral triangle. "
            "(b) An equilateral triangle is also an isosceles triangle. "
            "(c) The sum of any two sides of a triangle is always greater than the third side. "
            "(d) Each angle of an isosceles triangle is 60 degrees. "
            "(e) Two angles of an isosceles triangle are always equal."
        ),
    },
    75: {
        3: "In triangle ABC, if angle B = 46 degrees and angle C = 54 degrees, find angle A.",
    },
    77: {
        5: (
            "The perimeter of a triangle is 16 cm and two of its sides measure 3.8 cm and 5.6 cm "
            "respectively. Find the third side."
        ),
    },
}

for exercise_number, overrides in ADDITIONAL_MANUAL_SOURCE_OVERRIDES.items():
    MANUAL_SOURCE_OVERRIDES.setdefault(exercise_number, {}).update(overrides)

MANUAL_ANSWER_OVERRIDES: dict[int, dict[int, object]] = {
    20: {
        2: {
            "a": ["150"],
            "b": ["96"],
            "c": ["210"],
            "d": ["168"],
            "e": ["300"],
            "f": ["576"],
            "g": ["540"],
            "h": ["2040"],
            "i": ["1728"],
        },
    },
    35: {
        20: ["43 4/5", "43 4 5", "43.8"],
    },
    44: {
        29: ["62.72", "both products are equal to 62.72"],
        30: ["298.1715", "both products are equal to 298.1715"],
        31: ["40.02", "both products are equal to 40.02"],
    },
    56: {
        9: ["1 kg 985 g", "1kg 985g", "1985 g"],
        10: ["23 kg 670 g", "23kg 670g", "23670 g"],
        11: [
            "nisha by 3 kg 585 g",
            "nisha weighs more by 3 kg 585 g",
            "3 kg 585 g",
        ],
        12: ["34 kg 575 g", "34kg 575g", "34575 g"],
        13: ["205 kg 200 g", "205kg 200g", "205200 g"],
        14: ["8 kg 750 g", "8kg 750g", "8750 g"],
    },
    57: {
        1: {
            "a": ["8000 mL", "8000ml", "8000"],
            "b": ["3360 mL", "3360ml", "3360"],
            "c": ["6250 L", "6250l", "6250"],
            "d": ["56 L", "56l", "56"],
        },
        2: {
            "a": ["65345 L", "65345l", "65345"],
            "b": ["5035 L", "5035l", "5035"],
            "c": ["36005 L", "36005l", "36005"],
        },
        3: {
            "a": ["8375 mL", "8375ml", "8375"],
            "b": ["37065 mL", "37065ml", "37065"],
            "c": ["15006 mL", "15006ml", "15006"],
        },
    },
    79: {
        10: [
            "c",
            "option c",
            "4 cm long, 4 cm wide, 4 cm high",
            "4 cm long 4 cm wide 4 cm high",
        ],
        11: ["125000", "125000 cu cm", "125000 cubic cm"],
    },
    80: {
        4: {
            "a": ["400", "400 students"],
            "b": ["100", "100 students"],
            "c": ["500", "500 students"],
            "d": ["1100", "1100 students"],
            "e": ["on bicycle", "bicycle"],
        },
    },
}

ADDITIONAL_MANUAL_ANSWER_OVERRIDES: dict[int, dict[int, object]] = {
    74: {
        2: {
            "a": ["Isosceles"],
            "b": ["Equilateral"],
            "c": ["Isosceles"],
            "d": ["Scalene"],
            "e": ["Isosceles"],
            "f": ["Scalene"],
        },
    },
}

for exercise_number, overrides in ADDITIONAL_MANUAL_ANSWER_OVERRIDES.items():
    MANUAL_ANSWER_OVERRIDES.setdefault(exercise_number, {}).update(overrides)

MANUAL_EXERCISE_QUESTION_LIMITS: dict[int, int] = {
    57: 3,
}


def loc(english: str, hindi: str | None = None) -> dict[str, str]:
    return {
        "english": english,
        "hindi": english if hindi is None else hindi,
    }


def slugify(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", value.lower()).strip("_")


def strip_leading_page_number(text: str) -> str:
    lines = text.splitlines()
    if lines and lines[0].strip().isdigit():
        return "\n".join(lines[1:])
    return text


def clean_text(text: str, *, strip_page_numbers: bool = False) -> str:
    replacements = {
        "\r": "\n",
        "\u00a0": " ",
        "\u2002": " ",
        "\u2003": " ",
        "\u2009": " ",
        "\u202f": " ",
        "\u200b": " ",
        "\ufeff": " ",
        "\xad": "",
        "\x07": " ",
    }
    for old, new in replacements.items():
        text = text.replace(old, new)
    text = re.sub(r"(?<!\n)[\t ]{2,}([1-9]\d?\.\s)", r"\n\1", text)
    text = re.sub(r"\bN\s*C\s*i(?:t)?\s*M\s*(?:th|h)\s*(?:ti|i)\s*5\b", " ", text, flags=re.IGNORECASE)
    text = re.sub(r"(?:\b[A-Za-z]\b\s*){4,}\b\d+\b", " ", text)
    return text


def compact_text(text: str) -> str:
    return re.sub(r"\s+", " ", clean_text(text)).strip(" .;:-")


FRACTION_STYLE_EXERCISES = {1, *range(22, 39), 49}
MIXED_PROMPT_EXERCISES = {28, 31, 32, 33, 34, 35, 36, 37, 38}
MIXED_ANSWER_HINT_EXERCISES = {22, *range(26, 39)}
NON_MIXED_ANSWER_KEYWORDS = (
    "equivalent fraction",
    "simplest form",
    "lowest terms",
    "improper fraction",
    "as a fraction",
    "unit fractions",
    "like fractions",
    "unlike fractions",
    "proper fraction",
    "proper fractions",
    "improper fractions",
    "greater fraction",
    "smaller fraction",
    "reduce",
    "circle",
    "compare",
    "express",
)


def prefers_mixed_fraction_answer(prompt_text: str, exercise_number: int | None) -> bool:
    prompt = compact_text(prompt_text).lower()
    if not prompt:
        return False
    if "mixed number" in prompt or "mixed numeral" in prompt or "mixed numerals" in prompt:
        return True
    if any(keyword in prompt for keyword in NON_MIXED_ANSWER_KEYWORDS):
        return False
    if exercise_number in MIXED_PROMPT_EXERCISES:
        return True
    if exercise_number in MIXED_ANSWER_HINT_EXERCISES:
        return any(
            keyword in prompt
            for keyword in (
                "find the sum",
                "find the difference",
                "subtract",
                "multiply",
                "divide",
                "quotient",
                "product",
                "what must be",
                "how much",
                "how far",
                "what is the weight",
                "what is the cost",
                "breadth",
                "length",
            )
        )
    return False


def split_joined_mixed_number(value: str, denominator_text: str) -> str | None:
    if len(value) < 2 or not denominator_text.isdigit():
        return None
    denominator = int(denominator_text)
    if denominator <= 0:
        return None
    for split_index in range(1, len(value)):
        whole = value[:split_index]
        numerator = value[split_index:]
        if numerator.startswith("0"):
            continue
        numerator_value = int(numerator)
        if 0 < numerator_value < denominator:
            return f"{int(whole)} {numerator_value}/{denominator}"
    return None


def format_fraction_text(
    text: str,
    exercise_number: int | None,
    *,
    prompt_text: str | None = None,
    answer_mode: bool = False,
) -> str:
    formatted = compact_text(text)
    if not formatted or exercise_number not in FRACTION_STYLE_EXERCISES:
        return formatted

    prefers_mixed = (
        prefers_mixed_fraction_answer(prompt_text or formatted, exercise_number)
        if answer_mode
        else exercise_number in MIXED_PROMPT_EXERCISES
    )

    if prefers_mixed:
        formatted = re.sub(
            r"\b(\d+)\s+(\d+)\s+(\d+)\b",
            lambda match: f"{int(match.group(1))} {int(match.group(2))}/{int(match.group(3))}",
            formatted,
        )
        formatted = re.sub(
            r"\b(\d{2,4})\s+(\d+)\b",
            lambda match: split_joined_mixed_number(match.group(1), match.group(2)) or match.group(0),
            formatted,
        )
    else:
        formatted = re.sub(
            r"\b(\d+)\s+(\d+)\s+(\d+)\b",
            lambda match: f"{int(match.group(1))}{int(match.group(2))}/{int(match.group(3))}",
            formatted,
        )

    formatted = re.sub(
        r"\b(\d+)\s+(\d+)\b(?!/)",
        lambda match: f"{int(match.group(1))}/{int(match.group(2))}",
        formatted,
    )
    formatted = re.sub(r"\s+([,.;:?])", r"\1", formatted)
    formatted = re.sub(r"([(/])\s+", r"\1", formatted)
    formatted = re.sub(r"\s+([/)])", r"\1", formatted)
    return formatted


def extract_heading(context: str) -> str | None:
    lines = [compact_text(line) for line in clean_text(context).splitlines() if compact_text(line)]
    for line in reversed(lines):
        lowered = line.lower()
        if len(line) < 3 or len(line) > 180:
            continue
        if EXAMPLE_PATTERN.search(line):
            continue
        if any(marker.lower() in lowered for marker in PROMPT_TRIM_MARKERS):
            continue
        if lowered.startswith(("hence", "solution", "method", "step ", "we know", "we observe", "read the")):
            continue
        if "=" in line:
            continue
        if looks_like_real_question(line) or line.endswith(":"):
            return line.rstrip(":")
    return None


def should_apply_heading(value: str, heading: str | None) -> bool:
    if not heading:
        return False
    lowered = value.lower()
    if heading.lower() in lowered:
        return False
    if looks_like_real_question(value):
        return False
    if lowered.startswith("part ("):
        return False
    return len(value.split()) <= 18


def starts_with_question_clause(text: str) -> bool:
    compact = compact_text(text).lower()
    return any(compact.startswith(f"{verb} ") for verb in QUESTION_VERBS)


def split_trailing_heading(value: str) -> tuple[str, str | None]:
    normalized = re.sub(r"\s+", " ", clean_text(value)).strip()
    compact = compact_text(normalized)
    if not compact:
        return compact, None

    for match in QUESTION_START_PATTERN.finditer(normalized):
        if match.start() <= 12:
            continue
        prefix_raw = normalized[:match.start()].rstrip()
        suffix_raw = normalized[match.start():].lstrip()
        prefix = compact_text(prefix_raw)
        suffix = compact_text(suffix_raw)
        if not prefix or not suffix:
            continue
        if prefix_raw.endswith((".", "?", "!")) and not re.search(r"\brepresented by\s+\.$", prefix_raw, re.IGNORECASE):
            continue
        lowered_suffix = suffix.lower()
        is_heading = any(lowered_suffix.startswith(opener) for opener in HEADING_OPENERS)
        if looks_like_real_question(prefix):
            if is_heading and not re.search(r"\d", suffix) and len(suffix.split()) <= 12:
                return prefix, suffix.rstrip(":")
            continue
        if is_heading:
            if starts_with_question_clause(compact) and (re.search(r"\d", suffix) or len(suffix.split()) > 12):
                continue
            return prefix, suffix.rstrip(":")
    if starts_with_question_clause(compact):
        return compact, None
    return compact, None


def instruction_heading(prompt_text: str) -> str | None:
    compact = compact_text(prompt_text)
    if not compact or ":" not in compact:
        return None
    heading, tail = compact.split(":", 1)
    heading = compact_text(heading)
    tail = compact_text(tail)
    if not heading or not tail:
        return None
    return heading if looks_like_real_question(heading) else None


def has_question_clause(prompt_text: str) -> bool:
    compact = compact_text(prompt_text)
    if looks_like_real_question(compact):
        return True
    if instruction_heading(compact) is not None:
        return True
    return bool(re.search(r"[.:;]\s*(?:which|what|how|find|fill|draw|measure|mark|identify|match|write|convert|express|state|tick|use)\b", compact, re.IGNORECASE))


def is_valid_source_prompt(prompt_text: str) -> bool:
    compact = compact_text(prompt_text)
    if not compact:
        return False
    return has_question_clause(compact)


def ordered_question_occurrences(text: str) -> list[tuple[int, str]]:
    text = clean_text(text, strip_page_numbers=True)
    matches = list(QUESTION_PATTERN.finditer(text))
    items: list[tuple[int, str]] = []
    current_heading: str | None = None
    previous_end = 0
    for index, match in enumerate(matches):
        next_question_number = int(matches[index + 1].group(1)) if index + 1 < len(matches) else None
        heading = extract_heading(text[previous_end:match.start()])
        if heading:
            current_heading = heading
        question_number = int(match.group(1))
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        value = text[match.end():end]
        if not value:
            previous_end = end
            continue
        value, trailing_heading = split_trailing_heading(value)
        if should_apply_heading(value, current_heading):
            value = f"{current_heading}: {value}"
        value = compact_text(value)
        if value:
            items.append((question_number, value))
        rescued_question_number = None
        if (
            trailing_heading
            and next_question_number is not None
            and next_question_number > 1
            and next_question_number - 1 < question_number
        ):
            rescued_question_number = next_question_number - 1
            items.append((rescued_question_number, compact_text(trailing_heading)))
        if trailing_heading and rescued_question_number is None:
            current_heading = trailing_heading
        previous_end = end
    return items


def split_question_occurrences(text: str) -> dict[int, list[str]]:
    items: dict[int, list[str]] = {}
    for question_number, value in ordered_question_occurrences(text):
        items.setdefault(question_number, []).append(value)
    return items


def split_question_items(text: str) -> dict[int, str]:
    return {
        question_number: values[0]
        for question_number, values in split_question_occurrences(text).items()
        if values
    }


def cut_at_markers(text: str, markers: tuple[str, ...]) -> str:
    text = clean_text(text, strip_page_numbers=True)
    cut_index = len(text)
    for marker in markers:
        match = re.search(marker, text)
        if match:
            cut_index = min(cut_index, match.start())
    return text[:cut_index]


def split_parts(text: str) -> tuple[str, dict[str, str]] | None:
    matches = list(PART_PATTERN.finditer(text))
    if len(matches) < 2:
        return None
    stem = compact_text(text[:matches[0].start()])
    parts: dict[str, str] = {}
    for index, match in enumerate(matches):
        key = match.group(1).lower()
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        part_text = compact_text(text[match.end():end])
        if part_text:
            parts[key] = part_text
    if len(parts) < 2:
        return None
    return stem, parts


def looks_like_real_question(text: str) -> bool:
    compact = compact_text(text).lower()
    return "?" in compact or compact.startswith(QUESTION_VERBS)


def is_notebook_task(text: str) -> bool:
    compact = compact_text(text).lower()
    keywords = (
        "draw",
        "construct",
        "measure",
        "use protractor",
        "use ruler",
        "use set squares",
        "with the help of compass",
        "convey the information through a pictograph",
        "draw a bar graph",
        "draw a pictograph",
        "shade the ring",
        "copy it on your notebook",
    )
    return any(keyword in compact for keyword in keywords)


def notebook_solution(prompt_text: str) -> str:
    compact = compact_text(prompt_text).lower()
    if "pictograph" in compact:
        return (
            "Use the given data, choose a neat pictograph key, draw the symbols carefully, "
            "label every category, and then type done."
        )
    if "bar graph" in compact:
        return (
            "Use the given data, choose a suitable scale, label both axes, draw equal-width bars "
            "neatly, and then type done."
        )
    if "construct" in compact or "protractor" in compact or "compass" in compact:
        return (
            "Use the required instrument carefully, mark the measurement neatly in your notebook, "
            "complete the construction, and then type done."
        )
    if "measure" in compact:
        return (
            "Use the figure from the book, measure it carefully in your notebook, write the result, "
            "and then type done."
        )
    return "Complete this textbook task neatly in your notebook and then type done."


def unitless_answer_variants(answer_text: str) -> list[str]:
    variant = answer_text
    unit_patterns = (
        r"\bsq\.?\s*(?:cm|m|km|mm)\b",
        r"\bcu\.?\s*(?:cm|m|km|mm)\b",
        r"\b(?:square|cubic)\s+(?:centimetres?|centimeters?|metres?|meters?|kilometres?|kilometers?|millimetres?|millimeters?)\b",
        r"\b(?:centimetres?|centimeters?|metres?|meters?|kilometres?|kilometers?|millimetres?|millimeters?)\b",
        r"\b(?:kilograms?|grams?|milligrams?|litres?|liters?|millilitres?|milliliters?|rupees?|paisa|paise)\b",
        r"\b(?:cm2|m2|km2|mm2|cm3|m3|km3|mm3)\b",
        r"\b(?:kg|g|mg|kl|hl|l|ml|km|hm|dam|dm|cm|mm|m)\b",
        r"\brs\.?\b",
        r"₹",
    )
    for pattern in unit_patterns:
        variant = re.sub(pattern, " ", variant, flags=re.IGNORECASE)
    compact = compact_text(variant)
    return [compact] if compact and compact != compact_text(answer_text) else []


def accepted_answers_from_source(
    answer_source: object,
    *,
    exercise_number: int | None = None,
    prompt_text: str = "",
) -> list[str]:
    answers: list[str] = []

    def add_answer(value: str) -> None:
        variants = [
            format_fraction_text(
                value,
                exercise_number,
                prompt_text=prompt_text,
                answer_mode=True,
            ),
            compact_text(value),
        ]
        for compact in variants:
            if not compact:
                continue
            if compact not in answers:
                answers.append(compact)
            for variant in unitless_answer_variants(compact):
                if variant not in answers:
                    answers.append(variant)

    if isinstance(answer_source, list):
        for value in answer_source:
            add_answer(str(value))
        return answers
    if isinstance(answer_source, str):
        parts = [part.strip() for part in re.split(r"\bor\b", answer_source, flags=re.IGNORECASE) if part.strip()]
        for part in parts or [answer_source]:
            add_answer(part)
        return answers
    return []


def selected_part_keys(answer_source: str) -> set[str]:
    return {match.group(1).lower() for match in PART_KEY_PATTERN.finditer(answer_source)}


def selection_answers_for_stem(stem: str, *, selected: bool) -> list[str] | None:
    compact = compact_text(stem).lower()
    is_selection_prompt = any(
        marker in compact
        for marker in (
            "circle",
            "tick",
            "encircle",
            "which of the following",
            "separate even and odd",
            "identify",
        )
    )
    if "meaningless" in compact:
        return ["meaningless", "yes"] if selected else ["meaningful", "no", "not meaningless"]
    divisible_match = re.search(r"divisible by (\d+)", compact)
    if divisible_match:
        divisor = divisible_match.group(1)
        return (
            ["divisible", "yes", f"divisible by {divisor}"]
            if selected
            else ["not divisible", "no", f"not divisible by {divisor}"]
        )
    if "improper fractions" in compact and is_selection_prompt:
        return ["improper fraction", "improper", "yes"] if selected else ["proper fraction", "proper", "no"]
    return None


def is_consecutive_prefix(keys: set[str]) -> bool:
    if not keys:
        return False
    ordered = sorted(keys)
    expected = [chr(code) for code in range(ord("a"), ord("a") + len(ordered))]
    return ordered == expected


def trim_prompt_candidate(
    prompt_text: str,
    chapter_number: int | None = None,
    chapters: dict[int, tuple[str, str]] | None = None,
) -> str:
    trimmed = compact_text(prompt_text)
    trim_points: list[int] = []
    for marker in PROMPT_TRIM_MARKERS:
        match = re.search(re.escape(marker), trimmed, re.IGNORECASE)
        if match and match.start() > 0:
            trim_points.append(match.start())
    for pattern in (EXAMPLE_PATTERN, ACTIVITY_PATTERN):
        match = pattern.search(trimmed)
        if match and match.start() > 0:
            trim_points.append(match.start())
    if chapter_number is not None and chapters is not None and chapter_number + 1 in chapters:
        next_title = chapters[chapter_number + 1][0]
        next_marker = re.search(rf"{re.escape(next_title)}\s+{chapter_number + 1}\b", trimmed, re.IGNORECASE)
        if next_marker and next_marker.start() > 0:
            trim_points.append(next_marker.start())
    exercise_match = re.search(r"\bExercise\s+\d+\b", trimmed, re.IGNORECASE)
    if exercise_match and exercise_match.start() > 0:
        trim_points.append(exercise_match.start())
    if trim_points:
        trimmed = trimmed[: min(trim_points)]
    trimmed = re.sub(r"\bN\s*C\s*i(?:t)?\s*M\s*(?:th|h)\s*(?:ti|i)\s*5\b", " ", trimmed, flags=re.IGNORECASE)
    return compact_text(trimmed)


def prompt_score(prompt_text: str) -> tuple[int, int]:
    compact = compact_text(prompt_text)
    lowered = compact.lower()
    score = 0
    if looks_like_real_question(compact):
        score += 80
    if "?" in compact:
        score += 20
    if re.search(r"\d", compact):
        score += 12
    if any(marker.lower() in lowered for marker in PROMPT_TRIM_MARKERS):
        score -= 120
    if EXAMPLE_PATTERN.search(compact):
        score -= 100
    if ACTIVITY_PATTERN.search(compact):
        score -= 90
    if "n c it" in lowered:
        score -= 60
    if len(compact) > 320:
        score -= 40
    if not looks_like_real_question(compact) and not re.search(r"\d", compact):
        score -= 20
    return score, -len(compact)


def choose_best_prompt(
    candidates: list[str],
    chapter_number: int | None = None,
    chapters: dict[int, tuple[str, str]] | None = None,
) -> str | None:
    best_prompt: str | None = None
    best_score: tuple[int, int] | None = None
    seen: set[str] = set()
    for candidate in candidates:
        trimmed = trim_prompt_candidate(candidate, chapter_number, chapters)
        if not trimmed or trimmed in seen:
            continue
        seen.add(trimmed)
        score = prompt_score(trimmed)
        if best_score is None or score > best_score:
            best_score = score
            best_prompt = trimmed
    return best_prompt


class ExercisePdfParser:
    def __init__(self, pdf_path: Path) -> None:
        if not pdf_path.exists():
            raise FileNotFoundError(f"RS Aggarwal PDF not found: {pdf_path}")

        document = fitz.open(pdf_path)
        self.page_texts = [strip_leading_page_number(page.get_text("text")) for page in document]
        self.source_headers: dict[int, tuple[int, int]] = {}
        for page_number in range(1, ANSWER_PAGE_START_INDEX + 1):
            page_text = self.page_texts[page_number - 1]
            match = re.search(r"Exercise\s+(\d+)", page_text, re.IGNORECASE)
            if match:
                self.source_headers[int(match.group(1))] = (page_number, match.start())

        self.answer_text = "\n".join(self.page_texts[ANSWER_PAGE_START_INDEX:])
        self.answer_headers = {
            int(match.group(1)): match.start()
            for match in re.finditer(r"Exercise\s+(\d+)", self.answer_text, re.IGNORECASE)
        }

    def source_questions(
        self,
        exercise_number: int,
        chapter_number: int | None = None,
        chapters: dict[int, tuple[str, str]] | None = None,
    ) -> dict[int, str]:
        page_number, start_index = self.source_headers[exercise_number]
        next_exercise = next((number for number in sorted(self.source_headers) if number > exercise_number), None)
        answer_items = self.answer_questions(exercise_number)

        block_parts = [self.page_texts[page_number - 1]]
        if next_exercise is not None:
            next_page_number, next_start_index = self.source_headers[next_exercise]
            for current_page in range(page_number + 1, next_page_number):
                block_parts.append(self.page_texts[current_page - 1])
            if next_page_number > page_number:
                block_parts.append(self.page_texts[next_page_number - 1][:next_start_index])

        question_items: dict[int, str] = {}
        for question_number, candidate in ordered_question_occurrences(cut_at_markers("\n".join(block_parts), SOURCE_CUT_MARKERS)):
            if question_number in question_items:
                continue
            trimmed = trim_prompt_candidate(candidate, chapter_number, chapters)
            if is_valid_source_prompt(trimmed):
                question_items[question_number] = trimmed

        fill_limit = max(answer_items) if answer_items else (max(question_items) if question_items else 0)
        if answer_items and page_number > 1:
            for question_number, candidate in ordered_question_occurrences(self.page_texts[page_number - 2]):
                if question_number in question_items or question_number > fill_limit:
                    continue
                trimmed = trim_prompt_candidate(candidate, chapter_number, chapters)
                if is_valid_source_prompt(trimmed):
                    question_items[question_number] = trimmed

        if answer_items:
            max_answer_question = max(answer_items)
            question_items = {
                question_number: prompt_text
                for question_number, prompt_text in question_items.items()
                if question_number <= max_answer_question
                or is_notebook_task(prompt_text)
            }

        overrides = MANUAL_SOURCE_OVERRIDES.get(exercise_number, {})
        for question_number, prompt_text in overrides.items():
            question_items[question_number] = prompt_text

        return {question_number: question_items[question_number] for question_number in sorted(question_items)}

    def answer_questions(self, exercise_number: int) -> dict[int, str]:
        if exercise_number not in self.answer_headers:
            return {}

        exercise_numbers = sorted(self.answer_headers)
        current_index = exercise_numbers.index(exercise_number)
        start_index = self.answer_headers[exercise_number]
        end_index = (
            self.answer_headers[exercise_numbers[current_index + 1]]
            if current_index + 1 < len(exercise_numbers)
            else len(self.answer_text)
        )
        return split_question_items(cut_at_markers(self.answer_text[start_index:end_index], ANSWER_CUT_MARKERS))


def question_label(chapter_number: int, exercise_number: int, question_number: int, part_key: str | None) -> str:
    suffix = f"({part_key})" if part_key else ""
    return f"{chapter_number}.{exercise_number}.{question_number}{suffix}"


# ─────────────────────────────────────────────────────────────
# ARITHMETIC SOLVER  –  computes correct answers from prompt
# ─────────────────────────────────────────────────────────────


def _lcm2(a: int, b: int) -> int:
    return a * b // _gcd(a, b)


def _lcm_list(nums: list[int]) -> int:
    return _reduce(_lcm2, nums, 1)


def _frac_display(f: _Frac, *, prefer_mixed: bool = True) -> str:
    """Return a human-readable fraction string: '1 1/4', '5/7', '3', '-1/2'."""
    if f == 0:
        return "0"
    sign = "-" if f < 0 else ""
    fa = abs(f)
    if fa.denominator == 1:
        return f"{sign}{fa.numerator}"
    if prefer_mixed and fa.numerator > fa.denominator:
        w = fa.numerator // fa.denominator
        r = fa.numerator % fa.denominator
        return f"{sign}{w} {r}/{fa.denominator}"
    return f"{sign}{fa.numerator}/{fa.denominator}"


def _parse_frac(s: str) -> _Frac | None:
    """Parse '1 2/3', '5/7', or '3' into a Fraction. Returns None on failure."""
    s = s.strip()
    m = re.match(r"^(\d+)\s+(\d+)/(\d+)$", s)
    if m:
        w, n, d = int(m.group(1)), int(m.group(2)), int(m.group(3))
        return None if d == 0 else _Frac(w * d + n, d)
    m = re.match(r"^(\d+)/(\d+)$", s)
    if m:
        n, d = int(m.group(1)), int(m.group(2))
        return None if d == 0 else _Frac(n, d)
    m = re.match(r"^(\d+)$", s)
    if m:
        return _Frac(int(m.group(1)))
    return None


def _tokenize_expr(expr: str) -> list | None:
    """
    Tokenize a fraction arithmetic expression.
    Returns alternating list [Frac, op, Frac, op, Frac, …] or None on failure.
    Operators: + - × (→ *) ÷ (→ ^)
    """
    # Normalise operator glyphs; ^ = internal division placeholder
    text = (
        expr
        .replace("×", "*")
        .replace("÷", "^")
        .replace("\u2013", "-")  # EN DASH used as minus in some prompts
        .replace("\u2212", "-")  # MINUS SIGN
    )
    tokens: list = []
    i = 0
    n = len(text)
    while i < n:
        if text[i].isspace():
            i += 1
            continue
        # Mixed number: digit+ SPACE digit+/digit+
        m = re.match(r"(\d+)\s+(\d+)/(\d+)", text[i:])
        if m:
            w, num, den = int(m.group(1)), int(m.group(2)), int(m.group(3))
            if den == 0:
                return None
            tokens.append(_Frac(w * den + num, den))
            i += m.end()
            continue
        # Simple fraction: digit+/digit+
        m = re.match(r"(\d+)/(\d+)", text[i:])
        if m:
            num, den = int(m.group(1)), int(m.group(2))
            if den == 0:
                return None
            tokens.append(_Frac(num, den))
            i += m.end()
            continue
        # Decimal: digit+.digit+
        m = re.match(r"(\d+\.\d+)", text[i:])
        if m:
            try:
                tokens.append(_Frac(m.group(1)))
            except Exception:
                return None
            i += m.end()
            continue
        # Integer
        m = re.match(r"(\d+)", text[i:])
        if m:
            tokens.append(_Frac(int(m.group(1))))
            i += m.end()
            continue
        # Operator
        if text[i] in "+-*^":
            tokens.append(text[i])
            i += 1
            continue
        return None  # Unknown character
    return tokens


def _eval_bodmas(tokens: list) -> _Frac | None:
    """Evaluate token list with BODMAS (* ^ before + -)."""
    if not tokens or len(tokens) % 2 == 0:
        return None
    operands = tokens[0::2]
    ops = tokens[1::2]
    if not all(isinstance(v, _Frac) for v in operands):
        return None
    if not all(isinstance(op, str) and op in "+-*^" for op in ops):
        return None
    # First pass: multiply / divide
    vals = list(operands)
    ops2 = list(ops)
    i = 0
    while i < len(ops2):
        if ops2[i] in ("*", "^"):
            a, b = vals[i], vals[i + 1]
            vals[i] = a * b if ops2[i] == "*" else (a / b if b != 0 else None)
            if vals[i] is None:
                return None
            vals.pop(i + 1)
            ops2.pop(i)
        else:
            i += 1
    # Second pass: add / subtract
    result = vals[0]
    for idx, op in enumerate(ops2):
        result = result + vals[idx + 1] if op == "+" else result - vals[idx + 1]
    return result


def _add_sub_steps(operands: list, ops: list, result: _Frac, prefer_mixed: bool) -> list[str]:
    """Step-by-step solution for addition / subtraction of fractions."""
    steps: list[str] = []
    denoms = [int(f.denominator) for f in operands]
    display = lambda f: _frac_display(f, prefer_mixed=prefer_mixed)  # noqa: E731

    if len(set(denoms)) == 1:
        d = denoms[0]
        nums = [int(f.numerator) for f in operands]
        steps.append(f"Same denominator: {d}")
        if all(op == "+" for op in ops):
            expr = " + ".join(str(n) for n in nums)
            total = sum(nums)
            steps.append(f"Add numerators: {expr} = {total}")
        else:
            parts = [str(nums[0])]
            total = nums[0]
            for op, n in zip(ops, nums[1:]):
                sym = "+" if op == "+" else "-"
                parts.append(f"{sym} {n}")
                total = total + n if op == "+" else total - n
            steps.append(f"Numerators: {' '.join(parts)} = {total}")
        raw = _Frac(
            sum(int(f.numerator) * (1 if i == 0 or ops[i - 1] == "+" else -1)
                for i, f in enumerate(operands)),
            d,
        )
        if raw != result:
            steps.append(f"Simplify: {_frac_display(raw, prefer_mixed=False)} = {display(result)}")
        else:
            steps.append(f"Result: {display(result)}")
    else:
        lcd = _lcm_list(denoms)
        steps.append(f"LCM of {', '.join(str(d) for d in denoms)} = {lcd}")
        converted: list[int] = []
        for f in operands:
            mult = lcd // f.denominator
            cn = f.numerator * mult
            converted.append(cn)
            if mult != 1:
                steps.append(f"Convert {display(f)} → {cn}/{lcd}")
        # Combine numerators
        parts = [str(converted[0])]
        total = converted[0]
        for op, cn in zip(ops, converted[1:]):
            sym = "+" if op == "+" else "-"
            parts.append(f"{sym} {cn}")
            total = total + cn if op == "+" else total - cn
        steps.append(f"Numerators: {' '.join(parts)} = {total}")
        raw = _Frac(total, lcd)
        if raw != result:
            steps.append(f"Simplify: {_frac_display(raw, prefer_mixed=False)} = {display(result)}")
        else:
            steps.append(f"Result: {display(result)}")
    steps.append(f"Answer: {display(result)}")
    return steps


def _mul_div_steps(operands: list, ops: list, result: _Frac, prefer_mixed: bool) -> list[str]:
    """Step-by-step solution for multiplication / division of fractions."""
    steps: list[str] = []
    display = lambda f: _frac_display(f, prefer_mixed=prefer_mixed)  # noqa: E731

    if all(op == "*" for op in ops):
        nums = [int(f.numerator) for f in operands]
        dens = [int(f.denominator) for f in operands]
        np_ = _reduce(lambda a, b: a * b, nums)
        dp_ = _reduce(lambda a, b: a * b, dens)
        steps.append(f"Multiply numerators: {' × '.join(str(n) for n in nums)} = {np_}")
        steps.append(f"Multiply denominators: {' × '.join(str(d) for d in dens)} = {dp_}")
        raw = _Frac(np_, dp_)
        if raw != result:
            steps.append(f"Simplify: {np_}/{dp_} = {display(result)}")
    elif len(ops) == 1 and ops[0] == "^":
        a, b = operands[0], operands[1]
        steps.append(f"Reciprocal of {display(b)}: {b.denominator}/{b.numerator}")
        np_ = a.numerator * b.denominator
        dp_ = a.denominator * b.numerator
        raw = _Frac(np_, dp_)
        steps.append(f"Multiply: {display(a)} × {b.denominator}/{b.numerator} = {np_}/{dp_}")
        if raw != result:
            steps.append(f"Simplify: {display(result)}")
    else:
        # Mixed × and ^
        running = operands[0]
        for op, f in zip(ops, operands[1:]):
            sym = "×" if op == "*" else "÷"
            prev = running
            running = running * f if op == "*" else running / f
            steps.append(f"{display(prev)} {sym} {display(f)} = {display(running)}")
    steps.append(f"Answer: {display(result)}")
    return steps


def _bodmas_steps(tokens: list, result: _Frac, prefer_mixed: bool) -> list[str]:
    """Step-by-step BODMAS solution for mixed +/-/×/÷."""
    steps = ["Apply BODMAS: × and ÷ before + and −."]
    display = lambda f: _frac_display(f, prefer_mixed=prefer_mixed)  # noqa: E731
    operands = tokens[0::2]
    ops = tokens[1::2]
    vals = list(operands)
    ops2 = list(ops)
    i = 0
    while i < len(ops2):
        if ops2[i] in ("*", "^"):
            sym = "×" if ops2[i] == "*" else "÷"
            product = vals[i] * vals[i + 1] if ops2[i] == "*" else vals[i] / vals[i + 1]
            steps.append(f"{display(vals[i])} {sym} {display(vals[i + 1])} = {display(product)}")
            vals[i] = product
            vals.pop(i + 1)
            ops2.pop(i)
        else:
            i += 1
    if len(vals) > 1:
        steps.extend(_add_sub_steps(vals, ops2, result, prefer_mixed))
    else:
        steps.append(f"Answer: {display(result)}")
    return steps


def _frac_accepted_list(f: _Frac, prefer_mixed: bool) -> list[str]:
    """Return primary + alternate string forms for a Fraction result."""
    forms = []
    primary = _frac_display(f, prefer_mixed=prefer_mixed)
    if primary not in forms:
        forms.append(primary)
    # Also add the other display form
    alt = _frac_display(f, prefer_mixed=not prefer_mixed)
    if alt and alt not in forms:
        forms.append(alt)
    # Add improper if mixed was primary
    if f.denominator != 1 and f > 1:
        imp = f"{f.numerator}/{f.denominator}"
        if imp not in forms:
            forms.append(imp)
    return forms


def try_solve_arithmetic(
    prompt_text: str,
    exercise_number: int | None = None,
) -> tuple[str, list[str]] | None:
    """
    Try to compute the answer for a pure arithmetic prompt.
    Returns (answer_str, steps) or None if the prompt cannot be solved.
    Only attempted for FRACTION_STYLE_EXERCISES.
    """
    if exercise_number is not None and exercise_number not in FRACTION_STYLE_EXERCISES:
        return None

    # Extract expression after the last colon
    colon_idx = prompt_text.rfind(":")
    if colon_idx < 0:
        return None
    expr = prompt_text[colon_idx + 1:].strip().rstrip(".?")
    if not expr:
        return None

    # Remove "Part (x):" prefix if still present
    expr = re.sub(r"^Part\s*\([a-z]\):\s*", "", expr, flags=re.IGNORECASE).strip()

    try:
        tokens = _tokenize_expr(expr)
        if tokens is None or len(tokens) == 0:
            return None
        # Must be alternating; single number with no operator = skip
        if len(tokens) == 1:
            return None  # e.g., Roman numeral prompt
        if len(tokens) % 2 == 0:
            return None

        operands = tokens[0::2]
        ops = tokens[1::2]
        if not all(isinstance(v, _Frac) for v in operands):
            return None
        valid_ops = set("+-*^")
        if not all(isinstance(op, str) and op in valid_ops for op in ops):
            return None

        # Safety: skip if any operand looks like a concatenated mixed number
        # (2-digit numerator where trailing digit < small denominator — ambiguous).
        for frac in operands:
            num_str = str(frac.numerator)
            if (
                frac.numerator > 9
                and frac.denominator <= 12
                and len(num_str) == 2
            ):
                trailing = int(num_str[1])
                if 0 < trailing < frac.denominator:
                    return None  # Could be "6 2/3" compacted to "62/3" — skip

        result = _eval_bodmas(tokens)
        if result is None:
            return None

        op_set = set(ops)

        prefer_mixed = prefers_mixed_fraction_answer(prompt_text, exercise_number)
        # For add/subtract in fraction-style exercises, prefer mixed when result > 1
        if (
            not prefer_mixed
            and exercise_number in MIXED_ANSWER_HINT_EXERCISES
            and op_set <= {"+", "-"}
        ):
            lower = compact_text(prompt_text).lower()
            if any(k in lower for k in ("add", "sum", "subtract", "difference")):
                prefer_mixed = True
        answer_str = _frac_display(result, prefer_mixed=prefer_mixed)

        if op_set <= {"+", "-"}:
            steps = _add_sub_steps(operands, ops, result, prefer_mixed)
        elif op_set <= {"*"}:
            steps = _mul_div_steps(operands, ops, result, prefer_mixed)
        elif op_set == {"^"} and len(ops) == 1:
            steps = _mul_div_steps(operands, ops, result, prefer_mixed)
        elif op_set <= {"*", "^"} and "+" not in op_set and "-" not in op_set:
            steps = _mul_div_steps(operands, ops, result, prefer_mixed)
        else:
            steps = _bodmas_steps(tokens, result, prefer_mixed)

        return answer_str, steps

    except Exception:
        return None


def make_notebook_answer(prompt_text: str) -> dict[str, object]:
    solution_text = notebook_solution(prompt_text)
    return {
        "acceptedAnswers": NOTEBOOK_ACCEPTED_ANSWERS,
        "solutionText": "done",
        "wrongReason": "This is a notebook drawing or construction task. Finish it carefully and then type done.",
        "supportExample": solution_text,
        "reteachTitle": "How to complete this notebook task",
        "reteachParagraphs": [solution_text],
        "exampleText": solution_text,
        "quizPromptSuffix": "Then type done.",
    }


def display_answer_from_source(
    answer_source: object,
    *,
    exercise_number: int | None = None,
    prompt_text: str = "",
) -> str:
    if isinstance(answer_source, list) and answer_source:
        return format_fraction_text(
            str(answer_source[0]),
            exercise_number,
            prompt_text=prompt_text,
            answer_mode=True,
        )
    if isinstance(answer_source, str):
        parts = [part.strip() for part in re.split(r"\bor\b", answer_source, flags=re.IGNORECASE) if part.strip()]
        first_part = parts[0] if parts else answer_source
        return format_fraction_text(
            first_part,
            exercise_number,
            prompt_text=prompt_text,
            answer_mode=True,
        )
    return ""


def make_text_answer(
    answer_source: object,
    *,
    exercise_number: int | None = None,
    prompt_text: str = "",
) -> dict[str, object]:
    # ── Try to compute the answer arithmetically from the prompt ──────────
    computed = try_solve_arithmetic(prompt_text, exercise_number) if prompt_text else None

    if computed is not None:
        computed_answer_str, steps = computed

        # Build accepted answers: computed primary forms + unit-free variants
        accepted_answers: list[str] = []

        def _add_accepted(val: str) -> None:
            v = compact_text(val)
            if v and v not in accepted_answers:
                accepted_answers.append(v)
            for uv in unitless_answer_variants(v):
                if uv and uv not in accepted_answers:
                    accepted_answers.append(uv)

        # Formatted primary (exercise-aware fraction style)
        fmt_primary = format_fraction_text(
            computed_answer_str,
            exercise_number,
            prompt_text=prompt_text,
            answer_mode=True,
        ) or computed_answer_str
        _add_accepted(fmt_primary)
        _add_accepted(computed_answer_str)

        # Also absorb any PDF-extracted answer as additional fallback
        if answer_source is not None:
            for a in accepted_answers_from_source(
                answer_source,
                exercise_number=exercise_number,
                prompt_text=prompt_text,
            ):
                _add_accepted(a)

        solution_text = fmt_primary
        reteach_paragraphs = steps  # actual worked solution steps

        return {
            "acceptedAnswers": accepted_answers,
            "solutionText": solution_text,
            "wrongReason": "Solve step by step and check each line. Units do not need to be typed.",
            "supportExample": f"See the step-by-step solution below.",
            "reteachTitle": "See solution",
            "reteachParagraphs": reteach_paragraphs,
            "exampleText": f"Answer: {solution_text}",
            "quizPromptSuffix": "",
        }

    # ── Fall back to PDF-extracted answer ────────────────────────────────
    accepted_answers = accepted_answers_from_source(
        answer_source,
        exercise_number=exercise_number,
        prompt_text=prompt_text,
    )
    solution_text = display_answer_from_source(
        answer_source,
        exercise_number=exercise_number,
        prompt_text=prompt_text,
    ) or compact_text(accepted_answers[0] if accepted_answers else "")
    return {
        "acceptedAnswers": accepted_answers,
        "solutionText": solution_text,
        "wrongReason": "Check each step of your working carefully. Units do not need to be typed.",
        "supportExample": "Work through the steps carefully and compare your answer at the end.",
        "reteachTitle": "See solution",
        "reteachParagraphs": [
            "Read the question again carefully.",
            "Write down the given values and identify what you need to find.",
            "Apply the correct method step by step.",
            f"Check your working and verify: the answer is {solution_text}.",
        ],
        "exampleText": f"Answer: {solution_text}",
        "quizPromptSuffix": "",
    }


def compose_part_prompt(stem: str, part_key: str, part_text: str) -> str:
    if stem:
        return f"{stem} Part ({part_key}): {part_text}"
    return f"Part ({part_key}): {part_text}"


def build_topic(
    chapters: dict[int, tuple[str, str]],
    chapter_number: int,
    exercise_number: int,
    question_number: int,
    prompt_text: str,
    answer_source: object | None,
    part_key: str | None = None,
) -> dict[str, object]:
    chapter_title_en, chapter_title_hi = chapters[chapter_number]
    label = question_label(chapter_number, exercise_number, question_number, part_key)
    display_prompt_text = format_fraction_text(prompt_text, exercise_number)
    answer_payload = (
        make_text_answer(
            answer_source,
            exercise_number=exercise_number,
            prompt_text=display_prompt_text,
        )
        if answer_source is not None
        else make_notebook_answer(display_prompt_text)
    )
    quiz_prompt = compact_text(display_prompt_text)
    prompt_suffix = answer_payload["quizPromptSuffix"]
    normalized_prompt_suffix = compact_text(str(prompt_suffix))
    if normalized_prompt_suffix and not quiz_prompt.lower().endswith(normalized_prompt_suffix.lower()):
        quiz_prompt = f"{quiz_prompt} {normalized_prompt_suffix}"

    topic_id = f"rs_ex_ch{chapter_number:02d}_ex{exercise_number:02d}_q{question_number:02d}"
    if part_key is not None:
        topic_id += f"_{part_key}"

    explanation_title = f"Question {label}"
    lesson_title = f"Exercise {exercise_number}"
    subtopic_title = f"Question {label}"

    return {
        "id": topic_id,
        "sourceLessonId": f"rs_exercise_ch{chapter_number:02d}_ex{exercise_number:02d}",
        "chapterNumber": chapter_number,
        "chapterTitle": loc(chapter_title_en, chapter_title_hi),
        "lessonTitle": loc(lesson_title),
        "subtopicTitle": loc(subtopic_title),
        "knowPrompt": loc(f"Can you solve {subtopic_title}?"),
        "explanationTitle": loc(explanation_title),
        "explanationParagraphs": [
            loc(
                f"This is Exercise {exercise_number}, Question {label} from Chapter {chapter_number}."
            ),
            loc(f"Book question: {display_prompt_text}"),
        ],
        "examples": [loc(str(answer_payload["exampleText"]))],
        "visuals": [],
        "questions": [
            {
                "id": slugify(f"{topic_id}_q1"),
                "prompt": loc(quiz_prompt),
                "type": "TEXT_INPUT",
                "options": [],
                "correctOptionIndex": None,
                "acceptedAnswers": answer_payload["acceptedAnswers"],
                "hint": loc("Check the textbook rule, the numbers, and the final form carefully."),
                "wrongReason": loc(str(answer_payload["wrongReason"])),
                "supportExample": loc(str(answer_payload["supportExample"])),
                "mistakeType": "GENERAL",
                "reteachTitle": loc(str(answer_payload["reteachTitle"])),
                "reteachParagraphs": [loc(paragraph) for paragraph in answer_payload["reteachParagraphs"]],
            }
        ],
        "tags": [
            loc("Exercise Path"),
            loc(lesson_title),
            loc(subtopic_title),
        ],
        "mistakeFocus": "GENERAL",
    }


def build_topics_for_question(
    chapters: dict[int, tuple[str, str]],
    chapter_number: int,
    exercise_number: int,
    question_number: int,
    prompt_text: str,
    answer_source: object | None,
) -> list[dict[str, object]]:
    manual_answer_parts = answer_source if isinstance(answer_source, dict) else None
    prompt_parts = split_parts(prompt_text)
    parsed_answer_parts = None
    if isinstance(answer_source, str):
        parsed_answer_parts = split_parts(answer_source)

    if prompt_parts is not None:
        stem, prompt_part_map = prompt_parts
        if isinstance(answer_source, str):
            selection_keys = selected_part_keys(answer_source)
            if selection_keys:
                selection_template = selection_answers_for_stem(stem, selected=True)
                if selection_template is not None:
                    topics = []
                    for part_key, part_text in prompt_part_map.items():
                        topics.append(
                            build_topic(
                                chapters=chapters,
                                chapter_number=chapter_number,
                                exercise_number=exercise_number,
                                question_number=question_number,
                                prompt_text=compose_part_prompt(stem, part_key, part_text),
                                answer_source=selection_answers_for_stem(
                                    stem,
                                    selected=part_key in selection_keys,
                                ),
                                part_key=part_key,
                            )
                        )
                    return topics

        answer_part_map = manual_answer_parts.copy() if isinstance(manual_answer_parts, dict) else None
        if parsed_answer_parts is not None:
            _, parsed_part_map = parsed_answer_parts
            answer_part_map = {**parsed_part_map, **(answer_part_map or {})}
            if len(prompt_part_map) > len(answer_part_map) and is_consecutive_prefix(set(answer_part_map)):
                prompt_part_map = {
                    part_key: part_text
                    for part_key, part_text in prompt_part_map.items()
                    if part_key in answer_part_map
                }

        if answer_part_map is not None:
            topics = []
            for part_key, part_text in prompt_part_map.items():
                topics.append(
                    build_topic(
                        chapters=chapters,
                        chapter_number=chapter_number,
                        exercise_number=exercise_number,
                        question_number=question_number,
                        prompt_text=compose_part_prompt(stem, part_key, part_text),
                        answer_source=answer_part_map.get(part_key),
                        part_key=part_key,
                    )
                )
            return topics

        if answer_source is None and is_notebook_task(prompt_text):
            topics = []
            for part_key, part_text in prompt_part_map.items():
                topics.append(
                    build_topic(
                        chapters=chapters,
                        chapter_number=chapter_number,
                        exercise_number=exercise_number,
                        question_number=question_number,
                        prompt_text=compose_part_prompt(stem, part_key, part_text),
                        answer_source=None,
                        part_key=part_key,
                    )
                )
            return topics

    return [
        build_topic(
            chapters=chapters,
            chapter_number=chapter_number,
            exercise_number=exercise_number,
            question_number=question_number,
            prompt_text=prompt_text,
            answer_source=answer_source,
        )
    ]


def build_exercise_topics(chapters: dict[int, tuple[str, str]]) -> list[dict[str, object]]:
    parser = ExercisePdfParser(PDF_PATH)
    topics: list[dict[str, object]] = []

    for exercise_number in sorted(EXERCISE_TO_CHAPTER):
        chapter_number = EXERCISE_TO_CHAPTER[exercise_number]
        source_questions = parser.source_questions(
            exercise_number,
            chapter_number=chapter_number,
            chapters=chapters,
        )
        answer_questions = parser.answer_questions(exercise_number)
        manual_answers = MANUAL_ANSWER_OVERRIDES.get(exercise_number, {})
        question_limit = MANUAL_EXERCISE_QUESTION_LIMITS.get(exercise_number)
        if question_limit is not None:
            source_questions = {
                question_number: prompt_text
                for question_number, prompt_text in source_questions.items()
                if question_number <= question_limit
            }
            answer_questions = {
                question_number: answer_source
                for question_number, answer_source in answer_questions.items()
                if question_number <= question_limit
            }
            manual_answers = {
                question_number: answer_source
                for question_number, answer_source in manual_answers.items()
                if question_number <= question_limit
            }

        all_question_numbers = sorted(set(source_questions) | set(answer_questions) | set(manual_answers))
        for question_number in all_question_numbers:
            prompt_text = source_questions.get(
                question_number,
                f"Use the textbook prompt for Exercise {exercise_number}, Question {question_number}.",
            )
            answer_source = manual_answers.get(question_number, answer_questions.get(question_number))
            topics.extend(
                build_topics_for_question(
                    chapters=chapters,
                    chapter_number=chapter_number,
                    exercise_number=exercise_number,
                    question_number=question_number,
                    prompt_text=prompt_text,
                    answer_source=answer_source,
                )
            )

    return topics
