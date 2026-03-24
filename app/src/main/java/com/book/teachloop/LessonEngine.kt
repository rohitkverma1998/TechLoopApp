package com.book.teachloop

class LessonEngine(
    private val lessons: List<LessonTopic>,
) {
    var currentIndex: Int = 0
        private set

    var state: LearningState = LearningState.ASK_IF_KNOWN
        private set

    private var questionIndex: Int = 0
    private var explanationRepeats: Int = 0

    fun restore(snapshot: ProgressSnapshot) {
        currentIndex = snapshot.currentIndex.coerceIn(0, lessons.size)
        questionIndex = snapshot.questionIndex.coerceAtLeast(0)
        explanationRepeats = snapshot.explanationRepeats.coerceAtLeast(0)
        state = if (currentIndex >= lessons.size) LearningState.COMPLETED else snapshot.state
    }

    fun snapshot(): ProgressSnapshot {
        return ProgressSnapshot(
            currentIndex = currentIndex,
            state = state,
            questionIndex = questionIndex,
            explanationRepeats = explanationRepeats,
        )
    }

    fun totalLessons(): Int = lessons.size

    fun currentLesson(): LessonTopic? = lessons.getOrNull(currentIndex)

    fun currentQuestion(): QuizQuestion? {
        val lesson = currentLesson() ?: return null
        if (lesson.questions.isEmpty()) return null
        return lesson.questions[questionIndex % lesson.questions.size]
    }

    fun answerKnowTopic(knowsTopic: Boolean) {
        if (currentLesson() == null) {
            state = LearningState.COMPLETED
            return
        }

        state = if (knowsTopic) {
            LearningState.TAKE_QUIZ
        } else {
            explanationRepeats += 1
            LearningState.EXPLAIN_TOPIC
        }
    }

    fun answerUnderstood(understood: Boolean) {
        if (currentLesson() == null) {
            state = LearningState.COMPLETED
            return
        }

        if (understood) {
            state = LearningState.TAKE_QUIZ
        } else {
            explanationRepeats += 1
            state = LearningState.EXPLAIN_TOPIC
        }
    }

    fun submitChoice(selectedIndex: Int): QuizResult {
        val question = currentQuestion()
            ?: return QuizResult(correct = false, message = "No question is available for this topic.")

        val isCorrect = question.type == QuestionType.MULTIPLE_CHOICE &&
            question.correctOptionIndex == selectedIndex
        return finishAnswer(isCorrect)
    }

    fun submitText(answer: String): QuizResult {
        val question = currentQuestion()
            ?: return QuizResult(correct = false, message = "No question is available for this topic.")

        val isCorrect = question.type == QuestionType.TEXT_INPUT &&
            question.acceptedAnswers.any { normalize(it) == normalize(answer) }
        return finishAnswer(isCorrect)
    }

    fun explanationToken(): String {
        val lessonId = currentLesson()?.id ?: "done"
        return "$lessonId-$explanationRepeats"
    }

    fun restart() {
        currentIndex = 0
        state = LearningState.ASK_IF_KNOWN
        questionIndex = 0
        explanationRepeats = 0
    }

    private fun finishAnswer(isCorrect: Boolean): QuizResult {
        val lesson = currentLesson()
            ?: return QuizResult(correct = false, message = "No lesson is active.")

        return if (isCorrect) {
            val completedTopic = lesson.topicTitle
            currentIndex += 1
            questionIndex = 0
            explanationRepeats = 0
            state = if (currentIndex >= lessons.size) {
                LearningState.COMPLETED
            } else {
                LearningState.ASK_IF_KNOWN
            }

            if (state == LearningState.COMPLETED) {
                QuizResult(
                    correct = true,
                    message = "Correct. You have completed the full offline learning path."
                )
            } else {
                QuizResult(
                    correct = true,
                    message = "Correct. \"$completedTopic\" is done. Moving to the next topic."
                )
            }
        } else {
            questionIndex = (questionIndex + 1) % lesson.questions.size
            state = LearningState.ASK_IF_KNOWN
            QuizResult(
                correct = false,
                message = "That answer is not correct yet. Let's revisit the topic and try again."
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
            .replace(Regex("\\s+"), " ")
    }
}
