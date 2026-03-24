package com.book.teachloop

import kotlin.math.max

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
            snapshot.copy(
                bookId = book.id,
                queueTopicIds = validQueue,
                queueIndex = snapshot.queueIndex.coerceIn(0, validQueue.lastIndex),
            )
        }
    }

    fun snapshot(): SessionSnapshot = session

    fun hasActiveSession(): Boolean {
        return session.state != LearningState.DASHBOARD
    }

    fun startSession(mode: StudyMode, topicIds: List<String>) {
        if (topicIds.isEmpty()) {
            session = SessionSnapshot(bookId = book.id)
            return
        }

        session = SessionSnapshot(
            bookId = book.id,
            mode = mode,
            queueTopicIds = topicIds,
            queueIndex = 0,
            state = LearningState.ASK_IF_KNOWN,
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

    fun currentExplanationRepeats(): Int = session.explanationRepeats

    fun answerKnowTopic(knowsTopic: Boolean) {
        val topic = currentTopic() ?: run {
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
        val topic = currentTopic() ?: run {
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

    fun currentQuestion(difficulty: Difficulty): RenderedQuestion? {
        val topic = currentTopic() ?: return null
        if (topic.questions.isEmpty()) return null

        val baseIndex = (topic.questionSeedIndex + session.questionIndex) % topic.questions.size
        val baseQuestion = topic.questions[baseIndex]
        val supportExample = topic.examples.firstOrNull() ?: text(
            english = topic.explanationParagraphs.first().english,
            hindi = topic.explanationParagraphs.first().hindi,
        )

        val wrongReason = text(
            english = "This answer does not match the key idea in ${topic.subtopicTitle.english.lowercase()}.",
            hindi = "${topic.subtopicTitle.hindi.lowercase()} का विचार इस उत्तर से मेल नहीं खाता।",
        )

        val correctText = when (baseQuestion.type) {
            QuestionType.MULTIPLE_CHOICE -> {
                baseQuestion.options[baseQuestion.correctOptionIndex ?: 0]
            }

            QuestionType.TEXT_INPUT -> {
                baseQuestion.acceptedAnswers.firstOrNull().orEmpty()
            }
        }

        return when (difficulty) {
            Difficulty.EASY -> RenderedQuestion(
                id = "${baseQuestion.id}_easy",
                prompt = text(baseQuestion.prompt, baseQuestion.prompt),
                type = baseQuestion.type,
                options = baseQuestion.options.map(::text),
                correctOptionIndex = baseQuestion.correctOptionIndex,
                acceptedAnswers = baseQuestion.acceptedAnswers,
                hint = baseQuestion.hint?.let { text(it, it) },
                wrongReason = wrongReason,
                supportExample = supportExample,
            )

            Difficulty.MEDIUM -> RenderedQuestion(
                id = "${baseQuestion.id}_medium",
                prompt = text(
                    english = "${baseQuestion.prompt} Solve it carefully without depending only on the hint.",
                    hindi = "${baseQuestion.prompt} इसे ध्यान से हल कीजिए, केवल संकेत पर निर्भर मत रहिए।",
                ),
                type = baseQuestion.type,
                options = baseQuestion.options.map(::text),
                correctOptionIndex = baseQuestion.correctOptionIndex,
                acceptedAnswers = baseQuestion.acceptedAnswers,
                hint = text(
                    english = "Use the example from the lesson and think step by step.",
                    hindi = "पाठ के उदाहरण का उपयोग कीजिए और कदम-दर-कदम सोचिए।",
                ),
                wrongReason = wrongReason,
                supportExample = supportExample,
            )

            Difficulty.HARD -> {
                val hardPrompt = when (baseQuestion.type) {
                    QuestionType.MULTIPLE_CHOICE -> {
                        text(
                            english = "${baseQuestion.prompt} Type the correct answer instead of picking an option.",
                            hindi = "${baseQuestion.prompt} विकल्प चुनने के बजाय सही उत्तर लिखिए।",
                        )
                    }

                    QuestionType.TEXT_INPUT -> {
                        text(
                            english = "${baseQuestion.prompt} Write the final answer without using the hint.",
                            hindi = "${baseQuestion.prompt} संकेत के बिना अंतिम उत्तर लिखिए।",
                        )
                    }
                }

                RenderedQuestion(
                    id = "${baseQuestion.id}_hard",
                    prompt = hardPrompt,
                    type = QuestionType.TEXT_INPUT,
                    acceptedAnswers = listOf(correctText),
                    hint = null,
                    wrongReason = wrongReason,
                    supportExample = supportExample,
                )
            }
        }
    }

    fun submitChoice(selectedIndex: Int, difficulty: Difficulty): QuizResult {
        val question = currentQuestion(difficulty)
            ?: return QuizResult(
                correct = false,
                message = text("No question is available for this topic."),
            )

        val isCorrect = question.type == QuestionType.MULTIPLE_CHOICE &&
            question.correctOptionIndex == selectedIndex
        return finishAnswer(isCorrect, question)
    }

    fun submitText(answer: String, difficulty: Difficulty): QuizResult {
        val question = currentQuestion(difficulty)
            ?: return QuizResult(
                correct = false,
                message = text("No question is available for this topic."),
            )

        val isCorrect = question.acceptedAnswers.any { normalize(it) == normalize(answer) }
        return finishAnswer(isCorrect, question)
    }

    fun explanationToken(): String {
        val topicId = currentTopic()?.id ?: "dashboard"
        return "$topicId-${session.explanationRepeats}-${session.state}"
    }

    private fun finishAnswer(
        isCorrect: Boolean,
        question: RenderedQuestion,
    ): QuizResult {
        val topic = currentTopic()
            ?: return QuizResult(
                correct = false,
                message = text("No topic is active."),
            )

        return if (isCorrect) {
            val nextQueueIndex = session.queueIndex + 1
            session = if (nextQueueIndex >= session.queueTopicIds.size) {
                session.copy(
                    queueIndex = session.queueTopicIds.lastIndex,
                    questionIndex = 0,
                    explanationRepeats = 0,
                    state = LearningState.SESSION_COMPLETE,
                )
            } else {
                session.copy(
                    queueIndex = nextQueueIndex,
                    questionIndex = 0,
                    explanationRepeats = 0,
                    state = LearningState.ASK_IF_KNOWN,
                )
            }

            val message = if (session.state == LearningState.SESSION_COMPLETE) {
                text(
                    english = "Correct. This session is complete.",
                    hindi = "सही। यह अध्ययन सत्र पूरा हो गया।",
                )
            } else {
                text(
                    english = "Correct. ${topic.subtopicTitle.english} is done. Moving to the next step.",
                    hindi = "सही। ${topic.subtopicTitle.hindi} पूरा हुआ। अब अगले चरण पर चलते हैं।",
                )
            }

            QuizResult(
                correct = true,
                message = message,
            )
        } else {
            val nextQuestionIndex = if (topic.questions.isEmpty()) {
                0
            } else {
                (session.questionIndex + 1) % max(topic.questions.size, 1)
            }
            session = session.copy(
                questionIndex = nextQuestionIndex,
                explanationRepeats = 0,
                state = LearningState.ASK_IF_KNOWN,
            )

            QuizResult(
                correct = false,
                message = text(
                    english = "Not correct yet. Let us revisit the idea and try once more.",
                    hindi = "अभी सही नहीं हुआ। चलिए विचार को फिर से देखते हैं और एक बार और कोशिश करते हैं।",
                ),
                wrongReason = question.wrongReason,
                supportExample = question.supportExample,
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
}

object StudyPlanner {
    private val reviewDelays = listOf(1L, 3L, 7L)

    fun buildMainQueue(book: StudyBook, profile: StudentProfile): List<String> {
        return book.topics
            .filter { !(profile.topicProgress[it.id]?.mastered ?: false) }
            .map { it.id }
    }

    fun buildRevisionQueue(
        book: StudyBook,
        profile: StudentProfile,
        now: Long,
    ): List<String> {
        return book.topics
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
        return book.topics
            .filter { topic ->
                isWeak(profile.topicProgress[topic.id])
            }
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
        val masteredTopics = profile.topicProgress.values.count { it.mastered }
        val dueRevisionTopics = profile.topicProgress.values.count { it.mastered && it.nextRevisionAt in 1..now }
        val weakTopics = profile.topicProgress.values.count(::isWeak)
        val supportHeavyTopics = profile.topicProgress.values.count { it.explanationRepeats >= 2 }
        val focusTopics = book.topics
            .filter { topic ->
                val progress = profile.topicProgress[topic.id]
                ((progress?.wrongAnswers ?: 0) + (progress?.explanationRepeats ?: 0)) > 0
            }
            .sortedByDescending { topic ->
                val progress = profile.topicProgress[topic.id]
                (progress?.wrongAnswers ?: 0) + (progress?.explanationRepeats ?: 0)
            }
            .take(3)
            .map { topic -> topic.lessonTitle }

        return ReportSummary(
            masteredTopics = masteredTopics,
            totalTopics = book.topics.size,
            dueRevisionTopics = dueRevisionTopics,
            weakTopics = weakTopics,
            supportHeavyTopics = supportHeavyTopics,
            totalStars = profile.totalStars,
            focusTopics = focusTopics,
        )
    }

    fun updateProfileAfterAttempt(
        profile: StudentProfile,
        topic: StudyTopic,
        difficulty: Difficulty,
        mode: StudyMode?,
        correct: Boolean,
        explanationRepeats: Int,
        now: Long,
    ): StudentProfile {
        val existing = profile.topicProgress[topic.id] ?: TopicProgress(topicId = topic.id)
        val updatedProgress = if (correct) {
            val stage = when {
                !existing.mastered -> 0
                mode == StudyMode.REVISION -> (existing.reviewStage + 1).coerceAtMost(reviewDelays.lastIndex)
                else -> existing.reviewStage
            }

            existing.copy(
                totalAttempts = existing.totalAttempts + 1,
                correctAnswers = existing.correctAnswers + 1,
                explanationRepeats = existing.explanationRepeats + explanationRepeats,
                mastered = true,
                starsEarned = max(existing.starsEarned, difficulty.starValue),
                reviewStage = stage,
                lastStudiedAt = now,
                nextRevisionAt = now + reviewDelays[stage] * DAY_IN_MILLIS,
            )
        } else {
            existing.copy(
                totalAttempts = existing.totalAttempts + 1,
                wrongAnswers = existing.wrongAnswers + 1,
                explanationRepeats = existing.explanationRepeats + explanationRepeats,
                lastStudiedAt = now,
                reviewStage = 0,
                nextRevisionAt = now + DAY_IN_MILLIS,
            )
        }

        val updatedMap = profile.topicProgress.toMutableMap()
        updatedMap[topic.id] = updatedProgress
        val totalStars = updatedMap.values.sumOf { it.starsEarned }
        return profile.copy(
            topicProgress = updatedMap,
            totalStars = totalStars,
        )
    }

    private fun isWeak(progress: TopicProgress?): Boolean {
        progress ?: return false
        return !progress.mastered || progress.wrongAnswers >= 2 || progress.explanationRepeats >= 2
    }

    private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
}
