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
import com.duddy.portugues.data.model.PhraseDifficulty
import com.duddy.portugues.data.model.ReviewGrade
import com.duddy.portugues.data.model.TestingAnswerResult
import com.duddy.portugues.data.model.TestingLevel
import com.duddy.portugues.data.model.TestingQuestion
import com.duddy.portugues.data.model.TestingQuestionType
import com.duddy.portugues.data.model.TestingUiState
import com.duddy.portugues.BuildConfig
import com.duddy.portugues.audio.PcmAudioRecorder
import com.duddy.portugues.data.auth.SupabaseAuthClient
import com.duddy.portugues.data.local.DuddyDatabase
import com.duddy.portugues.data.preferences.OnboardingPreferences
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
    private val startingDifficulty: PhraseDifficulty = PhraseDifficulty.A1,
) : ViewModel() {
    private val allPhrases = phraseRepository.getPhrases()
    private val recommendedPhrases =
        allPhrases.filter { phrase -> phrase.difficulty.ordinal <= startingDifficulty.ordinal }
            .ifEmpty { allPhrases }
    private val libraryStats = phraseRepository.getLibraryStats()

    var uiState by mutableStateOf(
        TutorUiState(
            lessons = phraseRepository.getLessons(),
            allPhrases = allPhrases,
            activePhrases = recommendedPhrases,
            favoritePhraseIds = favoritePhraseRepository.getFavoritePhraseIds(),
            guidedSessionSteps = dailySessionSteps(),
            dueReviewCount = duePhraseIds().size,
            phraseLibraryStats = libraryStats,
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal(),
            statusMessage = "Loaded ${libraryStats.phraseCount} local phrases. Starting level: ${startingDifficulty.name}."
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
            allPhraseIds = recommendedPhrases.map { phrase -> phrase.id },
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

    fun startTestingLevel(level: TestingLevel) {
        val questions = buildTestingQuestions(level)
        if (questions.isEmpty()) {
            uiState = uiState.copy(statusMessage = "No phrases are available for this test yet.")
            return
        }

        dailyGoalRepository.recordPracticeMinutes()
        val testingPhrases = questions.map { question -> question.phrase }
        uiState = uiState.copy(
            currentScreen = AppScreen.Testing,
            activeLesson = null,
            activePhrases = if (level == TestingLevel.LevelIII) testingPhrases else uiState.activePhrases,
            currentPhraseIndex = 0,
            isSmartReviewSession = false,
            isGuidedSession = false,
            isAnswerRevealed = level == TestingLevel.LevelIII,
            currentTechnique = if (level == TestingLevel.LevelIII) {
                LearningTechnique.ProductionPractice
            } else {
                LearningTechnique.RetrievalPractice
            },
            spokenText = "",
            aiFeedback = "",
            pronunciationResult = null,
            pronunciationError = null,
            testing = TestingUiState(
                selectedLevel = level,
                questions = questions,
            ),
            dailyGoal = dailyGoal(),
            statusMessage = "${level.title} started. ${level.subtitle}"
        )
    }

    fun selectTestingChoice(choice: String) {
        val question = uiState.testing.currentQuestion ?: return
        if (question.type != TestingQuestionType.MultipleChoice || uiState.testing.isCurrentAnswered) return
        val isCorrect = choice == question.expectedAnswer
        if (!isCorrect && question.mustRetryUntilCorrect) {
            val wrongChoiceFeedback = wrongChoiceFeedbackFor(question, choice)
            uiState = uiState.copy(
                testing = uiState.testing.copy(
                    selectedChoice = choice,
                    lastResult = TestingAnswerResult(
                        isCorrect = false,
                        expectedAnswer = question.expectedAnswer,
                        message = "Try again",
                        spokenFeedback = choice,
                        spokenFeedbackLocaleTag = wrongChoiceFeedback.choiceLocaleTag,
                        spokenFeedbackTranslation = wrongChoiceFeedback.translation,
                        spokenFeedbackTranslationLocaleTag = wrongChoiceFeedback.translationLocaleTag,
                    ),
                ),
                statusMessage = "Try again",
            )
            return
        }

        recordTestingAnswer(
            isCorrect = isCorrect,
            expectedAnswer = question.expectedAnswer,
            selectedChoice = choice,
            message = if (isCorrect) {
                "Correct."
            } else {
                "Not quite. Correct answer: ${question.expectedAnswer}"
            }
        )
    }

    fun updateTestingFillBlankAnswer(value: String) {
        uiState = uiState.copy(
            testing = uiState.testing.copy(fillBlankAnswer = value)
        )
    }

    fun submitTestingFillBlankAnswer() {
        val question = uiState.testing.currentQuestion ?: return
        if (question.type != TestingQuestionType.FillBlank || uiState.testing.isCurrentAnswered) return
        val learnerAnswer = uiState.testing.fillBlankAnswer
        val isCorrect = normalizeForAnswer(learnerAnswer) == normalizeForAnswer(question.expectedAnswer)
        recordTestingAnswer(
            isCorrect = isCorrect,
            expectedAnswer = question.expectedAnswer,
            message = if (isCorrect) {
                "Correct. You produced the missing word."
            } else {
                "Review this one. Missing word: ${question.expectedAnswer}"
            }
        )
    }

    fun nextTestingQuestion() {
        val testing = uiState.testing
        if (testing.questions.isEmpty()) return

        if (testing.currentIndex >= testing.questions.lastIndex) {
            uiState = uiState.copy(
                testing = testing.copy(
                    completed = true,
                    selectedChoice = null,
                    fillBlankAnswer = "",
                    lastResult = null,
                ),
                pronunciationResult = null,
                pronunciationError = null,
                statusMessage = "Test complete. Score: ${testing.correctCount}/${testing.totalCount}."
            )
            return
        }

        val nextIndex = testing.currentIndex + 1
        uiState = uiState.copy(
            currentPhraseIndex = if (testing.selectedLevel == TestingLevel.LevelIII) nextIndex else uiState.currentPhraseIndex,
            spokenText = "",
            pronunciationResult = null,
            pronunciationError = null,
            testing = testing.copy(
                currentIndex = nextIndex,
                selectedChoice = null,
                fillBlankAnswer = "",
                lastResult = null,
            ),
            statusMessage = "Next test question ready."
        )
    }

    fun restartTestingLevel() {
        uiState.testing.selectedLevel?.let { level -> startTestingLevel(level) }
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
                    statusMessage = UserFacingErrors.forAiCoach(exception)
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
                statusMessage = UserFacingErrors.MICROPHONE_PERMISSION_REQUIRED,
                pronunciationError = UserFacingErrors.MICROPHONE_PERMISSION_REQUIRED
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
                val message = UserFacingErrors.forMicrophoneStart(it)
                uiState = uiState.copy(
                    pronunciationError = message,
                    statusMessage = message
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
                val assessedState = uiState.copy(
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
                if (assessedState.currentScreen == AppScreen.Testing) {
                    assessedState.withPronunciationTestingResult(result.overall.pronunciation?.roundToInt())
                } else {
                    assessedState
                }
            } catch (e: Exception) {
                val message = formatPronunciationError(e)
                uiState.copy(
                    isAssessing = false,
                    pronunciationError = message,
                    dailyGoal = dailyGoal(),
                    statusMessage = message
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

    private fun normalizeForAnswer(value: String): String =
        normalize(value)
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun formatPronunciationError(error: Exception): String {
        return UserFacingErrors.forPronunciation(error)
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

    private fun buildTestingQuestions(level: TestingLevel): List<TestingQuestion> {
        if (level == TestingLevel.LevelI) {
            return buildLevelOneTranslationQuestions()
        }
        if (level == TestingLevel.LevelII) {
            return buildLevelTwoListeningQuestions()
        }

        val eligible = when (level) {
            TestingLevel.LevelI -> allPhrases
            TestingLevel.LevelII -> allPhrases.filter { phrase ->
                phrase.difficulty.ordinal <= PhraseDifficulty.B1.ordinal
            }
            TestingLevel.LevelIII -> allPhrases.filter { phrase -> phrase.speakingPractice }
        }.ifEmpty { allPhrases }

        val limit = when (level) {
            TestingLevel.LevelI -> allPhrases.size
            TestingLevel.LevelII -> LEVEL_TWO_QUESTION_LIMIT
            TestingLevel.LevelIII -> LEVEL_THREE_QUESTION_LIMIT
        }

        return interleaveByCategory(eligible)
            .distinctBy { phrase -> phrase.id }
            .take(limit)
            .mapIndexed { index, phrase ->
                when (level) {
                    TestingLevel.LevelI -> multipleChoiceQuestion(level, phrase)
                    TestingLevel.LevelII -> if (index % 2 == 0) {
                        multipleChoiceQuestion(level, phrase)
                    } else {
                        fillBlankQuestion(level, phrase)
                    }
                    TestingLevel.LevelIII -> TestingQuestion(
                        id = "${level.name}-${phrase.id}",
                        level = level,
                        type = TestingQuestionType.Pronunciation,
                        phrase = phrase,
                        sectionTitle = "Pronunciation exam",
                        prompt = "Say this phrase clearly in Brazilian Portuguese.",
                        expectedAnswer = phrase.portuguese,
                    )
                }
            }
    }

    private fun buildLevelOneTranslationQuestions(): List<TestingQuestion> {
        val phrases = allPhrases.distinctBy { phrase -> phrase.id }
        val englishToPortuguese = phrases.shuffled().map { phrase ->
            multipleChoiceQuestion(
                level = TestingLevel.LevelI,
                phrase = phrase,
                sectionTitle = "Section 1: English to Portuguese",
                prompt = "Choose the Portuguese translation for:\n${phrase.english}",
                expectedAnswer = phrase.portuguese,
                choiceText = { candidate -> candidate.portuguese },
                idSuffix = "en-pt",
            )
        }
        val portugueseToEnglish = phrases.shuffled().map { phrase ->
            multipleChoiceQuestion(
                level = TestingLevel.LevelI,
                phrase = phrase,
                sectionTitle = "Section 2: Portuguese to English",
                prompt = "Choose the English translation for:\n${phrase.portuguese}",
                expectedAnswer = phrase.english,
                choiceText = { candidate -> candidate.english },
                idSuffix = "pt-en",
            )
        }

        return englishToPortuguese + portugueseToEnglish
    }

    private fun buildLevelTwoListeningQuestions(): List<TestingQuestion> =
        allPhrases
            .distinctBy { phrase -> phrase.id }
            .shuffled()
            .map { phrase ->
                if (listOf(true, false).random()) {
                    multipleChoiceQuestion(
                        level = TestingLevel.LevelII,
                        phrase = phrase,
                        sectionTitle = "Listening: English to Portuguese",
                        prompt = "Listen to the English phrase, then choose the Portuguese translation.",
                        expectedAnswer = phrase.portuguese,
                        choiceText = { candidate -> candidate.portuguese },
                        idSuffix = "listen-en-pt",
                        spokenPrompt = phrase.english,
                        spokenPromptLocaleTag = ENGLISH_TTS_LOCALE,
                        spokenAnswer = phrase.portuguese,
                        spokenAnswerLocaleTag = PORTUGUESE_TTS_LOCALE,
                        mustRetryUntilCorrect = true,
                    )
                } else {
                    multipleChoiceQuestion(
                        level = TestingLevel.LevelII,
                        phrase = phrase,
                        sectionTitle = "Listening: Portuguese to English",
                        prompt = "Listen to the Portuguese phrase, then choose the English translation.",
                        expectedAnswer = phrase.english,
                        choiceText = { candidate -> candidate.english },
                        idSuffix = "listen-pt-en",
                        spokenPrompt = phrase.portuguese,
                        spokenPromptLocaleTag = PORTUGUESE_TTS_LOCALE,
                        spokenAnswer = phrase.english,
                        spokenAnswerLocaleTag = ENGLISH_TTS_LOCALE,
                        mustRetryUntilCorrect = true,
                    )
                }
            }

    private fun wrongChoiceFeedbackFor(
        question: TestingQuestion,
        selectedChoice: String,
    ): WrongChoiceFeedback {
        val choiceIsPortuguese = question.spokenAnswerLocaleTag == PORTUGUESE_TTS_LOCALE
        val selectedPhrase = if (choiceIsPortuguese) {
            allPhrases.firstOrNull { phrase -> phrase.portuguese == selectedChoice }
        } else {
            allPhrases.firstOrNull { phrase -> phrase.english == selectedChoice }
        }

        return WrongChoiceFeedback(
            choiceLocaleTag = question.spokenAnswerLocaleTag,
            translation = if (choiceIsPortuguese) {
                selectedPhrase?.english.orEmpty()
            } else {
                selectedPhrase?.portuguese.orEmpty()
            },
            translationLocaleTag = question.spokenPromptLocaleTag,
        )
    }

    private data class WrongChoiceFeedback(
        val choiceLocaleTag: String,
        val translation: String,
        val translationLocaleTag: String,
    )

    private fun multipleChoiceQuestion(
        level: TestingLevel,
        phrase: Phrase,
        sectionTitle: String = "Multiple choice",
        prompt: String = "Choose the Portuguese translation for:\n${phrase.english}",
        expectedAnswer: String = phrase.portuguese,
        choiceText: (Phrase) -> String = { candidate -> candidate.portuguese },
        idSuffix: String = "mc",
        spokenPrompt: String = "",
        spokenPromptLocaleTag: String = "",
        spokenAnswer: String = expectedAnswer,
        spokenAnswerLocaleTag: String = "",
        mustRetryUntilCorrect: Boolean = false,
    ): TestingQuestion {
        val distractors = allPhrases
            .asSequence()
            .filter { candidate -> candidate.id != phrase.id }
            .filter { candidate -> candidate.category == phrase.category }
            .map(choiceText)
            .filter { option -> option != expectedAnswer }
            .distinct()
            .toList()
            .shuffled()
            .take(3)
            .toMutableList()

        if (distractors.size < 3) {
            allPhrases
                .asSequence()
                .filter { candidate -> candidate.id != phrase.id }
                .map(choiceText)
                .filter { option -> option != expectedAnswer && option !in distractors }
                .distinct()
                .toList()
                .shuffled()
                .take(3 - distractors.size)
                .forEach { option -> distractors += option }
        }

        val choices = distractors.toMutableList()
        choices += expectedAnswer

        return TestingQuestion(
            id = "${level.name}-${phrase.id}-$idSuffix",
            level = level,
            type = TestingQuestionType.MultipleChoice,
            phrase = phrase,
            sectionTitle = sectionTitle,
            prompt = prompt,
            choices = choices.shuffled(),
            expectedAnswer = expectedAnswer,
            spokenPrompt = spokenPrompt,
            spokenPromptLocaleTag = spokenPromptLocaleTag,
            spokenAnswer = spokenAnswer,
            spokenAnswerLocaleTag = spokenAnswerLocaleTag,
            mustRetryUntilCorrect = mustRetryUntilCorrect,
        )
    }

    private fun fillBlankQuestion(
        level: TestingLevel,
        phrase: Phrase,
    ): TestingQuestion {
        val (blankPrompt, expectedWord) = blankPromptFor(phrase.portuguese)
        return TestingQuestion(
            id = "${level.name}-${phrase.id}-blank",
            level = level,
            type = TestingQuestionType.FillBlank,
            phrase = phrase,
            sectionTitle = "Fill in the blank",
            prompt = "Fill in the missing Portuguese word for: ${phrase.english}",
            blankPrompt = blankPrompt,
            expectedAnswer = expectedWord,
        )
    }

    private fun blankPromptFor(portuguese: String): Pair<String, String> {
        val words = portuguese.split(" ")
        val blankIndex = words.indexOfFirst { word ->
            normalizeForAnswer(word).length >= 4
        }.takeIf { index -> index >= 0 } ?: words.lastIndex.coerceAtLeast(0)
        val expected = words.getOrNull(blankIndex).orEmpty()
        val prompt = words.mapIndexed { index, word ->
            if (index == blankIndex) "____" else word
        }.joinToString(" ")
        return prompt to expected
    }

    private fun recordTestingAnswer(
        isCorrect: Boolean,
        expectedAnswer: String,
        message: String,
        selectedChoice: String? = uiState.testing.selectedChoice,
    ) {
        val testing = uiState.testing
        val question = testing.currentQuestion ?: return
        val alreadyAnswered = question.id in testing.answeredQuestionIds
        val result = TestingAnswerResult(
            isCorrect = isCorrect,
            expectedAnswer = expectedAnswer,
            message = message,
        )

        if (!alreadyAnswered) {
            progressRepository.recordPhrasePracticed()
            dailyGoalRepository.recordPracticeMinutes()
        }

        uiState = uiState.copy(
            testing = testing.copy(
                selectedChoice = selectedChoice,
                answeredQuestionIds = if (alreadyAnswered) {
                    testing.answeredQuestionIds
                } else {
                    testing.answeredQuestionIds + question.id
                },
                correctCount = if (!alreadyAnswered && isCorrect) {
                    testing.correctCount + 1
                } else {
                    testing.correctCount
                },
                lastResult = result,
            ),
            stats = progressRepository.getStats(),
            dailyGoal = dailyGoal(),
            statusMessage = message,
        )
    }

    private fun TutorUiState.withPronunciationTestingResult(score: Int?): TutorUiState {
        val testingState = testing
        val question = testingState.currentQuestion ?: return this
        if (
            question.type != TestingQuestionType.Pronunciation ||
            question.id in testingState.answeredQuestionIds ||
            score == null
        ) {
            return this
        }

        val passed = score >= PRONUNCIATION_PASSING_SCORE
        val result = TestingAnswerResult(
            isCorrect = passed,
            expectedAnswer = question.expectedAnswer,
            message = if (passed) {
                "Pronunciation exam passed: $score/100."
            } else {
                "Pronunciation needs another pass: $score/100. Aim for $PRONUNCIATION_PASSING_SCORE+."
            },
            pronunciationScore = score,
        )

        return copy(
            testing = testingState.copy(
                answeredQuestionIds = testingState.answeredQuestionIds + question.id,
                correctCount = if (passed) testingState.correctCount + 1 else testingState.correctCount,
                lastResult = result,
                pronunciationScores = testingState.pronunciationScores + (question.id to score),
            ),
            statusMessage = result.message,
        )
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
        private const val LEVEL_TWO_QUESTION_LIMIT = 12
        private const val LEVEL_THREE_QUESTION_LIMIT = 8
        private const val PRONUNCIATION_PASSING_SCORE = 75
        private const val ENGLISH_TTS_LOCALE = "en-US"
        private const val PORTUGUESE_TTS_LOCALE = "pt-BR"

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
                        startingDifficulty       = OnboardingPreferences.placementLevel(context),
                    ) as T
                }
            }
    }
}
