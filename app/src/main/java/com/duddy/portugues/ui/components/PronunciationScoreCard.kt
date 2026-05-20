package com.duddy.portugues.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duddy.portugues.data.model.PhonemeErrorType
import com.duddy.portugues.data.model.PronunciationResult
import com.duddy.portugues.data.model.WordAssessment

/**
 * Renders the user's pronunciation score breakdown:
 *  - Big overall number
 *  - Sub-score bars (accuracy / fluency / completeness / prosody)
 *  - Word-level colour map showing where they went wrong
 *  - Phoneme-level diagnostic for weakest sounds
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PronunciationScoreCard(
    result: PronunciationResult,
    modifier: Modifier = Modifier,
) {
    val overall = result.overall.pronunciation ?: 0.0
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {

            // ── Big overall number ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = "%.0f".format(overall),
                    fontSize   = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color      = scoreColor(overall),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Pronunciation", style = MaterialTheme.typography.titleSmall)
                    Text(
                        scoreLabel(overall),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Sub-score bars ──
            ScoreBar("Accuracy",     result.overall.accuracy)
            ScoreBar("Fluency",      result.overall.fluency)
            ScoreBar("Completeness", result.overall.completeness)
            result.overall.prosody?.let { ScoreBar("Prosody (rhythm)", it) }

            Spacer(Modifier.height(12.dp))

            // ── Recognised text ──
            Text(
                "You said:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                result.recognizedText.ifBlank { "(unclear)" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            // ── Word-level breakdown ──
            if (result.words.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Word breakdown:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                FlowWords(result.words)
            }

            // ── Weakest phonemes ──
            if (result.phonemesLow.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Focus on these sounds:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    result.phonemesLow.take(5).forEach { ph ->
                        PhonemeChip(ph.phoneme, ph.score ?: 0.0)
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, value: Double?) {
    val v = value ?: 0.0
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Text(label, modifier = Modifier.width(140.dp), style = MaterialTheme.typography.labelMedium)
        LinearProgressIndicator(
            progress = { (v / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(8.dp),
            color = scoreColor(v),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text("%.0f".format(v), modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowWords(words: List<WordAssessment>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement   = Arrangement.spacedBy(4.dp),
    ) {
        words.forEach { w ->
            val color = when (w.errorType) {
                PhonemeErrorType.None             -> Color(0xFF1A6B35)
                PhonemeErrorType.Mispronunciation -> Color(0xFFE07B00)
                PhonemeErrorType.Omission         -> Color(0xFFC0392B)
                PhonemeErrorType.Insertion        -> Color(0xFFC0392B)
                else                              -> Color(0xFF6B7A8D)
            }
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
                    .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    "${w.word} ${w.accuracy?.let { "%.0f".format(it) } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun PhonemeChip(phoneme: String, score: Double) {
    Box(
        modifier = Modifier
            .background(scoreColor(score).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            "$phoneme  ${"%.0f".format(score)}",
            style = MaterialTheme.typography.labelSmall,
            color = scoreColor(score),
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun scoreColor(score: Double): Color = when {
    score >= 85 -> Color(0xFF1A6B35)
    score >= 70 -> Color(0xFF4A90E2)
    score >= 50 -> Color(0xFFE07B00)
    else        -> Color(0xFFC0392B)
}

private fun scoreLabel(score: Double): String = when {
    score >= 90 -> "Excellent — native-like"
    score >= 80 -> "Strong"
    score >= 70 -> "Good"
    score >= 55 -> "Understandable"
    score >= 40 -> "Practice needed"
    else        -> "Try again slowly"
}
