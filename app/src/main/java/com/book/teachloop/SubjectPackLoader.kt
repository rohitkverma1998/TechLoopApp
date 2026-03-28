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
        return parseBookJson(
            json = json,
            assetPath = assetPath,
            readAsset = { path -> readAsset(context, path) },
        )
    }

    internal fun parseBookJson(
        json: String,
        assetPath: String,
        readAsset: (String) -> String?,
    ): StudyBook? {
        val manifest = runCatching {
            gson.fromJson(json, SplitStudyBookManifest::class.java)
        }.getOrNull() ?: return null

        val mergedTopics = manifest.topics.orEmpty().toMutableList()
        manifest.chapterAssetPaths.orEmpty().forEach { chapterAssetPath ->
            val chapterJson = readAsset(resolveAssetPath(assetPath, chapterAssetPath)) ?: return null
            val chapterFile = runCatching {
                gson.fromJson(chapterJson, SplitStudyBookChapter::class.java)
            }.getOrNull() ?: return null
            mergedTopics += chapterFile.topics.orEmpty()
        }

        return manifest.toStudyBook(mergedTopics)
    }

    private fun readAsset(context: Context, path: String): String? {
        return runCatching {
            context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }

    internal fun resolveAssetPath(baseAssetPath: String, assetPath: String): String {
        val normalizedAssetPath = assetPath
            .trim()
            .replace('\\', '/')
            .removePrefix("/")
        if (normalizedAssetPath.startsWith(SUBJECT_PACKS_PREFIX)) {
            return normalizedAssetPath
        }

        val baseDirectory = baseAssetPath.substringBeforeLast('/', "")
        return if (baseDirectory.isBlank()) {
            normalizedAssetPath
        } else {
            "$baseDirectory/$normalizedAssetPath"
        }
    }

    private fun SplitStudyBookManifest.toStudyBook(topics: List<StudyTopic>): StudyBook? {
        val bookId = id ?: return null
        val subject = subjectTitle ?: return null
        val title = bookTitle ?: return null
        return StudyBook(
            id = bookId,
            subjectTitle = subject,
            bookTitle = title,
            teacherNote = teacherNote ?: text("Use short practice sessions and spaced revision."),
            topics = topics,
        )
    }

    private data class SplitStudyBookManifest(
        val id: String? = null,
        val subjectTitle: LocalizedText? = null,
        val bookTitle: LocalizedText? = null,
        val teacherNote: LocalizedText? = null,
        val topics: List<StudyTopic>? = null,
        val chapterAssetPaths: List<String>? = null,
    )

    private data class SplitStudyBookChapter(
        val chapterNumber: Int? = null,
        val chapterTitle: LocalizedText? = null,
        val topics: List<StudyTopic>? = null,
    )

    private const val CATALOG_PATH = "subject_packs/catalog.json"
    private const val SUBJECT_PACKS_PREFIX = "subject_packs/"
}
