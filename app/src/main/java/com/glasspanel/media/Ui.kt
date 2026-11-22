package com.glasspanel.media

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/** Krem + lime Material arayüz için ortak küçük yapı taşları. */
object Ui {

    fun c(ctx: Context, id: Int): Int = ctx.getColor(id)

    fun text(ctx: Context, s: String, sp: Float, color: Int = ctx.getColor(R.color.ink), bold: Boolean = false): TextView {
        val t = TextView(ctx)
        t.text = s
        t.textSize = sp
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    fun soft(ctx: Context, s: String, sp: Float = 13f): TextView = text(ctx, s, sp, ctx.getColor(R.color.ink_soft))

    fun section(ctx: Context, s: String): TextView {
        val t = text(ctx, s, 12f, ctx.getColor(R.color.ink_soft), bold = true)
        t.letterSpacing = 0.1f
        return t
    }

    fun lp(ctx: Context, top: Float = 0f): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        p.topMargin = ctx.dpi(top)
        return p
    }

    /** Karta eklenen dikey iç yerleşimi döndürür; kartı parent'a ekler. */
    fun card(ctx: Context, parent: LinearLayout, top: Float = 8f): LinearLayout {
        val card = MaterialCardView(ctx)
        card.radius = ctx.dpf(22f)
        card.cardElevation = 0f
        card.setCardBackgroundColor(ctx.getColor(R.color.surface))
        card.strokeColor = ctx.getColor(R.color.outline)
        card.strokeWidth = ctx.dpi(1f)
        val inner = LinearLayout(ctx)
        inner.orientation = LinearLayout.VERTICAL
        inner.setPadding(ctx.dpi(18f), ctx.dpi(16f), ctx.dpi(18f), ctx.dpi(16f))
        card.addView(inner, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        parent.addView(card, lp(ctx, top))
        return inner
    }

    fun divider(ctx: Context): View {
        val v = View(ctx)
        v.setBackgroundColor(ctx.getColor(R.color.outline))
        v.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dpi(1f))
        return v
    }

    /** Lime dolgulu ana düğme */
    fun filled(ctx: Context, label: String, onClick: () -> Unit): MaterialButton {
        val b = MaterialButton(ctx)
        b.text = label
        b.isAllCaps = false
        b.textSize = 15f
        b.cornerRadius = ctx.dpi(16f)
        b.backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.lime))
        b.setTextColor(ctx.getColor(R.color.ink))
        b.minimumHeight = ctx.dpi(50f)
        b.setOnClickListener { onClick() }
        return b
    }

    /** Çerçeveli ikincil düğme */
    fun outlined(ctx: Context, label: String, onClick: () -> Unit): MaterialButton {
        val b = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        b.text = label
        b.isAllCaps = false
        b.textSize = 15f
        b.cornerRadius = ctx.dpi(16f)
        b.setTextColor(ctx.getColor(R.color.ink))
        b.strokeColor = ColorStateList.valueOf(ctx.getColor(R.color.outline))
        b.minimumHeight = ctx.dpi(50f)
        b.setOnClickListener { onClick() }
        return b
    }

    /** Durum hapı: açık = yeşil, kapalı = kırmızımsı */
    fun setPill(t: TextView, on: Boolean, onText: String = "Açık", offText: String = "Kapalı") {
        val ctx = t.context
        t.text = if (on) onText else offText
        val col = ctx.getColor(if (on) R.color.green_ok else R.color.red_off)
        t.setTextColor(col)
        t.textSize = 12f
        t.typeface = Typeface.DEFAULT_BOLD
        t.setPadding(ctx.dpi(12f), ctx.dpi(5f), ctx.dpi(12f), ctx.dpi(5f))
        val bg = GradientDrawable()
        bg.cornerRadius = ctx.dpf(14f)
        bg.setColor(withAlpha(col, 28))
        t.background = bg
    }

    /** Başlık + açıklama solda, sağda durum hapı veya ok olan dokunulabilir satır */
    fun row(ctx: Context, title: String, desc: String, status: TextView?, onClick: () -> Unit): LinearLayout {
        val r = LinearLayout(ctx)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(0, ctx.dpi(12f), 0, ctx.dpi(12f))
        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        col.addView(text(ctx, title, 15f, bold = true))
        col.addView(soft(ctx, desc), lp(ctx, 2f))
        r.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        p.leftMargin = ctx.dpi(12f)
        r.addView(status ?: soft(ctx, "›", 22f), p)
        r.setOnClickListener { onClick() }
        return r
    }
}
