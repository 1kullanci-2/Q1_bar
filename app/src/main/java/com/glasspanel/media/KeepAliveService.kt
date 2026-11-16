package com.glasspanel.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService

/**
 * Uygulamanın süreci arka planda kapatılmasın ve bildirim dinleyicisi koparsa yeniden bağlansın diye
 * çalışan, sessiz bir ön plan servisi. Telefon açılınca ve uygulama güncellenince de başlar.
 */
class KeepAliveService : Service() {

    companion object {
        private const val CHANNEL = "arka_plan"
        private const val NOTIF_ID = 1001

        fun start(ctx: Context) {
            try {
                ctx.applicationContext.startForegroundService(
                    Intent(ctx.applicationContext, KeepAliveService::class.java)
                )
            } catch (_: Exception) {
            }
        }

        fun rebind(ctx: Context) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(ctx.applicationContext, MediaListenerService::class.java)
                )
            } catch (_: Exception) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(CHANNEL, "Arka plan", NotificationManager.IMPORTANCE_MIN)
            )
            val n = Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_play)
                .setContentTitle("Medya Paneli hazır")
                .setOngoing(true)
                .build()
            startForeground(NOTIF_ID, n)
        } catch (_: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        rebind(this)
        return START_STICKY
    }
}
