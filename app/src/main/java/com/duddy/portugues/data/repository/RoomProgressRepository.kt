package com.duddy.portugues.data.repository

import com.duddy.portugues.data.local.dao.ProgressStatsDao
import com.duddy.portugues.data.local.entity.ProgressStatsEntity
import com.duddy.portugues.data.model.GamificationRules
import com.duddy.portugues.data.model.ProgressStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.runBlocking

class RoomProgressRepository(
    private val dao: ProgressStatsDao,
) : ProgressRepository {

    override fun getStats(): ProgressStats = runBlocking {
        toModel(normalizePassiveState(dao.get() ?: ProgressStatsEntity()))
    }

    override fun recordLessonStarted() =
        bump(
            xp = GamificationRules.XP_LESSON_STARTED,
            countsAsStreakActivity = true,
        ) { it.copy(lessonsStarted = it.lessonsStarted + 1) }

    override fun recordPhrasePracticed() =
        bump(
            xp = GamificationRules.XP_PHRASE_PRACTICED,
            countsAsStreakActivity = true,
        ) { it.copy(practicedPhrases = it.practicedPhrases + 1) }

    override fun recordSampleAudioPlayed() =
        bump(xp = 0, countsAsStreakActivity = false) {
            it.copy(sampleAudioPlays = it.sampleAudioPlays + 1)
        }

    override fun recordSpeakingAttempt() =
        bump(
            xp = GamificationRules.XP_SPEAKING_ATTEMPT,
            countsAsStreakActivity = true,
        ) { it.copy(speakingAttempts = it.speakingAttempts + 1) }

    override fun recordPronunciationAssessed() =
        bump(
            xp = GamificationRules.XP_PRONUNCIATION_ASSESSED,
            countsAsStreakActivity = true,
        ) { it }

    override fun recordAiCoachRequest() =
        bump(
            xp = GamificationRules.XP_AI_COACH_REQUEST,
            countsAsStreakActivity = true,
        ) { it.copy(aiCoachRequests = it.aiCoachRequests + 1) }

    override fun recordMistake() = runBlocking {
        val current = normalizePassiveState(dao.get() ?: ProgressStatsEntity())
        dao.upsert(
            current.copy(
                hearts = (current.hearts - 1).coerceAtLeast(0),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override fun refillHearts() = runBlocking {
        val current = normalizePassiveState(dao.get() ?: ProgressStatsEntity())
        dao.upsert(
            current.copy(
                hearts = current.maxHearts.coerceAtLeast(1),
                lastHeartRefillAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override fun setDailyXpGoal(goalXp: Int) = runBlocking {
        val current = normalizePassiveState(dao.get() ?: ProgressStatsEntity())
        dao.upsert(
            current.copy(
                dailyXpGoal = goalXp.coerceIn(10, 500),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun bump(
        xp: Int,
        countsAsStreakActivity: Boolean,
        transform: (ProgressStatsEntity) -> ProgressStatsEntity,
    ) = runBlocking {
        val current = normalizePassiveState(dao.get() ?: ProgressStatsEntity())
        val withCounts = transform(current)
        val withStreak = if (countsAsStreakActivity) updateStreakForActivity(withCounts) else withCounts
        val withXp = if (xp > 0) addXp(withStreak, xp) else withStreak
        dao.upsert(withXp.copy(updatedAt = System.currentTimeMillis()))
    }

    private suspend fun normalizePassiveState(row: ProgressStatsEntity): ProgressStatsEntity {
        val today = dayString(0)
        val yesterday = dayString(-1)
        val now = System.currentTimeMillis()
        var updated = row

        val displayStreak = when (row.lastActiveDate) {
            today, yesterday -> row.streakDays
            else -> 0
        }.coerceAtLeast(0)
        if (displayStreak != row.streakDays) {
            updated = updated.copy(streakDays = displayStreak)
        }

        if (updated.todayXpDate != today) {
            updated = updated.copy(todayXp = 0, todayXpDate = today)
        }

        val week = weekString()
        if (updated.leagueWeekId != week) {
            updated = updated.copy(weeklyLeagueXp = 0, leagueWeekId = week)
        }

        updated = regenerateHearts(updated, now)

        if (updated != row) {
            dao.upsert(updated.copy(updatedAt = now))
        }
        return updated
    }

    private fun regenerateHearts(row: ProgressStatsEntity, now: Long): ProgressStatsEntity {
        val maxHearts = row.maxHearts.coerceAtLeast(1)
        val hearts = row.hearts.coerceIn(0, maxHearts)
        val refillIntervalMs = GamificationRules.HEART_REGEN_MINUTES * 60_000L
        val lastRefillAt = if (row.lastHeartRefillAt <= 0L) now else row.lastHeartRefillAt

        if (hearts >= maxHearts) {
            return row.copy(hearts = maxHearts, maxHearts = maxHearts, lastHeartRefillAt = now)
        }

        val gained = ((now - lastRefillAt) / refillIntervalMs).toInt().coerceAtLeast(0)
        if (gained == 0) {
            return row.copy(hearts = hearts, maxHearts = maxHearts, lastHeartRefillAt = lastRefillAt)
        }

        val newHearts = (hearts + gained).coerceAtMost(maxHearts)
        val newRefillAt = if (newHearts >= maxHearts) now else lastRefillAt + (gained * refillIntervalMs)
        return row.copy(
            hearts = newHearts,
            maxHearts = maxHearts,
            lastHeartRefillAt = newRefillAt,
        )
    }

    private fun updateStreakForActivity(row: ProgressStatsEntity): ProgressStatsEntity {
        val today = dayString(0)
        val yesterday = dayString(-1)
        val newStreak = when (row.lastActiveDate) {
            today -> row.streakDays.coerceAtLeast(1)
            yesterday -> row.streakDays + 1
            else -> 1
        }
        return row.copy(
            streakDays = newStreak,
            longestStreak = maxOf(newStreak, row.longestStreak),
            lastActiveDate = today,
        )
    }

    private fun addXp(row: ProgressStatsEntity, xp: Int): ProgressStatsEntity =
        row.copy(
            totalXp = row.totalXp + xp,
            todayXp = row.todayXp + xp,
            todayXpDate = dayString(0),
            weeklyLeagueXp = row.weeklyLeagueXp + xp,
            leagueWeekId = weekString(),
        )

    private fun toModel(row: ProgressStatsEntity): ProgressStats =
        ProgressStats(
            lessonsStarted = row.lessonsStarted,
            practicedPhrases = row.practicedPhrases,
            sampleAudioPlays = row.sampleAudioPlays,
            speakingAttempts = row.speakingAttempts,
            aiCoachRequests = row.aiCoachRequests,
            streakDays = row.streakDays,
            longestStreak = maxOf(row.longestStreak, row.streakDays),
            totalXp = row.totalXp,
            todayXp = row.todayXp,
            dailyXpGoal = row.dailyXpGoal,
            hearts = row.hearts,
            maxHearts = row.maxHearts,
            weeklyLeagueXp = row.weeklyLeagueXp,
            leagueName = row.leagueName,
        )

    private fun dayString(offset: Int): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun weekString(): String =
        SimpleDateFormat("YYYY-'W'ww", Locale.US).format(Calendar.getInstance().time)
}
