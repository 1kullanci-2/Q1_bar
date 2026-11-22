package com.glasspanel.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

/** GitHub Releases'ten son sürümü bulur, APK'yı indirir ve sistem yükleyicisini açar. */
object Updater {

    data class Release(val code: Int, val name: String, val apkUrl: String, val size: Long, val notes: String)

    /** Yeni sürüm varsa Release, yoksa null döner. Hata olursa exception fırlatır. */
    fun check(): Release? {
        val json = JSONObject(Net.getText("https://api.github.com/repos/${Config.OWNER}/${Config.REPO}/releases/latest"))
        val tag = json.optString("tag_name")
        val code = tag.filter { it.isDigit() }.toIntOrNull() ?: return null
        val assets = json.optJSONArray("assets") ?: return null
        var url: String? = null
        var size = 0L
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk")) {
                url = a.optString("browser_download_url")
                size = a.optLong("size")
                break
            }
        }
        if (url == null || code <= BuildConfig.VERSION_CODE) return null
        return Release(code, json.optString("name", tag), url, size, json.optString("body"))
    }

    /** APK'yı önbelleğe indirir; ilerleme 0..100 olarak bildirilir. */
    fun download(ctx: Context, r: Release, progress: (Int) -> Unit): File {
        val dir = File(ctx.cacheDir, "updates")
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "update.apk")
        val c = Net.open(r.apkUrl)
        c.setRequestProperty("Accept", "application/octet-stream")
        try {
            if (c.responseCode !in 200..299) throw RuntimeException("HTTP ${c.responseCode}")
            val total = if (c.contentLengthLong > 0) c.contentLengthLong else r.size
            var read = 0L
            var last = -1
            c.inputStream.use { input ->
                out.outputStream().use { o ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        o.write(buf, 0, n)
                        read += n
                        if (total > 0) {
                            val p = (read * 100 / total).toInt().coerceIn(0, 100)
                            if (p != last) {
                                last = p
                                progress(p)
                            }
                        }
                    }
                }
            }
            if (total > 0 && read != total) throw RuntimeException("İndirme yarım kaldı")
        } finally {
            c.disconnect()
        }
        return out
    }

    fun canInstall(ctx: Context): Boolean = ctx.packageManager.canRequestPackageInstalls()

    /** "Bilinmeyen uygulamaları yükle" izin ekranını açar. */
    fun openInstallPermission(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
