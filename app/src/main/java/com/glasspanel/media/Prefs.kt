package com.glasspanel.media

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("medya_paneli", Context.MODE_PRIVATE)

    fun lockPanel(c: Context): Boolean = sp(c).getBoolean("lock_panel", true)
    fun setLockPanel(c: Context, v: Boolean) = sp(c).edit().putBoolean("lock_panel", v).apply()

    fun onlyWhilePlaying(c: Context): Boolean = sp(c).getBoolean("only_playing", true)
    fun setOnlyWhilePlaying(c: Context, v: Boolean) = sp(c).edit().putBoolean("only_playing", v).apply()

    // 0..100: yüksek değer = cam daha şeffaf, arkadaki kapak bulanıklığı daha görünür
    fun glass(c: Context): Int = sp(c).getInt("glass", 60)
    fun setGlass(c: Context, v: Int) = sp(c).edit().putInt("glass", v).apply()
}
