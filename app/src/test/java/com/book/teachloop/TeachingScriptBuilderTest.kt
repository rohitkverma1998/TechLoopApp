package com.book.teachloop

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
    fun buildQuestionSolution_includesCorrectAnswerAndSupportExample() {
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

        assertTrue(script.any { it.contains("Solution for this question") })
        assertTrue(script.any { it.contains("Correct answer: thirteen thousand five hundred twenty") })
        assertTrue(script.any { it.contains("You skipped the hundreds part.") })
        assertTrue(script.any { it.contains("Example: 10,024 is ten thousand twenty-four.") })
    }
}
