package fr.aicore.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val permissionRequest = 42
    private val speechRequest = 43

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

    private inner class SpeechBridge {
        @JavascriptInterface
        fun start() {
            runOnUiThread {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), permissionRequest)
                } else {
                    startSpeechRecognition()
                }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequest) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startSpeechRecognition()
            else sendSpeechError("not-allowed")
        }
    }

    @Deprecated("Android speech recognizer result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != speechRequest) return
        if (resultCode == RESULT_OK) {
            val phrase = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            web.evaluateJavascript("window.__aiCoreSpeechResult(${JSONObject.quote(phrase)});", null)
        } else {
            web.evaluateJavascript("window.__aiCoreSpeechEnd();", null)
        }
    }

    private fun sendSpeechError(code: String) {
        web.evaluateJavascript("window.__aiCoreSpeechError(${JSONObject.quote(code)});", null)
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
