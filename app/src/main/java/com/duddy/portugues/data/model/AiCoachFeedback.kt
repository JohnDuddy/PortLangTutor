package com.duddy.portugues.data.model

data class AiCoachFeedback(
    val message: String,
    val score: Int? = null,
    val fix: String = "",
    val model: String = "",
    val nextRep: String = "",
    val encouragement: String = "",
    val focusArea: String = "",
    val adaptiveGrade: ReviewGrade? = null,
    val provider: String = "",
)
