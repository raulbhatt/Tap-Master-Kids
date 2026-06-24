package com.example.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundSynthesizer {
    private val scope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    fun playPop() {
        scope.launch {
            try {
                // Short bubbly popup: pitch sweeping upwards from 250Hz to 1000Hz in 0.08s
                val duration = 0.08f
                val numSamples = (SAMPLE_RATE * duration).toInt()
                val soundBuffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val progress = t / duration
                    val currentFreq = 250.0f + (750.0f * progress)
                    val angle = 2.0 * Math.PI * currentFreq * t
                    val envelope = if (progress < 0.1f) progress / 0.1f else 1.0f - progress
                    soundBuffer[i] = (sin(angle) * Short.MAX_VALUE * 0.40f * envelope).toInt().toShort()
                }

                playBuffer(soundBuffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playBoing() {
        scope.launch {
            try {
                // Playful downward cartoon spring boing sound: 380Hz to 160Hz with a wobble in 0.22s
                val duration = 0.22f
                val numSamples = (SAMPLE_RATE * duration).toInt()
                val soundBuffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val progress = t / duration
                    val freqBase = 380.0f * (1.0f - progress * 0.6f)
                    val wobble = sin(2.0 * Math.PI * 25.0 * t) * 20.0f
                    val currentFreq = freqBase + wobble
                    val angle = 2.0 * Math.PI * currentFreq * t
                    val envelope = 1.0f - progress
                    soundBuffer[i] = (sin(angle) * Short.MAX_VALUE * 0.35f * envelope).toInt().toShort()
                }

                playBuffer(soundBuffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playChime() {
        scope.launch {
            try {
                // A quick happy arpeggio chime (3 consecutive notes: E6, G6, C7)
                // Total duration 0.24s (0.08s per note)
                val duration = 0.25f
                val numSamples = (SAMPLE_RATE * duration).toInt()
                val soundBuffer = ShortArray(numSamples)

                val freqs = floatArrayOf(1318.51f, 1567.98f, 2093.00f) // E6, G6, C7

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val noteIndex = (t / 0.083f).toInt().coerceIn(0, 2)
                    val freq = freqs[noteIndex]
                    val angle = 2.0 * Math.PI * freq * t
                    
                    val noteProgress = (t % 0.083f) / 0.083f
                    val envelope = (1.0f - noteProgress) * 0.35f
                    
                    soundBuffer[i] = (sin(angle) * Short.MAX_VALUE * envelope).toInt().toShort()
                }

                playBuffer(soundBuffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playFever() {
        scope.launch {
            try {
                // Triumphant continuous rising multi-chime for Fever Mode entry!
                val duration = 0.45f
                val numSamples = (SAMPLE_RATE * duration).toInt()
                val soundBuffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val progress = t / duration
                    val f1 = 440.0f * (1.0f + progress)
                    val f2 = 880.0f * (1.0f + progress)
                    val angle = 2.0 * Math.PI * f1 * t + sin(2.0 * Math.PI * f2 * t) * 0.1
                    val envelope = if (progress < 0.15f) progress / 0.15f else 1.0f - progress
                    soundBuffer[i] = (sin(angle) * Short.MAX_VALUE * 0.30f * envelope).toInt().toShort()
                }

                playBuffer(soundBuffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playBuffer(buffer: ShortArray) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        
        scope.launch {
            try {
                val playDurationMs = ((buffer.size.toFloat() / SAMPLE_RATE) * 1000).toLong()
                kotlinx.coroutines.delay(playDurationMs + 200)
                audioTrack.stop()
                audioTrack.release()
            } catch (ignored: Exception) {}
        }
    }
}
