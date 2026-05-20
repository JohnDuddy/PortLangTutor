package com.duddy.portugues.data.repository

import com.duddy.portugues.data.model.AiCoachFeedback
import com.duddy.portugues.data.model.PhonemeScore
import com.duddy.portugues.data.model.Phrase
import com.duddy.portugues.data.model.ReviewGrade
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RemoteAiCoachRepository(
    private val authTokenProvider: suspend () -> String? = { null },
) : AiCoachRepository {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    override suspend fun getFeedback(
        endpointUrl: String,
        phrase: Phrase,
        spokenText: String,
    ): AiCoachFeedback =
        getFeedback(
            endpointUrl = endpointUrl,
            phrase = phrase,
            spokenText = spokenText,
            pronunciationScore = null,
            phonemeErrors = emptyList(),
        )

    suspend fun getFeedback(
        endpointUrl: String,
        phrase: Phrase,
        spokenText: String,
        pronunciationScore: Double?,
        phonemeErrors: List<PhonemeScore>,
    ): AiCoachFeedback = withContext(Dispatchers.IO) {
        val trimmedEndpoint = endpointUrl.trim()
        if (trimmedEndpoint.isBlank()) {
            throw IOException("AI coach endpoint is blank.")
        }

        val requestBody = if (usesFastApiContract(trimmedEndpoint)) {
            buildFastApiRequestBody(
                phrase = phrase,
                spokenText = spokenText,
                pronunciationScore = pronunciationScore,
                phonemeErrors = phonemeErrors,
            )
        } else {
            buildLegacyRequestBody(phrase = phrase, spokenText = spokenText)
        }

        val builder = Request.Builder()
            .url(trimmedEndpoint)
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))

        authTokenProvider()?.takeIf { token -> token.isNotBlank() }?.let { token ->
            builder.addHeader("Authorization", "Bearer $token")
        }

        http.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(extractError(body) ?: body.take(240).ifBlank { "AI coach request failed." })
            }

            parseFeedback(body)
        }
    }

    private fun buildFastApiRequestBody(
        phrase: Phrase,
        spokenText: String,
        pronunciationScore: Double?,
        phonemeErrors: List<PhonemeScore>,
    ): JSONObject =
        JSONObject()
            .put("target_phrase", phrase.portuguese)
            .put("english", phrase.english)
            .put("pronunciation_guide", phrase.pronunciationGuide)
            .put("category", phrase.category.displayName)
            .put("spoken_text", spokenText)
            .apply {
                pronunciationScore?.let { put("pronunciation_score", it) }
                if (phonemeErrors.isNotEmpty()) {
                    put(
                        "phoneme_errors",
                        JSONArray().apply {
                            phonemeErrors.take(MAX_PHONEME_ERRORS).forEach { phoneme ->
                                put(
                                    JSONObject()
                                        .put("phoneme", phoneme.phoneme)
                                        .put("score", phoneme.score ?: 0.0)
                                )
                            }
                        }
                    )
                }
            }

    private fun buildLegacyRequestBody(
        phrase: Phrase,
        spokenText: String,
    ): JSONObject =
        JSONObject()
            .put("targetPhrase", phrase.portuguese)
            .put("english", phrase.english)
            .put("pronunciationGuide", phrase.pronunciationGuide)
            .put("category", phrase.category.displayName)
            .put("spokenText", spokenText)

    private fun parseFeedback(body: String): AiCoachFeedback {
        val json = JSONObject(body)
        val legacy = json.optString("feedback").trim()
        if (legacy.isNotBlank()) return AiCoachFeedback(message = legacy)

        val score = json.optInt("score", -1).takeIf { it >= 0 }
        val fix = json.optString("fix").trim()
        val model = json.optString("model").trim()
        val nextRep = json.optString("next_rep").trim()
        val encouragement = json.optString("encouragement").trim()
        val focusArea = json.optString("focus_area").trim()
        val provider = json.optString("provider").trim()
        val adaptiveGrade = parseAdaptiveGrade(json.optString("adaptive_grade"))

        val message = buildString {
            score?.let { appendLine("Score: $it") }
            if (fix.isNotBlank()) appendLine("Fix: $fix")
            if (model.isNotBlank()) appendLine("Model: $model")
            if (nextRep.isNotBlank()) appendLine("Next rep: $nextRep")
            if (encouragement.isNotBlank()) append(encouragement)
        }.trim().ifBlank { "AI coach returned an empty response." }

        return AiCoachFeedback(
            message = message,
            score = score,
            fix = fix,
            model = model,
            nextRep = nextRep,
            encouragement = encouragement,
            focusArea = focusArea,
            adaptiveGrade = adaptiveGrade,
            provider = provider,
        )
    }

    private fun extractError(body: String): String? =
        runCatching {
            val json = JSONObject(body)
            when (val detail = json.opt("detail")) {
                is String -> detail
                is JSONObject -> detail.optString("detail").ifBlank { detail.optString("error") }
                is JSONArray -> detail.toString()
                else -> json.optString("error").takeIf { it.isNotBlank() }
            }
        }.getOrNull()

    private fun usesFastApiContract(endpointUrl: String): Boolean =
        endpointUrl.contains("/v1/coach", ignoreCase = true)

    private fun parseAdaptiveGrade(value: String): ReviewGrade? =
        when (value.trim().lowercase()) {
            "again" -> ReviewGrade.Again
            "hard" -> ReviewGrade.Hard
            "good" -> ReviewGrade.Good
            "easy" -> ReviewGrade.Easy
            else -> null
        }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        const val MAX_PHONEME_ERRORS = 5
    }
}
