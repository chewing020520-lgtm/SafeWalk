package com.example.safewalk.hardware

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.example.safewalk.fusion.WalkingState
import java.util.Locale

class HapticFeedbackManager(context: Context) {

    private val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.KOREAN
        }
    }

    private var lastFeedbackTime = 0L
    private val FEEDBACK_COOLDOWN_MS = 3000L

    fun provideFeedback(state: WalkingState, severityDeg: Float) {
        val now = System.currentTimeMillis()
        if (now - lastFeedbackTime < FEEDBACK_COOLDOWN_MS) return
        lastFeedbackTime = now

        when (state) {
            WalkingState.DRIFTING_LEFT -> {
                vibratePattern(longArrayOf(0, 100, 100, 100))
                speak("오른쪽으로 방향을 조금 틀어주세요")
            }
            WalkingState.DRIFTING_RIGHT -> {
                vibratePattern(longArrayOf(0, 300))
                speak("왼쪽으로 방향을 조금 틀어주세요")
            }
            WalkingState.ALIGNED -> { }
            WalkingState.UNCERTAIN -> { }
        }
    }

    private fun vibratePattern(pattern: LongArray) {
        vibrator.vibrate(
            VibrationEffect.createWaveform(pattern, -1)
        )
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun release() = tts.shutdown()
}
