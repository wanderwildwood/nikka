package com.wanderwildwood.nikka.ui

import com.wanderwildwood.nikka.data.Stats
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/*
 * The small decisions behind the habit and log screens, kept apart from Compose so they can be
 * tested without a phone: what a day is marked, what a press on it does next, which line goes
 * under a habit, and how a sitting added by hand gets its start.
 */

/** A day of one habit: not marked, done, or rested on purpose. Never both done and rested. */
enum class DayMark { NONE, DONE, RESTED }

fun markOf(day: LocalDate, done: Set<LocalDate>, rest: Set<LocalDate>): DayMark = when (day) {
    in done -> DayMark.DONE
    in rest -> DayMark.RESTED
    else -> DayMark.NONE
}

/**
 * What a press on a day in the month grid turns it into. A habit you made goes round empty, done,
 * rested, empty. Meditation is done by sittings, not by hand, so a press only marks or clears a
 * rest, and a day with a sitting does not change (null).
 */
fun nextMark(mark: DayMark, builtin: Boolean): DayMark? =
    if (builtin) when (mark) {
        DayMark.NONE -> DayMark.RESTED
        DayMark.RESTED -> DayMark.NONE
        DayMark.DONE -> null
    } else when (mark) {
        DayMark.NONE -> DayMark.DONE
        DayMark.DONE -> DayMark.RESTED
        DayMark.RESTED -> DayMark.NONE
    }

/** A cue as the subtitle starts with it: as typed, its first letter made a capital. */
fun cueLabel(cue: String, locale: Locale = Locale.getDefault()): String {
    val t = cue.trim()
    if (t.isEmpty()) return ""
    return t.substring(0, 1).uppercase(locale) + t.substring(1)
}

/** What the line under a habit on Today says, beside its cue. */
sealed interface Rate {
    data class Streak(val n: Int, val weeks: Boolean) : Rate
    data class Week(val done: Int, val of: Int) : Rate
    data class Recent(val done: Int, val span: Int) : Rate
}

/**
 * The line under a habit. With streaks shown: a daily habit's run once it is two days or more,
 * a weekly habit's days so far this week once there is one. Otherwise, and always with streaks
 * hidden, the days done in the last 30. Never a zero: a run that has ended, or a habit not done
 * at all lately, shows nothing rather than a 0 to feel bad about.
 */
fun rateFor(showStreaks: Boolean, daily: Boolean, perWeek: Int, s: Stats.Summary, recent: Stats.Recent): Rate? {
    if (showStreaks) {
        if (daily && s.streak > 1) return Rate.Streak(s.streak, weeks = false)
        if (!daily && s.thisWeek > 0) return Rate.Week(s.thisWeek, perWeek)
    }
    return if (recent.done > 0) Rate.Recent(recent.done, recent.span) else null
}

/**
 * A breathing session's pattern as the log shows it: "4,7,8,0" is "4 · 7 · 8", the empty holds
 * left out. Null when the pattern cannot be read.
 */
fun breathFigure(pattern: String): String? {
    val parts = pattern.split(',').map { it.trim().toIntOrNull() ?: return null }
    if (parts.isEmpty()) return null
    val shown = parts.filter { it > 0 }
    return if (shown.isEmpty()) null else shown.joinToString(" · ")
}

// ---- a sitting added by hand

/** An hour on a 12-hour clock (1 to 12) with AM or PM, as 0 to 23. */
fun to24(hour12: Int, pm: Boolean): Int = hour12 % 12 + if (pm) 12 else 0

/** 0 to 23 as a 12-hour clock: (1 to 12, PM). */
fun to12(hour24: Int): Pair<Int, Boolean> = (if (hour24 % 12 == 0) 12 else hour24 % 12) to (hour24 >= 12)

/**
 * The start of a sitting from what was typed, or null if it does not make one: an hour and
 * minute in range for the clock in use, on [day], not after [now].
 */
fun handStart(day: LocalDate, hour: String, minute: String, is24: Boolean, pm: Boolean, now: Long, zone: ZoneId): Long? {
    val h = hour.trim().toIntOrNull() ?: return null
    val m = minute.trim().ifEmpty { "0" }.toIntOrNull() ?: return null
    if (m !in 0..59) return null
    val h24 = if (is24) h.takeIf { it in 0..23 } ?: return null else if (h in 1..12) to24(h, pm) else return null
    val at = LocalDateTime.of(day, java.time.LocalTime.of(h24, m)).atZone(zone).toInstant().toEpochMilli()
    return if (at > now) null else at
}

/** The longest a sitting may be: ten hours, as on the Sit tab. */
const val HAND_MAX_MIN = 600
