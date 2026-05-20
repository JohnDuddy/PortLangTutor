package com.duddy.portugues.data.repository

import com.duddy.portugues.data.local.dao.PhraseReviewDao
import com.duddy.portugues.data.local.entity.PhraseReviewEntity
import com.duddy.portugues.data.model.PhraseReviewState
import com.duddy.portugues.data.model.ReviewGrade
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

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

        val nextEase     = nextEase(current.easeFactor, grade)
        val nextInterval = nextInterval(current.intervalDays, nextEase, grade)
        val next = current.copy(
            dueDate       = dayString(nextInterval),
            intervalDays  = nextInterval,
            easeFactor    = nextEase,
            reviewCount   = current.reviewCount + 1,
            correctStreak = if (grade == ReviewGrade.Again) 0 else current.correctStreak + 1,
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

    // ── SRS math (same as SharedPrefs version) ─────────────────────────────
    private fun nextEase(current: Double, grade: ReviewGrade): Double =
        when (grade) {
            ReviewGrade.Again -> current - 0.25
            ReviewGrade.Hard  -> current - 0.10
            ReviewGrade.Good  -> current
            ReviewGrade.Easy  -> current + 0.15
        }.coerceIn(1.30, 2.80)

    private fun nextInterval(current: Int, ease: Double, grade: ReviewGrade): Int =
        when (grade) {
            ReviewGrade.Again -> 0
            ReviewGrade.Hard  -> 1
            ReviewGrade.Good  -> if (current == 0) 1 else (current * ease).roundToInt().coerceAtLeast(1)
            ReviewGrade.Easy  -> if (current == 0) 3 else (current * (ease + 0.35)).roundToInt().coerceAtLeast(2)
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
        const val DEFAULT_EASE = 2.30
    }
}
