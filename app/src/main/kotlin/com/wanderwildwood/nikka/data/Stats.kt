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

    fun summary(
        days: Set<LocalDate>, perWeek: Int, today: LocalDate, firstDay: DayOfWeek,
        rest: Set<LocalDate> = emptySet(),
    ): Summary {
        val weekStart = startOfWeek(today, firstDay)
        val thisWeek = days.count { !it.isBefore(weekStart) && !it.isAfter(today) }
        val thisMonth = days.count { it.year == today.year && it.month == today.month && !it.isAfter(today) }
        val allTime = days.count { !it.isAfter(today) }
        return if (perWeek >= 7) {
            Summary(dailyStreak(days, today, rest), longestDaily(days, today, rest), false, thisWeek, thisMonth, allTime)
        } else {
            Summary(
                weeklyStreak(days, perWeek, today, firstDay, rest), longestWeekly(days, perWeek, today, firstDay, rest), true,
                thisWeek, thisMonth, allTime,
            )
        }
    }

    /**
     * The days in a row up to today. A day not yet done does not break it until it is over: done
     * yesterday and the day before, not yet today, is a streak of two that today can still make
     * three. A rest day, marked on purpose, is passed over: it neither breaks the run nor adds
     * to it.
     */
    fun dailyStreak(days: Set<LocalDate>, today: LocalDate, rest: Set<LocalDate> = emptySet()): Int {
        var day = if (today in days) today else today.minusDays(1)
        var n = 0
        while (day in days || day in rest) {
            if (day in days) n++
            day = day.minusDays(1)
        }
        return n
    }

    /** The longest run of days in a row, ever, counting nothing after today; rest days bridge it. */
    fun longestDaily(days: Set<LocalDate>, today: LocalDate, rest: Set<LocalDate> = emptySet()): Int {
        val counted = days.filter { !it.isAfter(today) }
        if (counted.isEmpty()) return 0
        var best = 0
        var run = 0
        var d = counted.min()
        while (!d.isAfter(today)) {
            when {
                d in days -> { run++; best = maxOf(best, run) }
                d in rest -> Unit
                else -> run = 0
            }
            d = d.plusDays(1)
        }
        return best
    }

    private fun done(days: Set<LocalDate>, weekStart: LocalDate): Int = (0L until 7L).count { weekStart.plusDays(it) in days }
    private fun rested(rest: Set<LocalDate>, weekStart: LocalDate): Int = (0L until 7L).count { weekStart.plusDays(it) in rest }

    /** Whether the week starting [weekStart] was done [perWeek] times or more. */
    private fun kept(days: Set<LocalDate>, weekStart: LocalDate, perWeek: Int): Boolean = done(days, weekStart) >= perWeek

    /** Short of the mark only by the days rested: such a week neither breaks a run nor adds to it. */
    private fun bridged(days: Set<LocalDate>, rest: Set<LocalDate>, weekStart: LocalDate, perWeek: Int): Boolean =
        done(days, weekStart) + rested(rest, weekStart) >= perWeek

    /**
     * Weeks in a row that were kept. This week counts once it is kept; until then it is still
     * open and the streak is the weeks before it.
     */
    fun weeklyStreak(
        days: Set<LocalDate>, perWeek: Int, today: LocalDate, firstDay: DayOfWeek, rest: Set<LocalDate> = emptySet(),
    ): Int {
        val thisWeek = startOfWeek(today, firstDay)
        var week = if (kept(days, thisWeek, perWeek)) thisWeek else thisWeek.minusWeeks(1)
        var n = 0
        while (true) {
            when {
                kept(days, week, perWeek) -> n++
                bridged(days, rest, week, perWeek) -> Unit
                else -> return n
            }
            week = week.minusWeeks(1)
        }
    }

    fun longestWeekly(
        days: Set<LocalDate>, perWeek: Int, today: LocalDate, firstDay: DayOfWeek, rest: Set<LocalDate> = emptySet(),
    ): Int {
        val counted = days.filter { !it.isAfter(today) }
        if (counted.isEmpty()) return 0
        var week = startOfWeek(counted.min(), firstDay)
        val last = startOfWeek(today, firstDay)
        var best = 0
        var run = 0
        val set = counted.toSet()
        while (!week.isAfter(last)) {
            when {
                kept(set, week, perWeek) -> { run++; best = maxOf(best, run) }
                bridged(set, rest, week, perWeek) || week == last -> Unit
                else -> run = 0
            }
            week = week.plusWeeks(1)
        }
        return best
    }

    /**
     * The gentler measure beside the streak: of the last [span] days up to today, how many were
     * done and how many rested. A missed day costs one day here, not the whole run.
     */
    data class Recent(val done: Int, val rested: Int, val span: Int)

    fun recent(days: Set<LocalDate>, today: LocalDate, rest: Set<LocalDate> = emptySet(), span: Int = 30): Recent {
        val from = today.minusDays(span - 1L)
        fun inside(d: LocalDate) = !d.isBefore(from) && !d.isAfter(today)
        return Recent(days.count(::inside), rest.count { inside(it) && it !in days }, span)
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

    /**
     * The local day each sitting began on. Any sitting, however short, ticks meditation; a
     * breathing session is logged beside them but does not (decided 2026-11-04).
     */
    fun sittingDays(sessions: List<Session>, zone: ZoneId): Set<LocalDate> =
        sessions.filter { !it.breathing }.mapTo(HashSet()) { dayOf(it.start, zone) }

    /** Seconds sat on each day, sittings only. */
    fun secondsByDay(sessions: List<Session>, zone: ZoneId): Map<LocalDate, Int> =
        sessions.filter { !it.breathing }.groupBy { dayOf(it.start, zone) }.mapValues { (_, s) -> s.sumOf { it.satSec } }

    /** Sittings and seconds sat from [from] to [to], both included. */
    fun sat(sessions: List<Session>, zone: ZoneId, from: LocalDate, to: LocalDate): Pair<Int, Int> {
        val inside = sessions.filter { val d = dayOf(it.start, zone); !it.breathing && !d.isBefore(from) && !d.isAfter(to) }
        return inside.size to inside.sumOf { it.satSec }
    }

    fun dayOf(at: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
}
