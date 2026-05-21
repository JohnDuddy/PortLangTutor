package com.duddy.portugues

import com.duddy.portugues.data.model.PhraseReviewState
import com.duddy.portugues.data.model.ReviewGrade
import com.duddy.portugues.data.repository.SpacedReviewScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedReviewLogicTest {
    @Test
    fun again_producesShortIntervalAndResetsCorrectStreak() {
        val result = SpacedReviewScheduler.schedule(
            current = reviewState(intervalDays = 6, correctStreak = 3),
            grade = ReviewGrade.Again,
            dueDateForInterval = ::dueDate,
        )

        assertEquals(0, result.intervalDays)
        assertEquals("2026-05-20", result.dueDate)
        assertEquals(0, result.correctStreak)
        assertTrue(result.easeFactor < SpacedReviewScheduler.DEFAULT_EASE)
    }

    @Test
    fun hard_producesOneDayInterval() {
        val result = SpacedReviewScheduler.schedule(
            current = reviewState(intervalDays = 4, correctStreak = 1),
            grade = ReviewGrade.Hard,
            dueDateForInterval = ::dueDate,
        )

        assertEquals(1, result.intervalDays)
        assertEquals("2026-05-21", result.dueDate)
        assertEquals(2, result.correctStreak)
    }

    @Test
    fun good_intervalIncreasesAfterSuccessfulReviews() {
        val firstGood = SpacedReviewScheduler.schedule(
            current = reviewState(intervalDays = 0),
            grade = ReviewGrade.Good,
            dueDateForInterval = ::dueDate,
        )
        val laterGood = SpacedReviewScheduler.schedule(
            current = firstGood.copy(intervalDays = 4, correctStreak = 2),
            grade = ReviewGrade.Good,
            dueDateForInterval = ::dueDate,
        )

        assertEquals(1, firstGood.intervalDays)
        assertTrue(laterGood.intervalDays > firstGood.intervalDays)
    }

    @Test
    fun easy_producesLongerIntervalThanGood() {
        val current = reviewState(intervalDays = 5)
        val good = SpacedReviewScheduler.schedule(current, ReviewGrade.Good, ::dueDate)
        val easy = SpacedReviewScheduler.schedule(current, ReviewGrade.Easy, ::dueDate)

        assertTrue(easy.intervalDays > good.intervalDays)
        assertTrue(easy.easeFactor > good.easeFactor)
    }

    private fun reviewState(
        intervalDays: Int = 0,
        correctStreak: Int = 0,
    ): PhraseReviewState =
        PhraseReviewState(
            phraseId = "phrase-1",
            dueDate = "2026-05-20",
            intervalDays = intervalDays,
            easeFactor = SpacedReviewScheduler.DEFAULT_EASE,
            reviewCount = 1,
            correctStreak = correctStreak,
        )

    private fun dueDate(offsetDays: Int): String =
        when (offsetDays) {
            0 -> "2026-05-20"
            1 -> "2026-05-21"
            3 -> "2026-05-23"
            else -> "2026-05-${20 + offsetDays}"
        }
}
