package com.duddy.portugues.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duddy.portugues.data.model.DailyGoalProgress
import com.duddy.portugues.data.model.PhraseLibraryStats
import com.duddy.portugues.data.model.ProgressStats

@Composable
fun ProgressScreen(
    stats: ProgressStats,
    dailyGoal: DailyGoalProgress,
    phraseLibraryStats: PhraseLibraryStats,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Local stats saved on this device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
            )
        }

        item {
            DailyGoalProgressCard(dailyGoal = dailyGoal)
        }

        item {
            XpProgressCard(stats = stats)
        }

        item {
            ProgressCard(label = "Local phrase library", value = phraseLibraryStats.phraseCount.toString())
        }

        item {
            ProgressCard(label = "Lessons started", value = stats.lessonsStarted.toString())
        }
        item {
            ProgressCard(label = "Practiced phrases", value = stats.practicedPhrases.toString())
        }
        item {
            ProgressCard(label = "Sample audio plays", value = stats.sampleAudioPlays.toString())
        }
        item {
            ProgressCard(label = "Speaking attempts", value = stats.speakingAttempts.toString())
        }
        item {
            ProgressCard(label = "AI coach requests", value = stats.aiCoachRequests.toString())
        }
        item {
            ProgressCard(label = "Day streak", value = stats.streakDays.toString())
        }
        item {
            ProgressCard(label = "Longest streak", value = stats.longestStreak.toString())
        }
        item {
            ProgressCard(label = "Hearts", value = "${stats.hearts}/${stats.maxHearts}")
        }
        item {
            ProgressCard(label = "League", value = stats.leagueName)
        }
    }
}

@Composable
private fun XpProgressCard(stats: ProgressStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Level ${stats.level}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { stats.levelProgressPercent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${stats.totalXp} XP total · ${stats.xpIntoCurrentLevel}/${stats.xpNeededForNextLevel} XP to next level",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { stats.dailyXpPercent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Daily XP goal: ${stats.todayXp}/${stats.dailyXpGoal}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DailyGoalProgressCard(
    dailyGoal: DailyGoalProgress
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Today's goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { dailyGoal.percentComplete / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${dailyGoal.percentComplete}% complete",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${dailyGoal.completedPracticeMinutes}/${dailyGoal.targets.practiceMinutesPerDay} min · " +
                    "${dailyGoal.completedSpeakingAttempts}/${dailyGoal.targets.speakingAttemptsPerDay} speaking · " +
                    "${dailyGoal.completedAiCoachRequests}/${dailyGoal.targets.aiCoachRequestsPerDay} coach",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dailyGoal.encouragementMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgressCard(
    label: String,
    value: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
