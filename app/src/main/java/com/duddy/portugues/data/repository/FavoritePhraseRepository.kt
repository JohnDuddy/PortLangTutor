package com.duddy.portugues.data.repository

interface FavoritePhraseRepository {
    fun getFavoritePhraseIds(): Set<String>
    fun toggleFavoritePhrase(phraseId: String): Set<String>
}
