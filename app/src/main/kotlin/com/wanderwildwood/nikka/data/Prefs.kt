package com.wanderwildwood.nikka.data

import android.content.Context
import com.wanderwildwood.nikka.sit.Breath
import com.wanderwildwood.nikka.sit.BreathCue
import com.wanderwildwood.nikka.sit.Plan

/**
 * The small things kept between visits: how the next sitting is set, the sitting under way if
 * there is one, and two switches. One private preferences file.
 */
class Prefs(context: Context) {
    private val p = context.getSharedPreferences("nikka", Context.MODE_PRIVATE)

    /** The length of the next sitting, in minutes. */
    var minutes: Int
        get() = p.getInt("minutes", 20)
        set(v) = p.edit().putInt("minutes", v).apply()

    var warmupSec: Int
        get() = p.getInt("warmup", 0)
        set(v) = p.edit().putInt("warmup", v).apply()

    /** Minutes between bells inside a sitting; 0 for none. */
    var intervalMin: Int
        get() = p.getInt("interval", 0)
        set(v) = p.edit().putInt("interval", v).apply()

    /** The Sit tab set for breathing rather than sitting. */
    var breathe: Boolean
        get() = p.getBoolean("breathe", false)
        set(v) = p.edit().putBoolean("breathe", v).apply()

    var breath: Breath
        get() = Breath(p.getInt("breath_in", 4), p.getInt("breath_hold", 7), p.getInt("breath_out", 8), p.getInt("breath_rest", 0))
            .takeIf { it.valid() } ?: Breath.FOUR_SEVEN_EIGHT
        set(v) = p.edit().putInt("breath_in", v.inSec).putInt("breath_hold", v.holdSec)
            .putInt("breath_out", v.outSec).putInt("breath_rest", v.restSec).apply()

    /** Minutes of breathing, rounded up to whole rounds. */
    var breathMinutes: Int
        get() = p.getInt("breath_minutes", 5)
        set(v) = p.edit().putInt("breath_minutes", v).apply()

    /** How each change of breath is marked: see BreathCue. */
    var breathCue: Int
        get() = p.getInt("breath_cue", BreathCue.VIBRATE)
        set(v) = p.edit().putInt("breath_cue", v).apply()

    /** Streaks shown beside each habit; off shows only the days done in the last 30. */
    var showStreaks: Boolean
        get() = p.getBoolean("show_streaks", true)
        set(v) = p.edit().putBoolean("show_streaks", v).apply()

    /** Minutes of breathing before a sitting, as its warm-up; 0 for none. */
    var breatheFirstMin: Int
        get() = p.getInt("breathe_first", 0)
        set(v) = p.edit().putInt("breathe_first", v).apply()

    var doNotDisturb: Boolean
        get() = p.getBoolean("dnd", false)
        set(v) = p.edit().putBoolean("dnd", v).apply()

    /** Said to be switched on in DuraSpeed's list, which no app can read; see DuraSpeed. */
    var duraSpeedAllowed: Boolean
        get() = p.getBoolean("duraspeed_allowed", false)
        set(v) = p.edit().putBoolean("duraspeed_allowed", v).apply()

    var duraSpeedStopSeen: Long
        get() = p.getLong("duraspeed_stop_seen", 0)
        set(v) = p.edit().putLong("duraspeed_stop_seen", v).apply()

    // ---- the sitting under way. commit, not apply: the next thing may be the process dying.

    var running: Plan?
        get() = if (!p.contains("run_start")) null else Plan(
            p.getLong("run_start", 0), p.getInt("run_warmup", 0), p.getInt("run_planned", 0), p.getInt("run_interval", 0),
        )
        set(v) {
            val e = p.edit()
            if (v == null) {
                e.remove("run_start").remove("run_warmup").remove("run_planned").remove("run_interval").remove("run_rung")
            } else {
                e.putLong("run_start", v.start).putInt("run_warmup", v.warmupSec)
                    .putInt("run_planned", v.plannedSec).putInt("run_interval", v.intervalSec).putInt("run_rung", -1)
            }
            e.commit()
        }

    /** The last bell rung in the sitting under way, -1 before the first. */
    var rung: Int
        get() = p.getInt("run_rung", -1)
        set(v) { p.edit().putInt("run_rung", v).commit() }

    /** The interruption filter in force before this app set Do Not Disturb; -1 when it did not. */
    var filterBefore: Int
        get() = p.getInt("filter_before", -1)
        set(v) { p.edit().putInt("filter_before", v).commit() }

    /** The sitting just ended, shown once with a place for a note; -1 for none. */
    var justEnded: Long
        get() = p.getLong("just_ended", -1)
        set(v) { p.edit().putLong("just_ended", v).commit() }
}
