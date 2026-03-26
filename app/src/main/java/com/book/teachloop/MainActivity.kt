package com.book.teachloop

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.book.teachloop.databinding.ActivityMainBinding
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var progressStore: ProgressStore
    private lateinit var book: StudyBook
    private lateinit var engine: LessonEngine

    private var appState = AppSnapshot()
    private var bookCatalog: List<SubjectPackCatalogItem> = emptyList()
    private var bookAdapter: ArrayAdapter<String>? = null
    private var profileAdapter: ArrayAdapter<String>? = null
    private var isSyncingBookSpinner = false
    private var isSyncingProfileSpinner = false
    private var reportExpanded = false
    private var latestStatusMessage: String? = null
    private var latestQuizResult: QuizResult? = null
    private var latestIncorrectQuestion: RenderedQuestion? = null
    private var solutionPreviewActive = false
    private var currentSpeechSentences: List<String> = emptyList()
    private var currentSpeechIndex = 0
    private var revealedSentenceCount = 0
    private var activeChalkSentenceIndex = -1
    private var activeChalkCharacterCount = 0
    private var lastAutoScrollSentenceIndex = -1
    private var lastAutoScrollCharacterCount = -1
    private var speakSequenceActive = false
    private var speechPaused = false
    private var currentExplanationBoardToken: String? = null
    private var baseScrollBottomPadding = 0
    private var baseDecisionBottomMargin = 0
    private val chalkHandler = Handler(Looper.getMainLooper())
    private var chalkWriteRunnable: Runnable? = null

    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var ttsInitializing = false
    private var lastSpokenToken: String? = null
    private var ttsIssueMessage: String? = null
    private var englishSpeechProfile: SpeechProfile? = null
    private var hindiSpeechProfile: SpeechProfile? = null
    private var activeSpeechProfileKey: String? = null

    private data class SpeechProfile(
        val locale: Locale,
        val voice: Voice? = null,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        baseScrollBottomPadding = binding.contentScrollView.paddingBottom
        baseDecisionBottomMargin =
            (binding.decisionContainer.layoutParams as? LinearLayout.LayoutParams)?.bottomMargin ?: 0

        progressStore = ProgressStore(this)
        bookCatalog = LessonRepository.catalog(this)
        appState = normalize(progressStore.load(), bookCatalog)
        book = LessonRepository.book(this, appState.selectedBookId)
        engine = LessonEngine(book)
        engine.restore(appState.session)

        setupListeners()
        initializeTextToSpeech()
        render()
    }

    override fun onInit(status: Int) {
        ttsInitializing = false
        Log.d(TAG, "TTS onInit status=$status")
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TTS defaultEngine=${textToSpeech?.defaultEngine}")
            ttsReady = configureSpeechLanguage()
            if (ttsReady) {
                if (latestStatusMessage == TTS_LOADING_MESSAGE || latestStatusMessage == ttsIssueMessage) {
                    latestStatusMessage = null
                }
                ttsIssueMessage = null
                maybeSpeakExplanation()
            } else {
                latestStatusMessage = ttsIssueMessage ?: TTS_UNAVAILABLE_MESSAGE
                renderStatus()
            }
        } else {
            ttsIssueMessage = TTS_UNAVAILABLE_MESSAGE
            latestStatusMessage = ttsIssueMessage
            renderStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!ttsReady && !ttsInitializing) {
            initializeTextToSpeech(forceRestart = textToSpeech != null)
        }
    }

    override fun onDestroy() {
        stopChalkWriting(completeCurrentLine = false)
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    private fun setupListeners() {
        binding.bookSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                if (isSyncingBookSpinner) return
                val selectedBook = bookCatalog.getOrNull(position) ?: return
                if (selectedBook.id == appState.selectedBookId) return
                switchBook(selectedBook.id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.profileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                if (isSyncingProfileSpinner) return
                val selectedProfile = appState.profiles.getOrNull(position) ?: return
                if (selectedProfile.id == appState.selectedProfileId) return
                appState = appState.copy(
                    selectedProfileId = selectedProfile.id,
                    session = SessionSnapshot(bookId = book.id),
                )
                engine.finishSession()
                latestStatusMessage = ui(
                    "Switched to ${selectedProfile.name}.",
                    "${selectedProfile.name} à¤ªà¥à¤°à¥‹à¤«à¤¼à¤¾à¤‡à¤² à¤šà¥à¤¨à¥€ à¤—à¤ˆà¥¤",
                )
                render()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.addProfileButton.setOnClickListener {
            showAddProfileDialog()
        }

        binding.languageToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val updatedLanguage = when (checkedId) {
                binding.languageHindiButton.id -> AppLanguage.HINDI
                binding.languageBilingualButton.id -> AppLanguage.BILINGUAL
                else -> AppLanguage.ENGLISH
            }
            if (updatedLanguage != appState.language) {
                appState = appState.copy(language = updatedLanguage)
                textToSpeech?.stop()
                lastSpokenToken = null
                activeSpeechProfileKey = null
                if (textToSpeech != null && !ttsInitializing) {
                    ttsReady = configureSpeechLanguage()
                }
                latestStatusMessage = if (ttsReady || textToSpeech == null) {
                    ui("Language updated.", "à¤­à¤¾à¤·à¤¾ à¤¬à¤¦à¤² à¤¦à¥€ à¤—à¤ˆà¥¤")
                } else {
                    ttsIssueMessage ?: TTS_UNAVAILABLE_MESSAGE
                }
                render()
            }
        }
        binding.paceToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val updatedPace = when (checkedId) {
                binding.paceSlowButton.id -> NarrationPace.SLOW
                else -> NarrationPace.NORMAL
            }
            if (updatedPace != appState.narrationPace) {
                appState = appState.copy(narrationPace = updatedPace)
                textToSpeech?.setSpeechRate(updatedPace.speechRate)
                latestStatusMessage = ui("Voice pace updated.", "à¤†à¤µà¤¾à¤œà¤¼ à¤•à¥€ à¤—à¤¤à¤¿ à¤¬à¤¦à¤²à¥€ à¤—à¤ˆà¥¤")
                render()
            }
        }

        binding.startLearningButton.setOnClickListener { startMode(StudyMode.MAIN_PATH) }
        binding.exercisePathButton.setOnClickListener { startMode(StudyMode.EXERCISE_PATH) }
        binding.revisionButton.setOnClickListener { startMode(StudyMode.REVISION) }
        binding.weakTopicsButton.setOnClickListener { startMode(StudyMode.WEAK_TOPICS) }

        binding.reportButton.setOnClickListener {
            reportExpanded = !reportExpanded
            render()
        }

        binding.resetProgressButton.setOnClickListener {
            resetSelectedProfile()
        }

        binding.positiveButton.setOnClickListener {
            when (engine.session.state) {
                LearningState.ASK_IF_KNOWN -> {
                    engine.answerKnowTopic(knowsTopic = true)
                    latestStatusMessage = ui(
                        "Great. Let us test this step.",
                        "à¤…à¤šà¥à¤›à¤¾à¥¤ à¤…à¤¬ à¤‡à¤¸ à¤šà¤°à¤£ à¤•à¥€ à¤œà¤¾à¤à¤š à¤•à¤°à¤¤à¥‡ à¤¹à¥ˆà¤‚à¥¤",
                    )
                }

                LearningState.EXPLAIN_TOPIC -> {
                    engine.answerUnderstood(understood = true)
                    if (solutionPreviewActive) {
                        solutionPreviewActive = false
                        latestQuizResult = null
                        latestIncorrectQuestion = null
                        latestStatusMessage = ui(
                            "Good. Now try the question again.",
                            "अच्छा। अब प्रश्न को फिर से हल कीजिए।",
                        )
                    } else {
                        latestStatusMessage = ui(
                            "Nice. Let us check the idea with a question.",
                            "à¤…à¤šà¥à¤›à¤¾à¥¤ à¤…à¤¬ à¤ªà¥à¤°à¤¶à¥à¤¨ à¤¸à¥‡ à¤‡à¤¸ à¤µà¤¿à¤šà¤¾à¤° à¤•à¥€ à¤œà¤¾à¤à¤š à¤•à¤°à¤¤à¥‡ à¤¹à¥ˆà¤‚à¥¤",
                        )
                    }
                }

                else -> Unit
            }
            render()
        }

        binding.negativeButton.setOnClickListener {
            when (engine.session.state) {
                LearningState.ASK_IF_KNOWN -> {
                    engine.answerKnowTopic(knowsTopic = false)
                    latestStatusMessage = ui(
                        "No problem. I will teach the idea first.",
                        "à¤•à¥‹à¤ˆ à¤¬à¤¾à¤¤ à¤¨à¤¹à¥€à¤‚à¥¤ à¤ªà¤¹à¤²à¥‡ à¤®à¥ˆà¤‚ à¤¯à¤¹ à¤µà¤¿à¤šà¤¾à¤° à¤¸à¤®à¤à¤¾à¤¤à¤¾ à¤¹à¥‚à¤à¥¤",
                    )
                }

                LearningState.EXPLAIN_TOPIC -> {
                    engine.answerUnderstood(understood = false)
                    latestStatusMessage = if (solutionPreviewActive) {
                        ui(
                            "Let us explain the solution once more.",
                            "चलो, समाधान को एक बार फिर समझते हैं।",
                        )
                    } else {
                        ui(
                            "Let us go through the explanation once more.",
                            "à¤šà¤²à¥‹, à¤‡à¤¸à¥‡ à¤à¤• à¤¬à¤¾à¤° à¤«à¤¿à¤° à¤¸à¤®à¤à¤¤à¥‡ à¤¹à¥ˆà¤‚à¥¤",
                        )
                    }
                }

                else -> Unit
            }
            render()
        }

        binding.playVoiceButton.setOnClickListener { speakCurrentExplanation() }
        binding.pauseVoiceButton.setOnClickListener { pauseSpeech() }
        binding.rewindVoiceButton.setOnClickListener { rewindSpeech() }
        binding.replaySentenceButton.setOnClickListener { replayCurrentSentence() }
        binding.openVoiceSettingsButton.setOnClickListener { openVoiceSettings() }
        binding.submitAnswerButton.setOnClickListener { submitAnswer() }
        binding.feedbackActionButton.setOnClickListener { openQuestionSolution() }
        binding.teacherModeButton.setOnClickListener { handleTeacherModeTap() }
        binding.teacherAssignmentsButton.setOnClickListener { showAssignmentsDialog() }
        binding.teacherWeakTopicsButton.setOnClickListener { showWeakTopicsDialog() }
        binding.teacherExportButton.setOnClickListener { exportTeacherSummary() }
        binding.teacherLockButton.setOnClickListener {
            appState = appState.copy(teacherModeUnlocked = false)
            latestStatusMessage = ui("Teacher mode locked.", "à¤Ÿà¥€à¤šà¤° à¤®à¥‹à¤¡ à¤¬à¤‚à¤¦ à¤•à¤¿à¤¯à¤¾ à¤—à¤¯à¤¾à¥¤")
            render()
        }

        binding.restartButton.setOnClickListener {
            engine.finishSession()
            latestStatusMessage = ui(
                "Session closed. You are back on the dashboard.",
                "à¤¸à¤¤à¥à¤° à¤¬à¤‚à¤¦ à¤•à¤¿à¤¯à¤¾ à¤—à¤¯à¤¾à¥¤ à¤†à¤ª à¤¡à¥ˆà¤¶à¤¬à¥‹à¤°à¥à¤¡ à¤ªà¤° à¤µà¤¾à¤ªà¤¸ à¤¹à¥ˆà¤‚à¥¤",
            )
            render()
        }
    }

    private fun startMode(mode: StudyMode) {
        latestQuizResult = null
        latestIncorrectQuestion = null
        solutionPreviewActive = false
        val queue = when (mode) {
            StudyMode.MAIN_PATH -> StudyPlanner.buildMainQueue(book, selectedProfile())
            StudyMode.EXERCISE_PATH -> StudyPlanner.buildExerciseQueue(book, selectedProfile())
            StudyMode.REVISION -> StudyPlanner.buildRevisionQueue(book, selectedProfile(), System.currentTimeMillis())
            StudyMode.WEAK_TOPICS -> StudyPlanner.buildWeakTopicQueue(book, selectedProfile())
        }

        if (queue.isEmpty()) {
            latestStatusMessage = when (mode) {
                StudyMode.MAIN_PATH -> ui(
                    "Main path is complete for this child. Use revision or weak-topic practice next.",
                    "à¤‡à¤¸ à¤¬à¤šà¥à¤šà¥‡ à¤•à¥‡ à¤²à¤¿à¤ à¤®à¥à¤–à¥à¤¯ à¤ªà¤¥ à¤ªà¥‚à¤°à¤¾ à¤¹à¥‹ à¤šà¥à¤•à¤¾ à¤¹à¥ˆà¥¤ à¤…à¤¬ à¤ªà¥à¤¨à¤°à¤¾à¤µà¥ƒà¤¤à¥à¤¤à¤¿ à¤¯à¤¾ à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯ à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤•à¤°à¥‡à¤‚à¥¤",
                )

                StudyMode.EXERCISE_PATH -> ui(
                    "Exercise path is complete for this child.",
                    "à¤‡à¤¸ à¤¬à¤šà¥à¤šà¥‡ à¤•à¥‡ à¤²à¤¿à¤ à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤ªà¤¥ à¤ªà¥‚à¤°à¤¾ à¤¹à¥‹ à¤šà¥à¤•à¤¾ à¤¹à¥ˆà¥¤",
                )

                StudyMode.REVISION -> ui(
                    "No revision topic is due right now.",
                    "à¤…à¤­à¥€ à¤•à¥‹à¤ˆ à¤ªà¥à¤¨à¤°à¤¾à¤µà¥ƒà¤¤à¥à¤¤à¤¿ à¤µà¤¿à¤·à¤¯ à¤¶à¥‡à¤· à¤¨à¤¹à¥€à¤‚ à¤¹à¥ˆà¥¤",
                )

                StudyMode.WEAK_TOPICS -> ui(
                    "No weak topics are pending right now.",
                    "à¤…à¤­à¥€ à¤•à¥‹à¤ˆ à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯ à¤²à¤‚à¤¬à¤¿à¤¤ à¤¨à¤¹à¥€à¤‚ à¤¹à¥ˆà¥¤",
                )
            }
            render()
            return
        }

        reportExpanded = false
        lastSpokenToken = null
        engine.startSession(mode, queue)
        latestStatusMessage = when (mode) {
            StudyMode.MAIN_PATH -> ui(
                "Starting the next learning path.",
                "à¤…à¤—à¤²à¤¾ à¤¸à¥€à¤–à¤¨à¥‡ à¤•à¤¾ à¤ªà¤¥ à¤¶à¥à¤°à¥‚ à¤¹à¥‹ à¤°à¤¹à¤¾ à¤¹à¥ˆà¥¤",
            )

            StudyMode.EXERCISE_PATH -> ui(
                "Starting the exercise path.",
                "à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤ªà¤¥ à¤¶à¥à¤°à¥‚ à¤¹à¥‹ à¤°à¤¹à¤¾ à¤¹à¥ˆà¥¤",
            )

            StudyMode.REVISION -> ui(
                "Starting the revision path for due topics.",
                "à¤¦à¥‡à¤¯ à¤µà¤¿à¤·à¤¯à¥‹à¤‚ à¤•à¥€ à¤ªà¥à¤¨à¤°à¤¾à¤µà¥ƒà¤¤à¥à¤¤à¤¿ à¤¶à¥à¤°à¥‚ à¤¹à¥‹ à¤°à¤¹à¥€ à¤¹à¥ˆà¥¤",
            )

            StudyMode.WEAK_TOPICS -> ui(
                "Starting practice for weak topics.",
                "à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯à¥‹à¤‚ à¤•à¤¾ à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤¶à¥à¤°à¥‚ à¤¹à¥‹ à¤°à¤¹à¤¾ à¤¹à¥ˆà¥¤",
            )
        }
        render()
    }

    private fun resetSelectedProfile() {
        val currentProfile = selectedProfile()
        val resetProfile = currentProfile.copy(
            totalStars = 0,
            topicProgress = emptyMap(),
        )
        replaceSelectedProfile(resetProfile)
        engine.finishSession()
        reportExpanded = false
        lastSpokenToken = null
        latestQuizResult = null
        latestIncorrectQuestion = null
        solutionPreviewActive = false
        latestStatusMessage = ui(
            "Progress reset for ${resetProfile.name}.",
            "${resetProfile.name} à¤•à¥€ à¤ªà¥à¤°à¤—à¤¤à¤¿ à¤°à¥€à¤¸à¥‡à¤Ÿ à¤•à¤° à¤¦à¥€ à¤—à¤ˆà¥¤",
        )
        render()
    }

    private fun submitAnswer() {
        val topic = engine.currentTopic() ?: return
        val currentQuestion = engine.currentQuestion(Difficulty.EASY) ?: return
        val explanationRepeats = engine.currentExplanationRepeats()
        val previousProfile = selectedProfile()
        val now = System.currentTimeMillis()

        val result = when (currentQuestion.type) {
            QuestionType.MULTIPLE_CHOICE -> {
                val checkedId = binding.answerOptions.checkedRadioButtonId
                if (checkedId == View.NO_ID) {
                    latestStatusMessage = ui(
                        "Please choose one option before submitting.",
                        "à¤œà¤®à¤¾ à¤•à¤°à¤¨à¥‡ à¤¸à¥‡ à¤ªà¤¹à¤²à¥‡ à¤à¤• à¤µà¤¿à¤•à¤²à¥à¤ª à¤šà¥à¤¨à¤¿à¤à¥¤",
                    )
                    renderStatus()
                    return
                }

                val checkedView = findViewById<RadioButton>(checkedId)
                val selectedIndex = binding.answerOptions.indexOfChild(checkedView)
                engine.submitChoice(selectedIndex, Difficulty.EASY, now)
            }

            QuestionType.TEXT_INPUT -> {
                val answer = binding.answerInputEditText.text?.toString().orEmpty()
                if (answer.isBlank()) {
                    latestStatusMessage = ui(
                        "Please type an answer before submitting.",
                        "à¤œà¤®à¤¾ à¤•à¤°à¤¨à¥‡ à¤¸à¥‡ à¤ªà¤¹à¤²à¥‡ à¤‰à¤¤à¥à¤¤à¤° à¤²à¤¿à¤–à¤¿à¤à¥¤",
                    )
                    renderStatus()
                    return
                }
                engine.submitText(answer, Difficulty.EASY, now)
            }

        }

        val updatedProfile = StudyPlanner.updateProfileAfterAttempt(
            book = book,
            profile = previousProfile,
            topic = topic,
            questionPrompt = currentQuestion.prompt,
            difficulty = Difficulty.EASY,
            mode = engine.session.mode,
            correct = result.correct,
            explanationRepeats = explanationRepeats,
            mistakeType = result.mistakeType,
            timeSpentMillis = (now - engine.currentTopicStartedAt()).coerceAtLeast(0L),
            now = now,
        )
        replaceSelectedProfile(updatedProfile)

        val starsEarned = updatedProfile.totalStars - previousProfile.totalStars
        latestQuizResult = if (result.correct) null else result
        latestIncorrectQuestion = if (result.correct) null else currentQuestion
        solutionPreviewActive = false
        latestStatusMessage = buildStatusMessage(result, starsEarned)
        binding.answerInputEditText.text?.clear()
        render()
    }

    private fun buildStatusMessage(
        result: QuizResult,
        starsEarned: Int,
    ): String {
        val baseMessage = result.message.display(appState.language)
        if (result.correct) {
            return if (starsEarned > 0) {
                "$baseMessage\n${ui("Stars earned: $starsEarned", "à¤®à¤¿à¤²à¥‡ à¤¹à¥à¤ à¤¸à¤¿à¤¤à¤¾à¤°à¥‡: $starsEarned")}"
            } else {
                baseMessage
            }
        }

        val detailParts = listOfNotNull(
            result.wrongReason?.display(appState.language),
            result.supportExample?.display(appState.language)?.let {
                "${ui("Example", "à¤‰à¤¦à¤¾à¤¹à¤°à¤£")}: $it"
            },
        )
        return listOf(baseMessage, detailParts.joinToString("\n"))
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    private fun openQuestionSolution() {
        if (latestIncorrectQuestion == null || latestQuizResult == null) return

        solutionPreviewActive = true
        lastSpokenToken = null
        latestStatusMessage = ui(
            "Opening the step-by-step solution.",
            "चरण-दर-चरण समाधान खोला जा रहा है।",
        )

        if (engine.session.state == LearningState.ASK_IF_KNOWN) {
            engine.answerKnowTopic(knowsTopic = false)
        }
        render()
    }

    private fun render() {
        saveState()
        if (engine.session.state != LearningState.EXPLAIN_TOPIC) {
            textToSpeech?.stop()
            resetTeachingBoardState()
            lastSpokenToken = null
        }

        val report = StudyPlanner.buildReport(book, selectedProfile(), System.currentTimeMillis())

        renderHeader(report)
        renderDashboard(report)
        renderTeacherPanel(report)
        renderReport(report)
        renderSession()
        renderCompletion()
        renderStatus()
        maybeSpeakExplanation()
    }

    private fun renderHeader(report: ReportSummary) {
        binding.appTitleText.text = ui("TeachLoop", "à¤Ÿà¥€à¤šà¤²à¥‚à¤ª")
        binding.bookTitleText.text = book.bookTitle.display(appState.language)
        binding.bookLabelText.text = ui("Book pack", "à¤¬à¥à¤• à¤ªà¥ˆà¤•")
        binding.languageLabelText.text = ui("Language", "à¤­à¤¾à¤·à¤¾")
        binding.starsChipText.text = ui(
            "Stars ${selectedProfile().totalStars}",
            "à¤¸à¤¿à¤¤à¤¾à¤°à¥‡ ${selectedProfile().totalStars}",
        )
        binding.modeChipText.text = when (engine.session.mode) {
            StudyMode.MAIN_PATH -> ui("Main path", "à¤®à¥à¤–à¥à¤¯ à¤ªà¤¥")
            StudyMode.EXERCISE_PATH -> ui("Exercise path", "à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤ªà¤¥")
            StudyMode.REVISION -> ui("Revision mode", "à¤ªà¥à¤¨à¤°à¤¾à¤µà¥ƒà¤¤à¥à¤¤à¤¿")
            StudyMode.WEAK_TOPICS -> ui("Weak topics", "à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯")
            null -> ui(
                "Mastered ${report.masteredTopics}/${report.totalTopics}",
                "à¤¸à¥€à¤–à¥‡ à¤—à¤ ${report.masteredTopics}/${report.totalTopics}",
            )
        }

        binding.languageEnglishButton.text = "English"
        binding.languageHindiButton.text = "हिंदी"
        binding.languageBilingualButton.text = "Both"

        syncBookSpinner()
        syncProfileSpinner()
        binding.bookSpinner.isEnabled = !engine.hasActiveSession()
        binding.profileSpinner.isEnabled = !engine.hasActiveSession()
        binding.addProfileButton.isEnabled = !engine.hasActiveSession()

        val languageButtonId = when (appState.language) {
            AppLanguage.ENGLISH -> binding.languageEnglishButton.id
            AppLanguage.HINDI -> binding.languageHindiButton.id
            AppLanguage.BILINGUAL -> binding.languageBilingualButton.id
        }
        binding.languageToggle.check(languageButtonId)

    }

    private fun syncBookSpinner() {
        val bookTitles = bookCatalog.map { it.title.display(appState.language).replace('\n', ' ') }
        if (bookAdapter == null) {
            bookAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                bookTitles.toMutableList(),
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.bookSpinner.adapter = adapter
            }
        } else {
            bookAdapter?.clear()
            bookAdapter?.addAll(bookTitles)
            bookAdapter?.notifyDataSetChanged()
        }

        val selectedIndex = bookCatalog.indexOfFirst { it.id == appState.selectedBookId }
            .coerceAtLeast(0)
        if (binding.bookSpinner.selectedItemPosition != selectedIndex) {
            isSyncingBookSpinner = true
            binding.bookSpinner.setSelection(selectedIndex, false)
            isSyncingBookSpinner = false
        }
    }

    private fun syncProfileSpinner() {
        val profileNames = appState.profiles.map { it.name }
        if (profileAdapter == null) {
            profileAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                profileNames.toMutableList(),
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.profileSpinner.adapter = adapter
            }
        } else {
            profileAdapter?.clear()
            profileAdapter?.addAll(profileNames)
            profileAdapter?.notifyDataSetChanged()
        }

        val selectedIndex = appState.profiles.indexOfFirst { it.id == appState.selectedProfileId }
            .coerceAtLeast(0)
        if (binding.profileSpinner.selectedItemPosition != selectedIndex) {
            isSyncingProfileSpinner = true
            binding.profileSpinner.setSelection(selectedIndex, false)
            isSyncingProfileSpinner = false
        }
    }

    private fun renderDashboard(report: ReportSummary) {
        val mainQueueCount = StudyPlanner.buildMainQueue(book, selectedProfile()).size
        val exerciseQueueCount = StudyPlanner.buildExerciseQueue(book, selectedProfile()).size
        val revisionCount = StudyPlanner.buildRevisionQueue(book, selectedProfile(), System.currentTimeMillis()).size
        val weakCount = StudyPlanner.buildWeakTopicQueue(book, selectedProfile()).size

        val dashboardVisible = !engine.hasActiveSession()
        binding.dashboardCard.isVisible = dashboardVisible
        binding.dashboardTitleText.text = ui(
            "Choose the next learning mission",
            "à¤…à¤—à¤²à¤¾ à¤¸à¥€à¤–à¤¨à¥‡ à¤•à¤¾ à¤®à¤¿à¤¶à¤¨ à¤šà¥à¤¨à¤¿à¤",
        )
        binding.dashboardBodyText.text = listOf(
            ui(
                "Main path -> Stars ${report.mainStars} | Mastered ${report.masteredTopics}/${report.totalTopics}",
                "à¤®à¥à¤–à¥à¤¯ à¤ªà¤¥ -> à¤¸à¤¿à¤¤à¤¾à¤°à¥‡ ${report.mainStars} | à¤¸à¥€à¤–à¥‡ à¤—à¤ ${report.masteredTopics}/${report.totalTopics}",
            ),
            ui(
                "Exercise path -> Stars ${report.exerciseStars} | Mastered ${report.exerciseMasteredTopics}/${report.exerciseTotalTopics}",
                "à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤ªà¤¥ -> à¤¸à¤¿à¤¤à¤¾à¤°à¥‡ ${report.exerciseStars} | à¤¸à¥€à¤–à¥‡ à¤—à¤ ${report.exerciseMasteredTopics}/${report.exerciseTotalTopics}",
            ),
            ui(
                "$revisionCount revision topics are due, and $weakCount topics still need extra support.",
                "$revisionCount à¤ªà¥à¤¨à¤°à¤¾à¤µà¥ƒà¤¤à¥à¤¤à¤¿ à¤µà¤¿à¤·à¤¯ à¤¦à¥‡à¤¯ à¤¹à¥ˆà¤‚ à¤”à¤° $weakCount à¤µà¤¿à¤·à¤¯à¥‹à¤‚ à¤•à¥‹ à¤…à¤­à¥€ à¤…à¤¤à¤¿à¤°à¤¿à¤•à¥à¤¤ à¤¸à¤¹à¤¾à¤¯à¤¤à¤¾ à¤šà¤¾à¤¹à¤¿à¤à¥¤",
            ),
        ).joinToString("\n\n")

        binding.startLearningButton.text = if (mainQueueCount > 0) {
            ui(
                "Continue main path ($mainQueueCount steps)",
                "à¤®à¥à¤–à¥à¤¯ à¤ªà¤¥ à¤œà¤¾à¤°à¥€ à¤°à¤–à¥‡à¤‚ ($mainQueueCount à¤šà¤°à¤£)",
            )
        } else {
            ui("Main path complete", "à¤®à¥à¤–à¥à¤¯ à¤ªà¤¥ à¤ªà¥‚à¤°à¤¾")
        }
        binding.startLearningButton.isEnabled = mainQueueCount > 0

        binding.exercisePathButton.text = if (exerciseQueueCount > 0) {
            ui(
                "Continue exercise path ($exerciseQueueCount questions)",
                "à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤ªà¤¥ à¤œà¤¾à¤°à¥€ à¤°à¤–à¥‡à¤‚ ($exerciseQueueCount à¤ªà¥à¤°à¤¶à¥à¤¨)",
            )
        } else {
            ui("Exercise path complete", "à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤ªà¤¥ à¤ªà¥‚à¤°à¤¾")
        }
        binding.exercisePathButton.isEnabled = exerciseQueueCount > 0

        binding.revisionButton.text = ui(
            "Revision mode ($revisionCount due)",
            "à¤ªà¥à¤¨à¤°à¤¾à¤µà¥ƒà¤¤à¥à¤¤à¤¿ ($revisionCount à¤¦à¥‡à¤¯)",
        )
        binding.revisionButton.isEnabled = revisionCount > 0

        binding.weakTopicsButton.text = ui(
            "Practice weak topics ($weakCount)",
            "à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯ à¤…à¤­à¥à¤¯à¤¾à¤¸ ($weakCount)",
        )
        binding.weakTopicsButton.isEnabled = weakCount > 0
    }

    private fun renderTeacherPanel(report: ReportSummary) {
        val visible = !engine.hasActiveSession() && appState.teacherModeUnlocked
        binding.teacherCard.isVisible = visible
        if (!visible) return

        val assignments = selectedProfile().assignedChapterNumbers.sorted()
        val assignmentText = if (assignments.isEmpty()) {
            ui("No chapters assigned yet.", "à¤…à¤­à¥€ à¤•à¥‹à¤ˆ à¤…à¤§à¥à¤¯à¤¾à¤¯ à¤¸à¥Œà¤‚à¤ªà¤¾ à¤¨à¤¹à¥€à¤‚ à¤—à¤¯à¤¾ à¤¹à¥ˆà¥¤")
        } else {
            ui(
                "Assigned chapters: ${assignments.joinToString(", ")}",
                "à¤¸à¥Œà¤‚à¤ªà¥‡ à¤—à¤ à¤…à¤§à¥à¤¯à¤¾à¤¯: ${assignments.joinToString(", ")}",
            )
        }

        binding.teacherTitleText.text = ui("Teacher panel", "à¤Ÿà¥€à¤šà¤° à¤ªà¥ˆà¤¨à¤²")
        binding.teacherSummaryText.text = listOf(
            ui(
                "${selectedProfile().name} is studying ${book.bookTitle.display(appState.language)}.",
                "${selectedProfile().name}, ${book.bookTitle.display(appState.language)} à¤ªà¤¢à¤¼ à¤°à¤¹à¤¾/à¤°à¤¹à¥€ à¤¹à¥ˆà¥¤",
            ),
            assignmentText,
            ui(
                "Exercise path: ${report.exerciseMasteredTopics}/${report.exerciseTotalTopics} | Exercise stars: ${report.exerciseStars}",
                "à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤ªà¤¥: ${report.exerciseMasteredTopics}/${report.exerciseTotalTopics} | à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤¸à¤¿à¤¤à¤¾à¤°à¥‡: ${report.exerciseStars}",
            ),
            ui(
                "Weak topics: ${report.weakTopics} | Revision due: ${report.dueRevisionTopics}",
                "à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯: ${report.weakTopics} | à¤¦à¥‡à¤¯ à¤ªà¥à¤¨à¤°à¤¾à¤µà¥ƒà¤¤à¥à¤¤à¤¿: ${report.dueRevisionTopics}",
            ),
        ).joinToString("\n\n")
        binding.teacherAssignmentsButton.text = ui("Assign chapters", "à¤…à¤§à¥à¤¯à¤¾à¤¯ à¤¸à¥Œà¤‚à¤ªà¥‡à¤‚")
        binding.teacherWeakTopicsButton.text = ui("View weak topics", "à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯ à¤¦à¥‡à¤–à¥‡à¤‚")
        binding.teacherExportButton.text = ui("Export summary", "à¤¸à¤¾à¤°à¤¾à¤‚à¤¶ à¤­à¥‡à¤œà¥‡à¤‚")
        binding.resetProgressButton.text = ui("Reset child progress", "à¤¬à¤šà¥à¤šà¥‡ à¤•à¥€ à¤ªà¥à¤°à¤—à¤¤à¤¿ à¤°à¥€à¤¸à¥‡à¤Ÿ à¤•à¤°à¥‡à¤‚")
        binding.reportButton.text = if (reportExpanded) {
            ui("Hide report", "à¤°à¤¿à¤ªà¥‹à¤°à¥à¤Ÿ à¤›à¥à¤ªà¤¾à¤à¤")
        } else {
            ui("Show report", "à¤°à¤¿à¤ªà¥‹à¤°à¥à¤Ÿ à¤¦à¤¿à¤–à¤¾à¤à¤")
        }
        binding.teacherLockButton.text = ui("Lock teacher mode", "à¤Ÿà¥€à¤šà¤° à¤®à¥‹à¤¡ à¤¬à¤‚à¤¦ à¤•à¤°à¥‡à¤‚")
    }

    private fun renderReport(report: ReportSummary) {
        binding.reportCard.isVisible = !engine.hasActiveSession() &&
            appState.teacherModeUnlocked &&
            reportExpanded
        binding.reportTitleText.text = ui("Parent and teacher report", "à¤…à¤­à¤¿à¤­à¤¾à¤µà¤• à¤”à¤° à¤¶à¤¿à¤•à¥à¤·à¤• à¤°à¤¿à¤ªà¥‹à¤°à¥à¤Ÿ")
        val focusText = if (report.focusTopics.isEmpty()) {
            ui("No high-need topics yet.", "à¤…à¤­à¥€ à¤•à¥‹à¤ˆ à¤µà¤¿à¤¶à¥‡à¤· à¤•à¤ à¤¿à¤¨ à¤µà¤¿à¤·à¤¯ à¤¨à¤¹à¥€à¤‚ à¤¹à¥ˆà¥¤")
        } else {
            report.focusTopics.joinToString("\n") { "- ${it.display(appState.language)}" }
        }
        val firstTryCorrectText = reportListText(
            items = report.firstAttemptCorrectTopics,
            emptyEnglish = "No topic has been solved correctly on the first try yet.",
            emptyHindi = "अभी तक कोई विषय पहली कोशिश में सही हल नहीं हुआ है।",
        )
        val firstTryWrongText = reportListText(
            items = report.firstAttemptWrongTopics,
            emptyEnglish = "No first-try mistakes yet.",
            emptyHindi = "अभी तक पहली कोशिश की कोई गलती नहीं है।",
        )
        val legacyTrackedText = reportListText(
            items = report.legacyTrackedTopics,
            emptyEnglish = "No older topics need migration notes.",
            emptyHindi = "कोई पुराना विषय माइग्रेशन नोट के लिए नहीं है।",
        )

        binding.reportBodyText.text = listOf(
            ui(
                "Mastered steps: ${report.masteredTopics}/${report.totalTopics}",
                "à¤¸à¥€à¤–à¥‡ à¤—à¤ à¤šà¤°à¤£: ${report.masteredTopics}/${report.totalTopics}",
            ),
            ui(
                "Due revisions: ${report.dueRevisionTopics}",
                "à¤¦à¥‡à¤¯ à¤ªà¥à¤¨à¤°à¤¾à¤µà¥ƒà¤¤à¥à¤¤à¤¿à¤¯à¤¾à¤: ${report.dueRevisionTopics}",
            ),
            ui("Weak topics: ${report.weakTopics}", "à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯: ${report.weakTopics}"),
            ui(
                "Topics needing repeated explanation: ${report.supportHeavyTopics}",
                "à¤¬à¤¾à¤°-à¤¬à¤¾à¤° à¤¸à¤®à¤à¤¾à¤¨à¥‡ à¤µà¤¾à¤²à¥‡ à¤µà¤¿à¤·à¤¯: ${report.supportHeavyTopics}",
            ),
            ui("Total stars: ${report.totalStars}", "à¤•à¥à¤² à¤¸à¤¿à¤¤à¤¾à¤°à¥‡: ${report.totalStars}"),
            ui(
                "Exercise path: ${report.exerciseMasteredTopics}/${report.exerciseTotalTopics} | Exercise stars: ${report.exerciseStars}",
                "à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤ªà¤¥: ${report.exerciseMasteredTopics}/${report.exerciseTotalTopics} | à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤¸à¤¿à¤¤à¤¾à¤°à¥‡: ${report.exerciseStars}",
            ),
            "${ui("First attempt correct", "पहली कोशिश में सही")}:\n$firstTryCorrectText",
            "${ui("First attempt wrong", "पहली कोशिश में गलत")}:\n$firstTryWrongText",
            "${ui("Older tracked topics", "पुराने ट्रैक किए गए विषय")}:\n${ui(
                "These were answered before first-try tracking was added, so the exact first attempt is unknown.",
                "इनका उत्तर पहली-कोशिश ट्रैकिंग जुड़ने से पहले दिया गया था, इसलिए पहली कोशिश का सटीक परिणाम उपलब्ध नहीं है।",
            )}\n$legacyTrackedText",
            "${ui("Focus topics", "à¤§à¥à¤¯à¤¾à¤¨ à¤¦à¥‡à¤¨à¥‡ à¤µà¤¾à¤²à¥‡ à¤µà¤¿à¤·à¤¯")}:\n$focusText",
        ).joinToString("\n\n")
        renderMetricBars(report)
        renderMasteryMap(report)
        renderMistakeBreakdown(report)
        renderBadgeSection(report)
    }

    private fun renderSession() {
        val topic = engine.currentTopic()
        val sessionActive = engine.hasActiveSession() && engine.session.state != LearningState.SESSION_COMPLETE
        binding.sessionGroup.isVisible = sessionActive
        binding.teacherPlaybackCard.isVisible = sessionActive && engine.session.state == LearningState.EXPLAIN_TOPIC
        if (!sessionActive || topic == null) {
            updateContentBottomInset(playbackVisible = false)
            updateDecisionContainerInset(playbackVisible = false)
            return
        }

        binding.progressBar.max = engine.totalQueuedTopics()
        binding.progressBar.progress = engine.currentTopicPosition().coerceAtLeast(1)
        binding.progressChipText.text = ui(
            "Step ${engine.currentTopicPosition()} of ${engine.totalQueuedTopics()}",
            "à¤šà¤°à¤£ ${engine.currentTopicPosition()} / ${engine.totalQueuedTopics()}",
        )
        binding.chapterLabelText.text = ui("Chapter ${topic.chapterNumber}", "à¤…à¤§à¥à¤¯à¤¾à¤¯ ${topic.chapterNumber}")
        binding.topicTitleText.text = topic.subtopicTitle.display(appState.language)
        binding.topicSourceText.text = listOf(
            topic.lessonTitle.display(appState.language),
            topic.chapterTitle.display(appState.language),
        ).joinToString(" - ")

        when (engine.session.state) {
            LearningState.ASK_IF_KNOWN -> renderKnowPrompt(topic)
            LearningState.EXPLAIN_TOPIC -> renderExplanation(topic)
            LearningState.TAKE_QUIZ -> renderQuiz(topic)
            else -> Unit
        }
    }

    private fun renderKnowPrompt(topic: StudyTopic) {
        binding.promptText.text = topic.knowPrompt.display(appState.language)
        binding.explanationCard.isVisible = false
        binding.quizCard.isVisible = false
        binding.teacherPlaybackCard.isVisible = false
        binding.decisionContainer.isVisible = true
        updateContentBottomInset(playbackVisible = false)
        updateDecisionContainerInset(playbackVisible = false)
        binding.positiveButton.text = ui("Yes, I know it", "à¤¹à¤¾à¤, à¤®à¥à¤à¥‡ à¤†à¤¤à¤¾ à¤¹à¥ˆ")
        binding.negativeButton.text = ui("No, teach me", "à¤¨à¤¹à¥€à¤‚, à¤¸à¤®à¤à¤¾à¤‡à¤")
        renderFeedbackCard()
    }

    private fun renderExplanation(topic: StudyTopic) {
        binding.explanationCard.isVisible = true
        binding.quizCard.isVisible = false
        binding.teacherPlaybackCard.isVisible = true
        binding.decisionContainer.isVisible = true
        if (solutionPreviewActive) {
            renderQuestionSolution(topic)
        } else {
            binding.promptText.text = ui(
                "Here is the lesson. Have you understood this topic?",
                "à¤¯à¤¹ à¤ªà¤¾à¤  à¤¹à¥ˆà¥¤ à¤•à¥à¤¯à¤¾ à¤†à¤ªà¤•à¥‹ à¤¯à¤¹ à¤µà¤¿à¤·à¤¯ à¤¸à¤®à¤ à¤†à¤¯à¤¾?",
            )
            binding.positiveButton.text = ui("Yes, I understood", "à¤¹à¤¾à¤, à¤¸à¤®à¤ à¤†à¤¯à¤¾")
            binding.negativeButton.text = ui("No, explain again", "à¤¨à¤¹à¥€à¤‚, à¤«à¤¿à¤° à¤¸à¤®à¤à¤¾à¤‡à¤")

            binding.explanationTitleText.text = topic.explanationTitle.display(appState.language)
            binding.explanationBodyText.isVisible = true
            binding.explanationBodyText.text = ui(
                "Watch the board. The teacher explains every step, visual, and example one by one.",
                "बोर्ड को देखिए। शिक्षक हर चरण, दृश्य और उदाहरण को एक-एक करके समझाएँगे।",
            )
            renderExplanationSentences(topic)
            val hasExamples = topic.examples.isNotEmpty()
            binding.examplesLabelText.isVisible = hasExamples
            binding.examplesText.isVisible = hasExamples
            if (hasExamples) {
                binding.examplesLabelText.text = ui("Examples", "उदाहरण")
                binding.examplesText.text = topic.examples.joinToString("\n") {
                    "- ${it.display(appState.language)}"
                }
            }
            renderVisuals(topic.visuals)
        }
        binding.openVoiceSettingsButton.text = ui("Open voice settings", "à¤µà¥‰à¤‡à¤¸ à¤¸à¥‡à¤Ÿà¤¿à¤‚à¤— à¤–à¥‹à¤²à¥‡à¤‚")
        binding.voiceLabelText.text = compactUi("Teacher pace", "शिक्षक की गति")
        binding.paceNormalButton.text = "Normal"
        binding.paceSlowButton.text = "Slow"
        binding.paceToggle.check(
            when (appState.narrationPace) {
                NarrationPace.NORMAL -> binding.paceNormalButton.id
                NarrationPace.SLOW -> binding.paceSlowButton.id
            }
        )
        renderPlaybackBar()
        renderFeedbackCard()
    }

    private fun renderQuiz(topic: StudyTopic) {
        val question = engine.currentQuestion(Difficulty.EASY) ?: return

        binding.promptText.text = ui("Answer this check question.", "à¤‡à¤¸ à¤œà¤¾à¤à¤š à¤ªà¥à¤°à¤¶à¥à¤¨ à¤•à¤¾ à¤‰à¤¤à¥à¤¤à¤° à¤¦à¥‡à¤‚à¥¤")
        binding.explanationCard.isVisible = false
        binding.feedbackCard.isVisible = false
        binding.quizCard.isVisible = true
        binding.teacherPlaybackCard.isVisible = false
        binding.decisionContainer.isVisible = false
        updateContentBottomInset(playbackVisible = false)
        updateDecisionContainerInset(playbackVisible = false)
        binding.questionTitleText.text = topic.subtopicTitle.display(appState.language)
        binding.questionPromptText.text = question.prompt.display(appState.language)
        binding.answerInputLayout.hint = ui("Type your answer", "à¤…à¤ªà¤¨à¤¾ à¤‰à¤¤à¥à¤¤à¤° à¤²à¤¿à¤–à¤¿à¤")
        binding.submitAnswerButton.text = ui("Submit answer", "à¤‰à¤¤à¥à¤¤à¤° à¤œà¤®à¤¾ à¤•à¤°à¥‡à¤‚")

        val hintText = question.hint?.display(appState.language).orEmpty()
        binding.hintText.isVisible = hintText.isNotBlank()
        binding.hintText.text = if (hintText.isBlank()) {
            ""
        } else {
            "${ui("Hint", "à¤¸à¤‚à¤•à¥‡à¤¤")}: $hintText"
        }

        if (question.type == QuestionType.MULTIPLE_CHOICE) {
            binding.answerOptions.removeAllViews()
            question.options.forEach { optionText ->
                val radioButton = RadioButton(this).apply {
                    id = View.generateViewId()
                    text = optionText.display(appState.language)
                    textSize = 16f
                    setTextColor(getColor(R.color.text_primary))
                    setPadding(0, dp(14), 0, dp(14))
                }
                binding.answerOptions.addView(radioButton)
            }
            binding.answerOptions.clearCheck()
            binding.answerOptions.isVisible = true
            binding.answerInputLayout.isVisible = false
        } else {
            binding.answerOptions.removeAllViews()
            binding.answerOptions.isVisible = false
            binding.answerInputLayout.isVisible = true
            binding.answerInputEditText.setText("")
        }
    }

    private fun renderQuestionSolution(topic: StudyTopic) {
        val question = latestIncorrectQuestion ?: return
        val result = latestQuizResult

        binding.promptText.text = ui(
            "Here is the solution. Have you understood how to solve this question?",
            "यह समाधान है। क्या अब आपको यह प्रश्न हल करना समझ आया?",
        )
        binding.positiveButton.text = ui("Yes, try again", "हाँ, फिर कोशिश करूँगा")
        binding.negativeButton.text = ui("Explain solution again", "समाधान फिर समझाइए")
        binding.explanationTitleText.text = ui(
            "Step-by-step solution",
            "चरण-दर-चरण समाधान",
        )
        binding.explanationBodyText.isVisible = true
        binding.explanationBodyText.text = ui(
            "Watch the board. The teacher explains why the answer was incorrect and how to solve it correctly.",
            "बोर्ड को देखिए। शिक्षक बताएँगे कि उत्तर गलत क्यों था और सही हल कैसे करना है।",
        )
        renderExplanationSentences(topic)
        binding.examplesLabelText.text = ui("How to solve", "हल कैसे करें")
        binding.examplesText.text = ui(
            "Follow each step on the board carefully. Work through the solution yourself, then try the question again.",
            "बोर्ड पर हर चरण को ध्यान से देखें। खुद हल करें, फिर प्रश्न दोबारा हल करने की कोशिश करें।",
        )
        renderVisuals(emptyList())
    }

    private fun renderExplanationSentences(topic: StudyTopic) {
        prepareExplanationBoard(topic)
        binding.explanationSentenceContainer.removeAllViews()
        val revealAllForReading = !ttsReady && !speakSequenceActive && revealedSentenceCount == 0
        var activeSentenceView: View? = null
        val spacerHeight = teachingBoardSpacerHeight()
        var addedSentenceContent = false

        binding.explanationSentenceContainer.addView(
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    spacerHeight,
                )
            },
        )

        currentSpeechSentences.forEachIndexed { index, sentence ->
            val shouldShow = revealAllForReading || index < revealedSentenceCount
            if (!shouldShow) return@forEachIndexed
            val isActiveLine = index == activeChalkSentenceIndex && (speakSequenceActive || speechPaused)
            addedSentenceContent = true

            val sentenceView = TextView(this).apply {
                text = if (isActiveLine) {
                    chalkDisplayText(sentence)
                } else {
                    sentence
                }
                textSize = 15f
                setTextColor(
                    getColor(
                        if (isActiveLine) R.color.chalk_text else R.color.chalk_text_dim,
                    )
                )
                setLineSpacing(0f, 1.15f)
                letterSpacing = 0.03f
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(dp(14), dp(12), dp(14), dp(12))
                if (isActiveLine) {
                    setBackgroundResource(R.drawable.chalk_active_sentence_surface)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    activeSentenceView = this
                } else {
                    background = null
                    alpha = 0.88f
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    if (index > 0) topMargin = dp(8)
                }
            }
            binding.explanationSentenceContainer.addView(sentenceView)
        }

        if (!addedSentenceContent) {
            binding.explanationSentenceContainer.addView(
                TextView(this).apply {
                    text = ui(
                        "Tap play and the teacher will begin writing each step here.",
                        "à¤ªà¥à¤²à¥‡ à¤¦à¤¬à¤¾à¤‡à¤, à¤¶à¤¿à¤•à¥à¤·à¤• à¤¹à¤° à¤šà¤°à¤£ à¤•à¥‹ à¤¯à¤¹à¤¾à¤ à¤à¤•-à¤à¤• à¤•à¤°à¤•à¥‡ à¤²à¤¿à¤–à¤¨à¤¾ à¤¶à¥à¤°à¥‚ à¤•à¤°à¥‡à¤‚à¤—à¥‡à¥¤",
                    )
                    textSize = 15f
                    setTextColor(getColor(R.color.chalk_text_dim))
                    typeface = android.graphics.Typeface.MONOSPACE
                    letterSpacing = 0.03f
                    setLineSpacing(0f, 1.15f)
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                }
            )
        }

        binding.explanationSentenceContainer.addView(
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    spacerHeight,
                )
            },
        )

        scrollToActiveSentence(activeSentenceView)
        renderPlaybackBar()
    }

    private fun prepareExplanationBoard(topic: StudyTopic) {
        val sentences = if (solutionPreviewActive && latestIncorrectQuestion != null) {
            TeachingScriptBuilder.buildQuestionSolution(
                topic = topic,
                question = latestIncorrectQuestion!!,
                result = latestQuizResult,
                language = appState.language,
            )
        } else {
            TeachingScriptBuilder.build(topic, appState.language)
        }.ifEmpty { listOf(topic.explanationTitle.display(appState.language)) }
        val token = if (solutionPreviewActive && latestIncorrectQuestion != null) {
            "solution_${topic.id}_${latestIncorrectQuestion!!.id}_${engine.explanationToken()}_${appState.language}"
        } else {
            "${topic.id}_${engine.explanationToken()}_${appState.language}"
        }
        if (token != currentExplanationBoardToken || currentSpeechSentences != sentences) {
            stopChalkWriting(completeCurrentLine = false)
            currentExplanationBoardToken = token
            currentSpeechSentences = sentences
            currentSpeechIndex = 0
            revealedSentenceCount = 0
            activeChalkSentenceIndex = -1
            activeChalkCharacterCount = 0
            lastAutoScrollSentenceIndex = -1
            lastAutoScrollCharacterCount = -1
            speechPaused = false
        } else {
            currentSpeechSentences = sentences
        }

        if (currentSpeechSentences.isNotEmpty()) {
            currentSpeechIndex = currentSpeechIndex.coerceIn(0, currentSpeechSentences.lastIndex)
            revealedSentenceCount = revealedSentenceCount.coerceIn(0, currentSpeechSentences.size)
        }
    }

    private fun renderPlaybackBar() {
        val visible = engine.session.state == LearningState.EXPLAIN_TOPIC
        binding.teacherPlaybackCard.isVisible = visible
        updateContentBottomInset(playbackVisible = visible)
        updateDecisionContainerInset(playbackVisible = visible)
        if (!visible) return

        val completed =
            currentSpeechSentences.isNotEmpty() &&
                revealedSentenceCount >= currentSpeechSentences.size &&
                !speakSequenceActive &&
                !speechPaused

        binding.teacherPlaybackTitleText.text = compactUi("Controls", "नियंत्रण")
        binding.playVoiceButton.text = when {
            speakSequenceActive -> compactUi("Teaching", "सिखा रहे")
            speechPaused -> compactUi("Resume", "फिर चलाएँ")
            completed -> compactUi("Replay", "फिर पढ़ाएँ")
            else -> compactUi("Play", "चलाएँ")
        }
        binding.pauseVoiceButton.text = compactUi("Pause", "रोकें")
        binding.rewindVoiceButton.text = compactUi("Back", "पीछे")
        binding.replaySentenceButton.text = compactUi("Replay line", "पंक्ति दोहराएँ")
        binding.pauseVoiceButton.isEnabled = speakSequenceActive
        binding.rewindVoiceButton.isEnabled = currentSpeechSentences.isNotEmpty()
        binding.replaySentenceButton.isEnabled = currentSpeechSentences.isNotEmpty()
    }

    private fun updateContentBottomInset(playbackVisible: Boolean) {
        val targetBottomPadding = if (playbackVisible) {
            val playbackHeight = binding.teacherPlaybackCard.height.takeIf { it > 0 } ?: dp(188)
            baseScrollBottomPadding + playbackHeight + dp(20)
        } else {
            baseScrollBottomPadding
        }

        val currentLeft = binding.contentScrollView.paddingLeft
        val currentTop = binding.contentScrollView.paddingTop
        val currentRight = binding.contentScrollView.paddingRight
        if (binding.contentScrollView.paddingBottom != targetBottomPadding) {
            binding.contentScrollView.setPadding(
                currentLeft,
                currentTop,
                currentRight,
                targetBottomPadding,
            )
        }

        if (playbackVisible) {
            binding.teacherPlaybackCard.post {
                val measuredTarget = baseScrollBottomPadding + binding.teacherPlaybackCard.height + dp(20)
                if (binding.contentScrollView.paddingBottom != measuredTarget) {
                    binding.contentScrollView.setPadding(
                        binding.contentScrollView.paddingLeft,
                        binding.contentScrollView.paddingTop,
                        binding.contentScrollView.paddingRight,
                        measuredTarget,
                    )
                }
            }
        }
    }

    private fun updateDecisionContainerInset(playbackVisible: Boolean) {
        val layoutParams = binding.decisionContainer.layoutParams as? LinearLayout.LayoutParams ?: return
        val targetBottomMargin = if (playbackVisible) {
            val playbackHeight = binding.teacherPlaybackCard.height.takeIf { it > 0 } ?: dp(188)
            baseDecisionBottomMargin + playbackHeight + dp(12)
        } else {
            baseDecisionBottomMargin
        }

        if (layoutParams.bottomMargin != targetBottomMargin) {
            layoutParams.bottomMargin = targetBottomMargin
            binding.decisionContainer.layoutParams = layoutParams
        }

        if (playbackVisible) {
            binding.teacherPlaybackCard.post {
                val measuredParams = binding.decisionContainer.layoutParams as? LinearLayout.LayoutParams ?: return@post
                val measuredBottomMargin = baseDecisionBottomMargin + binding.teacherPlaybackCard.height + dp(12)
                if (measuredParams.bottomMargin != measuredBottomMargin) {
                    measuredParams.bottomMargin = measuredBottomMargin
                    binding.decisionContainer.layoutParams = measuredParams
                }
            }
        }
    }

    private fun chalkDisplayText(sentence: String): String {
        val visibleCount = activeChalkCharacterCount.coerceIn(0, sentence.length)
        val visibleText = sentence.take(visibleCount)
        val showCursor = speakSequenceActive && visibleCount < sentence.length
        return if (showCursor) "$visibleText _" else visibleText
    }

    private fun startChalkWriting(sentenceIndex: Int) {
        val sentence = currentSpeechSentences.getOrNull(sentenceIndex) ?: return
        stopChalkWriting(completeCurrentLine = false)
        activeChalkSentenceIndex = sentenceIndex
        activeChalkCharacterCount = 0
        lastAutoScrollSentenceIndex = -1
        lastAutoScrollCharacterCount = -1
        scheduleNextChalkStroke(sentence, immediate = true)
    }

    private fun scheduleNextChalkStroke(
        sentence: String,
        immediate: Boolean = false,
    ) {
        val delayMillis = if (immediate) 0L else chalkStrokeDelayMillis(sentence, activeChalkCharacterCount)
        val runnable = Runnable {
            val currentSentence = currentSpeechSentences.getOrNull(activeChalkSentenceIndex) ?: return@Runnable
            if (currentSentence.isEmpty()) return@Runnable

            activeChalkCharacterCount = (activeChalkCharacterCount + 1).coerceAtMost(currentSentence.length)
            engine.currentTopic()?.let(::renderExplanationSentences)

            if (speakSequenceActive && activeChalkSentenceIndex == currentSpeechIndex && activeChalkCharacterCount < currentSentence.length) {
                scheduleNextChalkStroke(currentSentence)
            }
        }
        chalkWriteRunnable = runnable
        chalkHandler.postDelayed(runnable, delayMillis)
    }

    private fun chalkStrokeDelayMillis(
        sentence: String,
        currentCharacterCount: Int,
    ): Long {
        val baseDelay = when (appState.narrationPace) {
            NarrationPace.NORMAL -> 36L
            NarrationPace.SLOW -> 52L
        }
        val nextCharacter = sentence.getOrNull(currentCharacterCount) ?: return baseDelay
        return when (nextCharacter) {
            ' ' -> baseDelay / 2
            ',', ';', ':' -> baseDelay + 90L
            '.', '!', '?' -> baseDelay + 160L
            else -> baseDelay
        }
    }

    private fun stopChalkWriting(completeCurrentLine: Boolean) {
        chalkWriteRunnable?.let(chalkHandler::removeCallbacks)
        chalkWriteRunnable = null
        if (completeCurrentLine) {
            currentSpeechSentences.getOrNull(activeChalkSentenceIndex)?.let { sentence ->
                activeChalkCharacterCount = sentence.length
            }
        }
    }

    private fun resetTeachingBoardState() {
        stopChalkWriting(completeCurrentLine = false)
        speakSequenceActive = false
        speechPaused = false
        currentSpeechSentences = emptyList()
        currentSpeechIndex = 0
        revealedSentenceCount = 0
        activeChalkSentenceIndex = -1
        activeChalkCharacterCount = 0
        lastAutoScrollSentenceIndex = -1
        lastAutoScrollCharacterCount = -1
        currentExplanationBoardToken = null
    }

    private fun scrollToActiveSentence(activeSentenceView: View?) {
        activeSentenceView ?: return
        val shouldScroll =
            activeChalkSentenceIndex != lastAutoScrollSentenceIndex ||
                activeChalkCharacterCount <= 1 ||
                (activeChalkCharacterCount - lastAutoScrollCharacterCount) >= 12 ||
                !speakSequenceActive
        if (!shouldScroll) return

        lastAutoScrollSentenceIndex = activeChalkSentenceIndex
        lastAutoScrollCharacterCount = activeChalkCharacterCount
        binding.contentScrollView.post {
            val sentenceTop =
                binding.sessionGroup.top +
                    binding.explanationCard.top +
                    binding.explanationSentenceContainer.top +
                    activeSentenceView.top
            val sentenceCenter = sentenceTop + (activeSentenceView.height / 2)
            val playbackHeight = if (binding.teacherPlaybackCard.isVisible) binding.teacherPlaybackCard.height else 0
            val effectiveViewportHeight =
                (
                    binding.contentScrollView.height -
                        playbackHeight -
                        dp(24)
                    ).coerceAtLeast(dp(220))
            val desiredCenterY = effectiveViewportHeight / 2
            val maxScroll =
                (
                    (binding.contentScrollView.getChildAt(0)?.height ?: 0) +
                        binding.contentScrollView.paddingBottom -
                        binding.contentScrollView.height
                    ).coerceAtLeast(0)
            val targetY = (sentenceCenter - desiredCenterY).coerceIn(0, maxScroll)
            binding.contentScrollView.smoothScrollTo(0, targetY)
        }
    }

    private fun teachingBoardSpacerHeight(): Int {
        val screenHeight =
            if (binding.contentScrollView.height > 0) {
                binding.contentScrollView.height
            } else {
                resources.displayMetrics.heightPixels
            }
        val playbackHeight = if (binding.teacherPlaybackCard.isVisible) binding.teacherPlaybackCard.height else 0
        val effectiveViewportHeight = (screenHeight - playbackHeight - dp(24)).coerceAtLeast(dp(220))
        return ((effectiveViewportHeight / 2) - dp(96)).coerceAtLeast(dp(120))
    }

    private fun renderFeedbackCard() {
        val result = latestQuizResult
        val visible =
            result != null &&
                !solutionPreviewActive &&
                (engine.session.state == LearningState.ASK_IF_KNOWN || engine.session.state == LearningState.EXPLAIN_TOPIC)
        binding.feedbackCard.isVisible = visible
        if (!visible || result == null) return

        val fromWrongAnswerPrompt = engine.session.state == LearningState.ASK_IF_KNOWN
        binding.feedbackTitleText.setTextColor(
            getColor(if (fromWrongAnswerPrompt) R.color.feedback_error else R.color.text_primary),
        )
        binding.feedbackBodyText.setTextColor(
            getColor(if (fromWrongAnswerPrompt) R.color.feedback_error else R.color.text_primary),
        )
        binding.feedbackTitleText.text = if (fromWrongAnswerPrompt) {
            ui("Incorrect answer", "गलत उत्तर")
        } else {
            result.reteachTitle?.display(appState.language)
                ?: ui("Let us fix the confusion", "à¤šà¤²à¥‹ à¤­à¥à¤°à¤® à¤¦à¥‚à¤° à¤•à¤°à¥‡à¤‚")
        }
        binding.feedbackBodyText.text = listOfNotNull(
            if (fromWrongAnswerPrompt) result.message.display(appState.language) else null,
            result.wrongReason?.display(appState.language),
            result.supportExample?.display(appState.language)?.let { "${ui("Example", "à¤‰à¤¦à¤¾à¤¹à¤°à¤£")}: $it" },
            result.reteachParagraphs.takeIf { !fromWrongAnswerPrompt && it.isNotEmpty() }?.joinToString("\n") { it.display(appState.language) },
        ).joinToString("\n\n")
        binding.feedbackActionButton.isVisible = latestIncorrectQuestion != null
        binding.feedbackActionButton.text = ui("See solution", "समाधान देखें")
    }

    private fun renderMetricBars(report: ReportSummary) {
        binding.chartContainer.removeAllViews()
        report.chartPoints.forEach { point ->
            val line = "${point.label.display(appState.language)} ${barText(point.value, point.maxValue)} ${point.value}/${point.maxValue}"
            binding.chartContainer.addView(sectionText(line, true))
        }
    }

    private fun renderMasteryMap(report: ReportSummary) {
        binding.masteryMapContainer.removeAllViews()
        binding.masteryMapContainer.addView(sectionText(ui("Chapter mastery", "à¤…à¤§à¥à¤¯à¤¾à¤¯ à¤®à¤¾à¤¸à¥à¤Ÿà¤°à¥€"), false, true))
        report.chapterMastery.forEach { chapter ->
            val line = "${ui("Chapter", "à¤…à¤§à¥à¤¯à¤¾à¤¯")} ${chapter.chapterNumber}: ${barText(chapter.masteredTopics, chapter.totalTopics)} ${chapter.masteredTopics}/${chapter.totalTopics}"
            binding.masteryMapContainer.addView(sectionText(line, true))
        }
    }

    private fun renderMistakeBreakdown(report: ReportSummary) {
        binding.mistakeContainer.removeAllViews()
        binding.mistakeContainer.addView(sectionText(ui("Common mistake patterns", "à¤†à¤® à¤—à¤²à¤¤à¥€ à¤ªà¥ˆà¤Ÿà¤°à¥à¤¨"), false, true))
        val lines = if (report.topMistakes.isEmpty()) {
            listOf(ui("No major mistake pattern yet.", "à¤…à¤­à¥€ à¤•à¥‹à¤ˆ à¤¬à¤¡à¤¼à¤¾ à¤—à¤²à¤¤à¥€ à¤ªà¥ˆà¤Ÿà¤°à¥à¤¨ à¤¨à¤¹à¥€à¤‚ à¤¹à¥ˆà¥¤"))
        } else {
            report.topMistakes.map { "${mistakeLabel(it.type)}: ${it.count}" }
        }
        lines.forEach { binding.mistakeContainer.addView(sectionText(it, true)) }
    }

    private fun renderBadgeSection(report: ReportSummary) {
        binding.badgeContainer.removeAllViews()
        binding.badgeContainer.addView(sectionText(ui("Badges and rewards", "à¤¬à¥ˆà¤œ à¤”à¤° à¤ªà¥à¤°à¤¸à¥à¤•à¤¾à¤°"), false, true))
        val summaryLines = mutableListOf(
            ui("Streak days: ${report.streakDays}", "à¤²à¤—à¤¾à¤¤à¤¾à¤° à¤¦à¤¿à¤¨: ${report.streakDays}"),
            ui("Revision rewards: ${report.revisionRewardCount}", "à¤°à¤¿à¤µà¤¿à¤œà¤¨ à¤ªà¥à¤°à¤¸à¥à¤•à¤¾à¤°: ${report.revisionRewardCount}"),
            ui("Chapter trophies: ${report.chapterTrophies.size}", "à¤…à¤§à¥à¤¯à¤¾à¤¯ à¤Ÿà¥à¤°à¥‰à¤«à¤¿à¤¯à¤¾à¤: ${report.chapterTrophies.size}"),
        )
        if (report.badges.isNotEmpty()) {
            summaryLines += report.badges.map { "- ${it.title.display(appState.language)}" }
        }
        summaryLines.forEach { binding.badgeContainer.addView(sectionText(it, true)) }
    }

    private fun renderCompletion() {
        val sessionComplete = engine.session.state == LearningState.SESSION_COMPLETE
        binding.completionCard.isVisible = sessionComplete
        if (!sessionComplete) {
            return
        }

        binding.completionTitleText.text = ui("Session complete", "à¤¸à¤¤à¥à¤° à¤ªà¥‚à¤°à¤¾")
        binding.completionBodyText.text = when (engine.session.mode) {
            StudyMode.MAIN_PATH -> ui(
                "Great work. This child finished the current main-path queue. You can return to the dashboard for revision, weak-topic practice, or the report.",
                "à¤¬à¤¹à¥à¤¤ à¤…à¤šà¥à¤›à¤¾à¥¤ à¤‡à¤¸ à¤¬à¤šà¥à¤šà¥‡ à¤¨à¥‡ à¤…à¤­à¥€ à¤•à¤¾ à¤®à¥à¤–à¥à¤¯ à¤…à¤§à¥à¤¯à¤¯à¤¨-à¤ªà¤¥ à¤ªà¥‚à¤°à¤¾ à¤•à¤° à¤²à¤¿à¤¯à¤¾ à¤¹à¥ˆà¥¤ à¤…à¤¬ à¤†à¤ª à¤¡à¥ˆà¤¶à¤¬à¥‹à¤°à¥à¤¡ à¤ªà¤° à¤²à¥Œà¤Ÿà¤•à¤° à¤ªà¥à¤¨à¤°à¤¾à¤µà¥ƒà¤¤à¥à¤¤à¤¿, à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯ à¤…à¤­à¥à¤¯à¤¾à¤¸, à¤¯à¤¾ à¤°à¤¿à¤ªà¥‹à¤°à¥à¤Ÿ à¤¦à¥‡à¤– à¤¸à¤•à¤¤à¥‡ à¤¹à¥ˆà¤‚à¥¤",
            )

            StudyMode.EXERCISE_PATH -> ui(
                "Exercise practice is complete for now. You can return to the dashboard for more exercise questions, revision, or the report.",
                "à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤ªà¤¥ à¤…à¤­à¥€ à¤•à¥‡ à¤²à¤¿à¤ à¤ªà¥‚à¤°à¤¾ à¤¹à¥‹ à¤—à¤¯à¤¾ à¤¹à¥ˆà¥¤ à¤…à¤¬ à¤†à¤ª à¤¡à¥ˆà¤¶à¤¬à¥‹à¤°à¥à¤¡ à¤ªà¤° à¤²à¥Œà¤Ÿà¤•à¤° à¤…à¤­à¥à¤¯à¤¾à¤¸, à¤ªà¥à¤¨à¤°à¤¾à¤µà¥ƒà¤¤à¥à¤¤à¤¿ à¤¯à¤¾ à¤°à¤¿à¤ªà¥‹à¤°à¥à¤Ÿ à¤¦à¥‡à¤– à¤¸à¤•à¤¤à¥‡ à¤¹à¥ˆà¤‚à¥¤",
            )

            StudyMode.REVISION -> ui(
                "Revision topics are complete for now. The next due set will appear automatically later.",
                "à¤…à¤­à¥€ à¤•à¥‡ à¤²à¤¿à¤ à¤ªà¥à¤¨à¤°à¤¾à¤µà¥ƒà¤¤à¥à¤¤à¤¿ à¤µà¤¿à¤·à¤¯ à¤ªà¥‚à¤°à¥‡ à¤¹à¥‹ à¤—à¤ à¤¹à¥ˆà¤‚à¥¤ à¤…à¤—à¤²à¤¾ à¤¦à¥‡à¤¯ à¤¸à¥‡à¤Ÿ à¤¬à¤¾à¤¦ à¤®à¥‡à¤‚ à¤…à¤ªà¤¨à¥‡-à¤†à¤ª à¤¦à¤¿à¤–à¤¾à¤ˆ à¤¦à¥‡à¤—à¤¾à¥¤",
            )

            StudyMode.WEAK_TOPICS -> ui(
                "Weak-topic practice is complete for now. Check the report to see what still needs support.",
                "à¤…à¤­à¥€ à¤•à¥‡ à¤²à¤¿à¤ à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯à¥‹à¤‚ à¤•à¤¾ à¤…à¤­à¥à¤¯à¤¾à¤¸ à¤ªà¥‚à¤°à¤¾ à¤¹à¥‹ à¤—à¤¯à¤¾ à¤¹à¥ˆà¥¤ à¤†à¤—à¥‡ à¤•à¤¿à¤¸à¥‡ à¤¸à¤¹à¤¾à¤¯à¤¤à¤¾ à¤šà¤¾à¤¹à¤¿à¤, à¤¯à¤¹ à¤°à¤¿à¤ªà¥‹à¤°à¥à¤Ÿ à¤®à¥‡à¤‚ à¤¦à¥‡à¤–à¤¿à¤à¥¤",
            )

            null -> ui("You can return to the dashboard now.", "à¤…à¤¬ à¤†à¤ª à¤¡à¥ˆà¤¶à¤¬à¥‹à¤°à¥à¤¡ à¤ªà¤° à¤²à¥Œà¤Ÿ à¤¸à¤•à¤¤à¥‡ à¤¹à¥ˆà¤‚à¥¤")
        }
        binding.restartButton.text = ui("Back to dashboard", "à¤¡à¥ˆà¤¶à¤¬à¥‹à¤°à¥à¤¡ à¤ªà¤° à¤µà¤¾à¤ªà¤¸")
    }

    private fun renderStatus() {
        binding.statusText.isVisible = !latestStatusMessage.isNullOrBlank()
        binding.statusText.text = latestStatusMessage.orEmpty()
    }

    private fun renderVisuals(visuals: List<VisualBlock>) {
        binding.visualsContainer.removeAllViews()
        visuals.forEachIndexed { index, visual ->
            val card = MaterialCardView(this).apply {
                radius = dp(20).toFloat()
                setCardBackgroundColor(getColor(R.color.card_surface))
                strokeColor = getColor(R.color.card_stroke)
                strokeWidth = dp(1)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    if (index > 0) topMargin = dp(12)
                }
            }

            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }

            val titleView = TextView(this).apply {
                text = visual.title.display(appState.language)
                textSize = 16f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            column.addView(titleView)

            val descriptionView = TextView(this).apply {
                text = visual.description.display(appState.language)
                textSize = 14f
                setTextColor(getColor(R.color.text_primary))
                setLineSpacing(0f, 1.15f)
                setPadding(0, dp(8), 0, 0)
            }
            column.addView(descriptionView)

            if (visual.chips.isNotEmpty()) {
                val chipColumn = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, dp(10), 0, 0)
                }
                visual.chips.forEach { chip ->
                    val chipView = TextView(this).apply {
                        text = chip.display(appState.language)
                        gravity = Gravity.CENTER_VERTICAL
                        setTextColor(getColor(R.color.text_primary))
                        setBackgroundResource(R.drawable.chip_surface)
                        setPadding(dp(12), dp(8), dp(12), dp(8))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply { bottomMargin = dp(8) }
                    }
                    chipColumn.addView(chipView)
                }
                column.addView(chipColumn)
            }

            if (visual.rows.isNotEmpty()) {
                val rowColumn = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, dp(10), 0, 0)
                }
                visual.rows.forEach { row ->
                    val rowLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply { bottomMargin = dp(8) }
                    }
                    row.forEach { cell ->
                        val cellView = TextView(this).apply {
                            text = cell.display(appState.language)
                            setTextColor(getColor(R.color.text_primary))
                            setBackgroundResource(R.drawable.chip_surface)
                            setPadding(dp(10), dp(8), dp(10), dp(8))
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f,
                            ).apply { marginEnd = dp(8) }
                        }
                        rowLayout.addView(cellView)
                    }
                    rowColumn.addView(rowLayout)
                }
                column.addView(rowColumn)
            }

            card.addView(column)
            binding.visualsContainer.addView(card)
        }
    }

    private fun maybeSpeakExplanation() {
        if (engine.session.state != LearningState.EXPLAIN_TOPIC) return
        val token = engine.explanationToken()
        if (!ttsReady || lastSpokenToken == token) return

        speakCurrentExplanation(restartFromBeginning = true)
        lastSpokenToken = token
    }

    private fun speakCurrentExplanation(restartFromBeginning: Boolean = false) {
        val topic = engine.currentTopic() ?: return
        if (!ttsReady) {
            latestStatusMessage = TTS_LOADING_MESSAGE
            renderStatus()
            return
        }

        prepareExplanationBoard(topic)
        val shouldRestart =
            restartFromBeginning ||
                currentSpeechSentences.isEmpty() ||
                (revealedSentenceCount >= currentSpeechSentences.size && !speechPaused)
        if (shouldRestart) {
            currentSpeechIndex = 0
            revealedSentenceCount = 0
        }

        if (currentSpeechSentences.isEmpty()) return
        currentSpeechIndex = currentSpeechIndex.coerceIn(0, currentSpeechSentences.lastIndex)
        speakSequenceActive = true
        speechPaused = false
        activeChalkSentenceIndex = currentSpeechIndex
        activeChalkCharacterCount = 0
        renderExplanationSentences(topic)
        textToSpeech?.stop()
        textToSpeech?.setSpeechRate(appState.narrationPace.speechRate)
        speakSentenceAtCurrentIndex()
    }

    private fun pauseSpeech() {
        speakSequenceActive = false
        speechPaused = currentSpeechSentences.isNotEmpty()
        stopChalkWriting(completeCurrentLine = false)
        textToSpeech?.stop()
        engine.currentTopic()?.let(::renderExplanationSentences)
        latestStatusMessage = ui("Voice paused.", "à¤†à¤µà¤¾à¤œà¤¼ à¤°à¥‹à¤•à¥€ à¤—à¤ˆà¥¤")
        renderStatus()
    }

    private fun rewindSpeech() {
        val topic = engine.currentTopic() ?: return
        if (!ttsReady) {
            latestStatusMessage = TTS_LOADING_MESSAGE
            renderStatus()
            return
        }

        prepareExplanationBoard(topic)
        if (currentSpeechSentences.isEmpty()) {
            speakCurrentExplanation(restartFromBeginning = true)
            return
        }

        currentSpeechIndex = (currentSpeechIndex - 1).coerceAtLeast(0)
        revealedSentenceCount = (currentSpeechIndex + 1).coerceAtLeast(1)
        speakSequenceActive = true
        speechPaused = false
        activeChalkSentenceIndex = currentSpeechIndex
        activeChalkCharacterCount = 0
        renderExplanationSentences(topic)
        textToSpeech?.stop()
        textToSpeech?.setSpeechRate(appState.narrationPace.speechRate)
        speakSentenceAtCurrentIndex()
        latestStatusMessage = ui("Going back one step.", "à¤à¤• à¤šà¤°à¤£ à¤ªà¥€à¤›à¥‡ à¤œà¤¾ à¤°à¤¹à¥‡ à¤¹à¥ˆà¤‚à¥¤")
        renderStatus()
    }

    private fun replayCurrentSentence() {
        val topic = engine.currentTopic() ?: return
        if (!ttsReady) {
            latestStatusMessage = TTS_LOADING_MESSAGE
            renderStatus()
            return
        }

        prepareExplanationBoard(topic)
        if (currentSpeechSentences.isEmpty()) {
            speakCurrentExplanation(restartFromBeginning = true)
            return
        }

        currentSpeechIndex = currentSpeechIndex.coerceIn(0, currentSpeechSentences.lastIndex)
        speakSequenceActive = true
        speechPaused = false
        activeChalkSentenceIndex = currentSpeechIndex
        activeChalkCharacterCount = 0
        renderExplanationSentences(topic)
        textToSpeech?.stop()
        textToSpeech?.setSpeechRate(appState.narrationPace.speechRate)
        speakSentenceAtCurrentIndex()
        latestStatusMessage = ui("Replaying the current sentence.", "à¤µà¤°à¥à¤¤à¤®à¤¾à¤¨ à¤µà¤¾à¤•à¥à¤¯ à¤«à¤¿à¤° à¤¸à¥‡ à¤šà¤² à¤°à¤¹à¤¾ à¤¹à¥ˆà¥¤")
        renderStatus()
    }

    private fun speakSentenceAtCurrentIndex() {
        if (currentSpeechSentences.isEmpty()) return
        stopChalkWriting(completeCurrentLine = false)
        activeChalkSentenceIndex = currentSpeechIndex
        activeChalkCharacterCount = 0
        engine.currentTopic()?.let(::renderExplanationSentences)
        val sentence = currentSpeechSentences.getOrElse(currentSpeechIndex) { currentSpeechSentences.last() }
        val speechProfile = speechProfileForText(sentence) ?: defaultSpeechProfile()
        if (speechProfile == null || !applySpeechProfile(speechProfile, reason = "utterance")) {
            ttsReady = false
            ttsIssueMessage = TTS_UNAVAILABLE_MESSAGE
            latestStatusMessage = ttsIssueMessage
            renderStatus()
            return
        }
        textToSpeech?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "topic_sentence_$currentSpeechIndex")
    }

    private fun switchBook(bookId: String) {
        val nextBook = LessonRepository.book(this, bookId)
        book = nextBook
        engine = LessonEngine(book)
        appState = appState.copy(
            selectedBookId = book.id,
            session = SessionSnapshot(bookId = book.id),
        )
        engine.finishSession()
        latestQuizResult = null
        latestIncorrectQuestion = null
        solutionPreviewActive = false
        reportExpanded = false
        lastSpokenToken = null
        latestStatusMessage = ui(
            "Switched to ${book.bookTitle.english}.",
            "${book.bookTitle.hindi} à¤šà¥à¤¨à¥€ à¤—à¤ˆà¥¤",
        )
        render()
    }

    private fun handleTeacherModeTap() {
        if (appState.teacherModeUnlocked) {
            appState = appState.copy(teacherModeUnlocked = false)
            latestStatusMessage = ui("Teacher mode locked.", "à¤Ÿà¥€à¤šà¤° à¤®à¥‹à¤¡ à¤¬à¤‚à¤¦ à¤•à¤¿à¤¯à¤¾ à¤—à¤¯à¤¾à¥¤")
            render()
            return
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = if (appState.teacherPin.isBlank()) ui("Set a 4-digit PIN", "4 à¤…à¤‚à¤•à¥‹à¤‚ à¤•à¤¾ à¤ªà¤¿à¤¨ à¤¸à¥‡à¤Ÿ à¤•à¤°à¥‡à¤‚") else ui("Enter teacher PIN", "à¤Ÿà¥€à¤šà¤° à¤ªà¤¿à¤¨ à¤¦à¤°à¥à¤œ à¤•à¤°à¥‡à¤‚")
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        AlertDialog.Builder(this)
            .setTitle(ui("Teacher mode", "à¤Ÿà¥€à¤šà¤° à¤®à¥‹à¤¡"))
            .setView(input)
            .setPositiveButton(ui("Continue", "à¤œà¤¾à¤°à¥€ à¤°à¤–à¥‡à¤‚")) { _, _ ->
                val entered = input.text?.toString().orEmpty().trim()
                when {
                    entered.length < 4 -> {
                        latestStatusMessage = ui("Use at least 4 digits for the teacher PIN.", "à¤Ÿà¥€à¤šà¤° à¤ªà¤¿à¤¨ à¤•à¤® à¤¸à¥‡ à¤•à¤® 4 à¤…à¤‚à¤•à¥‹à¤‚ à¤•à¤¾ à¤¹à¥‹à¤¨à¤¾ à¤šà¤¾à¤¹à¤¿à¤à¥¤")
                        render()
                    }

                    appState.teacherPin.isBlank() -> {
                        appState = appState.copy(
                            teacherPin = entered,
                            teacherModeUnlocked = true,
                        )
                        latestStatusMessage = ui("Teacher mode unlocked.", "à¤Ÿà¥€à¤šà¤° à¤®à¥‹à¤¡ à¤–à¥à¤² à¤—à¤¯à¤¾à¥¤")
                        render()
                    }

                    appState.teacherPin == entered -> {
                        appState = appState.copy(teacherModeUnlocked = true)
                        latestStatusMessage = ui("Teacher mode unlocked.", "à¤Ÿà¥€à¤šà¤° à¤®à¥‹à¤¡ à¤–à¥à¤² à¤—à¤¯à¤¾à¥¤")
                        render()
                    }

                    else -> {
                        latestStatusMessage = ui("That PIN is not correct.", "à¤¯à¤¹ à¤ªà¤¿à¤¨ à¤¸à¤¹à¥€ à¤¨à¤¹à¥€à¤‚ à¤¹à¥ˆà¥¤")
                        render()
                    }
                }
            }
            .setNegativeButton(ui("Cancel", "à¤°à¤¦à¥à¤¦ à¤•à¤°à¥‡à¤‚"), null)
            .show()
    }

    private fun showAssignmentsDialog() {
        val chapterNumbers = book.topics.map { it.chapterNumber }.distinct().sorted()
        val checked = chapterNumbers.map { it in selectedProfile().assignedChapterNumbers }.toBooleanArray()
        val labels = chapterNumbers.map { ui("Chapter $it", "à¤…à¤§à¥à¤¯à¤¾à¤¯ $it") }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(ui("Assign chapters", "à¤…à¤§à¥à¤¯à¤¾à¤¯ à¤¸à¥Œà¤‚à¤ªà¥‡à¤‚"))
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(ui("Save", "à¤¸à¤¹à¥‡à¤œà¥‡à¤‚")) { _, _ ->
                val selectedChapters = chapterNumbers.filterIndexed { index, _ -> checked[index] }
                replaceSelectedProfile(selectedProfile().copy(assignedChapterNumbers = selectedChapters))
                latestStatusMessage = if (selectedChapters.isEmpty()) {
                    ui("Assignments cleared for this child.", "à¤‡à¤¸ à¤¬à¤šà¥à¤šà¥‡ à¤•à¥‡ à¤²à¤¿à¤ à¤…à¤¸à¤¾à¤‡à¤¨à¤®à¥‡à¤‚à¤Ÿ à¤¹à¤Ÿà¤¾à¤ à¤—à¤à¥¤")
                } else {
                    ui("Assigned chapters: ${selectedChapters.joinToString(", ")}", "à¤¸à¥Œà¤‚à¤ªà¥‡ à¤—à¤ à¤…à¤§à¥à¤¯à¤¾à¤¯: ${selectedChapters.joinToString(", ")}")
                }
                render()
            }
            .setNegativeButton(ui("Cancel", "à¤°à¤¦à¥à¤¦ à¤•à¤°à¥‡à¤‚"), null)
            .show()
    }

    private fun showWeakTopicsDialog() {
        val weakTopics = StudyPlanner.buildReport(book, selectedProfile(), System.currentTimeMillis()).weakTopicTitles
        val body = if (weakTopics.isEmpty()) {
            ui("No weak topics are flagged right now.", "à¤…à¤­à¥€ à¤•à¥‹à¤ˆ à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯ à¤šà¤¿à¤¹à¥à¤¨à¤¿à¤¤ à¤¨à¤¹à¥€à¤‚ à¤¹à¥ˆà¥¤")
        } else {
            weakTopics.joinToString("\n") { "- ${it.display(appState.language)}" }
        }
        AlertDialog.Builder(this)
            .setTitle(ui("Weak topics", "à¤•à¤®à¤œà¥‹à¤° à¤µà¤¿à¤·à¤¯"))
            .setMessage(body)
            .setPositiveButton(ui("OK", "à¤ à¥€à¤•"), null)
            .show()
    }

    private fun exportTeacherSummary() {
        val report = StudyPlanner.buildReport(book, selectedProfile(), System.currentTimeMillis())
        val summary = buildString {
            appendLine("${selectedProfile().name} - ${book.bookTitle.display(AppLanguage.ENGLISH)}")
            appendLine("Mastered: ${report.masteredTopics}/${report.totalTopics}")
            appendLine("Exercise path: ${report.exerciseMasteredTopics}/${report.exerciseTotalTopics}")
            appendLine("Revision due: ${report.dueRevisionTopics}")
            appendLine("Weak topics: ${report.weakTopics}")
            appendLine("Stars: ${report.totalStars}")
            appendLine("Exercise stars: ${report.exerciseStars}")
            if (report.focusTopics.isNotEmpty()) {
                appendLine("Focus topics: ${report.focusTopics.joinToString { it.english }}")
            }
        }

        startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "TeachLoop progress summary")
                putExtra(Intent.EXTRA_TEXT, summary)
            }
        )
    }

    private fun configureSpeechLanguage(): Boolean {
        englishSpeechProfile = resolveSpeechProfile(
            preferredLocales = listOf(
                Locale("en", "IN"),
                Locale.UK,
                Locale.US,
                Locale.getDefault(),
            ),
        )
        hindiSpeechProfile = resolveSpeechProfile(
            preferredLocales = listOf(
                Locale("hi", "IN"),
                Locale("hi"),
                Locale.getDefault(),
            ),
        )
        Log.d(
            TAG,
            "Resolved speech profiles english=${describeSpeechProfile(englishSpeechProfile)} hindi=${describeSpeechProfile(hindiSpeechProfile)}",
        )

        activeSpeechProfileKey = null
        val defaultProfile = defaultSpeechProfile()
        if (defaultProfile == null || !applySpeechProfile(defaultProfile, reason = "configure")) {
            ttsIssueMessage = TTS_UNAVAILABLE_MESSAGE
            return false
        }

        ttsIssueMessage = null
        return true
    }

    private fun resolveSpeechProfile(preferredLocales: List<Locale>): SpeechProfile? {
        val tts = textToSpeech ?: return null
        preferredLocales.distinct().forEach { locale ->
            val availability = tts.isLanguageAvailable(locale)
            if (isSupportedSpeechResult(availability)) {
                return SpeechProfile(
                    locale = locale,
                    voice = selectVoiceForLocale(locale),
                )
            }
        }
        return null
    }

    private fun selectVoiceForLocale(locale: Locale): Voice? {
        return textToSpeech
            ?.voices
            .orEmpty()
            .filter { voice -> voice.locale.language == locale.language }
            .sortedWith(
                compareBy<Voice>(
                    { !isExactLocaleMatch(it.locale, locale) },
                    { !isCountryMatch(it.locale, locale) },
                    { it.isNetworkConnectionRequired },
                    { isVoiceNotInstalled(it) },
                    { it.name.lowercase(Locale.US) },
                ),
            )
            .firstOrNull()
    }

    private fun defaultSpeechProfile(): SpeechProfile? {
        return when (appState.language) {
            AppLanguage.ENGLISH -> englishSpeechProfile ?: hindiSpeechProfile
            AppLanguage.HINDI -> hindiSpeechProfile ?: englishSpeechProfile
            AppLanguage.BILINGUAL -> hindiSpeechProfile ?: englishSpeechProfile
        }
    }

    private fun speechProfileForText(text: String): SpeechProfile? {
        val hasDevanagari = containsDevanagari(text)
        val hasLatinLetters = containsLatinLetters(text)
        return when {
            hasDevanagari -> hindiSpeechProfile ?: englishSpeechProfile
            hasLatinLetters -> englishSpeechProfile ?: hindiSpeechProfile
            appState.language == AppLanguage.ENGLISH -> englishSpeechProfile ?: hindiSpeechProfile
            else -> hindiSpeechProfile ?: englishSpeechProfile
        }
    }

    private fun applySpeechProfile(
        profile: SpeechProfile,
        reason: String,
    ): Boolean {
        val tts = textToSpeech ?: return false
        val desiredKey = buildSpeechProfileKey(profile)
        if (activeSpeechProfileKey == desiredKey) {
            tts.setSpeechRate(appState.narrationPace.speechRate)
            return true
        }

        val languageResult = tts.setLanguage(profile.locale)
        if (!isSupportedSpeechResult(languageResult)) {
            Log.w(TAG, "Speech locale ${profile.locale.toLanguageTag()} is not supported at playback time.")
            return false
        }

        val selectedVoice = profile.voice
        if (selectedVoice != null) {
            runCatching {
                tts.voice = selectedVoice
            }.onFailure { error ->
                Log.w(TAG, "Unable to apply voice ${selectedVoice.name}", error)
            }
        }

        tts.setSpeechRate(appState.narrationPace.speechRate)
        activeSpeechProfileKey = buildSpeechProfileKey(
            SpeechProfile(
                locale = profile.locale,
                voice = tts.voice,
            ),
        )
        Log.d(
            TAG,
            "Applied speech profile ($reason) locale=${profile.locale.toLanguageTag()} voice=${tts.voice?.name ?: "default"} network=${tts.voice?.isNetworkConnectionRequired == true}",
        )
        return true
    }

    private fun buildSpeechProfileKey(profile: SpeechProfile): String {
        return "${profile.locale.toLanguageTag()}|${profile.voice?.name.orEmpty()}"
    }

    private fun describeSpeechProfile(profile: SpeechProfile?): String {
        if (profile == null) return "unavailable"
        return "${profile.locale.toLanguageTag()}|${profile.voice?.name ?: "default"}"
    }

    private fun isSupportedSpeechResult(result: Int): Boolean {
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    private fun isExactLocaleMatch(
        candidate: Locale,
        target: Locale,
    ): Boolean {
        return candidate.language == target.language && candidate.country == target.country
    }

    private fun isCountryMatch(
        candidate: Locale,
        target: Locale,
    ): Boolean {
        return target.country.isBlank() || candidate.country == target.country
    }

    private fun isVoiceNotInstalled(voice: Voice): Boolean {
        return voice.features.any { feature -> feature.equals("notInstalled", ignoreCase = true) }
    }

    private fun containsDevanagari(text: String): Boolean {
        return text.any { character ->
            Character.UnicodeScript.of(character.code) == Character.UnicodeScript.DEVANAGARI
        }
    }

    private fun containsLatinLetters(text: String): Boolean {
        return text.any { character ->
            character.isLetter() &&
                Character.UnicodeScript.of(character.code) == Character.UnicodeScript.LATIN
        }
    }

    private fun initializeTextToSpeech(forceRestart: Boolean = false) {
        if (forceRestart) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } else if (textToSpeech != null) {
            return
        }

        ttsReady = false
        ttsInitializing = true
        textToSpeech = TextToSpeech(this, this).also { tts ->
            tts.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS onStart: utteranceId=$utteranceId")
                        val index = utteranceId?.removePrefix("topic_sentence_")?.toIntOrNull()
                        if (index != null) {
                            currentSpeechIndex = index
                            revealedSentenceCount = maxOf(revealedSentenceCount, index + 1)
                            speechPaused = false
                            runOnUiThread {
                                startChalkWriting(index)
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS onDone: utteranceId=$utteranceId")
                        val index = utteranceId?.removePrefix("topic_sentence_")?.toIntOrNull()
                        if (index != null && speakSequenceActive) {
                            runOnUiThread {
                                stopChalkWriting(completeCurrentLine = true)
                                val topic = engine.currentTopic()
                                val nextIndex = index + 1
                                if (topic == null || nextIndex > currentSpeechSentences.lastIndex) {
                                    speakSequenceActive = false
                                    speechPaused = false
                                    currentSpeechIndex = currentSpeechSentences.lastIndex.coerceAtLeast(0)
                                    revealedSentenceCount = currentSpeechSentences.size
                                    if (topic != null) {
                                        renderExplanationSentences(topic)
                                    }
                                } else {
                                    currentSpeechIndex = nextIndex
                                    speakSentenceAtCurrentIndex()
                                }
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS onError: utteranceId=$utteranceId")
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(TAG, "TTS onError: utteranceId=$utteranceId errorCode=$errorCode")
                    }
                }
            )
        }
    }

    private fun openVoiceSettings() {
        val voiceSettingsIntent = Intent(TTS_SETTINGS_ACTION)
        val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
        val targetIntent = when {
            voiceSettingsIntent.resolveActivity(packageManager) != null -> voiceSettingsIntent
            fallbackIntent.resolveActivity(packageManager) != null -> fallbackIntent
            else -> null
        }

        if (targetIntent == null) {
            latestStatusMessage = VOICE_SETTINGS_UNAVAILABLE_MESSAGE
            renderStatus()
            return
        }

        try {
            startActivity(targetIntent)
            latestStatusMessage = VOICE_SETTINGS_OPENED_MESSAGE
            renderStatus()
        } catch (error: ActivityNotFoundException) {
            Log.e(TAG, "Unable to open voice settings", error)
            latestStatusMessage = VOICE_SETTINGS_UNAVAILABLE_MESSAGE
            renderStatus()
        }
    }

    private fun showAddProfileDialog() {
        val input = EditText(this).apply {
            hint = ui("Child name", "à¤¬à¤šà¥à¤šà¥‡ à¤•à¤¾ à¤¨à¤¾à¤®")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        AlertDialog.Builder(this)
            .setTitle(ui("Add child profile", "à¤¬à¤šà¥à¤šà¥‡ à¤•à¥€ à¤ªà¥à¤°à¥‹à¤«à¤¼à¤¾à¤‡à¤² à¤œà¥‹à¤¡à¤¼à¥‡à¤‚"))
            .setView(input)
            .setPositiveButton(ui("Add", "à¤œà¥‹à¤¡à¤¼à¥‡à¤‚")) { _, _ ->
                val enteredName = input.text?.toString().orEmpty().trim()
                val profileName = if (enteredName.isBlank()) {
                    "Student ${appState.profiles.size + 1}"
                } else {
                    enteredName
                }
                val newProfile = StudentProfile(
                    id = "student_${System.currentTimeMillis()}",
                    name = profileName,
                )
                appState = appState.copy(
                    selectedProfileId = newProfile.id,
                    profiles = appState.profiles + newProfile,
                    session = SessionSnapshot(bookId = book.id),
                )
                engine.finishSession()
                latestStatusMessage = ui("$profileName was added.", "$profileName à¤œà¥‹à¤¡à¤¼à¤¾ à¤—à¤¯à¤¾à¥¤")
                render()
            }
            .setNegativeButton(ui("Cancel", "à¤°à¤¦à¥à¤¦ à¤•à¤°à¥‡à¤‚"), null)
            .show()
    }

    private fun normalize(
        snapshot: AppSnapshot,
        catalog: List<SubjectPackCatalogItem>,
    ): AppSnapshot {
        val profiles = (snapshot.profiles as? List<StudentProfile>)
            ?.map(::sanitizeProfile)
            .orEmpty()
            .ifEmpty {
            listOf(AppSnapshot.defaultProfile())
        }
        val availableBookIds = catalog.map { it.id }.toSet()
        val selectedBookId = (snapshot.selectedBookId as? String)
            ?.takeIf { it in availableBookIds }
            ?: catalog.firstOrNull()?.id
            ?: LessonRepository.BOOK_ID
        val requestedProfileId = snapshot.selectedProfileId as? String
        val selectedProfileId = if (profiles.any { it.id == requestedProfileId }) {
            requestedProfileId.orEmpty()
        } else {
            profiles.first().id
        }
        val normalizedSession = sanitizeSession(
            session = (snapshot.session as? SessionSnapshot) ?: SessionSnapshot(bookId = selectedBookId),
            selectedBookId = selectedBookId,
        )
        return AppSnapshot(
            selectedBookId = selectedBookId,
            selectedProfileId = selectedProfileId,
            language = (snapshot.language as? AppLanguage) ?: AppLanguage.ENGLISH,
            difficulty = Difficulty.EASY,
            narrationPace = (snapshot.narrationPace as? NarrationPace) ?: NarrationPace.NORMAL,
            profiles = profiles,
            teacherPin = (snapshot.teacherPin as? String).orEmpty(),
            teacherModeUnlocked = snapshot.teacherModeUnlocked,
            session = normalizedSession,
        )
    }

    private fun sanitizeSession(
        session: SessionSnapshot,
        selectedBookId: String,
    ): SessionSnapshot {
        val queueTopicIds = (session.queueTopicIds as? List<String>).orEmpty()
        val state = (session.state as? LearningState) ?: LearningState.DASHBOARD
        val mode = session.mode as? StudyMode
        return SessionSnapshot(
            bookId = (session.bookId as? String)?.takeIf { it == selectedBookId } ?: selectedBookId,
            mode = mode,
            queueTopicIds = queueTopicIds,
            queueIndex = session.queueIndex.coerceAtLeast(0),
            state = state,
            questionIndex = session.questionIndex.coerceAtLeast(0),
            explanationRepeats = session.explanationRepeats.coerceAtLeast(0),
            currentTopicStartedAt = session.currentTopicStartedAt.coerceAtLeast(0L),
            lastMistakeType = session.lastMistakeType as? MistakeType,
        )
    }

    private fun sanitizeProfile(profile: StudentProfile): StudentProfile {
        val topicProgress = ((profile.topicProgress as? Map<*, *>).orEmpty())
            .mapNotNull { (key, value) ->
                val topicId = key as? String ?: return@mapNotNull null
                val progress = value as? TopicProgress ?: return@mapNotNull null
                topicId to sanitizeTopicProgress(progress, topicId)
            }
            .toMap()

        val badges = ((profile.badges as? List<*>).orEmpty())
            .mapNotNull { it as? BadgeAward }
        val chapterTrophies = ((profile.chapterTrophies as? List<*>).orEmpty())
            .mapNotNull { it as? Number }
            .map { it.toInt() }
        val assignedChapterNumbers = ((profile.assignedChapterNumbers as? List<*>).orEmpty())
            .mapNotNull { it as? Number }
            .map { it.toInt() }

        return StudentProfile(
            id = (profile.id as? String).orEmpty().ifBlank { AppSnapshot.DEFAULT_PROFILE_ID },
            name = (profile.name as? String).orEmpty().ifBlank { "Student 1" },
            totalStars = profile.totalStars.coerceAtLeast(0),
            topicProgress = topicProgress,
            badges = badges,
            chapterTrophies = chapterTrophies,
            assignedChapterNumbers = assignedChapterNumbers,
            streakDays = profile.streakDays.coerceAtLeast(0),
            lastActiveDay = (profile.lastActiveDay as? String).orEmpty(),
            revisionRewardCount = profile.revisionRewardCount.coerceAtLeast(0),
        )
    }

    private fun sanitizeTopicProgress(
        progress: TopicProgress,
        fallbackTopicId: String,
    ): TopicProgress {
        val mistakeCounts = ((progress.mistakeCounts as? Map<*, *>).orEmpty())
            .mapNotNull { (key, value) ->
                val name = key as? String ?: return@mapNotNull null
                val count = value as? Number ?: return@mapNotNull null
                name to count.toInt()
            }
            .toMap()

        return TopicProgress(
            topicId = (progress.topicId as? String).orEmpty().ifBlank { fallbackTopicId },
            totalAttempts = progress.totalAttempts.coerceAtLeast(0),
            correctAnswers = progress.correctAnswers.coerceAtLeast(0),
            wrongAnswers = progress.wrongAnswers.coerceAtLeast(0),
            firstAttemptCorrect = progress.firstAttemptCorrect,
            firstAttemptQuestionPrompt = progress.firstAttemptQuestionPrompt?.let(::sanitizeLocalizedText),
            explanationRepeats = progress.explanationRepeats.coerceAtLeast(0),
            mastered = progress.mastered,
            starsEarned = progress.starsEarned.coerceAtLeast(0),
            reviewStage = progress.reviewStage.coerceAtLeast(0),
            lastStudiedAt = progress.lastStudiedAt.coerceAtLeast(0L),
            nextRevisionAt = progress.nextRevisionAt.coerceAtLeast(0L),
            timeSpentMillis = progress.timeSpentMillis.coerceAtLeast(0L),
            lastMistakeType = progress.lastMistakeType as? MistakeType,
            mistakeCounts = mistakeCounts,
        )
    }

    private fun selectedProfile(): StudentProfile {
        return appState.profiles.firstOrNull { it.id == appState.selectedProfileId }
            ?: appState.profiles.first()
    }

    private fun replaceSelectedProfile(updatedProfile: StudentProfile) {
        appState = appState.copy(
            profiles = appState.profiles.map { profile ->
                if (profile.id == updatedProfile.id) updatedProfile else profile
            }
        )
    }

    private fun saveState() {
        appState = appState.copy(session = engine.snapshot())
        progressStore.save(appState)
    }
    private fun splitIntoSentences(text: String): List<String> {
        return text
            .split(Regex("(?<=[.!?।])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun sectionText(
        value: String,
        compact: Boolean,
        bold: Boolean = false,
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = if (compact) 14f else 16f
            setTextColor(getColor(R.color.text_primary))
            setLineSpacing(0f, 1.1f)
            if (bold) {
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(8)
            }
        }
    }

    private fun barText(value: Int, maxValue: Int): String {
        val safeMax = maxValue.coerceAtLeast(1)
        val filled = ((value.coerceAtLeast(0).toFloat() / safeMax.toFloat()) * 10f).toInt().coerceIn(0, 10)
        return "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]"
    }

    private fun mistakeLabel(type: MistakeType): String {
        return when (type) {
            MistakeType.PLACE_VALUE -> ui("Place value", "à¤¸à¥à¤¥à¤¾à¤¨-à¤®à¥‚à¤²à¥à¤¯")
            MistakeType.UNIT_CONVERSION -> ui("Unit conversion", "à¤‡à¤•à¤¾à¤ˆ à¤°à¥‚à¤ªà¤¾à¤‚à¤¤à¤°à¤£")
            MistakeType.READING -> ui("Reading", "à¤ªà¤¢à¤¼à¤¨à¤¾")
            MistakeType.CONCEPT_CONFUSION -> ui("Concept confusion", "à¤…à¤µà¤§à¤¾à¤°à¤£à¤¾ à¤­à¥à¤°à¤®")
            MistakeType.FRACTION_COMPARE -> ui("Fraction compare", "à¤­à¤¿à¤¨à¥à¤¨ à¤¤à¥à¤²à¤¨à¤¾")
            MistakeType.ANGLE_TURN -> ui("Angle turn", "à¤•à¥‹à¤£ à¤®à¥‹à¤¡à¤¼")
            MistakeType.OPERATION_LINK -> ui("Operation link", "à¤•à¥à¤°à¤¿à¤¯à¤¾ à¤¸à¤‚à¤¬à¤‚à¤§")
            MistakeType.PATTERN_RULE -> ui("Pattern rule", "à¤ªà¥ˆà¤Ÿà¤°à¥à¤¨ à¤¨à¤¿à¤¯à¤®")
            MistakeType.MEASUREMENT_ESTIMATE -> ui("Measurement estimate", "à¤®à¤¾à¤ª à¤…à¤¨à¥à¤®à¤¾à¤¨")
            MistakeType.TIME_READING -> ui("Time reading", "à¤¸à¤®à¤¯ à¤ªà¤¢à¤¼à¤¨à¤¾")
            MistakeType.DIRECTION -> ui("Direction", "à¤¦à¤¿à¤¶à¤¾")
            MistakeType.DATA_SCALE -> ui("Data scale", "à¤¡à¥‡à¤Ÿà¤¾ à¤¸à¥à¤•à¥‡à¤²")
            MistakeType.GENERAL -> ui("General", "à¤¸à¤¾à¤®à¤¾à¤¨à¥à¤¯")
        }
    }

    private fun ui(english: String, hindi: String): String {
        val safeHindi = if (looksCorruptedHindi(hindi)) english else hindi
        return text(english, safeHindi).display(appState.language)
    }

    private fun compactUi(english: String, hindi: String): String {
        val safeHindi = if (looksCorruptedHindi(hindi)) english else hindi
        return if (appState.language == AppLanguage.HINDI) safeHindi else english
    }

    private fun sanitizeLocalizedText(value: LocalizedText): LocalizedText {
        val english = value.english.ifBlank { value.hindi }
        val hindi = value.hindi.ifBlank { english }
        return LocalizedText(english = english, hindi = hindi)
    }

    private fun reportListText(
        items: List<LocalizedText>,
        emptyEnglish: String,
        emptyHindi: String,
    ): String {
        if (items.isEmpty()) return ui(emptyEnglish, emptyHindi)
        return items.joinToString("\n") { "- ${it.display(appState.language)}" }
    }

    private fun looksCorruptedHindi(value: String): Boolean {
        return value.contains("à¤") ||
            value.contains("à¥") ||
            value.contains("â") ||
            value.contains("Ã") ||
            value.contains("Â") ||
            value.contains("�")
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private companion object {
        const val TAG = "TeachLoopTTS"
        const val TTS_SETTINGS_ACTION = "com.android.settings.TTS_SETTINGS"
        const val TTS_LOADING_MESSAGE =
            "Text-to-speech is starting. You can still read the explanation offline."
        const val TTS_UNAVAILABLE_MESSAGE =
            "Voice is unavailable on this phone right now. Reading still works offline."
        const val VOICE_SETTINGS_OPENED_MESSAGE =
            "Voice settings opened. When you come back, the app will try speech again."
        const val VOICE_SETTINGS_UNAVAILABLE_MESSAGE =
            "Could not open voice settings. Search for Text-to-speech in your phone settings."
    }
}






