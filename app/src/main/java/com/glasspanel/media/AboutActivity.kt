package com.glasspanel.media

import android.content.Intent
import android.graphics.Outline
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject

/** Sürüm bilgisi ve yapımcı (GitHub profili canlı çekilir). Sürüm yazısına 7 kez dokununca gizli sayaç açılır. */
class AboutActivity : AppCompatActivity() {

    private var taps = 0
    private var lastTap = 0L

    private lateinit var avatar: ImageView
    private lateinit var devName: TextView
    private lateinit var devBio: TextView
    private lateinit var devStats: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dpi(20f), dpi(26f), dpi(20f), dpi(36f))

        root.addView(Ui.outlined(this, "‹  Geri") { finish() }.also {
            it.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })

        val head = LinearLayout(this)
        head.orientation = LinearLayout.VERTICAL
        head.gravity = Gravity.CENTER_HORIZONTAL
        val icon = ImageView(this)
        icon.setImageResource(R.drawable.ic_launcher)
        head.addView(icon, LinearLayout.LayoutParams(dpi(84f), dpi(84f)))
        val name = Ui.text(this, "Medya Paneli", 26f, bold = true)
        head.addView(name, Ui.lp(this, 14f).also { it.gravity = Gravity.CENTER_HORIZONTAL })
        val version = Ui.soft(this, "Sürüm ${BuildConfig.VERSION_NAME}  ·  yapı ${BuildConfig.VERSION_CODE}", 14f)
        version.setPadding(dpi(16f), dpi(8f), dpi(16f), dpi(8f))
        version.setOnClickListener { onVersionTap() }
        head.addView(version, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(head, Ui.lp(this, 18f))

        root.addView(Ui.section(this, "YAPIMCI"), Ui.lp(this, 24f))
        val card = Ui.card(this, root, 8f)

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        avatar = ImageView(this)
        avatar.scaleType = ImageView.ScaleType.CENTER_CROP
        avatar.setBackgroundColor(getColor(R.color.surface_var))
        avatar.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) {
                o.setOval(0, 0, v.width, v.height)
            }
        }
        avatar.clipToOutline = true
        top.addView(avatar, LinearLayout.LayoutParams(dpi(64f), dpi(64f)))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        devName = Ui.text(this, Config.OWNER, 18f, bold = true)
        devStats = Ui.soft(this, "GitHub profili yükleniyor…")
        col.addView(devName)
        col.addView(devStats, Ui.lp(this, 2f))
        val cp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        cp.leftMargin = dpi(14f)
        top.addView(col, cp)
        card.addView(top)

        devBio = Ui.soft(this, "", 14f)
        devBio.visibility = View.GONE
        card.addView(devBio, Ui.lp(this, 12f))

        card.addView(Ui.filled(this, "GitHub profilini aç") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${Config.OWNER}")))
        }, Ui.lp(this, 16f))

        root.addView(Ui.section(this, "PROJE"), Ui.lp(this, 24f))
        val repo = Ui.card(this, root, 8f)
        repo.addView(Ui.row(this, "Kaynak kodu ve sürümler", "github.com/${Config.OWNER}/${Config.REPO}", null) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${Config.OWNER}/${Config.REPO}")))
        })

        root.addView(
            Ui.soft(this, "Yazı tipleri: Outfit, Fraunces, Space Grotesk (SIL Open Font License)", 12f),
            Ui.lp(this, 22f)
        )

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        loadProfile()
    }

    private fun loadProfile() {
        Net.async({
            val j = JSONObject(Net.getText("https://api.github.com/users/${Config.OWNER}"))
            val av = j.optString("avatar_url")
            val bmp = if (av.isNotBlank()) Net.getBitmap("$av&s=200") else null
            Pair(j, bmp)
        }) { r ->
            r.onSuccess { (j, bmp) ->
                val display = j.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: j.optString("login", Config.OWNER)
                devName.text = display
                val bio = j.optString("bio")
                if (bio.isNotBlank() && bio != "null") {
                    devBio.text = bio
                    devBio.visibility = View.VISIBLE
                }
                devStats.text = "@${j.optString("login", Config.OWNER)}"
                if (bmp != null) avatar.setImageBitmap(bmp)
            }
            r.onFailure { devStats.text = "@${Config.OWNER}  ·  profil şu an çekilemedi" }
        }
    }

    private fun onVersionTap() {
        val now = System.currentTimeMillis()
        if (now - lastTap > 1500) taps = 0
        lastTap = now
        taps++
        if (taps in 4..6) {
            Toast.makeText(this, "${7 - taps} dokunuş daha…", Toast.LENGTH_SHORT).show()
        }
        if (taps >= 7) {
            taps = 0
            showCounter()
        }
    }

    private fun showCounter() {
        if (!InstallCounter.enabled) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Kurulum sayacı")
                .setMessage("Sayaç henüz bağlı değil.")
                .setPositiveButton("Tamam", null)
                .show()
            return
        }
        Net.async({ InstallCounter.fetchTotal() }) { r ->
            val msg = r.fold({ "Toplam kurulum: $it\n\nUygulama silinince bu sayı azalmaz; her yeni kurulum 1 ekler." }, { "Sayaç şu an alınamadı." })
            MaterialAlertDialogBuilder(this)
                .setTitle("Kurulum sayacı")
                .setMessage(msg)
                .setPositiveButton("Tamam", null)
                .show()
        }
    }
}
