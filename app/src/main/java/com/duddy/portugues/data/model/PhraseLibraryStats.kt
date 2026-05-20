package com.duddy.portugues.data.model

data class PhraseLibraryStats(
    val phraseCount: Int,
    val categoryCount: Int,
    val warnings: List<String> = emptyList(),
) {
    val isValid: Boolean = warnings.isEmpty()
}
