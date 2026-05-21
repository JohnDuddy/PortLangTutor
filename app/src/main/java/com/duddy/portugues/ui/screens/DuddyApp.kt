package com.duddy.portugues.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.duddy.portugues.audio.ListenPlaybackService
import com.duddy.portugues.data.model.PhraseCategory
import com.duddy.portugues.data.model.ReviewGrade
import com.duddy.portugues.data.preferences.OnboardingPreferences
import com.duddy.portugues.presentation.AppScreen
import com.duddy.portugues.presentation.TutorViewModel

@Composable
fun DuddyApp(
    viewModel: TutorViewModel,
    isTrialMode: Boolean = false,
    trialSessionUsed: Boolean = false,
    onTrialSessionStarted: () -> Unit = {},
    onExitTrial: () -> Unit = {},
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    var showHomeTooltip by remember {
        mutableStateOf(!OnboardingPreferences.hasSeenHomeTooltip(context))
    }

    fun startListenPlayback(category: PhraseCategory? = null) {
        val intent = Intent(context, ListenPlaybackService::class.java).apply {
            action = if (category == null) {
                ListenPlaybackService.ACTION_PLAY_ALL
            } else {
                ListenPlaybackService.ACTION_PLAY_CATEGORY
            }
            category?.let { putExtra(ListenPlaybackService.EXTRA_CATEGORY_KEY, it.key) }
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopListenPlayback() {
        val intent = Intent(context, ListenPlaybackService::class.java).apply {
            action = ListenPlaybackService.ACTION_STOP
        }
        context.startService(intent)
    }

    Surface {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val availableScreens = if (isTrialMode && !trialSessionUsed) {
                        listOf(AppScreen.Home)
                    } else if (isTrialMode) {
                        listOf(AppScreen.Home, AppScreen.Practice)
                    } else {
                        AppScreen.entries
                    }
                    availableScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = uiState.currentScreen == screen,
                            onClick = { viewModel.navigateTo(screen) },
                            icon = { Text(screen.navIcon()) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            when (uiState.currentScreen) {
                AppScreen.Home -> HomeScreen(
                    phraseCount = uiState.allPhrases.size,
                    favoriteCount = uiState.favoritePhraseIds.size,
                    dueReviewCount = uiState.dueReviewCount,
                    dailyGoal = uiState.dailyGoal,
                    stats = uiState.stats,
                    guidedSessionSteps = uiState.guidedSessionSteps,
                    statusMessage = uiState.statusMessage,
                    showFirstRunTip = showHomeTooltip,
                    isTrialMode = isTrialMode,
                    trialSessionUsed = trialSessionUsed,
                    onExitTrial = onExitTrial,
                    onDismissFirstRunTip = {
                        OnboardingPreferences.markHomeTooltipSeen(context)
                        showHomeTooltip = false
                    },
                    onStartSmartReview = viewModel::startSmartReview,
                    onStartGuidedSession = {
                        if (isTrialMode && trialSessionUsed) {
                            onExitTrial()
                        } else {
                            if (isTrialMode) {
                                onTrialSessionStarted()
                            }
                            viewModel.startGuidedSession()
                        }
                    },
                    onStartPractice = viewModel::practiceAllPhrases,
                    onPracticeFavorites = viewModel::practiceFavoritePhrases,
                    onViewLessons = { viewModel.navigateTo(AppScreen.Lessons) },
                    modifier = modifier
                )

                AppScreen.Lessons -> LessonListScreen(
                    lessons = uiState.lessons,
                    searchQuery = uiState.phraseSearchQuery,
                    searchResults = uiState.phraseSearchResults,
                    favoritePhraseIds = uiState.favoritePhraseIds,
                    onSearchQueryChanged = viewModel::updatePhraseSearchQuery,
                    onLessonSelected = viewModel::startLesson,
                    onLessonListenSelected = { lesson ->
                        viewModel.listenToLesson(lesson)
                        startListenPlayback(lesson.category)
                    },
                    onPhraseSelected = viewModel::startPhrase,
                    onFavoriteToggled = viewModel::toggleFavoritePhrase,
                    modifier = modifier
                )

                AppScreen.Listen -> ListenScreen(
                    lessons = uiState.lessons,
                    totalPhraseCount = uiState.allPhrases.size,
                    onPlayAll = { startListenPlayback() },
                    onStop = { stopListenPlayback() },
                    onLessonListenSelected = { lesson -> startListenPlayback(lesson.category) },
                    modifier = modifier
                )

                AppScreen.Practice -> PhrasePracticeScreen(
                    uiState = uiState,
                    onNextPhrase = viewModel::nextPhrase,
                    onPlaySampleAudio = viewModel::playSampleAudio,
                    onRecordUser = viewModel::recordUser,
                    onRevealPhrase = viewModel::revealCurrentPhrase,
                    onGradePhrase = { grade: ReviewGrade -> viewModel.gradeCurrentPhrase(grade) },
                    onSpokenTextChanged = viewModel::updateSpokenText,
                    onSpeechTranscriptCaptured = viewModel::recordSpeechTranscript,
                    onSpeechRecognitionUnavailable = viewModel::speechRecognitionUnavailable,
                    onToggleFavorite = viewModel::toggleCurrentPhraseFavorite,
                    onAiServerUrlChanged = viewModel::updateAiServerUrl,
                    onRequestAiFeedback = viewModel::requestAiFeedback,
                    onClearAiFeedback = viewModel::clearAiFeedback,
                    onViewLessons = { viewModel.navigateTo(AppScreen.Lessons) },
                    onStartRecording = viewModel::startRecording,
                    onStopRecordingAndAssess = viewModel::stopRecordingAndAssess,
                    onCancelRecording = viewModel::cancelRecording,
                    onClearPronunciation = viewModel::clearPronunciationResult,
                    onPhraseAudioChanged = viewModel::showPhraseAt,
                    modifier = modifier
                )

                AppScreen.Progress -> ProgressScreen(
                    stats = uiState.stats,
                    dailyGoal = uiState.dailyGoal,
                    phraseLibraryStats = uiState.phraseLibraryStats,
                    modifier = modifier
                )
            }
        }
    }
}

private fun AppScreen.navIcon(): String =
    when (this) {
        AppScreen.Home -> "H"
        AppScreen.Lessons -> "L"
        AppScreen.Listen -> "A"
        AppScreen.Practice -> "P"
        AppScreen.Progress -> "%"
    }
