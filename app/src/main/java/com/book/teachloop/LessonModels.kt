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

enum class NarrationPace(val speechRate: Float) {
    NORMAL(1.0f),
    SLOW(0.82f),
}

enum class MistakeType {
    GENERAL,
    PLACE_VALUE,
    UNIT_CONVERSION,
    READING,
    CONCEPT_CONFUSION,
    FRACTION_COMPARE,
    ANGLE_TURN,
    OPERATION_LINK,
    PATTERN_RULE,
    MEASUREMENT_ESTIMATE,
    TIME_READING,
    DIRECTION,
    DATA_SCALE,
}

enum class BadgeType {
    FIRST_MASTERED,
    BRAVE_RETRY,
    REVISION_RANGER,
    STREAK_KEEPER,
    CHAPTER_CHAMP,
    ASSIGNMENT_ACE,
}

enum class VisualKind {
    INFO_CARD,
    TABLE,
    NUMBER_LINE,
    GRID,
    CLOCK,
    COMPASS,
    PICTOGRAPH,
    STEP_FLOW,
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
    val prompt: LocalizedText,
    val type: QuestionType,
    val options: List<LocalizedText> = emptyList(),
    val correctOptionIndex: Int? = null,
    val acceptedAnswers: List<String> = emptyList(),
    val hint: LocalizedText? = null,
    val wrongReason: LocalizedText? = null,
    val supportExample: LocalizedText? = null,
    val mistakeType: MistakeType = MistakeType.GENERAL,
    val reteachTitle: LocalizedText? = null,
    val reteachParagraphs: List<LocalizedText> = emptyList(),
)

data class LessonTopic(
    val id: String,
    val chapterNumber: Int,
    val chapterTitle: LocalizedText,
    val topicTitle: LocalizedText,
    val explanationParagraphs: List<LocalizedText>,
    val examples: List<LocalizedText>,
    val questions: List<QuizQuestion>,
)

data class VisualBlock(
    val title: LocalizedText,
    val description: LocalizedText,
    val kind: VisualKind = VisualKind.INFO_CARD,
    val chips: List<LocalizedText> = emptyList(),
    val rows: List<List<LocalizedText>> = emptyList(),
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
    val tags: List<LocalizedText> = emptyList(),
    val mistakeFocus: MistakeType = MistakeType.GENERAL,
)

data class StudyBook(
    val id: String,
    val subjectTitle: LocalizedText,
    val bookTitle: LocalizedText,
    val teacherNote: LocalizedText = text(
        english = "Use short practice sessions and spaced revision.",
        hindi = "छोटे अभ्यास सत्र और अंतराल वाली पुनरावृत्ति का उपयोग करें।",
    ),
    val topics: List<StudyTopic>,
)

data class SubjectPackCatalogItem(
    val id: String,
    val title: LocalizedText,
    val assetPath: String? = null,
)

data class RenderedQuestion(
    val id: String,
    val prompt: LocalizedText,
    val type: QuestionType,
    val options: List<LocalizedText> = emptyList(),
    val correctOptionIndex: Int? = null,
    val acceptedAnswers: List<String> = emptyList(),
    val solutionAnswer: LocalizedText = text(""),
    val hint: LocalizedText? = null,
    val wrongReason: LocalizedText,
    val supportExample: LocalizedText,
    val mistakeType: MistakeType,
    val reteachTitle: LocalizedText? = null,
    val reteachParagraphs: List<LocalizedText> = emptyList(),
)

data class BadgeAward(
    val type: BadgeType,
    val title: LocalizedText,
    val reason: LocalizedText,
    val earnedAt: Long,
)

data class TopicProgress(
    val topicId: String,
    val totalAttempts: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val firstAttemptCorrect: Boolean? = null,
    val firstAttemptQuestionPrompt: LocalizedText? = null,
    val explanationRepeats: Int = 0,
    val mastered: Boolean = false,
    val starsEarned: Int = 0,
    val reviewStage: Int = 0,
    val lastStudiedAt: Long = 0L,
    val nextRevisionAt: Long = 0L,
    val timeSpentMillis: Long = 0L,
    val lastMistakeType: MistakeType? = null,
    val mistakeCounts: Map<String, Int> = emptyMap(),
)

data class StudentProfile(
    val id: String,
    val name: String,
    val totalStars: Int = 0,
    val topicProgress: Map<String, TopicProgress> = emptyMap(),
    val badges: List<BadgeAward> = emptyList(),
    val chapterTrophies: List<Int> = emptyList(),
    val assignedChapterNumbers: List<Int> = emptyList(),
    val streakDays: Int = 0,
    val lastActiveDay: String = "",
    val revisionRewardCount: Int = 0,
)

data class SessionSnapshot(
    val bookId: String = LessonRepository.BOOK_ID,
    val mode: StudyMode? = null,
    val queueTopicIds: List<String> = emptyList(),
    val queueIndex: Int = 0,
    val state: LearningState = LearningState.DASHBOARD,
    val questionIndex: Int = 0,
    val explanationRepeats: Int = 0,
    val currentTopicStartedAt: Long = 0L,
    val lastMistakeType: MistakeType? = null,
)

data class AppSnapshot(
    val selectedBookId: String = LessonRepository.BOOK_ID,
    val selectedProfileId: String = DEFAULT_PROFILE_ID,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val difficulty: Difficulty = Difficulty.EASY,
    val narrationPace: NarrationPace = NarrationPace.NORMAL,
    val profiles: List<StudentProfile> = listOf(defaultProfile()),
    val teacherPin: String = "",
    val teacherModeUnlocked: Boolean = false,
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
    val mistakeType: MistakeType? = null,
    val reteachTitle: LocalizedText? = null,
    val reteachParagraphs: List<LocalizedText> = emptyList(),
)

data class ChapterMastery(
    val chapterNumber: Int,
    val chapterTitle: LocalizedText,
    val masteredTopics: Int,
    val totalTopics: Int,
)

data class MistakeBreakdown(
    val type: MistakeType,
    val count: Int,
)

data class ChartPoint(
    val label: LocalizedText,
    val value: Int,
    val maxValue: Int,
)

data class ReportSummary(
    val masteredTopics: Int,
    val totalTopics: Int,
    val dueRevisionTopics: Int,
    val weakTopics: Int,
    val supportHeavyTopics: Int,
    val totalStars: Int,
    val firstAttemptCorrectTopics: List<LocalizedText>,
    val firstAttemptWrongTopics: List<LocalizedText>,
    val legacyTrackedTopics: List<LocalizedText>,
    val focusTopics: List<LocalizedText>,
    val weakTopicTitles: List<LocalizedText>,
    val chartPoints: List<ChartPoint>,
    val chapterMastery: List<ChapterMastery>,
    val topMistakes: List<MistakeBreakdown>,
    val totalTimeMinutes: Int,
    val streakDays: Int,
    val badges: List<BadgeAward>,
    val chapterTrophies: List<Int>,
    val revisionRewardCount: Int,
)

fun text(english: String, hindi: String = english): LocalizedText {
    return LocalizedText(english = english, hindi = hindi)
}
