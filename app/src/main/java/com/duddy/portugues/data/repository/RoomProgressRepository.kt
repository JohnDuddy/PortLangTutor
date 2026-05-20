package com.duddy.portugues.data.repository

import com.duddy.portugues.data.local.dao.ProgressStatsDao
import com.duddy.portugues.data.local.entity.ProgressStatsEntity
import com.duddy.portugues.data.model.ProgressStats
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RoomProgressRepository(
    private val dao: ProgressStatsDao,
) : ProgressRepository {

    override fun getStats(): ProgressStats = runBlocking {
        refreshStreak()
        val row = dao.get() ?: ProgressStatsEntity()
        ProgressStats(
            lessonsStarted   = row.lessonsStarted,
            practicedPhrases = row.practicedPhrases,
            sampleAudioPlays = row.sampleAudioPlays,
            speakingAttempts = row.speakingAttempts,
            aiCoachRequests  = row.aiCoachRequests,
            streakDays       = row.streakDays.coerceAtLeast(1),
        )
    }

    override fun recordLessonStarted()     = bump { it.copy(lessonsStarted   = it.lessonsStarted   + 1) }
    override fun recordPhrasePracticed()   = bump { it.copy(practicedPhrases = it.practicedPhrases + 1) }
    override fun recordSampleAudioPlayed() = bump { it.copy(sampleAudioPlays = it.sampleAudioPlays + 1) }
    override fun recordSpeakingAttempt()   = bump { it.copy(speakingAttempts = it.speakingAttempts + 1) }
    override fun recordAiCoachRequest()    = bump { it.copy(aiCoachRequests  = it.aiCoachRequests  + 1) }

    private fun bump(transform: (ProgressStatsEntity) -> ProgressStatsEntity) = runBlocking {
        refreshStreak()
        val current = dao.get() ?: ProgressStatsEntity()
        dao.upsert(transform(current).copy(updatedAt = System.currentTimeMillis()))
    }

    private suspend fun refreshStreak() {
        val today     = dayString(0)
        val yesterday = dayString(-1)
        val current   = dao.get() ?: ProgressStatsEntity()
        val newStreak = when (current.lastActiveDate) {
            today     -> current.streakDays.coerceAtLeast(1)
            yesterday -> current.streakDays + 1
            null      -> 1
            else      -> 1
        }
        if (current.lastActiveDate != today || current.streakDays != newStreak) {
            dao.upsert(
                current.copy(
                    streakDays     = newStreak,
                    longestStreak  = maxOf(newStreak, current.longestStreak),
                    lastActiveDate = today,
                    updatedAt      = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun dayString(offset: Int): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
}
