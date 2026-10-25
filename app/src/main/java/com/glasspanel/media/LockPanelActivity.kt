package com.glasspanel.media

import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ScrollView

/** Kilit ekranının üstünde açılan medya paneli. Boş bir yere dokununca kapanır, kilit açılınca kendiliğinden kapanır. */
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        getSystemService(NotificationManager::class.java)?.cancel(MediaListenerService.NOTIF_ID)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = ScrollView.OVER_SCROLL_NEVER
            setBackgroundColor(Color.argb(110, 0, 0, 0))
        }

        val container = FrameLayout(this).apply {
            setPadding(0, dpi(40f), 0, dpi(40f))
            setOnClickListener { finish() }
        }

        val width = minOf(resources.displayMetrics.widthPixels - dpi(32f), dpi(380f))
        container.addView(
            PanelView(this),
            FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        scroll.addView(
            container,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        setContentView(scroll)

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
