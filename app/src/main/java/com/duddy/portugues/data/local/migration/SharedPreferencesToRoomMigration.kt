package com.duddy.portugues.data.local.migration

import android.content.Context
import android.util.Log
import com.duddy.portugues.data.local.DuddyDatabase
import com.duddy.portugues.data.local.entity.FavoritePhraseEntity
import com.duddy.portugues.data.local.entity.PhraseReviewEntity
import com.duddy.portugues.data.local.entity.ProgressStatsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-shot migration that pulls existing SharedPreferences state into Room.
 * Idempotent: runs once, sets a flag, then skips future launches.
 */
object SharedPreferencesToRoomMigration {
    private const val TAG = "DuddyMigration"
    private const val FLAG_PREFS = "duddy_migration"
    private const val FLAG_MIGRATED = "spr_to_room_v1_done"

    suspend fun migrateIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val flagPrefs = appContext.getSharedPreferences(FLAG_PREFS, Context.MODE_PRIVATE)
        if (flagPrefs.getBoolean(FLAG_MIGRATED, false)) {
            Log.d(TAG, "already migrated, skipping")
            return@withContext
        }

        val db = DuddyDatabase.getInstance(appContext)
        runCatching { migrateReviews(appContext, db) }
            .onFailure { Log.w(TAG, "review migration failed", it) }
        runCatching { migrateFavorites(appContext, db) }
            .onFailure { Log.w(TAG, "favorites migration failed", it) }
        runCatching { migrateProgress(appContext, db) }
            .onFailure { Log.w(TAG, "progress migration failed", it) }

        flagPrefs.edit().putBoolean(FLAG_MIGRATED, true).apply()
        Log.i(TAG, "migration complete")
    }

    private suspend fun migrateReviews(context: Context, db: DuddyDatabase) {
        val prefs = context.getSharedPreferences(REVIEW_PREFS, Context.MODE_PRIVATE)
        val all = prefs.all
        if (all.isEmpty()) return

        val grouped = mutableMapOf<String, MutableMap<String, Any?>>()
        for ((key, value) in all) {
            val dot = key.indexOf('.')
            if (dot <= 0) continue

            val phraseId = key.substring(0, dot)
            val field = key.substring(dot + 1)
            grouped.getOrPut(phraseId) { mutableMapOf() }[field] = value
        }

        val reviews = grouped.map { (phraseId, fields) ->
            PhraseReviewEntity(
                phraseId = phraseId,
                dueDate = fields[FIELD_DUE_DATE] as? String ?: "1970-01-01",
                intervalDays = fields[FIELD_INTERVAL_DAYS] as? Int ?: 0,
                easeFactor = when (val value = fields[FIELD_EASE_FACTOR]) {
                    is Float -> value.toDouble()
                    is Double -> value
                    is Int -> value.toDouble()
                    else -> 2.30
                },
                reviewCount = fields[FIELD_REVIEW_COUNT] as? Int ?: 0,
                correctStreak = fields[FIELD_CORRECT_STREAK] as? Int ?: 0,
                lastScore = (fields[FIELD_LAST_SCORE] as? Int)?.takeIf { it >= 0 },
                synced = false,
            )
        }

        reviews.forEach { review -> db.reviewDao().upsert(review) }
        Log.i(TAG, "migrated ${reviews.size} reviews")
    }

    private suspend fun migrateFavorites(context: Context, db: DuddyDatabase) {
        val prefs = context.getSharedPreferences(FAVORITE_PREFS, Context.MODE_PRIVATE)
        val favoriteIds = prefs.getStringSet(KEY_FAVORITE_PHRASE_IDS, emptySet()).orEmpty()
        if (favoriteIds.isEmpty()) return

        favoriteIds.forEach { phraseId ->
            db.favoriteDao().add(
                FavoritePhraseEntity(
                    phraseId = phraseId,
                    synced = false,
                )
            )
        }
        Log.i(TAG, "migrated ${favoriteIds.size} favorites")
    }

    private suspend fun migrateProgress(context: Context, db: DuddyDatabase) {
        val prefs = context.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE)
        if (prefs.all.isEmpty()) return

        val stats = ProgressStatsEntity(
            lessonsStarted = prefs.getInt(KEY_LESSONS_STARTED, 0),
            practicedPhrases = prefs.getInt(KEY_PRACTICED_PHRASES, 0),
            sampleAudioPlays = prefs.getInt(KEY_SAMPLE_AUDIO_PLAYS, 0),
            speakingAttempts = prefs.getInt(KEY_SPEAKING_ATTEMPTS, 0),
            aiCoachRequests = prefs.getInt(KEY_AI_COACH_REQUESTS, 0),
            streakDays = prefs.getInt(KEY_STREAK_DAYS, 0),
            lastActiveDate = prefs.getString(KEY_LAST_PRACTICE_DATE, null),
        )

        db.progressDao().upsert(stats)
        Log.i(TAG, "migrated progress stats")
    }

    private const val REVIEW_PREFS = "duddy_portugues_spaced_reviews"
    private const val FAVORITE_PREFS = "duddy_portugues_favorites"
    private const val PROGRESS_PREFS = "duddy_portugues_progress"

    private const val FIELD_DUE_DATE = "due_date"
    private const val FIELD_INTERVAL_DAYS = "interval_days"
    private const val FIELD_EASE_FACTOR = "ease_factor"
    private const val FIELD_REVIEW_COUNT = "review_count"
    private const val FIELD_CORRECT_STREAK = "correct_streak"
    private const val FIELD_LAST_SCORE = "last_score"

    private const val KEY_FAVORITE_PHRASE_IDS = "favorite_phrase_ids"

    private const val KEY_LESSONS_STARTED = "lessons_started"
    private const val KEY_PRACTICED_PHRASES = "practiced_phrases"
    private const val KEY_SAMPLE_AUDIO_PLAYS = "sample_audio_plays"
    private const val KEY_SPEAKING_ATTEMPTS = "speaking_attempts"
    private const val KEY_AI_COACH_REQUESTS = "ai_coach_requests"
    private const val KEY_LAST_PRACTICE_DATE = "last_practice_date"
    private const val KEY_STREAK_DAYS = "streak_days"
}
