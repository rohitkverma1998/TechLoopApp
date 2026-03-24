package com.book.teachloop

enum class LearningState {
    ASK_IF_KNOWN,
    EXPLAIN_TOPIC,
    TAKE_QUIZ,
    COMPLETED,
}

enum class QuestionType {
    MULTIPLE_CHOICE,
    TEXT_INPUT,
}

data class QuizQuestion(
    val id: String,
    val prompt: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int? = null,
    val acceptedAnswers: List<String> = emptyList(),
    val hint: String? = null,
)

data class LessonTopic(
    val id: String,
    val chapterNumber: Int,
    val chapterTitle: String,
    val topicTitle: String,
    val knowPrompt: String,
    val explanationTitle: String,
    val explanationParagraphs: List<String>,
    val examples: List<String>,
    val questions: List<QuizQuestion>,
)

data class ProgressSnapshot(
    val currentIndex: Int = 0,
    val state: LearningState = LearningState.ASK_IF_KNOWN,
    val questionIndex: Int = 0,
    val explanationRepeats: Int = 0,
)

data class QuizResult(
    val correct: Boolean,
    val message: String,
)
