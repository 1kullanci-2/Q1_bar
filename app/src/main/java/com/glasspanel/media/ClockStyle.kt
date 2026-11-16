package com.glasspanel.media

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.TextClock

/** Saat ve tarih için yazı tipleri ve ortak biçimlendirme. */
object ClockStyle {
    val FONT_NAMES = listOf("Outfit", "Fraunces", "Space Grotesk")

    private fun load(ctx: Context, res: Int): Typeface {
        return try {
            ctx.resources.getFont(res)
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    fun timeFont(ctx: Context, idx: Int): Typeface = load(
        ctx,
        when (idx) {
            1 -> R.font.fraunces_time
            2 -> R.font.grotesk_time
            else -> R.font.outfit_time
        }
    )

    fun dateFont(ctx: Context, idx: Int): Typeface = load(
        ctx,
        when (idx) {
            1 -> R.font.fraunces_date
            2 -> R.font.grotesk_date
            else -> R.font.outfit_date
        }
    )

    /** Seçili ayarlara göre saat ve tarih görünümünü uygular. */
    fun apply(ctx: Context, time: TextClock, date: TextClock, baseTimeSp: Float, accent: Int) {
        val font = Prefs.clockFont(ctx)
        time.typeface = timeFont(ctx, font)
        date.typeface = dateFont(ctx, font)

        time.textSize = baseTimeSp * (Prefs.clockSize(ctx) / 100f)
        val fmt = if (Prefs.clock24(ctx)) "HH:mm" else "h:mm"
        time.format24Hour = fmt
        time.format12Hour = fmt

        date.visibility = if (Prefs.showDate(ctx)) View.VISIBLE else View.GONE
        date.setTextColor(accent)
    }
}
