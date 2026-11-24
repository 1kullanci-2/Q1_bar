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
                "Hoş geldiniz",
                "Medya Paneli, çalan müziğin bilgilerini kilit ekranında ve ana ekranda gösterir. " +
                    "Bu işlevler için aşağıdaki izinler gereklidir. Her adımda iznin ne amaçla kullanıldığı açıklanır.",
                null, null, null
            )
        )
        list.add(
            Step(
                "Bildirim erişimi",
                "Şarkı adı, sanatçı, kapak görseli ve çalma süresi, telefonun medya bildirimlerinden okunur. " +
                    "Panelin çalışması için zorunludur. Açılan listede Medya Paneli etkinleştirilmelidir.",
                "İzin ver", { Perms.notificationAccess(this) }, { Perms.openNotificationAccess(this) }
            )
        )
        list.add(
            Step(
                "Diğer uygulamaların üstünde göster",
                "Müzik başladığında panelin kilit ekranında otomatik olarak açılmasını sağlar. " +
                    "İzin verilmezse panel yalnızca bildirim olarak görüntülenir.",
                "İzin ver", { Perms.overlay(this) }, { Perms.openOverlay(this) }
            )
        )
        if (Perms.postNotificationsNeeded()) {
            list.add(
                Step(
                    "Bildirim gönderme",
                    "Uygulamanın arka planda çalışır durumda kalmasını sağlayan sessiz bildirimin gösterilmesi için gereklidir.",
                    "İzin ver", { Perms.postNotifications(this) },
                    { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7) }
                )
            )
        }
        list.add(
            Step(
                "Pil kısıtlaması",
                "Pil tasarrufu ayarları uygulamayı arka planda kapatabilir; bu durumda panel kilit ekranında açılmaz. " +
                    "Bu nedenle uygulama için pil kısıtlaması kaldırılmalıdır.\n\n" +
                    "Huawei cihazlarda ayrıca: Ayarlar > Pil > Uygulama başlatma > Medya Paneli bölümünde “Otomatik yönet” kapatılarak " +
                    "Otomatik başlat, İkincil başlat ve Arka planda çalış seçenekleri açılmalıdır.",
                "İzin ver", { Perms.battery(this) }, { Perms.openBattery(this) }
            )
        )
        list.add(
            Step(
                "Kurulum tamamlandı",
                "Müzik çalarken ekran kapatılıp açıldığında panel kilit ekranında görüntülenir. " +
                    "Saat, yazı tipi ve diğer ayarlar ana ekrandan değiştirilebilir; bu sihirbaz da ana ekrandan yeniden çalıştırılabilir.",
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
