package com.duddy.portugues.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.duddy.portugues.MainActivity
import com.duddy.portugues.data.model.Phrase
import com.duddy.portugues.data.model.PhraseCategory
import com.duddy.portugues.data.repository.LocalPhraseRepository
import java.util.Locale

class ListenPlaybackService : Service(), TextToSpeech.OnInitListener {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var textToSpeech: TextToSpeech

    private var isTextToSpeechReady = false
    private var shouldPlayWhenReady = false
    private var phrases: List<Phrase> = emptyList()
    private var currentPhraseIndex = 0
    private var isEnglishTurn = true
    private var playbackRunId = 0
    private var playlistTitle = "Listen mode"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        textToSpeech = TextToSpeech(this, this)
        textToSpeech.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    handleUtteranceFinished(utteranceId)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    handleUtteranceFinished(utteranceId)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    handleUtteranceFinished(utteranceId)
                }
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_PLAY_CATEGORY -> {
                val category = PhraseCategory.parse(intent.getStringExtra(EXTRA_CATEGORY_KEY))
                val repository = LocalPhraseRepository(applicationContext)
                val categoryPhrases = category?.let(repository::getPhrasesForCategory).orEmpty()
                startPlaylist(
                    title = category?.displayName ?: "Category",
                    items = categoryPhrases
                )
            }

            ACTION_PLAY_ALL -> {
                val repository = LocalPhraseRepository(applicationContext)
                startPlaylist(
                    title = "All categories",
                    items = repository.getPhrases()
                )
            }
        }
        return START_STICKY
    }

    override fun onInit(status: Int) {
        isTextToSpeechReady = status == TextToSpeech.SUCCESS
        if (isTextToSpeechReady && shouldPlayWhenReady) {
            shouldPlayWhenReady = false
            playCurrent()
        } else if (!isTextToSpeechReady) {
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPlayback()
        if (::textToSpeech.isInitialized) {
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }

    private fun startPlaylist(title: String, items: List<Phrase>) {
        playbackRunId += 1
        playlistTitle = title
        phrases = items
        currentPhraseIndex = 0
        isEnglishTurn = true

        startForeground(
            NOTIFICATION_ID,
            notification(
                title = playlistTitle,
                text = if (phrases.isEmpty()) "No phrases available." else "Starting audio..."
            )
        )

        if (phrases.isEmpty()) {
            stopSelf()
            return
        }

        if (isTextToSpeechReady) {
            playCurrent()
        } else {
            shouldPlayWhenReady = true
        }
    }

    private fun playCurrent() {
        if (!isTextToSpeechReady || currentPhraseIndex !in phrases.indices) {
            stopSelf()
            return
        }

        val phrase = phrases[currentPhraseIndex]
        val locale = if (isEnglishTurn) Locale.US else Locale("pt", "BR")
        val text = if (isEnglishTurn) phrase.english else phrase.portuguese
        val language = if (isEnglishTurn) "English" else "Portuguese"
        val utteranceId = "$playbackRunId:${currentPhraseIndex}:${if (isEnglishTurn) "en" else "pt"}"

        textToSpeech.language = locale
        startForeground(
            NOTIFICATION_ID,
            notification(
                title = playlistTitle,
                text = "$language ${currentPhraseIndex + 1} of ${phrases.size}"
            )
        )

        val result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            advanceAfterDelay()
        }
    }

    private fun handleUtteranceFinished(utteranceId: String?) {
        val runId = utteranceId?.substringBefore(":")?.toIntOrNull()
        if (runId != playbackRunId) return
        handler.post { advanceAfterDelay() }
    }

    private fun advanceAfterDelay() {
        if (isEnglishTurn) {
            isEnglishTurn = false
            handler.postDelayed({ playCurrent() }, BETWEEN_LANGUAGES_DELAY_MS)
        } else {
            isEnglishTurn = true
            currentPhraseIndex += 1
            handler.postDelayed({ playCurrent() }, BETWEEN_PHRASES_DELAY_MS)
        }
    }

    private fun stopPlayback() {
        shouldPlayWhenReady = false
        playbackRunId += 1
        handler.removeCallbacksAndMessages(null)
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
        }
    }

    private fun notification(title: String, text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, ListenPlaybackService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Duddy Listen",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_PLAY_CATEGORY = "com.duddy.portugues.action.PLAY_CATEGORY"
        const val ACTION_PLAY_ALL = "com.duddy.portugues.action.PLAY_ALL"
        const val ACTION_STOP = "com.duddy.portugues.action.STOP_LISTEN"
        const val EXTRA_CATEGORY_KEY = "category_key"

        private const val CHANNEL_ID = "duddy_listen_playback"
        private const val NOTIFICATION_ID = 4201
        private const val BETWEEN_LANGUAGES_DELAY_MS = 350L
        private const val BETWEEN_PHRASES_DELAY_MS = 750L
    }
}
