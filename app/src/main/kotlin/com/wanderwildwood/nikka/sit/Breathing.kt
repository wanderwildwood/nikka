package com.wanderwildwood.nikka.sit

import android.content.Context
import com.wanderwildwood.nikka.data.Prefs
import com.wanderwildwood.nikka.data.Session
import com.wanderwildwood.nikka.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A breathing session: when it began ([start], wall clock ms), the pattern, how many rounds of
 * it, how each change is cued, and whether a sitting follows it.
 */
data class BreathRun(
    val start: Long,
    val breath: Breath,
    val rounds: Int,
    val cue: Int,
    val thenSit: Boolean,
) {
    val plannedSec: Int get() = rounds * breath.roundSec
}

/**
 * Starting and ending a breathing session. Unlike a sitting it is not timed by alarms: the cues
 * come from the breathing screen while it is on screen, and leaving the app ends the session.
 * So nothing here outlives the process, and nothing is kept in the preferences.
 */
object Breathing {

    private val _running = MutableStateFlow<BreathRun?>(null)
    /** The breathing session under way, for the screens. */
    val running: StateFlow<BreathRun?> = _running

    private val _done = MutableStateFlow(-1)
    /** The rounds breathed in the session just ended, shown until it is closed; -1 for none. */
    val done: StateFlow<Int> = _done

    /** Breathes for [minutes] in the pattern and with the cue set on the Breathe tab. */
    fun start(context: Context, minutes: Int, thenSit: Boolean) {
        val prefs = Prefs(context)
        val breath = prefs.breath
        _done.value = -1
        _running.value = BreathRun(
            start = System.currentTimeMillis(),
            breath = breath,
            rounds = breath.rounds(minutes),
            cue = prefs.breathCue,
            thenSit = thenSit,
        )
    }

    /**
     * Ends the session under way, by itself or by the person. It is logged when at least
     * [MIN_LOGGED_SEC] were breathed, as a session of its own that does not count as a sitting.
     * When a sitting follows and the session was not [cancelled], the sitting begins at once,
     * with no warm-up of its own; otherwise the screen says how many rounds were breathed.
     */
    fun end(context: Context, cancelled: Boolean) {
        val run = _running.value ?: return
        val now = System.currentTimeMillis()
        val roundMs = run.breath.roundSec * 1000L
        val elapsed = (now - run.start).coerceIn(0, run.rounds * roundMs)
        val sec = (elapsed / 1000).toInt()
        if (sec >= MIN_LOGGED_SEC) {
            val b = run.breath
            Store.get(context).addSession(
                start = run.start,
                plannedSec = run.plannedSec,
                satSec = sec,
                kind = Session.BREATHE,
                pattern = "${b.inSec},${b.holdSec},${b.outSec},${b.restSec}",
            )
        }
        _running.value = null
        if (run.thenSit && !cancelled) {
            Sitting.start(context, warmup = false)
        } else {
            _done.value = (elapsed / roundMs).toInt()
        }
    }

    /** The done screen closed. */
    fun close() {
        _done.value = -1
    }

    /** A session shorter than this is not written to the log. */
    const val MIN_LOGGED_SEC = 10
}
