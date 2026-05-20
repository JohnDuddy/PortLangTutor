package com.duddy.portugues.data.repository

import com.duddy.portugues.data.model.ProgressStats

interface ProgressRepository {
    fun getStats(): ProgressStats
    fun recordLessonStarted()
    fun recordPhrasePracticed()
    fun recordSampleAudioPlayed()
    fun recordSpeakingAttempt()
    fun recordPronunciationAssessed()
    fun recordAiCoachRequest()
    fun recordMistake()
    fun refillHearts()
    fun setDailyXpGoal(goalXp: Int)
}
