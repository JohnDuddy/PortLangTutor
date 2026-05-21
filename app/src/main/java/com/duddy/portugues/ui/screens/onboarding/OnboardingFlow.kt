package com.duddy.portugues.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.duddy.portugues.data.model.PhraseDifficulty

private enum class OnboardingStep {
    Intro,
    Placement,
    Goal,
}

@Composable
fun OnboardingFlow(
    onComplete: (placementLevel: PhraseDifficulty, dailyMinutes: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(OnboardingStep.Intro) }
    var placementLevel by remember { mutableStateOf(PhraseDifficulty.A1) }

    when (step) {
        OnboardingStep.Intro -> OnboardingScreen(
            onSkip = { step = OnboardingStep.Placement },
            onFinished = { step = OnboardingStep.Placement },
            modifier = modifier,
        )

        OnboardingStep.Placement -> PlacementScreen(
            onPlacementComplete = { level ->
                placementLevel = level
                step = OnboardingStep.Goal
            },
            modifier = modifier,
        )

        OnboardingStep.Goal -> GoalSettingScreen(
            placementLevel = placementLevel,
            onGoalComplete = { dailyMinutes -> onComplete(placementLevel, dailyMinutes) },
            modifier = modifier,
        )
    }
}
