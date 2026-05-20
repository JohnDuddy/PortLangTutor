package com.duddy.portugues.data.repository

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesFavoritePhraseRepository(context: Context) : FavoritePhraseRepository {
    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getFavoritePhraseIds(): Set<String> =
        sharedPreferences.getStringSet(KEY_FAVORITE_PHRASE_IDS, emptySet()).orEmpty().toSet()

    override fun toggleFavoritePhrase(phraseId: String): Set<String> {
        val currentFavorites = getFavoritePhraseIds().toMutableSet()
        if (phraseId in currentFavorites) {
            currentFavorites.remove(phraseId)
        } else {
            currentFavorites.add(phraseId)
        }

        sharedPreferences.edit()
            .putStringSet(KEY_FAVORITE_PHRASE_IDS, currentFavorites)
            .apply()

        return currentFavorites
    }

    private companion object {
        const val PREFERENCES_NAME = "duddy_portugues_favorites"
        const val KEY_FAVORITE_PHRASE_IDS = "favorite_phrase_ids"
    }
}
