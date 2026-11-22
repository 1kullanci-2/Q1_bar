package com.glasspanel.media

import android.Manifest
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

/** İlk açılış sihirbazı: her izni açıklar, "İzin ver" ile doğru ayar ekranına götürür. */
class SetupActivity : AppCompatActivity() {

    private class Step(
        val title: String,
        val body: String,
        val grantLabel: String?,
        /** null = durum otomatik anlaşılamıyor */
        val granted: (() -> Boolean)?,
        val grant: (() -> Unit)?
    )

    private lateinit var steps: List<Step>
    private var index = 0

    private lateinit var progress: LinearProgressIndicator
    private lateinit var counter: TextView
    private lateinit var title: TextView
    private lateinit var body: TextView
    private lateinit var status: TextView
    private lateinit var grantBtn: MaterialButton
    private lateinit var nextBtn: MaterialButton
    private lateinit var backBtn: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val list = ArrayList<Step>()
        list.add(
            Step(
                "Hoş geldin",
                "Medya Paneli, çalan müziği kilit ekranında ve ana ekranda gösterir. " +
                    "Çalışması için birkaç izne ihtiyacı var. Şimdi sırayla, her birinin ne işe yaradığını anlatıp ilgili ayar ekranını açacağım.",
                null, null, null
            )
        )
        list.add(
            Step(
                "Bildirim erişimi",
                "Hangi şarkının çaldığını (ad, sanatçı, kapak, süre) telefonun medya bildirimlerinden okumak için gerekir. " +
                    "Bu izin olmadan panel hiçbir şey gösteremez. Açılan listeden Medya Paneli'ni bulup aç.",
                "İzin ver", { Perms.notificationAccess(this) }, { Perms.openNotificationAccess(this) }
            )
        )
        list.add(
            Step(
                "Diğer uygulamaların üstünde göster",
                "Müzik başlayınca panelin kilit ekranında kendiliğinden açılabilmesi için gerekir. " +
                    "Kapalıysa panel yalnızca bildirim olarak çıkar. Açılan ekranda Medya Paneli'ni aç.",
                "İzin ver", { Perms.overlay(this) }, { Perms.openOverlay(this) }
            )
        )
        if (Perms.postNotificationsNeeded()) {
            list.add(
                Step(
                    "Bildirim gönderme",
                    "Uygulamanın arka planda hazır kalırken küçük bir sessiz bildirim göstermesi için gerekir.",
                    "İzin ver", { Perms.postNotifications(this) },
                    { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7) }
                )
            )
        }
        list.add(
            Step(
                "Pil kısıtlaması",
                "Telefon pil tasarrufu için uygulamayı arka planda kapatırsa panel kilit ekranında açılmaz. " +
                    "Açılan pencerede \"İzin ver\" de; liste açılırsa Medya Paneli için kısıtlamayı kaldır. " +
                    "Huawei kullanıyorsan ayrıca: Ayarlar > Pil > Uygulama başlatma > Medya Paneli > Otomatik yönet'i kapat; Otomatik başlat, İkincil başlat ve Arka planda çalış'ı aç.",
                "İzin ver", { Perms.battery(this) }, { Perms.openBattery(this) }
            )
        )
        list.add(
            Step(
                "Hazırsın",
                "Müzik çal, ekranı kapatıp tekrar aç: panel kilit ekranında belirir. " +
                    "Saat yazı tipi, boyut ve diğer ayarları ana ekrandan değiştirebilirsin. Bu sihirbazı istediğin zaman ana ekrandan yeniden çalıştırabilirsin.",
                null, null, null
            )
        )
        steps = list

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dpi(22f), dpi(30f), dpi(22f), dpi(24f))

        counter = Ui.section(this, "")
        root.addView(counter)

        progress = LinearProgressIndicator(this)
        progress.max = steps.size - 1
        progress.trackThickness = dpi(6f)
        progress.trackCornerRadius = dpi(3f)
        progress.setIndicatorColor(getColor(R.color.green_ok))
        progress.trackColor = getColor(R.color.surface_var)
        root.addView(progress, Ui.lp(this, 10f))

        title = Ui.text(this, "", 28f, bold = true)
        root.addView(title, Ui.lp(this, 34f))

        body = Ui.text(this, "", 16f, getColor(R.color.ink_soft))
        body.setLineSpacing(0f, 1.15f)
        root.addView(body, Ui.lp(this, 14f))

        status = TextView(this)
        status.gravity = Gravity.CENTER
        val sp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        sp.topMargin = dpi(20f)
        root.addView(status, sp)

        val spacer = android.view.View(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))

        grantBtn = Ui.filled(this, "İzin ver") { onGrant() }
        root.addView(grantBtn, Ui.lp(this, 20f))

        val nav = LinearLayout(this)
        nav.orientation = LinearLayout.HORIZONTAL
        backBtn = Ui.outlined(this, "Geri") { if (index > 0) { index--; render() } }
        nextBtn = Ui.outlined(this, "Sonraki") { onNext() }
        val half = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        nav.addView(backBtn, half)
        val half2 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        half2.leftMargin = dpi(10f)
        nav.addView(nextBtn, half2)
        root.addView(nav, Ui.lp(this, 10f))

        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.addView(root, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        setContentView(scroll)

        render()
    }

    override fun onResume() {
        super.onResume()
        if (::steps.isInitialized) render()
    }

    private fun onGrant() {
        steps[index].grant?.invoke()
    }

    private fun onNext() {
        if (index >= steps.size - 1) {
            Prefs.setSetupDone(this, true)
            finish()
        } else {
            index++
            render()
        }
    }

    private fun render() {
        val s = steps[index]
        val last = index == steps.size - 1
        counter.text = "ADIM ${index + 1} / ${steps.size}"
        progress.setProgressCompat(index, true)
        title.text = s.title
        body.text = s.body

        val g = s.granted
        if (g != null) {
            val ok = g()
            status.visibility = android.view.View.VISIBLE
            Ui.setPill(status, ok, "Verildi", "Henüz verilmedi")
            grantBtn.visibility = if (ok) android.view.View.GONE else android.view.View.VISIBLE
            grantBtn.text = s.grantLabel
            nextBtn.text = if (ok) "Devam" else "Atla"
        } else if (s.grant != null) {
            status.visibility = android.view.View.GONE
            grantBtn.visibility = android.view.View.VISIBLE
            grantBtn.text = s.grantLabel
            nextBtn.text = "Yaptım"
        } else {
            status.visibility = android.view.View.GONE
            grantBtn.visibility = android.view.View.GONE
            nextBtn.text = if (last) "Bitir" else "Başla"
        }
        if (last) nextBtn.text = "Bitir"
        backBtn.visibility = if (index == 0) android.view.View.INVISIBLE else android.view.View.VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        render()
    }
}
