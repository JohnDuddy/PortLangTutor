package com.duddy.portugues.data.model

enum class TestingLevel(
    val title: String,
    val subtitle: String,
) {
    LevelI(
        title = "Level I",
        subtitle = "Two-section translation test using every phrase.",
    ),
    LevelII(
        title = "Level II",
        subtitle = "Random listening test using every phrase.",
    ),
    LevelIII(
        title = "Level III",
        subtitle = "Speaking exam with live pronunciation scoring.",
    ),
}

enum class TestingQuestionType {
    MultipleChoice,
    FillBlank,
    Pronunciation,
}

data class TestingQuestion(
    val id: String,
    val level: TestingLevel,
    val type: TestingQuestionType,
    val phrase: Phrase,
    val sectionTitle: String = "",
    val prompt: String,
    val choices: List<String> = emptyList(),
    val blankPrompt: String = "",
    val expectedAnswer: String = phrase.portuguese,
    val spokenPrompt: String = "",
    val spokenPromptLocaleTag: String = "",
    val spokenAnswer: String = expectedAnswer,
    val spokenAnswerLocaleTag: String = "",
    val mustRetryUntilCorrect: Boolean = false,
)

data class TestingAnswerResult(
    val isCorrect: Boolean,
    val expectedAnswer: String,
    val message: String,
    val pronunciationScore: Int? = null,
    val spokenFeedback: String = "",
    val spokenFeedbackLocaleTag: String = "",
    val spokenFeedbackTranslation: String = "",
    val spokenFeedbackTranslationLocaleTag: String = "",
)

data class TestingUiState(
    val selectedLevel: TestingLevel? = null,
    val questions: List<TestingQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedChoice: String? = null,
    val fillBlankAnswer: String = "",
    val answeredQuestionIds: Set<String> = emptySet(),
    val correctCount: Int = 0,
    val completed: Boolean = false,
    val lastResult: TestingAnswerResult? = null,
    val pronunciationScores: Map<String, Int> = emptyMap(),
) {
    val currentQuestion: TestingQuestion?
        get() = questions.getOrNull(currentIndex)

    val answeredCount: Int
        get() = answeredQuestionIds.size

    val totalCount: Int
        get() = questions.size

    val percentScore: Int
        get() = if (answeredCount == 0) 0 else ((correctCount * 100.0) / answeredCount).toInt()

    val isCurrentAnswered: Boolean
        get() = currentQuestion?.id in answeredQuestionIds
}
