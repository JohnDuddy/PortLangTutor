package com.duddy.portugues.presentation

import com.duddy.portugues.BuildConfig
import com.duddy.portugues.data.model.DailyGoalProgress
import com.duddy.portugues.data.model.Lesson
import com.duddy.portugues.data.model.GuidedSessionStep
import com.duddy.portugues.data.model.LearningTechnique
import com.duddy.portugues.data.model.Phrase
import com.duddy.portugues.data.model.PhraseLibraryStats
import com.duddy.portugues.data.model.ProgressStats
import com.duddy.portugues.data.model.PronunciationResult

data class TutorUiState(
    val currentScreen: AppScreen = AppScreen.Home,
    val lessons: List<Lesson> = emptyList(),
    val allPhrases: List<Phrase> = emptyList(),
    val activeLesson: Lesson? = null,
    val activePhrases: List<Phrase> = emptyList(),
    val currentPhraseIndex: Int = 0,
    val categoryAudioRequestToken: Int = 0,
    val isSmartReviewSession: Boolean = false,
    val isGuidedSession: Boolean = false,
    val isAnswerRevealed: Boolean = false,
    val currentTechnique: LearningTechnique = LearningTechnique.RetrievalPractice,
    val guidedSessionSteps: List<GuidedSessionStep> = emptyList(),
    val dueReviewCount: Int = 0,
    val phraseLibraryStats: PhraseLibraryStats = PhraseLibraryStats(0, 0),
    val phraseSearchQuery: String = "",
    val phraseSearchResults: List<Phrase> = emptyList(),
    val favoritePhraseIds: Set<String> = emptySet(),
    val statusMessage: String = "Choose a lesson to begin.",
    val spokenText: String = "",

    /**
     * Default points at the FastAPI backend's /v1/coach endpoint. The previous
     * Node `/coach` endpoint at port 8787 is still supported if you point this
     * field at it manually — the AI coach repository handles both shapes.
     */
    val aiServerUrl: String = "${BuildConfig.DUDDY_BACKEND_URL}/v1/coach",

    val aiFeedback: String = "",
    val isAiLoading: Boolean = false,

    // ── Pronunciation assessment state ─────────────────────────────────────
    val isRecording: Boolean = false,
    val isAssessing: Boolean = false,
    val pronunciationResult: PronunciationResult? = null,
    val pronunciationError: String? = null,

    val stats: ProgressStats = ProgressStats(
        lessonsStarted = 0,
        practicedPhrases = 0,
        sampleAudioPlays = 0,
        speakingAttempts = 0,
        aiCoachRequests = 0,
        streakDays = 0
    ),
    val dailyGoal: DailyGoalProgress = DailyGoalProgress(date = "")
) {
    val currentPhrase: Phrase?
        get() = activePhrases.getOrNull(currentPhraseIndex)
}
