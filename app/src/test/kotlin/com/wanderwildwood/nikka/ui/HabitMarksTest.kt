package com.wanderwildwood.nikka.ui

import com.wanderwildwood.nikka.data.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class HabitMarksTest {

    private val today = LocalDate.of(2026, 10, 17)
    private val zone = ZoneId.of("America/New_York")

    @Test fun aDayIsDoneRestedOrNeither() {
        val done = setOf(today)
        val rest = setOf(today.minusDays(1))
        assertEquals(DayMark.DONE, markOf(today, done, rest))
        assertEquals(DayMark.RESTED, markOf(today.minusDays(1), done, rest))
        assertEquals(DayMark.NONE, markOf(today.minusDays(2), done, rest))
        // Both should never happen; done wins if it does.
        assertEquals(DayMark.DONE, markOf(today, done, setOf(today)))
    }

    @Test fun aHabitYouMadeGoesRoundThree() {
        var m = DayMark.NONE
        m = nextMark(m, builtin = false)!!
        assertEquals(DayMark.DONE, m)
        m = nextMark(m, builtin = false)!!
        assertEquals(DayMark.RESTED, m)
        m = nextMark(m, builtin = false)!!
        assertEquals(DayMark.NONE, m)
    }

    @Test fun meditationOnlyRestsAndASittingStays() {
        assertEquals(DayMark.RESTED, nextMark(DayMark.NONE, builtin = true))
        assertEquals(DayMark.NONE, nextMark(DayMark.RESTED, builtin = true))
        assertNull(nextMark(DayMark.DONE, builtin = true))
    }

    @Test fun cueStartsWithACapital() {
        assertEquals("After breakfast", cueLabel("after breakfast", Locale.US))
        assertEquals("After breakfast", cueLabel("  after breakfast ", Locale.US))
        assertEquals("Before bed, with tea", cueLabel("Before bed, with tea", Locale.US))
        assertEquals("", cueLabel("   ", Locale.US))
    }

    private fun summary(streak: Int, thisWeek: Int) = Stats.Summary(streak, streak, false, thisWeek, 0, 0)

    @Test fun aRunShowsOnceItIsTwoDays() {
        val r = Stats.Recent(18, 0, 30)
        assertEquals(Rate.Streak(4, false), rateFor(true, true, 7, summary(4, 2), r))
        assertEquals(Rate.Recent(18, 30), rateFor(true, true, 7, summary(1, 1), r))
    }

    @Test fun neverAZero() {
        assertEquals(Rate.Recent(18, 30), rateFor(true, true, 7, summary(0, 0), Stats.Recent(18, 0, 30)))
        assertNull(rateFor(true, true, 7, summary(0, 0), Stats.Recent(0, 3, 30)))
        assertNull(rateFor(false, true, 7, summary(0, 0), Stats.Recent(0, 0, 30)))
        // A weekly habit not yet done this week falls back to the last 30.
        assertEquals(Rate.Recent(5, 30), rateFor(true, false, 3, summary(0, 0), Stats.Recent(5, 0, 30)))
    }

    @Test fun weeklyHabitShowsThisWeek() {
        assertEquals(Rate.Week(2, 3), rateFor(true, false, 3, summary(0, 2), Stats.Recent(9, 0, 30)))
    }

    @Test fun streaksHiddenShowOnlyTheLast30() {
        assertEquals(Rate.Recent(18, 30), rateFor(false, true, 7, summary(9, 3), Stats.Recent(18, 1, 30)))
        assertEquals(Rate.Recent(9, 30), rateFor(false, false, 3, summary(2, 2), Stats.Recent(9, 0, 30)))
    }

    @Test fun breathingPatternSkipsEmptyHolds() {
        assertEquals("4 · 7 · 8", breathFigure("4,7,8,0"))
        assertEquals("6 · 6", breathFigure("6,0,6,0"))
        assertEquals("4 · 4 · 4 · 4", breathFigure("4,4,4,4"))
        assertNull(breathFigure(""))
        assertNull(breathFigure("4,x,8"))
        assertNull(breathFigure("0,0,0,0"))
    }

    @Test fun twelveHourClock() {
        assertEquals(0, to24(12, pm = false))
        assertEquals(12, to24(12, pm = true))
        assertEquals(7, to24(7, pm = false))
        assertEquals(19, to24(7, pm = true))
        assertEquals(12 to false, to12(0))
        assertEquals(12 to true, to12(12))
        assertEquals(7 to true, to12(19))
        for (h in 0..23) assertEquals(h, to24(to12(h).first, to12(h).second))
    }

    private fun at(h: Int, m: Int, day: LocalDate = today) =
        LocalDateTime.of(day, java.time.LocalTime.of(h, m)).atZone(zone).toInstant().toEpochMilli()

    @Test fun handStartReadsTheClockInUse() {
        val now = at(23, 0)
        assertEquals(at(7, 20), handStart(today, "7", "20", is24 = true, pm = false, now = now, zone = zone))
        assertEquals(at(19, 5), handStart(today, "19", "05", is24 = true, pm = false, now = now, zone = zone))
        assertEquals(at(19, 5), handStart(today, "7", "5", is24 = false, pm = true, now = now, zone = zone))
        assertEquals(at(7, 0), handStart(today, "7", "", is24 = false, pm = false, now = now, zone = zone))
    }

    @Test fun handStartRefusesWhatIsNotATime() {
        val now = at(23, 0)
        assertNull(handStart(today, "", "20", true, false, now, zone))
        assertNull(handStart(today, "24", "00", true, false, now, zone))
        assertNull(handStart(today, "0", "00", false, false, now, zone))
        assertNull(handStart(today, "13", "00", false, false, now, zone))
        assertNull(handStart(today, "7", "60", true, false, now, zone))
    }

    @Test fun handStartIsNeverLaterThanNow() {
        val now = at(7, 0)
        assertNull(handStart(today, "7", "01", true, false, now, zone))
        assertEquals(at(7, 0), handStart(today, "7", "00", true, false, now, zone))
        assertEquals(at(23, 0, today.minusDays(1)), handStart(today.minusDays(1), "23", "00", true, false, now, zone))
    }
}
