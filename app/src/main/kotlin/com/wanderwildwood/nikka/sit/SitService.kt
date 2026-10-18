package com.wanderwildwood.nikka.sit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import com.wanderwildwood.nikka.R
import com.wanderwildwood.nikka.data.Prefs

/**
 * Runs for the length of a sitting, with a quiet notification saying when it ends, and plays the
 * bells. The notification makes no sound and changes only when the warm-up gives way to the
 * sitting, so it costs the panel nothing while you sit.
 */
class SitService : Service() {

    private var sounding = 0
    private var stopping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Every start through startForegroundService has to be answered with startForeground,
        // the stop as well, so the notification goes up first whatever was asked.
        val plan = Prefs(this).running
        startForeground(NOTIFICATION_ID, notification(this, plan))
        when (intent?.action) {
            ACTION_START, ACTION_BELL -> {
                stopping = false
                Sitting.bell(this) { end -> sound(end) }
                Prefs(this).running?.let { startForeground(NOTIFICATION_ID, notification(this, it)) }
            }
            ACTION_STOP -> stopping = true
            else -> if (plan == null) stopping = true else Sitting.schedule(this)
        }
        if (Prefs(this).running == null) stopping = true
        stopIfDone()
        return START_NOT_STICKY
    }

    private fun sound(end: Boolean) {
        sounding++
        Bell.play(this, end) {
            sounding--
            stopIfDone()
        }
    }

    // The end bell is still ringing when the sitting is over: the service stays until it is done.
    private fun stopIfDone() {
        if (stopping && sounding == 0) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    companion object {
        const val ACTION_START = "com.wanderwildwood.nikka.START"
        const val ACTION_BELL = "com.wanderwildwood.nikka.BELL"
        const val ACTION_STOP = "com.wanderwildwood.nikka.STOP"
        const val CHANNEL = "sitting"
        private const val NOTIFICATION_ID = 1

        fun channel(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.channel_sitting), NotificationManager.IMPORTANCE_LOW).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                },
            )
        }

        fun notification(context: Context, plan: Plan?): Notification {
            channel(context)
            val now = System.currentTimeMillis()
            val time = { at: Long -> DateFormat.getTimeFormat(context).format(at) }
            val text = when {
                plan == null -> context.getString(R.string.notify_ended)
                plan.warmingUp(now) -> context.getString(R.string.notify_warming, time(plan.sitStart))
                else -> context.getString(R.string.notify_sitting, time(plan.end))
            }
            return NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setOngoing(true)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(Sitting.openApp(context))
                .build()
        }
    }
}
