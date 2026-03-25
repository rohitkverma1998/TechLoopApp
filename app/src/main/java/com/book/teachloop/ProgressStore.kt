package com.book.teachloop

import android.content.Context
import com.google.gson.Gson

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(): AppSnapshot {
        val json = prefs.getString(KEY_APP_STATE, null) ?: return AppSnapshot()
        return runCatching {
            gson.fromJson(json, AppSnapshot::class.java) ?: AppSnapshot()
        }.getOrDefault(AppSnapshot())
    }

    fun save(snapshot: AppSnapshot) {
        prefs.edit()
            .putString(KEY_APP_STATE, gson.toJson(snapshot))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "teach_loop_progress_v3"
        const val KEY_APP_STATE = "app_state"
    }
}
