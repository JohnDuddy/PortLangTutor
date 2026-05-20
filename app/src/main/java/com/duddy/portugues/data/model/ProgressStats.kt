package com.duddy.portugues.data.model

data class ProgressStats(
    val lessonsStarted: Int,
    val practicedPhrases: Int,
    val sampleAudioPlays: Int,
    val speakingAttempts: Int,
    val aiCoachRequests: Int,
    val streakDays: Int
)
