package com.wanderwildwood.nikka.sit

/**
 * One sitting as set going: when Start was pressed ([start], wall clock ms), the warm-up before
 * the first bell, the length of the sitting itself, and the interval between bells inside it
 * (0 for none). Every bell's time follows from these, so nothing but these needs keeping while
 * it runs: after the process is stopped and started again by an alarm, the plan is the same.
 */
data class Plan(
    val start: Long,
    val warmupSec: Int,
    val plannedSec: Int,
    val intervalSec: Int,
) {
    enum class Kind { START, INTERVAL, END }

    data class Bell(val index: Int, val at: Long, val kind: Kind)

    /** When the sitting proper begins, after the warm-up. */
    val sitStart: Long get() = start + warmupSec * 1000L

    val end: Long get() = sitStart + plannedSec * 1000L

    /**
     * A bell as the sitting begins, one at each interval inside it, and the end bell. An interval
     * that would fall on the end, or within a few seconds of it, is left out: two bells at once
     * is one bell too many.
     */
    fun bells(): List<Bell> {
        val out = ArrayList<Bell>()
        out += Bell(0, sitStart, Kind.START)
        if (intervalSec > 0) {
            var k = 1
            while (k.toLong() * intervalSec < plannedSec - MIN_GAP_SEC) {
                out += Bell(out.size, sitStart + k.toLong() * intervalSec * 1000L, Kind.INTERVAL)
                k++
            }
        }
        out += Bell(out.size, end, Kind.END)
        return out
    }

    /** The first bell still to ring after bell [rung] (-1 for none yet), or null when all have. */
    fun next(rung: Int): Bell? = bells().firstOrNull { it.index > rung }

    /** Seconds actually sat by [now]: none during the warm-up, never more than the plan. */
    fun satBy(now: Long): Int = ((now - sitStart) / 1000L).coerceIn(0L, plannedSec.toLong()).toInt()

    fun warmingUp(now: Long): Boolean = now < sitStart

    companion object {
        const val MIN_GAP_SEC = 5
    }
}
