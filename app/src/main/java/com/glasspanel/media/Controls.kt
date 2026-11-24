package com.glasspanel.media

import android.content.Context
import android.media.session.PlaybackState
import android.widget.Toast

/**
 * Karıştır, tekrar ve beğen düğmeleri. Müzik uygulamasının paylaştığı özel eylemler gönderilir.
 * Durum önce eylemin kimliğinden/adından okunur (ör. Spotify "TURN_REPEAT_ALL_ON", VIVI "Karıştırma açık");
 * okunamazsa dokunuşlar sayılarak izlenir.
 * Tekrar: 0 = kapalı, 1 = liste, 2 = tek şarkı.
 */
object Controls {
    private val SHUFFLE_KEYS = listOf("shuffle", "karıştır", "karistir")
    private val REPEAT_KEYS = listOf("repeat", "tekrar")
    private val LIKE_KEYS = listOf("like", "check", "heart", "collection", "favorite", "favourite", "beğen", "koleksiyon")

    private var countedRepeat = 0
    private var countedShuffle = false
    private var pkg: String? = null

    /** Müzik uygulaması değişince sayaçla izlenen durumu sıfırlar. */
    fun sync() {
        val p = MediaRepo.info?.packageName
        if (p != pkg) {
            pkg = p
            countedRepeat = 0
            countedShuffle = false
        }
    }

    fun findCustom(keys: List<String>): PlaybackState.CustomAction? {
        val st = MediaRepo.controller?.playbackState ?: return null
        for (a in st.customActions) {
            val s = text(a)
            if (keys.any { s.contains(it) }) return a
        }
        return null
    }

    private fun text(a: PlaybackState.CustomAction) = (a.action + " " + (a.name ?: "")).lowercase()

    val repeat: Int
        get() {
            sync()
            val a = findCustom(REPEAT_KEYS)
            return (if (a != null) inferRepeat(text(a)) else null) ?: countedRepeat
        }

    val shuffle: Boolean
        get() {
            sync()
            val a = findCustom(SHUFFLE_KEYS)
            return (if (a != null) inferShuffle(text(a)) else null) ?: countedShuffle
        }

    /** Eylem "sonraki modu aç" der; bu yüzden mevcut mod, eylemin hedefinden bir önceki moddur. */
    private fun inferRepeat(s: String): Int? {
        if (!(s.contains("repeat_") || s.contains("repeat "))) return null
        return when {
            s.contains("one") || s.contains("track") -> 1
            s.contains("all") && s.contains("off") -> 1
            s.contains("off") -> 2
            s.contains("all") -> 0
            else -> null
        }
    }

    private fun inferShuffle(s: String): Boolean? {
        return when {
            s.contains("shuffle_on") -> false
            s.contains("shuffle_off") -> true
            s.contains("açık") -> true
            s.contains("kapalı") -> false
            else -> null
        }
    }

    fun repeatSupported() = findCustom(REPEAT_KEYS) != null
    fun shuffleSupported() = findCustom(SHUFFLE_KEYS) != null
    fun likeSupported() = findCustom(LIKE_KEYS) != null

    /** Beğenilmiş mi? Eylem "kaldır / remove / fill" diyorsa evet. Desteklenmiyorsa null. */
    fun liked(): Boolean? {
        val a = findCustom(LIKE_KEYS) ?: return null
        val s = text(a)
        return s.contains("kaldır") || s.contains("remove") || s.contains("fill") ||
            s.contains("unlike") || s.contains("liked")
    }

    fun tapRepeat(ctx: Context) {
        sync()
        val a = findCustom(REPEAT_KEYS)
        if (a == null) {
            Toast.makeText(ctx, "Bu uygulama tekrar düğmesini paylaşmıyor", Toast.LENGTH_SHORT).show()
            return
        }
        MediaRepo.controller?.transportControls?.sendCustomAction(a, null)
        countedRepeat = (repeat + 1) % 3
    }

    fun tapShuffle(ctx: Context) {
        sync()
        val a = findCustom(SHUFFLE_KEYS)
        if (a == null) {
            Toast.makeText(ctx, "Bu uygulama karıştır düğmesini paylaşmıyor", Toast.LENGTH_SHORT).show()
            return
        }
        MediaRepo.controller?.transportControls?.sendCustomAction(a, null)
        countedShuffle = !shuffle
    }

    fun tapLike(ctx: Context) {
        val a = findCustom(LIKE_KEYS)
        if (a == null) {
            Toast.makeText(ctx, "Bu uygulama beğen düğmesini paylaşmıyor", Toast.LENGTH_SHORT).show()
            return
        }
        MediaRepo.controller?.transportControls?.sendCustomAction(a, null)
    }
}
