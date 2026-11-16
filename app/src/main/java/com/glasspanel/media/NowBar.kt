package com.glasspanel.media

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Ekranın altında beliren küçük, yuvarlak köşeli medya çubuğu (Samsung Now Bar benzeri).
 * Doğrudan bir pencere olarak eklendiği için müzik başlar başlamaz gecikmesiz görünür.
 * Dokununca tam panel açılır.
 */
object NowBar {
    private var view: BarView? = null
    private var manager: WindowManager? = null

    fun canShow(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(ctx)

    @Suppress("DEPRECATION")
    fun show(ctx: Context, demo: Boolean = false) {
        val existing = view
        if (existing != null) {
            existing.render()
            return
        }
        val app = ctx.applicationContext
        if (!canShow(app)) return
        if (!demo && MediaRepo.info == null) return

        val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val v = BarView(app, demo)
        val width = minOf(app.resources.displayMetrics.widthPixels - app.dpi(32f), app.dpi(420f))
        val lp = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        lp.y = app.dpi(40f)
        try {
            wm.addView(v, lp)
            manager = wm
            view = v
        } catch (_: Exception) {
        }
    }

    fun hide() {
        val v = view ?: return
        view = null
        try {
            manager?.removeView(v)
        } catch (_: Exception) {
        }
    }

    /** Ekran hâlâ kilitliyse ve çalan bir şey varsa çubuğu göster. */
    fun showIfLocked(ctx: Context) {
        val km = ctx.getSystemService(KeyguardManager::class.java)
        if (km == null || !km.isKeyguardLocked) return
        if (MediaRepo.info == null) return
        show(ctx)
    }
}

internal class BarView(ctx: Context, private val demo: Boolean) : FrameLayout(ctx) {

    private val bg = GradientDrawable().apply {
        cornerRadius = ctx.dpf(32f)
        setColor(CoverPalette.DEFAULT_BASE)
    }
    private val cover = ImageView(ctx)
    private val title = TextView(ctx)
    private val artist = TextView(ctx)
    private val play = ImageButton(ctx)
    private val playBg = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(CoverPalette.DEFAULT_ACCENT)
    }
    private val listener: () -> Unit = { render() }

    init {
        background = bg
        elevation = ctx.dpf(10f)

        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(ctx.dpi(10f), ctx.dpi(10f), ctx.dpi(12f), ctx.dpi(10f))

        cover.scaleType = ImageView.ScaleType.CENTER_CROP
        cover.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, ctx.dpf(18f))
            }
        }
        cover.clipToOutline = true
        row.addView(cover, LinearLayout.LayoutParams(ctx.dpi(52f), ctx.dpi(52f)))

        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        title.setTextColor(Color.WHITE)
        title.textSize = 14f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setSingleLine(true)
        title.ellipsize = TextUtils.TruncateAt.END
        artist.setTextColor(Color.argb(205, 255, 255, 255))
        artist.textSize = 12f
        artist.setSingleLine(true)
        artist.ellipsize = TextUtils.TruncateAt.END
        col.addView(
            title,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        col.addView(
            artist,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        row.addView(
            col,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ctx.dpi(12f)
                rightMargin = ctx.dpi(8f)
            }
        )

        play.setImageResource(R.drawable.ic_play)
        play.scaleType = ImageView.ScaleType.FIT_CENTER
        play.background = playBg
        val p = ctx.dpi(11f)
        play.setPadding(p, p, p, p)
        play.setColorFilter(0xFF10131C.toInt())
        play.setOnClickListener {
            val tc = MediaRepo.controller?.transportControls
            if (MediaRepo.info?.isPlaying == true) tc?.pause() else tc?.play()
        }
        row.addView(play, LinearLayout.LayoutParams(ctx.dpi(46f), ctx.dpi(46f)))

        addView(
            row,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        setOnClickListener { openPanel() }
    }

    private fun placeholder() = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(0xFF4A5675.toInt(), 0xFF262B3D.toInt())
    )

    private fun openPanel() {
        NowBar.hide()
        try {
            context.startActivity(
                Intent(context, LockPanelActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            )
        } catch (_: Exception) {
        }
    }

    fun render() {
        val i = MediaRepo.info
        if (i == null) {
            if (!demo) {
                NowBar.hide()
                return
            }
            title.text = "Çalan bir şey yok"
            artist.text = "Alt çubuk önizlemesi"
            cover.setImageDrawable(placeholder())
            play.setImageResource(R.drawable.ic_play)
            return
        }
        title.text = i.title.ifBlank { "Bilinmeyen parça" }
        artist.text = i.artist.ifBlank { i.appName }
        val art = i.art
        if (art != null) cover.setImageBitmap(art) else cover.setImageDrawable(placeholder())

        val base = CoverPalette.base(art)
        val accent = CoverPalette.accent(art)
        bg.setColor(withAlpha(base, 245))
        bg.setStroke(context.dpi(1f), withAlpha(accent, 80))
        playBg.setColor(accent)
        play.setImageResource(if (i.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        MediaRepo.addListener(listener)
        render()
    }

    override fun onDetachedFromWindow() {
        MediaRepo.removeListener(listener)
        super.onDetachedFromWindow()
    }
}
