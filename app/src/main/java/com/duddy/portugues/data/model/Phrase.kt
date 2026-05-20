package com.duddy.portugues.data.model

data class Phrase(
    val id: String,
    val portuguese: String,
    val english: String,
    val pronunciationGuide: String,
    val category: PhraseCategory,
    val subcategory: String = "",
    val difficulty: PhraseDifficulty = PhraseDifficulty.A1,
    val tags: List<String> = emptyList(),
    val speakingPractice: Boolean = true,
    val grammarNote: String? = null,
)

enum class PhraseDifficulty {
    A1, A2, B1, B2;

    companion object {
        fun parse(raw: String?): PhraseDifficulty =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: A1
    }
}

enum class PhraseCategory(val key: String, val displayName: String) {
    Survival("survival", "Survival"),
    Travel("travel", "Travel"),
    Restaurant("restaurants", "Restaurants"),
    Conversation("conversation", "Conversation"),
    DatingSocial("dating_social", "Dating / Social"),
    Medical("medical", "Medical"),
    Work("work", "Work"),
    Shopping("shopping", "Shopping"),
    Transportation("transportation", "Transportation"),
    Emotions("emotions", "Emotions"),
    Questions("questions", "Questions"),
    Verbs("verbs", "Verbs"),
    SentencePatterns("sentence_patterns", "Sentence Patterns");

    companion object {
        fun parse(raw: String?): PhraseCategory? {
            val normalized = raw.orEmpty().normalizeKey()
            return entries.firstOrNull { category ->
                normalized == category.key.normalizeKey() ||
                    normalized == category.name.normalizeKey() ||
                    normalized == category.displayName.normalizeKey()
            }
        }

        private fun String.normalizeKey(): String =
            lowercase().filter { it.isLetterOrDigit() }
    }
}
