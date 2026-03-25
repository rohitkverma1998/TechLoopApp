package com.book.teachloop

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
    private var currentSpeechSentences: List<String> = emptyList()
    private var currentSpeechIndex = 0
    private var speakSequenceActive = false

    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var ttsInitializing = false
    private var lastSpokenToken: String? = null
    private var ttsIssueMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                    "${selectedProfile.name} प्रोफ़ाइल चुनी गई।",
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
                latestStatusMessage = ui("Language updated.", "भाषा बदल दी गई।")
                render()
            }
        }

        binding.difficultyToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val updatedDifficulty = when (checkedId) {
                binding.difficultyMediumButton.id -> Difficulty.MEDIUM
                binding.difficultyHardButton.id -> Difficulty.HARD
                else -> Difficulty.EASY
            }
            if (updatedDifficulty != appState.difficulty) {
                appState = appState.copy(difficulty = updatedDifficulty)
                latestStatusMessage = ui("Difficulty updated.", "कठिनाई स्तर बदल दिया गया।")
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
                latestStatusMessage = ui("Voice pace updated.", "आवाज़ की गति बदली गई।")
                render()
            }
        }

        binding.startLearningButton.setOnClickListener { startMode(StudyMode.MAIN_PATH) }
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
                        "अच्छा। अब इस चरण की जाँच करते हैं।",
                    )
                }

                LearningState.EXPLAIN_TOPIC -> {
                    engine.answerUnderstood(understood = true)
                    latestStatusMessage = ui(
                        "Nice. Let us check the idea with a question.",
                        "अच्छा। अब प्रश्न से इस विचार की जाँच करते हैं।",
                    )
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
                        "कोई बात नहीं। पहले मैं यह विचार समझाता हूँ।",
                    )
                }

                LearningState.EXPLAIN_TOPIC -> {
                    engine.answerUnderstood(understood = false)
                    latestStatusMessage = ui(
                        "Let us go through the explanation once more.",
                        "चलो, इसे एक बार फिर समझते हैं।",
                    )
                }

                else -> Unit
            }
            render()
        }

        binding.playVoiceButton.setOnClickListener { speakCurrentExplanation() }
        binding.pauseVoiceButton.setOnClickListener { pauseSpeech() }
        binding.replaySentenceButton.setOnClickListener { replayCurrentSentence() }
        binding.openVoiceSettingsButton.setOnClickListener { openVoiceSettings() }
        binding.submitAnswerButton.setOnClickListener { submitAnswer() }
        binding.teacherModeButton.setOnClickListener { handleTeacherModeTap() }
        binding.teacherAssignmentsButton.setOnClickListener { showAssignmentsDialog() }
        binding.teacherWeakTopicsButton.setOnClickListener { showWeakTopicsDialog() }
        binding.teacherExportButton.setOnClickListener { exportTeacherSummary() }
        binding.teacherLockButton.setOnClickListener {
            appState = appState.copy(teacherModeUnlocked = false)
            latestStatusMessage = ui("Teacher mode locked.", "टीचर मोड बंद किया गया।")
            render()
        }

        binding.restartButton.setOnClickListener {
            engine.finishSession()
            latestStatusMessage = ui(
                "Session closed. You are back on the dashboard.",
                "सत्र बंद किया गया। आप डैशबोर्ड पर वापस हैं।",
            )
            render()
        }
    }

    private fun startMode(mode: StudyMode) {
        val queue = when (mode) {
            StudyMode.MAIN_PATH -> StudyPlanner.buildMainQueue(book, selectedProfile())
            StudyMode.REVISION -> StudyPlanner.buildRevisionQueue(book, selectedProfile(), System.currentTimeMillis())
            StudyMode.WEAK_TOPICS -> StudyPlanner.buildWeakTopicQueue(book, selectedProfile())
        }

        if (queue.isEmpty()) {
            latestStatusMessage = when (mode) {
                StudyMode.MAIN_PATH -> ui(
                    "Main path is complete for this child. Use revision or weak-topic practice next.",
                    "इस बच्चे के लिए मुख्य पथ पूरा हो चुका है। अब पुनरावृत्ति या कमजोर विषय अभ्यास करें।",
                )

                StudyMode.REVISION -> ui(
                    "No revision topic is due right now.",
                    "अभी कोई पुनरावृत्ति विषय शेष नहीं है।",
                )

                StudyMode.WEAK_TOPICS -> ui(
                    "No weak topics are pending right now.",
                    "अभी कोई कमजोर विषय लंबित नहीं है।",
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
                "अगला सीखने का पथ शुरू हो रहा है।",
            )

            StudyMode.REVISION -> ui(
                "Starting the revision path for due topics.",
                "देय विषयों की पुनरावृत्ति शुरू हो रही है।",
            )

            StudyMode.WEAK_TOPICS -> ui(
                "Starting practice for weak topics.",
                "कमजोर विषयों का अभ्यास शुरू हो रहा है।",
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
        latestStatusMessage = ui(
            "Progress reset for ${resetProfile.name}.",
            "${resetProfile.name} की प्रगति रीसेट कर दी गई।",
        )
        render()
    }

    private fun submitAnswer() {
        val topic = engine.currentTopic() ?: return
        val explanationRepeats = engine.currentExplanationRepeats()
        val previousProfile = selectedProfile()
        val now = System.currentTimeMillis()

        val result = when (engine.currentQuestion(appState.difficulty)?.type) {
            QuestionType.MULTIPLE_CHOICE -> {
                val checkedId = binding.answerOptions.checkedRadioButtonId
                if (checkedId == View.NO_ID) {
                    latestStatusMessage = ui(
                        "Please choose one option before submitting.",
                        "जमा करने से पहले एक विकल्प चुनिए।",
                    )
                    renderStatus()
                    return
                }

                val checkedView = findViewById<RadioButton>(checkedId)
                val selectedIndex = binding.answerOptions.indexOfChild(checkedView)
                engine.submitChoice(selectedIndex, appState.difficulty, now)
            }

            QuestionType.TEXT_INPUT -> {
                val answer = binding.answerInputEditText.text?.toString().orEmpty()
                if (answer.isBlank()) {
                    latestStatusMessage = ui(
                        "Please type an answer before submitting.",
                        "जमा करने से पहले उत्तर लिखिए।",
                    )
                    renderStatus()
                    return
                }
                engine.submitText(answer, appState.difficulty, now)
            }

            null -> return
        }

        val updatedProfile = StudyPlanner.updateProfileAfterAttempt(
            book = book,
            profile = previousProfile,
            topic = topic,
            difficulty = appState.difficulty,
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
                "$baseMessage\n${ui("Stars earned: $starsEarned", "मिले हुए सितारे: $starsEarned")}"
            } else {
                baseMessage
            }
        }

        val detailParts = listOfNotNull(
            result.wrongReason?.display(appState.language),
            result.supportExample?.display(appState.language)?.let {
                "${ui("Example", "उदाहरण")}: $it"
            },
        )
        return listOf(baseMessage, detailParts.joinToString("\n"))
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    private fun render() {
        saveState()
        if (engine.session.state != LearningState.EXPLAIN_TOPIC) {
            textToSpeech?.stop()
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
        binding.appTitleText.text = ui("TeachLoop", "टीचलूप")
        binding.bookTitleText.text = book.bookTitle.display(appState.language)
        binding.bookLabelText.text = ui("Book pack", "बुक पैक")
        binding.languageLabelText.text = ui("Language", "भाषा")
        binding.difficultyLabelText.text = ui("Difficulty", "कठिनाई")
        binding.starsChipText.text = ui(
            "Stars ${selectedProfile().totalStars}",
            "सितारे ${selectedProfile().totalStars}",
        )
        binding.modeChipText.text = when (engine.session.mode) {
            StudyMode.MAIN_PATH -> ui("Main path", "मुख्य पथ")
            StudyMode.REVISION -> ui("Revision mode", "पुनरावृत्ति")
            StudyMode.WEAK_TOPICS -> ui("Weak topics", "कमजोर विषय")
            null -> ui(
                "Mastered ${report.masteredTopics}/${report.totalTopics}",
                "सीखे गए ${report.masteredTopics}/${report.totalTopics}",
            )
        }

        binding.languageEnglishButton.text = ui("English", "अंग्रेज़ी")
        binding.languageHindiButton.text = ui("Hindi", "हिंदी")
        binding.languageBilingualButton.text = ui("Both", "दोनों")
        binding.difficultyEasyButton.text = ui("Easy", "आसान")
        binding.difficultyMediumButton.text = ui("Medium", "मध्यम")
        binding.difficultyHardButton.text = ui("Hard", "कठिन")

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

        val difficultyButtonId = when (appState.difficulty) {
            Difficulty.EASY -> binding.difficultyEasyButton.id
            Difficulty.MEDIUM -> binding.difficultyMediumButton.id
            Difficulty.HARD -> binding.difficultyHardButton.id
        }
        binding.difficultyToggle.check(difficultyButtonId)

        binding.resetProgressButton.text = ui("Reset child progress", "बच्चे की प्रगति रीसेट करें")
        binding.reportButton.text = if (reportExpanded) {
            ui("Hide report", "रिपोर्ट छुपाएँ")
        } else {
            ui("Show report", "रिपोर्ट दिखाएँ")
        }
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
        val revisionCount = StudyPlanner.buildRevisionQueue(book, selectedProfile(), System.currentTimeMillis()).size
        val weakCount = StudyPlanner.buildWeakTopicQueue(book, selectedProfile()).size

        val dashboardVisible = !engine.hasActiveSession()
        binding.dashboardCard.isVisible = dashboardVisible
        binding.dashboardTitleText.text = ui(
            "Choose the next learning mission",
            "अगला सीखने का मिशन चुनिए",
        )
        binding.dashboardBodyText.text = listOf(
            ui(
                "${selectedProfile().name} has mastered ${report.masteredTopics} of ${report.totalTopics} study steps.",
                "${selectedProfile().name} ने ${report.totalTopics} में से ${report.masteredTopics} अध्ययन चरण पूरे किए हैं।",
            ),
            ui(
                "$revisionCount revision topics are due, and $weakCount topics still need extra support.",
                "$revisionCount पुनरावृत्ति विषय देय हैं और $weakCount विषयों को अभी अतिरिक्त सहायता चाहिए।",
            ),
        ).joinToString("\n\n")

        binding.startLearningButton.text = if (mainQueueCount > 0) {
            ui(
                "Continue main path ($mainQueueCount steps)",
                "मुख्य पथ जारी रखें ($mainQueueCount चरण)",
            )
        } else {
            ui("Main path complete", "मुख्य पथ पूरा")
        }
        binding.startLearningButton.isEnabled = mainQueueCount > 0

        binding.revisionButton.text = ui(
            "Revision mode ($revisionCount due)",
            "पुनरावृत्ति ($revisionCount देय)",
        )
        binding.revisionButton.isEnabled = revisionCount > 0

        binding.weakTopicsButton.text = ui(
            "Practice weak topics ($weakCount)",
            "कमजोर विषय अभ्यास ($weakCount)",
        )
        binding.weakTopicsButton.isEnabled = weakCount > 0
    }

    private fun renderTeacherPanel(report: ReportSummary) {
        val visible = !engine.hasActiveSession() && appState.teacherModeUnlocked
        binding.teacherCard.isVisible = visible
        if (!visible) return

        val assignments = selectedProfile().assignedChapterNumbers.sorted()
        val assignmentText = if (assignments.isEmpty()) {
            ui("No chapters assigned yet.", "अभी कोई अध्याय सौंपा नहीं गया है।")
        } else {
            ui(
                "Assigned chapters: ${assignments.joinToString(", ")}",
                "सौंपे गए अध्याय: ${assignments.joinToString(", ")}",
            )
        }

        binding.teacherTitleText.text = ui("Teacher panel", "टीचर पैनल")
        binding.teacherSummaryText.text = listOf(
            ui(
                "${selectedProfile().name} is studying ${book.bookTitle.display(appState.language)}.",
                "${selectedProfile().name}, ${book.bookTitle.display(appState.language)} पढ़ रहा/रही है।",
            ),
            assignmentText,
            ui(
                "Weak topics: ${report.weakTopics} | Revision due: ${report.dueRevisionTopics}",
                "कमजोर विषय: ${report.weakTopics} | देय पुनरावृत्ति: ${report.dueRevisionTopics}",
            ),
        ).joinToString("\n\n")
        binding.teacherAssignmentsButton.text = ui("Assign chapters", "अध्याय सौंपें")
        binding.teacherWeakTopicsButton.text = ui("View weak topics", "कमजोर विषय देखें")
        binding.teacherExportButton.text = ui("Export summary", "सारांश भेजें")
        binding.teacherLockButton.text = ui("Lock teacher mode", "टीचर मोड बंद करें")
    }

    private fun renderReport(report: ReportSummary) {
        binding.reportCard.isVisible = !engine.hasActiveSession() && reportExpanded
        binding.reportTitleText.text = ui("Parent and teacher report", "अभिभावक और शिक्षक रिपोर्ट")
        val focusText = if (report.focusTopics.isEmpty()) {
            ui("No high-need topics yet.", "अभी कोई विशेष कठिन विषय नहीं है।")
        } else {
            report.focusTopics.joinToString("\n") { "- ${it.display(appState.language)}" }
        }

        binding.reportBodyText.text = listOf(
            ui(
                "Mastered steps: ${report.masteredTopics}/${report.totalTopics}",
                "सीखे गए चरण: ${report.masteredTopics}/${report.totalTopics}",
            ),
            ui(
                "Due revisions: ${report.dueRevisionTopics}",
                "देय पुनरावृत्तियाँ: ${report.dueRevisionTopics}",
            ),
            ui("Weak topics: ${report.weakTopics}", "कमजोर विषय: ${report.weakTopics}"),
            ui(
                "Topics needing repeated explanation: ${report.supportHeavyTopics}",
                "बार-बार समझाने वाले विषय: ${report.supportHeavyTopics}",
            ),
            ui("Total stars: ${report.totalStars}", "कुल सितारे: ${report.totalStars}"),
            "${ui("Focus topics", "ध्यान देने वाले विषय")}:\n$focusText",
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
        if (!sessionActive || topic == null) {
            return
        }

        binding.progressBar.max = engine.totalQueuedTopics()
        binding.progressBar.progress = engine.currentTopicPosition().coerceAtLeast(1)
        binding.progressChipText.text = ui(
            "Step ${engine.currentTopicPosition()} of ${engine.totalQueuedTopics()}",
            "चरण ${engine.currentTopicPosition()} / ${engine.totalQueuedTopics()}",
        )
        binding.chapterLabelText.text = ui("Chapter ${topic.chapterNumber}", "अध्याय ${topic.chapterNumber}")
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
        binding.feedbackCard.isVisible = false
        binding.quizCard.isVisible = false
        binding.decisionContainer.isVisible = true
        binding.positiveButton.text = ui("Yes, I know it", "हाँ, मुझे आता है")
        binding.negativeButton.text = ui("No, teach me", "नहीं, समझाइए")
    }

    private fun renderExplanation(topic: StudyTopic) {
        binding.promptText.text = ui(
            "Here is the lesson. Have you understood this topic?",
            "यह पाठ है। क्या आपको यह विषय समझ आया?",
        )
        binding.explanationCard.isVisible = true
        binding.quizCard.isVisible = false
        binding.decisionContainer.isVisible = true
        binding.positiveButton.text = ui("Yes, I understood", "हाँ, समझ आया")
        binding.negativeButton.text = ui("No, explain again", "नहीं, फिर समझाइए")

        binding.explanationTitleText.text = topic.explanationTitle.display(appState.language)
        renderExplanationSentences(topic)
        binding.examplesLabelText.text = ui("Examples", "उदाहरण")
        binding.examplesText.text = topic.examples.joinToString("\n") {
            "- ${it.display(appState.language)}"
        }
        binding.playVoiceButton.text = ui("Play explanation aloud", "व्याख्या सुनें")
        binding.openVoiceSettingsButton.text = ui("Open voice settings", "वॉइस सेटिंग खोलें")
        binding.voiceLabelText.text = ui("Voice coach", "वॉइस कोच")
        binding.pauseVoiceButton.text = ui("Pause voice", "आवाज़ रोकें")
        binding.replaySentenceButton.text = ui("Replay sentence", "वाक्य फिर चलाएँ")
        binding.paceNormalButton.text = ui("Normal", "सामान्य")
        binding.paceSlowButton.text = ui("Slow", "धीमा")
        binding.paceToggle.check(
            when (appState.narrationPace) {
                NarrationPace.NORMAL -> binding.paceNormalButton.id
                NarrationPace.SLOW -> binding.paceSlowButton.id
            }
        )
        renderFeedbackCard()
        renderVisuals(topic.visuals)
    }

    private fun renderQuiz(topic: StudyTopic) {
        val question = engine.currentQuestion(appState.difficulty) ?: return

        binding.promptText.text = ui("Answer this check question.", "इस जाँच प्रश्न का उत्तर दें।")
        binding.explanationCard.isVisible = false
        binding.feedbackCard.isVisible = false
        binding.quizCard.isVisible = true
        binding.decisionContainer.isVisible = false

        binding.questionTitleText.text = ui(
            "${topic.subtopicTitle.display(appState.language)} - ${difficultyLabel()}",
            "${topic.subtopicTitle.display(appState.language)} - ${difficultyLabel()}",
        )
        binding.questionPromptText.text = question.prompt.display(appState.language)
        binding.answerInputLayout.hint = ui("Type your answer", "अपना उत्तर लिखिए")
        binding.submitAnswerButton.text = ui("Submit answer", "उत्तर जमा करें")

        val hintText = question.hint?.display(appState.language).orEmpty()
        binding.hintText.isVisible = hintText.isNotBlank()
        binding.hintText.text = if (hintText.isBlank()) {
            ""
        } else {
            "${ui("Hint", "संकेत")}: $hintText"
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

    private fun renderExplanationSentences(topic: StudyTopic) {
        val sentences = topic.explanationParagraphs
            .flatMap { splitIntoSentences(it.display(appState.language)) }
            .ifEmpty { listOf(topic.explanationTitle.display(appState.language)) }
        currentSpeechSentences = sentences
        currentSpeechIndex = currentSpeechIndex.coerceIn(0, sentences.lastIndex)
        binding.explanationSentenceContainer.removeAllViews()
        sentences.forEachIndexed { index, sentence ->
            val sentenceView = TextView(this).apply {
                text = sentence
                textSize = 15f
                setTextColor(getColor(R.color.text_primary))
                setLineSpacing(0f, 1.15f)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                if (index == currentSpeechIndex) {
                    setBackgroundResource(R.drawable.status_surface)
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
    }

    private fun renderFeedbackCard() {
        val result = latestQuizResult
        val visible = engine.session.state == LearningState.EXPLAIN_TOPIC && result != null
        binding.feedbackCard.isVisible = visible
        if (!visible || result == null) return

        binding.feedbackTitleText.text = result.reteachTitle?.display(appState.language)
            ?: ui("Let us fix the confusion", "चलो भ्रम दूर करें")
        binding.feedbackBodyText.text = listOfNotNull(
            result.wrongReason?.display(appState.language),
            result.supportExample?.display(appState.language)?.let { "${ui("Example", "उदाहरण")}: $it" },
            result.reteachParagraphs.takeIf { it.isNotEmpty() }?.joinToString("\n") { it.display(appState.language) },
        ).joinToString("\n\n")
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
        binding.masteryMapContainer.addView(sectionText(ui("Chapter mastery", "अध्याय मास्टरी"), false, true))
        report.chapterMastery.forEach { chapter ->
            val line = "${ui("Chapter", "अध्याय")} ${chapter.chapterNumber}: ${barText(chapter.masteredTopics, chapter.totalTopics)} ${chapter.masteredTopics}/${chapter.totalTopics}"
            binding.masteryMapContainer.addView(sectionText(line, true))
        }
    }

    private fun renderMistakeBreakdown(report: ReportSummary) {
        binding.mistakeContainer.removeAllViews()
        binding.mistakeContainer.addView(sectionText(ui("Common mistake patterns", "आम गलती पैटर्न"), false, true))
        val lines = if (report.topMistakes.isEmpty()) {
            listOf(ui("No major mistake pattern yet.", "अभी कोई बड़ा गलती पैटर्न नहीं है।"))
        } else {
            report.topMistakes.map { "${mistakeLabel(it.type)}: ${it.count}" }
        }
        lines.forEach { binding.mistakeContainer.addView(sectionText(it, true)) }
    }

    private fun renderBadgeSection(report: ReportSummary) {
        binding.badgeContainer.removeAllViews()
        binding.badgeContainer.addView(sectionText(ui("Badges and rewards", "बैज और पुरस्कार"), false, true))
        val summaryLines = mutableListOf(
            ui("Streak days: ${report.streakDays}", "लगातार दिन: ${report.streakDays}"),
            ui("Revision rewards: ${report.revisionRewardCount}", "रिविजन पुरस्कार: ${report.revisionRewardCount}"),
            ui("Chapter trophies: ${report.chapterTrophies.size}", "अध्याय ट्रॉफियाँ: ${report.chapterTrophies.size}"),
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

        binding.completionTitleText.text = ui("Session complete", "सत्र पूरा")
        binding.completionBodyText.text = when (engine.session.mode) {
            StudyMode.MAIN_PATH -> ui(
                "Great work. This child finished the current main-path queue. You can return to the dashboard for revision, weak-topic practice, or the report.",
                "बहुत अच्छा। इस बच्चे ने अभी का मुख्य अध्ययन-पथ पूरा कर लिया है। अब आप डैशबोर्ड पर लौटकर पुनरावृत्ति, कमजोर विषय अभ्यास, या रिपोर्ट देख सकते हैं।",
            )

            StudyMode.REVISION -> ui(
                "Revision topics are complete for now. The next due set will appear automatically later.",
                "अभी के लिए पुनरावृत्ति विषय पूरे हो गए हैं। अगला देय सेट बाद में अपने-आप दिखाई देगा।",
            )

            StudyMode.WEAK_TOPICS -> ui(
                "Weak-topic practice is complete for now. Check the report to see what still needs support.",
                "अभी के लिए कमजोर विषयों का अभ्यास पूरा हो गया है। आगे किसे सहायता चाहिए, यह रिपोर्ट में देखिए।",
            )

            null -> ui("You can return to the dashboard now.", "अब आप डैशबोर्ड पर लौट सकते हैं।")
        }
        binding.restartButton.text = ui("Back to dashboard", "डैशबोर्ड पर वापस")
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

        speakCurrentExplanation()
        lastSpokenToken = token
    }

    private fun speakCurrentExplanation() {
        val topic = engine.currentTopic() ?: return
        if (!ttsReady) {
            latestStatusMessage = TTS_LOADING_MESSAGE
            renderStatus()
            return
        }

        currentSpeechSentences = topic.explanationParagraphs
            .flatMap { splitIntoSentences(it.display(appState.language)) }
            .ifEmpty { listOf(topic.explanationTitle.display(appState.language)) }
        currentSpeechIndex = 0
        speakSequenceActive = true
        renderExplanationSentences(topic)
        textToSpeech?.stop()
        textToSpeech?.setSpeechRate(appState.narrationPace.speechRate)
        speakSentenceAtCurrentIndex()
    }

    private fun pauseSpeech() {
        speakSequenceActive = false
        textToSpeech?.stop()
        latestStatusMessage = ui("Voice paused.", "आवाज़ रोकी गई।")
        renderStatus()
    }

    private fun replayCurrentSentence() {
        if (!ttsReady) {
            latestStatusMessage = TTS_LOADING_MESSAGE
            renderStatus()
            return
        }

        if (currentSpeechSentences.isEmpty()) {
            speakCurrentExplanation()
            return
        }

        val sentence = currentSpeechSentences.getOrElse(currentSpeechIndex) { currentSpeechSentences.first() }
        textToSpeech?.stop()
        textToSpeech?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "replay_$currentSpeechIndex")
        latestStatusMessage = ui("Replaying the current sentence.", "वर्तमान वाक्य फिर से चल रहा है।")
        renderStatus()
    }

    private fun speakSentenceAtCurrentIndex() {
        if (currentSpeechSentences.isEmpty()) return
        val sentence = currentSpeechSentences.getOrElse(currentSpeechIndex) { currentSpeechSentences.last() }
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
        reportExpanded = false
        lastSpokenToken = null
        latestStatusMessage = ui(
            "Switched to ${book.bookTitle.english}.",
            "${book.bookTitle.hindi} चुनी गई।",
        )
        render()
    }

    private fun handleTeacherModeTap() {
        if (appState.teacherModeUnlocked) {
            appState = appState.copy(teacherModeUnlocked = false)
            latestStatusMessage = ui("Teacher mode locked.", "टीचर मोड बंद किया गया।")
            render()
            return
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = if (appState.teacherPin.isBlank()) ui("Set a 4-digit PIN", "4 अंकों का पिन सेट करें") else ui("Enter teacher PIN", "टीचर पिन दर्ज करें")
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        AlertDialog.Builder(this)
            .setTitle(ui("Teacher mode", "टीचर मोड"))
            .setView(input)
            .setPositiveButton(ui("Continue", "जारी रखें")) { _, _ ->
                val entered = input.text?.toString().orEmpty().trim()
                when {
                    entered.length < 4 -> {
                        latestStatusMessage = ui("Use at least 4 digits for the teacher PIN.", "टीचर पिन कम से कम 4 अंकों का होना चाहिए।")
                        render()
                    }

                    appState.teacherPin.isBlank() -> {
                        appState = appState.copy(
                            teacherPin = entered,
                            teacherModeUnlocked = true,
                        )
                        latestStatusMessage = ui("Teacher mode unlocked.", "टीचर मोड खुल गया।")
                        render()
                    }

                    appState.teacherPin == entered -> {
                        appState = appState.copy(teacherModeUnlocked = true)
                        latestStatusMessage = ui("Teacher mode unlocked.", "टीचर मोड खुल गया।")
                        render()
                    }

                    else -> {
                        latestStatusMessage = ui("That PIN is not correct.", "यह पिन सही नहीं है।")
                        render()
                    }
                }
            }
            .setNegativeButton(ui("Cancel", "रद्द करें"), null)
            .show()
    }

    private fun showAssignmentsDialog() {
        val chapterNumbers = book.topics.map { it.chapterNumber }.distinct().sorted()
        val checked = chapterNumbers.map { it in selectedProfile().assignedChapterNumbers }.toBooleanArray()
        val labels = chapterNumbers.map { ui("Chapter $it", "अध्याय $it") }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(ui("Assign chapters", "अध्याय सौंपें"))
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(ui("Save", "सहेजें")) { _, _ ->
                val selectedChapters = chapterNumbers.filterIndexed { index, _ -> checked[index] }
                replaceSelectedProfile(selectedProfile().copy(assignedChapterNumbers = selectedChapters))
                latestStatusMessage = if (selectedChapters.isEmpty()) {
                    ui("Assignments cleared for this child.", "इस बच्चे के लिए असाइनमेंट हटाए गए।")
                } else {
                    ui("Assigned chapters: ${selectedChapters.joinToString(", ")}", "सौंपे गए अध्याय: ${selectedChapters.joinToString(", ")}")
                }
                render()
            }
            .setNegativeButton(ui("Cancel", "रद्द करें"), null)
            .show()
    }

    private fun showWeakTopicsDialog() {
        val weakTopics = StudyPlanner.buildReport(book, selectedProfile(), System.currentTimeMillis()).weakTopicTitles
        val body = if (weakTopics.isEmpty()) {
            ui("No weak topics are flagged right now.", "अभी कोई कमजोर विषय चिह्नित नहीं है।")
        } else {
            weakTopics.joinToString("\n") { "- ${it.display(appState.language)}" }
        }
        AlertDialog.Builder(this)
            .setTitle(ui("Weak topics", "कमजोर विषय"))
            .setMessage(body)
            .setPositiveButton(ui("OK", "ठीक"), null)
            .show()
    }

    private fun exportTeacherSummary() {
        val report = StudyPlanner.buildReport(book, selectedProfile(), System.currentTimeMillis())
        val summary = buildString {
            appendLine("${selectedProfile().name} - ${book.bookTitle.display(AppLanguage.ENGLISH)}")
            appendLine("Mastered: ${report.masteredTopics}/${report.totalTopics}")
            appendLine("Revision due: ${report.dueRevisionTopics}")
            appendLine("Weak topics: ${report.weakTopics}")
            appendLine("Stars: ${report.totalStars}")
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
        val preferredLocales = when (appState.language) {
            AppLanguage.HINDI -> listOf(Locale("hi", "IN"), Locale.forLanguageTag("hi-IN"), Locale.getDefault())
            AppLanguage.BILINGUAL -> listOf(Locale("hi", "IN"), Locale.US, Locale.getDefault())
            AppLanguage.ENGLISH -> listOf(Locale.US, Locale.UK, Locale.getDefault())
        }.distinct()

        preferredLocales.forEach { locale ->
            val result = textToSpeech?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setSpeechRate(appState.narrationPace.speechRate)
                return true
            }
        }

        ttsIssueMessage = TTS_UNAVAILABLE_MESSAGE
        return false
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
                            runOnUiThread {
                                engine.currentTopic()?.let(::renderExplanationSentences)
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS onDone: utteranceId=$utteranceId")
                        val index = utteranceId?.removePrefix("topic_sentence_")?.toIntOrNull()
                        if (index != null && speakSequenceActive) {
                            currentSpeechIndex = index + 1
                            runOnUiThread {
                                val topic = engine.currentTopic()
                                if (topic == null || currentSpeechIndex > currentSpeechSentences.lastIndex) {
                                    speakSequenceActive = false
                                } else {
                                    renderExplanationSentences(topic)
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
            hint = ui("Child name", "बच्चे का नाम")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        AlertDialog.Builder(this)
            .setTitle(ui("Add child profile", "बच्चे की प्रोफ़ाइल जोड़ें"))
            .setView(input)
            .setPositiveButton(ui("Add", "जोड़ें")) { _, _ ->
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
                latestStatusMessage = ui("$profileName was added.", "$profileName जोड़ा गया।")
                render()
            }
            .setNegativeButton(ui("Cancel", "रद्द करें"), null)
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
            difficulty = (snapshot.difficulty as? Difficulty) ?: Difficulty.EASY,
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

    private fun difficultyLabel(): String {
        return when (appState.difficulty) {
            Difficulty.EASY -> ui("Easy", "आसान")
            Difficulty.MEDIUM -> ui("Medium", "मध्यम")
            Difficulty.HARD -> ui("Hard", "कठिन")
        }
    }

    private fun splitIntoSentences(text: String): List<String> {
        return text
            .split(Regex("(?<=[.!?।])\\s+"))
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
        return "█".repeat(filled) + "░".repeat(10 - filled)
    }

    private fun mistakeLabel(type: MistakeType): String {
        return when (type) {
            MistakeType.PLACE_VALUE -> ui("Place value", "स्थान-मूल्य")
            MistakeType.UNIT_CONVERSION -> ui("Unit conversion", "इकाई रूपांतरण")
            MistakeType.READING -> ui("Reading", "पढ़ना")
            MistakeType.CONCEPT_CONFUSION -> ui("Concept confusion", "अवधारणा भ्रम")
            MistakeType.FRACTION_COMPARE -> ui("Fraction compare", "भिन्न तुलना")
            MistakeType.ANGLE_TURN -> ui("Angle turn", "कोण मोड़")
            MistakeType.OPERATION_LINK -> ui("Operation link", "क्रिया संबंध")
            MistakeType.PATTERN_RULE -> ui("Pattern rule", "पैटर्न नियम")
            MistakeType.MEASUREMENT_ESTIMATE -> ui("Measurement estimate", "माप अनुमान")
            MistakeType.TIME_READING -> ui("Time reading", "समय पढ़ना")
            MistakeType.DIRECTION -> ui("Direction", "दिशा")
            MistakeType.DATA_SCALE -> ui("Data scale", "डेटा स्केल")
            MistakeType.GENERAL -> ui("General", "सामान्य")
        }
    }

    private fun ui(english: String, hindi: String): String {
        return text(english, hindi).display(appState.language)
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
