package com.wanderwildwood.nikka.sit

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.VibratorManager
import com.wanderwildwood.nikka.R

/**
 * The cue at each change of a breath, so it can be followed with the eyes shut: a buzz, a soft
 * tone, both, or nothing but the screen. In is one buzz and the higher tone; out is two buzzes
 * and the lower tone; a hold is the faintest tick and a quiet tone between the two. The tones
 * play on the alarm stream, as the bowl does, so they follow the alarm volume.
 *
 * Made for one session and let go at its end.
 */
class BreathCue(context: Context, private val cue: Int) {

    private val app = context.applicationContext
    private val vibrator = app.getSystemService(VibratorManager::class.java)?.defaultVibrator
    private val pool: SoundPool? = if (tones) {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    } else null
    // The tones load in the background, and the first phase is cued the moment a session starts,
    // before they are ready. A tone asked for before it has loaded plays as soon as it has.
    private val loaded = HashSet<Int>()
    private var waiting = 0

    init {
        pool?.setOnLoadCompleteListener { p, id, status ->
            if (status != 0) return@setOnLoadCompleteListener
            loaded += id
            if (id == waiting) {
                waiting = 0
                p.play(id, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    private val inId = pool?.load(app, R.raw.breath_in, 1) ?: 0
    private val holdId = pool?.load(app, R.raw.breath_hold, 1) ?: 0
    private val outId = pool?.load(app, R.raw.breath_out, 1) ?: 0

    private val tones: Boolean get() = cue == TONE || cue == BOTH
    private val buzz: Boolean get() = cue == VIBRATE || cue == BOTH

    fun phase(kind: Breath.Kind) {
        if (buzz) {
            vibrator?.vibrate(
                when (kind) {
                    Breath.Kind.IN -> VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
                    Breath.Kind.OUT -> VibrationEffect.createWaveform(longArrayOf(0, 60, 140, 60), -1)
                    else -> VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                },
            )
        }
        val id = when (kind) {
            Breath.Kind.IN -> inId
            Breath.Kind.OUT -> outId
            else -> holdId
        }
        if (id == 0) return
        if (id in loaded) {
            waiting = 0
            pool?.play(id, 1f, 1f, 1, 0, 1f)
        } else {
            waiting = id
        }
    }

    /** The session is over: the bowl once, or a long buzz, as the cue was chosen. */
    fun end() {
        if (buzz) vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1))
        if (tones) Bell.play(app, end = false)
    }

    fun release() {
        pool?.release()
    }

    companion object {
        const val VIBRATE = 0
        const val TONE = 1
        const val BOTH = 2
        const val SCREEN = 3
        val ALL = listOf(VIBRATE, TONE, BOTH, SCREEN)
    }
}
