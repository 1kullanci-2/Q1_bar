package com.glasspanel.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File

/**
 * Yapımcı profili ve depo adresi. Depo, sabit numarasıyla sorgulanır; böylece kullanıcı adı ya da depo adı
 * değişse bile güncel sahip, ad ve adresler bulunur. Son bilinen değerler çevrimdışı gösterim için saklanır.
 */
object Profile {
    private fun sp(c: Context) = c.getSharedPreferences("profile", Context.MODE_PRIVATE)

    fun login(c: Context): String = sp(c).getString("login", null) ?: Config.OWNER
    fun displayName(c: Context): String = sp(c).getString("name", null)?.takeIf { it.isNotBlank() } ?: login(c)
    fun bio(c: Context): String = sp(c).getString("bio", "") ?: ""
    fun profileUrl(c: Context): String = "https://github.com/" + login(c)
    fun repoFullName(c: Context): String = sp(c).getString("repo", null) ?: "${Config.OWNER}/${Config.REPO}"
    fun repoUrl(c: Context): String = "https://github.com/" + repoFullName(c)

    fun avatar(c: Context): Bitmap? {
        val f = File(c.filesDir, "avatar.png")
        return if (f.exists()) BitmapFactory.decodeFile(f.path) else null
    }

    /** Ağdan günceller; bağlantı yoksa exception fırlatır (eski değerler korunur). Arka plan iş parçacığında çağır. */
    fun refresh(c: Context) {
        val app = c.applicationContext
        val repo = JSONObject(Net.getText("https://api.github.com/repositories/${Config.REPO_ID}"))
        val owner = repo.getJSONObject("owner").getString("login")
        val user = JSONObject(Net.getText("https://api.github.com/users/$owner"))

        val name = user.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: ""
        val bio = user.optString("bio").takeIf { it.isNotBlank() && it != "null" } ?: ""
        val avatarUrl = user.optString("avatar_url")

        val old = sp(app).getString("avatar_url", null)
        if (avatarUrl.isNotBlank() && (avatarUrl != old || !File(app.filesDir, "avatar.png").exists())) {
            val bmp: Bitmap? = Net.getBitmap("$avatarUrl&s=200")
            if (bmp != null) {
                File(app.filesDir, "avatar.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        sp(app).edit()
            .putString("login", owner)
            .putString("name", name)
            .putString("bio", bio)
            .putString("avatar_url", avatarUrl)
            .putString("repo", repo.optString("full_name", "$owner/${Config.REPO}"))
            .apply()
    }
}
