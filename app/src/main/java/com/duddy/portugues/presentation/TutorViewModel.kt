package com.duddy.portugues.presentation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duddy.portugues.data.model.GuidedSessionStep
import com.duddy.portugues.data.model.LearningTechnique
import com.duddy.portugues.data.model.Lesson
import com.duddy.portugues.data.model.Phrase
import com.duddy.portugues.data.model.ReviewGrade
import com.duddy.portugues.BuildConfig
import com.duddy.portugues.audio.PcmAudioRecorder
import com.duddy.portugues.data.auth.SupabaseAuthClient
import com.duddy.portugues.data.local.DuddyDatabase
import com.duddy.portugues.data.repository.AiCoachRepository
import com.duddy.portugues.data.repository.DailyGoalRepository
import com.duddy.portugues.data.repository.FavoritePhraseRepository
import com.duddy.portugues.data.repository.LocalPhraseRepository
import com.duddy.portugues.data.repository.PhraseRepository
import com.duddy.portugues.data.repository.PhraseSearchNormalizer
import com.duddy.portugues.data.repository.ProgressRepository
import com.duddy.portugues.data.repository.PronunciationRepository
import com.duddy.portugues.data.repository.RemoteAiCoachRepository
import com.duddy.portugues.data.repository.RemotePronunciationRepository
import com.duddy.portugues.data.repository.RoomFavoritePhraseRepository
import com.duddy.portugues.data.repository.RoomProgressRepository
import com.duddy.portugues.data.repository.RoomSpacedReviewRepository
import com.duddy.portugues.data.repository.SharedPreferencesDailyGoalRepository
import com.duddy.portugues.data.repository.SpacedReviewRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TutorViewModel(
    private val progressRepository: ProgressRepository,
    private val favoritePhraseRepository: FavoritePhraseRepository,
    private val spacedReviewRepository: SpacedReviewRepository,
    private val dailyGoalRepository: DailyGoalRepository,
    private val phraseRepository: PhraseRepository = LocalPhraseRepository(),
    private val aiCoachRepository: AiCoachRepository = RemoteAiCoachRepository(),
    private val pronunciationRepository: PronunciationRepository? = null,
    private val audioRecorder: PcmAudioRecorder? = null,
) : ViewModel() {
    private val allPhrases = phraseRepository.getPhrases()
    private val libraryStats = phraseRepository.getLibraryStats()

    var uiState by mutableStateOf(
        TutorUiState(
            lessons = phraseRepository.getLessons(),
            allPhrases = allPhrases,
            activePhrases = allPhrases,
            favoritePhraseIds = favoritePhraseRepository.getFavoritePhraseIds(),
            guidedSessionSteps = dailySessionSteps(),
            dueReviewCount = duePhraseIds().size,
            phraseLibraryStats = libraryStats,
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal(),
            statusMessage = "Loaded ${libraryStats.phraseCount} local phrases. Choose a lesson to begin."
        )
    )
        private set

    fun navigateTo(screen: AppScreen) {
        uiState = uiState.copy(
            currentScreen = screen,
            guidedSessionSteps = dailySessionSteps(),
            dueReviewCount = duePhraseIds().size,
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal()
        )
    }

    fun startLesson(lesson: Lesson) {
        progressRepository.recordLessonStarted()
        dailyGoalRepository.recordPracticeMinutes()
        uiState = uiState.copy(
            currentScreen = AppScreen.Practice,
            activeLesson = lesson,
            activePhrases = phraseRepository.getPhrasesForCategory(lesson.category),
            currentPhraseIndex = 0,
            isSmartReviewSession = false,
            isGuidedSession = false,
            isAnswerRevealed = false,
            currentTechnique = LearningTechnique.RetrievalPractice,
            spokenText = "",
            aiFeedback = "",
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal(),
            statusMessage = "Lesson loaded: ${lesson.title}"
        )
    }

    fun listenToLesson(lesson: Lesson) {
        progressRepository.recordLessonStarted()
        progressRepository.recordSampleAudioPlayed()
        dailyGoalRepository.recordPracticeMinutes()
        uiState = uiState.copy(
            currentScreen = AppScreen.Practice,
            activeLesson = lesson,
            activePhrases = phraseRepository.getPhrasesForCategory(lesson.category),
            currentPhraseIndex = 0,
            isSmartReviewSession = false,
            isGuidedSession = false,
            isAnswerRevealed = true,
            currentTechnique = LearningTechnique.Feedback,
            spokenText = "",
            pronunciationResult = null,
            pronunciationError = null,
            aiFeedback = "",
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal(),
            statusMessage = "Listening to ${lesson.title}: English, then Portuguese."
        )
    }

    fun startPhrase(phrase: Phrase) {
        dailyGoalRepository.recordPracticeMinutes()
        uiState = uiState.copy(
            currentScreen = AppScreen.Practice,
            activeLesson = null,
            activePhrases = listOf(phrase),
            currentPhraseIndex = 0,
            isSmartReviewSession = false,
            isGuidedSession = false,
            isAnswerRevealed = false,
            currentTechnique = LearningTechnique.RetrievalPractice,
            spokenText = "",
            aiFeedback = "",
            dailyGoal = dailyGoal(),
            statusMessage = "Phrase loaded. Recall first, then reveal."
        )
    }

    fun practiceAllPhrases() {
        dailyGoalRepository.recordPracticeMinutes()
        uiState = uiState.copy(
            currentScreen = AppScreen.Practice,
            activeLesson = null,
            activePhrases = allPhrases,
            currentPhraseIndex = 0,
            isSmartReviewSession = false,
            isGuidedSession = false,
            isAnswerRevealed = false,
            currentTechnique = LearningTechnique.RetrievalPractice,
            spokenText = "",
            aiFeedback = "",
            dailyGoal = dailyGoal(),
            statusMessage = "Practicing all phrases."
        )
    }

    fun startGuidedSession() {
        dailyGoalRepository.recordPracticeMinutes()
        val adaptiveIds = spacedReviewRepository.getAdaptivePhraseIds(
            allPhraseIds = allPhrases.map { phrase -> phrase.id },
            favoritePhraseIds = uiState.favoritePhraseIds,
            limit = GUIDED_SESSION_LIMIT
        )
        val adaptivePhrases = adaptiveIds.mapNotNull { phraseId ->
            allPhrases.firstOrNull { phrase -> phrase.id == phraseId }
        }
        val sessionPhrases = interleaveByCategory(adaptivePhrases)

        if (sessionPhrases.isEmpty()) {
            uiState = uiState.copy(statusMessage = "No phrases are available for a guided session.")
            return
        }

        uiState = uiState.copy(
            currentScreen = AppScreen.Practice,
            activeLesson = null,
            activePhrases = sessionPhrases,
            currentPhraseIndex = 0,
            isSmartReviewSession = true,
            isGuidedSession = true,
            isAnswerRevealed = false,
            currentTechnique = LearningTechnique.RetrievalPractice,
            spokenText = "",
            aiFeedback = "",
            guidedSessionSteps = dailySessionSteps(),
            dueReviewCount = duePhraseIds().size,
            dailyGoal = dailyGoal(),
            statusMessage = "Guided session started. Recall, reveal, speak, coach, grade."
        )
    }

    fun startSmartReview() {
        dailyGoalRepository.recordPracticeMinutes()
        val dueIds = duePhraseIds(limit = SMART_REVIEW_LIMIT)
        val duePhrases = dueIds.mapNotNull { dueId -> allPhrases.firstOrNull { phrase -> phrase.id == dueId } }
        if (duePhrases.isEmpty()) {
            uiState = uiState.copy(statusMessage = "No due reviews right now. Try all phrases or save favorites.")
            return
        }

        uiState = uiState.copy(
            currentScreen = AppScreen.Practice,
            activeLesson = null,
            activePhrases = interleaveByCategory(duePhrases),
            currentPhraseIndex = 0,
            isSmartReviewSession = true,
            isGuidedSession = false,
            isAnswerRevealed = false,
            currentTechnique = LearningTechnique.RetrievalPractice,
            spokenText = "",
            aiFeedback = "",
            dueReviewCount = duePhraseIds().size,
            dailyGoal = dailyGoal(),
            statusMessage = "Smart review started. Recall first, then reveal."
        )
    }

    fun practiceFavoritePhrases() {
        dailyGoalRepository.recordPracticeMinutes()
        val favoritePhrases = allPhrases.filter { phrase -> phrase.id in uiState.favoritePhraseIds }
        if (favoritePhrases.isEmpty()) {
            uiState = uiState.copy(statusMessage = "Save a phrase first, then practice favorites.")
            return
        }

        uiState = uiState.copy(
            currentScreen = AppScreen.Practice,
            activeLesson = null,
            activePhrases = favoritePhrases,
            currentPhraseIndex = 0,
            isSmartReviewSession = false,
            isGuidedSession = false,
            isAnswerRevealed = false,
            currentTechnique = LearningTechnique.RetrievalPractice,
            spokenText = "",
            aiFeedback = "",
            dailyGoal = dailyGoal(),
            statusMessage = "Practicing saved phrases."
        )
    }

    fun nextPhrase() {
        val phraseCount = uiState.activePhrases.size
        if (phraseCount == 0) return

        progressRepository.recordPhrasePracticed()
        dailyGoalRepository.recordPracticeMinutes()
        uiState = uiState.copy(
            currentPhraseIndex = (uiState.currentPhraseIndex + 1) % phraseCount,
            isAnswerRevealed = false,
            currentTechnique = LearningTechnique.RetrievalPractice,
            spokenText = "",
            aiFeedback = "",
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal(),
            statusMessage = "Next phrase ready."
        )
    }

    fun showPhraseAt(index: Int) {
        if (index !in uiState.activePhrases.indices) return
        uiState = uiState.copy(
            currentPhraseIndex = index,
            isAnswerRevealed = true,
            spokenText = "",
            pronunciationResult = null,
            pronunciationError = null
        )
    }

    fun revealCurrentPhrase() {
        uiState = uiState.copy(
            isAnswerRevealed = true,
            currentTechnique = LearningTechnique.ProductionPractice,
            statusMessage = "Answer revealed. Grade your recall honestly."
        )
    }

    fun gradeCurrentPhrase(grade: ReviewGrade) {
        val phrase = uiState.currentPhrase ?: return
        val currentStats = progressRepository.getStats()
        if (grade == ReviewGrade.Again && !currentStats.hasHearts) {
            uiState = uiState.copy(
                stats = currentStats,
                statusMessage = "No hearts left. Review easy phrases or wait for hearts to refill."
            )
            return
        }
        val isNewPhrase = spacedReviewRepository.getReviewState(phrase.id) == null
        progressRepository.recordPhrasePracticed()
        if (grade == ReviewGrade.Again) {
            progressRepository.recordMistake()
        }
        if (isNewPhrase) {
            dailyGoalRepository.recordNewPhrase()
        } else {
            dailyGoalRepository.recordReview()
        }
        dailyGoalRepository.recordPracticeMinutes()
        val reviewState = spacedReviewRepository.recordReview(phrase.id, grade)
        val phraseCount = uiState.activePhrases.size
        val nextIndex = if (phraseCount == 0) 0 else (uiState.currentPhraseIndex + 1) % phraseCount
        uiState = uiState.copy(
            currentPhraseIndex = nextIndex,
            isAnswerRevealed = false,
            currentTechnique = LearningTechnique.RetrievalPractice,
            spokenText = "",
            aiFeedback = "",
            dueReviewCount = duePhraseIds().size,
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal(),
            statusMessage = "Review scheduled for ${reviewState.dueDate}."
        )
    }

    fun playSampleAudio() {
        val phrase = uiState.currentPhrase ?: return
        progressRepository.recordSampleAudioPlayed()
        dailyGoalRepository.recordPracticeMinutes()
        uiState = uiState.copy(
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal(),
            statusMessage = "Playing sample audio for '${phrase.portuguese}'."
        )
    }

    fun recordUser() {
        uiState = uiState.copy(
            currentTechnique = LearningTechnique.ProductionPractice,
            statusMessage = "Listening for Portuguese speech..."
        )
    }

    fun updateAiServerUrl(value: String) {
        uiState = uiState.copy(aiServerUrl = value)
    }

    fun updateSpokenText(value: String) {
        uiState = uiState.copy(
            spokenText = value,
            statusMessage = if (value.isBlank()) {
                "No speech captured yet."
            } else {
                "Captured speech transcript."
            }
        )
    }

    fun updatePhraseSearchQuery(value: String) {
        val normalizedQuery = normalize(value)
        val results = if (normalizedQuery.isBlank()) {
            emptyList()
        } else {
            allPhrases.filter { phrase ->
                listOf(
                    phrase.portuguese,
                    phrase.english,
                    phrase.pronunciationGuide,
                    phrase.category.displayName
                ).any { field -> normalize(field).contains(normalizedQuery) }
            }
        }

        uiState = uiState.copy(
            phraseSearchQuery = value,
            phraseSearchResults = results,
            statusMessage = if (results.isEmpty() && value.isNotBlank()) {
                "No phrase matches found."
            } else {
                uiState.statusMessage
            }
        )
    }

    fun toggleFavoritePhrase(phrase: Phrase) {
        val favorites = favoritePhraseRepository.toggleFavoritePhrase(phrase.id)
        uiState = uiState.copy(
            favoritePhraseIds = favorites,
            statusMessage = if (phrase.id in favorites) {
                "Phrase saved."
            } else {
                "Phrase removed from saved."
            }
        )
    }

    fun toggleCurrentPhraseFavorite() {
        val phrase = uiState.currentPhrase ?: return
        toggleFavoritePhrase(phrase)
    }

    fun recordSpeechTranscript(value: String) {
        if (value.isNotBlank()) {
            progressRepository.recordSpeakingAttempt()
            dailyGoalRepository.recordSpeakingAttempt()
            dailyGoalRepository.recordPracticeMinutes()
        }
        uiState = uiState.copy(
            spokenText = value,
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal(),
            statusMessage = if (value.isBlank()) {
                "No speech captured yet."
            } else {
                "Captured speech transcript."
            }
        )
    }

    fun speechRecognitionUnavailable() {
        uiState = uiState.copy(
            statusMessage = "Speech recognition is not available on this device."
        )
    }

    fun requestAiFeedback() {
        val phrase = uiState.currentPhrase ?: return
        val endpointUrl = uiState.aiServerUrl.trim()
        val spokenText = uiState.spokenText.trim()

        if (endpointUrl.isBlank()) {
            uiState = uiState.copy(statusMessage = "Enter an AI server URL before asking the coach.")
            return
        }

        if (spokenText.isBlank()) {
            uiState = uiState.copy(statusMessage = "Record or type what you said before asking the AI coach.")
            return
        }

        progressRepository.recordAiCoachRequest()
        dailyGoalRepository.recordAiCoachRequest()
        dailyGoalRepository.recordPracticeMinutes()
        uiState = uiState.copy(
            isAiLoading = true,
            aiFeedback = "",
            currentTechnique = LearningTechnique.Feedback,
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal(),
            statusMessage = "Asking AI coach..."
        )

        viewModelScope.launch {
            uiState = try {
                val pronResult = uiState.pronunciationResult
                val pronScore  = pronResult?.overall?.pronunciation
                val phonemes   = pronResult?.phonemesLow ?: emptyList()
                val feedback = if (aiCoachRepository is RemoteAiCoachRepository) {
                    aiCoachRepository.getFeedback(
                        endpointUrl        = endpointUrl,
                        phrase             = phrase,
                        spokenText         = spokenText,
                        pronunciationScore = pronScore,
                        phonemeErrors      = phonemes,
                    )
                } else {
                    aiCoachRepository.getFeedback(
                        endpointUrl = endpointUrl,
                        phrase      = phrase,
                        spokenText  = spokenText,
                    )
                }
                uiState.copy(
                    isAiLoading = false,
                    aiFeedback = feedback.message,
                    dueReviewCount = feedback.score?.let { score ->
                        spacedReviewRepository.recordPronunciationScore(phrase.id, score)
                        duePhraseIds().size
                    } ?: uiState.dueReviewCount,
                    stats = progressRepository.getStats(),
                    dailyGoal = dailyGoal(),
                    statusMessage = feedback.score?.let { score ->
                        "AI coach feedback ready. Score: $score/100."
                    } ?: "AI coach feedback ready."
                )
            } catch (exception: Exception) {
                uiState.copy(
                    isAiLoading = false,
                    aiFeedback = "",
                    stats = progressRepository.getStats(),
                    dailyGoal = dailyGoal(),
                    statusMessage = exception.message ?: "AI coach request failed."
                )
            }
        }
    }

    fun clearAiFeedback() {
        uiState = uiState.copy(
            aiFeedback = "",
            statusMessage = "AI feedback cleared."
        )
    }

    // ── Pronunciation Assessment (Azure via backend) ───────────────────────

    /** Start recording the learner's spoken attempt. */
    fun startRecording() {
        val recorder = audioRecorder
        if (recorder == null) {
            uiState = uiState.copy(statusMessage = "Microphone recording is not configured.")
            return
        }
        if (!recorder.hasPermission()) {
            uiState = uiState.copy(
                statusMessage = "Grant microphone permission to record.",
                pronunciationError = "Microphone permission required."
            )
            return
        }
        runCatching { recorder.start() }
            .onSuccess {
                uiState = uiState.copy(
                    isRecording = true,
                    pronunciationResult = null,
                    pronunciationError = null,
                    statusMessage = "Recording — speak the phrase clearly."
                )
            }
            .onFailure {
                uiState = uiState.copy(
                    pronunciationError = it.message ?: "Couldn't start microphone.",
                    statusMessage = "Microphone error."
                )
            }
    }

    /** Stop recording and submit to Azure for assessment. */
    fun stopRecordingAndAssess() {
        val recorder = audioRecorder ?: return
        val pronRepo = pronunciationRepository
        val phrase   = uiState.currentPhrase

        if (pronRepo == null || phrase == null) {
            recorder.cancel()
            uiState = uiState.copy(
                isRecording = false,
                statusMessage = if (phrase == null) "No phrase selected." else "Pronunciation service not configured."
            )
            return
        }

        val recording = recorder.stop()
        if (recording.wavBytes.isEmpty()) {
            uiState = uiState.copy(isRecording = false, pronunciationError = "No audio captured.")
            return
        }

        progressRepository.recordSpeakingAttempt()
        dailyGoalRepository.recordSpeakingAttempt()
        dailyGoalRepository.recordPracticeMinutes()
        uiState = uiState.copy(
            isRecording = false,
            isAssessing = true,
            pronunciationError = null,
            statusMessage = "Scoring your pronunciation…",
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal(),
        )

        viewModelScope.launch {
            uiState = try {
                val result = pronRepo.assess(
                    wavBytes        = recording.wavBytes,
                    referenceText   = phrase.portuguese,
                    locale          = "pt-BR",
                    durationSeconds = recording.durationSeconds,
                )
                result.overall.pronunciation?.roundToInt()?.let { score ->
                    spacedReviewRepository.recordPronunciationScore(phrase.id, score)
                }
                progressRepository.recordPronunciationAssessed()
                uiState.copy(
                    isAssessing = false,
                    pronunciationResult = result,
                    spokenText = result.recognizedText,
                    guidedSessionSteps = dailySessionSteps(),
                    dueReviewCount = duePhraseIds().size,
                    dailyGoal = dailyGoal(),
                    statusMessage = result.overall.pronunciation?.let {
                        "Score: ${"%.0f".format(it)} / 100"
                    } ?: "Assessment complete."
                )
            } catch (e: Exception) {
                uiState.copy(
                    isAssessing = false,
                    pronunciationError = formatPronunciationError(e),
                    dailyGoal = dailyGoal(),
                    statusMessage = "Couldn't score that attempt."
                )
            }
        }
    }

    /** Cancel an in-progress recording without sending. */
    fun cancelRecording() {
        audioRecorder?.cancel()
        uiState = uiState.copy(isRecording = false, statusMessage = "Recording cancelled.")
    }

    fun clearPronunciationResult() {
        uiState = uiState.copy(pronunciationResult = null, pronunciationError = null)
    }

    private fun normalize(value: String): String =
        PhraseSearchNormalizer.normalize(value)

    private fun formatPronunciationError(error: Exception): String {
        val rawMessage = error.message.orEmpty()
        val backendUrl = BuildConfig.DUDDY_BACKEND_URL
        val isLocalDevBackend = backendUrl.contains("127.0.0.1") || backendUrl.contains("10.0.2.2")
        val isConnectionFailure = rawMessage.contains("failed to connect", ignoreCase = true) ||
            rawMessage.contains("connection refused", ignoreCase = true)

        if (isLocalDevBackend && isConnectionFailure) {
            return "Could not reach the local pronunciation backend at $backendUrl. For a USB-connected phone, keep the phone plugged in and run tools\\repair_phone_backend_tunnel.ps1 or adb reverse tcp:8010 tcp:8010, then try again."
        }

        return rawMessage.ifBlank { "Pronunciation assessment failed." }
    }

    private fun duePhraseIds(limit: Int = Int.MAX_VALUE): List<String> =
        spacedReviewRepository.getDuePhraseIds(
            allPhraseIds = allPhrases.map { phrase -> phrase.id },
            limit = limit
        )

    private fun dailyGoal() =
        dailyGoalRepository.getTodayProgress(
            streakDays = progressRepository.getStats().streakDays
        )

    private fun interleaveByCategory(phrases: List<Phrase>): List<Phrase> {
        val queues = phrases
            .groupBy { phrase -> phrase.category }
            .values
            .map { groupedPhrases -> groupedPhrases.toMutableList() }
            .toMutableList()

        val mixed = mutableListOf<Phrase>()
        while (queues.any { queue -> queue.isNotEmpty() }) {
            for (queue in queues) {
                if (queue.isNotEmpty()) {
                    mixed += queue.removeAt(0)
                }
            }
        }
        return mixed
    }

    private fun dailySessionSteps(): List<GuidedSessionStep> =
        listOf(
            GuidedSessionStep(
                title = "Recall",
                description = "Start from the English meaning and retrieve the Portuguese.",
                technique = LearningTechnique.RetrievalPractice
            ),
            GuidedSessionStep(
                title = "Reveal",
                description = "Check the answer only after you try.",
                technique = LearningTechnique.SpacedRepetition
            ),
            GuidedSessionStep(
                title = "Speak",
                description = "Say the phrase aloud and capture the transcript.",
                technique = LearningTechnique.ProductionPractice
            ),
            GuidedSessionStep(
                title = "Coach",
                description = "Use AI feedback for one correction and one next attempt.",
                technique = LearningTechnique.Feedback
            ),
            GuidedSessionStep(
                title = "Grade",
                description = "Choose Again, Hard, Good, or Easy to schedule review.",
                technique = LearningTechnique.SpacedRepetition
            )
        )

    companion object {
        private const val SMART_REVIEW_LIMIT = 20
        private const val GUIDED_SESSION_LIMIT = 20

        fun factory(
            context: Context,
            authClient: SupabaseAuthClient? = null,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = DuddyDatabase.getInstance(context)

                    // ── Token provider for authenticated backend calls ──
                    val tokenProvider: suspend () -> String? = {
                        authClient?.currentAccessToken()
                    }

                    val aiCoach: AiCoachRepository =
                        RemoteAiCoachRepository(authTokenProvider = tokenProvider)

                    val pronRepo: PronunciationRepository =
                        RemotePronunciationRepository(
                            endpointUrl = "${BuildConfig.DUDDY_BACKEND_URL}/v1/pronunciation",
                            authTokenProvider = tokenProvider,
                        )

                    val recorder = PcmAudioRecorder(context.applicationContext)

                    return TutorViewModel(
                        progressRepository       = RoomProgressRepository(db.progressDao()),
                        favoritePhraseRepository = RoomFavoritePhraseRepository(db.favoriteDao()),
                        spacedReviewRepository   = RoomSpacedReviewRepository(db.reviewDao()),
                        dailyGoalRepository      = SharedPreferencesDailyGoalRepository(context),
                        phraseRepository         = LocalPhraseRepository(context.applicationContext),
                        aiCoachRepository        = aiCoach,
                        pronunciationRepository  = pronRepo,
                        audioRecorder            = recorder,
                    ) as T
                }
            }
    }
}
