package com.duddy.portugues

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duddy.portugues.data.auth.AuthState
import com.duddy.portugues.data.auth.SupabaseAuthClient
import com.duddy.portugues.data.local.migration.SharedPreferencesToRoomMigration
import com.duddy.portugues.data.preferences.OnboardingPreferences
import com.duddy.portugues.presentation.AuthViewModel
import com.duddy.portugues.presentation.TutorViewModel
import com.duddy.portugues.ui.screens.DuddyApp
import com.duddy.portugues.ui.screens.auth.AuthScreen
import com.duddy.portugues.ui.screens.onboarding.OnboardingFlow
import com.duddy.portugues.ui.theme.DuddyPortuguesTheme
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var ready by mutableStateOf(false)

    /** Asks for microphone permission. Result triggers a recomposition. */
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled implicitly via recorder.hasPermission() */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Migrate SharedPreferences → Room ONCE before any ViewModel touches the DB.
        lifecycleScope.launch {
            SharedPreferencesToRoomMigration.migrateIfNeeded(this@MainActivity)
            ready = true
        }

        // Proactively request microphone permission at first launch.
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            DuddyPortuguesTheme {
                if (!ready) {
                    SplashLoading()
                } else {
                    AppRoot(context = applicationContext)
                }
            }
        }
    }
}

@Composable
private fun SplashLoading() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Loading Duddy…")
            }
        }
    }
}

@Composable
private fun AppRoot(context: android.content.Context) {
    var onboardingComplete by remember {
        mutableStateOf(OnboardingPreferences.isCompleted(context))
    }
    var trialMode by remember { mutableStateOf(false) }
    var trialSessionUsed by remember {
        mutableStateOf(OnboardingPreferences.hasUsedTrialSession(context))
    }

    if (!onboardingComplete) {
        OnboardingFlow(
            onComplete = { placementLevel, dailyMinutes ->
                OnboardingPreferences.complete(
                    context = context,
                    placementLevel = placementLevel,
                    dailyMinutes = dailyMinutes,
                )
                onboardingComplete = true
            },
        )
        return
    }

    if (trialMode) {
        val tutorVm: TutorViewModel = viewModel(
            factory = TutorViewModel.factory(context, authClient = null)
        )
        DuddyApp(
            viewModel = tutorVm,
            isTrialMode = true,
            trialSessionUsed = trialSessionUsed,
            onTrialSessionStarted = {
                OnboardingPreferences.markTrialSessionUsed(context)
                trialSessionUsed = true
            },
            onExitTrial = { trialMode = false },
        )
        return
    }

    if (!SupabaseAuthClient.isConfigured()) {
        val tutorVm: TutorViewModel = viewModel(
            factory = TutorViewModel.factory(context, authClient = null)
        )
        DuddyApp(viewModel = tutorVm)
        return
    }

    val authClient = remember { SupabaseAuthClient.get(context) }

    // Trigger initial session load (auto-restore persisted token if any)
    LaunchedEffect(Unit) { authClient.awaitSessionLoaded() }

    val authState by authClient.observe().collectAsState(initial = AuthState.Loading)

    when (val s = authState) {
        is AuthState.Loading -> SplashLoading()

        is AuthState.SignedOut -> {
            val authVm: AuthViewModel = viewModel(factory = AuthViewModel.factory(authClient))
            AuthScreen(
                viewModel = authVm,
                onContinueWithoutAccount = { trialMode = true },
                trialAvailable = !trialSessionUsed,
            )
        }

        is AuthState.SignedIn -> {
            val tutorVm: TutorViewModel = viewModel(
                factory = TutorViewModel.factory(context, authClient)
            )
            DuddyApp(viewModel = tutorVm)
        }
    }
}
