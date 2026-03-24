package com.book.teachloop

enum class AppLanguage {
    ENGLISH,
    HINDI,
    BILINGUAL,
}

enum class Difficulty(val starValue: Int) {
    EASY(1),
    MEDIUM(2),
    HARD(3),
}

enum class StudyMode {
    MAIN_PATH,
    REVISION,
    WEAK_TOPICS,
}

enum class LearningState {
    DASHBOARD,
    ASK_IF_KNOWN,
    EXPLAIN_TOPIC,
    TAKE_QUIZ,
    SESSION_COMPLETE,
}

enum class QuestionType {
    MULTIPLE_CHOICE,
    TEXT_INPUT,
}

data class LocalizedText(
    val english: String,
    val hindi: String = english,
) {
    fun display(language: AppLanguage): String {
        return when (language) {
            AppLanguage.ENGLISH -> english
            AppLanguage.HINDI -> hindi
            AppLanguage.BILINGUAL -> {
                if (english == hindi) {
                    english
                } else {
                    "$english\n$hindi"
                }
            }
        }
    }
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

data class VisualBlock(
    val title: LocalizedText,
    val description: LocalizedText,
    val chips: List<LocalizedText> = emptyList(),
)

data class StudyTopic(
    val id: String,
    val sourceLessonId: String,
    val chapterNumber: Int,
    val chapterTitle: LocalizedText,
    val lessonTitle: LocalizedText,
    val subtopicTitle: LocalizedText,
    val knowPrompt: LocalizedText,
    val explanationTitle: LocalizedText,
    val explanationParagraphs: List<LocalizedText>,
    val examples: List<LocalizedText>,
    val visuals: List<VisualBlock>,
    val questions: List<QuizQuestion>,
    val questionSeedIndex: Int = 0,
)

data class StudyBook(
    val id: String,
    val subjectTitle: LocalizedText,
    val bookTitle: LocalizedText,
    val topics: List<StudyTopic>,
)

data class RenderedQuestion(
    val id: String,
    val prompt: LocalizedText,
    val type: QuestionType,
    val options: List<LocalizedText> = emptyList(),
    val correctOptionIndex: Int? = null,
    val acceptedAnswers: List<String> = emptyList(),
    val hint: LocalizedText? = null,
    val wrongReason: LocalizedText,
    val supportExample: LocalizedText,
)

data class TopicProgress(
    val topicId: String,
    val totalAttempts: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val explanationRepeats: Int = 0,
    val mastered: Boolean = false,
    val starsEarned: Int = 0,
    val reviewStage: Int = 0,
    val lastStudiedAt: Long = 0L,
    val nextRevisionAt: Long = 0L,
)

data class StudentProfile(
    val id: String,
    val name: String,
    val totalStars: Int = 0,
    val topicProgress: Map<String, TopicProgress> = emptyMap(),
)

data class SessionSnapshot(
    val bookId: String = LessonRepository.BOOK_ID,
    val mode: StudyMode? = null,
    val queueTopicIds: List<String> = emptyList(),
    val queueIndex: Int = 0,
    val state: LearningState = LearningState.DASHBOARD,
    val questionIndex: Int = 0,
    val explanationRepeats: Int = 0,
)

data class AppSnapshot(
    val selectedBookId: String = LessonRepository.BOOK_ID,
    val selectedProfileId: String = DEFAULT_PROFILE_ID,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val difficulty: Difficulty = Difficulty.EASY,
    val profiles: List<StudentProfile> = listOf(defaultProfile()),
    val session: SessionSnapshot = SessionSnapshot(),
) {
    companion object {
        const val DEFAULT_PROFILE_ID = "student_1"

        fun defaultProfile(): StudentProfile {
            return StudentProfile(
                id = DEFAULT_PROFILE_ID,
                name = "Student 1",
            )
        }
    }
}

data class QuizResult(
    val correct: Boolean,
    val message: LocalizedText,
    val wrongReason: LocalizedText? = null,
    val supportExample: LocalizedText? = null,
)

data class ReportSummary(
    val masteredTopics: Int,
    val totalTopics: Int,
    val dueRevisionTopics: Int,
    val weakTopics: Int,
    val supportHeavyTopics: Int,
    val totalStars: Int,
    val focusTopics: List<LocalizedText>,
)

fun text(english: String, hindi: String = english): LocalizedText {
    return LocalizedText(english = english, hindi = hindi)
}
