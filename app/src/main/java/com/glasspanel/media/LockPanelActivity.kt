package com.glasspanel.media

import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
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
 * Üstte seçtiğin yazı tipiyle saat ve tarih, altta ekrana göre oranlanan iki çerçeveli panel.
 * Boş bir yere dokununca kapanır, kilit açılınca kendiliğinden kapanır.
 * Uygulama adı kutucuğuna dokununca önce kilit açtırır, sonra müzik uygulamasına gider.
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

        // --- üst bölüm: saat ve tarih ---
        val density = resources.displayMetrics.density
        val screenDp = resources.displayMetrics.heightPixels / density
        val baseTimeSp = (screenDp * 0.09f).coerceIn(56f, 112f)

        val clockBox = LinearLayout(this)
        clockBox.orientation = LinearLayout.VERTICAL
        clockBox.gravity = Gravity.CENTER

        val time = TextClock(this)
        time.setTextColor(Color.WHITE)
        time.includeFontPadding = false
        time.letterSpacing = -0.02f
        time.setShadowLayer(dpf(14f), 0f, dpf(2f), Color.argb(90, 0, 0, 0))
        clockBox.addView(
            time,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        val date = TextClock(this)
        date.format24Hour = "EEEE, d MMMM"
        date.format12Hour = "EEEE, d MMMM"
        date.textSize = 17f
        date.letterSpacing = 0.06f
        date.setShadowLayer(dpf(8f), 0f, dpf(1f), Color.argb(90, 0, 0, 0))
        clockBox.addView(
            date,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpi(4f)
            }
        )
        ClockStyle.apply(this, time, date, baseTimeSp, CoverPalette.DEFAULT_ACCENT)
        root.addView(clockBox, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.27f))

        // --- alt bölüm: panel, kalan yüksekliğe göre oranlanır ---
        val panelArea = FrameLayout(this)
        val panel = PanelView(this)
        panel.onPalette = { accent -> date.setTextColor(accent) }
        panel.onOpenApp = { openMusicApp() }
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

    /** Önce kilidi açtırır, sonra çalan müzik uygulamasını açar. */
    private fun openMusicApp() {
        if (MediaRepo.info == null) return
        val km = getSystemService(KeyguardManager::class.java)
        if (km != null && km.isKeyguardLocked) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    launchMusicApp()
                }
            })
        } else {
            launchMusicApp()
        }
    }

    private fun launchMusicApp() {
        val app = applicationContext
        val pkg = MediaRepo.info?.packageName
        val session = MediaRepo.controller?.sessionActivity
        var done = false
        if (session != null) {
            try {
                session.send()
                done = true
            } catch (_: Exception) {
            }
        }
        if (!done && pkg != null) {
            try {
                val launch = packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    app.startActivity(launch)
                }
            } catch (_: Exception) {
            }
        }
        finish()
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
