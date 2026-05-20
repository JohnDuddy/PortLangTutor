package com.duddy.portugues.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table tracking the user's cumulative progress.
 * Field names mirror the existing ProgressStats data model so the migration
 * from SharedPreferences is straightforward.
 */
@Entity(tableName = "progress_stats")
data class ProgressStatsEntity(
    @PrimaryKey
    val id: Int = 1, // singleton

    @ColumnInfo(name = "lessons_started")    val lessonsStarted:   Int = 0,
    @ColumnInfo(name = "practiced_phrases")  val practicedPhrases: Int = 0,
    @ColumnInfo(name = "sample_audio_plays") val sampleAudioPlays: Int = 0,
    @ColumnInfo(name = "speaking_attempts")  val speakingAttempts: Int = 0,
    @ColumnInfo(name = "ai_coach_requests")  val aiCoachRequests:  Int = 0,
    @ColumnInfo(name = "streak_days")        val streakDays:       Int = 0,
    @ColumnInfo(name = "last_active_date")   val lastActiveDate:   String? = null,
    @ColumnInfo(name = "total_xp")           val totalXp:          Int = 0,
    @ColumnInfo(name = "longest_streak")     val longestStreak:    Int = 0,
    @ColumnInfo(name = "hearts")             val hearts:           Int = 5,
    @ColumnInfo(name = "max_hearts")         val maxHearts:        Int = 5,
    @ColumnInfo(name = "last_heart_refill_at") val lastHeartRefillAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "daily_xp_goal")      val dailyXpGoal:      Int = 50,
    @ColumnInfo(name = "today_xp")           val todayXp:          Int = 0,
    @ColumnInfo(name = "today_xp_date")      val todayXpDate:      String? = null,
    @ColumnInfo(name = "weekly_league_xp")   val weeklyLeagueXp:   Int = 0,
    @ColumnInfo(name = "league_name")        val leagueName:       String = "Bronze",
    @ColumnInfo(name = "league_week_id")     val leagueWeekId:     String? = null,
    @ColumnInfo(name = "updated_at")         val updatedAt:        Long = System.currentTimeMillis(),
)
