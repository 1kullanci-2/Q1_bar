package com.glasspanel.media

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService

class MediaListenerService : NotificationListenerService() {

    companion object {
        const val NOTIF_ID = 4242
        private const val CHANNEL = "kilit_paneli"
    }

    private var msm: MediaSessionManager? = null
    private var component: ComponentName? = null
    private var current: MediaController? = null
    private var receiverRegistered = false
    private var lastPlaying = false
    private var artKey: String? = null
    private var artCache: Bitmap? = null
    private val handler = Handler(Looper.getMainLooper())
    private val iconCache = HashMap<String, Bitmap?>()

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            rebuild()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            rebuild()
        }

        override fun onSessionDestroyed() {
            refresh()
        }
    }

    private val widgetListener: () -> Unit = {
        MediaWidgetProvider.refresh(this@MediaListenerService)
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) showForLock()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val m = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val comp = ComponentName(this, MediaListenerService::class.java)
        msm = m
        component = comp
        try {
            m.addOnActiveSessionsChangedListener(sessionsListener, comp)
        } catch (_: SecurityException) {
        }
        if (!receiverRegistered) {
            val f = IntentFilter()
            f.addAction(Intent.ACTION_SCREEN_ON)
            registerReceiver(screenReceiver, f)
            receiverRegistered = true
        }
        MediaRepo.removeListener(widgetListener)
        MediaRepo.addListener(widgetListener)
        KeepAliveService.start(this)
        refresh()
    }

    override fun onListenerDisconnected() {
        cleanup()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        try {
            msm?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (_: Exception) {
        }
        current?.let {
            try {
                it.unregisterCallback(callback)
            } catch (_: Exception) {
            }
        }
        current = null
        if (receiverRegistered) {
            try {
                unregisterReceiver(screenReceiver)
            } catch (_: Exception) {
            }
            receiverRegistered = false
        }
        MediaRepo.removeListener(widgetListener)
        MediaRepo.publish(null, null)
        MediaWidgetProvider.refresh(this)
    }

    private fun refresh() {
        val m = msm ?: return
        val comp = component ?: return
        val list = try {
            m.getActiveSessions(comp)
        } catch (_: SecurityException) {
            emptyList<MediaController>()
        }
        pick(list)
    }

    private fun pick(list: List<MediaController>?) {
        val chosen = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list?.firstOrNull()
        if (chosen?.sessionToken != current?.sessionToken) {
            current?.let {
                try {
                    it.unregisterCallback(callback)
                } catch (_: Exception) {
                }
            }
            current = chosen
            chosen?.registerCallback(callback, handler)
        }
        rebuild()
    }

    private fun rebuild() {
        val c = current
        val md = c?.metadata
        if (c == null || md == null) {
            lastPlaying = false
            MediaRepo.publish(null, null)
            return
        }
        val st = c.playbackState
        val title = md.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: ""
        val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
        val album = md.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = md.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val art = md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: loadArtFromUri(md)
        val playing = st?.state == PlaybackState.STATE_PLAYING
        val info = TrackInfo(
            title = title,
            artist = artist,
            album = album,
            art = art,
            durationMs = duration,
            positionMs = st?.position ?: 0L,
            updatedAt = st?.lastPositionUpdateTime ?: android.os.SystemClock.elapsedRealtime(),
            speed = st?.playbackSpeed ?: 1f,
            isPlaying = playing,
            appName = appLabel(c.packageName),
            packageName = c.packageName,
            appIcon = iconBitmap(c.packageName)
        )
        MediaRepo.publish(c, info)

        if (playing && !lastPlaying) showForLock()
        lastPlaying = playing
    }

    private fun loadArtFromUri(md: MediaMetadata): Bitmap? {
        val uriStr = md.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
            ?: return null
        if (uriStr == artKey) return artCache
        val bmp = try {
            if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                contentResolver.openInputStream(Uri.parse(uriStr))?.use { BitmapFactory.decodeStream(it) }
            } else null
        } catch (_: Exception) {
            null
        }
        artKey = uriStr
        artCache = bmp
        return bmp
    }

    private fun iconBitmap(pkg: String): Bitmap? {
        if (iconCache.containsKey(pkg)) return iconCache[pkg]
        val bmp = try {
            val d = packageManager.getApplicationIcon(pkg)
            val s = 96
            val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
            val cv = Canvas(b)
            d.setBounds(0, 0, s, s)
            d.draw(cv)
            b
        } catch (_: Exception) {
            null
        }
        iconCache[pkg] = bmp
        return bmp
    }

    private fun appLabel(pkg: String): String {
        return try {
            val ai = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            pkg.substringAfterLast('.')
        }
    }

    /** Kilit ekranındayken tam paneli hemen açar. */
    private fun showForLock() {
        if (!Prefs.lockPanel(this)) return
        val i = MediaRepo.info ?: return
        if (Prefs.onlyWhilePlaying(this) && !i.isPlaying) return
        val km = getSystemService(KeyguardManager::class.java)
        if (km == null || !km.isKeyguardLocked) return
        launchPanel(i)
    }

    private fun launchPanel(i: TrackInfo) {
        val intent = Intent(this, LockPanelActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )
        // Android 10+ arka plandan aktivite açmak için "diğer uygulamaların üstünde göster" izni gerekir
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(this)) {
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
        // İzin yoksa tam ekran bildirim ile dene
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Kilit ekranı paneli", NotificationManager.IMPORTANCE_HIGH)
        )
        val pi = PendingIntent.getActivity(
            this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_play)
            .setContentTitle(i.title.ifBlank { "Medya Paneli" })
            .setContentText(i.artist)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setAutoCancel(true)
            .setFullScreenIntent(pi, true)
            .build()
        try {
            nm.notify(NOTIF_ID, n)
        } catch (_: SecurityException) {
        }
    }
}
