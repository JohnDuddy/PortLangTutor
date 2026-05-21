package com.duddy.portugues

import com.duddy.portugues.data.repository.PhraseSearchNormalizer
import com.duddy.portugues.presentation.TutorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhraseSearchTest {
    @Test
    fun normalize_stripsAccentsLowercasesAndTrims() {
        assertEquals(
            "ola, saida",
            PhraseSearchNormalizer.normalize("  Olá, SAÍDA  "),
        )
    }

    @Test
    fun search_matchesPortugueseAccentsAndEnglishFields() {
        val viewModel = testViewModel()

        viewModel.updatePhraseSearchQuery("saida")
        assertEquals(listOf("survival-exit"), viewModel.uiState.phraseSearchResults.map { it.id })

        viewModel.updatePhraseSearchQuery("doctor")
        assertEquals(listOf("medical-doctor"), viewModel.uiState.phraseSearchResults.map { it.id })
    }

    @Test
    fun search_matchesPronunciationGuideAndCategory() {
        val viewModel = testViewModel()

        viewModel.updatePhraseSearchQuery("ah-gwah")
        assertEquals(listOf("restaurant-water"), viewModel.uiState.phraseSearchResults.map { it.id })

        viewModel.updatePhraseSearchQuery("travel")
        assertTrue(viewModel.uiState.phraseSearchResults.any { it.id == "travel-airport" })
    }

    private fun testViewModel(): TutorViewModel =
        TutorViewModel(
            progressRepository = FakeProgressRepository(),
            favoritePhraseRepository = FakeFavoritePhraseRepository(),
            spacedReviewRepository = FakeSpacedReviewRepository(),
            dailyGoalRepository = FakeDailyGoalRepository(),
            phraseRepository = FakePhraseRepository(),
            aiCoachRepository = FakeAiCoachRepository(),
        )
}
