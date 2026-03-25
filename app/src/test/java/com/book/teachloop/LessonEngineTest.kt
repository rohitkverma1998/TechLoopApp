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
            questionPrompt = topic.questions.firstOrNull()?.prompt,
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
    fun wrongAnswer_keepsSameQuestionReadyForRetry() {
        val engine = LessonEngine(book)
        val firstTopicId = book.topics.first().id
        engine.startSession(StudyMode.MAIN_PATH, listOf(firstTopicId))
        engine.answerKnowTopic(knowsTopic = true)

        val beforeQuestion = engine.currentQuestion(Difficulty.EASY)
        val wrongIndex = if (beforeQuestion?.correctOptionIndex == 0) 1 else 0
        engine.submitChoice(wrongIndex, Difficulty.EASY)

        val afterQuestion = engine.currentQuestion(Difficulty.EASY)

        assertEquals(LearningState.ASK_IF_KNOWN, engine.session.state)
        assertEquals(beforeQuestion?.id, afterQuestion?.id)
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
            questionPrompt = topic.questions.firstOrNull()?.prompt,
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

    @Test
    fun retryAfterFirstWrongAnswer_doesNotEarnTopicStars() {
        val topic = book.topics.first()
        val profile = StudentProfile(id = "p1", name = "Student")

        val afterWrong = StudyPlanner.updateProfileAfterAttempt(
            book = book,
            profile = profile,
            topic = topic,
            questionPrompt = topic.questions.firstOrNull()?.prompt,
            difficulty = Difficulty.EASY,
            mode = StudyMode.MAIN_PATH,
            correct = false,
            explanationRepeats = 0,
            mistakeType = MistakeType.READING,
            timeSpentMillis = 1000L,
            now = 1_000L,
        )
        val afterRetryCorrect = StudyPlanner.updateProfileAfterAttempt(
            book = book,
            profile = afterWrong,
            topic = topic,
            questionPrompt = topic.questions.firstOrNull()?.prompt,
            difficulty = Difficulty.EASY,
            mode = StudyMode.MAIN_PATH,
            correct = true,
            explanationRepeats = 1,
            mistakeType = null,
            timeSpentMillis = 1000L,
            now = 2_000L,
        )

        assertEquals(0, afterRetryCorrect.totalStars)
        assertEquals(0, afterRetryCorrect.topicProgress.getValue(topic.id).starsEarned)
        assertTrue(afterRetryCorrect.topicProgress.getValue(topic.id).mastered)
    }

    @Test
    fun report_separatesFirstAttemptCorrectAndWrongTopics() {
        val firstTopic = book.topics[0]
        val secondTopic = book.topics[1]
        val thirdTopic = book.topics[2]
        val profile = StudentProfile(
            id = "p1",
            name = "Student",
            topicProgress = mapOf(
                firstTopic.id to TopicProgress(
                    topicId = firstTopic.id,
                    totalAttempts = 1,
                    correctAnswers = 1,
                    firstAttemptCorrect = true,
                    firstAttemptQuestionPrompt = firstTopic.questions.firstOrNull()?.prompt,
                    mastered = true,
                    starsEarned = 1,
                ),
                secondTopic.id to TopicProgress(
                    topicId = secondTopic.id,
                    totalAttempts = 2,
                    correctAnswers = 1,
                    wrongAnswers = 1,
                    firstAttemptCorrect = false,
                    firstAttemptQuestionPrompt = secondTopic.questions.firstOrNull()?.prompt,
                    mastered = true,
                ),
                thirdTopic.id to TopicProgress(
                    topicId = thirdTopic.id,
                    totalAttempts = 1,
                    correctAnswers = 1,
                    mastered = true,
                ),
            ),
        )

        val report = StudyPlanner.buildReport(book, profile, now = 10_000L)

        assertTrue(report.firstAttemptCorrectTopics.any { it.english.contains(firstTopic.subtopicTitle.english) })
        assertTrue(report.firstAttemptWrongTopics.any { it.english.contains(secondTopic.subtopicTitle.english) })
        assertTrue(report.legacyTrackedTopics.any { it.english.contains(thirdTopic.subtopicTitle.english) })
    }
}
