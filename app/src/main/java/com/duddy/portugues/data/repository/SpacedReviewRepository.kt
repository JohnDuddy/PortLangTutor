package com.duddy.portugues.data.repository

import com.duddy.portugues.data.model.PhraseReviewState
import com.duddy.portugues.data.model.ReviewGrade

interface SpacedReviewRepository {
    fun getReviewState(phraseId: String): PhraseReviewState?
    fun getDuePhraseIds(allPhraseIds: List<String>, limit: Int): List<String>
    fun getAdaptivePhraseIds(
        allPhraseIds: List<String>,
        favoritePhraseIds: Set<String>,
        limit: Int
    ): List<String> = getDuePhraseIds(allPhraseIds, limit)
    fun recordReview(phraseId: String, grade: ReviewGrade): PhraseReviewState
    fun recordPronunciationScore(phraseId: String, score: Int): PhraseReviewState? = getReviewState(phraseId)
}
