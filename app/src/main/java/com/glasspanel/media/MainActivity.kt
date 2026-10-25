package com.glasspanel.media

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var notifStatus: TextView
    private lateinit var overlayStatus: TextView
    private lateinit var wallStatus: TextView
    private lateinit var panel: PanelView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpi(18f), dpi(28f), dpi(18f), dpi(32f))
        }

        root.addView(text("Medya Paneli", 28f, Color.WHITE, bold = true))
        root.addView(
            text("Çalan müziği kilit ekranında cam görünümlü bir panelde gösterir.", 14f, Color.argb(160, 255, 255, 255)),
            lp(top = 4f)
        )

        // Önizleme
        panel = PanelView(this)
        val previewWrap = FrameLayout(this)
        val w = minOf(resources.displayMetrics.widthPixels - dpi(36f), dpi(380f))
        previewWrap.addView(panel, FrameLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL))
        root.addView(previewWrap, lp(top = 20f))

        // İzinler
        root.addView(section("İZİNLER"), lp(top = 28f))
        val perms = card()
        notifStatus = text("", 13f, Color.argb(170, 255, 255, 255))
        perms.addView(text("Bildirim erişimi", 16f, Color.WHITE, bold = true))
        perms.addView(text("Medya oturumlarını (şarkı, kapak, süre) okumak için gerekli.", 13f, Color.argb(150, 255, 255, 255)), lp(top = 2f))
        perms.addView(notifStatus, lp(top = 6f))
        perms.addView(button("Bildirim erişimini aç") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }, lp(top = 8f))

        perms.addView(divider(), lp(top = 14f, bottom = 14f))

        overlayStatus = text("", 13f, Color.argb(170, 255, 255, 255))
        perms.addView(text("Diğer uygulamaların üstünde göster", 16f, Color.WHITE, bold = true))
        perms.addView(text("Android 10 ve üstünde panelin kilit ekranında kendiliğinden açılması için gerekli.", 13f, Color.argb(150, 255, 255, 255)), lp(top = 2f))
        perms.addView(overlayStatus, lp(top = 6f))
        perms.addView(button("İzni aç") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }, lp(top = 8f))

        perms.addView(divider(), lp(top = 14f, bottom = 14f))

        wallStatus = text("", 13f, Color.argb(170, 255, 255, 255))
        perms.addView(text("Duvar kâğıdına erişim", 16f, Color.WHITE, bold = true))
        perms.addView(text("Panelin camının arkasında senin duvar kâğıdını bulanık göstermek için gerekli.", 13f, Color.argb(150, 255, 255, 255)), lp(top = 2f))
        perms.addView(wallStatus, lp(top = 6f))
        perms.addView(button("İzni ver") {
            if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 33) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)
            }
        }, lp(top = 8f))

        perms.addView(divider(), lp(top = 14f, bottom = 14f))

        perms.addView(text("Pil kısıtlaması", 16f, Color.WHITE, bold = true))
        perms.addView(text("Huawei gibi telefonlar arka plandaki uygulamaları kapatabilir. Bu uygulamayı kısıtlamasız yap.", 13f, Color.argb(150, 255, 255, 255)), lp(top = 2f))
        perms.addView(button("Pil ayarlarını aç") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }, lp(top = 8f))
        root.addView(perms, lp(top = 8f))

        // Ayarlar
        root.addView(section("AYARLAR"), lp(top = 24f))
        val set = card()

        val swLock = Switch(this).apply {
            text = "Kilit ekranında paneli göster"
            setTextColor(Color.WHITE)
            textSize = 15f
            isChecked = Prefs.lockPanel(this@MainActivity)
            setOnCheckedChangeListener { _, v -> Prefs.setLockPanel(this@MainActivity, v) }
        }
        set.addView(swLock)

        val swPlaying = Switch(this).apply {
            text = "Sadece müzik çalarken göster"
            setTextColor(Color.WHITE)
            textSize = 15f
            isChecked = Prefs.onlyWhilePlaying(this@MainActivity)
            setOnCheckedChangeListener { _, v -> Prefs.setOnlyWhilePlaying(this@MainActivity, v) }
        }
        set.addView(swPlaying, lp(top = 16f))

        set.addView(text("Cam şeffaflığı", 15f, Color.WHITE), lp(top = 18f))
        val glassBar = SeekBar(this).apply {
            max = 100
            progress = Prefs.glass(this@MainActivity)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) {
                        Prefs.setGlass(this@MainActivity, p)
                        panel.render()
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        set.addView(glassBar, lp(top = 4f))
        root.addView(set, lp(top = 8f))

        root.addView(button("Paneli şimdi aç (deneme)") {
            startActivity(Intent(this, LockPanelActivity::class.java))
        }, lp(top = 22f))

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0E1015"))
            addView(root)
        }
        setContentView(scroll)

        val missing = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!hasWallpaperAccess()) {
            missing.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 1)
        }
    }

    private fun hasWallpaperAccess(): Boolean {
        if (Build.VERSION.SDK_INT < 23 || Build.VERSION.SDK_INT >= 33) return true
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateStatuses()
    }

    private fun setStatus(tv: TextView, on: Boolean) {
        tv.text = if (on) "Durum: açık ✓" else "Durum: kapalı"
        tv.setTextColor(if (on) Color.parseColor("#7BE0A0") else Color.parseColor("#FF8A80"))
    }

    private fun updateStatuses() {
        setStatus(notifStatus, isNotificationAccessOn())
        setStatus(overlayStatus, Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(this))
        setStatus(wallStatus, hasWallpaperAccess())
    }

    override fun onResume() {
        super.onResume()
        updateStatuses()
        val notifOn = isNotificationAccessOn()

        if (notifOn) {
            try {
                NotificationListenerService.requestRebind(ComponentName(this, MediaListenerService::class.java))
            } catch (_: Exception) {
            }
        }
        panel.render()
    }

    private fun isNotificationAccessOn(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { ComponentName.unflattenFromString(it)?.packageName == packageName }
    }

    // --- küçük arayüz yardımcıları ---

    private fun text(s: String, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = sp
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun section(s: String) = text(s, 12f, Color.argb(140, 255, 255, 255), bold = true).apply {
        letterSpacing = 0.1f
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dpi(16f), dpi(16f), dpi(16f), dpi(16f))
        background = GradientDrawable().apply {
            cornerRadius = dpf(20f)
            setColor(Color.parseColor("#171A21"))
        }
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(Color.argb(30, 255, 255, 255))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpi(1f))
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setAllCaps(false)
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            cornerRadius = dpf(14f)
            setColor(Color.parseColor("#2A3350"))
        }
        setOnClickListener { onClick() }
    }

    private fun lp(top: Float = 0f, bottom: Float = 0f) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dpi(top)
            bottomMargin = dpi(bottom)
        }
}
