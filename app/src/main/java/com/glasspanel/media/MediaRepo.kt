package com.glasspanel.media

import android.graphics.Bitmap
import android.media.session.MediaController
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.CopyOnWriteArrayList

data class TrackInfo(
    val title: String,
    val artist: String,
    val album: String,
    val art: Bitmap?,
    val durationMs: Long,
    val positionMs: Long,
    val updatedAt: Long,
    val speed: Float,
    val isPlaying: Boolean,
    val appName: String,
    val packageName: String
) {
    fun currentPosition(): Long {
        if (!isPlaying) return positionMs.coerceAtLeast(0L)
        val p = positionMs + ((SystemClock.elapsedRealtime() - updatedAt) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0L, durationMs) else p.coerceAtLeast(0L)
    }
}

object MediaRepo {
    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    @Volatile
    var controller: MediaController? = null
        private set

    @Volatile
    var info: TrackInfo? = null
        private set

    fun addListener(l: () -> Unit) {
        listeners.add(l)
    }

    fun removeListener(l: () -> Unit) {
        listeners.remove(l)
    }

    fun publish(c: MediaController?, i: TrackInfo?) {
        controller = c
        info = i
        main.post { listeners.forEach { it() } }
    }
}
