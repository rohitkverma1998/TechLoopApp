package com.book.teachloop

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.roundToInt

class LessonEngine(
    private val book: StudyBook,
) {
    private val topicsById = book.topics.associateBy { it.id }

    var session: SessionSnapshot = SessionSnapshot(bookId = book.id)
        private set

    fun restore(snapshot: SessionSnapshot) {
        val validQueue = snapshot.queueTopicIds.filter { topicsById.containsKey(it) }
        session = if (snapshot.bookId != book.id || validQueue.isEmpty()) {
            SessionSnapshot(bookId = book.id)
        } else {
            val restoredState = if (snapshot.state == LearningState.ASK_IF_KNOWN) {
                LearningState.TAKE_QUIZ
            } else {
                snapshot.state
            }
            snapshot.copy(
                bookId = book.id,
                queueTopicIds = validQueue,
                queueIndex = snapshot.queueIndex.coerceIn(0, validQueue.lastIndex),
                state = restoredState,
            )
        }
    }

    fun snapshot(): SessionSnapshot = session

    fun hasActiveSession(): Boolean = session.state != LearningState.DASHBOARD

    fun startSession(
        mode: StudyMode,
        topicIds: List<String>,
        timerSettings: TimerSettings = TimerSettings(),
        startingStarsQuarters: Int = 0,
        now: Long = System.currentTimeMillis(),
    ) {
        if (topicIds.isEmpty()) {
            session = SessionSnapshot(bookId = book.id)
            return
        }

        val totalQuestions = topicIds.size
        val timerEnabled = timerSettings.enabled && totalQuestions > 0
        val timerDurationMillis = if (timerEnabled) {
            totalQuestions.toLong() * timerSettings.secondsPerQuestion.coerceAtLeast(1).toLong() * 1000L
        } else {
            0L
        }

        session = SessionSnapshot(
            bookId = book.id,
            mode = mode,
            queueTopicIds = topicIds,
            queueIndex = 0,
            state = LearningState.TAKE_QUIZ,
            currentTopicStartedAt = now,
            totalQuestions = totalQuestions,
            timerEnabled = timerEnabled,
            timerDurationMillis = timerDurationMillis,
            timerStartedAt = if (timerEnabled) now else 0L,
            timerEndsAt = if (timerEnabled) now + timerDurationMillis else 0L,
            startingStarsQuarters = startingStarsQuarters,
        )
    }

    fun finishSession() {
        session = SessionSnapshot(bookId = book.id)
    }

    fun currentTopic(): StudyTopic? {
        return session.queueTopicIds.getOrNull(session.queueIndex)?.let(topicsById::get)
    }

    fun currentTopicPosition(): Int = session.queueIndex + 1

    fun totalQueuedTopics(): Int = session.queueTopicIds.size

    fun totalQuestionCount(): Int = session.totalQuestions.coerceAtLeast(session.queueTopicIds.size)

    fun currentExplanationRepeats(): Int = session.explanationRepeats

    fun currentMistakeType(): MistakeType? = session.lastMistakeType

    fun currentTopicStartedAt(): Long = session.currentTopicStartedAt

    fun remainingTimerMillis(now: Long = System.currentTimeMillis()): Long? {
        if (!session.timerEnabled) return null
        if (session.state == LearningState.DASHBOARD ||
            session.state == LearningState.SESSION_COMPLETE ||
            session.state == LearningState.SESSION_TIMEOUT
        ) {
            return null
        }
        return (session.timerEndsAt - now).coerceAtLeast(0L)
    }

    fun attemptedQuestionCount(): Int = session.attemptedTopicIds.size

    fun correctQuestionCount(): Int = session.correctTopicIds.size

    fun wrongQuestionCount(): Int = (attemptedQuestionCount() - correctQuestionCount()).coerceAtLeast(0)

    fun markTimedOut(now: Long = System.currentTimeMillis()) {
        if (!session.timerEnabled) return
        if (session.state == LearningState.DASHBOARD ||
            session.state == LearningState.SESSION_COMPLETE ||
            session.state == LearningState.SESSION_TIMEOUT
        ) {
            return
        }
        session = session.copy(
            state = LearningState.SESSION_TIMEOUT,
            currentTopicStartedAt = now,
            lastMistakeType = null,
        )
    }

    fun answerKnowTopic(knowsTopic: Boolean) {
        currentTopic() ?: run {
            session = SessionSnapshot(bookId = book.id)
            return
        }

        session = if (knowsTopic) {
            session.copy(state = LearningState.TAKE_QUIZ)
        } else {
            session.copy(
                explanationRepeats = session.explanationRepeats + 1,
                state = LearningState.EXPLAIN_TOPIC,
            )
        }
    }

    fun answerUnderstood(understood: Boolean) {
        currentTopic() ?: run {
            session = SessionSnapshot(bookId = book.id)
            return
        }

        session = if (understood) {
            session.copy(state = LearningState.TAKE_QUIZ)
        } else {
            session.copy(
                explanationRepeats = session.explanationRepeats + 1,
                state = LearningState.EXPLAIN_TOPIC,
            )
        }
    }

    fun currentQuestion(difficulty: Difficulty = Difficulty.EASY): RenderedQuestion? {
        val topic = currentTopic() ?: return null
        if (topic.questions.isEmpty()) return null

        val baseIndex = session.questionIndex % topic.questions.size
        val baseQuestion = topic.questions[baseIndex]
        val strippedTopicContent =
            topic.explanationParagraphs.isEmpty() &&
                topic.examples.isEmpty() &&
                topic.visuals.isEmpty() &&
                baseQuestion.hint == null &&
                baseQuestion.wrongReason == null &&
                baseQuestion.supportExample == null &&
                baseQuestion.reteachTitle == null &&
                baseQuestion.reteachParagraphs.isEmpty()
        val supportExample = baseQuestion.supportExample
            ?: topic.examples.firstOrNull()
            ?: topic.explanationParagraphs.firstOrNull()
            ?: text("")
        val wrongReason = if (strippedTopicContent) {
            text("")
        } else {
            baseQuestion.wrongReason ?: text(
            english = "This answer does not match the key idea of ${topic.subtopicTitle.english.lowercase()}.",
            hindi = "${topic.subtopicTitle.hindi.lowercase()} के मुख्य विचार से यह उत्तर मेल नहीं खाता।",
            )
        }

        val correctText = when (baseQuestion.type) {
            QuestionType.MULTIPLE_CHOICE -> {
                baseQuestion.options[baseQuestion.correctOptionIndex ?: 0].english
            }

            QuestionType.TEXT_INPUT -> {
                baseQuestion.acceptedAnswers.firstOrNull().orEmpty()
            }
        }

        return when (difficulty) {
            Difficulty.EASY -> RenderedQuestion(
                id = "${baseQuestion.id}_easy",
                prompt = baseQuestion.prompt,
                type = baseQuestion.type,
                options = baseQuestion.options,
                correctOptionIndex = baseQuestion.correctOptionIndex,
                acceptedAnswers = baseQuestion.acceptedAnswers,
                solutionAnswer = when (baseQuestion.type) {
                    QuestionType.MULTIPLE_CHOICE -> baseQuestion.options[baseQuestion.correctOptionIndex ?: 0]
                    QuestionType.TEXT_INPUT -> text(correctText, correctText)
                },
                hint = baseQuestion.hint,
                wrongReason = wrongReason,
                supportExample = supportExample,
                mistakeType = baseQuestion.mistakeType,
                reteachTitle = baseQuestion.reteachTitle,
                reteachParagraphs = baseQuestion.reteachParagraphs,
                questionImageAsset = baseQuestion.questionImageAsset,
            )

            Difficulty.MEDIUM -> RenderedQuestion(
                id = "${baseQuestion.id}_medium",
                prompt = text(
                    english = "${baseQuestion.prompt.english} Solve it carefully and say the pattern to yourself.",
                    hindi = "${baseQuestion.prompt.hindi} इसे ध्यान से हल कीजिए और पैटर्न मन में बोलिए।",
                ),
                type = baseQuestion.type,
                options = baseQuestion.options,
                correctOptionIndex = baseQuestion.correctOptionIndex,
                acceptedAnswers = baseQuestion.acceptedAnswers,
                solutionAnswer = when (baseQuestion.type) {
                    QuestionType.MULTIPLE_CHOICE -> baseQuestion.options[baseQuestion.correctOptionIndex ?: 0]
                    QuestionType.TEXT_INPUT -> text(correctText, correctText)
                },
                hint = if (baseQuestion.hint != null) {
                    baseQuestion.hint
                } else if (strippedTopicContent) {
                    null
                } else {
                    text(
                    english = "Think step by step before answering.",
                    hindi = "उत्तर देने से पहले एक-एक कदम सोचिए।",
                    )
                },
                wrongReason = wrongReason,
                supportExample = supportExample,
                mistakeType = baseQuestion.mistakeType,
                reteachTitle = baseQuestion.reteachTitle,
                reteachParagraphs = baseQuestion.reteachParagraphs,
                questionImageAsset = baseQuestion.questionImageAsset,
            )

            Difficulty.HARD -> RenderedQuestion(
                id = "${baseQuestion.id}_hard",
                prompt = when (baseQuestion.type) {
                    QuestionType.MULTIPLE_CHOICE -> {
                        text(
                            english = "${baseQuestion.prompt.english} Choose the correct option.",
                            hindi = "${baseQuestion.prompt.hindi} सही विकल्प चुनिए।",
                        )
                    }

                    QuestionType.TEXT_INPUT -> {
                        text(
                            english = "${baseQuestion.prompt.english} Write the final answer.",
                            hindi = "${baseQuestion.prompt.hindi} अंतिम उत्तर लिखिए।",
                        )
                    }
                },
                type = baseQuestion.type,
                options = baseQuestion.options,
                correctOptionIndex = baseQuestion.correctOptionIndex,
                acceptedAnswers = (listOf(correctText) + baseQuestion.acceptedAnswers).distinct(),
                solutionAnswer = when (baseQuestion.type) {
                    QuestionType.MULTIPLE_CHOICE -> baseQuestion.options[baseQuestion.correctOptionIndex ?: 0]
                    QuestionType.TEXT_INPUT -> text(correctText, correctText)
                },
                hint = null,
                wrongReason = wrongReason,
                supportExample = supportExample,
                mistakeType = baseQuestion.mistakeType,
                reteachTitle = baseQuestion.reteachTitle,
                reteachParagraphs = baseQuestion.reteachParagraphs,
                questionImageAsset = baseQuestion.questionImageAsset,
            )
        }
    }

    fun submitChoice(
        selectedIndex: Int,
        difficulty: Difficulty,
        now: Long = System.currentTimeMillis(),
    ): QuizResult {
        val question = currentQuestion(difficulty)
            ?: return QuizResult(
                correct = false,
                message = text("No question is available for this topic."),
            )

        val isCorrect = question.type == QuestionType.MULTIPLE_CHOICE &&
            question.correctOptionIndex == selectedIndex
        return finishAnswer(isCorrect, question, now)
    }

    fun submitText(
        answer: String,
        difficulty: Difficulty,
        now: Long = System.currentTimeMillis(),
    ): QuizResult {
        val question = currentQuestion(difficulty)
            ?: return QuizResult(
                correct = false,
                message = text("No question is available for this topic."),
            )

        val normalizedAnswer = normalize(answer)
        val unitFreeAnswer = normalizeIgnoringUnits(answer)
        val isCorrect = question.acceptedAnswers.any { accepted ->
            val normalizedAccepted = normalize(accepted)
            val unitFreeAccepted = normalizeIgnoringUnits(accepted)
            normalizedAccepted == normalizedAnswer ||
                unitFreeAccepted == unitFreeAnswer ||
                normalizedAccepted == unitFreeAnswer ||
                unitFreeAccepted == normalizedAnswer
        }
        return finishAnswer(isCorrect, question, now)
    }

    fun explanationToken(): String {
        val topicId = currentTopic()?.id ?: "dashboard"
        return "$topicId-${session.explanationRepeats}-${session.state}"
    }

    private fun finishAnswer(
        isCorrect: Boolean,
        question: RenderedQuestion,
        now: Long,
    ): QuizResult {
        val topic = currentTopic()
            ?: return QuizResult(
                correct = false,
                message = text("No topic is active."),
            )

        val updatedAttemptedTopicIds = (session.attemptedTopicIds + topic.id).distinct()

        return if (isCorrect) {
            val nextQueueIndex = session.queueIndex + 1
            val updatedCorrectTopicIds = (session.correctTopicIds + topic.id).distinct()
            session = if (nextQueueIndex >= session.queueTopicIds.size) {
                session.copy(
                    queueIndex = session.queueTopicIds.lastIndex,
                    questionIndex = 0,
                    explanationRepeats = 0,
                    state = LearningState.SESSION_COMPLETE,
                    currentTopicStartedAt = now,
                    lastMistakeType = null,
                    attemptedTopicIds = updatedAttemptedTopicIds,
                    correctTopicIds = updatedCorrectTopicIds,
                )
            } else {
                session.copy(
                    queueIndex = nextQueueIndex,
                    questionIndex = 0,
                    explanationRepeats = 0,
                    state = LearningState.TAKE_QUIZ,
                    currentTopicStartedAt = now,
                    lastMistakeType = null,
                    attemptedTopicIds = updatedAttemptedTopicIds,
                    correctTopicIds = updatedCorrectTopicIds,
                )
            }

            val message = if (session.state == LearningState.SESSION_COMPLETE) {
                text(
                    english = "Correct. This session is complete.",
                    hindi = "सही। यह अध्ययन सत्र पूरा हो गया।",
                )
            } else {
                text(
                    english = "Correct. ${topic.subtopicTitle.english} is complete. Moving to the next step.",
                    hindi = "सही। ${topic.subtopicTitle.hindi} पूरा हुआ। अब अगले चरण पर चलते हैं।",
                )
            }

            QuizResult(
                correct = true,
                message = message,
            )
        } else {
            session = session.copy(
                questionIndex = session.questionIndex.coerceIn(0, max(topic.questions.lastIndex, 0)),
                explanationRepeats = 0,
                state = LearningState.TAKE_QUIZ,
                currentTopicStartedAt = now,
                lastMistakeType = question.mistakeType,
                attemptedTopicIds = updatedAttemptedTopicIds,
            )

            QuizResult(
                correct = false,
                message = text(
                    english = "Not correct yet. Let us revisit the idea and try once more.",
                    hindi = "अभी सही नहीं हुआ। चलिए विचार को फिर से देखते हैं और एक बार और कोशिश करते हैं।",
                ),
                wrongReason = question.wrongReason.takeUnless {
                    it.english.isBlank() && it.hindi.isBlank()
                },
                supportExample = question.supportExample.takeUnless {
                    it.english.isBlank() && it.hindi.isBlank()
                },
                mistakeType = question.mistakeType,
                reteachTitle = question.reteachTitle,
                reteachParagraphs = question.reteachParagraphs,
            )
        }
    }

    private fun normalize(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace(",", "")
            .replace(".", "")
            .replace("?", "")
            .replace("-", " ")
            .replace("'", "")
            .replace(Regex("\\s+"), " ")
    }

    private fun normalizeIgnoringUnits(value: String): String {
        if (value.none { it.isDigit() }) {
            return normalize(value)
        }

        var sanitized = value.lowercase()
        val unitPatterns = listOf(
            "sq\\.?\\s*(cm|m|km|mm)",
            "cu\\.?\\s*(cm|m|km|mm)",
            "square\\s+(centimetres?|centimeters?|metres?|meters?|kilometres?|kilometers?|millimetres?|millimeters?)",
            "cubic\\s+(centimetres?|centimeters?|metres?|meters?|kilometres?|kilometers?|millimetres?|millimeters?)",
            "centimetres?",
            "centimeters?",
            "metres?",
            "meters?",
            "kilometres?",
            "kilometers?",
            "millimetres?",
            "millimeters?",
            "kilograms?",
            "milligrams?",
            "grams?",
            "millilitres?",
            "milliliters?",
            "litres?",
            "liters?",
            "rupees?",
            "paise",
            "paisa",
            "cm2",
            "m2",
            "km2",
            "mm2",
            "cm3",
            "m3",
            "km3",
            "mm3",
            "kg",
            "mg",
            "kl",
            "hl",
            "ml",
            "km",
            "hm",
            "dam",
            "dm",
            "cm",
            "mm",
            "rs\\.?",
            "g",
            "l",
            "m",
            "₹",
        )
        unitPatterns.forEach { pattern ->
            sanitized = sanitized.replace(Regex("\\b(?:$pattern)\\b"), " ")
        }
        return normalize(sanitized)
    }
}

object StudyPlanner {
    private val reviewDelays = listOf(1L, 3L, 7L)

    fun buildMainQueue(book: StudyBook, profile: StudentProfile): List<String> {
        return eligibleMainTopics(book, profile)
            .filter { !(profile.topicProgress[it.id]?.mastered ?: false) }
            .map { it.id }
    }

    fun buildExerciseQueue(book: StudyBook, profile: StudentProfile): List<String> {
        return eligibleExerciseTopics(book, profile)
            .filter { !(profile.topicProgress[it.id]?.mastered ?: false) }
            .map { it.id }
    }

    fun buildRevisionQueue(
        book: StudyBook,
        profile: StudentProfile,
        now: Long,
    ): List<String> {
        return eligibleMainTopics(book, profile)
            .filter { topic ->
                val progress = profile.topicProgress[topic.id] ?: return@filter false
                progress.mastered && progress.nextRevisionAt in 1..now
            }
            .sortedBy { profile.topicProgress[it.id]?.nextRevisionAt ?: Long.MAX_VALUE }
            .map { it.id }
    }

    fun buildWeakTopicQueue(
        book: StudyBook,
        profile: StudentProfile,
    ): List<String> {
        return eligibleMainTopics(book, profile)
            .filter { topic -> isWeak(profile.topicProgress[topic.id]) }
            .sortedByDescending { topic ->
                val progress = profile.topicProgress[topic.id]
                (progress?.wrongAnswers ?: 0) + (progress?.explanationRepeats ?: 0)
            }
            .map { it.id }
    }

    fun buildReport(
        book: StudyBook,
        profile: StudentProfile,
        now: Long,
    ): ReportSummary {
        val mainBookTopics = book.topics.filterNot(::isExerciseTopic)
        val exerciseBookTopics = book.topics.filter(::isExerciseTopic)
        val reportMainTopics = eligibleMainTopics(book, profile)
        val masteredTopics = mainBookTopics.count { profile.topicProgress[it.id]?.mastered == true }
        val exerciseMasteredTopics = exerciseBookTopics.count { profile.topicProgress[it.id]?.mastered == true }
        val dueRevisionTopics = reportMainTopics.count { topic ->
            val progress = profile.topicProgress[topic.id]
            progress?.mastered == true && progress.nextRevisionAt in 1..now
        }
        val weakTopics = reportMainTopics.count { topic -> isWeak(profile.topicProgress[topic.id]) }
        val supportHeavyTopics = reportMainTopics.count { topic ->
            (profile.topicProgress[topic.id]?.explanationRepeats ?: 0) >= 2
        }
        val focusTopics = reportMainTopics
            .filter { topic ->
                val progress = profile.topicProgress[topic.id]
                ((progress?.wrongAnswers ?: 0) + (progress?.explanationRepeats ?: 0)) > 0
            }
            .sortedByDescending { topic ->
                val progress = profile.topicProgress[topic.id]
                (progress?.wrongAnswers ?: 0) + (progress?.explanationRepeats ?: 0)
            }
            .take(4)
            .map { it.subtopicTitle }

        val weakTopicTitles = reportMainTopics
            .filter { topic -> isWeak(profile.topicProgress[topic.id]) }
            .take(6)
            .map { it.subtopicTitle }

        val firstAttemptCorrectTopics = reportMainTopics
            .mapNotNull { topic ->
                val progress = profile.topicProgress[topic.id] ?: return@mapNotNull null
                if (progress.firstAttemptCorrect == true) attemptSummaryText(topic, progress) else null
            }

        val firstAttemptWrongTopics = reportMainTopics
            .mapNotNull { topic ->
                val progress = profile.topicProgress[topic.id] ?: return@mapNotNull null
                if (progress.firstAttemptCorrect == false) attemptSummaryText(topic, progress) else null
            }

        val legacyTrackedTopics = reportMainTopics
            .mapNotNull { topic ->
                val progress = profile.topicProgress[topic.id] ?: return@mapNotNull null
                if (progress.totalAttempts > 0 && progress.firstAttemptCorrect == null) {
                    attemptSummaryText(topic, progress)
                } else {
                    null
                }
            }

        val chapterMastery = mainBookTopics
            .groupBy { it.chapterNumber }
            .toSortedMap()
            .map { (chapterNumber, topics) ->
                ChapterMastery(
                    chapterNumber = chapterNumber,
                    chapterTitle = topics.first().chapterTitle,
                    masteredTopics = topics.count { profile.topicProgress[it.id]?.mastered == true },
                    totalTopics = topics.size,
                )
            }

        val totalTimeMinutes = profile.topicProgress.values.sumOf { it.timeSpentMillis } / 60000
        val exerciseStars = starsForTopics(exerciseBookTopics, profile)
        val mainStars = profile.totalStars - exerciseStars
        val topMistakes = profile.topicProgress.values
            .flatMap { progress -> progress.mistakeCounts.entries }
            .groupBy({ it.key }, { it.value })
            .mapNotNull { (typeName, values) ->
                runCatching { MistakeType.valueOf(typeName) }.getOrNull()?.let { type ->
                    MistakeBreakdown(type = type, count = values.sum())
                }
            }
            .sortedByDescending { it.count }
            .take(4)

        val chartPoints = listOf(
            ChartPoint(
                label = text("Mastery", "मास्टरी"),
                value = masteredTopics,
                maxValue = mainBookTopics.size.coerceAtLeast(1),
            ),
            ChartPoint(
                label = text("Revision due", "रिविजन"),
                value = dueRevisionTopics,
                maxValue = mainBookTopics.size.coerceAtLeast(1),
            ),
            ChartPoint(
                label = text("Weak topics", "कमजोर विषय"),
                value = weakTopics,
                maxValue = mainBookTopics.size.coerceAtLeast(1),
            ),
            ChartPoint(
                label = text("Stars", "सितारे"),
                value = profile.totalStars,
                maxValue = max(
                    profile.totalStars,
                    mainBookTopics.size * Difficulty.HARD.starValue,
                ).coerceAtLeast(1),
            ),
        )

        return ReportSummary(
            masteredTopics = masteredTopics,
            totalTopics = mainBookTopics.size,
            dueRevisionTopics = dueRevisionTopics,
            weakTopics = weakTopics,
            supportHeavyTopics = supportHeavyTopics,
            totalStars = profile.totalStars,
            mainStars = mainStars,
            exerciseMasteredTopics = exerciseMasteredTopics,
            exerciseTotalTopics = exerciseBookTopics.size,
            exerciseStars = exerciseStars,
            firstAttemptCorrectTopics = firstAttemptCorrectTopics,
            firstAttemptWrongTopics = firstAttemptWrongTopics,
            legacyTrackedTopics = legacyTrackedTopics,
            focusTopics = focusTopics,
            weakTopicTitles = weakTopicTitles,
            chartPoints = chartPoints,
            chapterMastery = chapterMastery,
            topMistakes = topMistakes,
            totalTimeMinutes = totalTimeMinutes.toInt(),
            streakDays = profile.streakDays,
            badges = profile.badges.takeLast(8).reversed(),
            chapterTrophies = profile.chapterTrophies.sorted(),
            revisionRewardCount = profile.revisionRewardCount,
        )
    }

    fun updateProfileAfterAttempt(
        book: StudyBook,
        profile: StudentProfile,
        topic: StudyTopic,
        questionPrompt: LocalizedText?,
        difficulty: Difficulty,
        mode: StudyMode?,
        correct: Boolean,
        explanationRepeats: Int,
        mistakeType: MistakeType?,
        timeSpentMillis: Long,
        now: Long,
        starSettings: StarSettings = StarSettings(),
    ): StudentProfile {
        val existing = profile.topicProgress[topic.id] ?: TopicProgress(topicId = topic.id)
        val updatedMistakeCounts = if (!correct && mistakeType != null) {
            existing.mistakeCounts.toMutableMap().apply {
                val key = mistakeType.name
                this[key] = (this[key] ?: 0) + 1
            }
        } else {
            existing.mistakeCounts
        }
        val firstAttemptCorrect = existing.firstAttemptCorrect ?: correct
        val firstAttemptQuestionPrompt = existing.firstAttemptQuestionPrompt ?: questionPrompt

        val updatedProgress = if (correct) {
            val stage = when {
                !existing.mastered -> 0
                mode == StudyMode.REVISION -> (existing.reviewStage + 1).coerceAtMost(reviewDelays.lastIndex)
                else -> existing.reviewStage
            }
            val firstAttemptWasCorrect = existing.totalAttempts == 0 && existing.wrongAnswers == 0
            val topicStars = if (firstAttemptWasCorrect) {
                max(existing.starsEarned, starSettings.correctStars.coerceAtLeast(0f).roundToInt().coerceAtLeast(1))
            } else {
                existing.starsEarned
            }

            existing.copy(
                totalAttempts = existing.totalAttempts + 1,
                correctAnswers = existing.correctAnswers + 1,
                firstAttemptCorrect = firstAttemptCorrect,
                firstAttemptQuestionPrompt = firstAttemptQuestionPrompt,
                explanationRepeats = existing.explanationRepeats + explanationRepeats,
                mastered = true,
                starsEarned = topicStars,
                reviewStage = stage,
                lastStudiedAt = now,
                nextRevisionAt = now + reviewDelays[stage] * DAY_IN_MILLIS,
                timeSpentMillis = existing.timeSpentMillis + timeSpentMillis,
                mistakeCounts = updatedMistakeCounts,
            )
        } else {
            existing.copy(
                totalAttempts = existing.totalAttempts + 1,
                wrongAnswers = existing.wrongAnswers + 1,
                firstAttemptCorrect = firstAttemptCorrect,
                firstAttemptQuestionPrompt = firstAttemptQuestionPrompt,
                explanationRepeats = existing.explanationRepeats + explanationRepeats,
                lastStudiedAt = now,
                reviewStage = 0,
                nextRevisionAt = now + DAY_IN_MILLIS,
                timeSpentMillis = existing.timeSpentMillis + timeSpentMillis,
                lastMistakeType = mistakeType,
                mistakeCounts = updatedMistakeCounts,
            )
        }

        val updatedMap = profile.topicProgress.toMutableMap()
        updatedMap[topic.id] = updatedProgress
        val baseStars = updatedMap.values.sumOf { it.starsEarned }
        val revisionBonus = if (correct && mode == StudyMode.REVISION) starSettings.revisionBonus.coerceAtLeast(0f).roundToInt() else 0
        val isWrongFirstAttempt = !correct && existing.totalAttempts == 0
        val streakDays = updatedStreak(profile, now)
        val revisionRewards = profile.revisionRewardCount + revisionBonus
        val chapterTrophies = updateChapterTrophies(book, updatedMap, profile.chapterTrophies)
        val badges = updateBadges(
            book = book,
            existingProfile = profile,
            updatedProgressMap = updatedMap,
            topic = topic,
            mode = mode,
            correct = correct,
            explanationRepeats = explanationRepeats,
            streakDays = streakDays,
            chapterTrophies = chapterTrophies,
            now = now,
        )

        return profile.copy(
            totalStars = baseStars + revisionBonus,
            starPenaltyQuarters = profile.starPenaltyQuarters + if (isWrongFirstAttempt) (starSettings.wrongPenalty.coerceAtLeast(0f) * 4).roundToInt() else 0,
            topicProgress = updatedMap,
            badges = badges,
            chapterTrophies = chapterTrophies,
            streakDays = streakDays,
            lastActiveDay = dayKey(now),
            revisionRewardCount = revisionRewards,
        )
    }

    private fun updateChapterTrophies(
        book: StudyBook,
        progressMap: Map<String, TopicProgress>,
        currentTrophies: List<Int>,
    ): List<Int> {
        val trophies = currentTrophies.toMutableSet()
        book.topics
            .filterNot(::isExerciseTopic)
            .groupBy { it.chapterNumber }
            .forEach { (chapterNumber, topics) ->
            if (topics.all { progressMap[it.id]?.mastered == true }) {
                trophies += chapterNumber
            }
        }
        return trophies.toList().sorted()
    }

    private fun updateBadges(
        book: StudyBook,
        existingProfile: StudentProfile,
        updatedProgressMap: Map<String, TopicProgress>,
        topic: StudyTopic,
        mode: StudyMode?,
        correct: Boolean,
        explanationRepeats: Int,
        streakDays: Int,
        chapterTrophies: List<Int>,
        now: Long,
    ): List<BadgeAward> {
        val badges = existingProfile.badges.toMutableList()
        val masteredCount = updatedProgressMap.values.count { it.mastered }

        fun award(type: BadgeType, title: LocalizedText, reason: LocalizedText) {
            val exists = badges.any { it.type == type && it.reason.english == reason.english }
            if (!exists) {
                badges += BadgeAward(
                    type = type,
                    title = title,
                    reason = reason,
                    earnedAt = now,
                )
            }
        }

        if (correct && masteredCount == 1) {
            award(
                BadgeType.FIRST_MASTERED,
                text("First step", "पहला कदम"),
                text("Completed the first mastered topic.", "पहला विषय पूरी तरह सीखा।"),
            )
        }

        if (correct && explanationRepeats > 0) {
            award(
                BadgeType.BRAVE_RETRY,
                text("Brave retry", "बहादुर दोबारा प्रयास"),
                text("Came back after support and answered correctly.", "सहायता के बाद फिर से सही उत्तर दिया।"),
            )
        }

        if (correct && mode == StudyMode.REVISION) {
            award(
                BadgeType.REVISION_RANGER,
                text("Revision ranger", "रिविजन रेंजर"),
                text("Completed a spaced revision check.", "अंतराल वाली पुनरावृत्ति पूरी की।"),
            )
        }

        if (streakDays >= 3) {
            award(
                BadgeType.STREAK_KEEPER,
                text("Streak keeper", "स्ट्रीक कीपर"),
                text("Learned on three or more days in a row.", "लगातार तीन या अधिक दिनों तक सीखा।"),
            )
        }

        if (correct && !isExerciseTopic(topic) && chapterTrophies.contains(topic.chapterNumber)) {
            val chapterName = chapterLabel(topic.chapterNumber, topic.chapterTitle)
            award(
                BadgeType.CHAPTER_CHAMP,
                text("Chapter champ", "अध्याय चैंप"),
                text(
                    english = "Finished every subtopic in ${chapterName.english}.",
                    hindi = "${chapterName.hindi} के सभी उपविषय पूरे किए।",
                ),
            )
        }

        if (correct && !isExerciseTopic(topic) && existingProfile.assignedChapterNumbers.isNotEmpty()) {
            val assignedTopics = book.topics
                .filterNot(::isExerciseTopic)
                .filter { it.chapterNumber in existingProfile.assignedChapterNumbers }
            val assignedComplete = assignedTopics.isNotEmpty() &&
                assignedTopics.all { updatedProgressMap[it.id]?.mastered == true }
            if (assignedComplete) {
                award(
                    BadgeType.ASSIGNMENT_ACE,
                    text("Assignment ace", "असाइनमेंट ऐस"),
                    text("Completed every teacher-assigned chapter.", "शिक्षक द्वारा दिए गए सभी अध्याय पूरे किए।"),
                )
            }
        }

        return badges.sortedBy { it.earnedAt }
    }

    private fun updatedStreak(profile: StudentProfile, now: Long): Int {
        val today = dayKey(now)
        if (profile.lastActiveDay.isBlank()) return 1
        if (profile.lastActiveDay == today) return profile.streakDays.coerceAtLeast(1)

        val previousDay = runCatching { LocalDate.parse(profile.lastActiveDay) }.getOrNull()
        val currentDay = runCatching { LocalDate.parse(today) }.getOrNull()
        return if (previousDay != null && currentDay != null && previousDay.plusDays(1) == currentDay) {
            profile.streakDays + 1
        } else {
            1
        }
    }

    private fun eligibleTopics(book: StudyBook, profile: StudentProfile): List<StudyTopic> {
        return if (profile.assignedChapterNumbers.isEmpty()) {
            book.topics
        } else {
            book.topics.filter { it.chapterNumber in profile.assignedChapterNumbers }
        }
    }

    private fun eligibleMainTopics(book: StudyBook, profile: StudentProfile): List<StudyTopic> {
        return eligibleTopics(book, profile).filterNot(::isExerciseTopic)
    }

    private fun eligibleExerciseTopics(book: StudyBook, profile: StudentProfile): List<StudyTopic> {
        return eligibleTopics(book, profile).filter(::isExerciseTopic)
    }

    private fun isWeak(progress: TopicProgress?): Boolean {
        progress ?: return false
        return !progress.mastered || progress.wrongAnswers >= 2 || progress.explanationRepeats >= 2
    }

    private fun isExerciseTopic(topic: StudyTopic): Boolean {
        return topic.id.startsWith("rs_ex_") ||
            topic.sourceLessonId.startsWith("rs_exercise_") ||
            topic.tags.any { it.english.equals("Exercise Path", ignoreCase = true) }
    }

    private fun starsForTopics(
        topics: List<StudyTopic>,
        profile: StudentProfile,
    ): Int {
        return topics.sumOf { topic -> profile.topicProgress[topic.id]?.starsEarned ?: 0 }
    }

    private fun attemptSummaryText(
        topic: StudyTopic,
        progress: TopicProgress,
    ): LocalizedText {
        val prompt = progress.firstAttemptQuestionPrompt ?: topic.questions.firstOrNull()?.prompt
        return if (prompt == null) {
            topic.subtopicTitle
        } else {
            text(
                english = "${topic.subtopicTitle.english} -> ${prompt.english}",
                hindi = "${topic.subtopicTitle.hindi} -> ${prompt.hindi}",
            )
        }
    }

    private fun dayKey(now: Long): String {
        return Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }

    private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
}
