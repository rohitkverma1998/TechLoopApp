package com.book.teachloop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SubjectPackLoaderTest {
    @Test
    fun `parseBookJson keeps inline topics for monolithic packs`() {
        val json = """
            {
              "id": "demo_book",
              "subjectTitle": {"english": "Math", "hindi": "गणित"},
              "bookTitle": {"english": "Demo", "hindi": "डेमो"},
              "teacherNote": {"english": "Note", "hindi": "नोट"},
              "topics": [
                ${topicJson("inline_topic", 1, "Chapter 1")}
              ]
            }
        """.trimIndent()

        val book = SubjectPackLoader.parseBookJson(
            json = json,
            assetPath = "subject_packs/demo_book.json",
            readAsset = { null },
        )

        assertNotNull(book)
        assertEquals(listOf("inline_topic"), book!!.topics.map { it.id })
    }

    @Test
    fun `parseBookJson merges chapter files from a split manifest`() {
        val manifest = """
            {
              "id": "demo_book",
              "subjectTitle": {"english": "Math", "hindi": "गणित"},
              "bookTitle": {"english": "Demo", "hindi": "डेमो"},
              "teacherNote": {"english": "Note", "hindi": "नोट"},
              "chapterAssetPaths": [
                "demo_book/chapter_01_intro.json",
                "demo_book/chapter_02_numbers.json"
              ]
            }
        """.trimIndent()
        val assets = mapOf(
            "subject_packs/demo_book/chapter_01_intro.json" to """
                {
                  "chapterNumber": 1,
                  "chapterTitle": {"english": "Intro", "hindi": "परिचय"},
                  "topics": [${topicJson("chapter_one_topic", 1, "Intro")}]
                }
            """.trimIndent(),
            "subject_packs/demo_book/chapter_02_numbers.json" to """
                {
                  "chapterNumber": 2,
                  "chapterTitle": {"english": "Numbers", "hindi": "संख्याएँ"},
                  "topics": [${topicJson("chapter_two_topic", 2, "Numbers")}]
                }
            """.trimIndent(),
        )

        val book = SubjectPackLoader.parseBookJson(
            json = manifest,
            assetPath = "subject_packs/demo_book.json",
            readAsset = assets::get,
        )

        assertNotNull(book)
        assertEquals(
            listOf("chapter_one_topic", "chapter_two_topic"),
            book!!.topics.map { it.id },
        )
        assertEquals("Note", book.teacherNote.english)
    }

    private fun topicJson(
        topicId: String,
        chapterNumber: Int,
        chapterTitle: String,
    ): String {
        return """
            {
              "id": "$topicId",
              "sourceLessonId": "lesson_$topicId",
              "chapterNumber": $chapterNumber,
              "chapterTitle": {"english": "$chapterTitle", "hindi": "$chapterTitle"},
              "lessonTitle": {"english": "Lesson", "hindi": "Lesson"},
              "subtopicTitle": {"english": "Subtopic", "hindi": "Subtopic"},
              "knowPrompt": {"english": "Know this?", "hindi": "Know this?"},
              "explanationTitle": {"english": "Explain", "hindi": "Explain"},
              "explanationParagraphs": [
                {"english": "Line 1", "hindi": "Line 1"}
              ],
              "examples": [
                {"english": "Example", "hindi": "Example"}
              ],
              "visuals": [],
              "questions": [
                {
                  "id": "q1",
                  "prompt": {"english": "What?", "hindi": "What?"},
                  "type": "TEXT_INPUT",
                  "options": [],
                  "correctOptionIndex": null,
                  "acceptedAnswers": ["1"],
                  "hint": {"english": "Hint", "hindi": "Hint"},
                  "wrongReason": {"english": "Wrong", "hindi": "Wrong"},
                  "supportExample": {"english": "Support", "hindi": "Support"},
                  "mistakeType": "GENERAL",
                  "reteachTitle": {"english": "Retry", "hindi": "Retry"},
                  "reteachParagraphs": [
                    {"english": "Again", "hindi": "Again"}
                  ]
                }
              ],
              "tags": [],
              "mistakeFocus": "GENERAL"
            }
        """.trimIndent()
    }
}
