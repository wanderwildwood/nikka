package com.wanderwildwood.nikka.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * The numbers the statistics screen shows, worked out from the days a habit was done. Pure, with
 * "today" and the first day of the week passed in, so every case can be tested without a phone.
 *
 * A streak is counted in days for a daily habit and in weeks for one meant N times a week, since
 * a week of three-times-a-week is kept or broken as a whole.
 */
object Stats {

    /** What is shown for one habit. [streak] and [longest] are days, or weeks when [inWeeks]. */
    data class Summary(
        val streak: Int,
        val longest: Int,
        val inWeeks: Boolean,
        val thisWeek: Int,
        val thisMonth: Int,
        val allTime: Int,
    )

    fun summary(days: Set<LocalDate>, perWeek: Int, today: LocalDate, firstDay: DayOfWeek): Summary {
        val weekStart = startOfWeek(today, firstDay)
        val thisWeek = days.count { !it.isBefore(weekStart) && !it.isAfter(today) }
        val thisMonth = days.count { it.year == today.year && it.month == today.month && !it.isAfter(today) }
        val allTime = days.count { !it.isAfter(today) }
        return if (perWeek >= 7) {
            Summary(dailyStreak(days, today), longestDaily(days, today), false, thisWeek, thisMonth, allTime)
        } else {
            Summary(
                weeklyStreak(days, perWeek, today, firstDay), longestWeekly(days, perWeek, today, firstDay), true,
                thisWeek, thisMonth, allTime,
            )
        }
    }

    /**
     * The days in a row up to today. A day not yet done does not break it until it is over: done
     * yesterday and the day before, not yet today, is a streak of two that today can still make
     * three.
     */
    fun dailyStreak(days: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in days) today else today.minusDays(1)
        var n = 0
        while (day in days) {
            n++
            day = day.minusDays(1)
        }
        return n
    }

    /** The longest run of days in a row, ever, counting nothing after today. */
    fun longestDaily(days: Set<LocalDate>, today: LocalDate): Int {
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in days.filter { !it.isAfter(today) }.sorted()) {
            run = if (prev != null && d == prev.plusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        return best
    }

    /** Whether the week starting [weekStart] was done [perWeek] times or more. */
    private fun kept(days: Set<LocalDate>, weekStart: LocalDate, perWeek: Int): Boolean =
        (0L until 7L).count { weekStart.plusDays(it) in days } >= perWeek

    /**
     * Weeks in a row that were kept. This week counts once it is kept; until then it is still
     * open and the streak is the weeks before it.
     */
    fun weeklyStreak(days: Set<LocalDate>, perWeek: Int, today: LocalDate, firstDay: DayOfWeek): Int {
        val thisWeek = startOfWeek(today, firstDay)
        var week = if (kept(days, thisWeek, perWeek)) thisWeek else thisWeek.minusWeeks(1)
        var n = 0
        while (kept(days, week, perWeek)) {
            n++
            week = week.minusWeeks(1)
        }
        return n
    }

    fun longestWeekly(days: Set<LocalDate>, perWeek: Int, today: LocalDate, firstDay: DayOfWeek): Int {
        val counted = days.filter { !it.isAfter(today) }
        if (counted.isEmpty()) return 0
        var week = startOfWeek(counted.min(), firstDay)
        val last = startOfWeek(today, firstDay)
        var best = 0
        var run = 0
        val set = counted.toSet()
        while (!week.isAfter(last)) {
            run = if (kept(set, week, perWeek)) run + 1 else 0
            best = maxOf(best, run)
            week = week.plusWeeks(1)
        }
        return best
    }

    fun startOfWeek(day: LocalDate, firstDay: DayOfWeek): LocalDate =
        day.with(TemporalAdjusters.previousOrSame(firstDay))

    /**
     * A month laid out as the grid shows it: rows of seven, starting on [firstDay], with null
     * for the cells before the 1st and after the last day.
     */
    fun monthGrid(month: YearMonth, firstDay: DayOfWeek): List<List<LocalDate?>> {
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value - firstDay.value + 7) % 7
        val cells = ArrayList<LocalDate?>()
        repeat(lead) { cells += null }
        for (d in 1..month.lengthOfMonth()) cells += month.atDay(d)
        while (cells.size % 7 != 0) cells += null
        return cells.chunked(7)
    }

    /** The seven days of the week in the grid's order. */
    fun weekDays(firstDay: DayOfWeek): List<DayOfWeek> = (0L until 7L).map { firstDay.plus(it) }

    /** The local day each sitting began on. Any sitting, however short, ticks meditation. */
    fun sittingDays(sessions: List<Session>, zone: ZoneId): Set<LocalDate> =
        sessions.mapTo(HashSet()) { dayOf(it.start, zone) }

    /** Seconds sat on each day. */
    fun secondsByDay(sessions: List<Session>, zone: ZoneId): Map<LocalDate, Int> =
        sessions.groupBy { dayOf(it.start, zone) }.mapValues { (_, s) -> s.sumOf { it.satSec } }

    /** Sittings and seconds sat from [from] to [to], both included. */
    fun sat(sessions: List<Session>, zone: ZoneId, from: LocalDate, to: LocalDate): Pair<Int, Int> {
        val inside = sessions.filter { val d = dayOf(it.start, zone); !d.isBefore(from) && !d.isAfter(to) }
        return inside.size to inside.sumOf { it.satSec }
    }

    fun dayOf(at: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
}
