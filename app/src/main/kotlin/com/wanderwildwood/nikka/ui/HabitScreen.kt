package com.wanderwildwood.nikka.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.nikka.R
import com.wanderwildwood.nikka.data.Stats
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * One habit's history: its current and longest streak, totals for this week, this month and all
 * time, and a month of days as squares, filled for done and outlined for not. Meditation adds
 * the minutes sat on each of the last seven days.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(c: AppState, id: Long, onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val habit = remember(c.version, id) { c.store.habit(id) }
    if (habit == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
        return
    }
    val zone = ZoneId.systemDefault()
    val days = remember(c.version, id) { doneDays(c, habit) }
    val s = remember(days, c.today, habit.perWeek) { Stats.summary(days, habit.perWeek, c.today, c.firstDay) }
    val sessions = remember(c.version, habit.builtin) { if (habit.builtin) c.store.sessions() else emptyList() }
    var month by remember { mutableStateOf(YearMonth.from(c.today)) }
    var editing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = habitName(habit)) },
                navigationIcon = { BarButton(Icons.Back, stringResource(R.string.cd_back), onBack) },
                actions = { BarButton(Icons.Edit, stringResource(R.string.cd_edit)) { editing = true } },
            )
        },
    ) { padding ->
        val state = rememberLazyListState()
        PagedColumn(Modifier.padding(padding).fillMaxSize(), state = state, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 12.dp)) {
            item(key = "streaks") {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    Figure(s.streak.toString(), streakWords(s.streak, s.inWeeks, current = true), Modifier.weight(1f), "streak")
                    Figure(s.longest.toString(), streakWords(s.longest, s.inWeeks, current = false), Modifier.weight(1f), "longest")
                }
                if (!habit.daily) {
                    TextMMD(
                        text = pluralStringResource(R.plurals.times_a_week_goal, habit.perWeek, habit.perWeek.toString()),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            item(key = "totals") {
                Heading(stringResource(R.string.stats_totals))
                val week = Stats.startOfWeek(c.today, c.firstDay)
                val monthStart = c.today.withDayOfMonth(1)
                TotalRow(stringResource(R.string.stats_this_week), s.thisWeek, if (habit.builtin) Stats.sat(sessions, zone, week, c.today) else null)
                TotalRow(stringResource(R.string.stats_this_month), s.thisMonth, if (habit.builtin) Stats.sat(sessions, zone, monthStart, c.today) else null)
                TotalRow(stringResource(R.string.stats_all_time), s.allTime, if (habit.builtin) Stats.sat(sessions, zone, LocalDate.MIN, c.today) else null)
            }
            item(key = "grid") {
                Spacer(Modifier.height(12.dp))
                MonthGrid(
                    month = month,
                    today = c.today,
                    firstDay = c.firstDay,
                    done = days,
                    onMonth = { month = it },
                    onDay = if (habit.builtin) null else { d -> c.store.setTick(habit.id, d, d !in days) },
                )
                if (!habit.builtin) {
                    TextMMD(text = stringResource(R.string.stats_tap_a_day), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
                }
            }
            if (habit.builtin) {
                item(key = "minutes") {
                    Heading(stringResource(R.string.stats_minutes_per_day), Modifier.padding(top = 8.dp))
                    val byDay = Stats.secondsByDay(sessions, zone)
                    val week = (6 downTo 0).map { c.today.minusDays(it.toLong()) }
                    val most = week.maxOf { byDay[it] ?: 0 }.coerceAtLeast(60)
                    for (d in week) MinutesRow(Words.shortDay(d), byDay[d] ?: 0, most)
                }
            }
            if (!habit.builtin) {
                item(key = "delete") {
                    Spacer(Modifier.height(16.dp))
                    ArmedRow(stringResource(R.string.habit_delete), stringResource(R.string.habit_delete_armed)) {
                        c.store.deleteHabit(habit.id)
                        onBack()
                    }
                }
            }
        }
    }
    if (editing) {
        HabitDialog(habit, onSave = { name, perWeek -> c.store.updateHabit(habit.id, if (habit.builtin) "" else name, perWeek) }, onDismiss = { editing = false },
            onDelete = {
                c.store.deleteHabit(habit.id)
                onBack()
            },
        )
    }
}

@Composable
private fun streakWords(n: Int, weeks: Boolean, current: Boolean): String {
    val unit = pluralStringResource(if (weeks) R.plurals.weeks_word else R.plurals.days_word, n)
    return stringResource(if (current) R.string.stats_current else R.string.stats_longest, unit)
}

/** A number shown large, with what it counts beneath. */
@Composable
private fun Figure(value: String, label: String, modifier: Modifier, tag: String) {
    Column(modifier) {
        TextMMD(text = value, fontSize = 44.sp, fontWeight = FontWeight.Medium, modifier = Modifier.testTag(tag))
        TextMMD(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TotalRow(label: String, days: Int, sat: Pair<Int, Int>?) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        val value = if (sat == null) pluralStringResource(R.plurals.days_count, days, days.toString())
        else stringResource(
            R.string.stats_sittings_minutes,
            pluralStringResource(R.plurals.sittings_count, sat.first, sat.first.toString()),
            context.getString(R.string.minutes_short, (sat.second / 60).toString()),
        )
        TextMMD(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

/** A day's minutes as a black bar: no colour needed, no axis to read. */
@Composable
private fun MinutesRow(day: String, seconds: Int, most: Int) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
        TextMMD(text = day, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(96.dp))
        Box(Modifier.weight(1f).fillMaxHeight().padding(vertical = 9.dp)) {
            if (seconds > 0) {
                Box(Modifier.fillMaxWidth((seconds.toFloat() / most).coerceIn(0.03f, 1f)).fillMaxHeight().background(Color.Black))
            }
        }
        TextMMD(
            text = if (seconds == 0) "" else context.getString(R.string.minutes_short, (seconds / 60).toString()),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(64.dp).padding(start = 8.dp),
        )
    }
}

/**
 * A month as squares, one per day: filled where the habit was done, outlined where it was not,
 * and nothing drawn for days still to come. Today's square has a heavier edge. [onDay] ticks a
 * past day by hand; null where the days are ticked by something else.
 */
@Composable
fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    firstDay: DayOfWeek,
    done: Set<LocalDate>,
    onMonth: (YearMonth) -> Unit,
    onDay: ((LocalDate) -> Unit)?,
) {
    val thisMonth = YearMonth.from(today)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BarButton(Icons.ChevronLeft, stringResource(R.string.cd_previous_month)) { onMonth(month.minusMonths(1)) }
            TextMMD(
                text = Words.month(month.atDay(1)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).testTag("grid_month"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (month < thisMonth) {
                BarButton(Icons.ChevronRight, stringResource(R.string.cd_next_month)) { onMonth(month.plusMonths(1)) }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            for (d in Stats.weekDays(firstDay)) {
                TextMMD(
                    text = d.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                )
            }
        }
        for (week in Stats.monthGrid(month, firstDay)) {
            Row(Modifier.fillMaxWidth()) {
                for (day in week) {
                    Box(Modifier.weight(1f).height(48.dp)) {
                        if (day != null) DayCell(day, today, day in done, onDay)
                    }
                }
            }
        }
    }
}


@Composable
private fun DayCell(day: LocalDate, today: LocalDate, done: Boolean, onDay: ((LocalDate) -> Unit)?) {
    val shape = RoundedCornerShape(4.dp)
    val future = day.isAfter(today)
    val state = stringResource(if (done) R.string.day_done else R.string.day_not_done)
    // The whole 48 dp cell is the target; the square drawn inside it is inset so squares keep their gap.
    Box(
        Modifier
            .fillMaxSize()
            .let { if (onDay != null && !future) it.clickable { onDay(day) } else it }
            .semantics { if (!future) stateDescription = state }
            .testTag("day_$day")
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    when {
                        future -> Modifier
                        done -> Modifier.background(Color.Black, shape)
                        else -> Modifier.border(if (day == today) 3.dp else 1.5.dp, Color.Black, shape)
                    },
                )
                .then(if (done && day == today) Modifier.border(3.dp, Color.Black, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            TextMMD(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (done) Color.White else Color.Black,
            )
        }
    }
}
