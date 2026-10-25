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
 * Liquid Glass görünümlü kart: kapak resminden üretilmiş bulanık arka plan,
 * açık/koyu cam tonu, üstte ışık yansıması ve gradyan kenar çizgisi.
 */
class GlassCard(context: Context) : FrameLayout(context) {

    var backdrop: Bitmap? = null
        set(value) {
            field = value
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

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        val cm = ColorMatrix()
        cm.setSaturation(1.5f)
        colorFilter = ColorMatrixColorFilter(cm)
    }
    private val scrim = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tint = Paint(Paint.ANTI_ALIAS_FLAG)
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        basePaint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            0xFF3A4766.toInt(), 0xFF171B29.toInt(), Shader.TileMode.CLAMP
        )
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

        // bulanık kapak (merkezden kırparak doldur)
        backdrop?.let { bmp ->
            if (!bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
                val boxRatio = w / h
                val bmpRatio = bmp.width.toFloat() / bmp.height
                if (bmpRatio > boxRatio) {
                    val sw = (bmp.height * boxRatio).toInt().coerceAtLeast(1)
                    val l = (bmp.width - sw) / 2
                    src.set(l, 0, l + sw, bmp.height)
                } else {
                    val sh = (bmp.width / boxRatio).toInt().coerceAtLeast(1)
                    val t = (bmp.height - sh) / 2
                    src.set(0, t, bmp.width, t + sh)
                }
                canvas.drawBitmap(bmp, src, rect, bmpPaint)
            }
        }

        val a = glass / 100f
        // koyu perde (cam ne kadar şeffafsa o kadar hafif)
        scrim.color = Color.argb((255 * (0.52f - 0.36f * a)).toInt(), 8, 10, 16)
        canvas.drawRoundRect(rect, radius, radius, scrim)
        // süt camı tonu
        tint.color = Color.argb((255 * (0.07f + 0.09f * a)).toInt(), 255, 255, 255)
        canvas.drawRoundRect(rect, radius, radius, tint)
        // üst ışık yansıması
        canvas.drawRoundRect(rect, radius, radius, sheen)

        // kenar çizgisi
        val half = strokeW / 2f
        strokeRect.set(half, half, w - half, h - half)
        canvas.drawRoundRect(strokeRect, radius - half, radius - half, stroke)
    }
}
