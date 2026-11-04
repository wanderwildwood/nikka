package com.wanderwildwood.nikka.data

/**
 * A habit you named yourself, or the one the app keeps for you: meditation, which is ticked by
 * any logged sitting rather than by hand, and has no name of its own because its name is a word
 * of the interface ([builtin]).
 *
 * [perWeek] is how often it is meant: 7 is every day, anything less is that many times a week,
 * on whichever days.
 */
data class Habit(
    val id: Long,
    val name: String,
    val perWeek: Int,
    val builtin: Boolean,
    val position: Int,
    /** When or after what it is done, in the person's words ("after breakfast"); empty for none. */
    val cue: String = "",
) {
    val daily: Boolean get() = perWeek >= 7
}

/**
 * One session as it was logged: when it began (wall clock, ms), how long it was meant to be, how
 * long it actually lasted, and a line about it if one was written. A session ended early is
 * still logged, so [satSec] can be less than [plannedSec].
 *
 * [kind] is a sitting or a breathing session. Only sittings tick meditation; breathing is in the
 * log beside them. [pattern] is a breathing session's "in,hold,out,rest" seconds, empty for a
 * sitting. Both are data, not interface, and stay in English.
 */
data class Session(
    val id: Long,
    val start: Long,
    val plannedSec: Int,
    val satSec: Int,
    val note: String,
    val kind: String = SIT,
    val pattern: String = "",
) {
    val endedEarly: Boolean get() = satSec < plannedSec
    val breathing: Boolean get() = kind == BREATHE

    companion object {
        const val SIT = "sit"
        const val BREATHE = "breathe"
    }
}
