package com.duddy.portugues.data.repository

import com.duddy.portugues.data.model.Lesson
import com.duddy.portugues.data.model.Phrase
import com.duddy.portugues.data.model.PhraseCategory
import com.duddy.portugues.data.model.PhraseLibraryStats

interface PhraseRepository {
    fun getLessons(): List<Lesson>
    fun getPhrases(): List<Phrase>
    fun getPhrasesForCategory(category: PhraseCategory): List<Phrase>
    fun getLibraryStats(): PhraseLibraryStats =
        PhraseLibraryStats(
            phraseCount = getPhrases().size,
            categoryCount = getPhrases().map { it.category }.distinct().size,
        )
}
