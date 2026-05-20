package com.duddy.portugues.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Spaced-repetition state for a single phrase.
 * One row per phrase the user has ever practised.
 */
@Entity(tableName = "phrase_review")
data class PhraseReviewEntity(
    @PrimaryKey
    @ColumnInfo(name = "phrase_id")
    val phraseId: String,

    /** ISO date the phrase next becomes due, yyyy-MM-dd. */
    @ColumnInfo(name = "due_date")
    val dueDate: String,

    /** Current SRS interval in days. */
    @ColumnInfo(name = "interval_days")
    val intervalDays: Int,

    /** SM-2 ease factor (clamped 1.30…2.80). */
    @ColumnInfo(name = "ease_factor")
    val easeFactor: Double,

    /** Number of completed reviews. */
    @ColumnInfo(name = "review_count")
    val reviewCount: Int,

    /** Consecutive non-"Again" grades. */
    @ColumnInfo(name = "correct_streak")
    val correctStreak: Int,

    /** Last pronunciation accuracy score (0–100), null if never scored. */
    @ColumnInfo(name = "last_score")
    val lastScore: Int? = null,

    /** Epoch millis of last update — used for cloud sync diff. */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    /** Whether this row has been synced to Supabase. */
    @ColumnInfo(name = "synced")
    val synced: Boolean = false,
)
