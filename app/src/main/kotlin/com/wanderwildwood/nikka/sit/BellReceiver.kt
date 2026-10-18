package com.wanderwildwood.nikka.sit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * A bell's alarm. The sound is played by the service, which is foreground for the length of a
 * sitting; an exact alarm is one of the things Android 12 lets start a foreground service from
 * the background, so this works even after the process was stopped.
 */
class BellReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.startForegroundService(
            Intent(context, SitService::class.java)
                .setAction(SitService.ACTION_BELL)
                .putExtra(EXTRA_INDEX, intent.getIntExtra(EXTRA_INDEX, 0)),
        )
    }

    companion object {
        const val EXTRA_INDEX = "index"
    }
}
