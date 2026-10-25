package com.glasspanel.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout

/**
 * Liquid Glass görünümlü kart:
 *  - arkada kullanıcının bulanık duvar kâğıdı (kartın ekrandaki konumuna denk bölge)
 *  - üstünde kapak resminin renklerinden oluşan renk tonu
 *  - süt camı tonu, üst ışık yansıması ve gradyan kenar çizgisi
 */
class GlassCard(context: Context) : FrameLayout(context) {

    companion object {
        const val DEFAULT_TOP = 0xFF3A4766.toInt()
        const val DEFAULT_BOTTOM = 0xFF171B29.toInt()
    }

    var screenBackdrop: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    var screenW: Int = 0
    var screenH: Int = 0

    var tintTop: Int = DEFAULT_TOP
        set(value) {
            field = value
            rebuildTint()
            invalidate()
        }

    var tintBottom: Int = DEFAULT_BOTTOM
        set(value) {
            field = value
            rebuildTint()
            invalidate()
        }

    // 0..100 (yüksek = daha şeffaf cam)
    var glass: Int = 60
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    private val radius = context.dpf(30f)
    private val strokeW = context.dpf(1.3f)

    private val rect = RectF()
    private val strokeRect = RectF()
    private val src = Rect()
    private val loc = IntArray(2)

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        val cm = ColorMatrix()
        cm.setSaturation(1.35f)
        colorFilter = ColorMatrixColorFilter(cm)
    }
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scrim = Paint(Paint.ANTI_ALIAS_FLAG)
    private val milk = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeW
    }

    init {
        setWillNotDraw(false)
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        clipToOutline = true
    }

    private fun rebuildTint() {
        if (height <= 0) return
        tintPaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(), tintTop, tintBottom, Shader.TileMode.CLAMP
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        basePaint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            0xFF2E3850.toInt(), 0xFF141824.toInt(), Shader.TileMode.CLAMP
        )
        rebuildTint()
        sheen.shader = LinearGradient(
            0f, 0f, 0f, h * 0.55f,
            Color.argb(70, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP
        )
        stroke.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(
                Color.argb(190, 255, 255, 255),
                Color.argb(25, 255, 255, 255),
                Color.argb(110, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)

        // taban
        canvas.drawRoundRect(rect, radius, radius, basePaint)

        // duvar kâğıdı: kartın ekrandaki yerine denk gelen bölge
        val bm = screenBackdrop
        if (bm != null && !bm.isRecycled && screenW > 0 && screenH > 0) {
            getLocationOnScreen(loc)
            val sx = bm.width.toFloat() / screenW
            val sy = bm.height.toFloat() / screenH
            val l = (loc[0] * sx).toInt().coerceIn(0, bm.width - 1)
            val t = (loc[1] * sy).toInt().coerceIn(0, bm.height - 1)
            val r = ((loc[0] + width) * sx).toInt().coerceIn(l + 1, bm.width)
            val b = ((loc[1] + height) * sy).toInt().coerceIn(t + 1, bm.height)
            src.set(l, t, r, b)
            canvas.drawBitmap(bm, src, rect, bmpPaint)
        }

        val a = glass / 100f
        // kapaktan gelen renk tonu
        tintPaint.alpha = (255 * (0.58f - 0.16f * a)).toInt()
        canvas.drawRoundRect(rect, radius, radius, tintPaint)
        // okunabilirlik için hafif koyu perde
        scrim.color = Color.argb((255 * (0.30f - 0.16f * a)).toInt(), 6, 8, 14)
        canvas.drawRoundRect(rect, radius, radius, scrim)
        // süt camı tonu
        milk.color = Color.argb((255 * (0.05f + 0.06f * a)).toInt(), 255, 255, 255)
        canvas.drawRoundRect(rect, radius, radius, milk)
        // üst ışık yansıması
        canvas.drawRoundRect(rect, radius, radius, sheen)

        // kenar çizgisi
        val half = strokeW / 2f
        strokeRect.set(half, half, w - half, h - half)
        canvas.drawRoundRect(strokeRect, radius - half, radius - half, stroke)
    }
}
