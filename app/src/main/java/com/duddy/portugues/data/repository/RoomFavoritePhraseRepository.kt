package com.duddy.portugues.data.repository

import com.duddy.portugues.data.local.dao.FavoritePhraseDao
import com.duddy.portugues.data.local.entity.FavoritePhraseEntity
import kotlinx.coroutines.runBlocking

class RoomFavoritePhraseRepository(
    private val dao: FavoritePhraseDao,
) : FavoritePhraseRepository {

    override fun getFavoritePhraseIds(): Set<String> = runBlocking {
        dao.getIds().toSet()
    }

    override fun toggleFavoritePhrase(phraseId: String): Set<String> = runBlocking {
        if (dao.isFavorite(phraseId)) {
            dao.remove(phraseId)
        } else {
            dao.add(FavoritePhraseEntity(phraseId, synced = false))
        }
        dao.getIds().toSet()
    }
}
