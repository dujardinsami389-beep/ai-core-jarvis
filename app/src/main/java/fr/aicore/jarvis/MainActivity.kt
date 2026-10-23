package fr.aicore.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val permissionRequest = 42
    private val speechRequest = 43
    private val modelRequest = 44
    private val mediaRequest = 45
    private val worker = Executors.newSingleThreadExecutor()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private val modelFile by lazy { File(filesDir, "ai-core-gemma3-1b-int4.litertlm") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 17, 31)
        window.navigationBarColor = Color.rgb(7, 17, 31)

        web = WebView(this)
        web.setBackgroundColor(Color.rgb(7, 17, 31))
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = false
        web.addJavascriptInterface(SpeechBridge(), "AndroidSpeech")
        web.addJavascriptInterface(AICoreBridge(), "AndroidAI")
        web.addJavascriptInterface(MediaBridge(), "AndroidMedia")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "file" || uri.scheme == "about") return false
                return openExternal(uri)
            }
        }
        web.webChromeClient = WebChromeClient()
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    private fun openExternal(uri: Uri): Boolean {
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun sendToPage(script: String) {
        if (::web.isInitialized) runOnUiThread { web.evaluateJavascript(script, null) }
    }

    private fun modelStatus(message: String) =
        sendToPage("window.__aiCoreModelStatus && window.__aiCoreModelStatus(${JSONObject.quote(message)});")

    private inner class AICoreBridge {
        @JavascriptInterface fun hasModel(): Boolean = modelFile.exists() && modelFile.length() > 400L * 1024 * 1024

        @JavascriptInterface fun openModelPage() {
            runOnUiThread {
                openExternal(Uri.parse("https://huggingface.co/litert-community/Gemma3-1B-IT"))
            }
        }

        @JavascriptInterface fun chooseModel() {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_TITLE, "Choisir gemma3-1b-it-int4.litertlm")
                }
                startActivityForResult(intent, modelRequest)
            }
        }

        @JavascriptInterface fun ask(prompt: String) {
            val cleaned = prompt.trim().take(5000)
            if (cleaned.isEmpty()) return
            worker.execute {
                try {
                    if (conversation == null) initializeModel()
                    modelStatus("L’IA réfléchit…")
                    val answer = conversation!!.sendMessage(cleaned).toString()
                    sendToPage("window.__aiCoreAnswer && window.__aiCoreAnswer(${JSONObject.quote(answer)});")
                    modelStatus("IA hors ligne prête")
                } catch (e: Exception) {
                    modelStatus("Erreur : ${e.message ?: "impossible de lancer le modèle"}")
                    sendToPage("window.__aiCoreAnswer && window.__aiCoreAnswer(${JSONObject.quote("Je n’ai pas pu lancer le modèle. Vérifie que tu as choisi le fichier Gemma LiteRT prévu pour Android.")});")
                }
            }
        }

        @JavascriptInterface fun newConversation() {
            worker.execute {
                try {
                    conversation?.close()
                    conversation = engine?.createConversation()
                    modelStatus("Nouvelle conversation prête")
                } catch (_: Exception) {
                    try { engine?.close() } catch (_: Exception) {}
                    engine = null
                    conversation = null
                    modelStatus("Nouvelle conversation prête")
                }
            }
        }
    }

    private fun initializeModel() {
        if (!modelFile.exists()) throw IllegalStateException("Télécharge puis importe le modèle Gemma")
        modelStatus("Chargement du modèle sur ce téléphone…")
        val newEngine = Engine(EngineConfig(modelPath = modelFile.absolutePath))
        newEngine.initialize()
        engine = newEngine
        conversation = newEngine.createConversation()
    }

    private inner class MediaBridge {
        @JavascriptInterface fun chooseVideo() {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "video/*"
                    putExtra(Intent.EXTRA_TITLE, "Choisir une vidéo pour AI CORE")
                }
                startActivityForResult(intent, mediaRequest)
            }
        }

        @JavascriptInterface fun listVideos(): String = videoLibraryJson().toString()

        @JavascriptInterface fun deleteVideo(id: String) {
            worker.execute {
                val safeId = id.take(80)
                val dir = File(filesDir, "ai-core-videos")
                val target = dir.listFiles()?.firstOrNull { it.isFile && it.nameWithoutExtension == safeId }
                val removed = target?.delete() == true
                if (removed) getSharedPreferences("ai-core-videos", MODE_PRIVATE).edit().remove(safeId).apply()
                sendToPage("window.__aiCoreVideosChanged && window.__aiCoreVideosChanged();")
                if (!removed) mediaStatus("Impossible de supprimer cette vidéo.")
            }
        }
    }

    private fun videoLibraryJson(): org.json.JSONArray {
        val items = org.json.JSONArray()
        val dir = File(filesDir, "ai-core-videos")
        dir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") }?.sortedByDescending { it.lastModified() }?.forEach { file ->
            val id = file.nameWithoutExtension
            val title = getSharedPreferences("ai-core-videos", MODE_PRIVATE).getString(id, id) ?: id
            items.put(JSONObject().put("id", id).put("title", title).put("uri", Uri.fromFile(file).toString()).put("bytes", file.length()))
        }
        return items
    }

    private fun mediaStatus(message: String) =
        sendToPage("window.__aiCoreMediaStatus && window.__aiCoreMediaStatus(${JSONObject.quote(message)});")

    private fun importVideo(uri: Uri) {
        worker.execute {
            val dir = File(filesDir, "ai-core-videos")
            val name = displayName(uri).orEmpty().ifBlank { "Ma vidéo.mp4" }
            val extension = name.substringAfterLast('.', "mp4").lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }.take(8).ifBlank { "mp4" }
            val id = java.util.UUID.randomUUID().toString()
            val temp = File(dir, "$id.tmp")
            val target = File(dir, "$id.$extension")
            try {
                if (!dir.exists() && !dir.mkdirs()) throw java.io.IOException("Impossible de créer la vidéothèque.")
                if (dir.usableSpace < 100L * 1024 * 1024) throw java.io.IOException("Il faut garder au moins 100 Mo libres sur l’appareil.")
                mediaStatus("Import de la vidéo en cours…")
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "La vidéo n’est pas accessible." }
                    temp.outputStream().buffered().use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        var copied = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (dir.usableSpace < count + 50L * 1024 * 1024) throw java.io.IOException("Espace insuffisant pour copier cette vidéo.")
                            output.write(buffer, 0, count)
                            copied += count
                        }
                        if (copied == 0L) throw java.io.IOException("Le fichier vidéo est vide.")
                    }
                }
                if (!temp.renameTo(target)) throw java.io.IOException("Impossible d’enregistrer la vidéo.")
                getSharedPreferences("ai-core-videos", MODE_PRIVATE).edit().putString(id, name).apply()
                val payload = videoLibraryJson().toString()
                sendToPage("window.__aiCoreVideoImported && window.__aiCoreVideoImported(${JSONObject.quote(payload)});")
                mediaStatus("Vidéo ajoutée à ta vidéothèque.")
            } catch (e: Exception) {
                temp.delete()
                mediaStatus("Import impossible : ${e.message ?: "réessaie avec une autre vidéo"}")
            }
        }
    }

    private inner class SpeechBridge {
        @JavascriptInterface fun start() {
            runOnUiThread {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), permissionRequest)
                } else startSpeechRecognition()
            }
        }
    }

    private fun startSpeechRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "AI CORE vous écoute")
            }
            startActivityForResult(intent, speechRequest)
        } catch (_: Exception) {
            sendSpeechError("not-available")
        }
    }

    private fun displayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor?.moveToFirst() == true) cursor.getString(0) else null
        } finally {
            cursor?.close()
        }
    }

    private fun importModel(uri: Uri) {
        worker.execute {
            val name = displayName(uri).orEmpty()
            if (!name.endsWith(".litertlm", ignoreCase = true) || !name.contains("gemma3-1b-it-int4", ignoreCase = true)) {
                modelStatus("Choisis le fichier gemma3-1b-it-int4.litertlm téléchargé depuis la page officielle.")
                return@execute
            }
            val temp = File(filesDir, "ai-core-model.tmp")
            try {
                val free = filesDir.usableSpace
                if (free < 700L * 1024 * 1024) {
                    modelStatus("Libère au moins 700 Mo sur le téléphone puis réessaie.")
                    return@execute
                }
                modelStatus("Installation du modèle : 0 %")
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Le fichier n’est pas accessible." }
                    temp.outputStream().buffered().use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        var copied = 0L
                        var lastPercent = -1
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            copied += count
                            val percent = (copied * 100 / (529L * 1024 * 1024)).toInt().coerceAtMost(99)
                            if (percent >= lastPercent + 5) {
                                lastPercent = percent
                                modelStatus("Installation du modèle : $percent %")
                            }
                        }
                    }
                }
                if (temp.length() < 400L * 1024 * 1024) throw IllegalArgumentException("Le fichier est trop petit pour être le modèle complet.")
                if (modelFile.exists()) modelFile.delete()
                if (!temp.renameTo(modelFile)) temp.copyTo(modelFile, overwrite = true)
                temp.delete()
                modelStatus("Modèle installé. Chargement…")
                initializeModel()
                modelStatus("IA hors ligne prête")
            } catch (e: Exception) {
                temp.delete()
                modelStatus("Installation impossible : ${e.message ?: "réessaie avec le fichier complet"}")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequest) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startSpeechRecognition()
            else sendSpeechError("not-allowed")
        }
    }

    @Deprecated("Android activity result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == modelRequest && resultCode == RESULT_OK) data?.data?.let(::importModel)
        if (requestCode == mediaRequest && resultCode == RESULT_OK) data?.data?.let(::importVideo)
        if (requestCode != speechRequest) return
        if (resultCode == RESULT_OK) {
            val phrase = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            sendToPage("window.__aiCoreSpeechResult && window.__aiCoreSpeechResult(${JSONObject.quote(phrase)});")
        } else sendToPage("window.__aiCoreSpeechEnd && window.__aiCoreSpeechEnd();")
    }

    private fun sendSpeechError(code: String) =
        sendToPage("window.__aiCoreSpeechError && window.__aiCoreSpeechError(${JSONObject.quote(code)});")

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
        worker.shutdownNow()
        super.onDestroy()
    }
}
