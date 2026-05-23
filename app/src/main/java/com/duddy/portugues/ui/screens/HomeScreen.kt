package com.duddy.portugues.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duddy.portugues.data.model.DailyGoalProgress
import com.duddy.portugues.data.model.GuidedSessionStep
import com.duddy.portugues.data.model.ProgressStats

@Composable
fun HomeScreen(
    phraseCount: Int,
    favoriteCount: Int,
    dueReviewCount: Int,
    dailyGoal: DailyGoalProgress,
    stats: ProgressStats,
    guidedSessionSteps: List<GuidedSessionStep>,
    statusMessage: String,
    showFirstRunTip: Boolean,
    onDismissFirstRunTip: () -> Unit,
    onStartSmartReview: () -> Unit,
    onStartGuidedSession: () -> Unit,
    onStartPractice: () -> Unit,
    onPracticeFavorites: () -> Unit,
    onViewLessons: () -> Unit,
    onStartTesting: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Duddy Português",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "A speaking-first tutor for Brazilian Portuguese.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$phraseCount phrases · $favoriteCount saved · $dueReviewCount due",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showFirstRunTip) {
            FirstRunTipCard(onDismiss = onDismissFirstRunTip)
        }

        DailyPlanCard(
            guidedSessionSteps = guidedSessionSteps,
            dueReviewCount = dueReviewCount,
            onStartGuidedSession = onStartGuidedSession
        )

        GamificationSummaryCard(stats = stats)

        DailyGoalCard(
            dailyGoal = dailyGoal,
            stats = stats,
            onStartGuidedSession = onStartGuidedSession
        )

        TestingCenterCard(onStartTesting = onStartTesting)

        Button(
            onClick = onStartSmartReview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start smart review")
        }
        Button(
            onClick = onStartPractice,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Browse all practice")
        }
        OutlinedButton(
            onClick = onPracticeFavorites,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Practice saved phrases")
        }
        OutlinedButton(
            onClick = onViewLessons,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search lessons")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TestingCenterCard(
    onStartTesting: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Testing center",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Level I: translation multiple choice. Level II: randomized listening multiple choice. Level III: pronunciation exam.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onStartTesting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Testing")
            }
        }
    }
}

@Composable
private fun FirstRunTipCard(onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "First step",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tap Start guided session to begin with recall, speaking practice, coaching, and review.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Got it")
            }
        }
    }
}

@Composable
private fun DailyGoalCard(
    dailyGoal: DailyGoalProgress,
    stats: ProgressStats,
    onStartGuidedSession: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Today's goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { dailyGoal.percentComplete / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${stats.todayXp}/${stats.dailyXpGoal} XP today · ${stats.dailyXpPercent}% XP goal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = dailyGoal.encouragementMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${dailyGoal.completedNewPhrases}/${dailyGoal.targets.newPhrasesPerDay} new · " +
                    "${dailyGoal.completedReviews}/${dailyGoal.targets.reviewsPerDay} reviews · " +
                    "${dailyGoal.completedSpeakingAttempts}/${dailyGoal.targets.speakingAttemptsPerDay} speaking",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onStartGuidedSession,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start today's session")
            }
        }
    }
}

@Composable
private fun GamificationSummaryCard(stats: ProgressStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Level ${stats.level}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { stats.levelProgressPercent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${stats.totalXp} total XP · ${stats.xpIntoCurrentLevel}/${stats.xpNeededForNextLevel} toward next level",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${stats.streakDays} day streak · longest ${stats.longestStreak} · hearts ${stats.hearts}/${stats.maxHearts}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DailyPlanCard(
    guidedSessionSteps: List<GuidedSessionStep>,
    dueReviewCount: Int,
    onStartGuidedSession: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Today's guided session",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$dueReviewCount due reviews plus a few fresh phrases.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            guidedSessionSteps.forEachIndexed { index, step ->
                Text(
                    text = "${index + 1}. ${step.title} · ${step.technique.displayName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onStartGuidedSession,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start guided session")
            }
        }
    }
}
