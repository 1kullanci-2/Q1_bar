package com.glasspanel.media

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.slider.Slider
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var banner: TextView
    private lateinit var notifStatus: TextView
    private lateinit var overlayStatus: TextView
    private lateinit var batteryStatus: TextView
    private lateinit var panel: PanelView
    private lateinit var previewTime: TextClock
    private lateinit var previewDate: TextClock
    private lateinit var sizeLabel: TextView
    private var previewAccent = CoverPalette.DEFAULT_ACCENT

    // güncelleme kartı
    private lateinit var updTitle: TextView
    private lateinit var updInfo: TextView
    private lateinit var updProgress: LinearProgressIndicator
    private lateinit var updBtn: MaterialButton
    private var pendingRelease: Updater.Release? = null
    private var pendingApk: File? = null
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InstallCounter.register(this)
        if (!Prefs.setupDone(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dpi(18f), dpi(28f), dpi(18f), dpi(36f))

        root.addView(Ui.text(this, "Medya Paneli", 30f, bold = true))
        root.addView(
            Ui.soft(this, "Çalan müziği kilit ekranında ve ana ekranda şık bir panelle gösterir.", 14f),
            Ui.lp(this, 4f)
        )

        // Durum şeridi
        banner = Ui.text(this, "", 14f, bold = true)
        banner.setPadding(dpi(16f), dpi(14f), dpi(16f), dpi(14f))
        root.addView(banner, Ui.lp(this, 18f))

        // ---- Güncelleme ----
        val upd = Ui.card(this, root, 12f)
        updTitle = Ui.text(this, "Güncelleme", 16f, bold = true)
        updInfo = Ui.soft(this, "Sürüm ${BuildConfig.VERSION_NAME}")
        updProgress = LinearProgressIndicator(this)
        updProgress.max = 100
        updProgress.trackThickness = dpi(6f)
        updProgress.trackCornerRadius = dpi(3f)
        updProgress.setIndicatorColor(getColor(R.color.green_ok))
        updProgress.trackColor = getColor(R.color.surface_var)
        updProgress.visibility = View.GONE
        updBtn = Ui.filled(this, "Güncellemeyi denetle") { onUpdateButton() }
        upd.addView(updTitle)
        upd.addView(updInfo, Ui.lp(this, 2f))
        upd.addView(updProgress, Ui.lp(this, 12f))
        upd.addView(updBtn, Ui.lp(this, 14f))

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
        root.addView(Ui.section(this, "ÖNİZLEME"), Ui.lp(this, 24f))
        root.addView(previewWrap, Ui.lp(this, 10f))

        // ---- Kurulum ----
        root.addView(Ui.section(this, "KURULUM"), Ui.lp(this, 26f))
        val setup = Ui.card(this, root, 8f)
        notifStatus = TextView(this)
        overlayStatus = TextView(this)
        batteryStatus = TextView(this)
        setup.addView(
            Ui.row(this, "Bildirim erişimi", "Şarkı, kapak ve süreyi okumak için gerekli", notifStatus) {
                Perms.openNotificationAccess(this)
            }
        )
        setup.addView(Ui.divider(this))
        setup.addView(
            Ui.row(this, "Diğer uygulamaların üstünde göster", "Panelin kilit ekranında kendiliğinden açılması için", overlayStatus) {
                Perms.openOverlay(this)
            }
        )
        setup.addView(Ui.divider(this))
        setup.addView(
            Ui.row(this, "Pil kısıtlaması yok", "Telefonun uygulamayı arka planda kapatmaması için", batteryStatus) {
                Perms.openBattery(this)
            }
        )
        setup.addView(Ui.divider(this))
        setup.addView(
            Ui.row(
                this,
                "Huawei başlatma ayarları",
                "Otomatik yönet kapalı; Otomatik başlat, İkincil başlat ve Arka planda çalış açık olsun",
                null
            ) { Perms.openStartupSettings(this) }
        )
        setup.addView(Ui.divider(this))
        setup.addView(
            Ui.row(this, "Kurulum sihirbazı", "İzinleri adım adım yeniden gözden geçir", null) {
                startActivity(Intent(this, SetupActivity::class.java))
            }
        )

        // ---- Kilit ekranı ----
        root.addView(Ui.section(this, "KİLİT EKRANI"), Ui.lp(this, 24f))
        val lock = Ui.card(this, root, 8f)
        lock.addView(switchRow("Müzik çalınca paneli otomatik aç", Prefs.lockPanel(this)) { Prefs.setLockPanel(this, it) })
        lock.addView(switchRow("Sadece müzik çalarken göster", Prefs.onlyWhilePlaying(this)) { Prefs.setOnlyWhilePlaying(this, it) })
        lock.addView(Ui.outlined(this, "Paneli şimdi aç (deneme)") {
            startActivity(Intent(this, LockPanelActivity::class.java))
        }, Ui.lp(this, 12f))

        // ---- Saat ----
        root.addView(Ui.section(this, "SAAT"), Ui.lp(this, 24f))
        val clock = Ui.card(this, root, 8f)

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
        clockBg.setColor(Color.parseColor("#1E2535"))
        clockBox.background = clockBg
        clockBox.setPadding(dpi(12f), dpi(18f), dpi(12f), dpi(18f))
        clock.addView(clockBox, Ui.lp(this))

        clock.addView(Ui.text(this, "Yazı tipi", 15f, bold = true), Ui.lp(this, 16f))
        val fontGroup = RadioGroup(this)
        fontGroup.orientation = RadioGroup.VERTICAL
        val radios = ArrayList<RadioButton>()
        for (idx in ClockStyle.FONT_NAMES.indices) {
            val r = MaterialRadioButton(this)
            r.id = View.generateViewId()
            r.text = ClockStyle.FONT_NAMES[idx] + "   ·   12:45"
            r.setTextColor(getColor(R.color.ink))
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
        clock.addView(fontGroup, Ui.lp(this, 4f))

        sizeLabel = Ui.text(this, "", 15f, bold = true)
        clock.addView(sizeLabel, Ui.lp(this, 14f))
        val slider = Slider(this)
        slider.valueFrom = 70f
        slider.valueTo = 130f
        slider.stepSize = 1f
        slider.value = Prefs.clockSize(this).coerceIn(70, 130).toFloat()
        slider.addOnChangeListener { _, v, fromUser ->
            if (fromUser) {
                Prefs.setClockSize(this, v.toInt())
                applyClockPreview()
            }
        }
        clock.addView(slider, Ui.lp(this, 2f))

        clock.addView(switchRow("Tarihi göster", Prefs.showDate(this)) {
            Prefs.setShowDate(this, it)
            applyClockPreview()
        })
        clock.addView(switchRow("24 saatlik gösterim", Prefs.clock24(this)) {
            Prefs.setClock24(this, it)
            applyClockPreview()
        })

        // ---- Widget ----
        root.addView(Ui.section(this, "WİDGET"), Ui.lp(this, 24f))
        val widget = Ui.card(this, root, 8f)
        widget.addView(Ui.text(this, "Ana ekran widget'ı", 16f, bold = true))
        widget.addView(
            Ui.soft(
                this,
                "Ana ekranda boş bir yere uzun bas, Widget'lar'dan Medya Paneli'ni seç. " +
                    "Kapaktan renk alan ince bir çubuk gelir: kapak, şarkı, sanatçı, süre çubuğu, " +
                    "karıştır / tekrar ve önceki / oynat-durdur / sonraki. Dokununca çalan müzik uygulaması açılır. " +
                    "Karıştır ve tekrar, müzik uygulaması bu düğmeleri paylaşıyorsa çalışır; paylaşmıyorsa soluk görünür."
            ),
            Ui.lp(this, 4f)
        )

        // ---- Hakkında ----
        root.addView(Ui.section(this, "UYGULAMA"), Ui.lp(this, 24f))
        val about = Ui.card(this, root, 8f)
        about.addView(Ui.row(this, "Hakkında", "Sürüm ${BuildConfig.VERSION_NAME} · yapımcı ve proje", null) {
            startActivity(Intent(this, AboutActivity::class.java))
        })

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(getColor(R.color.bg))
        scroll.addView(root)
        setContentView(scroll)

        applyClockPreview()

        // açılışta (en fazla 6 saatte bir) sessizce güncelleme bak
        if (System.currentTimeMillis() - Prefs.lastUpdateCheck(this) > 6 * 3600 * 1000L) {
            checkUpdate(manual = false)
        }
    }

    private fun switchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit): MaterialSwitch {
        val s = MaterialSwitch(this)
        s.text = label
        s.setTextColor(getColor(R.color.ink))
        s.textSize = 15f
        s.isChecked = checked
        s.setOnCheckedChangeListener { _, v -> onChange(v) }
        s.layoutParams = Ui.lp(this, 6f)
        return s
    }

    private fun applyClockPreview() {
        if (!::previewTime.isInitialized || !::previewDate.isInitialized || !::sizeLabel.isInitialized) return
        ClockStyle.apply(this, previewTime, previewDate, 52f, previewAccent)
        sizeLabel.text = "Saat boyutu: %" + Prefs.clockSize(this)
    }

    // ---- güncelleme ----

    private fun onUpdateButton() {
        if (busy) return
        val r = pendingRelease
        if (r == null) checkUpdate(manual = true) else downloadAndInstall(r)
    }

    private fun checkUpdate(manual: Boolean) {
        if (busy) return
        busy = true
        if (manual) {
            updInfo.text = "Denetleniyor…"
            updBtn.isEnabled = false
        }
        Net.async({ Updater.check() }) { res ->
            busy = false
            updBtn.isEnabled = true
            Prefs.setLastUpdateCheck(this, System.currentTimeMillis())
            res.onSuccess { r ->
                pendingRelease = r
                if (r != null) {
                    updTitle.text = "Yeni sürüm var"
                    updInfo.text = "${r.name} hazır (${"%.1f".format(r.size / 1048576.0)} MB). Ayarların ve izinlerin korunur."
                    updBtn.text = "İndir ve kur"
                } else {
                    updTitle.text = "Güncelleme"
                    updInfo.text = "Sürüm ${BuildConfig.VERSION_NAME}" + if (manual) " · en güncel sürümdesin" else ""
                    updBtn.text = "Güncellemeyi denetle"
                }
            }
            res.onFailure {
                if (manual) updInfo.text = "Denetlenemedi. İnternet bağlantını kontrol et (depo herkese açık olmalı)."
                else updInfo.text = "Sürüm ${BuildConfig.VERSION_NAME}"
            }
        }
    }

    private fun downloadAndInstall(r: Updater.Release) {
        busy = true
        updBtn.isEnabled = false
        updProgress.visibility = View.VISIBLE
        updProgress.setProgressCompat(0, false)
        updInfo.text = "İndiriliyor…"
        Net.async({
            Updater.download(applicationContext, r) { p -> runOnUiThread { updProgress.setProgressCompat(p, true) } }
        }) { res ->
            busy = false
            updBtn.isEnabled = true
            updProgress.visibility = View.GONE
            res.onSuccess { apk ->
                updInfo.text = "İndirildi. Kurulum ekranı açılıyor…"
                tryInstall(apk)
            }
            res.onFailure { updInfo.text = "İndirilemedi: ${it.message ?: "bilinmeyen hata"}" }
        }
    }

    private fun tryInstall(apk: File) {
        if (Updater.canInstall(this)) {
            pendingApk = null
            Updater.install(this, apk)
        } else {
            pendingApk = apk
            Toast.makeText(this, "Önce bu uygulamaya kurulum izni ver, sonra geri dön", Toast.LENGTH_LONG).show()
            Updater.openInstallPermission(this)
        }
    }

    override fun onResume() {
        super.onResume()
        val notifOn = Perms.notificationAccess(this)
        val overlayOn = Perms.overlay(this)
        val batteryOk = Perms.battery(this)

        Ui.setPill(notifStatus, notifOn)
        Ui.setPill(overlayStatus, overlayOn)
        Ui.setPill(batteryStatus, batteryOk, "Tamam", "Kısıtlı")

        val missing = ArrayList<String>()
        if (!notifOn) missing.add("bildirim erişimi")
        if (!overlayOn) missing.add("üstünde göster izni")
        val ok = missing.isEmpty()
        banner.text = if (ok) {
            "Hazır ✓  Müzik çalınca panel kilit ekranında açılır."
        } else {
            "Eksik: " + missing.joinToString(", ")
        }
        val tint = getColor(if (ok) R.color.green_ok else R.color.amber)
        banner.setTextColor(tint)
        val bg = GradientDrawable()
        bg.cornerRadius = dpf(18f)
        bg.setColor(com.glasspanel.media.withAlpha(tint, 30))
        bg.setStroke(dpi(1f), com.glasspanel.media.withAlpha(tint, 90))
        banner.background = bg

        if (notifOn) {
            // dinleyiciyi ve arka plan servisini canlandır
            KeepAliveService.start(this)
            KeepAliveService.rebind(this)
        }
        panel.render()

        // kurulum izni verilip dönüldüyse indirilmiş APK'yı kur
        val apk = pendingApk
        if (apk != null && Updater.canInstall(this)) {
            pendingApk = null
            Updater.install(this, apk)
        }
    }
}
