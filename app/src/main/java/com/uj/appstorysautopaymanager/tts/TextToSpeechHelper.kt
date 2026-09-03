package com.uj.appstorysautopaymanager.tts

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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

    // Text is built here, not passed in, so the whole sentence changes with the speech-language
    // setting - not just the TTS engine's pronunciation accent on text that stayed English.
    fun speak(kind: AnnouncementKind, merchant: String, amount: Int) {
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

            if (preferenceManager.playChimeFirstFlow.first()) {
                playChime(preferenceManager.alertToneFlow.first())
            }

            val langCode = preferenceManager.speechLanguageFlow.first()
            val speed = preferenceManager.speechSpeedFlow.first()
            val voiceEngine = preferenceManager.voiceEngineFlow.first()
            val text = SpeechTemplates.build(langCode, kind, merchant, amount)
            val wantsFemale = voiceEngine.equals("Female Voice", ignoreCase = true)

            tts?.let { speech ->
                speech.language = Locale(langCode)
                speech.setSpeechRate(speed)
                val genderVoice = selectGenderVoice(speech, Locale(langCode), wantsFemale)
                genderVoice?.let { speech.voice = it }
                // Kept as a supplementary nudge even when a real gender-matched voice was found
                // above, and as the only signal left when it wasn't - harmless either way.
                speech.setPitch(if (wantsFemale) 1.05f else 0.95f)
                Log.d("TTS_VOICE_DEBUG", "wantsFemale=$wantsFemale selectedVoice=${genderVoice?.name}")
                speech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AutoPayTTS")
            }
        }
    }

    // "Play Chime First" was toggleable and its selected tone was stored, but nothing anywhere
    // ever read either setting - the same shape of bug as speechLanguage/voiceEngine before this.
    // res/raw/cashier.mp3, gun_shot.mp3, temple_bell.mp3 (matching the sibling SoundBox app's own
    // asset names) don't exist in this project - looked up by name at runtime rather than a
    // compile-time R.raw reference, so dropping a properly-licensed file in later picks it up
    // automatically with no code change. Until then, each tone plays a synthesized ToneGenerator
    // pattern instead - deliberately NOT a downloaded "cash register"/"gunshot"/"temple bell"
    // recording, since those carry real licensing terms even on "free" sound-effect sites and
    // shouldn't be grabbed from an arbitrary source without the license being checked first.
    // DTMF tone frequencies are a fixed telephony standard, not vendor/device-specific, so these
    // three patterns sound the same on every device.
    private suspend fun playChime(alertTone: String) = suspendCancellableCoroutine<Unit> { continuation ->
        val resName = when (alertTone) {
            "Cashier" -> "cashier"
            "Gun Shot" -> "gun_shot"
            "Temple Bell" -> "temple_bell"
            else -> null
        }
        val resId = resName
            ?.let { context.resources.getIdentifier(it, "raw", context.packageName) }
            ?.takeIf { it != 0 }

        if (resId != null) {
            try {
                val player = MediaPlayer.create(context, resId)
                if (player != null) {
                    player.setOnCompletionListener {
                        it.release()
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                    }
                    player.start()
                    return@suspendCancellableCoroutine
                }
            } catch (e: Exception) {
                Log.e("TTS", "Failed to play bundled alert tone '$resName', falling back to a synthesized tone", e)
            }
        }

        try {
            // STREAM_MUSIC, not STREAM_NOTIFICATION: the speech right after this uses the media
            // volume slider (TextToSpeech's default output stream), and on most phones the
            // notification slider is independently set lower/muted - that mismatch is why the
            // beep was silent even with everything else wired correctly.
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            when (alertTone) {
                // Two quick bright pips - a "cha-ching" rhythm.
                "Cashier" -> {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 90)
                    scope.launch {
                        delay(140)
                        toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 90)
                        delay(150)
                        toneGenerator.release()
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                    }
                }
                // One short, sharp burst.
                "Gun Shot" -> {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_S, 80)
                    scope.launch {
                        delay(150)
                        toneGenerator.release()
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                    }
                }
                // One longer, lower, sustained tone.
                "Temple Bell" -> {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 700)
                    scope.launch {
                        delay(750)
                        toneGenerator.release()
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                    }
                }
                else -> {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    scope.launch {
                        delay(250)
                        toneGenerator.release()
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TTS", "Failed to play fallback tone", e)
            if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
        }
    }

    // Google's on-device TTS voices (the "xx-yy-x-abc-local"/"-network" ones) don't put the word
    // "female"/"male" anywhere in the name - confirmed on a real device (Punjabi voices like
    // "pa-in-x-pac-local" have nothing to substring-match on at all). What they do encode is the
    // gender in the last letter of the subtag right after "-x-": a/c/e/g/i are female-voiced
    // models, b/d/f/h/j are male. That's what was missing before - without decoding this, the
    // gender filter always came up empty and the code silently fell back to the engine's own
    // default pick, which is why the toggle had no audible effect regardless of pitch.
    private fun selectGenderVoice(speech: TextToSpeech, locale: Locale, wantsFemale: Boolean): Voice? {
        val voices = speech.voices ?: return null
        var best: Voice? = null
        var bestScore = -1

        for (voice in voices) {
            if (!voice.locale.language.equals(locale.language, ignoreCase = true)) continue

            val features = voice.features ?: emptySet()
            val name = voice.name.lowercase(Locale.ROOT)
            var isFemale = false
            var isMale = false

            if ("female" in features || "gender=female" in features) {
                isFemale = true
            } else if ("male" in features || "gender=male" in features) {
                isMale = true
            } else if ("female" in name) {
                isFemale = true
            } else if ("male" in name) {
                isMale = true
            } else if ("-x-" in name) {
                when (name.substringAfter("-x-").substringBefore("-").lastOrNull()) {
                    'a', 'c', 'e', 'g', 'i' -> isFemale = true
                    'b', 'd', 'f', 'h', 'j' -> isMale = true
                }
            }

            if ((wantsFemale && isFemale) || (!wantsFemale && isMale)) {
                var score = 0
                if (!voice.isNetworkConnectionRequired) score += 50
                if (voice.quality >= Voice.QUALITY_HIGH) score += 10
                if (score > bestScore) {
                    bestScore = score
                    best = voice
                }
            }
        }
        return best
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
