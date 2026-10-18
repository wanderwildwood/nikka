package com.wanderwildwood.nikka.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.nikka.R
import com.wanderwildwood.nikka.data.Store
import com.wanderwildwood.nikka.sit.Sitting
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

private sealed interface Screen {
    data object Tabs : Screen
    data object Settings : Screen
    data class Habit(val id: Long) : Screen
}

/** What the screens share: the store, today, and the first day of the week where the phone is. */
class AppState(val store: Store, val today: LocalDate, val firstDay: DayOfWeek, val version: Int)

@Composable
fun PracticeApp() {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsState()
    val running by Sitting.running.collectAsState()
    val ended by Sitting.ended.collectAsState()

    // Read again on every return to the app: the day may have turned, a sitting may have ended.
    var resumed by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        Sitting.load(context)
        resumed++
        onPauseOrDispose { }
    }
    val today = remember(resumed, version) { LocalDate.now() }
    val firstDay = remember(resumed) { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val c = AppState(store, today, firstDay, version)

    var screen by remember { mutableStateOf<Screen>(Screen.Tabs) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val plan = running
    when {
        plan != null -> SessionScreen(plan)
        ended >= 0 -> EndScreen(c, ended)
        else -> when (val s = screen) {
            Screen.Tabs -> Tabs(
                c, tab, onTab = { tab = it },
                onSettings = { screen = Screen.Settings },
                onHabit = { screen = Screen.Habit(it) },
            )
            Screen.Settings -> SettingsScreen(c, onBack = { screen = Screen.Tabs })
            is Screen.Habit -> HabitScreen(c, s.id, onBack = { screen = Screen.Tabs })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Tabs(c: AppState, tab: Int, onTab: (Int) -> Unit, onSettings: () -> Unit, onHabit: (Long) -> Unit) {
    // Back from Today or the log goes to Sit first, as from any tabbed screen of these apps.
    BackHandler(enabled = tab != 0) { onTab(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.app_name)) },
                actions = { BarButton(Icons.Settings, stringResource(R.string.cd_settings), onSettings) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            StoneTabs(
                labels = listOf(stringResource(R.string.tab_sit), stringResource(R.string.tab_today), stringResource(R.string.tab_log)),
                selected = tab,
                onSelect = onTab,
            )
            when (tab) {
                0 -> SitTab(c)
                1 -> TodayTab(c, onHabit = onHabit, onSit = { onTab(0) })
                else -> LogTab(c)
            }
        }
    }
}
