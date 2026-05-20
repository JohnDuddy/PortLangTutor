package com.duddy.portugues.data.model

enum class LearningTechnique(
    val displayName: String,
    val learnerCue: String
) {
    RetrievalPractice(
        displayName = "Active recall",
        learnerCue = "Try from memory before revealing the answer."
    ),
    SpacedRepetition(
        displayName = "Spaced review",
        learnerCue = "Grade recall so the app schedules the next review."
    ),
    Interleaving(
        displayName = "Mixed practice",
        learnerCue = "Phrases are mixed across situations to improve recognition."
    ),
    ProductionPractice(
        displayName = "Speak aloud",
        learnerCue = "Say the phrase and compare your transcript."
    ),
    Feedback(
        displayName = "AI feedback",
        learnerCue = "Ask the coach for one correction and one next step."
    )
}
