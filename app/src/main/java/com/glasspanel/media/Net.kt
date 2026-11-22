package com.glasspanel.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL

/** Çok küçük ağ yardımcıları: arka planda çalıştır, sonucu ana iş parçacığında ver. */
object Net {
    private val main = Handler(Looper.getMainLooper())

    fun <T> async(work: () -> T, done: (Result<T>) -> Unit) {
        Thread {
            val r = runCatching(work)
            main.post { done(r) }
        }.start()
    }

    fun open(url: String, method: String = "GET"): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 12000
        c.readTimeout = 20000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "MedyaPaneli")
        c.setRequestProperty("Accept", "application/vnd.github+json")
        return c
    }

    fun getText(url: String): String {
        val c = open(url)
        try {
            if (c.responseCode !in 200..299) throw RuntimeException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun getBitmap(url: String): Bitmap? {
        val c = open(url)
        try {
            if (c.responseCode !in 200..299) return null
            return c.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            c.disconnect()
        }
    }
}
