package com.wanderwildwood.nikka.sit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathTest {

    @Test fun emptyHoldsAreLeftOut() {
        assertEquals(listOf(Breath.Kind.IN, Breath.Kind.OUT), Breath.EVEN.phases.map { it.kind })
        assertEquals(listOf(Breath.Kind.IN, Breath.Kind.HOLD, Breath.Kind.OUT), Breath.FOUR_SEVEN_EIGHT.phases.map { it.kind })
        assertEquals(4, Breath.BOX.phases.size)
    }

    @Test fun figures() {
        assertEquals("4 · 7 · 8", Breath.FOUR_SEVEN_EIGHT.figure())
        assertEquals("6 · 6", Breath.EVEN.figure())
        assertEquals("4 · 4 · 4 · 4", Breath.BOX.figure())
    }

    @Test fun roundsFillTheLengthAndNeverStopMidBreath() {
        assertEquals(25, Breath.EVEN.rounds(5))            // 300 s / 12 s
        assertEquals(16, Breath.FOUR_SEVEN_EIGHT.rounds(5)) // 300 / 19 = 15.8, so 16
        assertEquals(1, Breath(20, 20, 20, 20).rounds(1))
    }

    @Test fun phaseAtATime() {
        val b = Breath.FOUR_SEVEN_EIGHT
        val start = b.at(0, 2)!!
        assertEquals(Breath.Kind.IN, start.phase.kind)
        assertEquals(4000L, start.leftMs)
        val hold = b.at(4_500, 2)!!
        assertEquals(Breath.Kind.HOLD, hold.phase.kind)
        assertEquals(6500L, hold.leftMs)
        val out = b.at(18_999, 2)!!
        assertEquals(Breath.Kind.OUT, out.phase.kind)
        assertEquals(1L, out.leftMs)
        val second = b.at(19_000, 2)!!
        assertEquals(1, second.round)
        assertEquals(Breath.Kind.IN, second.phase.kind)
    }

    @Test fun overAfterTheLastRound() {
        assertNull(Breath.EVEN.at(24_000, 2))
        assertTrue(Breath.EVEN.at(23_999, 2) != null)
    }

    @Test fun validity() {
        assertTrue(Breath.BOX.valid())
        assertFalse(Breath(0, 4, 4, 0).valid())
        assertFalse(Breath(4, 4, 0, 0).valid())
        assertFalse(Breath(4, 21, 4, 0).valid())
    }
}
