package com.duddy.portugues.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duddy.portugues.data.auth.SupabaseAuthClient
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val mode: AuthMode = AuthMode.SignIn,
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

enum class AuthMode { SignIn, SignUp, ResetPassword }

class AuthViewModel(
    private val auth: SupabaseAuthClient,
) : ViewModel() {

    var uiState by mutableStateOf(AuthUiState())
        private set

    fun setEmail(v: String)     { uiState = uiState.copy(email = v,    errorMessage = null) }
    fun setPassword(v: String)  { uiState = uiState.copy(password = v, errorMessage = null) }
    fun setConfirmPwd(v: String){ uiState = uiState.copy(confirmPassword = v, errorMessage = null) }

    fun setMode(mode: AuthMode) {
        uiState = uiState.copy(mode = mode, errorMessage = null, infoMessage = null)
    }

    fun submit() {
        val s = uiState
        if (s.email.isBlank() || !s.email.contains("@")) {
            uiState = s.copy(errorMessage = "Enter a valid email address."); return
        }
        if (s.mode != AuthMode.ResetPassword && s.password.length < 6) {
            uiState = s.copy(errorMessage = "Password must be at least 6 characters."); return
        }
        if (s.mode == AuthMode.SignUp && s.password != s.confirmPassword) {
            uiState = s.copy(errorMessage = "Passwords do not match."); return
        }

        uiState = s.copy(loading = true, errorMessage = null, infoMessage = null)
        viewModelScope.launch {
            val result = when (s.mode) {
                AuthMode.SignIn        -> auth.signIn(s.email.trim(), s.password)
                AuthMode.SignUp        -> auth.signUp(s.email.trim(), s.password)
                AuthMode.ResetPassword -> auth.sendPasswordReset(s.email.trim())
            }
            uiState = result.fold(
                onSuccess = {
                    val msg = when (s.mode) {
                        AuthMode.SignUp        -> "Check your email to confirm your account."
                        AuthMode.ResetPassword -> "Reset link sent to ${s.email}."
                        AuthMode.SignIn        -> null
                    }
                    uiState.copy(loading = false, infoMessage = msg, password = "", confirmPassword = "")
                },
                onFailure = { uiState.copy(loading = false, errorMessage = it.message ?: "Authentication failed.") },
            )
        }
    }

    companion object {
        fun factory(auth: SupabaseAuthClient): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(auth) as T
            }
    }
}
