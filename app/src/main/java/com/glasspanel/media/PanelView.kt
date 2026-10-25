package com.glasspanel.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Kilit ekranı ve uygulama içi önizleme için ortak panel:
 *  - üstte büyük cam kart: kapak, şarkı / sanatçı / albüm
 *  - altta ince geniş cam kart: süre çubuğu, geri / oynat-durdur / ileri
 */
class PanelView(context: Context) : LinearLayout(context) {

    private class SquareImageView(context: Context) : ImageView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        }
    }

    private val cardTop = GlassCard(context)
    private val cardBottom = GlassCard(context)

    private val appText = TextView(context)
    private val art = SquareImageView(context)
    private val title = TextView(context)
    private val artist = TextView(context)
    private val album = TextView(context)

    private val seek = SeekBarGlass(context)
    private val timeCur = TextView(context)
    private val timeTotal = TextView(context)
    private val btnPrev = ImageButton(context)
    private val btnPlay = ImageButton(context)
    private val btnNext = ImageButton(context)

    private var lastArt: Bitmap? = null
    private var lastBlur: Bitmap? = null

    private val listener: () -> Unit = { render() }

    private val ticker = object : Runnable {
        override fun run() {
            tick()
            postDelayed(this, 500)
        }
    }

    init {
        orientation = VERTICAL
        isClickable = true
        buildTop()
        buildBottom()
        addView(cardTop, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(
            cardBottom,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(12f)
            }
        )
    }

    private fun buildTop() {
        val pad = context.dpi(20f)
        val inner = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(pad, context.dpi(16f), pad, pad)
        }

        appText.apply {
            setTextColor(Color.argb(190, 255, 255, 255))
            textSize = 12f
            letterSpacing = 0.06f
            maxLines = 1
        }
        inner.addView(appText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        art.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, context.dpf(22f))
                }
            }
            clipToOutline = true
            elevation = context.dpf(6f)
        }
        inner.addView(
            art,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(10f)
            }
        )

        title.apply {
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
            isSelected = true
        }
        inner.addView(
            title,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(16f)
            }
        )

        artist.apply {
            setTextColor(Color.argb(235, 255, 255, 255))
            textSize = 16f
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
        }
        inner.addView(
            artist,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(2f)
            }
        )

        album.apply {
            setTextColor(Color.argb(150, 255, 255, 255))
            textSize = 14f
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
        }
        inner.addView(
            album,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(2f)
            }
        )

        cardTop.addView(inner, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun buildBottom() {
        val inner = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(context.dpi(22f), context.dpi(12f), context.dpi(22f), context.dpi(14f))
        }

        val timeRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listOf(timeCur, timeTotal).forEach {
            it.setTextColor(Color.argb(200, 255, 255, 255))
            it.textSize = 12f
            it.typeface = Typeface.DEFAULT_BOLD
        }
        timeCur.minWidth = context.dpi(36f)
        timeTotal.minWidth = context.dpi(36f)
        timeTotal.gravity = Gravity.END
        timeRow.addView(timeCur, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        timeRow.addView(
            seek,
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = context.dpi(8f)
                rightMargin = context.dpi(8f)
            }
        )
        timeRow.addView(timeTotal, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        inner.addView(timeRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val btnRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }
        styleButton(btnPrev, R.drawable.ic_prev, 14f)
        styleButton(btnPlay, R.drawable.ic_play, 18f)
        styleButton(btnNext, R.drawable.ic_next, 14f)
        btnPlay.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(60, 255, 255, 255))
            setStroke(context.dpi(1f), Color.argb(90, 255, 255, 255))
        }
        val side = context.dpi(56f)
        val mid = context.dpi(72f)
        btnRow.addView(btnPrev, LayoutParams(side, side))
        btnRow.addView(
            btnPlay,
            LayoutParams(mid, mid).apply {
                leftMargin = context.dpi(22f)
                rightMargin = context.dpi(22f)
            }
        )
        btnRow.addView(btnNext, LayoutParams(side, side))
        inner.addView(
            btnRow,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(2f)
            }
        )

        seek.onDrag = { f ->
            val i = MediaRepo.info
            if (i != null && i.durationMs > 0) timeCur.text = formatTime((f * i.durationMs).toLong())
        }
        seek.onSeek = { f ->
            val i = MediaRepo.info
            if (i != null && i.durationMs > 0) {
                MediaRepo.controller?.transportControls?.seekTo((f * i.durationMs).toLong())
            }
        }
        btnPrev.setOnClickListener { MediaRepo.controller?.transportControls?.skipToPrevious() }
        btnNext.setOnClickListener { MediaRepo.controller?.transportControls?.skipToNext() }
        btnPlay.setOnClickListener {
            val tc = MediaRepo.controller?.transportControls
            if (MediaRepo.info?.isPlaying == true) tc?.pause() else tc?.play()
        }

        cardBottom.addView(inner, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun styleButton(b: ImageButton, res: Int, padDp: Float) {
        b.setImageResource(res)
        b.scaleType = ImageView.ScaleType.FIT_CENTER
        b.setBackgroundColor(Color.TRANSPARENT)
        val p = context.dpi(padDp)
        b.setPadding(p, p, p, p)
    }

    private fun placeholderArt() = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(0xFF4A5675.toInt(), 0xFF262B3D.toInt())
    )

    private fun blur(src: Bitmap): Bitmap? {
        return try {
            if (src.isRecycled || src.width <= 0 || src.height <= 0) return null
            val h1 = (48f * src.height / src.width).toInt().coerceAtLeast(1)
            val a = Bitmap.createScaledBitmap(src, 48, h1, true)
            val h2 = (12f * h1 / 48f).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(a, 12, h2, true)
        } catch (_: Exception) {
            null
        }
    }

    fun render() {
        val g = Prefs.glass(context)
        cardTop.glass = g
        cardBottom.glass = g

        val i = MediaRepo.info
        if (i == null) {
            appText.text = "MEDYA PANELİ"
            title.text = "Çalan bir şey yok"
            artist.text = "Bir müzik uygulamasında şarkı başlat"
            album.visibility = View.GONE
            art.setImageDrawable(placeholderArt())
            cardTop.backdrop = null
            cardBottom.backdrop = null
            btnPlay.setImageResource(R.drawable.ic_play)
            timeCur.text = "0:00"
            timeTotal.text = "--:--"
            seek.fraction = 0f
            return
        }

        appText.text = i.appName.uppercase()
        title.text = i.title.ifBlank { "Bilinmeyen parça" }
        artist.text = i.artist
        artist.visibility = if (i.artist.isBlank()) View.GONE else View.VISIBLE
        album.text = i.album
        album.visibility = if (i.album.isBlank()) View.GONE else View.VISIBLE

        val bmp = i.art
        if (bmp != null) {
            art.setImageBitmap(bmp)
            if (bmp !== lastArt) {
                lastArt = bmp
                lastBlur = blur(bmp)
            }
        } else {
            art.setImageDrawable(placeholderArt())
            lastArt = null
            lastBlur = null
        }
        cardTop.backdrop = lastBlur
        cardBottom.backdrop = lastBlur

        btnPlay.setImageResource(if (i.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        tick()
    }

    private fun tick() {
        val i = MediaRepo.info ?: return
        val pos = i.currentPosition()
        timeCur.text = formatTime(pos)
        timeTotal.text = if (i.durationMs > 0) formatTime(i.durationMs) else "--:--"
        seek.fraction = if (i.durationMs > 0) pos.toFloat() / i.durationMs else 0f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        MediaRepo.addListener(listener)
        render()
        post(ticker)
    }

    override fun onDetachedFromWindow() {
        MediaRepo.removeListener(listener)
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }
}
