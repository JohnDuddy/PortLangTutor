package com.duddy.portugues

import com.duddy.portugues.data.model.LearningTechnique
import com.duddy.portugues.data.model.ReviewGrade
import com.duddy.portugues.presentation.AppScreen
import com.duddy.portugues.presentation.TutorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorViewModelTest {
    @Test
    fun gradeCurrentPhrase_recordsReviewAdvancesAndResetsPracticeState() {
        val spacedReviewRepository = FakeSpacedReviewRepository()
        val progressRepository = FakeProgressRepository()
        val dailyGoalRepository = FakeDailyGoalRepository()
        val viewModel = testViewModel(
            progressRepository = progressRepository,
            spacedReviewRepository = spacedReviewRepository,
            dailyGoalRepository = dailyGoalRepository,
        )
        val firstPhrase = viewModel.uiState.currentPhrase!!

        viewModel.revealCurrentPhrase()
        viewModel.updateSpokenText("onde fica a saida")
        viewModel.gradeCurrentPhrase(ReviewGrade.Good)

        assertEquals(listOf(firstPhrase.id to ReviewGrade.Good), spacedReviewRepository.recordedReviews)
        assertEquals(1, viewModel.uiState.currentPhraseIndex)
        assertFalse(viewModel.uiState.isAnswerRevealed)
        assertEquals(LearningTechnique.RetrievalPractice, viewModel.uiState.currentTechnique)
        assertEquals("", viewModel.uiState.spokenText)
        assertEquals(1, progressRepository.currentStats.practicedPhrases)
        assertEquals(1, dailyGoalRepository.progress.completedNewPhrases)
    }

    @Test
    fun eachReviewGradeProducesExpectedSchedulingShape() {
        val schedules = ReviewGrade.entries.associateWith { grade ->
            val spacedReviewRepository = FakeSpacedReviewRepository()
            val viewModel = testViewModel(spacedReviewRepository = spacedReviewRepository)

            viewModel.gradeCurrentPhrase(grade)

            spacedReviewRepository.states.getValue("survival-exit")
        }

        assertEquals(0, schedules.getValue(ReviewGrade.Again).intervalDays)
        assertEquals(1, schedules.getValue(ReviewGrade.Hard).intervalDays)
        assertEquals(1, schedules.getValue(ReviewGrade.Good).intervalDays)
        assertEquals(3, schedules.getValue(ReviewGrade.Easy).intervalDays)
        assertNotEquals(
            schedules.getValue(ReviewGrade.Hard).easeFactor,
            schedules.getValue(ReviewGrade.Good).easeFactor,
            0.001,
        )
        assertTrue(schedules.getValue(ReviewGrade.Easy).easeFactor > schedules.getValue(ReviewGrade.Good).easeFactor)
    }

    @Test
    fun startSmartReview_withEmptyDueListShowsMessageAndKeepsScreen() {
        val spacedReviewRepository = FakeSpacedReviewRepository().apply {
            dueIds = emptyList()
        }
        val viewModel = testViewModel(spacedReviewRepository = spacedReviewRepository)

        viewModel.startSmartReview()

        assertEquals(AppScreen.Home, viewModel.uiState.currentScreen)
        assertFalse(viewModel.uiState.isSmartReviewSession)
        assertTrue(viewModel.uiState.statusMessage.contains("No due reviews"))
    }

    @Test
    fun startSmartReview_withDuePhrasesOpensPracticeSession() {
        val spacedReviewRepository = FakeSpacedReviewRepository().apply {
            dueIds = listOf("restaurant-water", "survival-exit")
        }
        val viewModel = testViewModel(spacedReviewRepository = spacedReviewRepository)

        viewModel.startSmartReview()

        assertEquals(AppScreen.Practice, viewModel.uiState.currentScreen)
        assertTrue(viewModel.uiState.isSmartReviewSession)
        assertFalse(viewModel.uiState.isGuidedSession)
        assertEquals(
            listOf("restaurant-water", "survival-exit"),
            viewModel.uiState.activePhrases.map { it.id },
        )
    }

    @Test
    fun startGuidedSession_usesAdaptiveIdsAndInterleavesSession() {
        val spacedReviewRepository = FakeSpacedReviewRepository().apply {
            adaptiveIds = listOf("restaurant-water", "medical-doctor", "travel-airport")
        }
        val viewModel = testViewModel(spacedReviewRepository = spacedReviewRepository)

        viewModel.startGuidedSession()

        assertEquals(1, spacedReviewRepository.adaptiveRequestCount)
        assertEquals(AppScreen.Practice, viewModel.uiState.currentScreen)
        assertTrue(viewModel.uiState.isSmartReviewSession)
        assertTrue(viewModel.uiState.isGuidedSession)
        assertEquals(
            listOf("restaurant-water", "medical-doctor", "travel-airport"),
            viewModel.uiState.activePhrases.map { it.id },
        )
    }

    @Test
    fun nextPhrase_wrapsAroundAtEnd() {
        val viewModel = testViewModel()

        viewModel.practiceAllPhrases()
        viewModel.showPhraseAt(viewModel.uiState.activePhrases.lastIndex)
        viewModel.nextPhrase()

        assertEquals(0, viewModel.uiState.currentPhraseIndex)
        assertFalse(viewModel.uiState.isAnswerRevealed)
    }

    @Test
    fun toggleFavoritePhrase_togglesSetInUiState() {
        val viewModel = testViewModel()
        val phrase = viewModel.uiState.allPhrases.first()

        viewModel.toggleFavoritePhrase(phrase)
        assertTrue(phrase.id in viewModel.uiState.favoritePhraseIds)

        viewModel.toggleFavoritePhrase(phrase)
        assertFalse(phrase.id in viewModel.uiState.favoritePhraseIds)
    }

    private fun testViewModel(
        progressRepository: FakeProgressRepository = FakeProgressRepository(),
        favoritePhraseRepository: FakeFavoritePhraseRepository = FakeFavoritePhraseRepository(),
        spacedReviewRepository: FakeSpacedReviewRepository = FakeSpacedReviewRepository(),
        dailyGoalRepository: FakeDailyGoalRepository = FakeDailyGoalRepository(),
    ): TutorViewModel =
        TutorViewModel(
            progressRepository = progressRepository,
            favoritePhraseRepository = favoritePhraseRepository,
            spacedReviewRepository = spacedReviewRepository,
            dailyGoalRepository = dailyGoalRepository,
            phraseRepository = FakePhraseRepository(),
            aiCoachRepository = FakeAiCoachRepository(),
        )
}
