package com.duddy.portugues

import com.duddy.portugues.data.repository.BackendRequestException
import com.duddy.portugues.presentation.UserFacingErrors
import java.net.ConnectException
import org.junit.Assert.assertEquals
import org.junit.Test

class UserFacingErrorsTest {
    @Test
    fun quotaErrorsUseFreeLimitMessage() {
        assertEquals(
            "You've hit today's free limit.",
            UserFacingErrors.forAiCoach(
                BackendRequestException(
                    statusCode = 402,
                    backendMessage = "Daily coach limit reached.",
                )
            ),
        )
    }

    @Test
    fun networkErrorsUseServerMessage() {
        assertEquals(
            "Can't reach the server. Check your connection and try again.",
            UserFacingErrors.forAiCoach(ConnectException("failed to connect")),
        )
    }

    @Test
    fun pronunciationServiceErrorsUsePronunciationMessage() {
        assertEquals(
            "Pronunciation service is unavailable. Try again in a little bit.",
            UserFacingErrors.forPronunciation(
                BackendRequestException(
                    statusCode = 503,
                    backendMessage = "Azure returned 503.",
                )
            ),
        )
    }

    @Test
    fun microphonePermissionErrorsUsePermissionMessage() {
        assertEquals(
            "Microphone permission required.",
            UserFacingErrors.forMicrophoneStart(SecurityException("missing permission")),
        )
    }
}
