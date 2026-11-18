package com.wanderwildwood.nikka.sit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * After the phone starts again: a sitting it was in the middle of is over, since its bells went
 * with the restart. It is ended here, as a long press would, so Do Not Disturb is put back
 * without waiting for the app to be opened. It is logged for the time it is known to have run
 * (see [Plan.aliveUntil]), not for the time the phone was off. The word that the phone is
 * shutting down reaches only a receiver registered while the app runs, so [SitService] listens
 * for it, not this one.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Sitting.endAfterRestart(context)
    }
}
