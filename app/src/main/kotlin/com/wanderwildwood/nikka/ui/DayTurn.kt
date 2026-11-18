package com.wanderwildwood.nikka.ui

import java.time.Duration
import java.time.ZonedDateTime

/** Milliseconds from [now] to the start of the next day where it is, a second over to be past it. */
internal fun msToNextDay(now: ZonedDateTime): Long {
    val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, next).toMillis().coerceAtLeast(0L) + 1_000L
}
