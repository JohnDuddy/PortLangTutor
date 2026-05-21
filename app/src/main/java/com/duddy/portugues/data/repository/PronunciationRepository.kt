package com.duddy.portugues.data.repository

import com.duddy.portugues.data.model.PhonemeErrorType
import com.duddy.portugues.data.model.PhonemeScore
import com.duddy.portugues.data.model.PronunciationResult
import com.duddy.portugues.data.model.PronunciationScores
import com.duddy.portugues.data.model.WordAssessment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

interface PronunciationRepository {
    /**
     * Send recorded audio to the backend for Azure assessment.
     *
     * @param wavBytes 16 kHz mono PCM WAV bytes
     * @param referenceText The phrase the user was supposed to say
     * @param locale BCP-47 locale, default "pt-BR"
     * @param durationSeconds Optional accurate duration for accurate quota metering
     */
    suspend fun assess(
        wavBytes: ByteArray,
        referenceText: String,
        locale: String = "pt-BR",
        durationSeconds: Double? = null,
    ): PronunciationResult
}

/**
 * Sends multipart/form-data to the backend `/v1/pronunciation` endpoint.
 *
 * @param endpointUrl      Full URL e.g. "https://api.duddy.app/v1/pronunciation"
 * @param authTokenProvider Suspendable getter for the current Supabase JWT
 */
class RemotePronunciationRepository(
    private val endpointUrl: String,
    private val authTokenProvider: suspend () -> String?,
) : PronunciationRepository {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    override suspend fun assess(
        wavBytes: ByteArray,
        referenceText: String,
        locale: String,
        durationSeconds: Double?,
    ): PronunciationResult = withContext(Dispatchers.IO) {
        val token = authTokenProvider()

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("reference_text", referenceText)
            .addFormDataPart("locale", locale)
            .apply { durationSeconds?.let { addFormDataPart("duration_seconds", it.toString()) } }
            .addFormDataPart(
                "audio", "rec.wav",
                wavBytes.toRequestBody("audio/wav".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(endpointUrl)
            .post(multipart)
            .apply {
                token?.takeIf { it.isNotBlank() }?.let {
                    addHeader("Authorization", "Bearer $it")
                }
            }
            .build()

        http.newCall(request).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw BackendRequestException(
                    statusCode = resp.code,
                    backendMessage = extractError(bodyStr)
                        ?: bodyStr.take(240).ifBlank { "Pronunciation request failed." },
                )
            }
            parse(bodyStr, referenceText)
        }
    }

    private fun extractError(body: String): String? = runCatching {
        val obj = JSONObject(body)
        when (val d = obj.opt("detail")) {
            is String     -> d
            is JSONObject -> d.optString("detail").ifBlank { d.optString("error") }
            else          -> obj.optString("error").takeIf { it.isNotBlank() }
        }
    }.getOrNull()

    private fun parse(body: String, reference: String): PronunciationResult {
        val obj     = JSONObject(body)
        val overall = obj.optJSONObject("overall") ?: JSONObject()

        val scores = PronunciationScores(
            pronunciation = overall.optDoubleOrNull("pronunciation"),
            accuracy      = overall.optDoubleOrNull("accuracy"),
            fluency       = overall.optDoubleOrNull("fluency"),
            completeness  = overall.optDoubleOrNull("completeness"),
            prosody       = overall.optDoubleOrNull("prosody"),
        )

        val words = obj.optJSONArray("words").mapJsonObjects { w ->
            WordAssessment(
                word      = w.optString("word"),
                accuracy  = w.optDoubleOrNull("accuracy"),
                errorType = PhonemeErrorType.parse(w.optString("error_type")),
                phonemes  = w.optJSONArray("phonemes").mapJsonObjects { p ->
                    PhonemeScore(p.optString("phoneme"), p.optDoubleOrNull("score"))
                },
            )
        }

        val weak = obj.optJSONArray("phonemes_low").mapJsonObjects { p ->
            PhonemeScore(p.optString("phoneme"), p.optDoubleOrNull("score"))
        }

        return PronunciationResult(
            overall        = scores,
            words          = words,
            phonemesLow    = weak,
            recognizedText = obj.optString("recognized_text"),
            referenceText  = obj.optString("reference_text").ifBlank { reference },
        )
    }
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key).takeIf { !it.isNaN() }
}

private inline fun <T> JSONArray?.mapJsonObjects(block: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val o = optJSONObject(i) ?: continue
        out.add(block(o))
    }
    return out
}
