package com.glasspanel.media

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable

/**
 * Kullanıcının duvar kâğıdını okuyup ekran oranına kırpar ve güçlü biçimde bulanıklaştırır.
 * Kartlar, ekrandaki konumlarına denk gelen bölgeyi çizerek gerçek cam etkisi verir.
 */
object WallpaperBackdrop {
    private var cache: Bitmap? = null
    private var cacheKey: String? = null

    @Synchronized
    fun load(ctx: Context, sw: Int, sh: Int): Bitmap? {
        if (sw <= 0 || sh <= 0) return null
        val wm = WallpaperManager.getInstance(ctx)
        val key = try {
            "${wm.getWallpaperId(WallpaperManager.FLAG_LOCK)}_${wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM)}_${sw}x$sh"
        } catch (_: Exception) {
            "x_${sw}x$sh"
        }
        if (cache != null && key == cacheKey) return cache
        val raw = decode(wm) ?: return null
        val out = try {
            build(raw, sw, sh)
        } catch (_: Exception) {
            null
        }
        if (out != null) {
            cache = out
            cacheKey = key
        }
        return out
    }

    private fun fromFile(wm: WallpaperManager, which: Int): Bitmap? {
        return try {
            wm.getWallpaperFile(which)?.use { pfd ->
                val o = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, o)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decode(wm: WallpaperManager): Bitmap? {
        fromFile(wm, WallpaperManager.FLAG_LOCK)?.let { return it }
        fromFile(wm, WallpaperManager.FLAG_SYSTEM)?.let { return it }
        return try {
            (wm.drawable as? BitmapDrawable)?.bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun build(src: Bitmap, sw: Int, sh: Int): Bitmap {
        val target = sw.toFloat() / sh
        val ratio = src.width.toFloat() / src.height
        val cw: Int
        val ch: Int
        if (ratio > target) {
            ch = src.height
            cw = (ch * target).toInt().coerceIn(1, src.width)
        } else {
            cw = src.width
            ch = (cw / target).toInt().coerceIn(1, src.height)
        }
        val x = (src.width - cw) / 2
        val y = (src.height - ch) / 2
        var cur = Bitmap.createBitmap(src, x, y, cw, ch)
        for (w in intArrayOf(160, 80, 40, 32)) {
            val h = (w / target).toInt().coerceAtLeast(1)
            cur = Bitmap.createScaledBitmap(cur, w, h, true)
        }
        return cur
    }
}
