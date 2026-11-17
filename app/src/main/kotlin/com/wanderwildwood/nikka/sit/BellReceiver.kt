package com.wanderwildwood.nikka.sit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

/**
 * A bell's alarm. The sound is played by the service, which is foreground for the length of a
 * sitting; an exact alarm is one of the things Android 12 lets start a foreground service from
 * the background, so this works even after the process was stopped.
 */
class BellReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // The alarm's own wake lock ends when this returns; hold the phone awake a few seconds
        // more, until the service is up and the bell has taken a wake lock of its own.
        context.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nikka:bell-start")
            .apply { setReferenceCounted(false) }
            .acquire(HANDOFF_MS)
        try {
            context.startForegroundService(
                Intent(context, SitService::class.java)
                    .setAction(SitService.ACTION_BELL)
                    .putExtra(EXTRA_INDEX, intent.getIntExtra(EXTRA_INDEX, 0)),
            )
        } catch (e: IllegalStateException) {
            // An inexact alarm (no exact-alarm permission) may not start a foreground service
            // from the background. The bell still rings, played from here.
            Sitting.bell(context) { end -> Bell.play(context, end) }
        }
    }

    companion object {
        const val EXTRA_INDEX = "index"
        private const val HANDOFF_MS = 10_000L
    }
}
