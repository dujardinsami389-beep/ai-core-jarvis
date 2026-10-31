package fr.aicore.jarvis

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.speech.*
import androidx.annotation.RequiresApi

/** User-initiated foreground listening. Android speech providers may stop or require network. */
class WakeWordService : Service(), RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private val channel = "ai_core_listening"
    private val restart = Runnable { listen() }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(channel, "Écoute de Jarvis", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopListening(); stopSelf(); return START_NOT_STICKY }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return START_NOT_STICKY
        }
        val open = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 2, Intent(this, WakeWordService::class.java).setAction("STOP"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val note = Notification.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("AI CORE · Jarvis à l'écoute")
            .setContentText("Microphone actif · Touchez pour ouvrir AI CORE")
            .setContentIntent(open).addAction(Notification.Action.Builder(null, "Arrêter", stop).build())
            .setOngoing(true).build()
        startForeground(101, note)
        active = true
        listen()
        return START_NOT_STICKY
    }

    private fun listen() {
        if (!active || !SpeechRecognizer.isRecognitionAvailable(this)) return
        handler.removeCallbacks(restart)
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
        val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try { recognizer?.startListening(request) } catch (_: Exception) { schedule() }
    }
    private fun schedule() { if (active) { handler.removeCallbacks(restart); handler.postDelayed(restart, 1500) } }
    private fun found(bundle: Bundle?) {
        val phrase = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        if (Regex("\\bjarvis\\b", RegexOption.IGNORE_CASE).containsMatchIn(phrase)) {
            val i = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("wake_command", phrase)
            }
            val pending = PendingIntent.getActivity(this, 3, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = Notification.Builder(this, channel).setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Jarvis a entendu son nom").setContentText("Touchez pour ouvrir votre assistant")
                .setContentIntent(pending).setAutoCancel(true).build()
            getSystemService(NotificationManager::class.java).notify(102, notification)
        }
    }
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onError(error: Int) { schedule() }
    override fun onResults(results: Bundle?) { found(results); schedule() }
    override fun onPartialResults(partialResults: Bundle?) { found(partialResults) }
    override fun onEvent(eventType: Int, params: Bundle?) {}
    private fun stopListening() { active = false; handler.removeCallbacksAndMessages(null); recognizer?.cancel(); recognizer?.destroy(); recognizer = null; stopForeground(STOP_FOREGROUND_REMOVE) }
    override fun onDestroy() { stopListening(); super.onDestroy() }
    override fun onBind(intent: Intent?) : IBinder? = null
}
