package com.duddy.portugues.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duddy.portugues.data.model.PhraseDifficulty

private data class GoalOption(
    val minutes: Int,
    val title: String,
    val description: String,
)

private val goalOptions = listOf(
    GoalOption(
        minutes = 5,
        title = "5 minutes",
        description = "Casual: one quick speaking round and a small review set.",
    ),
    GoalOption(
        minutes = 10,
        title = "10 minutes",
        description = "Regular: a balanced daily mix of new phrases, review, and speech.",
    ),
    GoalOption(
        minutes = 20,
        title = "20 minutes",
        description = "Intensive: more reviews, more speaking reps, and deeper coaching.",
    ),
)

@Composable
fun GoalSettingScreen(
    placementLevel: PhraseDifficulty,
    onGoalComplete: (dailyMinutes: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMinutes by remember { mutableIntStateOf(10) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Set Your Daily Goal",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Your starting level is ${placementLevel.name}. Choose a pace you can keep.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            goalOptions.forEach { option ->
                GoalOptionCard(
                    option = option,
                    selected = selectedMinutes == option.minutes,
                    onClick = { selectedMinutes = option.minutes },
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onGoalComplete(selectedMinutes) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Start Learning")
            }
        }
    }
}

@Composable
private fun GoalOptionCard(
    option: GoalOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = if (selected) {
        CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    } else {
        CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.background)
    }

    OutlinedCard(
        onClick = onClick,
        colors = colors,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (selected) "Selected: ${option.title}" else option.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            Text(
                text = option.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
