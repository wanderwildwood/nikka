package com.wanderwildwood.nikka.ui

import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.nikka.R
import com.wanderwildwood.nikka.data.Backup
import com.wanderwildwood.nikka.data.DuraSpeed
import com.wanderwildwood.nikka.data.Prefs
import com.wanderwildwood.nikka.sit.Bell
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(c: AppState, onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var v by remember { mutableIntStateOf(0) }
    var about by remember { mutableStateOf(false) }
    var ringing by remember { mutableStateOf(false) }

    // Read again on every return from the phone's settings, where these are changed.
    var checks by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        checks++
        onPauseOrDispose { }
    }
    val nm = context.getSystemService(NotificationManager::class.java)
    val policy = remember(checks) { nm.isNotificationPolicyAccessGranted }
    val dnd = remember(checks, v) { prefs.doNotDisturb && policy }
    val duraSpeedRisk = remember(checks, v) {
        DuraSpeed.newStop(context, prefs)
        DuraSpeed.atRisk(context, prefs)
    }

    fun open(intent: Intent) = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { Toast.makeText(context, context.getString(R.string.cannot_open), Toast.LENGTH_LONG).show() }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(Backup.write(c.store).toByteArray()) }
        }.onSuccess { Toast.makeText(context, context.getString(R.string.backup_saved), Toast.LENGTH_SHORT).show() }
            .onFailure {
                // The picker has already made an empty file; do not leave it behind.
                runCatching { android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri) }
                Toast.makeText(context, context.getString(R.string.backup_not_saved), Toast.LENGTH_LONG).show()
            }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
            c.store.replaceAll(Backup.read(text))
        }.onSuccess { Toast.makeText(context, context.getString(R.string.restore_done), Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(context, context.getString(R.string.restore_not_ours), Toast.LENGTH_LONG).show() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.settings_title)) },
                navigationIcon = { BarButton(Icons.Back, stringResource(R.string.cd_back), onBack) },
                actions = { BarButton(Icons.Info, stringResource(R.string.cd_about)) { about = true } },
            )
        },
    ) { padding ->
        val state = rememberLazyListState()
        PagedColumn(Modifier.padding(padding).fillMaxSize(), state = state, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 12.dp)) {
            item { Spacer(Modifier.height(8.dp)) }
            // Only while it is still to do: a row for something already done is furniture.
            if (duraSpeedRisk) {
                item(key = "dura") {
                    SettingRow(stringResource(R.string.settings_duraspeed), stringResource(R.string.settings_duraspeed_open)) {
                        open(DuraSpeed.appInfo())
                    }
                }
                item(key = "dura_done") {
                    SettingRow(stringResource(R.string.settings_duraspeed_done), stringResource(R.string.settings_duraspeed_done_note)) {
                        prefs.duraSpeedAllowed = true
                        v++
                    }
                }
            }
            item(key = "bell") {
                SettingRow(
                    stringResource(R.string.settings_try_bell),
                    stringResource(if (ringing) R.string.settings_bell_ringing else R.string.settings_bell_volume),
                ) {
                    if (!ringing) {
                        ringing = true
                        Bell.play(context, end = false) { ringing = false }
                    }
                }
            }
            item(key = "dnd") {
                SwitchRow(stringResource(R.string.settings_dnd), dnd, stringResource(R.string.settings_dnd_note)) { on ->
                    prefs.doNotDisturb = on
                    if (on && !policy) open(Intent(AndroidSettings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    v++
                }
            }
            item(key = "backup") { Heading(stringResource(R.string.settings_backup_heading)) }
            item(key = "save") {
                SettingRow(stringResource(R.string.settings_save_backup), stringResource(R.string.settings_save_backup_note)) {
                    save.launch("practice-${LocalDate.now()}.json")
                }
            }
            item(key = "restore") {
                ArmedRow(stringResource(R.string.settings_restore), stringResource(R.string.settings_restore_armed)) {
                    restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
            }
        }
    }
    if (about) AboutDialog(onDismiss = { about = false })
}
