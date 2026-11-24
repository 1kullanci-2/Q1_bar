package com.glasspanel.media

import android.content.Context
import android.media.session.PlaybackState
import android.widget.Toast

/**
 * Karıştır ve tekrar düğmeleri. Müzik uygulamasının paylaştığı özel eylemler gönderilir; uygulamalar mevcut modu
 * dışarı bildirmediği için durum burada izlenir.
 * Tekrar: 0 = kapalı, 1 = liste, 2 = tek şarkı (her dokunuşta sırayla ilerler).
 */
object Controls {
    private val SHUFFLE_KEYS = listOf("shuffle", "karıştır", "karistir")
    private val REPEAT_KEYS = listOf("repeat", "tekrar")

    var repeat = 0
        private set
    var shuffle = false
        private set
    private var pkg: String? = null

    /** Müzik uygulaması değişince izlenen durumu sıfırlar. */
    fun sync() {
        val p = MediaRepo.info?.packageName
        if (p != pkg) {
            pkg = p
            repeat = 0
            shuffle = false
        }
    }

    fun findCustom(keys: List<String>): PlaybackState.CustomAction? {
        val st = MediaRepo.controller?.playbackState ?: return null
        for (a in st.customActions) {
            val s = (a.action + " " + (a.name ?: "")).lowercase()
            if (keys.any { s.contains(it) }) return a
        }
        return null
    }

    fun repeatSupported() = findCustom(REPEAT_KEYS) != null
    fun shuffleSupported() = findCustom(SHUFFLE_KEYS) != null

    fun tapRepeat(ctx: Context) {
        sync()
        val a = findCustom(REPEAT_KEYS)
        if (a == null) {
            Toast.makeText(ctx, "Bu uygulama tekrar düğmesini paylaşmıyor", Toast.LENGTH_SHORT).show()
            return
        }
        MediaRepo.controller?.transportControls?.sendCustomAction(a, null)
        repeat = (repeat + 1) % 3
    }

    fun tapShuffle(ctx: Context) {
        sync()
        val a = findCustom(SHUFFLE_KEYS)
        if (a == null) {
            Toast.makeText(ctx, "Bu uygulama karıştır düğmesini paylaşmıyor", Toast.LENGTH_SHORT).show()
            return
        }
        MediaRepo.controller?.transportControls?.sendCustomAction(a, null)
        shuffle = !shuffle
    }
}
