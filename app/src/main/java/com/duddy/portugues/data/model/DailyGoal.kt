package com.duddy.portugues.data.model

data class DailyGoalTargets(
    val newPhrasesPerDay: Int = 5,
    val reviewsPerDay: Int = 15,
    val speakingAttemptsPerDay: Int = 5,
    val practiceMinutesPerDay: Int = 10,
    val aiCoachRequestsPerDay: Int = 2,
)

data class DailyGoalProgress(
    val date: String,
    val targets: DailyGoalTargets = DailyGoalTargets(),
    val completedNewPhrases: Int = 0,
    val completedReviews: Int = 0,
    val completedSpeakingAttempts: Int = 0,
    val completedPracticeMinutes: Int = 0,
    val completedAiCoachRequests: Int = 0,
    val streakDays: Int = 0,
) {
    val completedItems: Int
        get() =
            completedNewPhrases.coerceAtMost(targets.newPhrasesPerDay) +
                completedReviews.coerceAtMost(targets.reviewsPerDay) +
                completedSpeakingAttempts.coerceAtMost(targets.speakingAttemptsPerDay) +
                completedPracticeMinutes.coerceAtMost(targets.practiceMinutesPerDay) +
                completedAiCoachRequests.coerceAtMost(targets.aiCoachRequestsPerDay)

    val targetItems: Int
        get() =
            targets.newPhrasesPerDay +
                targets.reviewsPerDay +
                targets.speakingAttemptsPerDay +
                targets.practiceMinutesPerDay +
                targets.aiCoachRequestsPerDay

    val percentComplete: Int
        get() = if (targetItems == 0) 0 else ((completedItems * 100) / targetItems).coerceIn(0, 100)

    val isComplete: Boolean
        get() = percentComplete >= 100

    val encouragementMessage: String
        get() = when {
            isComplete -> "Daily goal complete. Lock it in with one easy review."
            percentComplete >= 75 -> "Almost there. A short speaking round will finish strong."
            percentComplete >= 40 -> "Good momentum. Keep the session moving."
            streakDays > 1 -> "Protect your $streakDays-day streak with a quick session."
            else -> "Start small: five minutes and one spoken phrase."
        }
}
