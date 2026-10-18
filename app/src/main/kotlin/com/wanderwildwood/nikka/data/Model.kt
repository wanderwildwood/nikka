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
) {
    val daily: Boolean get() = perWeek >= 7
}

/**
 * One sitting as it was logged: when it began (wall clock, ms), how long it was meant to be, how
 * long was actually sat, and a line about it if one was written. A sitting ended early is still
 * a sitting, so [satSec] can be less than [plannedSec].
 */
data class Session(
    val id: Long,
    val start: Long,
    val plannedSec: Int,
    val satSec: Int,
    val note: String,
) {
    val endedEarly: Boolean get() = satSec < plannedSec
}
