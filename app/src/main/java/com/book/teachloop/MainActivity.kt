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
    private var profileAdapter: ArrayAdapter<String>? = null
    private var isSyncingProfileSpinner = false
    private var reportExpanded = false
    private var latestStatusMessage: String? = null

    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var ttsInitializing = false
    private var lastSpokenToken: String? = null
    private var ttsIssueMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        book = LessonRepository.grade5MathMelaBook()
        progressStore = ProgressStore(this)
        appState = normalize(progressStore.load())
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
        binding.openVoiceSettingsButton.setOnClickListener { openVoiceSettings() }
        binding.submitAnswerButton.setOnClickListener { submitAnswer() }

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
                engine.submitChoice(selectedIndex, appState.difficulty)
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
                engine.submitText(answer, appState.difficulty)
            }

            null -> return
        }

        val updatedProfile = StudyPlanner.updateProfileAfterAttempt(
            profile = previousProfile,
            topic = topic,
            difficulty = appState.difficulty,
            mode = engine.session.mode,
            correct = result.correct,
            explanationRepeats = explanationRepeats,
            now = System.currentTimeMillis(),
        )
        replaceSelectedProfile(updatedProfile)

        val starsEarned = updatedProfile.totalStars - previousProfile.totalStars
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
        renderReport(report)
        renderSession()
        renderCompletion()
        renderStatus()
        maybeSpeakExplanation()
    }

    private fun renderHeader(report: ReportSummary) {
        binding.appTitleText.text = ui("TeachLoop", "टीचलूप")
        binding.bookTitleText.text = book.bookTitle.display(appState.language)
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

        syncProfileSpinner()
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

    private fun renderReport(report: ReportSummary) {
        binding.reportCard.isVisible = !engine.hasActiveSession() && reportExpanded
        binding.reportTitleText.text = ui("Parent and teacher report", "अभिभावक और शिक्षक रिपोर्ट")
        val focusText = if (report.focusTopics.isEmpty()) {
            ui("No high-need topics yet.", "अभी कोई विशेष कठिन विषय नहीं है।")
        } else {
            report.focusTopics.joinToString("\n") { "• ${it.display(appState.language)}" }
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
        ).joinToString(" • ")

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
        binding.explanationBodyText.text = topic.explanationParagraphs.joinToString("\n\n") {
            it.display(appState.language)
        }
        binding.examplesLabelText.text = ui("Examples", "उदाहरण")
        binding.examplesText.text = topic.examples.joinToString("\n") {
            "• ${it.display(appState.language)}"
        }
        binding.playVoiceButton.text = ui("Play explanation aloud", "व्याख्या सुनें")
        binding.openVoiceSettingsButton.text = ui("Open voice settings", "वॉइस सेटिंग खोलें")
        renderVisuals(topic.visuals)
    }

    private fun renderQuiz(topic: StudyTopic) {
        val question = engine.currentQuestion(appState.difficulty) ?: return

        binding.promptText.text = ui("Answer this check question.", "इस जाँच प्रश्न का उत्तर दें।")
        binding.explanationCard.isVisible = false
        binding.quizCard.isVisible = true
        binding.decisionContainer.isVisible = false

        binding.questionTitleText.text = ui(
            "${topic.subtopicTitle.display(appState.language)} • ${difficultyLabel()}",
            "${topic.subtopicTitle.display(appState.language)} • ${difficultyLabel()}",
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

        val spokenText = buildString {
            append(topic.subtopicTitle.english)
            append(". ")
            topic.explanationParagraphs.forEach {
                append(it.english)
                append(" ")
            }
            if (topic.examples.isNotEmpty()) {
                append("Examples. ")
                topic.examples.forEach {
                    append(it.english)
                    append(". ")
                }
            }
        }

        textToSpeech?.stop()
        textToSpeech?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, topic.id)
    }

    private fun configureSpeechLanguage(): Boolean {
        val preferredLocales = listOf(
            Locale.US,
            Locale.UK,
            Locale.getDefault(),
        ).distinct()

        preferredLocales.forEach { locale ->
            val result = textToSpeech?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
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
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS onDone: utteranceId=$utteranceId")
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

    private fun normalize(snapshot: AppSnapshot): AppSnapshot {
        val profiles = if (snapshot.profiles.isEmpty()) {
            listOf(AppSnapshot.defaultProfile())
        } else {
            snapshot.profiles
        }
        val selectedProfileId = if (profiles.any { it.id == snapshot.selectedProfileId }) {
            snapshot.selectedProfileId
        } else {
            profiles.first().id
        }
        return snapshot.copy(
            selectedBookId = book.id,
            selectedProfileId = selectedProfileId,
            profiles = profiles,
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
