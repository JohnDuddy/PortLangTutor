package com.duddy.portugues.data.auth

import android.content.Context
import android.util.Log
import com.duddy.portugues.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Thin wrapper around the Supabase Kotlin auth SDK.
 *
 *  All UI code goes through this — no direct SDK usage anywhere else.
 *  The wrapper produces simple suspend functions and a Flow of [AuthState].
 */
class SupabaseAuthClient private constructor(
    private val client: SupabaseClient,
) {

    companion object {
        private const val TAG = "SupabaseAuthClient"

        @Volatile private var instance: SupabaseAuthClient? = null

        fun isConfigured(): Boolean =
            BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

        fun get(context: Context): SupabaseAuthClient =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): SupabaseAuthClient {
            require(BuildConfig.SUPABASE_URL.isNotBlank()) {
                "SUPABASE_URL not configured in local.properties"
            }
            require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
                "SUPABASE_ANON_KEY not configured in local.properties"
            }
            val client = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
            ) {
                install(Auth) {
                    autoLoadFromStorage = true
                    alwaysAutoRefresh   = true
                }
            }
            return SupabaseAuthClient(client)
        }
    }

    /** Suspends until the persisted session (if any) has loaded. */
    suspend fun awaitSessionLoaded() {
        client.auth.awaitInitialization()
    }

    /** Reactive auth state. Emits Loading → SignedIn/SignedOut. */
    fun observe(): Flow<AuthState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated    -> AuthState.SignedIn(
                userId      = status.session.user?.id ?: "",
                email       = status.session.user?.email,
                accessToken = status.session.accessToken,
            )
            is SessionStatus.NotAuthenticated -> AuthState.SignedOut
            is SessionStatus.Initializing     -> AuthState.Loading
            is SessionStatus.RefreshFailure   -> AuthState.SignedOut
        }
    }

    suspend fun currentAccessToken(): String? =
        client.auth.currentSessionOrNull()?.accessToken

    suspend fun currentUserId(): String? =
        client.auth.currentSessionOrNull()?.user?.id

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signUpWith(Email) {
            this.email    = email
            this.password = password
        }
        Unit
    }.onFailure { Log.w(TAG, "signUp failed", it) }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signInWith(Email) {
            this.email    = email
            this.password = password
        }
    }.onFailure { Log.w(TAG, "signIn failed", it) }

    suspend fun signOut(): Result<Unit> = runCatching { client.auth.signOut() }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        client.auth.resetPasswordForEmail(email)
    }.onFailure { Log.w(TAG, "sendPasswordReset failed", it) }
}

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(
        val userId: String,
        val email: String?,
        val accessToken: String,
    ) : AuthState
}
