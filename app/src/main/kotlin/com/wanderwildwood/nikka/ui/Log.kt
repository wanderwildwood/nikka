package com.wanderwildwood.nikka.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.nikka.R
import com.wanderwildwood.nikka.data.Prefs
import com.wanderwildwood.nikka.data.Session
import com.wanderwildwood.nikka.data.Stats
import java.time.Instant
import java.time.ZoneId

/** What a session's second line says: its length, and for breathing, the pattern first. */
fun sessionLine(context: Context, s: Session): String {
    if (!s.breathing) return Words.sat(context, s)
    val length = Words.length(context, s.satSec)
    val figure = breathFigure(s.pattern)
    return if (figure == null) context.getString(R.string.log_breathing, length)
    else context.getString(R.string.log_breathing_pattern, figure, length)
}

/**
 * Every sitting and breathing session, newest first, under a row to add a sitting by hand (a
 * retreat, a phone left at home). A press on one opens it to change its day, time, length or
 * note, or delete it.
 */
@Composable
fun LogTab(c: AppState) {
    val context = LocalContext.current
    val sessions = remember(c.version) { c.store.sessions() }
    var open by remember { mutableStateOf<Session?>(null) }
    var adding by remember { mutableStateOf(false) }
    val state = rememberLazyListState()
    PagedColumn(Modifier.fillMaxSize(), state = state, showCutRow = true) {
        // At the top, not the foot: the log runs newest first, and its foot can be pages away.
        item(key = "add") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { adding = true }
                    .testTag("add_sitting")
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                TextMMD(text = stringResource(R.string.log_add), style = MaterialTheme.typography.bodyMedium)
            }
            DottedRule(start = 20.dp, end = 20.dp)
        }
        if (sessions.isEmpty()) {
            item(key = "empty") {
                TextMMD(
                    text = stringResource(R.string.log_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
        for (s in sessions) {
            item(key = s.id) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { open = s }
                        .testTag("session_${s.id}")
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    TextMMD(text = Words.whenSat(context, s.start), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    TextMMD(text = sessionLine(context, s), style = MaterialTheme.typography.bodyMedium)
                    if (s.note.isNotBlank()) TextMMD(text = s.note, style = MaterialTheme.typography.labelSmall)
                }
                DottedRule(start = 20.dp, end = 20.dp)
            }
        }
    }
    if (adding) SessionDialog(c, null, onDismiss = { adding = false })
    open?.let { s -> SessionDialog(c, s, onDismiss = { open = null }) }
}

/**
 * A sitting added by hand ([s] null), or a change to one already logged: its day (a stepper,
 * never past today), the time it began on the phone's own 12- or 24-hour clock, its length and
 * a note. A new sitting is logged as sat in full, and ticks meditation for its day.
 */
@Composable
private fun SessionDialog(c: AppState, s: Session?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val is24 = remember { DateFormat.is24HourFormat(context) }
    val began = remember { Instant.ofEpochMilli(s?.start ?: System.currentTimeMillis()).atZone(zone).toLocalDateTime() }
    val (h12, pm0) = to12(began.hour)
    var day by remember { mutableStateOf(began.toLocalDate()) }
    var hour by remember { mutableStateOf((if (is24) began.hour else h12).toString()) }
    var minute by remember { mutableStateOf("%02d".format(began.minute)) }
    var pm by remember { mutableStateOf(pm0) }
    // A new one starts at the length the Sit tab is set to: a real figure, not a hint that looks
    // like one and is refused on Save.
    var minutes by remember { mutableStateOf((s?.satSec?.div(60) ?: Prefs(context).minutes).toString()) }
    var note by remember { mutableStateOf(s?.note.orEmpty()) }
    var problem by remember { mutableStateOf<String?>(null) }
    EInkDialog(onDismiss = onDismiss) {
        // Its own scope: the dialog is a window of its own, so a touch elsewhere in it disarms.
        ArmedScope(Modifier.fillMaxWidth()) {
            val delete = rememberArmed()
            // Scrolls only when the keyboard leaves it too little room.
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                TextMMD(
                    text = when {
                        s == null -> stringResource(R.string.log_add_title)
                        s.breathing -> sessionLine(context, s)
                        else -> stringResource(R.string.log_edit_title)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (s != null && s.endedEarly && !s.breathing) {
                    TextMMD(
                        text = stringResource(R.string.log_planned, context.getString(R.string.minutes_short, (s.plannedSec / 60).toString())),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    BarButton(Icons.ChevronLeft, stringResource(R.string.cd_previous_day)) { day = day.minusDays(1) }
                    TextMMD(
                        text = if (day == c.today) stringResource(R.string.log_today) else Words.shortDay(day),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).testTag("hand_day"),
                    )
                    if (day.isBefore(c.today)) {
                        BarButton(Icons.ChevronRight, stringResource(R.string.cd_next_day)) { day = day.plusDays(1) }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Field(stringResource(R.string.log_hour), hour, { hour = it.filter(Char::isDigit).take(2) }, number = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Field(stringResource(R.string.log_minute), minute, { minute = it.filter(Char::isDigit).take(2) }, number = true, modifier = Modifier.weight(1f))
                    if (!is24) {
                        Spacer(Modifier.width(8.dp))
                        // One press turns AM to PM and back.
                        HouseButton(
                            stringResource(if (pm) R.string.log_pm else R.string.log_am),
                            { pm = !pm },
                            Modifier.padding(bottom = 6.dp).testTag("hand_ampm"),
                        )
                    }
                }
                Field(
                    stringResource(R.string.log_length), minutes, { minutes = it.filter(Char::isDigit).take(3) }, number = true,
                )
                Field(stringResource(R.string.end_note), note, { note = it.replace('\n', ' ').take(200) }, hint = stringResource(R.string.end_note_hint))
                problem?.let { TextMMD(text = it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(8.dp))
                DialogButtons(
                    cancel = stringResource(R.string.cancel),
                    confirm = stringResource(R.string.save),
                    onCancel = onDismiss,
                    confirmOn = true,
                    onConfirm = {
                        val start = handStart(day, hour, minute, is24, pm, Long.MAX_VALUE, zone)
                        val m = minutes.trim().toIntOrNull()
                        problem = when {
                            start == null -> context.getString(R.string.log_check_time)
                            start > System.currentTimeMillis() -> context.getString(R.string.log_not_yet)
                            // A logged sitting under a minute shows 0, and may keep it.
                            m == null || (m !in 1..HAND_MAX_MIN && m != s?.let { it.satSec / 60 }) ->context.getString(R.string.log_check_length, HAND_MAX_MIN)
                            else -> null
                        }
                        if (problem != null || start == null || m == null) return@DialogButtons
                        if (s == null) {
                            c.store.addSession(start = start, plannedSec = m * 60, satSec = m * 60, note = note.trim(), kind = Session.SIT)
                        } else {
                            // Unchanged minutes keep the seconds they had; a new figure is whole minutes.
                            val sat = if (m == s.satSec / 60) s.satSec else m * 60
                            // Unchanged to the minute keeps the start it had, seconds and all.
                            val moved = Stats.dayOf(s.start, zone) != day || start / 60_000 != s.start / 60_000
                            c.store.updateSession(s.id, sat, note.trim(), if (moved) start else null)
                        }
                        onDismiss()
                    },
                )
                if (s != null) {
                    Spacer(Modifier.height(4.dp))
                    DialogButton(stringResource(if (delete.on) R.string.log_delete_armed else R.string.log_delete)) {
                        delete.press {
                            c.store.deleteSession(s.id)
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}
