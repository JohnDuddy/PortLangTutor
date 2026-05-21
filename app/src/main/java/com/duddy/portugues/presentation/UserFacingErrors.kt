package com.duddy.portugues.presentation

import com.duddy.portugues.data.repository.BackendRequestException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object UserFacingErrors {
    const val CANT_REACH_SERVER = "Can't reach the server. Check your connection and try again."
    const val PRONUNCIATION_UNAVAILABLE = "Pronunciation service is unavailable. Try again in a little bit."
    const val DAILY_FREE_LIMIT = "You've hit today's free limit."
    const val MICROPHONE_PERMISSION_REQUIRED = "Microphone permission required."

    fun forAiCoach(error: Throwable): String =
        when {
            error.isQuotaLimit() -> DAILY_FREE_LIMIT
            error.isServerUnavailable() || error.isNetworkFailure() -> CANT_REACH_SERVER
            else -> "AI coach is unavailable. Try again in a little bit."
        }

    fun forPronunciation(error: Throwable): String =
        when {
            error.isQuotaLimit() -> DAILY_FREE_LIMIT
            error.isServerUnavailable() -> PRONUNCIATION_UNAVAILABLE
            error.isNetworkFailure() -> CANT_REACH_SERVER
            else -> PRONUNCIATION_UNAVAILABLE
        }

    fun forMicrophoneStart(error: Throwable): String =
        if (error.message.orEmpty().contains("permission", ignoreCase = true) ||
            error is SecurityException
        ) {
            MICROPHONE_PERMISSION_REQUIRED
        } else {
            "Couldn't start microphone. Try again."
        }

    private fun Throwable.isQuotaLimit(): Boolean =
        this is BackendRequestException && statusCode == 402 ||
            message.orEmpty().contains("quota_exceeded", ignoreCase = true) ||
            message.orEmpty().contains("limit reached", ignoreCase = true)

    private fun Throwable.isServerUnavailable(): Boolean =
        this is BackendRequestException && statusCode in setOf(500, 502, 503, 504)

    private fun Throwable.isNetworkFailure(): Boolean =
        this is UnknownHostException ||
            this is SocketTimeoutException ||
            this is IOException && this !is BackendRequestException ||
            message.orEmpty().contains("failed to connect", ignoreCase = true) ||
            message.orEmpty().contains("connection refused", ignoreCase = true) ||
            message.orEmpty().contains("timeout", ignoreCase = true)
}
