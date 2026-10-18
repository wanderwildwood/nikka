package com.wanderwildwood.nikka.ui

import android.app.Activity
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.nikka.R
import com.wanderwildwood.nikka.data.Prefs
import com.wanderwildwood.nikka.data.Stats
import com.wanderwildwood.nikka.sit.Plan
import com.wanderwildwood.nikka.sit.Sitting
import kotlinx.coroutines.delay
import java.time.ZoneId

private val PRESETS = listOf(5, 10, 15, 20, 30, 45, 60)
private val WARMUPS = listOf(0, 10, 30, 60, 120)
private val INTERVALS = listOf(0, 5, 10, 15, 20, 30)

/** The sitting as it will be set going: its length, a warm-up, an interval bell, and Start. */
@Composable
fun SitTab(c: AppState) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var v by remember { mutableIntStateOf(0) }
    val minutes = remember(v) { prefs.minutes }
    val warmup = remember(v) { prefs.warmupSec }
    val interval = remember(v) { prefs.intervalMin }
    var pickOther by remember { mutableStateOf(false) }
    var pickWarmup by remember { mutableStateOf(false) }
    var pickInterval by remember { mutableStateOf(false) }

    var checks by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        checks++
        onPauseOrDispose { }
    }
    val exactOn = remember(checks) { context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            // A figure, like a speedometer's: exempt from the type scale.
            TextMMD(text = minutes.toString(), fontSize = 64.sp, fontWeight = FontWeight.Medium, modifier = Modifier.testTag("minutes"))
            TextMMD(
                text = pluralStringResource(R.plurals.minutes_word, minutes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp, bottom = 14.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        val cells = PRESETS.map { it.toString() } + stringResource(R.string.sit_other)
        for (row in cells.chunked(4)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (label in row) {
                    val preset = label.toIntOrNull()
                    val chosen = if (preset != null) preset == minutes else minutes !in PRESETS
                    Stone(label, chosen, Modifier.weight(1f)) {
                        if (preset != null) { prefs.minutes = preset; v++ } else pickOther = true
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDividerMMD(thickness = 1.dp)
        SettingRow(stringResource(R.string.sit_warmup), warmupName(warmup)) { pickWarmup = true }
        SettingRow(stringResource(R.string.sit_interval), intervalName(interval)) { pickInterval = true }
        if (!exactOn) {
            SettingRow(stringResource(R.string.sit_exact), stringResource(R.string.sit_exact_allow)) {
                runCatching {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        ButtonMMD(
            onClick = { Sitting.start(context) },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("start"),
        ) { TextMMD(text = stringResource(R.string.sit_start), style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(20.dp))
    }

    if (pickOther) OtherLengthDialog(minutes, onSet = { prefs.minutes = it; v++ }, onDismiss = { pickOther = false })
    if (pickWarmup) {
        OptionsDialog(stringResource(R.string.sit_warmup), WARMUPS.map { it to warmupName(it) }, warmup,
            onPick = { prefs.warmupSec = it; v++ }, onDismiss = { pickWarmup = false })
    }
    if (pickInterval) {
        OptionsDialog(stringResource(R.string.sit_interval), INTERVALS.map { it to intervalName(it) }, interval,
            onPick = { prefs.intervalMin = it; v++ }, onDismiss = { pickInterval = false })
    }
}

/** A choice drawn as the tabs are: the chosen one a black stone, the others outlined. */
@Composable
private fun Stone(label: String, chosen: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .then(if (chosen) Modifier.background(Color.Black, shape) else Modifier.border(1.dp, Color.Black, shape))
            .clickable(onClick = onClick)
            .testTag("preset_$label"),
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (chosen) Color.White else Color.Black,
        )
    }
}

@Composable
private fun warmupName(sec: Int): String = when {
    sec == 0 -> stringResource(R.string.none)
    sec < 60 -> pluralStringResource(R.plurals.seconds_long, sec, sec.toString())
    else -> pluralStringResource(R.plurals.minutes_long, sec / 60, (sec / 60).toString())
}

@Composable
private fun intervalName(min: Int): String =
    if (min == 0) stringResource(R.string.none) else pluralStringResource(R.plurals.every_minutes, min, min.toString())

@Composable
private fun OtherLengthDialog(minutes: Int, onSet: (Int) -> Unit, onDismiss: () -> Unit) {
    // Empty, with the length now as its hint: typing replaces it rather than adding to it.
    var text by remember { mutableStateOf("") }
    val value = (text.trim().toIntOrNull() ?: minutes).takeIf { it in 1..MAX_MINUTES }
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = stringResource(R.string.sit_other_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Field(stringResource(R.string.sit_other_minutes), text, { text = it.filter(Char::isDigit).take(3) }, hint = minutes.toString(), number = true)
        Spacer(Modifier.height(8.dp))
        DialogButton(stringResource(R.string.set), filled = true) {
            if (value != null) { onSet(value); onDismiss() }
        }
        DialogButton(stringResource(R.string.cancel), onClick = onDismiss)
    }
}

private const val MAX_MINUTES = 600

/**
 * While you sit: the screen dark, its light down, and nothing on it that changes, so the panel is
 * not redrawn until the warm-up gives way to the sitting or the sitting ends. It turns itself off
 * as it always would. A long press anywhere ends the sitting early; Back leaves it running.
 */
@Composable
fun SessionScreen(plan: Plan) {
    val context = LocalContext.current
    val activity = context as? Activity
    BackHandler { activity?.moveTaskToBack(true) }
    DisposableEffect(Unit) {
        val window = activity?.window
        val before = window?.attributes?.screenBrightness
        val bar = window?.statusBarColor
        window?.attributes = window?.attributes?.apply { screenBrightness = 0f }
        // The status bar goes dark with the rest, its icons light.
        window?.statusBarColor = android.graphics.Color.BLACK
        window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = false }
        onDispose {
            if (bar != null) window.statusBarColor = bar
            window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = true }
            window?.attributes = window?.attributes?.apply {
                screenBrightness = before ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }
    var warming by remember(plan) { mutableStateOf(plan.warmingUp(System.currentTimeMillis())) }
    LaunchedEffect(plan, warming) {
        if (warming) {
            delay((plan.sitStart - System.currentTimeMillis()).coerceAtLeast(0))
            warming = false
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("session")
            .pointerInput(plan) { detectTapGestures(onLongPress = { Sitting.endEarly(context) }) },
    ) {
        Column(Modifier.align(Alignment.Center).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TextMMD(
                text = stringResource(if (warming) R.string.session_warming else R.string.session_sitting),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            TextMMD(
                text = if (warming) stringResource(R.string.session_first_bell, Words.time(context, plan.sitStart))
                else stringResource(R.string.session_until, Words.time(context, plan.end)),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
        }
        TextMMD(
            text = stringResource(R.string.session_hold),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        )
    }
}

/** The sitting just ended: how long it was, the streak it keeps, and a line about it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndScreen(c: AppState, id: Long) {
    val context = LocalContext.current
    val session = remember(id, c.version) { c.store.session(id) }
    var note by remember(id) { mutableStateOf(session?.note.orEmpty()) }
    val done = {
        if (session != null && note.trim() != session.note) c.store.updateSession(id, session.satSec, note.trim())
        Sitting.noteDone(context)
    }
    BackHandler { done() }
    if (session == null) {
        LaunchedEffect(Unit) { Sitting.noteDone(context) }
        return
    }
    val streak = remember(c.version, c.today) {
        Stats.dailyStreak(Stats.sittingDays(c.store.sessions(), ZoneId.systemDefault()), c.today)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { TopAppBarMMD(title = { TextMMD(text = stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            TextMMD(
                text = Words.sat(context, session),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag("end_sat"),
            )
            TextMMD(
                text = stringResource(R.string.end_span, Words.time(context, session.start), Words.time(context, session.start + (session.satSec * 1000L))),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (streak > 1) {
                Spacer(Modifier.height(4.dp))
                TextMMD(text = Words.streak(context, streak, weeks = false), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(20.dp))
            Field(stringResource(R.string.end_note), note, { note = it.replace('\n', ' ').take(200) }, hint = stringResource(R.string.end_note_hint))
            Spacer(Modifier.weight(1f))
            ButtonMMD(onClick = done, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("end_done")) {
                TextMMD(text = stringResource(R.string.done), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
