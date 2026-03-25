from __future__ import annotations

import re
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
    "identify",
    "look",
    "mark",
    "match",
    "measure",
    "multiply",
    "name",
    "observe",
    "read",
    "rewrite",
    "round",
    "state",
    "subtract",
    "tick",
    "use",
    "verify",
    "what",
    "which",
    "write",
)

MANUAL_SOURCE_OVERRIDES: dict[int, dict[int, str]] = {
    4: {
        4: (
            "Encircle the largest number in each of the following. "
            "(a) 31650829, 307482134, 4536794, 41035106, 238590746 "
            "(b) 102234102, 93645753, 27810591, 102240003, 93646800 "
            "(c) 9037848, 12345716, 101010706, 91537964, 100718967 "
            "(d) 9000009, 90000001, 9935469, 87590909, 88888888"
        ),
    },
    17: {
        15: "Write all odd numbers between (a) 64 and 80 (b) 624 and 640.",
    },
    27: {
        17: "Add the fractions: 1/2 + 1/3 + 1/4 + 1/6.",
        18: "Add the fractions: 1 + 2/3 + 3/4 + 5/8.",
    },
    28: {
        10: "Find the sum: 3 2/5 + 1 1/10 + 4/15.",
    },
    31: {
        10: "Find the result: 10 - 3 1/5 - 5 3/10.",
    },
    34: {
        18: "Multiply: 0.0325 by 0.09.",
    },
    35: {
        20: "The cost of one pencil is 3 13/20 rupees. What is the cost of 12 such pencils?",
    },
    60: {
        4: "Find the average of first nine counting numbers.",
    },
    70: {
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
        10: ["23 kg 670 g", "23kg 670g", "23670 g"],
        11: [
            "nisha by 3 kg 585 g",
            "nisha weighs more by 3 kg 585 g",
            "3 kg 585 g",
        ],
        12: ["34 kg 575 g", "34kg 575g", "34575 g"],
        13: ["205 kg 200 g", "205kg 200g", "205200 g"],
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
    text = re.sub(r"(?:\b[A-Za-z]\b\s*){4,}\b\d+\b", " ", text)
    return text


def compact_text(text: str) -> str:
    return re.sub(r"\s+", " ", clean_text(text)).strip(" .;:-")


def split_question_items(text: str) -> dict[int, str]:
    text = clean_text(text, strip_page_numbers=True)
    matches = list(QUESTION_PATTERN.finditer(text))
    items: dict[int, str] = {}
    for index, match in enumerate(matches):
        question_number = int(match.group(1))
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        value = compact_text(text[match.end():end])
        if value:
            items.setdefault(question_number, value)
    return items


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


def accepted_answers_from_source(answer_source: object) -> list[str]:
    if isinstance(answer_source, list):
        answers: list[str] = []
        for value in answer_source:
            compact = compact_text(str(value))
            if compact:
                answers.append(compact)
        return answers
    if isinstance(answer_source, str):
        answer = compact_text(answer_source)
        return [answer] if answer else []
    return []


def selected_part_keys(answer_source: str) -> set[str]:
    return {match.group(1).lower() for match in PART_KEY_PATTERN.finditer(answer_source)}


def selection_answers_for_stem(stem: str, *, selected: bool) -> list[str] | None:
    compact = compact_text(stem).lower()
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
    if "improper fractions" in compact:
        return ["improper fraction", "improper", "yes"] if selected else ["proper fraction", "proper", "no"]
    return None


def is_consecutive_prefix(keys: set[str]) -> bool:
    if not keys:
        return False
    ordered = sorted(keys)
    expected = [chr(code) for code in range(ord("a"), ord("a") + len(ordered))]
    return ordered == expected


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

    def source_questions(self, exercise_number: int) -> dict[int, str]:
        page_number, start_index = self.source_headers[exercise_number]
        next_exercise = next((number for number in sorted(self.source_headers) if number > exercise_number), None)

        block_parts = [self.page_texts[page_number - 1][start_index:]]
        if next_exercise is not None:
            next_page_number, next_start_index = self.source_headers[next_exercise]
            for current_page in range(page_number + 1, next_page_number):
                block_parts.append(self.page_texts[current_page - 1])
            if next_page_number > page_number:
                block_parts.append(self.page_texts[next_page_number - 1][:next_start_index])

        question_items = split_question_items(cut_at_markers("\n".join(block_parts), SOURCE_CUT_MARKERS))
        prefix_items = split_question_items(clean_text(self.page_texts[page_number - 1][:start_index], strip_page_numbers=True))
        answer_items = self.answer_questions(exercise_number)
        fill_limit = max(answer_items) if answer_items else (max(question_items) if question_items else 0)
        for question_number in range(1, fill_limit + 1):
            if question_number not in question_items and question_number in prefix_items:
                question_items[question_number] = prefix_items[question_number]
        if not question_items and prefix_items:
            question_items = prefix_items.copy()

        if answer_items:
            max_answer_question = max(answer_items)
            question_items = {
                question_number: prompt_text
                for question_number, prompt_text in question_items.items()
                if question_number <= max_answer_question or (
                    looks_like_real_question(prompt_text) and
                    "Example" not in prompt_text and
                    "Solution:" not in prompt_text
                )
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


def make_text_answer(answer_source: object) -> dict[str, object]:
    accepted_answers = accepted_answers_from_source(answer_source)
    solution_text = compact_text(accepted_answers[0] if accepted_answers else "")
    return {
        "acceptedAnswers": accepted_answers,
        "solutionText": solution_text,
        "wrongReason": "Check the textbook answer carefully and compare the final form again.",
        "supportExample": f"Textbook answer: {solution_text}",
        "reteachTitle": "Textbook answer check",
        "reteachParagraphs": [f"Official answer: {solution_text}"],
        "exampleText": f"Textbook answer: {solution_text}",
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
    answer_payload = make_text_answer(answer_source) if answer_source is not None else make_notebook_answer(prompt_text)
    quiz_prompt = compact_text(prompt_text)
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
            loc(f"Book question: {compact_text(prompt_text)}"),
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
        source_questions = parser.source_questions(exercise_number)
        answer_questions = parser.answer_questions(exercise_number)
        manual_answers = MANUAL_ANSWER_OVERRIDES.get(exercise_number, {})

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
