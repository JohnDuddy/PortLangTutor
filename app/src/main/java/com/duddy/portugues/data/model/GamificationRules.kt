package com.duddy.portugues.data.model

object GamificationRules {
    const val DEFAULT_MAX_HEARTS = 5
    const val DEFAULT_DAILY_XP_GOAL = 50
    const val HEART_REGEN_MINUTES = 30

    const val XP_LESSON_STARTED = 10
    const val XP_PHRASE_PRACTICED = 5
    const val XP_SPEAKING_ATTEMPT = 10
    const val XP_PRONUNCIATION_ASSESSED = 15
    const val XP_AI_COACH_REQUEST = 5

    private val levelMinimumXp = listOf(
        0,
        101,
        251,
        451,
        701,
        1001,
        1401,
        1901,
        2501,
        3201,
        4001,
        5001,
    )

    fun levelForXp(totalXp: Int): Int {
        val safeXp = totalXp.coerceAtLeast(0)
        val index = levelMinimumXp.indexOfLast { minXp -> safeXp >= minXp }
        return (index + 1).coerceAtLeast(1)
    }

    fun currentLevelMinXp(totalXp: Int): Int =
        levelMinimumXp.getOrElse(levelForXp(totalXp) - 1) { levelMinimumXp.last() }

    fun nextLevelMinXp(totalXp: Int): Int {
        val level = levelForXp(totalXp)
        return levelMinimumXp.getOrNull(level) ?: (levelMinimumXp.last() + ((level - levelMinimumXp.size + 1) * 1200))
    }
}
