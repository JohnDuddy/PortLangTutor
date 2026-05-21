package com.duddy.portugues

import com.duddy.portugues.data.model.DailyGoalProgress
import com.duddy.portugues.data.model.DailyGoalTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyGoalProgressTest {
    @Test
    fun percentComplete_capsEachMetricAtItsTarget() {
        val progress = DailyGoalProgress(
            date = "2026-05-20",
            targets = DailyGoalTargets(
                newPhrasesPerDay = 2,
                reviewsPerDay = 2,
                speakingAttemptsPerDay = 2,
                practiceMinutesPerDay = 2,
                aiCoachRequestsPerDay = 2,
            ),
            completedNewPhrases = 10,
            completedReviews = 1,
            completedSpeakingAttempts = 1,
            completedPracticeMinutes = 1,
            completedAiCoachRequests = 0,
        )

        assertEquals(50, progress.percentComplete)
        assertFalse(progress.isComplete)
    }

    @Test
    fun isComplete_whenAllTargetsReached() {
        val progress = DailyGoalProgress(
            date = "2026-05-20",
            completedNewPhrases = 5,
            completedReviews = 15,
            completedSpeakingAttempts = 5,
            completedPracticeMinutes = 10,
            completedAiCoachRequests = 2,
        )

        assertEquals(100, progress.percentComplete)
        assertTrue(progress.isComplete)
    }

    @Test
    fun encouragementMessage_tracksProgressBands() {
        val starting = DailyGoalProgress(date = "2026-05-20")
        val streak = DailyGoalProgress(date = "2026-05-20", streakDays = 3)
        val mid = DailyGoalProgress(date = "2026-05-20", completedReviews = 15)
        val almost = DailyGoalProgress(
            date = "2026-05-20",
            completedNewPhrases = 5,
            completedReviews = 15,
            completedPracticeMinutes = 10,
        )
        val complete = DailyGoalProgress(
            date = "2026-05-20",
            completedNewPhrases = 5,
            completedReviews = 15,
            completedSpeakingAttempts = 5,
            completedPracticeMinutes = 10,
            completedAiCoachRequests = 2,
        )

        assertEquals("Start small: five minutes and one spoken phrase.", starting.encouragementMessage)
        assertEquals("Protect your 3-day streak with a quick session.", streak.encouragementMessage)
        assertEquals("Good momentum. Keep the session moving.", mid.encouragementMessage)
        assertEquals("Almost there. A short speaking round will finish strong.", almost.encouragementMessage)
        assertEquals("Daily goal complete. Lock it in with one easy review.", complete.encouragementMessage)
    }
}
