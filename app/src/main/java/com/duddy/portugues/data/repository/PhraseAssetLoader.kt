package com.duddy.portugues.data.repository

import android.content.Context
import android.util.Log
import com.duddy.portugues.data.model.Phrase
import com.duddy.portugues.data.model.PhraseCategory
import com.duddy.portugues.data.model.PhraseDifficulty
import com.duddy.portugues.data.model.PhraseLibraryStats
import org.json.JSONArray
import org.json.JSONObject

data class PhraseLoadResult(
    val phrases: List<Phrase>,
    val stats: PhraseLibraryStats,
)

object PhraseAssetLoader {
    private const val TAG = "PhraseAssetLoader"
    private const val ASSET_FILE = "phrases_v1.json"

    fun load(context: Context): PhraseLoadResult {
        val json = context.assets.open(ASSET_FILE)
            .bufferedReader()
            .use { reader -> reader.readText() }
        val phrases = parse(JSONArray(json))
        val warnings = validate(phrases)
        val stats = PhraseLibraryStats(
            phraseCount = phrases.size,
            categoryCount = phrases.map { it.category }.distinct().size,
            warnings = warnings,
        )

        if (warnings.isEmpty()) {
            Log.i(TAG, "Loaded ${phrases.size} local phrases from $ASSET_FILE")
        } else {
            Log.w(TAG, "Loaded ${phrases.size} local phrases with ${warnings.size} warnings")
            warnings.take(10).forEach { warning -> Log.w(TAG, warning) }
        }

        return PhraseLoadResult(phrases = phrases, stats = stats)
    }

    private fun parse(array: JSONArray): List<Phrase> {
        val phrases = ArrayList<Phrase>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val category = PhraseCategory.parse(item.optString("category")) ?: PhraseCategory.Survival
            phrases += Phrase(
                id = item.optString("id"),
                portuguese = item.optString("portuguese"),
                english = item.optString("english"),
                pronunciationGuide = item.optString("pronunciationGuide"),
                category = category,
                subcategory = item.optString("subcategory"),
                difficulty = PhraseDifficulty.parse(item.optString("difficulty")),
                tags = item.optJSONArray("tags").toStringList(),
                speakingPractice = item.optBoolean("speakingPractice", true),
                grammarNote = item.optNullableString("grammarNote"),
            )
        }
        return phrases
    }

    private fun validate(phrases: List<Phrase>): List<String> {
        val warnings = mutableListOf<String>()
        val seenIds = mutableSetOf<String>()
        phrases.forEachIndexed { index, phrase ->
            if (phrase.id.isBlank()) warnings += "Phrase at index $index has a blank id."
            if (!seenIds.add(phrase.id)) warnings += "Duplicate phrase id: ${phrase.id}"
            if (phrase.portuguese.isBlank()) warnings += "Phrase ${phrase.id} has blank Portuguese text."
            if (phrase.english.isBlank()) warnings += "Phrase ${phrase.id} has blank English text."
            if (phrase.pronunciationGuide.isBlank()) warnings += "Phrase ${phrase.id} has no pronunciation guide."
            if (phrase.category.key.isBlank()) warnings += "Phrase ${phrase.id} has no category."
        }
        return warnings
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val values = ArrayList<String>(length())
        for (index in 0 until length()) {
            val value = optString(index).trim()
            if (value.isNotBlank()) values += value
        }
        return values
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
}
