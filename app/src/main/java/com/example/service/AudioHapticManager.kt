package com.example.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.model.HazardUrgency
import java.util.Locale

private const val TAG = "AudioHapticManager"

class AudioHapticManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var toneGenerator: ToneGenerator? = null

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    var isVoiceEnabled: Boolean = true
    var isHapticEnabled: Boolean = true

    init {
        tts = TextToSpeech(context.applicationContext, this)
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator init failed", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "TTS language US not supported, fallback default")
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(speechRate)
            isTtsReady = true
            Log.d(TAG, "TextToSpeech initialized successfully")
            speak("StepSight audio active", isUrgent = false)
        } else {
            Log.e(TAG, "TextToSpeech init failed with status $status")
        }
    }

    fun speak(text: String, isUrgent: Boolean = false) {
        if (!isVoiceEnabled || text.isBlank()) return

        if (!isTtsReady) {
            Log.w(TAG, "TTS not ready yet: $text")
            return
        }

        val queueMode = if (isUrgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, null, "step_sight_speech_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun notifyHazard(urgency: HazardUrgency, spokenPrompt: String) {
        when (urgency) {
            HazardUrgency.CRITICAL -> {
                playWarningTone()
                vibrateCritical()
                speak(spokenPrompt, isUrgent = true)
            }
            HazardUrgency.CAUTION -> {
                vibrateCaution()
                speak(spokenPrompt, isUrgent = true)
            }
            HazardUrgency.INFO -> {
                vibrateDirection()
                speak(spokenPrompt, isUrgent = false)
            }
            HazardUrgency.CLEAR -> {
                vibrateClear()
                speak(spokenPrompt, isUrgent = false)
            }
        }
    }

    fun vibrateCritical() {
        if (!isHapticEnabled) return
        val pattern = longArrayOf(0, 160, 90, 160, 90, 320)
        vibratePattern(pattern)
    }

    fun vibrateCaution() {
        if (!isHapticEnabled) return
        val pattern = longArrayOf(0, 130, 80, 130)
        vibratePattern(pattern)
    }

    fun vibrateClear() {
        if (!isHapticEnabled) return
        val pattern = longArrayOf(0, 45)
        vibratePattern(pattern)
    }

    fun vibrateDirection() {
        if (!isHapticEnabled) return
        val pattern = longArrayOf(0, 180)
        vibratePattern(pattern)
    }

    private fun playWarningTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        } catch (e: Exception) {
            Log.w(TAG, "Tone play error", e)
        }
    }

    private fun vibratePattern(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibrator error", e)
        }
    }

    fun destroy() {
        try {
            tts?.stop()
            tts?.shutdown()
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup error", e)
        }
    }
}
