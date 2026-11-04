package com.wanderwildwood.nikka.sit

/**
 * A breathing pattern: seconds breathing in, holding full, breathing out, and holding empty.
 * Either hold may be 0, and is then left out. 4-7-8 is (4, 7, 8, 0); 6-6 is (6, 0, 6, 0); a box
 * is (4, 4, 4, 4).
 */
data class Breath(val inSec: Int, val holdSec: Int, val outSec: Int, val restSec: Int) {

    enum class Kind { IN, HOLD, OUT, REST }

    data class Phase(val kind: Kind, val sec: Int)

    /** The phases of one round, the empty holds left out. */
    val phases: List<Phase>
        get() = listOf(Phase(Kind.IN, inSec), Phase(Kind.HOLD, holdSec), Phase(Kind.OUT, outSec), Phase(Kind.REST, restSec))
            .filter { it.sec > 0 }

    val roundSec: Int get() = inSec + holdSec + outSec + restSec

    /** Whole rounds that fill [minutes], at least one: a session never stops mid-breath. */
    fun rounds(minutes: Int): Int = maxOf(1, (minutes * 60 + roundSec - 1) / roundSec)

    /** Where a session is, [ms] after it began: the phase, its number in the round, and the time left in it. */
    data class At(val round: Int, val phaseIndex: Int, val phase: Phase, val leftMs: Long)

    /** Where a session of [rounds] is at [ms] from its start, or null once it is over. */
    fun at(ms: Long, rounds: Int): At? {
        if (ms < 0) return At(0, 0, phases[0], phases[0].sec * 1000L)
        val roundMs = roundSec * 1000L
        val round = (ms / roundMs).toInt()
        if (round >= rounds) return null
        var into = ms - round * roundMs
        phases.forEachIndexed { i, p ->
            val len = p.sec * 1000L
            if (into < len) return At(round, i, p, len - into)
            into -= len
        }
        return null
    }

    /** The figure for the Sit tab: "4 · 7 · 8", "6 · 6", "4 · 4 · 4 · 4". */
    fun figure(): String = phases.joinToString(" · ") { it.sec.toString() }

    fun valid(): Boolean = inSec in 1..MAX_SEC && outSec in 1..MAX_SEC && holdSec in 0..MAX_SEC && restSec in 0..MAX_SEC

    companion object {
        const val MAX_SEC = 20
        val FOUR_SEVEN_EIGHT = Breath(4, 7, 8, 0)
        val EVEN = Breath(6, 0, 6, 0)
        val BOX = Breath(4, 4, 4, 4)
        val PRESETS = listOf(FOUR_SEVEN_EIGHT, EVEN, BOX)
    }
}
