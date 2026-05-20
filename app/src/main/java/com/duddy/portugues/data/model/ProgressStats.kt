package com.duddy.portugues.data.model

data class ProgressStats(
    val lessonsStarted: Int,
    val practicedPhrases: Int,
    val sampleAudioPlays: Int,
    val speakingAttempts: Int,
    val aiCoachRequests: Int,
    val streakDays: Int,
    val longestStreak: Int = 0,
    val totalXp: Int = 0,
    val todayXp: Int = 0,
    val dailyXpGoal: Int = GamificationRules.DEFAULT_DAILY_XP_GOAL,
    val hearts: Int = GamificationRules.DEFAULT_MAX_HEARTS,
    val maxHearts: Int = GamificationRules.DEFAULT_MAX_HEARTS,
    val weeklyLeagueXp: Int = 0,
    val leagueName: String = "Bronze",
) {
    val level: Int
        get() = GamificationRules.levelForXp(totalXp)

    val currentLevelMinXp: Int
        get() = GamificationRules.currentLevelMinXp(totalXp)

    val nextLevelMinXp: Int
        get() = GamificationRules.nextLevelMinXp(totalXp)

    val xpIntoCurrentLevel: Int
        get() = (totalXp - currentLevelMinXp).coerceAtLeast(0)

    val xpNeededForNextLevel: Int
        get() = (nextLevelMinXp - currentLevelMinXp).coerceAtLeast(1)

    val levelProgressPercent: Int
        get() = ((xpIntoCurrentLevel * 100) / xpNeededForNextLevel).coerceIn(0, 100)

    val dailyXpPercent: Int
        get() = if (dailyXpGoal <= 0) 0 else ((todayXp * 100) / dailyXpGoal).coerceIn(0, 100)

    val isDailyXpGoalComplete: Boolean
        get() = todayXp >= dailyXpGoal

    val hasHearts: Boolean
        get() = hearts > 0
}
