package com.example.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Cyberpunk & Mechatronic synthesized acoustic sound engine for Battle Box : FFF.
 * Uses 16-bit PCM AudioTrack waveforms with a ToneGenerator fallback and full Robolectric safety.
 */
object BattleBoxSoundEngine {

    enum class SoundType {
        QR_SCAN_LOCK,
        PASS_CHECK_IN,
        CHECK_OUT,
        ADMIN_JOIN_LEAVE,
        USER_ALERT_BANNER,
        MEMBERSHIP_RENEWAL,
        REST_TIMER_DONE
    }

    private data class ToneStep(
        val freqHz: Double,
        val durationMs: Int,
        val pauseMs: Int = 18,
        val gain: Double = 0.35
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
    }

    fun toggleMute(): Boolean {
        val next = !_isMuted.value
        _isMuted.value = next
        return next
    }

    fun play(type: SoundType) {
        if (_isMuted.value) return
        if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) return

        scope.launch {
            runCatching {
                val sequence = when (type) {
                    SoundType.QR_SCAN_LOCK -> listOf(
                        ToneStep(1180.0, 55, 16, 0.35),
                        ToneStep(1680.0, 75, 0, 0.40)
                    )
                    SoundType.PASS_CHECK_IN -> listOf(
                        ToneStep(523.25, 75, 15, 0.32),
                        ToneStep(659.25, 75, 15, 0.34),
                        ToneStep(1046.50, 140, 0, 0.40)
                    )
                    SoundType.CHECK_OUT -> listOf(
                        ToneStep(880.0, 75, 16, 0.34),
                        ToneStep(587.33, 80, 16, 0.30),
                        ToneStep(440.0, 140, 0, 0.28)
                    )
                    SoundType.ADMIN_JOIN_LEAVE -> listOf(
                        ToneStep(987.77, 65, 22, 0.34),
                        ToneStep(1318.51, 95, 0, 0.38)
                    )
                    SoundType.USER_ALERT_BANNER -> listOf(
                        ToneStep(740.0, 80, 25, 0.34),
                        ToneStep(987.77, 90, 25, 0.36),
                        ToneStep(740.0, 110, 0, 0.30)
                    )
                    SoundType.MEMBERSHIP_RENEWAL -> listOf(
                        ToneStep(587.33, 85, 20, 0.34),
                        ToneStep(880.0, 95, 20, 0.38),
                        ToneStep(1174.66, 155, 0, 0.40)
                    )
                    SoundType.REST_TIMER_DONE -> listOf(
                        ToneStep(1396.91, 90, 35, 0.42),
                        ToneStep(1396.91, 90, 35, 0.42),
                        ToneStep(1760.0, 180, 0, 0.45)
                    )
                }
                playPcmSequence(sequence)
            }.onFailure {
                runCatching {
                    val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
                    val tone = when (type) {
                        SoundType.QR_SCAN_LOCK -> ToneGenerator.TONE_PROP_BEEP
                        SoundType.PASS_CHECK_IN -> ToneGenerator.TONE_PROP_ACK
                        SoundType.CHECK_OUT -> ToneGenerator.TONE_PROP_BEEP2
                        SoundType.ADMIN_JOIN_LEAVE -> ToneGenerator.TONE_PROP_PROMPT
                        SoundType.USER_ALERT_BANNER -> ToneGenerator.TONE_SUP_PIP
                        SoundType.MEMBERSHIP_RENEWAL -> ToneGenerator.TONE_PROP_NACK
                        SoundType.REST_TIMER_DONE -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                    }
                    tg.startTone(tone, 140)
                    Thread.sleep(160)
                    tg.release()
                }
            }
        }
    }

    private fun playPcmSequence(steps: List<ToneStep>) {
        val sampleRate = 22050
        val totalSamples = steps.sumOf { ((it.durationMs + it.pauseMs) * sampleRate) / 1000 }
        if (totalSamples <= 0) return

        val pcm = ShortArray(totalSamples)
        var cursor = 0
        for (step in steps) {
            val toneSamples = (step.durationMs * sampleRate) / 1000
            val pauseSamples = (step.pauseMs * sampleRate) / 1000
            val fadeSamples = (toneSamples / 8).coerceAtLeast(1)

            for (i in 0 until toneSamples) {
                val env = when {
                    i < fadeSamples -> i.toDouble() / fadeSamples
                    i > toneSamples - fadeSamples -> (toneSamples - i).toDouble() / fadeSamples
                    else -> 1.0
                }
                val angle = 2.0 * PI * step.freqHz * i / sampleRate
                // Slight second harmonic gives a crisp mechatronic/cyberpunk synth timbre
                val wave = 0.82 * sin(angle) + 0.18 * sin(angle * 2.0)
                val sampleVal = (wave * env * step.gain * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                if (cursor < pcm.size) {
                    pcm[cursor++] = sampleVal.toShort()
                }
            }
            cursor = (cursor + pauseSamples).coerceAtMost(pcm.size)
        }

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(pcm.size * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            val totalMs = steps.sumOf { it.durationMs + it.pauseMs }.toLong()
            Thread.sleep(totalMs + 40L)
            track.stop()
        } finally {
            track.release()
        }
    }
}
