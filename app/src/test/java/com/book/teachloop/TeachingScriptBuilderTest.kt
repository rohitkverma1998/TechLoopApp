package com.book.teachloop

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachingScriptBuilderTest {
    @Test
    fun build_includesExplanationVisualsAndExamples() {
        val topic = StudyTopic(
            id = "topic_1",
            sourceLessonId = "lesson_1",
            chapterNumber = 1,
            chapterTitle = text("Chapter 1", "अध्याय 1"),
            lessonTitle = text("Large Numbers", "बड़ी संख्याएँ"),
            subtopicTitle = text("Read commas", "कॉमा पढ़ना"),
            knowPrompt = text("Do you know this?", "क्या तुम यह जानते हो?"),
            explanationTitle = text("Commas break numbers", "कॉमा संख्याएँ तोड़ते हैं"),
            explanationParagraphs = listOf(
                text(
                    "Read the thousands part first. Then read the hundreds part.",
                    "पहले हजारों वाला भाग पढ़िए। फिर सैकड़ों वाला भाग पढ़िए।",
                ),
            ),
            examples = listOf(
                text("10,024 is ten thousand twenty-four.", "10,024 दस हजार चौबीस है।"),
            ),
            visuals = listOf(
                VisualBlock(
                    title = text("Reading flow", "पढ़ने का क्रम"),
                    description = text("Pause at the comma and continue.", "कॉमा पर रुककर आगे पढ़िए।"),
                    chips = listOf(text("10", "10"), text("024", "024")),
                    rows = listOf(
                        listOf(
                            text("ten thousand", "दस हजार"),
                            text("twenty-four", "चौबीस"),
                        ),
                    ),
                ),
            ),
            questions = emptyList(),
        )

        val script = TeachingScriptBuilder.build(topic, AppLanguage.ENGLISH)

        assertTrue(script.any { it.contains("Commas break numbers") })
        assertTrue(script.any { it.contains("Read the thousands part first.") })
        assertTrue(script.any { it.contains("Reading flow") })
        assertTrue(script.any { it.contains("Pause at the comma and continue.") })
        assertTrue(script.any { it.contains("ten thousand, twenty-four") })
        assertTrue(script.any { it.contains("Examples") })
        assertTrue(script.any { it.contains("Example 1: 10,024 is ten thousand twenty-four.") })
    }

    @Test
    fun buildQuestionSolution_includesQuestionAndSolutionOnly() {
        val topic = StudyTopic(
            id = "topic_1",
            sourceLessonId = "lesson_1",
            chapterNumber = 1,
            chapterTitle = text("Chapter 1"),
            lessonTitle = text("Large Numbers"),
            subtopicTitle = text("Read commas"),
            knowPrompt = text("Do you know this?"),
            explanationTitle = text("Commas break numbers"),
            explanationParagraphs = listOf(text("Read the thousands part first.")),
            examples = emptyList(),
            visuals = emptyList(),
            questions = emptyList(),
        )
        val question = RenderedQuestion(
            id = "q1",
            prompt = text("13,520 is read as"),
            type = QuestionType.TEXT_INPUT,
            acceptedAnswers = listOf("thirteen thousand five hundred twenty"),
            solutionAnswer = text("thirteen thousand five hundred twenty"),
            wrongReason = text("You skipped the hundreds part."),
            supportExample = text("10,024 is ten thousand twenty-four."),
            mistakeType = MistakeType.READING,
            reteachTitle = text("Read each place in order"),
            reteachParagraphs = listOf(text("Say the thousands part first and then the hundreds part.")),
        )
        val result = QuizResult(
            correct = false,
            message = text("Not correct yet."),
            wrongReason = text("You skipped the hundreds part."),
            supportExample = text("10,024 is ten thousand twenty-four."),
            mistakeType = MistakeType.READING,
            reteachTitle = text("Read each place in order"),
            reteachParagraphs = listOf(text("Say the thousands part first and then the hundreds part.")),
        )

        val script = TeachingScriptBuilder.buildQuestionSolution(topic, question, result, AppLanguage.ENGLISH)

        assertTrue(script.any { it == "Question: 13,520 is read as" })
        assertTrue(script.any { it == "Solution:" })
        assertTrue(script.any { it.contains("Say the thousands part first and then the hundreds part.") })
        assertTrue(script.none { it.contains("You skipped the hundreds part.") })
        assertTrue(script.none { it.contains("10,024 is ten thousand twenty-four.") })
    }

    @Test
    fun buildQuestionSolution_withoutStoredSolution_showsGeneratorNote() {
        val topic = StudyTopic(
            id = "topic_1",
            sourceLessonId = "lesson_1",
            chapterNumber = 1,
            chapterTitle = text("Chapter 1"),
            lessonTitle = text("Large Numbers"),
            subtopicTitle = text("Read commas"),
            knowPrompt = text("Do you know this?"),
            explanationTitle = text("Commas break numbers"),
            explanationParagraphs = listOf(text("Read the thousands part first.")),
            examples = emptyList(),
            visuals = emptyList(),
            questions = emptyList(),
        )
        val question = RenderedQuestion(
            id = "q1",
            prompt = text("13,520 is read as"),
            type = QuestionType.TEXT_INPUT,
            acceptedAnswers = listOf("thirteen thousand five hundred twenty"),
            solutionAnswer = text("thirteen thousand five hundred twenty"),
            wrongReason = text("You skipped the hundreds part."),
            supportExample = text("10,024 is ten thousand twenty-four."),
            mistakeType = MistakeType.READING,
            reteachParagraphs = emptyList(),
        )

        val script = TeachingScriptBuilder.buildQuestionSolution(topic, question, null, AppLanguage.ENGLISH)

        assertTrue(script.any { it.contains("No prompt-generated solution is stored for this question yet.") })
    }

    @Test
    fun build_forRsMainTeach_omitsGenericTitleAndExampleLabels() {
        val topic = StudyTopic(
            id = "topic_1",
            sourceLessonId = "rs_ch10",
            chapterNumber = 10,
            chapterTitle = text("Decimals"),
            lessonTitle = text("Decimals"),
            subtopicTitle = text("Decimal Fractions and Place Value Chart"),
            knowPrompt = text("Do you know this?"),
            explanationTitle = text("Decimal Fractions and Place Value Chart"),
            explanationParagraphs = listOf(
                text("Key idea: Decimal fractions use places after the decimal point."),
                text("Worked example: In 18.956, the digit 5 is in the hundredths place."),
            ),
            examples = listOf(
                text("Remember: The 5 is in the second place after the decimal point."),
            ),
            visuals = emptyList(),
            questions = emptyList(),
        )

        val script = TeachingScriptBuilder.build(topic, AppLanguage.ENGLISH)

        assertFalse(script.any { it == "Decimal Fractions and Place Value Chart" })
        assertFalse(script.any { it == "Examples" })
        assertFalse(script.any { it.startsWith("Example 1:") })
        assertTrue(script.any { it.contains("Key idea: Decimal fractions use places after the decimal point.") })
        assertTrue(script.any { it.contains("Remember: The 5 is in the second place after the decimal point.") })
    }

    @Test
    fun build_forRsExerciseTeach_omitsGenericTitleAndExampleLabels() {
        val topic = StudyTopic(
            id = "topic_2",
            sourceLessonId = "rs_exercise_ch10_ex39",
            chapterNumber = 10,
            chapterTitle = text("Decimals"),
            lessonTitle = text("Exercise 39"),
            subtopicTitle = text("Question 10.39.1(a)"),
            knowPrompt = text("Can you solve Question 10.39.1(a)?"),
            explanationTitle = text("Question 10.39.1(a)"),
            explanationParagraphs = listOf(
                text("Key idea: Decimals depend on place value after the decimal point."),
                text("How to begin: Read the whole-number part, then say each digit after the decimal point."),
                text("Use this model: 16.23 is read as sixteen point two three."),
                text("Write the final answer in words exactly as the question asks."),
            ),
            examples = emptyList(),
            visuals = emptyList(),
            questions = emptyList(),
        )

        val script = TeachingScriptBuilder.build(topic, AppLanguage.ENGLISH)

        assertFalse(script.any { it == "Question 10.39.1(a)" })
        assertFalse(script.any { it == "Examples" })
        assertFalse(script.any { it.startsWith("Example 1:") })
        assertTrue(script.any { it.contains("Key idea: Decimals depend on place value after the decimal point.") })
        assertTrue(script.any { it.contains("Use this model: 16.23 is read as sixteen point two three.") })
        assertTrue(script.any { it.contains("Write the final answer in words exactly as the question asks.") })
    }
}
