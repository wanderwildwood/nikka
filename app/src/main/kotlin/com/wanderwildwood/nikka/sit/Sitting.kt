package com.wanderwildwood.nikka.sit

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wanderwildwood.nikka.BuildConfig
import com.wanderwildwood.nikka.MainActivity
import com.wanderwildwood.nikka.data.Prefs
import com.wanderwildwood.nikka.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Starting, ringing and ending a sitting. The timing is AlarmManager's, not a coroutine's: each
 * bell is an exact alarm allowed while the phone is idle, and the end bell is an alarm clock,
 * which Doze always lets through. So the bells come with the screen off, in Doze, and after the
 * process has been stopped. A foreground service with a quiet notification runs alongside so
 * the system keeps the app in hand for the length of the sitting.
 *
 * Only the next bell is ever set; ringing it sets the one after. Each bell carries its number,
 * and one that has already rung is not rung twice.
 */
object Sitting {

    private val _running = MutableStateFlow<Plan?>(null)
    /** The sitting under way, for the screens. */
    val running: StateFlow<Plan?> = _running

    private val _ended = MutableStateFlow(-1L)
    /** The sitting just ended, waiting for its note; -1 for none. */
    val ended: StateFlow<Long> = _ended

    /** Reads the state back after the process has started again. */
    fun load(context: Context) {
        val prefs = Prefs(context)
        _running.value = prefs.running
        _ended.value = prefs.justEnded
        // A sitting whose end passed while nothing was running to hear it (its alarm went with
        // a force-stop) is finished now, at its planned length.
        val p = prefs.running
        if (p != null && System.currentTimeMillis() > p.end + 60_000L) finish(context, p.plannedSec)
    }

    fun start(context: Context) {
        val prefs = Prefs(context)
        val plan = Plan(
            start = System.currentTimeMillis(),
            warmupSec = prefs.warmupSec,
            plannedSec = prefs.minutes * 60,
            intervalSec = prefs.intervalMin * 60,
        )
        prefs.running = plan
        prefs.justEnded = -1
        _ended.value = -1
        _running.value = plan
        quiet(context, prefs, on = true)
        context.startForegroundService(Intent(context, SitService::class.java).setAction(SitService.ACTION_START))
    }

    /**
     * Rings the bell that is due, if it has not rung, then sets the next. Called by the alarm,
     * and once as the sitting starts, which rings the first bell straight away when there is no
     * warm-up. Bells are found by the time, not by the alarm's word for which one it was: one
     * that comes late, after Doze held it, rings once for everything due by then.
     */
    fun bell(context: Context, onSound: (end: Boolean) -> Unit) {
        val prefs = Prefs(context)
        val plan = prefs.running ?: return
        val bells = plan.bells()
        val now = System.currentTimeMillis()
        // Any bell due by now that has not rung rings once, as the latest of them.
        val due = bells.lastOrNull { it.index > prefs.rung && it.at <= now + 1_000 }
        if (due != null) {
            if (BuildConfig.DEBUG) Log.i("nikka", "bell ${due.index} ${due.kind} due ${due.at} rang ${now} late ${now - due.at} ms")
            prefs.rung = due.index
            if (due.kind == Plan.Kind.END) {
                onSound(true)
                finish(context, plan.plannedSec)
                return
            }
            onSound(false)
        }
        schedule(context)
    }

    /** Sets the alarm for the next bell still to ring. */
    fun schedule(context: Context) {
        val prefs = Prefs(context)
        val plan = prefs.running ?: return
        val next = plan.next(prefs.rung) ?: return
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context, next.index)
        if (!am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pi)
        } else if (next.kind == Plan.Kind.END) {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(next.at, openApp(context)), pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pi)
        }
    }

    /** Ends the sitting early, by a long press: logged for the time actually sat. */
    fun endEarly(context: Context) {
        val plan = Prefs(context).running ?: return
        finish(context, plan.satBy(System.currentTimeMillis()))
    }

    private fun finish(context: Context, satSec: Int) {
        val prefs = Prefs(context)
        val plan = prefs.running ?: return
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context, 0))
        // A warm-up left before the first bell is not a sitting.
        val id = if (satSec > 0) Store.get(context).addSession(plan.sitStart, plan.plannedSec, satSec) else -1L
        prefs.running = null
        prefs.justEnded = id
        quiet(context, prefs, on = false)
        _running.value = null
        _ended.value = id
        context.startService(Intent(context, SitService::class.java).setAction(SitService.ACTION_STOP))
    }

    fun noteDone(context: Context) {
        Prefs(context).justEnded = -1
        _ended.value = -1
    }

    /**
     * Do Not Disturb for the length of a sitting, alarms only, so the bells still ring and
     * nothing else does. Put back afterwards as it was, unless the person changed it meanwhile.
     */
    private fun quiet(context: Context, prefs: Prefs, on: Boolean) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return
        if (on && prefs.doNotDisturb) {
            prefs.filterBefore = nm.currentInterruptionFilter
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
        } else if (!on && prefs.filterBefore >= 0) {
            if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
                nm.setInterruptionFilter(prefs.filterBefore)
            }
            prefs.filterBefore = -1
        }
    }

    // One PendingIntent for every bell, so setting the next replaces the last and a cancel
    // catches whichever is pending. The bell's number rides as an extra.
    private fun alarmIntent(context: Context, index: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, BellReceiver::class.java).putExtra(BellReceiver.EXTRA_INDEX, index),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 1,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
