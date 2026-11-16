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
import android.os.PowerManager
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
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextClock
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var banner: TextView
    private lateinit var notifStatus: TextView
    private lateinit var overlayStatus: TextView
    private lateinit var batteryStatus: TextView
    private lateinit var panel: PanelView
    private lateinit var previewTime: TextClock
    private lateinit var previewDate: TextClock
    private lateinit var sizeLabel: TextView
    private var previewAccent = CoverPalette.DEFAULT_ACCENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dpi(18f), dpi(28f), dpi(18f), dpi(36f))

        root.addView(text("Medya Paneli", 30f, Color.WHITE, bold = true))
        root.addView(
            text("Çalan müziği kilit ekranında ve ana ekranda şık bir panelle gösterir.", 14f, Color.argb(160, 255, 255, 255)),
            lp(top = 4f)
        )

        // Durum şeridi
        banner = text("", 14f, Color.WHITE, bold = true)
        banner.setPadding(dpi(16f), dpi(14f), dpi(16f), dpi(14f))
        root.addView(banner, lp(top = 18f))

        // Önizleme
        panel = PanelView(this)
        panel.maxCoverDp = 230f
        panel.onPalette = { a ->
            previewAccent = a
            applyClockPreview()
        }
        val previewWrap = FrameLayout(this)
        val w = minOf(resources.displayMetrics.widthPixels - dpi(36f), dpi(380f))
        previewWrap.addView(
            panel,
            FrameLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL)
        )
        root.addView(section("ÖNİZLEME"), lp(top = 24f))
        root.addView(previewWrap, lp(top = 10f))

        // ---- Kurulum ----
        root.addView(section("KURULUM"), lp(top = 26f))
        val setup = card()
        notifStatus = pill()
        overlayStatus = pill()
        batteryStatus = pill()
        setup.addView(
            row("Bildirim erişimi", "Şarkı, kapak ve süreyi okumak için gerekli", notifStatus) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        )
        setup.addView(divider())
        setup.addView(
            row("Diğer uygulamaların üstünde göster", "Panelin kilit ekranında kendiliğinden açılması için", overlayStatus) {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
            }
        )
        setup.addView(divider())
        setup.addView(
            row("Pil kısıtlaması yok", "Telefonun uygulamayı arka planda kapatmaması için", batteryStatus) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        )
        setup.addView(divider())
        setup.addView(
            row(
                "Huawei başlatma ayarları",
                "Otomatik yönet kapalı; Otomatik başlat, İkincil başlat ve Arka planda çalış açık olsun",
                null
            ) { openStartupSettings() }
        )
        root.addView(setup, lp(top = 8f))

        // ---- Kilit ekranı ----
        root.addView(section("KİLİT EKRANI"), lp(top = 24f))
        val lock = card()

        val swLock = Switch(this)
        swLock.text = "Müzik çalınca paneli otomatik aç"
        swLock.setTextColor(Color.WHITE)
        swLock.textSize = 15f
        swLock.isChecked = Prefs.lockPanel(this)
        swLock.setOnCheckedChangeListener { _, v -> Prefs.setLockPanel(this, v) }
        lock.addView(swLock)

        val swPlaying = Switch(this)
        swPlaying.text = "Sadece müzik çalarken göster"
        swPlaying.setTextColor(Color.WHITE)
        swPlaying.textSize = 15f
        swPlaying.isChecked = Prefs.onlyWhilePlaying(this)
        swPlaying.setOnCheckedChangeListener { _, v -> Prefs.setOnlyWhilePlaying(this, v) }
        lock.addView(swPlaying, lp(top = 14f))

        lock.addView(button("Paneli şimdi aç (deneme)") {
            startActivity(Intent(this, LockPanelActivity::class.java))
        }, lp(top = 16f))
        root.addView(lock, lp(top = 8f))

        // ---- Saat ----
        root.addView(section("SAAT"), lp(top = 24f))
        val clock = card()

        previewTime = TextClock(this)
        previewTime.setTextColor(Color.WHITE)
        previewTime.includeFontPadding = false
        previewTime.letterSpacing = -0.02f
        previewDate = TextClock(this)
        previewDate.format24Hour = "EEEE, d MMMM"
        previewDate.format12Hour = "EEEE, d MMMM"
        previewDate.textSize = 16f
        previewDate.letterSpacing = 0.06f
        val clockBox = LinearLayout(this)
        clockBox.orientation = LinearLayout.VERTICAL
        clockBox.gravity = Gravity.CENTER
        clockBox.addView(
            previewTime,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        clockBox.addView(
            previewDate,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        val clockBg = GradientDrawable()
        clockBg.cornerRadius = dpf(16f)
        clockBg.setColor(Color.parseColor("#0E1015"))
        clockBox.background = clockBg
        clockBox.setPadding(dpi(12f), dpi(18f), dpi(12f), dpi(18f))
        clock.addView(clockBox, lp())

        clock.addView(text("Yazı tipi", 15f, Color.WHITE, bold = true), lp(top = 16f))
        val fontGroup = RadioGroup(this)
        fontGroup.orientation = RadioGroup.VERTICAL
        val radios = ArrayList<RadioButton>()
        for (idx in ClockStyle.FONT_NAMES.indices) {
            val r = RadioButton(this)
            r.id = View.generateViewId()
            r.text = ClockStyle.FONT_NAMES[idx] + "   ·   12:45"
            r.setTextColor(Color.WHITE)
            r.textSize = 17f
            r.typeface = ClockStyle.dateFont(this, idx)
            fontGroup.addView(r)
            radios.add(r)
        }
        fontGroup.check(radios[Prefs.clockFont(this).coerceIn(0, radios.size - 1)].id)
        fontGroup.setOnCheckedChangeListener { _, id ->
            val idx = radios.indexOfFirst { it.id == id }
            if (idx >= 0) {
                Prefs.setClockFont(this, idx)
                applyClockPreview()
            }
        }
        clock.addView(fontGroup, lp(top = 4f))

        sizeLabel = text("", 15f, Color.WHITE, bold = true)
        clock.addView(sizeLabel, lp(top = 14f))
        val sizeBar = SeekBar(this)
        sizeBar.max = 60
        sizeBar.progress = Prefs.clockSize(this).coerceIn(70, 130) - 70
        sizeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) {
                    Prefs.setClockSize(this@MainActivity, p + 70)
                    applyClockPreview()
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        clock.addView(sizeBar, lp(top = 2f))

        val swDate = Switch(this)
        swDate.text = "Tarihi göster"
        swDate.setTextColor(Color.WHITE)
        swDate.textSize = 15f
        swDate.isChecked = Prefs.showDate(this)
        swDate.setOnCheckedChangeListener { _, v ->
            Prefs.setShowDate(this, v)
            applyClockPreview()
        }
        clock.addView(swDate, lp(top = 14f))

        val sw24 = Switch(this)
        sw24.text = "24 saatlik gösterim"
        sw24.setTextColor(Color.WHITE)
        sw24.textSize = 15f
        sw24.isChecked = Prefs.clock24(this)
        sw24.setOnCheckedChangeListener { _, v ->
            Prefs.setClock24(this, v)
            applyClockPreview()
        }
        clock.addView(sw24, lp(top = 14f))
        root.addView(clock, lp(top = 8f))

        // ---- Widget ----
        root.addView(section("WİDGET"), lp(top = 24f))
        val widget = card()
        widget.addView(text("Ana ekran widget'ı", 16f, Color.WHITE, bold = true))
        widget.addView(
            note(
                "Ana ekranda boş bir yere uzun bas, Widget'lar'dan Medya Paneli'ni seç. " +
                    "Kapaktan renk alan ince bir çubuk gelir: kapak, şarkı, sanatçı, süre çubuğu, " +
                    "karıştır / tekrar ve önceki / oynat-durdur / sonraki. Dokununca çalan müzik uygulaması açılır. " +
                    "Karıştır ve tekrar, müzik uygulaması bu düğmeleri paylaşıyorsa çalışır; paylaşmıyorsa soluk görünür."
            ),
            lp(top = 4f)
        )
        root.addView(widget, lp(top = 8f))

        root.addView(
            note("Yazı tipleri: Outfit, Fraunces, Space Grotesk (SIL Open Font License)"),
            lp(top = 20f)
        )

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor("#0E1015"))
        scroll.addView(root)
        setContentView(scroll)

        applyClockPreview()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun applyClockPreview() {
        if (!::previewTime.isInitialized || !::previewDate.isInitialized || !::sizeLabel.isInitialized) return
        ClockStyle.apply(this, previewTime, previewDate, 52f, previewAccent)
        sizeLabel.text = "Saat boyutu: %" + Prefs.clockSize(this)
    }

    override fun onResume() {
        super.onResume()
        val notifOn = isNotificationAccessOn()
        val overlayOn = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(this)
        val pm = getSystemService(PowerManager::class.java)
        val batteryOk = pm?.isIgnoringBatteryOptimizations(packageName) ?: false

        setPill(notifStatus, notifOn)
        setPill(overlayStatus, overlayOn)
        setPill(batteryStatus, batteryOk, "Tamam", "Kısıtlı")

        val missing = ArrayList<String>()
        if (!notifOn) missing.add("bildirim erişimi")
        if (!overlayOn) missing.add("üstünde göster izni")
        val ok = missing.isEmpty()
        banner.text = if (ok) {
            "Hazır ✓  Müzik çalınca panel kilit ekranında açılır."
        } else {
            "Eksik: " + missing.joinToString(", ")
        }
        val tint = if (ok) Color.parseColor("#7BE0A0") else Color.parseColor("#FFB74D")
        banner.setTextColor(tint)
        val bg = GradientDrawable()
        bg.cornerRadius = dpf(16f)
        bg.setColor(withAlpha(tint, 34))
        bg.setStroke(dpi(1f), withAlpha(tint, 90))
        banner.background = bg

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

    private fun pill(): TextView {
        val t = TextView(this)
        t.textSize = 12f
        t.typeface = Typeface.DEFAULT_BOLD
        t.setPadding(dpi(12f), dpi(5f), dpi(12f), dpi(5f))
        return t
    }

    private fun setPill(t: TextView, on: Boolean, onText: String = "Açık", offText: String = "Kapalı") {
        t.text = if (on) onText else offText
        val c = if (on) Color.parseColor("#7BE0A0") else Color.parseColor("#FF8A80")
        t.setTextColor(c)
        val bg = GradientDrawable()
        bg.cornerRadius = dpf(14f)
        bg.setColor(withAlpha(c, 38))
        t.background = bg
    }

    private fun row(title: String, desc: String, status: TextView?, onClick: () -> Unit): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(0, dpi(12f), 0, dpi(12f))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(text(title, 15f, Color.WHITE, bold = true))
        col.addView(note(desc), lp(top = 2f))
        r.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (status != null) {
            val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            p.leftMargin = dpi(12f)
            r.addView(status, p)
        } else {
            val arrow = text("›", 22f, Color.argb(150, 255, 255, 255))
            val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            p.leftMargin = dpi(12f)
            r.addView(arrow, p)
        }
        r.setOnClickListener { onClick() }
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
