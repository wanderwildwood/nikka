package com.wanderwildwood.nikka.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * The habits, their ticks and the log of sittings, in one small SQLite file in the app's own
 * storage. Nothing else reads it, and Android does not back it up: the backup is the JSON file
 * Settings writes where the person chooses.
 *
 * Every write bumps [version], which is how the screens know to read again.
 */
class Store private constructor(context: Context) :
    SQLiteOpenHelper(context, "nikka.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE habit (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                per_week INTEGER NOT NULL DEFAULT 7,
                builtin INTEGER NOT NULL DEFAULT 0,
                position INTEGER NOT NULL DEFAULT 0,
                cue TEXT NOT NULL DEFAULT ''
            )""",
        )
        // A day is the local date as an epoch day: a tick belongs to a day, not to an instant.
        db.execSQL("CREATE TABLE tick (habit_id INTEGER NOT NULL, day INTEGER NOT NULL, PRIMARY KEY (habit_id, day))")
        db.execSQL(
            """CREATE TABLE session (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                start INTEGER NOT NULL,
                planned INTEGER NOT NULL,
                sat INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                kind TEXT NOT NULL DEFAULT 'sit',
                pattern TEXT NOT NULL DEFAULT ''
            )""",
        )
        db.execSQL("CREATE INDEX session_start ON session (start)")
        createRest(db)
        addMeditation(db)
    }

    /**
     * 1 → 2 (0.2.0): breathing sessions in the log (kind, pattern), a cue line on each habit,
     * and rest days. Everything already there stays as it was: every old session is a sitting.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE session ADD COLUMN kind TEXT NOT NULL DEFAULT 'sit'")
            db.execSQL("ALTER TABLE session ADD COLUMN pattern TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE habit ADD COLUMN cue TEXT NOT NULL DEFAULT ''")
            createRest(db)
        }
    }

    // A rest day: marked on purpose, it neither breaks a streak nor counts toward one.
    private fun createRest(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS rest (habit_id INTEGER NOT NULL, day INTEGER NOT NULL, PRIMARY KEY (habit_id, day))")
    }

    private fun addMeditation(db: SQLiteDatabase) {
        db.insert("habit", null, ContentValues().apply {
            put("name", "")
            put("per_week", 7)
            put("builtin", 1)
            put("position", 0)
        })
    }

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version
    private fun changed() { _version.value++ }

    // ---- habits

    fun habits(): List<Habit> = readableDatabase.rawQuery(
        "SELECT id, name, per_week, builtin, position, cue FROM habit ORDER BY position, id", null,
    ).use { c ->
        buildList {
            while (c.moveToNext()) add(Habit(c.getLong(0), c.getString(1), c.getInt(2), c.getInt(3) != 0, c.getInt(4), c.getString(5)))
        }
    }

    fun habit(id: Long): Habit? = habits().firstOrNull { it.id == id }

    fun addHabit(name: String, perWeek: Int, cue: String = ""): Long {
        val pos = (habits().maxOfOrNull { it.position } ?: 0) + 1
        val id = writableDatabase.insert("habit", null, ContentValues().apply {
            put("name", name)
            put("per_week", perWeek)
            put("cue", cue)
            put("position", pos)
        })
        changed()
        return id
    }

    /** Meditation's name is the interface's, so for it only [perWeek] and [cue] are kept. */
    fun updateHabit(id: Long, name: String, perWeek: Int, cue: String) {
        writableDatabase.update("habit", ContentValues().apply {
            put("name", name)
            put("per_week", perWeek)
            put("cue", cue)
        }, "id = ?", arrayOf(id.toString()))
        changed()
    }

    /** Deletes a habit you made, and its ticks. Meditation cannot be deleted. */
    fun deleteHabit(id: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("tick", "habit_id = ?", arrayOf(id.toString()))
            db.delete("rest", "habit_id = ?", arrayOf(id.toString()))
            db.delete("habit", "id = ? AND builtin = 0", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        changed()
    }

    // ---- ticks

    fun ticks(habitId: Long): Set<LocalDate> = readableDatabase.rawQuery(
        "SELECT day FROM tick WHERE habit_id = ?", arrayOf(habitId.toString()),
    ).use { c -> buildSet { while (c.moveToNext()) add(LocalDate.ofEpochDay(c.getLong(0))) } }

    fun setTick(habitId: Long, day: LocalDate, on: Boolean) {
        if (on) {
            writableDatabase.delete("rest", "habit_id = ? AND day = ?", arrayOf(habitId.toString(), day.toEpochDay().toString()))
            writableDatabase.insertWithOnConflict("tick", null, ContentValues().apply {
                put("habit_id", habitId)
                put("day", day.toEpochDay())
            }, SQLiteDatabase.CONFLICT_IGNORE)
        } else {
            writableDatabase.delete("tick", "habit_id = ? AND day = ?", arrayOf(habitId.toString(), day.toEpochDay().toString()))
        }
        changed()
    }

    // ---- rest days

    fun rests(habitId: Long): Set<LocalDate> = readableDatabase.rawQuery(
        "SELECT day FROM rest WHERE habit_id = ?", arrayOf(habitId.toString()),
    ).use { c -> buildSet { while (c.moveToNext()) add(LocalDate.ofEpochDay(c.getLong(0))) } }

    /** Marks or clears a rest day. A day is done or rested, not both: marking one clears the other. */
    fun setRest(habitId: Long, day: LocalDate, on: Boolean) {
        val args = arrayOf(habitId.toString(), day.toEpochDay().toString())
        if (on) {
            writableDatabase.delete("tick", "habit_id = ? AND day = ?", args)
            writableDatabase.insertWithOnConflict("rest", null, ContentValues().apply {
                put("habit_id", habitId)
                put("day", day.toEpochDay())
            }, SQLiteDatabase.CONFLICT_IGNORE)
        } else {
            writableDatabase.delete("rest", "habit_id = ? AND day = ?", args)
        }
        changed()
    }

    // ---- sittings and breathing

    /** Newest first. */
    fun sessions(): List<Session> = readableDatabase.rawQuery(
        "SELECT id, start, planned, sat, note, kind, pattern FROM session ORDER BY start DESC, id DESC", null,
    ).use { c ->
        buildList {
            while (c.moveToNext()) {
                add(Session(c.getLong(0), c.getLong(1), c.getInt(2), c.getInt(3), c.getString(4), c.getString(5), c.getString(6)))
            }
        }
    }

    fun session(id: Long): Session? = sessions().firstOrNull { it.id == id }

    fun addSession(
        start: Long, plannedSec: Int, satSec: Int, note: String = "",
        kind: String = Session.SIT, pattern: String = "",
    ): Long {
        val id = writableDatabase.insert("session", null, ContentValues().apply {
            put("start", start)
            put("planned", plannedSec)
            put("sat", satSec)
            put("note", note)
            put("kind", kind)
            put("pattern", pattern)
        })
        changed()
        return id
    }

    /** Changes a logged session; [start] is given only when it is moved (a sitting added by hand). */
    fun updateSession(id: Long, satSec: Int, note: String, start: Long? = null) {
        writableDatabase.update("session", ContentValues().apply {
            put("sat", satSec)
            put("note", note)
            if (start != null) put("start", start)
        }, "id = ?", arrayOf(id.toString()))
        changed()
    }

    fun deleteSession(id: Long) {
        writableDatabase.delete("session", "id = ?", arrayOf(id.toString()))
        changed()
    }

    // ---- restore

    /** Replaces everything with [b], in one transaction: a backup that fails to read changes nothing. */
    fun replaceAll(b: Backup.Contents) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("tick", null, null)
            db.delete("rest", null, null)
            db.delete("habit", null, null)
            db.delete("session", null, null)
            if (b.habits.none { it.habit.builtin }) addMeditation(db)
            for (h in b.habits) {
                val id = db.insert("habit", null, ContentValues().apply {
                    put("name", if (h.habit.builtin) "" else h.habit.name)
                    put("per_week", h.habit.perWeek.coerceIn(1, 7))
                    put("builtin", if (h.habit.builtin) 1 else 0)
                    put("position", h.habit.position)
                    put("cue", h.habit.cue)
                })
                for (d in h.rests) {
                    db.insertWithOnConflict("rest", null, ContentValues().apply {
                        put("habit_id", id)
                        put("day", d.toEpochDay())
                    }, SQLiteDatabase.CONFLICT_IGNORE)
                }
                if (!h.habit.builtin) for (d in h.ticks - h.rests) {
                    db.insertWithOnConflict("tick", null, ContentValues().apply {
                        put("habit_id", id)
                        put("day", d.toEpochDay())
                    }, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
            for (s in b.sessions) {
                db.insert("session", null, ContentValues().apply {
                    put("start", s.start)
                    put("planned", s.plannedSec)
                    put("sat", s.satSec)
                    put("note", s.note)
                    put("kind", s.kind)
                    put("pattern", s.pattern)
                })
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        changed()
    }

    companion object {
        @Volatile private var instance: Store? = null
        fun get(context: Context): Store =
            instance ?: synchronized(this) { instance ?: Store(context.applicationContext).also { instance = it } }
    }
}
