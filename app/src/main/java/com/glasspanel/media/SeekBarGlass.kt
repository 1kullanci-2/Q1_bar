package com.glasspanel.media

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/** İnce, cam görünümlü ilerleme çubuğu. Parmakla sürüklenerek sarılabilir. */
class SeekBarGlass(context: Context) : View(context) {

    var fraction: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var onSeek: ((Float) -> Unit)? = null
    var onDrag: ((Float) -> Unit)? = null

    private var dragging = false
    private var dragFraction = 0f

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(80, 255, 255, 255) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(240, 255, 255, 255) }
    private val rect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = context.dpi(28f)
        setMeasuredDimension(resolveSize(suggestedMinimumWidth, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val barH = context.dpf(if (dragging) 8f else 5f)
        val top = (h - barH) / 2f
        val r = barH / 2f
        rect.set(0f, top, w, top + barH)
        canvas.drawRoundRect(rect, r, r, track)
        val f = if (dragging) dragFraction else fraction
        val fw = (w * f).coerceAtLeast(0f)
        if (fw > 0f) {
            rect.set(0f, top, fw.coerceAtLeast(barH), top + barH)
            canvas.drawRoundRect(rect, r, r, fill)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (width <= 0) return false
        val f = (event.x / width).coerceIn(0f, 1f)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                dragging = true
                dragFraction = f
                onDrag?.invoke(f)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                dragFraction = f
                onDrag?.invoke(f)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                dragging = false
                onSeek?.invoke(f)
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                invalidate()
            }
        }
        return true
    }
}
