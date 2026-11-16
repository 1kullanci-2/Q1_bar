package com.glasspanel.media

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("medya_paneli", Context.MODE_PRIVATE)

    /** Müzik çalınca kilit ekranında panel kendiliğinden açılsın mı? (eski "çubuk" ayarı da açık sayılır) */
    fun lockPanel(c: Context): Boolean = sp(c).getInt("lock_mode", 2) != 0
    fun setLockPanel(c: Context, v: Boolean) = sp(c).edit().putInt("lock_mode", if (v) 2 else 0).apply()

    fun onlyWhilePlaying(c: Context): Boolean = sp(c).getBoolean("only_playing", true)
    fun setOnlyWhilePlaying(c: Context, v: Boolean) = sp(c).edit().putBoolean("only_playing", v).apply()

    /** 0 = Outfit, 1 = Fraunces, 2 = Space Grotesk */
    fun clockFont(c: Context): Int = sp(c).getInt("clock_font", 0)
    fun setClockFont(c: Context, v: Int) = sp(c).edit().putInt("clock_font", v).apply()

    /** Saat boyutu yüzdesi (70..130) */
    fun clockSize(c: Context): Int = sp(c).getInt("clock_size", 100)
    fun setClockSize(c: Context, v: Int) = sp(c).edit().putInt("clock_size", v).apply()

    fun showDate(c: Context): Boolean = sp(c).getBoolean("show_date", true)
    fun setShowDate(c: Context, v: Boolean) = sp(c).edit().putBoolean("show_date", v).apply()

    fun clock24(c: Context): Boolean = sp(c).getBoolean("clock_24", true)
    fun setClock24(c: Context, v: Boolean) = sp(c).edit().putBoolean("clock_24", v).apply()
}
