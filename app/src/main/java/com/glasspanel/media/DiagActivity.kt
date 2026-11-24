package com.glasspanel.media

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.media.session.PlaybackState
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** Medya tanı: çalan uygulamanın medya oturumu üzerinden gerçekte neleri paylaştığını listeler. */
class DiagActivity : AppCompatActivity() {

    private lateinit var out: TextView
    private val listener: () -> Unit = { render() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dpi(20f), dpi(26f), dpi(20f), dpi(36f))

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.addView(Ui.outlined(this, "‹  Geri") { finish() })
        val copy = Ui.filled(this, "Kopyala") {
            val cm = getSystemService(ClipboardManager::class.java)
            cm?.setPrimaryClip(ClipData.newPlainText("Medya tanı", out.text))
            Toast.makeText(this, "Kopyalandı", Toast.LENGTH_SHORT).show()
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.leftMargin = dpi(10f)
        bar.addView(copy, lp)
        root.addView(bar)

        root.addView(Ui.text(this, "Medya tanı", 26f, bold = true), Ui.lp(this, 18f))
        root.addView(
            Ui.soft(
                this,
                "Müzik uygulamasını çaldırırken bu ekranı aç. Uygulamanın paylaştığı sıra, düğmeler ve desteklenen işlemler listelenir. " +
                    "Her uygulama için ayrı bakılmalıdır."
            ),
            Ui.lp(this, 4f)
        )

        out = TextView(this)
        out.typeface = Typeface.MONOSPACE
        out.textSize = 12f
        out.setTextColor(getColor(R.color.ink))
        out.setTextIsSelectable(true)
        val card = Ui.card(this, root, 16f)
        card.addView(out)

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        render()
    }

    override fun onStart() {
        super.onStart()
        MediaRepo.addListener(listener)
        render()
    }

    override fun onStop() {
        MediaRepo.removeListener(listener)
        super.onStop()
    }

    private fun actionNames(a: Long): String {
        val names = listOf(
            1L to "durdur", 2L to "duraklat", 4L to "oynat", 8L to "geri sar", 16L to "önceki", 32L to "sonraki",
            64L to "ileri sar", 128L to "puanla", 256L to "konuma atla", 512L to "oynat/duraklat",
            1024L to "kimlikle oynat", 2048L to "aramayla oynat", 4096L to "sıradaki öğeye atla", 8192L to "adresle oynat"
        )
        val list = names.filter { a and it.first != 0L }.map { it.second }.toMutableList()
        val rest = a and 16383L.inv()
        if (rest != 0L) list.add("diğer(0x" + java.lang.Long.toHexString(rest) + ")")
        return if (list.isEmpty()) "yok" else list.joinToString(", ")
    }

    private fun render() {
        val sb = StringBuilder()
        val c = MediaRepo.controller
        val i = MediaRepo.info
        if (c == null || i == null) {
            out.text = "Etkin bir medya oturumu yok.\nBir müzik uygulamasında şarkı başlat, sonra bu ekrana dön."
            return
        }
        sb.append("Uygulama: ").append(i.appName).append(" (").append(c.packageName).append(")\n")
        sb.append("Şarkı: ").append(i.title).append(" — ").append(i.artist).append("\n")

        val st: PlaybackState? = c.playbackState
        sb.append("\nDURUM\n")
        sb.append("Çalma durumu: ").append(st?.state ?: "bilinmiyor").append("\n")
        sb.append("Desteklenen işlemler: ").append(if (st != null) actionNames(st.actions) else "bilinmiyor").append("\n")

        sb.append("\nÖZEL DÜĞMELER (karıştır / tekrar / beğen burada görünür)\n")
        val ca = st?.customActions
        if (ca.isNullOrEmpty()) {
            sb.append("Paylaşılmıyor\n")
        } else {
            for (a in ca) sb.append("• ").append(a.name).append("  [").append(a.action).append("]\n")
        }
        sb.append("Karıştır algılandı: ").append(if (Controls.shuffleSupported()) "evet" else "hayır").append("\n")
        sb.append("Tekrar algılandı: ").append(if (Controls.repeatSupported()) "evet" else "hayır").append("\n")

        sb.append("\nSIRA\n")
        val q = try { c.queue } catch (_: Exception) { null }
        if (q.isNullOrEmpty()) {
            sb.append("Sıra paylaşılmıyor\n")
        } else {
            sb.append("Sıra başlığı: ").append(c.queueTitle ?: "-").append("\n")
            sb.append("Öğe sayısı: ").append(q.size).append("\n")
            val active = st?.activeQueueItemId ?: -1L
            for ((n, item) in q.take(15).withIndex()) {
                val d = item.description
                sb.append(if (item.queueId == active) "▶ " else "  ")
                    .append(n + 1).append(". ").append(d.title ?: "?")
                if (!d.subtitle.isNullOrBlank()) sb.append(" — ").append(d.subtitle)
                sb.append("\n")
            }
            if (q.size > 15) sb.append("  … ve ").append(q.size - 15).append(" öğe daha\n")
        }
        out.text = sb.toString()
    }
}
