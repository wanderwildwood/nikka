package com.wanderwildwood.nikka.ui

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.nikka.R
import com.wanderwildwood.nikka.data.Prefs
import com.wanderwildwood.nikka.sit.Breath
import com.wanderwildwood.nikka.sit.BreathCue
import com.wanderwildwood.nikka.sit.BreathRun
import com.wanderwildwood.nikka.sit.Breathing
import kotlinx.coroutines.delay

private val BREATH_MINUTES = listOf(1, 2, 3, 5, 10, 15)

/** A breathing session as it will be set going: the pattern, its length, its cue, and Start. */
@Composable
fun BreatheTab() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var v by remember { mutableIntStateOf(0) }
    val breath = remember(v) { prefs.breath }
    val minutes = remember(v) { prefs.breathMinutes }
    val cue = remember(v) { prefs.breathCue }
    var pickOther by remember { mutableStateOf(false) }
    var pickLength by remember { mutableStateOf(false) }
    var pickCue by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(10.dp))
        // A figure, like the Sit tab's minutes: exempt from the type scale. A long pattern
        // ("20 · 20 · 20 · 20") steps down until it fits on one line.
        FittedFigure(breath.figure(), Modifier.fillMaxWidth().testTag("breath_figure"))
        TextMMD(
            text = breathLine(breath),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (preset in Breath.PRESETS) {
                val label = if (preset == Breath.BOX) stringResource(R.string.breath_box)
                else preset.phases.joinToString("-") { it.sec.toString() }
                HouseButton(label, { prefs.breath = preset; v++ }, Modifier.weight(1f).testTag("breath_$label"), on = breath == preset)
            }
            HouseButton(
                stringResource(R.string.sit_other), { pickOther = true },
                Modifier.weight(1f).testTag("breath_other"), on = breath !in Breath.PRESETS,
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDividerMMD(thickness = 1.dp)
        // The rounds beside the minutes: a slow pattern makes one round longer than a short length.
        val rounds = breath.rounds(minutes)
        SettingRow(
            stringResource(R.string.breath_length),
            stringResource(
                R.string.breath_length_value,
                pluralStringResource(R.plurals.minutes_long, minutes, minutes.toString()),
                pluralStringResource(R.plurals.breath_done_rounds, rounds, rounds),
            ),
        ) { pickLength = true }
        SettingRow(stringResource(R.string.breath_cue), cueName(cue)) { pickCue = true }
        Spacer(Modifier.weight(1f))
        ButtonMMD(
            onClick = { Breathing.start(context, minutes, thenSit = false) },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("breathe_start"),
        ) { TextMMD(text = stringResource(R.string.sit_start), style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(20.dp))
    }

    if (pickOther) OtherBreathDialog(breath, onSet = { prefs.breath = it; v++ }, onDismiss = { pickOther = false })
    if (pickLength) {
        val names = BREATH_MINUTES.map { it to pluralStringResource(R.plurals.minutes_long, it, it.toString()) }
        OptionsDialog(stringResource(R.string.breath_length), names, minutes,
            onPick = { prefs.breathMinutes = it; v++ }, onDismiss = { pickLength = false })
    }
    if (pickCue) {
        OptionsDialog(stringResource(R.string.breath_cue), BreathCue.ALL.map { it to cueName(it) }, cue,
            onPick = { prefs.breathCue = it; v++ }, onDismiss = { pickCue = false })
    }
}

/** The figure at the largest size, 64sp down to 32, that fits the width on one line. */
@Composable
private fun FittedFigure(text: String, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val room = constraints.maxWidth
        val size: TextUnit = remember(text, room) {
            listOf(64, 56, 48, 40, 32).firstOrNull { sp ->
                measurer.measure(text, TextStyle(fontSize = sp.sp, fontWeight = FontWeight.Medium), softWrap = false).size.width <= room
            }?.sp ?: 32.sp
        }
        // The height kept at the largest size's, so the rows below do not move with the pattern.
        Box(Modifier.height(with(density) { (64.sp * 1.3f).toDp() }), contentAlignment = Alignment.Center) {
            TextMMD(text = text, fontSize = size, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
        }
    }
}

/** "in · hold · out, 3 breaths a minute" */
@Composable
private fun breathLine(breath: Breath): String {
    val names = breath.phases.map {
        stringResource(
            when (it.kind) {
                Breath.Kind.IN -> R.string.breath_in_short
                Breath.Kind.HOLD -> R.string.breath_hold_short
                Breath.Kind.OUT -> R.string.breath_out_short
                Breath.Kind.REST -> R.string.breath_rest_short
            },
        )
    }.joinToString(" · ")
    val sec = breath.roundSec
    val pace = if (sec <= 30) {
        val n = Math.round(60.0 / sec).toInt()
        pluralStringResource(R.plurals.breaths_a_minute, n, n)
    } else {
        pluralStringResource(R.plurals.breath_every_seconds, sec, sec)
    }
    return stringResource(R.string.breath_line, names, pace)
}

@Composable
private fun cueName(cue: Int): String = stringResource(
    when (cue) {
        BreathCue.TONE -> R.string.cue_tone
        BreathCue.BOTH -> R.string.cue_both
        BreathCue.SCREEN -> R.string.cue_screen
        else -> R.string.cue_vibrate
    },
)

/**
 * A pattern of one's own: four fields, in pairs, each empty with the pattern now as its hint,
 * so typing replaces a number rather than adding to it, and a field left empty keeps it.
 */
@Composable
private fun OtherBreathDialog(breath: Breath, onSet: (Breath) -> Unit, onDismiss: () -> Unit) {
    var inText by remember { mutableStateOf("") }
    var holdText by remember { mutableStateOf("") }
    var outText by remember { mutableStateOf("") }
    var restText by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    fun num(text: String, now: Int) = text.trim().toIntOrNull() ?: now
    val value = Breath(num(inText, breath.inSec), num(holdText, breath.holdSec), num(outText, breath.outSec), num(restText, breath.restSec))
        .takeIf { it.valid() }
    val digits = { s: String -> s.filter(Char::isDigit).take(2) }
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = stringResource(R.string.breath_other_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(stringResource(R.string.breath_field_in), inText, { inText = digits(it) }, hint = breath.inSec.toString(), number = true, modifier = Modifier.weight(1f))
            Field(stringResource(R.string.breath_field_hold), holdText, { holdText = digits(it) }, hint = breath.holdSec.toString(), number = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(stringResource(R.string.breath_field_out), outText, { outText = digits(it) }, hint = breath.outSec.toString(), number = true, modifier = Modifier.weight(1f))
            Field(stringResource(R.string.breath_field_rest), restText, { restText = digits(it) }, hint = breath.restSec.toString(), number = true, modifier = Modifier.weight(1f))
        }
        if (tried && value == null) {
            TextMMD(text = stringResource(R.string.breath_other_range, Breath.MAX_SEC), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(8.dp))
        DialogButton(stringResource(R.string.set), filled = true) {
            if (value != null) { onSet(value); onDismiss() } else tried = true
        }
        DialogButton(stringResource(R.string.cancel), onClick = onDismiss)
    }
}

/**
 * While breathing: the app's own light screen, kept awake, with the phase in words, the seconds
 * left in it, and the round. The seconds change once a second and the words once a phase, so
 * the panel is redrawn no more than that. The cues are timed here, on screen, which is why
 * leaving the app ends the session. A long press ends it early, or, when a sitting follows,
 * moves on to the sitting; Back ends both.
 */
@Composable
fun BreathingScreen(run: BreathRun) {
    val context = LocalContext.current
    val activity = context as? Activity
    val cue = remember(run) { BreathCue(context, run.cue) }
    DisposableEffect(run) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            cue.release()
        }
    }
    BackHandler { Breathing.end(context, cancelled = true) }
    // Cues are only timed while the screen is up: put away, the session is over.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { Breathing.end(context, cancelled = true) }

    var kind by remember(run) { mutableStateOf(run.breath.phases[0].kind) }
    var round by remember(run) { mutableIntStateOf(0) }
    var left by remember(run) { mutableIntStateOf(run.breath.phases[0].sec) }
    LaunchedEffect(run) {
        var last = -1
        while (true) {
            val at = run.breath.at(System.currentTimeMillis() - run.start, run.rounds)
            if (at == null) {
                // Ran to the end. A sitting that follows rings its own bowl as it begins.
                if (!run.thenSit) cue.end()
                Breathing.end(context, cancelled = false)
                break
            }
            val key = at.round * 4 + at.phaseIndex
            if (key != last) {
                last = key
                kind = at.phase.kind
                round = at.round
                cue.phase(at.phase.kind)
            }
            left = ((at.leftMs + 999) / 1000).toInt()
            // Wake as the shown second runs out, a little after, so the next look sees the next one.
            val toNext = at.leftMs % 1000
            delay((if (toNext == 0L) 1000L else toNext) + 15)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("breathing")
            .pointerInput(run) { detectTapGestures(onLongPress = { Breathing.end(context, cancelled = false) }) },
    ) {
        Column(Modifier.align(Alignment.Center).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TextMMD(
                text = stringResource(
                    when (kind) {
                        Breath.Kind.IN -> R.string.breath_now_in
                        Breath.Kind.OUT -> R.string.breath_now_out
                        else -> R.string.breath_now_hold
                    },
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("breath_phase"),
            )
            // A figure, like the Sit tab's minutes: exempt from the type scale.
            TextMMD(text = left.toString(), fontSize = 96.sp, fontWeight = FontWeight.Medium, modifier = Modifier.testTag("breath_left"))
            TextMMD(
                text = stringResource(R.string.breath_round, round + 1, run.rounds),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        TextMMD(
            text = stringResource(if (run.thenSit) R.string.breath_hold_to_sit else R.string.session_hold),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        )
    }
}

/** The breathing just ended: how many rounds, and a tap to close. */
@Composable
fun BreathDoneScreen(rounds: Int) {
    BackHandler { Breathing.close() }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("breath_done")
            .pointerInput(Unit) { detectTapGestures(onTap = { Breathing.close() }) },
    ) {
        Column(Modifier.align(Alignment.Center).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TextMMD(text = stringResource(R.string.breath_done), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium)
            if (rounds > 0) {
                Spacer(Modifier.height(8.dp))
                TextMMD(text = pluralStringResource(R.plurals.breath_done_rounds, rounds, rounds), style = MaterialTheme.typography.bodyLarge)
            }
        }
        TextMMD(
            text = stringResource(R.string.breath_tap_close),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        )
    }
}
