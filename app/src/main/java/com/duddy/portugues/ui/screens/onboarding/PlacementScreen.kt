package com.duddy.portugues.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duddy.portugues.data.model.PhraseDifficulty

private data class PlacementQuestion(
    val english: String,
    val options: List<String>,
    val correctIndex: Int,
)

private val questions = listOf(
    PlacementQuestion(
        english = "Where is the exit?",
        options = listOf("Onde fica a saida?", "Eu quero cafe.", "Tudo bem?", "Quanto custa?"),
        correctIndex = 0,
    ),
    PlacementQuestion(
        english = "I need a doctor.",
        options = listOf("Eu moro aqui.", "Preciso de um medico.", "A conta, por favor.", "Vamos amanha."),
        correctIndex = 1,
    ),
    PlacementQuestion(
        english = "Can you speak more slowly?",
        options = listOf("Voce trabalha hoje?", "Fica perto daqui?", "Pode falar mais devagar?", "Eu gostei muito."),
        correctIndex = 2,
    ),
    PlacementQuestion(
        english = "I would like to make a reservation.",
        options = listOf("Eu queria fazer uma reserva.", "Eu preciso trocar dinheiro.", "Estou com dor.", "Nao entendi."),
        correctIndex = 0,
    ),
    PlacementQuestion(
        english = "If I had more time, I would travel through Brazil.",
        options = listOf(
            "Eu chego as oito.",
            "Se eu tivesse mais tempo, viajaria pelo Brasil.",
            "Voce pode me ajudar?",
            "Esta muito caro para mim.",
        ),
        correctIndex = 1,
    ),
)

@Composable
fun PlacementScreen(
    onPlacementComplete: (PhraseDifficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    val answers = remember { mutableStateMapOf<Int, Int>() }
    val score = answers.count { (index, selectedIndex) ->
        questions[index].correctIndex == selectedIndex
    }
    val isComplete = answers.size == questions.size
    val placementLevel = placementLevelForScore(score)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Quick Placement",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Choose the Brazilian Portuguese phrase that best matches each prompt.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { answers.size / questions.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            questions.forEachIndexed { index, question ->
                QuestionCard(
                    questionNumber = index + 1,
                    question = question,
                    selectedIndex = answers[index],
                    onOptionSelected = { selectedIndex -> answers[index] = selectedIndex },
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (isComplete) {
                    "Starting level: ${placementLevel.name}"
                } else {
                    "Answer ${questions.size - answers.size} more to set your starting level."
                },
                fontWeight = FontWeight.SemiBold,
            )
            Button(
                onClick = { onPlacementComplete(placementLevel) },
                enabled = isComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun QuestionCard(
    questionNumber: Int,
    question: PlacementQuestion,
    selectedIndex: Int?,
    onOptionSelected: (Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "$questionNumber. ${question.english}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            question.options.forEachIndexed { index, option ->
                OutlinedButton(
                    onClick = { onOptionSelected(index) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Text(
                            text = if (selectedIndex == index) {
                                "Selected: $option"
                            } else {
                                option
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun placementLevelForScore(score: Int): PhraseDifficulty =
    when (score) {
        0, 1 -> PhraseDifficulty.A1
        2, 3 -> PhraseDifficulty.A2
        else -> PhraseDifficulty.B1
    }
