package com.wanderwildwood.nikka.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.nikka.R
import com.wanderwildwood.nikka.data.Session

/** Every sitting, newest first. A press opens it to change its length or note, or delete it. */
@Composable
fun LogTab(c: AppState) {
    val context = LocalContext.current
    val sessions = remember(c.version) { c.store.sessions() }
    var open by remember { mutableStateOf<Session?>(null) }
    if (sessions.isEmpty()) {
        TextMMD(
            text = stringResource(R.string.log_empty),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(20.dp),
        )
        return
    }
    val state = rememberLazyListState()
    ListJumpBox(state, ListJump.Pages) {
        PagedColumn(Modifier.fillMaxSize(), state = state) {
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
                        TextMMD(text = Words.sat(context, s), style = MaterialTheme.typography.bodyMedium)
                        if (s.note.isNotBlank()) TextMMD(text = s.note, style = MaterialTheme.typography.labelSmall)
                    }
                    HorizontalDividerMMD(thickness = 1.dp)
                }
            }
        }
    }
    open?.let { s -> SessionDialog(c, s, onDismiss = { open = null }) }
}

@Composable
private fun SessionDialog(c: AppState, s: Session, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var minutes by remember { mutableStateOf((s.satSec / 60).toString()) }
    var note by remember { mutableStateOf(s.note) }
    val delete = rememberArmed()
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = Words.whenSat(context, s.start), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        TextMMD(
            text = stringResource(R.string.log_planned, context.getString(R.string.minutes_short, (s.plannedSec / 60).toString())),
            style = MaterialTheme.typography.labelSmall,
        )
        Field(stringResource(R.string.log_minutes_sat), minutes, { minutes = it.filter(Char::isDigit).take(3) }, number = true)
        Field(stringResource(R.string.end_note), note, { note = it.replace('\n', ' ').take(200) })
        Spacer(Modifier.height(8.dp))
        DialogButton(stringResource(R.string.save), filled = true) {
            val m = minutes.toIntOrNull()
            // Unchanged minutes keep the seconds they had; a new figure is whole minutes.
            val sat = if (m == null || m == s.satSec / 60) s.satSec else m * 60
            c.store.updateSession(s.id, sat, note.trim())
            onDismiss()
        }
        DialogButton(stringResource(if (delete.on) R.string.log_delete_armed else R.string.log_delete)) {
            delete.press {
                c.store.deleteSession(s.id)
                onDismiss()
            }
        }
        DialogButton(stringResource(R.string.cancel), onClick = onDismiss)
    }
}
