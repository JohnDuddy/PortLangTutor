package com.duddy.portugues.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.duddy.portugues.data.local.entity.FavoritePhraseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePhraseDao {

    @Query("SELECT phrase_id FROM favorite_phrase")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT phrase_id FROM favorite_phrase")
    suspend fun getIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_phrase WHERE phrase_id = :phraseId)")
    suspend fun isFavorite(phraseId: String): Boolean

    @Upsert
    suspend fun add(entity: FavoritePhraseEntity)

    @Query("DELETE FROM favorite_phrase WHERE phrase_id = :phraseId")
    suspend fun remove(phraseId: String)

    @Query("SELECT * FROM favorite_phrase WHERE synced = 0")
    suspend fun getUnsynced(): List<FavoritePhraseEntity>

    @Query("UPDATE favorite_phrase SET synced = 1 WHERE phrase_id IN (:ids)")
    suspend fun markSynced(ids: List<String>)
}
