package com.duddy.portugues.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.duddy.portugues.data.local.entity.ProgressStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressStatsDao {

    @Query("SELECT * FROM progress_stats WHERE id = 1")
    fun observe(): Flow<ProgressStatsEntity?>

    @Query("SELECT * FROM progress_stats WHERE id = 1")
    suspend fun get(): ProgressStatsEntity?

    @Upsert
    suspend fun upsert(stats: ProgressStatsEntity)
}
