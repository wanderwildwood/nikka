package com.wanderwildwood.nikka.ui

import android.content.Context
import android.text.format.DateFormat
import com.wanderwildwood.nikka.R
import com.wanderwildwood.nikka.data.Session
import com.wanderwildwood.nikka.data.Stats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** How lengths, days and streaks are said. Numbers go in as text, as in his other apps. */
object Words {

    /** "20 min", or "40 sec" for a sitting under a minute. */
    fun length(context: Context, seconds: Int): String =
        if (seconds < 60) context.getString(R.string.seconds_short, seconds.toString())
        else context.getString(R.string.minutes_short, (seconds / 60).toString())

    /** "20 min", or "12 min of 20" for one ended early; "40 sec of 20 min" under a minute. */
    fun sat(context: Context, s: Session): String =
        if (!s.endedEarly) length(context, s.satSec)
        else context.getString(
            R.string.sat_of,
            length(context, s.satSec),
            if (s.satSec < 60) length(context, s.plannedSec) else (s.plannedSec / 60).toString(),
        )

    fun streak(context: Context, n: Int, weeks: Boolean): String =
        context.resources.getQuantityString(if (weeks) R.plurals.streak_weeks else R.plurals.streak_days, n, n.toString())

    fun time(context: Context, at: Long): String = DateFormat.getTimeFormat(context).format(at)

    private fun pattern(skeleton: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton))

    /** "Saturday 17 October", the heading over today's habits. */
    fun longDay(day: LocalDate): String = pattern("EEEEdMMMM").format(day)

    /** "Sat 17 Oct". */
    fun shortDay(day: LocalDate): String = pattern("EEEdMMM").format(day)

    /** "October 2026". */
    fun month(day: LocalDate): String = pattern("MMMMyyyy").format(day)

    /** "Sat 17 Oct, 07:20", a sitting in the log. */
    fun whenSat(context: Context, at: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        context.getString(R.string.day_time, shortDay(Stats.dayOf(at, zone)), time(context, at))

    fun instantDay(at: Long): LocalDate = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
}
