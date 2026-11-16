package com.glasspanel.media

import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock

/**
 * Kilit ekranının üstünde açılan tam medya paneli.
 * Üstte büyük, ince saat ve altında kapağın rengini alan tarih hapı; altta ekrana göre oranlanan kapak odaklı kart.
 * Boş bir yere dokununca kapanır, kilit açılınca kendiliğinden kapanır.
 */
class LockPanelActivity : Activity() {

    private val userPresent = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    private val repoListener: () -> Unit = {
        if (MediaRepo.info == null) finish()
    }

    private var registered = false

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }

        getSystemService(NotificationManager::class.java)?.cancel(MediaListenerService.NOTIF_ID)
        NowBar.hide()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.argb(95, 0, 0, 0))
        root.setPadding(dpi(16f), dpi(36f), dpi(16f), dpi(28f))
        root.setOnClickListener { finish() }
        root.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                dpi(16f),
                insets.systemWindowInsetTop + dpi(12f),
                dpi(16f),
                insets.systemWindowInsetBottom + dpi(20f)
            )
            insets
        }

        // --- üst bölüm: saat ve tarih hapı ---
        val density = resources.displayMetrics.density
        val screenDp = resources.displayMetrics.heightPixels / density
        val timeSp = (screenDp * 0.09f).coerceIn(56f, 112f)

        val clockBox = LinearLayout(this)
        clockBox.orientation = LinearLayout.VERTICAL
        clockBox.gravity = Gravity.CENTER

        val time = TextClock(this)
        time.format24Hour = "HH:mm"
        time.format12Hour = "HH:mm"
        time.setTextColor(Color.WHITE)
        time.textSize = timeSp
        time.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        time.includeFontPadding = false
        time.letterSpacing = -0.02f
        time.setShadowLayer(dpf(14f), 0f, dpf(2f), Color.argb(90, 0, 0, 0))
        clockBox.addView(
            time,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        val pillBg = GradientDrawable()
        pillBg.cornerRadius = dpf(22f)
        pillBg.setColor(withAlpha(CoverPalette.DEFAULT_ACCENT, 46))
        pillBg.setStroke(dpi(1f), withAlpha(CoverPalette.DEFAULT_ACCENT, 110))

        val date = TextClock(this)
        date.format24Hour = "EEEE, d MMMM"
        date.format12Hour = "EEEE, d MMMM"
        date.setTextColor(Color.WHITE)
        date.textSize = 15f
        date.letterSpacing = 0.04f
        date.setPadding(dpi(18f), dpi(7f), dpi(18f), dpi(7f))
        date.background = pillBg
        clockBox.addView(
            date,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpi(8f)
            }
        )
        root.addView(clockBox, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.27f))

        // --- alt bölüm: panel, kalan yüksekliğe göre oranlanır ---
        val panelArea = FrameLayout(this)
        val panel = PanelView(this)
        panel.onPalette = { accent ->
            pillBg.setColor(withAlpha(accent, 46))
            pillBg.setStroke(dpi(1f), withAlpha(accent, 110))
        }
        val width = minOf(resources.displayMetrics.widthPixels - dpi(32f), dpi(380f))
        panelArea.addView(
            panel,
            FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        root.addView(panelArea, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.73f))

        setContentView(root)

        registerReceiver(userPresent, IntentFilter(Intent.ACTION_USER_PRESENT))
        registered = true
        MediaRepo.addListener(repoListener)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getSystemService(NotificationManager::class.java)?.cancel(MediaListenerService.NOTIF_ID)
    }

    override fun onDestroy() {
        MediaRepo.removeListener(repoListener)
        if (registered) {
            try {
                unregisterReceiver(userPresent)
            } catch (_: Exception) {
            }
        }
        // Ekran hâlâ kilitliyse ve çubuk modundaysa çubuğu geri getir
        if (Prefs.mode(this) == Prefs.MODE_BAR) NowBar.showIfLocked(this)
        super.onDestroy()
    }
}
