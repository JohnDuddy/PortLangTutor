package com.duddy.portugues

import com.duddy.portugues.data.model.AiCoachFeedback
import com.duddy.portugues.data.model.DailyGoalProgress
import com.duddy.portugues.data.model.Lesson
import com.duddy.portugues.data.model.Phrase
import com.duddy.portugues.data.model.PhraseCategory
import com.duddy.portugues.data.model.PhraseReviewState
import com.duddy.portugues.data.model.ProgressStats
import com.duddy.portugues.data.model.ReviewGrade
import com.duddy.portugues.data.repository.AiCoachRepository
import com.duddy.portugues.data.repository.DailyGoalRepository
import com.duddy.portugues.data.repository.FavoritePhraseRepository
import com.duddy.portugues.data.repository.PhraseRepository
import com.duddy.portugues.data.repository.ProgressRepository
import com.duddy.portugues.data.repository.SpacedReviewRepository
import com.duddy.portugues.data.repository.SpacedReviewScheduler

internal class FakePhraseRepository(
    private val phrases: List<Phrase> = testPhrases(),
) : PhraseRepository {
    override fun getLessons(): List<Lesson> =
        phrases.groupBy { it.category }.map { (category, categoryPhrases) ->
            Lesson(
                id = category.key,
                title = category.displayName,
                description = "Practice ${category.displayName.lowercase()} phrases.",
                category = category,
                phraseCount = categoryPhrases.size,
            )
        }

    override fun getPhrases(): List<Phrase> = phrases

    override fun getPhrasesForCategory(category: PhraseCategory): List<Phrase> =
        phrases.filter { it.category == category }
}

internal class FakeProgressRepository(
    var currentStats: ProgressStats = ProgressStats(
        lessonsStarted = 0,
        practicedPhrases = 0,
        sampleAudioPlays = 0,
        speakingAttempts = 0,
        aiCoachRequests = 0,
        streakDays = 1,
        hearts = 5,
    ),
) : ProgressRepository {
    override fun getStats(): ProgressStats = currentStats

    override fun recordLessonStarted() {
        currentStats = currentStats.copy(lessonsStarted = currentStats.lessonsStarted + 1)
    }

    override fun recordPhrasePracticed() {
        currentStats = currentStats.copy(
            practicedPhrases = currentStats.practicedPhrases + 1,
            totalXp = currentStats.totalXp + 10,
            todayXp = currentStats.todayXp + 10,
            weeklyLeagueXp = currentStats.weeklyLeagueXp + 10,
        )
    }

    override fun recordSampleAudioPlayed() {
        currentStats = currentStats.copy(sampleAudioPlays = currentStats.sampleAudioPlays + 1)
    }

    override fun recordSpeakingAttempt() {
        currentStats = currentStats.copy(
            speakingAttempts = currentStats.speakingAttempts + 1,
            totalXp = currentStats.totalXp + 15,
            todayXp = currentStats.todayXp + 15,
            weeklyLeagueXp = currentStats.weeklyLeagueXp + 15,
        )
    }

    override fun recordPronunciationAssessed() = Unit

    override fun recordAiCoachRequest() {
        currentStats = currentStats.copy(aiCoachRequests = currentStats.aiCoachRequests + 1)
    }

    override fun recordMistake() {
        currentStats = currentStats.copy(hearts = (currentStats.hearts - 1).coerceAtLeast(0))
    }

    override fun refillHearts() {
        currentStats = currentStats.copy(hearts = currentStats.maxHearts)
    }

    override fun setDailyXpGoal(goalXp: Int) {
        currentStats = currentStats.copy(dailyXpGoal = goalXp)
    }
}

internal class FakeFavoritePhraseRepository(
    initialFavorites: Set<String> = emptySet(),
) : FavoritePhraseRepository {
    private val favorites = initialFavorites.toMutableSet()

    override fun getFavoritePhraseIds(): Set<String> = favorites.toSet()

    override fun toggleFavoritePhrase(phraseId: String): Set<String> {
        if (!favorites.add(phraseId)) {
            favorites.remove(phraseId)
        }
        return favorites.toSet()
    }
}

internal class FakeSpacedReviewRepository : SpacedReviewRepository {
    val states = mutableMapOf<String, PhraseReviewState>()
    var dueIds: List<String> = emptyList()
    var adaptiveIds: List<String> = emptyList()
    var adaptiveRequestCount = 0
    var recordedReviews = mutableListOf<Pair<String, ReviewGrade>>()

    override fun getReviewState(phraseId: String): PhraseReviewState? = states[phraseId]

    override fun getDuePhraseIds(allPhraseIds: List<String>, limit: Int): List<String> =
        dueIds.filter { it in allPhraseIds }.take(limit)

    override fun getAdaptivePhraseIds(
        allPhraseIds: List<String>,
        favoritePhraseIds: Set<String>,
        limit: Int,
    ): List<String> {
        adaptiveRequestCount += 1
        return (adaptiveIds.ifEmpty { allPhraseIds }).filter { it in allPhraseIds }.take(limit)
    }

    override fun recordReview(phraseId: String, grade: ReviewGrade): PhraseReviewState {
        recordedReviews += phraseId to grade
        val current = states[phraseId] ?: PhraseReviewState(
            phraseId = phraseId,
            dueDate = "2026-05-20",
            intervalDays = 0,
            easeFactor = SpacedReviewScheduler.DEFAULT_EASE,
            reviewCount = 0,
            correctStreak = 0,
        )
        val next = SpacedReviewScheduler.schedule(current, grade, ::dueDate)
        states[phraseId] = next
        return next
    }

    override fun recordPronunciationScore(phraseId: String, score: Int): PhraseReviewState? {
        val current = states[phraseId] ?: PhraseReviewState(
            phraseId = phraseId,
            dueDate = "2026-05-20",
            intervalDays = 0,
            easeFactor = SpacedReviewScheduler.DEFAULT_EASE,
            reviewCount = 0,
            correctStreak = 0,
        )
        val next = current.copy(lastScore = score.coerceIn(0, 100))
        states[phraseId] = next
        return next
    }

    private fun dueDate(offsetDays: Int): String =
        when (offsetDays) {
            0 -> "2026-05-20"
            1 -> "2026-05-21"
            else -> "2026-05-${20 + offsetDays}"
        }
}

internal class FakeDailyGoalRepository : DailyGoalRepository {
    var progress = DailyGoalProgress(date = "2026-05-20")

    override fun getTodayProgress(streakDays: Int): DailyGoalProgress =
        progress.copy(streakDays = streakDays)

    override fun recordNewPhrase() {
        progress = progress.copy(completedNewPhrases = progress.completedNewPhrases + 1)
    }

    override fun recordReview() {
        progress = progress.copy(completedReviews = progress.completedReviews + 1)
    }

    override fun recordSpeakingAttempt() {
        progress = progress.copy(completedSpeakingAttempts = progress.completedSpeakingAttempts + 1)
    }

    override fun recordPracticeMinutes(minutes: Int) {
        progress = progress.copy(completedPracticeMinutes = progress.completedPracticeMinutes + minutes)
    }

    override fun recordAiCoachRequest() {
        progress = progress.copy(completedAiCoachRequests = progress.completedAiCoachRequests + 1)
    }
}

internal class FakeAiCoachRepository : AiCoachRepository {
    override suspend fun getFeedback(
        endpointUrl: String,
        phrase: Phrase,
        spokenText: String,
    ): AiCoachFeedback = AiCoachFeedback(message = "Good start.", score = 80)
}

internal fun testPhrases(): List<Phrase> =
    listOf(
        Phrase(
            id = "survival-exit",
            portuguese = "Onde fica a saída?",
            english = "Where is the exit?",
            pronunciationGuide = "onde fica a saida",
            category = PhraseCategory.Survival,
        ),
        Phrase(
            id = "restaurant-water",
            portuguese = "Eu gostaria de água.",
            english = "I would like water.",
            pronunciationGuide = "eu gos-tah-REE-ah jeh AH-gwah",
            category = PhraseCategory.Restaurant,
        ),
        Phrase(
            id = "medical-doctor",
            portuguese = "Preciso de um médico.",
            english = "I need a doctor.",
            pronunciationGuide = "preh-SEE-zoo jee oong MEH-jee-koo",
            category = PhraseCategory.Medical,
        ),
        Phrase(
            id = "travel-airport",
            portuguese = "O aeroporto fica longe?",
            english = "Is the airport far?",
            pronunciationGuide = "oo ah-eh-roh-PORT-oh FEE-kah LON-jee",
            category = PhraseCategory.Travel,
        ),
    )
