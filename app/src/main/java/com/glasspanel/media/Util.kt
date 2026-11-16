package com.glasspanel.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color

fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density

fun Context.dpi(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}

fun withAlpha(color: Int, alpha: Int): Int =
    Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

/** Kapak resminden koyu bir zemin rengi ve parlak bir vurgu rengi üretir. */
object CoverPalette {
    const val DEFAULT_BASE = 0xFF1E2535.toInt()
    const val DEFAULT_ACCENT = 0xFFA9BBFF.toInt()
    private const val NEUTRAL_ACCENT = 0xFFEDEFF5.toInt()

    private fun average(b: Bitmap): FloatArray? {
        return try {
            val s = Bitmap.createScaledBitmap(b, 16, 16, true)
            var r = 0L
            var g = 0L
            var bl = 0L
            for (y in 0 until 16) {
                for (x in 0 until 16) {
                    val p = s.getPixel(x, y)
                    r += Color.red(p)
                    g += Color.green(p)
                    bl += Color.blue(p)
                }
            }
            val hsv = FloatArray(3)
            Color.colorToHSV(Color.rgb((r / 256).toInt(), (g / 256).toInt(), (bl / 256).toInt()), hsv)
            hsv
        } catch (_: Exception) {
            null
        }
    }

    fun base(b: Bitmap?): Int {
        if (b == null) return DEFAULT_BASE
        val hsv = average(b) ?: return DEFAULT_BASE
        if (hsv[1] < 0.10f) {
            hsv[1] = 0f
        } else {
            hsv[1] = (hsv[1] * 1.1f).coerceAtMost(0.85f)
        }
        hsv[2] = hsv[2].coerceIn(0.17f, 0.34f)
        return Color.HSVToColor(hsv)
    }

    fun accent(b: Bitmap?): Int {
        if (b == null) return DEFAULT_ACCENT
        val hsv = average(b) ?: return DEFAULT_ACCENT
        if (hsv[1] < 0.12f) return NEUTRAL_ACCENT
        hsv[1] = hsv[1].coerceIn(0.38f, 0.80f)
        hsv[2] = 0.96f
        return Color.HSVToColor(hsv)
    }
}
