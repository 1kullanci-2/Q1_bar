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
 *  - üstte büyük cam kart: kapak (sol üstte uygulama simgesi), şarkı / sanatçı / albüm
 *  - altta ince geniş cam kart: süre çubuğu, geri / oynat-durdur / ileri
 * Kapak boyutu, panele verilen yüksekliğe göre otomatik ayarlanır.
 */
class PanelView(context: Context) : LinearLayout(context) {

    private val cardTop = GlassCard(context)
    private val cardBottom = GlassCard(context)
    private val topInner = LinearLayout(context)

    private val appText = TextView(context)
    private val artBox = FrameLayout(context)
    private val art = ImageView(context)
    private val badge = ImageView(context)
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

    fun setScreenBackdrop(bmp: Bitmap?, sw: Int, sh: Int) {
        for (c in listOf(cardTop, cardBottom)) {
            c.screenW = sw
            c.screenH = sh
            c.screenBackdrop = bmp
        }
    }

    private fun buildTop() {
        val pad = context.dpi(20f)
        topInner.orientation = VERTICAL
        topInner.setPadding(pad, context.dpi(16f), pad, pad)

        appText.apply {
            setTextColor(Color.argb(200, 255, 255, 255))
            textSize = 12f
            letterSpacing = 0.06f
            maxLines = 1
        }
        topInner.addView(appText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

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
        artBox.addView(art, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        badge.apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
            elevation = context.dpf(10f)
            visibility = View.GONE
        }
        artBox.addView(
            badge,
            FrameLayout.LayoutParams(context.dpi(32f), context.dpi(32f), Gravity.TOP or Gravity.START).apply {
                topMargin = context.dpi(10f)
                leftMargin = context.dpi(10f)
            }
        )

        // boyutu onMeasure içinde ekran yüksekliğine göre belirlenir
        topInner.addView(
            artBox,
            LayoutParams(0, 0).apply {
                gravity = Gravity.CENTER_HORIZONTAL
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
        topInner.addView(
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
        topInner.addView(
            artist,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(2f)
            }
        )

        album.apply {
            setTextColor(Color.argb(160, 255, 255, 255))
            textSize = 14f
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
        }
        topInner.addView(
            album,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(2f)
            }
        )

        cardTop.addView(
            topInner,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
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
            it.setTextColor(Color.argb(215, 255, 255, 255))
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

        cardBottom.addView(
            inner,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
    }

    private fun styleButton(b: ImageButton, res: Int, padDp: Float) {
        b.setImageResource(res)
        b.scaleType = ImageView.ScaleType.FIT_CENTER
        b.setBackgroundColor(Color.TRANSPARENT)
        val p = context.dpi(padDp)
        b.setPadding(p, p, p, p)
    }

    /** Verilen yüksekliğe sığacak en büyük kare kapağı hesaplar (genişlik sınırını aşmaz). */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val availH = MeasureSpec.getSize(heightMeasureSpec)
        val lp = artBox.layoutParams as LayoutParams
        val minSide = context.dpi(110f)
        val innerW = (w - paddingLeft - paddingRight - 2 * context.dpi(20f)).coerceAtLeast(0)
        val maxSide = maxOf(innerW, minSide)
        var side = maxSide

        if (hMode != MeasureSpec.UNSPECIFIED && w > 0) {
            lp.width = 0
            lp.height = 0
            val ws = MeasureSpec.makeMeasureSpec(w - paddingLeft - paddingRight, MeasureSpec.EXACTLY)
            val us = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            cardTop.measure(ws, us)
            cardBottom.measure(ws, us)
            val gap = context.dpi(12f)
            side = (availH - cardTop.measuredHeight - cardBottom.measuredHeight - gap)
                .coerceIn(minSide, maxSide)
        }

        if (lp.width != side || lp.height != side) {
            lp.width = side
            lp.height = side
        }
        // yukarıdaki deneme ölçümlerinden kalan önbelleği geçersiz kıl
        artBox.forceLayout()
        topInner.forceLayout()
        cardTop.forceLayout()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
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

    private fun avgColor(b: Bitmap, y0: Int, y1: Int): Int {
        var r = 0L
        var g = 0L
        var bl = 0L
        var n = 0L
        for (y in y0 until y1) {
            for (x in 0 until b.width) {
                val p = b.getPixel(x, y)
                r += Color.red(p)
                g += Color.green(p)
                bl += Color.blue(p)
                n++
            }
        }
        if (n == 0L) return GlassCard.DEFAULT_TOP
        val c = Color.rgb((r / n).toInt(), (g / n).toInt(), (bl / n).toInt())
        val hsv = FloatArray(3)
        Color.colorToHSV(c, hsv)
        hsv[1] = (hsv[1] * 1.25f).coerceAtMost(1f)
        hsv[2] = hsv[2].coerceIn(0.28f, 0.80f)
        return Color.HSVToColor(hsv)
    }

    private fun applyTints(b: Bitmap?) {
        if (b == null || b.height <= 0) {
            cardTop.tintTop = GlassCard.DEFAULT_TOP
            cardTop.tintBottom = GlassCard.DEFAULT_BOTTOM
            cardBottom.tintTop = GlassCard.DEFAULT_TOP
            cardBottom.tintBottom = GlassCard.DEFAULT_BOTTOM
            return
        }
        val mid = b.height / 2
        val top = avgColor(b, 0, maxOf(mid, 1))
        val bottom = avgColor(b, minOf(mid, b.height - 1), b.height)
        cardTop.tintTop = top
        cardTop.tintBottom = bottom
        cardBottom.tintTop = top
        cardBottom.tintBottom = bottom
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
            artist.visibility = View.VISIBLE
            album.visibility = View.GONE
            badge.visibility = View.GONE
            art.setImageDrawable(placeholderArt())
            lastArt = null
            lastBlur = null
            applyTints(null)
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

        if (i.appIcon != null) {
            badge.setImageBitmap(i.appIcon)
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }

        val bmp = i.art
        if (bmp != null) {
            art.setImageBitmap(bmp)
            if (bmp !== lastArt) {
                lastArt = bmp
                lastBlur = blur(bmp)
                applyTints(lastBlur)
            }
        } else {
            art.setImageDrawable(placeholderArt())
            if (lastArt != null || lastBlur != null) {
                lastArt = null
                lastBlur = null
                applyTints(null)
            }
        }

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
