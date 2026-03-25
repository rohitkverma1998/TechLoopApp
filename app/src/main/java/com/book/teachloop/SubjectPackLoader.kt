package com.book.teachloop

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal object SubjectPackLoader {
    private val gson = Gson()

    fun loadCatalog(context: Context): List<SubjectPackCatalogItem>? {
        val json = readAsset(context, CATALOG_PATH) ?: return null
        val type = object : TypeToken<List<SubjectPackCatalogItem>>() {}.type
        return runCatching { gson.fromJson<List<SubjectPackCatalogItem>>(json, type) }.getOrNull()
    }

    fun loadBook(context: Context, assetPath: String): StudyBook? {
        val json = readAsset(context, assetPath) ?: return null
        return runCatching { gson.fromJson(json, StudyBook::class.java) }.getOrNull()
    }

    private fun readAsset(context: Context, path: String): String? {
        return runCatching {
            context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }

    private const val CATALOG_PATH = "subject_packs/catalog.json"
}
