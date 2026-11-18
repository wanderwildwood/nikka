package com.wanderwildwood.nikka.sit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanTest {

    private val t0 = 1_000_000L

    @Test fun noIntervalIsStartAndEnd() {
        val b = Plan(t0, 0, 600, 0).bells()
        assertEquals(listOf(Plan.Kind.START, Plan.Kind.END), b.map { it.kind })
        assertEquals(t0, b[0].at)
        assertEquals(t0 + 600_000, b[1].at)
    }

    @Test fun warmUpMovesEveryBell() {
        val p = Plan(t0, 30, 600, 0)
        assertEquals(t0 + 30_000, p.bells()[0].at)
        assertEquals(t0 + 630_000, p.end)
    }

    @Test fun intervalsInsideTheSitting() {
        val b = Plan(t0, 0, 20 * 60, 5 * 60).bells()
        assertEquals(listOf(0, 5, 10, 15, 20).map { t0 + it * 60_000L }, b.map { it.at })
        assertEquals(Plan.Kind.END, b.last().kind)
        assertEquals((0 until b.size).toList(), b.map { it.index })
    }

    @Test fun noIntervalBellOnTopOfTheEnd() {
        // 7 minutes with a bell every 3.5: the second would land on the end.
        val b = Plan(t0, 0, 420, 210).bells()
        assertEquals(3, b.size)
    }

    @Test fun nextAfterRung() {
        val p = Plan(t0, 0, 600, 300)
        assertEquals(0, p.next(-1)!!.index)
        assertEquals(2, p.next(1)!!.index)
        assertNull(p.next(2))
    }

    @Test fun satNeverCountsTheWarmUpOrMoreThanPlanned() {
        val p = Plan(t0, 60, 600, 0)
        assertEquals(0, p.satBy(t0 + 30_000))
        assertEquals(true, p.warmingUp(t0 + 30_000))
        assertEquals(90, p.satBy(t0 + 150_000))
        assertEquals(600, p.satBy(t0 + 9_999_999))
    }

    @Test fun aRestartLogsTheTimeSatUntilItWasLastSeen() {
        val p = Plan(t0, 30, 600, 0)
        val boot = t0 + 3_600_000
        assertEquals(0, p.satAfterRestart(0, t0 + 20_000, boot))
        assertEquals(120, p.satAfterRestart(0, t0 + 150_000, boot))
        // Shut down after the end, with the end bell lost: the plan, no more.
        assertEquals(600, p.satAfterRestart(0, p.end + 30_000, boot))
    }

    @Test fun theTimeThePhoneWasOffIsNotTimeSat() {
        // 30 minutes planned, an interval bell every 5; the battery runs out after the first.
        val p = Plan(t0, 0, 1800, 300)
        val boot = t0 + 20 * 60_000L
        assertEquals(300, p.satAfterRestart(1, -1, boot))
        // Back after the planned end, still only what is known to have been sat.
        assertEquals(300, p.satAfterRestart(1, -1, p.end + 3_600_000))
        // Nothing rung past the start bell and no word of a shutdown: nothing is known sat.
        assertEquals(0, p.satAfterRestart(0, -1, boot))
    }

    @Test fun noIntervalsLogsUpToTheLastTimeItWasSeen() {
        // 20 minutes, no interval bells, so only the start bell (0) has rung when the phone goes.
        val p = Plan(t0, 0, 1200, 0)
        val boot = t0 + 3_600_000
        // No word at all (the battery ran out with the app closed): nothing is known sat, so
        // nothing is logged.
        assertEquals(0, p.satAfterRestart(0, -1, boot))
        // Left the app 2 minutes in, phone off later without a word: those 2 minutes.
        assertEquals(120, p.satAfterRestart(0, t0 + 120_000, boot))
        // The phone said it was shutting down 9 minutes in: 9 minutes.
        assertEquals(540, p.satAfterRestart(0, t0 + 540_000, boot))
    }

    @Test fun aLaterBellOutweighsAnEarlierSighting() {
        val p = Plan(t0, 0, 1800, 300)
        // Opened 2 minutes in, then the bell at 10 minutes rang before the phone went.
        assertEquals(600, p.satAfterRestart(2, t0 + 120_000, t0 + 3_600_000))
    }

    @Test fun aShutdownFromAnEarlierSittingIsNotThisOnes() {
        val p = Plan(t0, 0, 600, 0)
        assertEquals(0, p.satAfterRestart(0, t0 - 1, t0 + 3_600_000))
    }

    @Test fun neverPastTheBoot() {
        val p = Plan(t0, 0, 600, 0)
        assertEquals(t0 + 100_000, p.aliveUntil(0, t0 + 500_000, t0 + 100_000))
    }

    @Test fun onlyASittingFromBeforeTheBootIsCutOffByIt() {
        val p = Plan(t0, 0, 600, 0)
        assertEquals(true, p.beganBefore(t0 + 60_000))
        assertEquals(false, p.beganBefore(t0 - 60_000))
    }
}
