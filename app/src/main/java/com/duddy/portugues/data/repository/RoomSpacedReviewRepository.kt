package com.duddy.portugues.data.repository

import com.duddy.portugues.data.local.dao.PhraseReviewDao
import com.duddy.portugues.data.local.entity.PhraseReviewEntity
import com.duddy.portugues.data.model.PhraseReviewState
import com.duddy.portugues.data.model.ReviewGrade
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Room-backed implementation of [SpacedReviewRepository].
 *
 * Interface is sync (returns values directly); since the existing TutorViewModel
 * calls these methods from non-suspend code paths we use runBlocking on Dispatchers.IO.
 * Calls are fast (single-row SQLite reads) so this is acceptable.
 *
 * For a fully-async future refactor, see the `*Suspend` variants below.
 */
class RoomSpacedReviewRepository(
    private val dao: PhraseReviewDao,
) : SpacedReviewRepository {

    override fun getReviewState(phraseId: String): PhraseReviewState? = runBlocking {
        dao.getById(phraseId)?.toModel()
    }

    override fun getDuePhraseIds(allPhraseIds: List<String>, limit: Int): List<String> = runBlocking {
        val today    = dayString(0)
        val reviewed = dao.getAllReviewedIds().toSet()

        // Never-seen phrases are "due" (state == null in the old logic)
        val newOnes = allPhraseIds.filter { it !in reviewed }
        val dueOnes = dao.getDueIds(today, limit)

        // Match the old behaviour: maintain original ordering, mix new + due
        (newOnes + dueOnes).distinct().take(limit)
    }

    override fun getAdaptivePhraseIds(
        allPhraseIds: List<String>,
        favoritePhraseIds: Set<String>,
        limit: Int
    ): List<String> = runBlocking {
        if (allPhraseIds.isEmpty()) return@runBlocking emptyList()

        val today = dayString(0)
        val reviews = dao.getByIds(allPhraseIds).associateBy { it.phraseId }

        allPhraseIds
            .sortedWith(
                compareByDescending<String> { phraseId ->
                    val state = reviews[phraseId]
                    val isFavorite = phraseId in favoritePhraseIds
                    adaptiveScore(state, today, isFavorite)
                }.thenBy { phraseId -> allPhraseIds.indexOf(phraseId) }
            )
            .take(limit)
    }

    override fun recordReview(phraseId: String, grade: ReviewGrade): PhraseReviewState = runBlocking {
        val current = dao.getById(phraseId) ?: PhraseReviewEntity(
            phraseId      = phraseId,
            dueDate       = dayString(0),
            intervalDays  = 0,
            easeFactor    = DEFAULT_EASE,
            reviewCount   = 0,
            correctStreak = 0,
        )

        val scheduled = SpacedReviewScheduler.schedule(current.toModel(), grade, ::dayString)
        val next = current.copy(
            dueDate       = scheduled.dueDate,
            intervalDays  = scheduled.intervalDays,
            easeFactor    = scheduled.easeFactor,
            reviewCount   = scheduled.reviewCount,
            correctStreak = scheduled.correctStreak,
            updatedAt     = System.currentTimeMillis(),
            synced        = false,
        )
        dao.upsert(next)
        next.toModel()
    }

    override fun recordPronunciationScore(phraseId: String, score: Int): PhraseReviewState? = runBlocking {
        val current = dao.getById(phraseId) ?: PhraseReviewEntity(
            phraseId      = phraseId,
            dueDate       = dayString(0),
            intervalDays  = 0,
            easeFactor    = DEFAULT_EASE,
            reviewCount   = 0,
            correctStreak = 0,
        )
        val next = current.copy(
            lastScore = score.coerceIn(0, 100),
            updatedAt = System.currentTimeMillis(),
            synced = false,
        )
        dao.upsert(next)
        next.toModel()
    }

    private fun dayString(offsetDays: Int): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offsetDays) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun adaptiveScore(
        state: PhraseReviewEntity?,
        today: String,
        isFavorite: Boolean,
    ): Int {
        val dueBonus = when {
            state == null -> 700
            state.dueDate <= today -> 600
            else -> 100
        }
        val weaknessBonus = state?.lastScore?.let { 100 - it.coerceIn(0, 100) } ?: 30
        val newnessBonus = state?.reviewCount?.let { (8 - it).coerceAtLeast(0) * 8 } ?: 80
        val favoriteBonus = if (isFavorite) 35 else 0
        val lapseBonus = state?.correctStreak?.let { if (it == 0) 20 else 0 } ?: 0
        return dueBonus + weaknessBonus + newnessBonus + favoriteBonus + lapseBonus
    }

    private fun PhraseReviewEntity.toModel() = PhraseReviewState(
        phraseId      = phraseId,
        dueDate       = dueDate,
        intervalDays  = intervalDays,
        easeFactor    = easeFactor,
        reviewCount   = reviewCount,
        correctStreak = correctStreak,
        lastScore     = lastScore,
    )

    private companion object {
        const val DEFAULT_EASE = SpacedReviewScheduler.DEFAULT_EASE
    }
}
