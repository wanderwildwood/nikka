package com.wanderwildwood.nikka.ui

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DayTurnTest {

    private val zone = ZoneId.of("America/New_York")

    @Test
    fun anHourBeforeMidnight() {
        val now = ZonedDateTime.of(2026, 3, 1, 23, 0, 0, 0, zone)
        assertEquals(3_600_000L + 1_000L, msToNextDay(now))
    }

    @Test
    fun justAfterMidnightWaitsForTheNextOne() {
        val now = ZonedDateTime.of(2026, 3, 1, 0, 0, 1, 0, zone)
        assertEquals(86_399_000L + 1_000L, msToNextDay(now))
    }

    @Test
    fun aDayWithTheClocksPutForwardIsShorter() {
        // 8 March 2026: clocks go from 2:00 to 3:00 in New York, so the day has 23 hours.
        val now = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, zone)
        assertEquals(23 * 3_600_000L + 1_000L, msToNextDay(now))
    }
}
