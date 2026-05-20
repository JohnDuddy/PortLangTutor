package com.duddy.portugues.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.duddy.portugues.data.local.entity.PhraseReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseReviewDao {

    @Query("SELECT * FROM phrase_review WHERE phrase_id = :phraseId")
    suspend fun getById(phraseId: String): PhraseReviewEntity?

    @Query("SELECT * FROM phrase_review WHERE phrase_id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<PhraseReviewEntity>

    /** IDs that are due today or earlier (already reviewed at least once). */
    @Query("SELECT phrase_id FROM phrase_review WHERE due_date <= :today ORDER BY due_date ASC LIMIT :limit")
    suspend fun getDueIds(today: String, limit: Int): List<String>

    @Query("SELECT phrase_id FROM phrase_review")
    suspend fun getAllReviewedIds(): List<String>

    @Query("SELECT COUNT(*) FROM phrase_review WHERE due_date <= :today")
    fun observeDueCount(today: String): Flow<Int>

    @Upsert
    suspend fun upsert(review: PhraseReviewEntity)

    @Query("UPDATE phrase_review SET synced = 1 WHERE phrase_id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT * FROM phrase_review WHERE synced = 0 ORDER BY updated_at ASC LIMIT :limit")
    suspend fun getUnsynced(limit: Int = 200): List<PhraseReviewEntity>

    @Query("DELETE FROM phrase_review")
    suspend fun clear()
}
