package com.duddy.portugues.data.model

data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val category: PhraseCategory,
    val phraseCount: Int
)
