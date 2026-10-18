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
}
