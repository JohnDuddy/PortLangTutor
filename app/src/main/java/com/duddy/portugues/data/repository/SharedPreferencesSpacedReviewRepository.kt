package com.duddy.portugues.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.duddy.portugues.data.model.PhraseReviewState
import com.duddy.portugues.data.model.ReviewGrade
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class SharedPreferencesSpacedReviewRepository(context: Context) : SpacedReviewRepository {
    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getReviewState(phraseId: String): PhraseReviewState? {
        val dueDate = sharedPreferences.getString(key(phraseId, FIELD_DUE_DATE), null) ?: return null
        return PhraseReviewState(
            phraseId = phraseId,
            dueDate = dueDate,
            intervalDays = sharedPreferences.getInt(key(phraseId, FIELD_INTERVAL_DAYS), 0),
            easeFactor = sharedPreferences.getFloat(key(phraseId, FIELD_EASE_FACTOR), DEFAULT_EASE.toFloat()).toDouble(),
            reviewCount = sharedPreferences.getInt(key(phraseId, FIELD_REVIEW_COUNT), 0),
            correctStreak = sharedPreferences.getInt(key(phraseId, FIELD_CORRECT_STREAK), 0),
            lastScore = sharedPreferences.getInt(key(phraseId, FIELD_LAST_SCORE), -1).takeIf { it >= 0 }
        )
    }

    override fun getDuePhraseIds(allPhraseIds: List<String>, limit: Int): List<String> {
        val today = dayString(0)
        val duePhraseIds = allPhraseIds.filter { phraseId ->
            val state = getReviewState(phraseId)
            state == null || state.dueDate <= today
        }

        return duePhraseIds.take(limit)
    }

    override fun recordReview(phraseId: String, grade: ReviewGrade): PhraseReviewState {
        val current = getReviewState(phraseId) ?: PhraseReviewState(
            phraseId = phraseId,
            dueDate = dayString(0),
            intervalDays = 0,
            easeFactor = DEFAULT_EASE,
            reviewCount = 0,
            correctStreak = 0
        )

        val nextEase = nextEase(current.easeFactor, grade)
        val nextInterval = nextInterval(current.intervalDays, nextEase, grade)
        val nextState = PhraseReviewState(
            phraseId = phraseId,
            dueDate = dayString(nextInterval),
            intervalDays = nextInterval,
            easeFactor = nextEase,
            reviewCount = current.reviewCount + 1,
            correctStreak = if (grade == ReviewGrade.Again) 0 else current.correctStreak + 1,
            lastScore = current.lastScore
        )

        sharedPreferences.edit()
            .putString(key(phraseId, FIELD_DUE_DATE), nextState.dueDate)
            .putInt(key(phraseId, FIELD_INTERVAL_DAYS), nextState.intervalDays)
            .putFloat(key(phraseId, FIELD_EASE_FACTOR), nextState.easeFactor.toFloat())
            .putInt(key(phraseId, FIELD_REVIEW_COUNT), nextState.reviewCount)
            .putInt(key(phraseId, FIELD_CORRECT_STREAK), nextState.correctStreak)
            .putInt(key(phraseId, FIELD_LAST_SCORE), nextState.lastScore ?: -1)
            .apply()

        return nextState
    }

    override fun recordPronunciationScore(phraseId: String, score: Int): PhraseReviewState? {
        val current = getReviewState(phraseId) ?: PhraseReviewState(
            phraseId = phraseId,
            dueDate = dayString(0),
            intervalDays = 0,
            easeFactor = DEFAULT_EASE,
            reviewCount = 0,
            correctStreak = 0
        )
        val nextState = current.copy(lastScore = score.coerceIn(0, 100))
        sharedPreferences.edit()
            .putString(key(phraseId, FIELD_DUE_DATE), nextState.dueDate)
            .putInt(key(phraseId, FIELD_INTERVAL_DAYS), nextState.intervalDays)
            .putFloat(key(phraseId, FIELD_EASE_FACTOR), nextState.easeFactor.toFloat())
            .putInt(key(phraseId, FIELD_REVIEW_COUNT), nextState.reviewCount)
            .putInt(key(phraseId, FIELD_CORRECT_STREAK), nextState.correctStreak)
            .putInt(key(phraseId, FIELD_LAST_SCORE), nextState.lastScore ?: -1)
            .apply()
        return nextState
    }

    private fun nextEase(currentEase: Double, grade: ReviewGrade): Double =
        when (grade) {
            ReviewGrade.Again -> currentEase - 0.25
            ReviewGrade.Hard -> currentEase - 0.10
            ReviewGrade.Good -> currentEase
            ReviewGrade.Easy -> currentEase + 0.15
        }.coerceIn(1.30, 2.80)

    private fun nextInterval(currentInterval: Int, ease: Double, grade: ReviewGrade): Int =
        when (grade) {
            ReviewGrade.Again -> 0
            ReviewGrade.Hard -> 1
            ReviewGrade.Good -> if (currentInterval == 0) 1 else (currentInterval * ease).roundToInt().coerceAtLeast(1)
            ReviewGrade.Easy -> if (currentInterval == 0) 3 else (currentInterval * (ease + 0.35)).roundToInt().coerceAtLeast(2)
        }

    private fun dayString(offsetDays: Int): String {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, offsetDays)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun key(phraseId: String, field: String): String = "$phraseId.$field"

    private companion object {
        const val PREFERENCES_NAME = "duddy_portugues_spaced_reviews"
        const val DEFAULT_EASE = 2.30
        const val FIELD_DUE_DATE = "due_date"
        const val FIELD_INTERVAL_DAYS = "interval_days"
        const val FIELD_EASE_FACTOR = "ease_factor"
        const val FIELD_REVIEW_COUNT = "review_count"
        const val FIELD_CORRECT_STREAK = "correct_streak"
        const val FIELD_LAST_SCORE = "last_score"
    }
}
