package com.duddy.portugues.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.duddy.portugues.data.model.GamificationRules
import com.duddy.portugues.data.model.ProgressStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SharedPreferencesProgressRepository(context: Context) : ProgressRepository {
    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getStats(): ProgressStats {
        ensureDailyXpDate()
        return ProgressStats(
            lessonsStarted = sharedPreferences.getInt(KEY_LESSONS_STARTED, 0),
            practicedPhrases = sharedPreferences.getInt(KEY_PRACTICED_PHRASES, 0),
            sampleAudioPlays = sharedPreferences.getInt(KEY_SAMPLE_AUDIO_PLAYS, 0),
            speakingAttempts = sharedPreferences.getInt(KEY_SPEAKING_ATTEMPTS, 0),
            aiCoachRequests = sharedPreferences.getInt(KEY_AI_COACH_REQUESTS, 0),
            streakDays = displayStreak(),
            longestStreak = sharedPreferences.getInt(KEY_LONGEST_STREAK, 0),
            totalXp = sharedPreferences.getInt(KEY_TOTAL_XP, 0),
            todayXp = sharedPreferences.getInt(KEY_TODAY_XP, 0),
            dailyXpGoal = sharedPreferences.getInt(KEY_DAILY_XP_GOAL, GamificationRules.DEFAULT_DAILY_XP_GOAL),
            hearts = sharedPreferences.getInt(KEY_HEARTS, GamificationRules.DEFAULT_MAX_HEARTS),
            maxHearts = GamificationRules.DEFAULT_MAX_HEARTS,
            weeklyLeagueXp = sharedPreferences.getInt(KEY_WEEKLY_LEAGUE_XP, 0),
            leagueName = sharedPreferences.getString(KEY_LEAGUE_NAME, "Bronze") ?: "Bronze",
        )
    }

    override fun recordLessonStarted() =
        increment(KEY_LESSONS_STARTED, GamificationRules.XP_LESSON_STARTED, countsAsStreakActivity = true)

    override fun recordPhrasePracticed() =
        increment(KEY_PRACTICED_PHRASES, GamificationRules.XP_PHRASE_PRACTICED, countsAsStreakActivity = true)

    override fun recordSampleAudioPlayed() =
        increment(KEY_SAMPLE_AUDIO_PLAYS, xp = 0, countsAsStreakActivity = false)

    override fun recordSpeakingAttempt() =
        increment(KEY_SPEAKING_ATTEMPTS, GamificationRules.XP_SPEAKING_ATTEMPT, countsAsStreakActivity = true)

    override fun recordPronunciationAssessed() =
        addXp(GamificationRules.XP_PRONUNCIATION_ASSESSED, countsAsStreakActivity = true)

    override fun recordAiCoachRequest() =
        increment(KEY_AI_COACH_REQUESTS, GamificationRules.XP_AI_COACH_REQUEST, countsAsStreakActivity = true)

    override fun recordMistake() {
        regenerateHearts()
        sharedPreferences.edit()
            .putInt(KEY_HEARTS, (sharedPreferences.getInt(KEY_HEARTS, GamificationRules.DEFAULT_MAX_HEARTS) - 1).coerceAtLeast(0))
            .apply()
    }

    override fun refillHearts() {
        sharedPreferences.edit()
            .putInt(KEY_HEARTS, GamificationRules.DEFAULT_MAX_HEARTS)
            .putLong(KEY_LAST_HEART_REFILL_AT, System.currentTimeMillis())
            .apply()
    }

    override fun setDailyXpGoal(goalXp: Int) {
        sharedPreferences.edit()
            .putInt(KEY_DAILY_XP_GOAL, goalXp.coerceIn(10, 500))
            .apply()
    }

    private fun increment(key: String, xp: Int, countsAsStreakActivity: Boolean) {
        sharedPreferences.edit()
            .putInt(key, sharedPreferences.getInt(key, 0) + 1)
            .apply()
        addXp(xp, countsAsStreakActivity)
    }

    private fun addXp(xp: Int, countsAsStreakActivity: Boolean) {
        ensureDailyXpDate()
        regenerateHearts()
        if (countsAsStreakActivity) recordStreakActivity()
        if (xp <= 0) return
        sharedPreferences.edit()
            .putInt(KEY_TOTAL_XP, sharedPreferences.getInt(KEY_TOTAL_XP, 0) + xp)
            .putInt(KEY_TODAY_XP, sharedPreferences.getInt(KEY_TODAY_XP, 0) + xp)
            .putInt(KEY_WEEKLY_LEAGUE_XP, sharedPreferences.getInt(KEY_WEEKLY_LEAGUE_XP, 0) + xp)
            .apply()
    }

    private fun recordStreakActivity() {
        val today = dayString(0)
        val yesterday = dayString(-1)
        val lastPracticeDate = sharedPreferences.getString(KEY_LAST_PRACTICE_DATE, null)
        val currentStreak = sharedPreferences.getInt(KEY_STREAK_DAYS, 0)
        val newStreak = when (lastPracticeDate) {
            today -> currentStreak.coerceAtLeast(1)
            yesterday -> currentStreak + 1
            else -> 1
        }
        sharedPreferences.edit()
            .putString(KEY_LAST_PRACTICE_DATE, today)
            .putInt(KEY_STREAK_DAYS, newStreak)
            .putInt(KEY_LONGEST_STREAK, maxOf(newStreak, sharedPreferences.getInt(KEY_LONGEST_STREAK, 0)))
            .apply()
    }

    private fun displayStreak(): Int =
        when (sharedPreferences.getString(KEY_LAST_PRACTICE_DATE, null)) {
            dayString(0), dayString(-1) -> sharedPreferences.getInt(KEY_STREAK_DAYS, 0)
            else -> 0
        }

    private fun ensureDailyXpDate() {
        val today = dayString(0)
        if (sharedPreferences.getString(KEY_TODAY_XP_DATE, null) == today) return
        sharedPreferences.edit()
            .putString(KEY_TODAY_XP_DATE, today)
            .putInt(KEY_TODAY_XP, 0)
            .apply()
    }

    private fun regenerateHearts() {
        val maxHearts = GamificationRules.DEFAULT_MAX_HEARTS
        val hearts = sharedPreferences.getInt(KEY_HEARTS, maxHearts).coerceIn(0, maxHearts)
        if (hearts >= maxHearts) return
        val now = System.currentTimeMillis()
        val last = sharedPreferences.getLong(KEY_LAST_HEART_REFILL_AT, now)
        val intervalMs = GamificationRules.HEART_REGEN_MINUTES * 60_000L
        val gained = ((now - last) / intervalMs).toInt().coerceAtLeast(0)
        if (gained <= 0) return
        sharedPreferences.edit()
            .putInt(KEY_HEARTS, (hearts + gained).coerceAtMost(maxHearts))
            .putLong(KEY_LAST_HEART_REFILL_AT, now)
            .apply()
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
        const val KEY_LONGEST_STREAK = "longest_streak"
        const val KEY_TOTAL_XP = "total_xp"
        const val KEY_TODAY_XP = "today_xp"
        const val KEY_TODAY_XP_DATE = "today_xp_date"
        const val KEY_DAILY_XP_GOAL = "daily_xp_goal"
        const val KEY_HEARTS = "hearts"
        const val KEY_LAST_HEART_REFILL_AT = "last_heart_refill_at"
        const val KEY_WEEKLY_LEAGUE_XP = "weekly_league_xp"
        const val KEY_LEAGUE_NAME = "league_name"
    }
}
