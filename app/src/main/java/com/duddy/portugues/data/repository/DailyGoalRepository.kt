package com.duddy.portugues.data.repository

import android.content.Context
import com.duddy.portugues.data.model.DailyGoalProgress
import com.duddy.portugues.data.model.DailyGoalTargets
import com.duddy.portugues.data.preferences.OnboardingPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

interface DailyGoalRepository {
    fun getTodayProgress(streakDays: Int): DailyGoalProgress
    fun recordNewPhrase()
    fun recordReview()
    fun recordSpeakingAttempt()
    fun recordPracticeMinutes(minutes: Int = 1)
    fun recordAiCoachRequest()
}

class SharedPreferencesDailyGoalRepository(context: Context) : DailyGoalRepository {
    private val appContext = context.applicationContext
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getTodayProgress(streakDays: Int): DailyGoalProgress {
        ensureToday()
        return DailyGoalProgress(
            date = today(),
            targets = targetsForDailyMinutes(OnboardingPreferences.dailyMinutes(appContext)),
            completedNewPhrases = prefs.getInt(KEY_NEW_PHRASES, 0),
            completedReviews = prefs.getInt(KEY_REVIEWS, 0),
            completedSpeakingAttempts = prefs.getInt(KEY_SPEAKING_ATTEMPTS, 0),
            completedPracticeMinutes = prefs.getInt(KEY_PRACTICE_MINUTES, 0),
            completedAiCoachRequests = prefs.getInt(KEY_AI_REQUESTS, 0),
            streakDays = streakDays,
        )
    }

    override fun recordNewPhrase() = increment(KEY_NEW_PHRASES)
    override fun recordReview() = increment(KEY_REVIEWS)
    override fun recordSpeakingAttempt() = increment(KEY_SPEAKING_ATTEMPTS)
    override fun recordPracticeMinutes(minutes: Int) = increment(KEY_PRACTICE_MINUTES, minutes.coerceAtLeast(0))
    override fun recordAiCoachRequest() = increment(KEY_AI_REQUESTS)

    private fun increment(key: String, amount: Int = 1) {
        ensureToday()
        prefs.edit()
            .putInt(key, prefs.getInt(key, 0) + amount)
            .apply()
    }

    private fun ensureToday() {
        val currentDate = today()
        if (prefs.getString(KEY_DATE, null) == currentDate) return
        prefs.edit()
            .putString(KEY_DATE, currentDate)
            .putInt(KEY_NEW_PHRASES, 0)
            .putInt(KEY_REVIEWS, 0)
            .putInt(KEY_SPEAKING_ATTEMPTS, 0)
            .putInt(KEY_PRACTICE_MINUTES, 0)
            .putInt(KEY_AI_REQUESTS, 0)
            .apply()
    }

    private fun today(): String {
        val cal = Calendar.getInstance()
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun targetsForDailyMinutes(minutes: Int): DailyGoalTargets =
        when {
            minutes <= 5 -> DailyGoalTargets(
                newPhrasesPerDay = 3,
                reviewsPerDay = 8,
                speakingAttemptsPerDay = 3,
                practiceMinutesPerDay = 5,
                aiCoachRequestsPerDay = 1,
            )

            minutes >= 20 -> DailyGoalTargets(
                newPhrasesPerDay = 8,
                reviewsPerDay = 25,
                speakingAttemptsPerDay = 10,
                practiceMinutesPerDay = 20,
                aiCoachRequestsPerDay = 4,
            )

            else -> DailyGoalTargets()
        }

    private companion object {
        const val PREFS_NAME = "duddy_portugues_daily_goals"
        const val KEY_DATE = "date"
        const val KEY_NEW_PHRASES = "new_phrases"
        const val KEY_REVIEWS = "reviews"
        const val KEY_SPEAKING_ATTEMPTS = "speaking_attempts"
        const val KEY_PRACTICE_MINUTES = "practice_minutes"
        const val KEY_AI_REQUESTS = "ai_requests"
    }
}
