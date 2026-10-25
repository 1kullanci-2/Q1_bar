package com.glasspanel.media

import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
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
 * Kilit ekranının üstünde açılan medya paneli.
 * Üstte saat ve tarih, altta ekrana göre oranlanan cam kartlar.
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

        val real = Point()
        windowManager.defaultDisplay.getRealSize(real)
        val screenW = real.x
        val screenH = real.y

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.argb(50, 0, 0, 0))
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

        // --- üst bölüm: saat ve tarih ---
        val density = resources.displayMetrics.density
        val screenDp = resources.displayMetrics.heightPixels / density
        val timeSp = (screenDp * 0.085f).coerceIn(52f, 104f)

        val clockBox = LinearLayout(this)
        clockBox.orientation = LinearLayout.VERTICAL
        clockBox.gravity = Gravity.CENTER

        val time = TextClock(this)
        time.format24Hour = "HH:mm"
        time.format12Hour = "HH:mm"
        time.setTextColor(Color.argb(240, 255, 255, 255))
        time.textSize = timeSp
        time.typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        time.includeFontPadding = false
        time.letterSpacing = 0.02f
        time.setShadowLayer(dpf(12f), 0f, dpf(2f), Color.argb(80, 0, 0, 0))
        clockBox.addView(
            time,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        val date = TextClock(this)
        date.format24Hour = "EEEE, d MMMM"
        date.format12Hour = "EEEE, d MMMM"
        date.setTextColor(Color.argb(215, 255, 255, 255))
        date.textSize = 18f
        date.letterSpacing = 0.05f
        date.setShadowLayer(dpf(8f), 0f, dpf(1f), Color.argb(80, 0, 0, 0))
        clockBox.addView(
            date,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpi(2f)
            }
        )
        root.addView(clockBox, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.27f))

        // --- alt bölüm: panel, kalan yüksekliğe göre oranlanır ---
        val panelArea = FrameLayout(this)
        val panel = PanelView(this)
        val width = minOf(resources.displayMetrics.widthPixels - dpi(32f), dpi(380f))
        panelArea.addView(
            panel,
            FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        root.addView(panelArea, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.73f))

        setContentView(root)

        // duvar kâğıdını arka planda yükle, hazır olunca cama uygula
        val appCtx = applicationContext
        Thread {
            val bmp = WallpaperBackdrop.load(appCtx, screenW, screenH)
            runOnUiThread {
                if (!isFinishing && !isDestroyed) panel.setScreenBackdrop(bmp, screenW, screenH)
            }
        }.start()

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
        super.onDestroy()
    }
}
