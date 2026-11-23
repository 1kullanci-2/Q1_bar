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

    /** 0 = kapak odaklı, 1 = Material */
    fun panelStyle(c: Context): Int = sp(c).getInt("panel_style", 0)
    fun setPanelStyle(c: Context, v: Int) = sp(c).edit().putInt("panel_style", v).apply()

    fun setupDone(c: Context): Boolean = sp(c).getBoolean("setup_done", false)
    fun setSetupDone(c: Context, v: Boolean) = sp(c).edit().putBoolean("setup_done", v).apply()

    /** Bu kuruluma ait rastgele, anonim kimlik (uygulama silinince yenilenir) */
    fun installId(c: Context): String {
        val cur = sp(c).getString("install_id", null)
        if (cur != null) return cur
        val id = java.util.UUID.randomUUID().toString()
        sp(c).edit().putString("install_id", id).apply()
        return id
    }

    fun installCounted(c: Context): Boolean = sp(c).getBoolean("install_counted", false)
    fun setInstallCounted(c: Context, v: Boolean) = sp(c).edit().putBoolean("install_counted", v).apply()

    fun lastUpdateCheck(c: Context): Long = sp(c).getLong("last_update_check", 0L)
    fun setLastUpdateCheck(c: Context, v: Long) = sp(c).edit().putLong("last_update_check", v).apply()
}
