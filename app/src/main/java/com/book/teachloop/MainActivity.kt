package com.book.teachloop

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.book.teachloop.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var engine: LessonEngine
    private lateinit var progressStore: ProgressStore

    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var ttsInitializing = false
    private var lastSpokenToken: String? = null
    private var latestStatusMessage: String? = null
    private var ttsIssueMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressStore = ProgressStore(this)
        engine = LessonEngine(LessonRepository.grade5MathMela())
        engine.restore(progressStore.load())

        setupStaticText()
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
            Log.d(TAG, "TTS ready=$ttsReady issue=$ttsIssueMessage")
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

    private fun setupStaticText() {
        binding.appTitleText.text = getString(R.string.app_name)
        binding.bookTitleText.text = LessonRepository.BOOK_TITLE
    }

    private fun setupListeners() {
        binding.positiveButton.setOnClickListener {
            when (engine.state) {
                LearningState.ASK_IF_KNOWN -> {
                    engine.answerKnowTopic(knowsTopic = true)
                    latestStatusMessage = null
                }

                LearningState.EXPLAIN_TOPIC -> {
                    engine.answerUnderstood(understood = true)
                    latestStatusMessage = "Nice. Let us check this topic with a question."
                }

                LearningState.TAKE_QUIZ,
                LearningState.COMPLETED,
                -> Unit
            }
            render()
        }

        binding.negativeButton.setOnClickListener {
            when (engine.state) {
                LearningState.ASK_IF_KNOWN -> {
                    engine.answerKnowTopic(knowsTopic = false)
                    latestStatusMessage = "No problem. I will teach the idea first."
                }

                LearningState.EXPLAIN_TOPIC -> {
                    engine.answerUnderstood(understood = false)
                    latestStatusMessage = "Let us go through the explanation once more."
                }

                LearningState.TAKE_QUIZ,
                LearningState.COMPLETED,
                -> Unit
            }
            render()
        }

        binding.playVoiceButton.setOnClickListener {
            speakCurrentExplanation()
        }

        binding.openVoiceSettingsButton.setOnClickListener {
            openVoiceSettings()
        }

        binding.submitAnswerButton.setOnClickListener {
            submitAnswer()
        }

        binding.resetProgressButton.setOnClickListener {
            restartFromChapterOne()
        }

        binding.restartButton.setOnClickListener {
            restartFromChapterOne()
        }
    }

    private fun submitAnswer() {
        val question = engine.currentQuestion() ?: return

        val result = when (question.type) {
            QuestionType.MULTIPLE_CHOICE -> {
                val checkedId = binding.answerOptions.checkedRadioButtonId
                if (checkedId == View.NO_ID) {
                    latestStatusMessage = "Please choose one option before submitting."
                    renderStatus()
                    return
                }

                val checkedView = findViewById<RadioButton>(checkedId)
                val selectedIndex = binding.answerOptions.indexOfChild(checkedView)
                engine.submitChoice(selectedIndex)
            }

            QuestionType.TEXT_INPUT -> {
                val answer = binding.answerInputEditText.text?.toString().orEmpty()
                if (answer.isBlank()) {
                    latestStatusMessage = "Please type an answer before submitting."
                    renderStatus()
                    return
                }
                engine.submitText(answer)
            }
        }

        latestStatusMessage = result.message
        binding.answerInputEditText.text?.clear()
        render()
    }

    private fun render() {
        progressStore.save(engine.snapshot())
        if (engine.state != LearningState.EXPLAIN_TOPIC) {
            textToSpeech?.stop()
        }

        val lesson = engine.currentLesson()
        val totalLessons = engine.totalLessons()
        val currentDisplayIndex = if (engine.state == LearningState.COMPLETED) totalLessons else engine.currentIndex + 1

        binding.progressBar.max = totalLessons
        binding.progressBar.progress = engine.currentIndex.coerceIn(0, totalLessons)
        binding.progressChipText.text = "Topic $currentDisplayIndex of $totalLessons"

        if (lesson == null || engine.state == LearningState.COMPLETED) {
            renderCompletion(totalLessons)
            renderStatus()
            return
        }

        binding.headerContentGroup.isVisible = true
        binding.completionCard.isVisible = false

        binding.chapterLabelText.text = "Chapter ${lesson.chapterNumber}"
        binding.topicTitleText.text = lesson.topicTitle
        binding.topicSourceText.text = lesson.chapterTitle

        when (engine.state) {
            LearningState.ASK_IF_KNOWN -> renderKnowPrompt(lesson)
            LearningState.EXPLAIN_TOPIC -> renderExplanation(lesson)
            LearningState.TAKE_QUIZ -> renderQuiz(lesson)
            LearningState.COMPLETED -> renderCompletion(totalLessons)
        }

        renderStatus()
        maybeSpeakExplanation()
    }

    private fun renderKnowPrompt(lesson: LessonTopic) {
        binding.promptText.text = lesson.knowPrompt
        binding.explanationCard.isVisible = false
        binding.quizCard.isVisible = false
        binding.decisionContainer.isVisible = true
        binding.positiveButton.text = "Yes, I know it"
        binding.negativeButton.text = "No, teach me"
    }

    private fun renderExplanation(lesson: LessonTopic) {
        binding.promptText.text = "Here is the lesson. Have you understood this topic?"
        binding.explanationCard.isVisible = true
        binding.quizCard.isVisible = false
        binding.decisionContainer.isVisible = true
        binding.positiveButton.text = "Yes, I understood"
        binding.negativeButton.text = "No, explain again"

        binding.explanationTitleText.text = lesson.explanationTitle
        binding.explanationBodyText.text = lesson.explanationParagraphs.joinToString("\n\n")
        binding.examplesText.text = lesson.examples.joinToString("\n") { "- $it" }
    }

    private fun renderQuiz(lesson: LessonTopic) {
        val question = engine.currentQuestion() ?: return

        binding.promptText.text = "Answer this check question from the book path."
        binding.explanationCard.isVisible = false
        binding.quizCard.isVisible = true
        binding.decisionContainer.isVisible = false

        binding.questionTitleText.text = lesson.topicTitle
        binding.questionPromptText.text = question.prompt
        binding.hintText.text = question.hint.orEmpty()

        if (question.type == QuestionType.MULTIPLE_CHOICE) {
            binding.answerOptions.removeAllViews()
            question.options.forEach { optionText ->
                val radioButton = RadioButton(this).apply {
                    id = View.generateViewId()
                    text = optionText
                    textSize = 16f
                    setTextColor(getColor(R.color.text_primary))
                    setPadding(0, 18, 0, 18)
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

    private fun renderCompletion(totalLessons: Int) {
        binding.headerContentGroup.isVisible = false
        binding.explanationCard.isVisible = false
        binding.quizCard.isVisible = false
        binding.decisionContainer.isVisible = false
        binding.promptText.text = ""

        binding.completionCard.isVisible = true
        binding.completionTitleText.text = "Book Path Complete"
        binding.completionBodyText.text =
            "You finished all $totalLessons offline topics in ${LessonRepository.BOOK_TITLE}. You can restart and practise again."
    }

    private fun renderStatus() {
        binding.statusText.isVisible = !latestStatusMessage.isNullOrBlank()
        binding.statusText.text = latestStatusMessage.orEmpty()
    }

    private fun maybeSpeakExplanation() {
        if (engine.state != LearningState.EXPLAIN_TOPIC) return
        val token = engine.explanationToken()
        if (!ttsReady || lastSpokenToken == token) return

        speakCurrentExplanation()
        lastSpokenToken = token
    }

    private fun speakCurrentExplanation() {
        val lesson = engine.currentLesson() ?: return
        if (!ttsReady) {
            latestStatusMessage = TTS_LOADING_MESSAGE
            Log.w(TAG, "speakCurrentExplanation called while TTS not ready")
            renderStatus()
            return
        }

        val spokenText = buildString {
            append(lesson.topicTitle)
            append(". ")
            lesson.explanationParagraphs.forEach {
                append(it)
                append(" ")
            }
            if (lesson.examples.isNotEmpty()) {
                append("Examples. ")
                lesson.examples.forEach {
                    append(it)
                    append(". ")
                }
            }
        }

        textToSpeech?.stop()
        val speakResult = textToSpeech?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, lesson.id)
        Log.d(
            TAG,
            "speakCurrentExplanation lesson=${lesson.id} chars=${spokenText.length} result=$speakResult"
        )
    }

    private fun restartFromChapterOne() {
        engine.restart()
        progressStore.clear()
        lastSpokenToken = null
        textToSpeech?.stop()
        latestStatusMessage = "Progress reset. Starting again from Chapter 1."
        render()
    }

    private fun configureSpeechLanguage(): Boolean {
        val preferredLocales = listOf(
            Locale.US,
            Locale.UK,
            Locale.getDefault(),
        ).distinct()

        preferredLocales.forEach { locale ->
            val result = textToSpeech?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            Log.d(TAG, "configureSpeechLanguage locale=$locale result=$result")
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
