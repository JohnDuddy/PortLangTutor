package com.duddy.portugues.data.model

data class PhraseReviewState(
    val phraseId: String,
    val dueDate: String,
    val intervalDays: Int,
    val easeFactor: Double,
    val reviewCount: Int,
    val correctStreak: Int,
    val lastScore: Int? = null
)
