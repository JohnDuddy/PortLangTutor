package com.duddy.portugues.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duddy.portugues.data.model.ReviewGrade
import com.duddy.portugues.presentation.TutorUiState
import com.duddy.portugues.ui.components.PronunciationScoreCard
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun PhrasePracticeScreen(
    uiState: TutorUiState,
    onNextPhrase: () -> Unit,
    onPlaySampleAudio: () -> Unit,
    onRecordUser: () -> Unit,
    onRevealPhrase: () -> Unit,
    onGradePhrase: (ReviewGrade) -> Unit,
    onSpokenTextChanged: (String) -> Unit,
    onSpeechTranscriptCaptured: (String) -> Unit,
    onSpeechRecognitionUnavailable: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAiServerUrlChanged: (String) -> Unit,
    onRequestAiFeedback: () -> Unit,
    onClearAiFeedback: () -> Unit,
    onViewLessons: () -> Unit,
    onStartRecording: () -> Unit = {},
    onStopRecordingAndAssess: () -> Unit = {},
    onCancelRecording: () -> Unit = {},
    onClearPronunciation: () -> Unit = {},
    onPhraseAudioChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val phrase = uiState.currentPhrase
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isTextToSpeechReady by remember { mutableStateOf(false) }
    val textToSpeechHolder = remember { mutableStateOf<TextToSpeech?>(null) }
    var categoryAudioJob by remember { mutableStateOf<Job?>(null) }
    var isCategoryAudioPlaying by remember { mutableStateOf(false) }
    var categoryAudioPhraseIndex by remember { mutableStateOf(0) }
    var handledCategoryAudioToken by remember { mutableStateOf(0) }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            isTextToSpeechReady = status == TextToSpeech.SUCCESS
            if (status == TextToSpeech.SUCCESS) {
                textToSpeechHolder.value?.language = Locale("pt", "BR")
            }
        }
        textToSpeechHolder.value = textToSpeech

        onDispose {
            categoryAudioJob?.cancel()
            textToSpeech.stop()
            textToSpeech.shutdown()
            textToSpeechHolder.value = null
        }
    }

    fun stopCategoryAudio() {
        categoryAudioJob?.cancel()
        categoryAudioJob = null
        isCategoryAudioPlaying = false
        textToSpeechHolder.value?.stop()
    }

    fun startCategoryAudio() {
        val textToSpeech = textToSpeechHolder.value ?: return
        val phrasesToPlay = uiState.activePhrases
        if (!isTextToSpeechReady || phrasesToPlay.isEmpty()) return

        stopCategoryAudio()
        categoryAudioJob = coroutineScope.launch {
            isCategoryAudioPlaying = true
            try {
                phrasesToPlay.forEachIndexed { index, item ->
                    categoryAudioPhraseIndex = index
                    onPhraseAudioChanged(index)
                    textToSpeech.speakAndAwait(
                        text = item.english,
                        locale = Locale.US,
                        utteranceId = "listen-en-${item.id}"
                    )
                    delay(350)
                    textToSpeech.speakAndAwait(
                        text = item.portuguese,
                        locale = Locale("pt", "BR"),
                        utteranceId = "listen-pt-${item.id}"
                    )
                    delay(700)
                }
            } finally {
                isCategoryAudioPlaying = false
                categoryAudioJob = null
                textToSpeech.stop()
            }
        }
    }

    LaunchedEffect(uiState.categoryAudioRequestToken, isTextToSpeechReady) {
        if (
            uiState.categoryAudioRequestToken > 0 &&
            uiState.categoryAudioRequestToken != handledCategoryAudioToken &&
            isTextToSpeechReady
        ) {
            handledCategoryAudioToken = uiState.categoryAudioRequestToken
            startCategoryAudio()
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val transcript = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            onSpeechTranscriptCaptured(transcript)
        } else {
            onSpeechTranscriptCaptured("")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (uiState.isSmartReviewSession) {
                "Smart Review"
            } else {
                uiState.activeLesson?.title ?: "Phrase Practice"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (phrase == null) {
            Text("No phrases are available yet.")
            Button(onClick = onViewLessons) {
                Text("Choose a lesson")
            }
            return@Column
        }

        Text(
            text = "Card ${uiState.currentPhraseIndex + 1} of ${uiState.activePhrases.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        TechniqueCueCard(
            title = uiState.currentTechnique.displayName,
            cue = uiState.currentTechnique.learnerCue
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = phrase.category.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = phrase.english,
                    style = MaterialTheme.typography.titleMedium
                )
                if (uiState.isAnswerRevealed) {
                    Text(
                        text = phrase.portuguese,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pronunciation: ${phrase.pronunciationGuide}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Recall the Portuguese from memory before revealing the answer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        CategoryAudioControls(
            title = uiState.activeLesson?.title ?: "Current phrase set",
            phraseCount = uiState.activePhrases.size,
            currentIndex = categoryAudioPhraseIndex,
            isPlaying = isCategoryAudioPlaying,
            isReady = isTextToSpeechReady,
            onPlay = { startCategoryAudio() },
            onStop = { stopCategoryAudio() }
        )

        OutlinedButton(
            onClick = onToggleFavorite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (phrase.id in uiState.favoritePhraseIds) "Saved phrase" else "Save phrase")
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    stopCategoryAudio()
                    if (isTextToSpeechReady) {
                        textToSpeechHolder.value?.run {
                            language = Locale("pt", "BR")
                            speak(
                                phrase.portuguese,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                phrase.id
                            )
                        }
                    }
                    onPlaySampleAudio()
                },
                enabled = uiState.isAnswerRevealed,
                modifier = Modifier.weight(1f)
            ) {
                Text("Play sample audio")
            }
            OutlinedButton(
                onClick = {
                    onRecordUser()
                    val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the phrase in Portuguese")
                    }

                    try {
                        speechLauncher.launch(recognizerIntent)
                    } catch (_: ActivityNotFoundException) {
                        onSpeechRecognitionUnavailable()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Record me")
            }
        }

        if (!uiState.isAnswerRevealed) {
            Button(
                onClick = onRevealPhrase,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reveal phrase")
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "How well did you recall it?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { onGradePhrase(ReviewGrade.Again) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Again")
                    }
                    OutlinedButton(
                        onClick = { onGradePhrase(ReviewGrade.Hard) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hard")
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onGradePhrase(ReviewGrade.Good) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Good")
                    }
                    Button(
                        onClick = { onGradePhrase(ReviewGrade.Easy) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Easy")
                    }
                }
            }
        }

        OutlinedTextField(
            value = uiState.spokenText,
            onValueChange = onSpokenTextChanged,
            label = { Text("What you said") },
            placeholder = { Text("Speech transcript appears here") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        // ── Live pronunciation scoring (Azure via backend) ──────────────────
        Text(
            "🎤 Live pronunciation scoring",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "Tap Record, say the phrase, then tap Stop. Azure scores each phoneme.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!uiState.isRecording) {
                Button(
                    onClick = onStartRecording,
                    enabled = !uiState.isAssessing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isAssessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text("Scoring…")
                    } else {
                        Text("● Record")
                    }
                }
            } else {
                Button(
                    onClick = onStopRecordingAndAssess,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("■ Stop & score")
                }
                OutlinedButton(
                    onClick = onCancelRecording,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }
            OutlinedButton(
                onClick = onClearPronunciation,
                enabled = uiState.pronunciationResult != null || uiState.pronunciationError != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
        }

        uiState.pronunciationError?.let { err ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    err,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        uiState.pronunciationResult?.let { result ->
            PronunciationScoreCard(result = result)
        }

        OutlinedTextField(
            value = uiState.aiServerUrl,
            onValueChange = onAiServerUrlChanged,
            label = { Text("AI server URL") },
            placeholder = { Text("http://127.0.0.1:8010/v1/coach") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onRequestAiFeedback,
                enabled = !uiState.isAiLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isAiLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
                Text("Ask AI coach")
            }
            OutlinedButton(
                onClick = onClearAiFeedback,
                enabled = uiState.aiFeedback.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
        }

        if (uiState.aiFeedback.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI coach",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = uiState.aiFeedback,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onNextPhrase,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next phrase")
        }

        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onViewLessons) {
            Text("Back to lessons")
        }
    }
}

@Composable
private fun TechniqueCueCard(
    title: String,
    cue: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = cue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryAudioControls(
    title: String,
    phraseCount: Int,
    currentIndex: Int,
    isPlaying: Boolean,
    isReady: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit
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
                text = "Category audio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isPlaying) {
                    "$title is playing ${currentIndex + 1} of $phraseCount: English, then Portuguese."
                } else {
                    "Play this set hands-free: English phrase, then Brazilian Portuguese."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onPlay,
                    enabled = isReady && phraseCount > 0 && !isPlaying,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Play all")
                }
                OutlinedButton(
                    onClick = onStop,
                    enabled = isPlaying,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop")
                }
            }
        }
    }
}

private suspend fun TextToSpeech.speakAndAwait(
    text: String,
    locale: Locale,
    utteranceId: String
) {
    suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean(false)

        fun finish() {
            if (completed.compareAndSet(false, true) && continuation.isActive) {
                continuation.resume(Unit)
            }
        }

        setLanguage(locale)
        setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    finish()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    finish()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    finish()
                }
            }
        )

        val result = speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            finish()
        }

        continuation.invokeOnCancellation {
            if (completed.compareAndSet(false, true)) {
                stop()
            }
        }
    }
}
