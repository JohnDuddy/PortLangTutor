package com.duddy.portugues.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.duddy.portugues.data.model.ProgressStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SharedPreferencesProgressRepository(context: Context) : ProgressRepository {
    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getStats(): ProgressStats {
        refreshStreak()

        return ProgressStats(
            lessonsStarted = sharedPreferences.getInt(KEY_LESSONS_STARTED, 0),
            practicedPhrases = sharedPreferences.getInt(KEY_PRACTICED_PHRASES, 0),
            sampleAudioPlays = sharedPreferences.getInt(KEY_SAMPLE_AUDIO_PLAYS, 0),
            speakingAttempts = sharedPreferences.getInt(KEY_SPEAKING_ATTEMPTS, 0),
            aiCoachRequests = sharedPreferences.getInt(KEY_AI_COACH_REQUESTS, 0),
            streakDays = sharedPreferences.getInt(KEY_STREAK_DAYS, 1)
        )
    }

    override fun recordLessonStarted() {
        increment(KEY_LESSONS_STARTED)
    }

    override fun recordPhrasePracticed() {
        increment(KEY_PRACTICED_PHRASES)
    }

    override fun recordSampleAudioPlayed() {
        increment(KEY_SAMPLE_AUDIO_PLAYS)
    }

    override fun recordSpeakingAttempt() {
        increment(KEY_SPEAKING_ATTEMPTS)
    }

    override fun recordAiCoachRequest() {
        increment(KEY_AI_COACH_REQUESTS)
    }

    private fun increment(key: String) {
        refreshStreak()
        sharedPreferences.edit()
            .putInt(key, sharedPreferences.getInt(key, 0) + 1)
            .apply()
    }

    private fun refreshStreak() {
        val today = dayString(0)
        val yesterday = dayString(-1)
        val lastPracticeDate = sharedPreferences.getString(KEY_LAST_PRACTICE_DATE, null)
        val currentStreak = sharedPreferences.getInt(KEY_STREAK_DAYS, 0)

        val newStreak = when (lastPracticeDate) {
            today -> currentStreak.coerceAtLeast(1)
            yesterday -> currentStreak + 1
            null -> 1
            else -> 1
        }

        if (lastPracticeDate != today || currentStreak != newStreak) {
            sharedPreferences.edit()
                .putString(KEY_LAST_PRACTICE_DATE, today)
                .putInt(KEY_STREAK_DAYS, newStreak)
                .apply()
        }
    }

    private fun dayString(offsetDays: Int): String {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, offsetDays)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private companion object {
        const val PREFERENCES_NAME = "duddy_portugues_progress"
        const val KEY_LESSONS_STARTED = "lessons_started"
        const val KEY_PRACTICED_PHRASES = "practiced_phrases"
        const val KEY_SAMPLE_AUDIO_PLAYS = "sample_audio_plays"
        const val KEY_SPEAKING_ATTEMPTS = "speaking_attempts"
        const val KEY_AI_COACH_REQUESTS = "ai_coach_requests"
        const val KEY_LAST_PRACTICE_DATE = "last_practice_date"
        const val KEY_STREAK_DAYS = "streak_days"
    }
}
