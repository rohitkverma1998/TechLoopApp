package com.book.teachloop

import android.content.Context

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ProgressSnapshot {
        val stateName = prefs.getString(KEY_STATE, LearningState.ASK_IF_KNOWN.name)
        val state = LearningState.entries.firstOrNull { it.name == stateName }
            ?: LearningState.ASK_IF_KNOWN

        return ProgressSnapshot(
            currentIndex = prefs.getInt(KEY_CURRENT_INDEX, 0),
            state = state,
            questionIndex = prefs.getInt(KEY_QUESTION_INDEX, 0),
            explanationRepeats = prefs.getInt(KEY_EXPLANATION_REPEATS, 0),
        )
    }

    fun save(snapshot: ProgressSnapshot) {
        prefs.edit()
            .putInt(KEY_CURRENT_INDEX, snapshot.currentIndex)
            .putString(KEY_STATE, snapshot.state.name)
            .putInt(KEY_QUESTION_INDEX, snapshot.questionIndex)
            .putInt(KEY_EXPLANATION_REPEATS, snapshot.explanationRepeats)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "teach_loop_progress"
        const val KEY_CURRENT_INDEX = "current_index"
        const val KEY_STATE = "state"
        const val KEY_QUESTION_INDEX = "question_index"
        const val KEY_EXPLANATION_REPEATS = "explanation_repeats"
    }
}
