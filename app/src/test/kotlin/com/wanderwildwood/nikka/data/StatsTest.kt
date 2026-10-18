package com.wanderwildwood.nikka.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class StatsTest {

    // Saturday 17 October 2026.
    private val today = LocalDate.of(2026, 10, 17)
    private fun ago(n: Long) = today.minusDays(n)
    private fun days(vararg back: Long) = back.map(::ago).toSet()

    @Test fun noDaysIsNoStreak() {
        assertEquals(0, Stats.dailyStreak(emptySet(), today))
        assertEquals(0, Stats.longestDaily(emptySet(), today))
    }

    @Test fun streakRunsBackFromToday() {
        assertEquals(3, Stats.dailyStreak(days(0, 1, 2, 4), today))
    }

    @Test fun todayNotYetDoneDoesNotBreakIt() {
        assertEquals(2, Stats.dailyStreak(days(1, 2), today))
    }

    @Test fun aMissedYesterdayBreaksIt() {
        assertEquals(0, Stats.dailyStreak(days(2, 3, 4), today))
        assertEquals(1, Stats.dailyStreak(days(0, 2, 3, 4), today))
    }

    @Test fun longestFindsTheBestRunAnywhere() {
        assertEquals(4, Stats.longestDaily(days(0, 1, 5, 6, 7, 8, 20), today))
    }

    @Test fun futureDaysCountForNothing() {
        val d = days(0, 1) + today.plusDays(1) + today.plusDays(2)
        assertEquals(2, Stats.dailyStreak(d, today))
        assertEquals(2, Stats.longestDaily(d, today))
        assertEquals(2, Stats.summary(d, 7, today, DayOfWeek.MONDAY).allTime)
    }

    @Test fun streakCrossesAMonthAndAYear() {
        val newYear = LocalDate.of(2027, 1, 2)
        val d = setOf(LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1), newYear)
        assertEquals(4, Stats.dailyStreak(d, newYear))
    }

    // Weeks starting Monday: this week is Mon 12 to Sun 18 October.
    @Test fun weeklyStreakCountsKeptWeeks() {
        val mon = DayOfWeek.MONDAY
        // three times a week: this week (12, 14, 16), last week (5, 6, 9), the week before twice only
        val d = setOf(12, 14, 16, 5, 6, 9).map { LocalDate.of(2026, 10, it) }.toSet() +
            setOf(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 10, 1))
        assertEquals(2, Stats.weeklyStreak(d, 3, today, mon))
    }

    @Test fun anOpenWeekDoesNotBreakAWeeklyStreak() {
        val mon = DayOfWeek.MONDAY
        // this week once so far; the two weeks before kept at twice
        val d = setOf(13, 6, 8, 1, 2).map { LocalDate.of(2026, 10, it) }.toSet() +
            setOf(LocalDate.of(2026, 9, 29))
        assertEquals(2, Stats.weeklyStreak(d, 2, today, mon))
        assertEquals(2, Stats.longestWeekly(d, 2, today, mon))
    }

    @Test fun weekStartFollowsTheLocale() {
        // Sunday 11 October and Saturday 17 are one week from Sunday, two from Monday.
        val d = setOf(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 17))
        assertEquals(1, Stats.weeklyStreak(d, 2, today, DayOfWeek.SUNDAY))
        assertEquals(0, Stats.weeklyStreak(d, 2, today, DayOfWeek.MONDAY))
    }

    @Test fun summaryTotals() {
        val d = setOf(17, 16, 12, 11, 1).map { LocalDate.of(2026, 10, it) }.toSet() + LocalDate.of(2026, 9, 30)
        val s = Stats.summary(d, 7, today, DayOfWeek.MONDAY)
        assertEquals(3, s.thisWeek)     // 12, 16, 17
        assertEquals(5, s.thisMonth)
        assertEquals(6, s.allTime)
        assertEquals(2, s.streak)
        assertEquals(false, s.inWeeks)
    }

    @Test fun octoberGridFromMonday() {
        val g = Stats.monthGrid(YearMonth.of(2026, 10), DayOfWeek.MONDAY)
        // 1 October 2026 is a Thursday: three blanks first.
        assertEquals(5, g.size)
        g.forEach { assertEquals(7, it.size) }
        assertNull(g[0][2])
        assertEquals(LocalDate.of(2026, 10, 1), g[0][3])
        assertEquals(LocalDate.of(2026, 10, 31), g[4][5])
        assertNull(g[4][6])
    }

    @Test fun gridFromSunday() {
        val g = Stats.monthGrid(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 10, 1), g[0][4])
        assertEquals(31, g.flatten().count { it != null })
    }

    @Test fun aFebruaryThatFitsFourRows() {
        // February 2027 starts on a Monday and has 28 days.
        assertEquals(4, Stats.monthGrid(YearMonth.of(2027, 2), DayOfWeek.MONDAY).size)
    }

    @Test fun aMonthThatNeedsSixRows() {
        // August 2026 starts on a Saturday and has 31 days.
        assertEquals(6, Stats.monthGrid(YearMonth.of(2026, 8), DayOfWeek.MONDAY).size)
    }

    @Test fun weekDaysInOrder() {
        assertEquals(DayOfWeek.SUNDAY, Stats.weekDays(DayOfWeek.SUNDAY)[0])
        assertEquals(DayOfWeek.SATURDAY, Stats.weekDays(DayOfWeek.SUNDAY)[6])
    }

    @Test fun sittingsTickTheirLocalDay() {
        val zone = ZoneId.of("America/New_York")
        fun at(y: Int, m: Int, d: Int, h: Int) = LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()
        val sessions = listOf(
            Session(1, at(2026, 10, 16, 23), 1200, 1200, ""),   // late on the 16th, local
            Session(2, at(2026, 10, 17, 6), 600, 300, ""),      // ended early, still counts
            Session(3, at(2026, 10, 17, 19), 900, 900, ""),
        )
        assertEquals(setOf(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 17)), Stats.sittingDays(sessions, zone))
        assertEquals(1200, Stats.secondsByDay(sessions, zone)[LocalDate.of(2026, 10, 17)])
        assertEquals(2 to 1200, Stats.sat(sessions, zone, LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 17)))
    }
}
