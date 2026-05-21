package com.duddy.portugues.data.repository

import com.duddy.portugues.data.model.PhraseReviewState
import com.duddy.portugues.data.model.ReviewGrade
import kotlin.math.roundToInt

object SpacedReviewScheduler {
    const val DEFAULT_EASE = 2.30

    fun schedule(
        current: PhraseReviewState,
        grade: ReviewGrade,
        dueDateForInterval: (Int) -> String,
    ): PhraseReviewState {
        val nextEase = nextEase(current.easeFactor, grade)
        val nextInterval = nextInterval(current.intervalDays, nextEase, grade)
        return current.copy(
            dueDate = dueDateForInterval(nextInterval),
            intervalDays = nextInterval,
            easeFactor = nextEase,
            reviewCount = current.reviewCount + 1,
            correctStreak = if (grade == ReviewGrade.Again) 0 else current.correctStreak + 1,
        )
    }

    fun nextEase(currentEase: Double, grade: ReviewGrade): Double =
        when (grade) {
            ReviewGrade.Again -> currentEase - 0.25
            ReviewGrade.Hard -> currentEase - 0.10
            ReviewGrade.Good -> currentEase
            ReviewGrade.Easy -> currentEase + 0.15
        }.coerceIn(1.30, 2.80)

    fun nextInterval(currentInterval: Int, ease: Double, grade: ReviewGrade): Int =
        when (grade) {
            ReviewGrade.Again -> 0
            ReviewGrade.Hard -> 1
            ReviewGrade.Good -> if (currentInterval == 0) 1 else (currentInterval * ease).roundToInt().coerceAtLeast(1)
            ReviewGrade.Easy -> if (currentInterval == 0) 3 else (currentInterval * (ease + 0.35)).roundToInt().coerceAtLeast(2)
        }
}
