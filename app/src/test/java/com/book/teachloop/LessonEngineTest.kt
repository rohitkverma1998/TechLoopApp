package com.book.teachloop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonEngineTest {
    private val book = LessonRepository.builtInMathBook()

    @Test
    fun mainQueue_usesAssignedChaptersWhenPresent() {
        val profile = StudentProfile(
            id = "p1",
            name = "Student",
            assignedChapterNumbers = listOf(1, 2),
        )

        val queue = StudyPlanner.buildMainQueue(book, profile)

        assertTrue(queue.isNotEmpty())
        assertTrue(queue.all { topicId ->
            book.topics.first { it.id == topicId }.chapterNumber in listOf(1, 2)
        })
    }

    @Test
    fun wrongAnswer_tracksMistakeAndKeepsTopicWeak() {
        val topic = book.topics.first()
        val profile = StudentProfile(id = "p1", name = "Student")

        val updated = StudyPlanner.updateProfileAfterAttempt(
            book = book,
            profile = profile,
            topic = topic,
            difficulty = Difficulty.EASY,
            mode = StudyMode.MAIN_PATH,
            correct = false,
            explanationRepeats = 1,
            mistakeType = MistakeType.PLACE_VALUE,
            timeSpentMillis = 1200L,
            now = 10_000L,
        )

        val progress = updated.topicProgress.getValue(topic.id)
        assertEquals(1, progress.wrongAnswers)
        assertEquals(MistakeType.PLACE_VALUE, progress.lastMistakeType)
        assertEquals(1, progress.mistakeCounts.getValue(MistakeType.PLACE_VALUE.name))
    }

    @Test
    fun correctRevision_addsRewardAndMastery() {
        val topic = book.topics.first()
        val firstPass = StudentProfile(
            id = "p1",
            name = "Student",
            topicProgress = mapOf(
                topic.id to TopicProgress(
                    topicId = topic.id,
                    mastered = true,
                    reviewStage = 0,
                    nextRevisionAt = 1L,
                ),
            ),
        )

        val updated = StudyPlanner.updateProfileAfterAttempt(
            book = book,
            profile = firstPass,
            topic = topic,
            difficulty = Difficulty.MEDIUM,
            mode = StudyMode.REVISION,
            correct = true,
            explanationRepeats = 0,
            mistakeType = null,
            timeSpentMillis = 1500L,
            now = 100_000L,
        )

        assertEquals(1, updated.revisionRewardCount)
        assertTrue(updated.topicProgress.getValue(topic.id).mastered)
    }
}
