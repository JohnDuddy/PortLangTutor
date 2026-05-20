package com.duddy.portugues.data.repository

import android.content.Context
import android.util.Log
import com.duddy.portugues.data.model.Lesson
import com.duddy.portugues.data.model.Phrase
import com.duddy.portugues.data.model.PhraseCategory
import com.duddy.portugues.data.model.PhraseDifficulty
import com.duddy.portugues.data.model.PhraseLibraryStats

class LocalPhraseRepository(
    context: Context? = null,
) : PhraseRepository {
    private val loadResult: PhraseLoadResult = runCatching {
        requireNotNull(context) { "Context required to load phrase assets." }
        PhraseAssetLoader.load(context.applicationContext)
    }.getOrElse { error ->
        Log.w(TAG, "Using fallback phrase list because asset loading failed.", error)
        PhraseLoadResult(
            phrases = fallbackPhrases(),
            stats = PhraseLibraryStats(
                phraseCount = fallbackPhrases().size,
                categoryCount = fallbackPhrases().map { it.category }.distinct().size,
                warnings = listOf("Phrase asset loading failed: ${error.message}"),
            ),
        )
    }

    private val phrases = loadResult.phrases

    override fun getLessons(): List<Lesson> =
        PhraseCategory.entries.mapNotNull { category ->
            val count = phrases.count { phrase -> phrase.category == category }
            if (count == 0) {
                null
            } else {
                Lesson(
                    id = category.name,
                    title = category.displayName,
                    description = lessonDescription(category),
                    category = category,
                    phraseCount = count,
                )
            }
        }

    override fun getPhrases(): List<Phrase> = phrases

    override fun getPhrasesForCategory(category: PhraseCategory): List<Phrase> =
        phrases.filter { phrase -> phrase.category == category }

    override fun getLibraryStats(): PhraseLibraryStats = loadResult.stats

    private fun lessonDescription(category: PhraseCategory): String =
        when (category) {
            PhraseCategory.Survival -> "Immediate needs, safety, clarity, and essential interactions."
            PhraseCategory.Travel -> "Airports, hotels, planning, documents, and visitor situations."
            PhraseCategory.Restaurant -> "Ordering, preferences, allergies, payment, and dining flow."
            PhraseCategory.Conversation -> "Everyday small talk, opinions, follow-ups, and stories."
            PhraseCategory.DatingSocial -> "Meeting people, invitations, compliments, and social plans."
            PhraseCategory.Medical -> "Symptoms, medicine, appointments, emergencies, and care."
            PhraseCategory.Work -> "Meetings, schedules, tasks, feedback, and professional phrases."
            PhraseCategory.Shopping -> "Prices, sizes, returns, receipts, and store interactions."
            PhraseCategory.Transportation -> "Rides, routes, tickets, stations, and directions in motion."
            PhraseCategory.Emotions -> "Feelings, preferences, reactions, and personal state."
            PhraseCategory.Questions -> "Question frames that unlock flexible conversation."
            PhraseCategory.Verbs -> "High-frequency verbs in useful sentence chunks."
            PhraseCategory.SentencePatterns -> "Reusable patterns for building new sentences."
        }

    private fun fallbackPhrases(): List<Phrase> =
        listOf(
            Phrase(
                id = "fallback-bom-dia",
                portuguese = "Bom dia.",
                english = "Good morning.",
                pronunciationGuide = "bohm JEE-ah",
                category = PhraseCategory.Conversation,
                subcategory = "greetings",
                difficulty = PhraseDifficulty.A1,
                tags = listOf("greeting", "daily"),
            ),
            Phrase(
                id = "fallback-preciso-ajuda",
                portuguese = "Preciso de ajuda.",
                english = "I need help.",
                pronunciationGuide = "preh-SEE-zoo jee ah-JOO-dah",
                category = PhraseCategory.Survival,
                subcategory = "help",
                difficulty = PhraseDifficulty.A1,
                tags = listOf("help", "survival"),
            ),
            Phrase(
                id = "fallback-fale-devagar",
                portuguese = "Pode falar mais devagar?",
                english = "Can you speak more slowly?",
                pronunciationGuide = "POH-jee fah-LAHR myz deh-vah-GAHR",
                category = PhraseCategory.Questions,
                subcategory = "clarification",
                difficulty = PhraseDifficulty.A1,
                tags = listOf("question", "clarification"),
            ),
        )

    private companion object {
        const val TAG = "LocalPhraseRepository"
    }
}
