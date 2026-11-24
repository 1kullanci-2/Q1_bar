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

/** Kapaktan renk alan, arkasında kapağın bulanık hâli olan yuvarlak köşeli kart. */
internal class CoverCard(context: Context) : FrameLayout(context) {

    private val radius = context.dpf(28f)
    private val rect = RectF()
    private val src = Rect()
    private var baseColor = CoverPalette.DEFAULT_BASE
    private var backdrop: Bitmap? = null

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        val cm = ColorMatrix()
        cm.setSaturation(1.25f)
        colorFilter = ColorMatrixColorFilter(cm)
        alpha = 200
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpf(1f)
        color = Color.argb(40, 255, 255, 255)
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

    fun setLook(base: Int, bmp: Bitmap?) {
        baseColor = base
        backdrop = bmp
        rebuildScrim()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildScrim()
    }

    private fun rebuildScrim() {
        val h = height.toFloat()
        if (h <= 0f) return
        scrimPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(withAlpha(baseColor, 90), withAlpha(baseColor, 175), withAlpha(baseColor, 240)),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)

        basePaint.color = baseColor
        canvas.drawRoundRect(rect, radius, radius, basePaint)

        val bmp = backdrop
        if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0 && h > 0f) {
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

        canvas.drawRoundRect(rect, radius, radius, scrimPaint)
        canvas.drawRoundRect(rect, radius, radius, strokePaint)
    }
}

/**
 * Medya paneli, iki ayrı çerçeve:
 *  - üstte büyük kart: uygulama adı kutucuğu, kapak (oranına göre şekillenir, sol üstte uygulama simgesi),
 *    şarkı / sanatçı / albüm
 *  - altta ince kart: süre çubuğu ve önceki / oynat-durdur / sonraki düğmeleri
 * Kapak boyutu, panele verilen yüksekliğe göre otomatik ayarlanır.
 */
class PanelView(context: Context) : LinearLayout(context) {

    var maxCoverDp: Float = 360f

    /** Kapak değişince vurgu rengini dışarıya bildirir. */
    var onPalette: ((Int) -> Unit)? = null

    /** Uygulama adı kutucuğuna ya da simgesine dokunulunca çağrılır. */
    var onOpenApp: (() -> Unit)? = null

    private val cardTop = CoverCard(context)
    private val cardBottom = CoverCard(context)

    private val topContent = LinearLayout(context)
    private val bottomContent = LinearLayout(context)

    private val appChip = TextView(context)
    private val coverBox = FrameLayout(context)
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
    private val btnShuffle = ImageButton(context)
    private val btnRepeat = ImageButton(context)

    private var lastArt: Bitmap? = null
    private var backdrop: Bitmap? = null
    private var baseColor = CoverPalette.DEFAULT_BASE
    private var accentColor = CoverPalette.DEFAULT_ACCENT
    private var ratio = 1f

    private val playBg = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(CoverPalette.DEFAULT_ACCENT)
    }

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
        addView(
            cardTop,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        addView(
            cardBottom,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(10f)
            }
        )
    }

    private fun buildTop() {
        topContent.orientation = LinearLayout.VERTICAL
        topContent.setPadding(context.dpi(20f), context.dpi(16f), context.dpi(20f), context.dpi(18f))

        // uygulama adı: yuvarlak köşeli küçük kutucuk
        val chipBg = GradientDrawable()
        chipBg.cornerRadius = context.dpf(12f)
        chipBg.setColor(Color.argb(55, 255, 255, 255))
        chipBg.setStroke(context.dpi(1f), Color.argb(60, 255, 255, 255))
        appChip.background = chipBg
        appChip.setTextColor(Color.WHITE)
        appChip.textSize = 12f
        appChip.typeface = Typeface.DEFAULT_BOLD
        appChip.letterSpacing = 0.04f
        appChip.maxLines = 1
        appChip.ellipsize = TextUtils.TruncateAt.END
        appChip.setPadding(context.dpi(12f), context.dpi(6f), context.dpi(12f), context.dpi(6f))
        appChip.setOnClickListener { onOpenApp?.invoke() }
        topContent.addView(
            appChip,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        // kapak
        art.scaleType = ImageView.ScaleType.CENTER_CROP
        art.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, context.dpf(22f))
            }
        }
        art.clipToOutline = true
        art.elevation = context.dpf(8f)
        coverBox.addView(
            art,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        badge.scaleType = ImageView.ScaleType.FIT_CENTER
        badge.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        badge.clipToOutline = true
        badge.elevation = context.dpf(12f)
        badge.visibility = View.GONE
        badge.setOnClickListener { onOpenApp?.invoke() }
        coverBox.addView(
            badge,
            FrameLayout.LayoutParams(context.dpi(32f), context.dpi(32f), Gravity.TOP or Gravity.START).apply {
                topMargin = context.dpi(10f)
                leftMargin = context.dpi(10f)
            }
        )
        // asıl boyut onMeasure içinde kapağın oranına göre verilir
        topContent.addView(
            coverBox,
            LinearLayout.LayoutParams(0, 0).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = context.dpi(12f)
            }
        )

        title.setTextColor(Color.WHITE)
        title.textSize = 22f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setSingleLine(true)
        title.ellipsize = TextUtils.TruncateAt.MARQUEE
        title.marqueeRepeatLimit = -1
        title.isSelected = true
        topContent.addView(
            title,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(16f)
            }
        )

        artist.setTextColor(Color.argb(235, 255, 255, 255))
        artist.textSize = 16f
        artist.setSingleLine(true)
        artist.ellipsize = TextUtils.TruncateAt.END
        topContent.addView(
            artist,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(2f)
            }
        )

        album.setTextColor(Color.argb(160, 255, 255, 255))
        album.textSize = 14f
        album.setSingleLine(true)
        album.ellipsize = TextUtils.TruncateAt.END
        topContent.addView(
            album,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpi(2f)
            }
        )

        cardTop.addView(
            topContent,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
    }

    private fun buildBottom() {
        bottomContent.orientation = LinearLayout.VERTICAL
        bottomContent.setPadding(context.dpi(20f), context.dpi(12f), context.dpi(20f), context.dpi(10f))

        val timeRow = LinearLayout(context)
        timeRow.orientation = LinearLayout.HORIZONTAL
        timeRow.gravity = Gravity.CENTER_VERTICAL
        for (t in listOf(timeCur, timeTotal)) {
            t.setTextColor(Color.argb(215, 255, 255, 255))
            t.textSize = 12f
            t.typeface = Typeface.DEFAULT_BOLD
            t.minWidth = context.dpi(36f)
        }
        timeTotal.gravity = Gravity.END
        timeRow.addView(
            timeCur,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        timeRow.addView(
            seek,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = context.dpi(8f)
                rightMargin = context.dpi(8f)
            }
        )
        timeRow.addView(
            timeTotal,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        bottomContent.addView(
            timeRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        val btnRow = LinearLayout(context)
        btnRow.orientation = LinearLayout.HORIZONTAL
        btnRow.gravity = Gravity.CENTER
        styleButton(btnShuffle, R.drawable.ic_shuffle, 10f)
        styleButton(btnRepeat, R.drawable.ic_repeat, 10f)
        styleButton(btnPrev, R.drawable.ic_prev, 11f)
        styleButton(btnPlay, R.drawable.ic_play, 14f)
        styleButton(btnNext, R.drawable.ic_next, 11f)
        btnPlay.background = playBg
        btnPlay.setColorFilter(0xFF10131C.toInt())
        val side = context.dpi(46f)
        val small = context.dpi(40f)
        val mid = context.dpi(60f)
        btnRow.addView(btnShuffle, LinearLayout.LayoutParams(small, small))
        btnRow.addView(
            btnPrev,
            LinearLayout.LayoutParams(side, side).apply { leftMargin = context.dpi(10f) }
        )
        btnRow.addView(
            btnPlay,
            LinearLayout.LayoutParams(mid, mid).apply {
                leftMargin = context.dpi(12f)
                rightMargin = context.dpi(12f)
            }
        )
        btnRow.addView(
            btnNext,
            LinearLayout.LayoutParams(side, side).apply { rightMargin = context.dpi(10f) }
        )
        btnRow.addView(btnRepeat, LinearLayout.LayoutParams(small, small))
        bottomContent.addView(
            btnRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
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
        btnShuffle.setOnClickListener {
            Controls.tapShuffle(context)
            updateToggles()
        }
        btnRepeat.setOnClickListener {
            Controls.tapRepeat(context)
            updateToggles()
        }
        btnPrev.setOnClickListener { MediaRepo.controller?.transportControls?.skipToPrevious() }
        btnNext.setOnClickListener { MediaRepo.controller?.transportControls?.skipToNext() }
        btnPlay.setOnClickListener {
            val tc = MediaRepo.controller?.transportControls
            if (MediaRepo.info?.isPlaying == true) tc?.pause() else tc?.play()
        }

        cardBottom.addView(
            bottomContent,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
    }

    /** Karıştır / tekrar durumu: kapalıyken soluk, açıkken vurgu renginde; tek şarkı tekrarında simgenin ortasında "1". */
    private fun updateToggles() {
        Controls.sync()
        val off = Color.argb(110, 255, 255, 255)
        btnShuffle.setColorFilter(if (Controls.shuffle) accentColor else off)
        btnRepeat.setImageResource(if (Controls.repeat == 2) R.drawable.ic_repeat_one else R.drawable.ic_repeat)
        btnRepeat.setColorFilter(if (Controls.repeat == 0) off else accentColor)
    }

    private fun styleButton(b: ImageButton, res: Int, padDp: Float) {
        b.setImageResource(res)
        b.scaleType = ImageView.ScaleType.FIT_CENTER
        b.setBackgroundColor(Color.TRANSPARENT)
        val p = context.dpi(padDp)
        b.setPadding(p, p, p, p)
    }

    /** Oranı korunan, verilen alana sığan en büyük kapak boyutu. */
    private fun coverSize(maxW: Int, maxH: Int): Pair<Int, Int> {
        val r = ratio
        val cw: Int
        val ch: Int
        if (r >= 1f) {
            cw = minOf(maxW, (maxH * r).toInt())
            ch = (cw / r).toInt()
        } else {
            ch = minOf(maxH, (maxW / r).toInt())
            cw = (ch * r).toInt()
        }
        return Pair(cw.coerceAtLeast(1), ch.coerceAtLeast(1))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val availH = MeasureSpec.getSize(heightMeasureSpec)
        val boxLp = coverBox.layoutParams as LinearLayout.LayoutParams
        val minSide = context.dpi(110f)
        val innerW = (w - context.dpi(40f)).coerceAtLeast(minSide)

        // 1) kapak olmadan geri kalan her şeyin yüksekliğini ölç
        boxLp.width = innerW
        boxLp.height = 0
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val rest = measuredHeight

        // 2) kalan yüksekliğe ve kapağın oranına göre kapağı büyüt
        val cap = context.dpi(maxCoverDp)
        val maxH = if (hMode == MeasureSpec.UNSPECIFIED) cap else minOf(cap, availH - rest).coerceAtLeast(minSide)
        val (cw, ch) = coverSize(innerW, maxH)
        boxLp.width = cw
        boxLp.height = ch
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun placeholderArt() = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(0xFF4F6B3A.toInt(), 0xFF22301B.toInt())
    )

    private fun tiny(b: Bitmap): Bitmap? {
        return try {
            if (b.isRecycled || b.width <= 0 || b.height <= 0) return null
            val h1 = (48f * b.height / b.width).toInt().coerceAtLeast(1)
            val a = Bitmap.createScaledBitmap(b, 48, h1, true)
            val h2 = (10f * h1 / 48f).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(a, 10, h2, true)
        } catch (_: Exception) {
            null
        }
    }

    private fun applyPalette(b: Bitmap?) {
        baseColor = CoverPalette.base(b)
        accentColor = CoverPalette.accent(b)
        seek.accent = accentColor
        playBg.setColor(accentColor)
        updateToggles()
        cardTop.setLook(baseColor, backdrop)
        cardBottom.setLook(baseColor, backdrop)
        onPalette?.invoke(accentColor)
    }

    private fun resetCover() {
        lastArt = null
        backdrop = null
        ratio = 1f
        applyPalette(null)
        requestLayout()
    }

    fun render() {
        val i = MediaRepo.info
        if (i == null) {
            appChip.text = "Medya Paneli"
            title.text = "Çalan bir şey yok"
            artist.text = "Bir müzik uygulamasında şarkı başlat"
            artist.visibility = View.VISIBLE
            album.visibility = View.GONE
            badge.visibility = View.GONE
            art.setImageDrawable(placeholderArt())
            if (lastArt != null || backdrop != null) resetCover()
            btnPlay.setImageResource(R.drawable.ic_play)
            timeCur.text = "0:00"
            timeTotal.text = "--:--"
            seek.fraction = 0f
            return
        }

        appChip.text = i.appName
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
        if (bmp != null && bmp.width > 0 && bmp.height > 0) {
            art.setImageBitmap(bmp)
            if (bmp !== lastArt) {
                lastArt = bmp
                ratio = (bmp.width.toFloat() / bmp.height).coerceIn(0.6f, 1.9f)
                backdrop = tiny(bmp)
                applyPalette(bmp)
                requestLayout()
            }
        } else {
            art.setImageDrawable(placeholderArt())
            if (lastArt != null || backdrop != null) resetCover()
        }

        btnPlay.setImageResource(if (i.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        updateToggles()
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
        applyPalette(lastArt)
        post(ticker)
    }

    override fun onDetachedFromWindow() {
        MediaRepo.removeListener(listener)
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }
}
