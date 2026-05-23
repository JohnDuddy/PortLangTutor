package com.duddy.portugues

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.duddy.portugues.data.model.PhraseDifficulty
import com.duddy.portugues.ui.screens.onboarding.GoalSettingScreen
import com.duddy.portugues.ui.screens.onboarding.OnboardingScreen
import com.duddy.portugues.ui.screens.onboarding.PlacementScreen
import com.duddy.portugues.ui.theme.DuddyPortuguesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingScreen_showsFourPages() {
        composeRule.setContent {
            DuddyPortuguesTheme {
                OnboardingScreen(onSkip = {}, onFinished = {})
            }
        }

        composeRule.onNodeWithText("Speak from Day One").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("AI-Powered Coaching").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Pronunciation Scoring").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Smart Spaced Repetition").assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_skipNavigatesToPlacement() {
        var skipped = false
        composeRule.setContent {
            DuddyPortuguesTheme {
                OnboardingScreen(onSkip = { skipped = true }, onFinished = {})
            }
        }

        composeRule.onNodeWithText("Skip").performClick()

        composeRule.runOnIdle {
            assertTrue(skipped)
        }
    }

    @Test
    fun placementScreen_correctAnswersSetB1() {
        var level: PhraseDifficulty? = null
        composeRule.setContent {
            DuddyPortuguesTheme {
                PlacementScreen(onPlacementComplete = { level = it })
            }
        }

        composeRule.onNodeWithText("Onde fica a saida?").performClick()
        composeRule.onNodeWithText("Preciso de um medico.").performClick()
        composeRule.onNodeWithText("Pode falar mais devagar?").performClick()
        composeRule.onNodeWithText("Eu queria fazer uma reserva.").performClick()
        composeRule.onNodeWithText("Se eu tivesse mais tempo, viajaria pelo Brasil.").performClick()
        composeRule.onNodeWithText("Continue").performClick()

        composeRule.runOnIdle {
            assertEquals(PhraseDifficulty.B1, level)
        }
    }

    @Test
    fun placementScreen_noCorrectAnswersSetsA1() {
        var level: PhraseDifficulty? = null
        composeRule.setContent {
            DuddyPortuguesTheme {
                PlacementScreen(onPlacementComplete = { level = it })
            }
        }

        composeRule.onNodeWithText("Eu quero cafe.").performClick()
        composeRule.onNodeWithText("Eu moro aqui.").performClick()
        composeRule.onNodeWithText("Voce trabalha hoje?").performClick()
        composeRule.onNodeWithText("Eu preciso trocar dinheiro.").performClick()
        composeRule.onNodeWithText("Eu chego as oito.").performClick()
        composeRule.onNodeWithText("Continue").performClick()

        composeRule.runOnIdle {
            assertEquals(PhraseDifficulty.A1, level)
        }
    }

    @Test
    fun goalSettingScreen_defaultIsRegular() {
        var selectedMinutes = 0
        composeRule.setContent {
            DuddyPortuguesTheme {
                GoalSettingScreen(
                    placementLevel = PhraseDifficulty.A1,
                    onGoalComplete = { selectedMinutes = it },
                )
            }
        }

        composeRule.onNodeWithText("Selected: 10 minutes").assertIsDisplayed()
        composeRule.onNodeWithText("Start Learning").performClick()

        composeRule.runOnIdle {
            assertEquals(10, selectedMinutes)
        }
    }
}
