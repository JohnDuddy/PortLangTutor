package com.duddy.portugues.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duddy.portugues.data.model.TestingAnswerResult
import com.duddy.portugues.data.model.TestingLevel
import com.duddy.portugues.data.model.TestingQuestion
import com.duddy.portugues.data.model.TestingQuestionType
import com.duddy.portugues.presentation.TutorUiState
import com.duddy.portugues.ui.components.PronunciationScoreCard
import java.util.Locale

@Composable
fun TestingScreen(
    uiState: TutorUiState,
    onStartLevel: (TestingLevel) -> Unit,
    onChoiceSelected: (String) -> Unit,
    onFillBlankChanged: (String) -> Unit,
    onSubmitFillBlank: () -> Unit,
    onNextQuestion: () -> Unit,
    onRestartLevel: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecordingAndAssess: () -> Unit,
    onCancelRecording: () -> Unit,
    onClearPronunciation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val testing = uiState.testing
    val question = testing.currentQuestion
    val context = LocalContext.current
    var isTextToSpeechReady by remember { mutableStateOf(false) }
    val textToSpeechHolder = remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            isTextToSpeechReady = status == TextToSpeech.SUCCESS
        }
        textToSpeechHolder.value = textToSpeech

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
            textToSpeechHolder.value = null
        }
    }

    fun speakTestingText(
        text: String,
        localeTag: String,
        utteranceId: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
    ) {
        if (!isTextToSpeechReady || text.isBlank()) return
        val textToSpeech = textToSpeechHolder.value ?: return
        textToSpeech.language = Locale.forLanguageTag(localeTag.ifBlank { ENGLISH_TTS_LOCALE })
        textToSpeech.speak(text, queueMode, null, utteranceId)
    }

    LaunchedEffect(question?.id, isTextToSpeechReady) {
        val activeQuestion = question ?: return@LaunchedEffect
        if (activeQuestion.spokenPrompt.isNotBlank()) {
            speakTestingText(
                text = activeQuestion.spokenPrompt,
                localeTag = activeQuestion.spokenPromptLocaleTag,
                utteranceId = "testing-prompt-${activeQuestion.id}",
            )
        }
    }

    LaunchedEffect(question?.id, testing.lastResult, isTextToSpeechReady) {
        val activeQuestion = question ?: return@LaunchedEffect
        val result = testing.lastResult ?: return@LaunchedEffect
        if (result.isCorrect && activeQuestion.spokenAnswer.isNotBlank()) {
            speakTestingText(
                text = activeQuestion.spokenAnswer,
                localeTag = activeQuestion.spokenAnswerLocaleTag,
                utteranceId = "testing-answer-${activeQuestion.id}",
            )
        } else if (!result.isCorrect && result.spokenFeedback.isNotBlank()) {
            speakTestingText(
                text = result.spokenFeedback,
                localeTag = result.spokenFeedbackLocaleTag,
                utteranceId = "testing-wrong-choice-${activeQuestion.id}",
            )
            speakTestingText(
                text = result.spokenFeedbackTranslation,
                localeTag = result.spokenFeedbackTranslationLocaleTag,
                utteranceId = "testing-wrong-translation-${activeQuestion.id}",
                queueMode = TextToSpeech.QUEUE_ADD,
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Testing",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Prove recognition first, then listening comprehension, then pronunciation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (testing.selectedLevel == null) {
            LevelPicker(onStartLevel = onStartLevel)
            return@Column
        }

        if (testing.completed) {
            TestCompleteCard(
                level = testing.selectedLevel,
                correctCount = testing.correctCount,
                totalCount = testing.totalCount,
                percentScore = testing.percentScore,
                onRestartLevel = onRestartLevel,
            )
            LevelPicker(onStartLevel = onStartLevel)
            return@Column
        }

        if (question == null) {
            Text("No test questions are available yet.")
            LevelPicker(onStartLevel = onStartLevel)
            return@Column
        }

        ActiveTestHeader(
            level = testing.selectedLevel,
            sectionTitle = question.sectionTitle,
            currentIndex = testing.currentIndex,
            totalCount = testing.totalCount,
            correctCount = testing.correctCount,
        )

        when (question.type) {
            TestingQuestionType.MultipleChoice -> MultipleChoiceQuestion(
                question = question,
                selectedChoice = testing.selectedChoice,
                answered = testing.isCurrentAnswered,
                result = testing.lastResult,
                isSpeechReady = isTextToSpeechReady,
                onChoiceSelected = onChoiceSelected,
                onReplayPrompt = {
                    speakTestingText(
                        text = question.spokenPrompt,
                        localeTag = question.spokenPromptLocaleTag,
                        utteranceId = "testing-replay-${question.id}",
                    )
                },
                onReplayAnswer = {
                    speakTestingText(
                        text = question.spokenAnswer,
                        localeTag = question.spokenAnswerLocaleTag,
                        utteranceId = "testing-answer-replay-${question.id}",
                    )
                },
            )

            TestingQuestionType.FillBlank -> FillBlankQuestion(
                question = question,
                answer = testing.fillBlankAnswer,
                answered = testing.isCurrentAnswered,
                result = testing.lastResult,
                onAnswerChanged = onFillBlankChanged,
                onSubmit = onSubmitFillBlank,
            )

            TestingQuestionType.Pronunciation -> PronunciationQuestion(
                uiState = uiState,
                question = question,
                answered = testing.isCurrentAnswered,
                result = testing.lastResult,
                onStartRecording = onStartRecording,
                onStopRecordingAndAssess = onStopRecordingAndAssess,
                onCancelRecording = onCancelRecording,
                onClearPronunciation = onClearPronunciation,
            )
        }

        Button(
            onClick = onNextQuestion,
            enabled = testing.isCurrentAnswered,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (testing.currentIndex == testing.questions.lastIndex) "Finish test" else "Next question")
        }

        OutlinedButton(
            onClick = onRestartLevel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Restart ${testing.selectedLevel.title}")
        }
    }
}

@Composable
private fun LevelPicker(onStartLevel: (TestingLevel) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TestingLevel.entries.forEach { level ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = level.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = level.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onStartLevel(level) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Start ${level.title}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveTestHeader(
    level: TestingLevel,
    sectionTitle: String,
    currentIndex: Int,
    totalCount: Int,
    correctCount: Int,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "${level.title}: question ${currentIndex + 1} of $totalCount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (sectionTitle.isNotBlank()) {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LinearProgressIndicator(
                progress = { (currentIndex + 1) / totalCount.coerceAtLeast(1).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Score so far: $correctCount correct",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MultipleChoiceQuestion(
    question: TestingQuestion,
    selectedChoice: String?,
    answered: Boolean,
    result: TestingAnswerResult?,
    isSpeechReady: Boolean,
    onChoiceSelected: (String) -> Unit,
    onReplayPrompt: () -> Unit,
    onReplayAnswer: () -> Unit,
) {
    QuestionCard(question = question) {
        if (question.spokenPrompt.isNotBlank()) {
            OutlinedButton(
                onClick = onReplayPrompt,
                enabled = isSpeechReady,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isSpeechReady) "Replay spoken phrase" else "Preparing audio")
            }
        }

        question.choices.forEach { choice ->
            val selected = selectedChoice == choice
            val isCorrectChoice = answered && choice == question.expectedAnswer
            val buttonText = when {
                isCorrectChoice -> "Correct: $choice"
                selected -> "Selected: $choice"
                else -> choice
            }
            if (isCorrectChoice) {
                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(buttonText)
                }
            } else {
                OutlinedButton(
                    onClick = { onChoiceSelected(choice) },
                    enabled = !answered,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(buttonText)
                }
            }
        }
        if (answered && result?.isCorrect == true && question.spokenAnswer.isNotBlank()) {
            OutlinedButton(
                onClick = onReplayAnswer,
                enabled = isSpeechReady,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isSpeechReady) "Replay correct answer" else "Preparing audio")
            }
        }
        ResultMessage(result = result)
    }
}

@Composable
private fun FillBlankQuestion(
    question: TestingQuestion,
    answer: String,
    answered: Boolean,
    result: TestingAnswerResult?,
    onAnswerChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    QuestionCard(question = question) {
        Text(
            text = question.blankPrompt,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChanged,
            enabled = !answered,
            label = { Text("Missing word") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onSubmit,
            enabled = answer.isNotBlank() && !answered,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Submit answer")
        }
        ResultMessage(result = result)
    }
}

@Composable
private fun PronunciationQuestion(
    uiState: TutorUiState,
    question: TestingQuestion,
    answered: Boolean,
    result: TestingAnswerResult?,
    onStartRecording: () -> Unit,
    onStopRecordingAndAssess: () -> Unit,
    onCancelRecording: () -> Unit,
    onClearPronunciation: () -> Unit,
) {
    QuestionCard(question = question) {
        Text(
            text = question.phrase.portuguese,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Pronunciation: ${question.phrase.pronunciationGuide}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Passing target: 75/100 or better.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!uiState.isRecording) {
                Button(
                    onClick = onStartRecording,
                    enabled = !uiState.isAssessing && !answered,
                    modifier = Modifier.weight(1f),
                ) {
                    if (uiState.isAssessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Text("Scoring")
                    } else {
                        Text("Record")
                    }
                }
            } else {
                Button(
                    onClick = onStopRecordingAndAssess,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Stop and score")
                }
                OutlinedButton(
                    onClick = onCancelRecording,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
            }
            OutlinedButton(
                onClick = onClearPronunciation,
                enabled = uiState.pronunciationResult != null || uiState.pronunciationError != null,
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear")
            }
        }

        uiState.pronunciationError?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        uiState.pronunciationResult?.let { pronunciation ->
            PronunciationScoreCard(result = pronunciation)
        }
        ResultMessage(result = result)
    }
}

@Composable
private fun QuestionCard(
    question: TestingQuestion,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = question.sectionTitle.ifBlank { question.type.label() },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun ResultMessage(result: TestingAnswerResult?) {
    result ?: return
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (result.isCorrect) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = when {
                    result.isCorrect -> "Passed"
                    result.message == "Try again" -> "Try again"
                    else -> "Review"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(result.message)
            result.pronunciationScore?.let { score ->
                Text("Pronunciation score: $score/100")
            }
        }
    }
}

@Composable
private fun TestCompleteCard(
    level: TestingLevel,
    correctCount: Int,
    totalCount: Int,
    percentScore: Int,
    onRestartLevel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "${level.title} complete",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$correctCount of $totalCount correct ($percentScore%).",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (percentScore >= 80) {
                    "Strong result. You are ready to move up or keep reviewing weak phrases."
                } else {
                    "Useful diagnostic. Repeat this level after a short review session."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRestartLevel,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Retake ${level.title}")
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

private fun TestingQuestionType.label(): String =
    when (this) {
        TestingQuestionType.MultipleChoice -> "Multiple choice"
        TestingQuestionType.FillBlank -> "Fill in the blank"
        TestingQuestionType.Pronunciation -> "Pronunciation exam"
    }

private const val ENGLISH_TTS_LOCALE = "en-US"
