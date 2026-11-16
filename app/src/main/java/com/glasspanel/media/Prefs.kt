package com.glasspanel.media

import android.content.Context

object Prefs {
    const val MODE_OFF = 0
    const val MODE_BAR = 1
    const val MODE_FULL = 2

    private fun sp(c: Context) = c.getSharedPreferences("medya_paneli", Context.MODE_PRIVATE)

    /** Kilit ekranında ne gösterilsin: kapalı / alt çubuk / tam panel */
    fun mode(c: Context): Int = sp(c).getInt("lock_mode", MODE_BAR)
    fun setMode(c: Context, v: Int) = sp(c).edit().putInt("lock_mode", v).apply()

    fun onlyWhilePlaying(c: Context): Boolean = sp(c).getBoolean("only_playing", true)
    fun setOnlyWhilePlaying(c: Context, v: Boolean) = sp(c).edit().putBoolean("only_playing", v).apply()
}
