package com.uj.appstorysautopaymanager.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class TextToSpeechHelper(
    private val context: Context,
    private val preferenceManager: PreferenceManager
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                scope.launch {
                    val langCode = preferenceManager.speechLanguageFlow.first()
                    val speed = preferenceManager.speechSpeedFlow.first()
                    
                    tts?.let { speech ->
                        speech.language = Locale(langCode)
                        speech.setSpeechRate(speed)
                    }
                }
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }
    }

    fun speak(text: String) {
        scope.launch {
            val isEnabled = preferenceManager.isVoiceAlertsEnabledFlow.first()
            if (!isEnabled) return@launch

            if (!isInitialized || tts == null) {
                initializeTts()
                var retries = 0
                while (!isInitialized && retries < 15) {
                    Thread.sleep(200)
                    retries++
                }
            }

            val langCode = preferenceManager.speechLanguageFlow.first()
            val speed = preferenceManager.speechSpeedFlow.first()

            tts?.let { speech ->
                speech.language = Locale(langCode)
                speech.setSpeechRate(speed)
                speech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AutoPayTTS")
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
