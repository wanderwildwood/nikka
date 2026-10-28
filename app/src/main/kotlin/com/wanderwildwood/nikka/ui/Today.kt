package com.wanderwildwood.nikka.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.nikka.R
import com.wanderwildwood.nikka.data.Habit
import com.wanderwildwood.nikka.data.Stats
import java.time.LocalDate
import java.time.ZoneId

/** The days a habit was done: ticked by hand, or for meditation, any day with a sitting. */
fun doneDays(c: AppState, h: Habit): Set<LocalDate> =
    if (h.builtin) Stats.sittingDays(c.store.sessions(), ZoneId.systemDefault()) else c.store.ticks(h.id)

/**
 * Every habit with a box for today. Meditation's box is ticked by a sitting, not by hand, so a
 * press on it goes to the Sit tab; a press on any row opens its history.
 */
@Composable
fun TodayTab(c: AppState, onHabit: (Long) -> Unit, onSit: () -> Unit) {
    val context = LocalContext.current
    val rows = remember(c.version, c.today) {
        c.store.habits().map { h ->
            val days = doneDays(c, h)
            Triple(h, c.today in days, Stats.summary(days, h.perWeek, c.today, c.firstDay))
        }
    }
    var adding by remember { mutableStateOf(false) }
    val state = rememberLazyListState()
    PagedColumn(Modifier.fillMaxSize(), state = state) {
        item(key = "day") {
            Heading(Words.longDay(c.today), Modifier.padding(horizontal = 20.dp))
        }
        for ((h, done, s) in rows) {
            item(key = h.id) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onHabit(h.id) }
                        .testTag("habit_${h.id}")
                        .padding(start = 20.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        TextMMD(text = habitName(h), style = MaterialTheme.typography.titleMedium)
                        val sub = when {
                            !h.daily -> stringResource(R.string.today_of_week, s.thisWeek.toString(), h.perWeek.toString())
                            s.streak > 1 -> Words.streak(context, s.streak, weeks = false)
                            else -> null
                        }
                        if (sub != null) TextMMD(text = sub, style = MaterialTheme.typography.labelSmall)
                    }
                    val press = { if (h.builtin) onSit() else c.store.setTick(h.id, c.today, !done) }
                    // CheckboxMMD takes every press on itself even with no handler, so the
                    // handler goes to it as well as to the margin round it.
                    Box(
                        Modifier
                            .size(56.dp)
                            .clickable(onClick = press)
                            .testTag("tick_${h.id}"),
                        contentAlignment = Alignment.Center,
                    ) { CheckboxMMD(checked = done, onCheckedChange = { press() }) }
                    Spacer(Modifier.width(8.dp))
                }
                HorizontalDividerMMD(thickness = 1.dp)
            }
        }
        item(key = "add") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { adding = true }
                    .testTag("add_habit")
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                TextMMD(text = stringResource(R.string.today_add), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    if (adding) HabitDialog(null, onSave = { name, perWeek -> c.store.addHabit(name, perWeek) }, onDismiss = { adding = false })
}

@Composable
fun habitName(h: Habit): String = if (h.builtin) stringResource(R.string.meditation) else h.name

/** A new habit, or a change to one: its name, and every day or some days a week. */
@Composable
fun HabitDialog(habit: Habit?, onSave: (String, Int) -> Unit, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null) {
    var name by remember { mutableStateOf(habit?.name.orEmpty()) }
    var daily by remember { mutableStateOf(habit?.daily ?: true) }
    var perWeek by remember { mutableStateOf(habit?.perWeek?.takeIf { it < 7 } ?: 3) }
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(
            text = when {
                habit == null -> stringResource(R.string.habit_new)
                habit.builtin -> stringResource(R.string.meditation)
                else -> stringResource(R.string.habit_edit)
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        if (habit?.builtin != true) {
            Field(stringResource(R.string.habit_name), name, { name = it.replace('\n', ' ').take(60) }, hint = stringResource(R.string.habit_name_hint))
        }
        Spacer(Modifier.height(8.dp))
        ChoiceRow(stringResource(R.string.habit_daily), daily) { daily = true }
        ChoiceRow(stringResource(R.string.habit_weekly), !daily) { daily = false }
        if (!daily) {
            Row(Modifier.fillMaxWidth().padding(start = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                StepButton("−", enabled = perWeek > 1) { perWeek-- }
                TextMMD(
                    text = stringResource(R.string.habit_times_a_week, perWeek.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("per_week"),
                )
                StepButton("+", enabled = perWeek < 6) { perWeek++ }
            }
        }
        Spacer(Modifier.height(12.dp))
        val ok = habit?.builtin == true || name.isNotBlank()
        DialogButton(stringResource(R.string.save), filled = true) {
            if (ok) {
                onSave(name.trim(), if (daily) 7 else perWeek)
                onDismiss()
            }
        }
        DialogButton(stringResource(R.string.cancel), onClick = onDismiss)
        if (habit != null && !habit.builtin && onDelete != null) {
            ArmedRow(stringResource(R.string.habit_delete), stringResource(R.string.habit_delete_armed)) {
                onDelete()
                onDismiss()
            }
        }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).let { if (enabled) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        )
    }
}
