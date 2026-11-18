package com.wanderwildwood.nikka.sit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * After the phone starts again: a sitting it was in the middle of is over, since its bells went
 * with the restart. It is ended here, as a long press would, so Do Not Disturb is put back
 * without waiting for the app to be opened. As the phone shuts down, the time is kept, so the
 * sitting is logged for the time sat until then and not for the time the phone was off.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> Sitting.endAfterRestart(context)
            Intent.ACTION_SHUTDOWN -> Sitting.shuttingDown(context)
        }
    }
}
