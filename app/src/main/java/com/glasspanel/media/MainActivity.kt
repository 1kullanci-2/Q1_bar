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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var notifStatus: TextView
    private lateinit var overlayStatus: TextView
    private lateinit var panel: PanelView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dpi(18f), dpi(28f), dpi(18f), dpi(32f))

        root.addView(text("Medya Paneli", 28f, Color.WHITE, bold = true))
        root.addView(
            text("Çalan müziği kilit ekranında ve ana ekranda şık bir panelle gösterir.", 14f, Color.argb(160, 255, 255, 255)),
            lp(top = 4f)
        )

        // Önizleme
        panel = PanelView(this)
        panel.maxCoverDp = 240f
        val previewWrap = FrameLayout(this)
        val w = minOf(resources.displayMetrics.widthPixels - dpi(36f), dpi(380f))
        previewWrap.addView(
            panel,
            FrameLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL)
        )
        root.addView(previewWrap, lp(top = 20f))

        // ---- İzinler ----
        root.addView(section("İZİNLER"), lp(top = 28f))
        val perms = card()

        notifStatus = text("", 13f, Color.argb(170, 255, 255, 255))
        perms.addView(text("Bildirim erişimi", 16f, Color.WHITE, bold = true))
        perms.addView(note("Medya oturumlarını (şarkı, kapak, süre) okumak için gerekli."), lp(top = 2f))
        perms.addView(notifStatus, lp(top = 6f))
        perms.addView(button("Bildirim erişimini aç") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }, lp(top = 8f))

        perms.addView(divider(), lp(top = 14f, bottom = 14f))

        overlayStatus = text("", 13f, Color.argb(170, 255, 255, 255))
        perms.addView(text("Diğer uygulamaların üstünde göster", 16f, Color.WHITE, bold = true))
        perms.addView(note("Alt çubuğun ve panelin kilit ekranında kendiliğinden açılması için gerekli."), lp(top = 2f))
        perms.addView(overlayStatus, lp(top = 6f))
        perms.addView(button("İzni aç") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }, lp(top = 8f))

        perms.addView(divider(), lp(top = 14f, bottom = 14f))

        perms.addView(text("Arka planda çalışsın (Huawei için önemli)", 16f, Color.WHITE, bold = true))
        perms.addView(
            note(
                "Uygulamayı açmadan da panelin gelmesi için telefonun bu uygulamayı kapatmaması gerekir. " +
                    "Önce pil kısıtlamasını kaldır. Sonra Huawei başlatma ayarlarında Medya Paneli için " +
                    "\"Otomatik yönet\"i kapatıp Otomatik başlat, İkincil başlat ve Arka planda çalış seçeneklerini aç."
            ),
            lp(top = 2f)
        )
        perms.addView(button("Pil ayarlarını aç") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }, lp(top = 8f))
        perms.addView(button("Huawei başlatma ayarlarını aç") { openStartupSettings() }, lp(top = 8f))
        root.addView(perms, lp(top = 8f))

        // ---- Kilit ekranı ----
        root.addView(section("KİLİT EKRANI"), lp(top = 24f))
        val lock = card()
        lock.addView(text("Müzik çalınca ne görünsün?", 16f, Color.WHITE, bold = true))

        val group = RadioGroup(this)
        group.orientation = RadioGroup.VERTICAL
        val rbBar = radio("Alt çubuk (dokununca panel açılır)")
        val rbFull = radio("Tam panel")
        val rbOff = radio("Hiçbir şey")
        group.addView(rbBar)
        group.addView(rbFull)
        group.addView(rbOff)
        group.check(
            when (Prefs.mode(this)) {
                Prefs.MODE_FULL -> rbFull.id
                Prefs.MODE_OFF -> rbOff.id
                else -> rbBar.id
            }
        )
        group.setOnCheckedChangeListener { _, id ->
            val mode = when (id) {
                rbFull.id -> Prefs.MODE_FULL
                rbOff.id -> Prefs.MODE_OFF
                else -> Prefs.MODE_BAR
            }
            Prefs.setMode(this, mode)
        }
        lock.addView(group, lp(top = 6f))

        val swPlaying = Switch(this)
        swPlaying.text = "Sadece müzik çalarken göster"
        swPlaying.setTextColor(Color.WHITE)
        swPlaying.textSize = 15f
        swPlaying.isChecked = Prefs.onlyWhilePlaying(this)
        swPlaying.setOnCheckedChangeListener { _, v -> Prefs.setOnlyWhilePlaying(this, v) }
        lock.addView(swPlaying, lp(top = 14f))

        lock.addView(button("Alt çubuğu dene (5 sn)") {
            if (!NowBar.canShow(this)) {
                Toast.makeText(this, "Önce \"Diğer uygulamaların üstünde göster\" iznini ver", Toast.LENGTH_LONG).show()
            } else {
                NowBar.show(this, demo = true)
                panel.postDelayed({ NowBar.hide() }, 5000)
            }
        }, lp(top = 16f))
        lock.addView(button("Tam paneli şimdi aç (deneme)") {
            startActivity(Intent(this, LockPanelActivity::class.java))
        }, lp(top = 8f))
        root.addView(lock, lp(top = 8f))

        // ---- Widget ----
        root.addView(section("WIDGET"), lp(top = 24f))
        val widget = card()
        widget.addView(text("Ana ekran widget'ı", 16f, Color.WHITE, bold = true))
        widget.addView(
            note(
                "Ana ekranda boş bir yere uzun bas, Widget'lar'a gir ve Medya Paneli'ni seç. " +
                    "Kapak, şarkı, sanatçı, çalan uygulamanın simgesi ve önceki / oynat-durdur / sonraki düğmeleri gelir."
            ),
            lp(top = 2f)
        )
        root.addView(widget, lp(top = 8f))

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor("#0E1015"))
        scroll.addView(root)
        setContentView(scroll)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun setStatus(tv: TextView, on: Boolean) {
        tv.text = if (on) "Durum: açık ✓" else "Durum: kapalı"
        tv.setTextColor(if (on) Color.parseColor("#7BE0A0") else Color.parseColor("#FF8A80"))
    }

    override fun onResume() {
        super.onResume()
        val notifOn = isNotificationAccessOn()
        setStatus(notifStatus, notifOn)
        setStatus(overlayStatus, Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(this))

        if (notifOn) {
            // dinleyiciyi ve arka plan servisini canlandır
            KeepAliveService.start(this)
            KeepAliveService.rebind(this)
        }
        panel.render()
    }

    private fun isNotificationAccessOn(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { ComponentName.unflattenFromString(it)?.packageName == packageName }
    }

    /** Huawei'nin "Uygulama başlatma" ekranını açmayı dener, olmazsa uygulama ayrıntılarına gider. */
    private fun openStartupSettings() {
        val targets = listOf(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            ),
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        )
        for (c in targets) {
            try {
                startActivity(Intent().setComponent(c))
                return
            } catch (_: Exception) {
            }
        }
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            )
        } catch (_: Exception) {
        }
    }

    // --- küçük arayüz yardımcıları ---

    private fun text(s: String, sp: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = sp
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun note(s: String) = text(s, 13f, Color.argb(150, 255, 255, 255))

    private fun section(s: String): TextView {
        val t = text(s, 12f, Color.argb(140, 255, 255, 255), bold = true)
        t.letterSpacing = 0.1f
        return t
    }

    private fun radio(label: String): RadioButton {
        val r = RadioButton(this)
        r.id = View.generateViewId()
        r.text = label
        r.setTextColor(Color.WHITE)
        r.textSize = 15f
        return r
    }

    private fun card(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dpi(16f), dpi(16f), dpi(16f), dpi(16f))
        val bg = GradientDrawable()
        bg.cornerRadius = dpf(20f)
        bg.setColor(Color.parseColor("#171A21"))
        c.background = bg
        return c
    }

    private fun divider(): View {
        val v = View(this)
        v.setBackgroundColor(Color.argb(30, 255, 255, 255))
        v.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpi(1f))
        return v
    }

    private fun button(label: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.setAllCaps(false)
        b.setTextColor(Color.WHITE)
        val bg = GradientDrawable()
        bg.cornerRadius = dpf(14f)
        bg.setColor(Color.parseColor("#2A3350"))
        b.background = bg
        b.setOnClickListener { onClick() }
        return b
    }

    private fun lp(top: Float = 0f, bottom: Float = 0f): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        p.topMargin = dpi(top)
        p.bottomMargin = dpi(bottom)
        return p
    }
}
