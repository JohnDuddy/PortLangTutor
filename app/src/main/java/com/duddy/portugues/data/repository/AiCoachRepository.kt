package com.duddy.portugues.data.repository

import com.duddy.portugues.data.model.AiCoachFeedback
import com.duddy.portugues.data.model.Phrase

interface AiCoachRepository {
    suspend fun getFeedback(
        endpointUrl: String,
        phrase: Phrase,
        spokenText: String
    ): AiCoachFeedback
}
