package com.glasspanel.media

import android.content.Context
import org.json.JSONObject

/**
 * Anonim toplam kurulum sayacı. İlk açılışta rastgele kimliği bir kez bildirir; silinince düşmez.
 * Adres Config.COUNTER_URL boşsa hiçbir şey yapmaz.
 */
object InstallCounter {

    val enabled: Boolean get() = Config.COUNTER_URL.isNotBlank()

    fun register(ctx: Context) {
        if (!enabled || Prefs.installCounted(ctx)) return
        val app = ctx.applicationContext
        val id = Prefs.installId(app)
        Thread {
            try {
                val c = Net.open(Config.COUNTER_URL.trimEnd('/') + "/install", "POST")
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write("""{"id":"$id"}""".toByteArray()) }
                if (c.responseCode in 200..299) Prefs.setInstallCounted(app, true)
                c.disconnect()
            } catch (_: Exception) {
            }
        }.start()
    }

    /** Toplam kurulum sayısını getirir (arka plan iş parçacığında çağır). */
    fun fetchTotal(): Int {
        val t = Net.getText(Config.COUNTER_URL.trimEnd('/') + "/count")
        return JSONObject(t).getInt("total")
    }
}
