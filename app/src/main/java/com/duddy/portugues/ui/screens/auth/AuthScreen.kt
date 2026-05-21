package com.duddy.portugues.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duddy.portugues.presentation.AuthMode
import com.duddy.portugues.presentation.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onContinueWithoutAccount: (() -> Unit)? = null,
    trialAvailable: Boolean = onContinueWithoutAccount != null,
) {
    val state = viewModel.uiState
    var showPassword by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            // ── Brand header ───────────────────────────────────────────────
            Text(
                text       = "Duddy Português",
                fontSize   = 32.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
            )
            Text(
                text     = "Your Brazilian Portuguese tutor",
                fontSize = 14.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))

            // ── Mode tabs ──────────────────────────────────────────────────
            if (state.mode != AuthMode.ResetPassword) {
                TabRow(
                    selectedTabIndex = if (state.mode == AuthMode.SignIn) 0 else 1,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Tab(
                        selected = state.mode == AuthMode.SignIn,
                        onClick  = { viewModel.setMode(AuthMode.SignIn) },
                        text     = { Text("Sign in") },
                    )
                    Tab(
                        selected = state.mode == AuthMode.SignUp,
                        onClick  = { viewModel.setMode(AuthMode.SignUp) },
                        text     = { Text("Create account") },
                    )
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Text(
                    "Reset password",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Email ──
            OutlinedTextField(
                value          = state.email,
                onValueChange  = viewModel::setEmail,
                label          = { Text("Email") },
                singleLine     = true,
                modifier       = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                enabled        = !state.loading,
            )

            // ── Password ──
            if (state.mode != AuthMode.ResetPassword) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = state.password,
                    onValueChange = viewModel::setPassword,
                    label         = { Text("Password") },
                    singleLine    = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier      = Modifier.fillMaxWidth(),
                    enabled       = !state.loading,
                    trailingIcon  = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(if (showPassword) "Hide" else "Show", fontSize = 12.sp)
                        }
                    },
                )
            }

            // ── Confirm password (sign up only) ──
            if (state.mode == AuthMode.SignUp) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = state.confirmPassword,
                    onValueChange = viewModel::setConfirmPwd,
                    label         = { Text("Confirm password") },
                    singleLine    = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier      = Modifier.fillMaxWidth(),
                    enabled       = !state.loading,
                )
            }

            // ── Error / info ──
            state.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            state.infoMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Color(0xFF1A6B35), fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            // ── Submit ──
            Button(
                onClick  = { viewModel.submit() },
                enabled  = !state.loading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color     = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        when (state.mode) {
                            AuthMode.SignIn        -> "Sign in"
                            AuthMode.SignUp        -> "Create account"
                            AuthMode.ResetPassword -> "Send reset link"
                        }
                    )
                }
            }

            if (onContinueWithoutAccount != null && trialAvailable) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onContinueWithoutAccount,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Try one session first")
                }
            } else if (onContinueWithoutAccount != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Trial session used. Create a free account to save progress.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Footer links ──
            Spacer(Modifier.height(8.dp))
            if (state.mode == AuthMode.SignIn) {
                TextButton(onClick = { viewModel.setMode(AuthMode.ResetPassword) }) {
                    Text("Forgot password?", fontSize = 13.sp)
                }
            } else if (state.mode == AuthMode.ResetPassword) {
                TextButton(onClick = { viewModel.setMode(AuthMode.SignIn) }) {
                    Text("Back to sign in", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(40.dp))
            Text(
                "By signing in you agree to the Terms & Privacy Policy.",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
