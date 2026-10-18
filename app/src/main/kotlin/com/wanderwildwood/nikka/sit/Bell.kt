package com.wanderwildwood.nikka.sit

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.wanderwildwood.nikka.R

/**
 * The bowl, played on the ALARM stream: its loudness is the phone's alarm volume, so a phone
 * left with the media volume down still rings the end of a sitting, and one with the ringer
 * silenced does too. The stock app's bell followed media volume, which was the complaint.
 *
 * The CPU is held awake for as long as the sound lasts, and no longer.
 */
object Bell {

    private const val HOLD_MS = 25_000L

    // Held here until each finishes. A MediaPlayer nothing refers to is collected and released
    // part-way through its sound: seen when an alarm started a fresh process, where the end bell
    // stopped after a second and a half and nothing said so.
    private val playing = mutableSetOf<MediaPlayer>()
    private val main = Handler(Looper.getMainLooper())

    fun play(context: Context, end: Boolean, onDone: () -> Unit = {}) {
        val app = context.applicationContext
        val wake = app.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nikka:bell")
            .apply { setReferenceCounted(false); acquire(HOLD_MS) }
        val player = MediaPlayer()
        var finished = false
        val done = {
            if (!finished) {
                finished = true
                playing.remove(player)
                if (wake.isHeld) wake.release()
                onDone()
            }
        }
        playing.add(player)
        // However the sound ends, or fails to, the service is let go once it must be over.
        main.postDelayed({ if (!finished) { runCatching { player.release() }; done() } }, HOLD_MS)
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            app.resources.openRawResourceFd(if (end) R.raw.bowl_end else R.raw.bowl).use {
                player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            player.setOnCompletionListener { mp -> mp.release(); done() }
            player.setOnErrorListener { mp, _, _ -> mp.release(); done(); true }
            player.prepare()
            player.start()
        } catch (e: Exception) {
            player.release()
            done()
        }
    }
}
