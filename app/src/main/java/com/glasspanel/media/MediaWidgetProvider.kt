package com.glasspanel.media

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.widget.RemoteViews

/** Ana ekran widget'ı: kapak, uygulama simgesi, şarkı, sanatçı ve önceki / oynat-durdur / sonraki. */
class MediaWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV = "com.glasspanel.media.WIDGET_PREV"
        const val ACTION_PLAY = "com.glasspanel.media.WIDGET_PLAY"
        const val ACTION_NEXT = "com.glasspanel.media.WIDGET_NEXT"

        fun refresh(ctx: Context) {
            try {
                val app = ctx.applicationContext
                val mgr = AppWidgetManager.getInstance(app)
                val ids = mgr.getAppWidgetIds(ComponentName(app, MediaWidgetProvider::class.java))
                if (ids.isEmpty()) return
                mgr.updateAppWidget(ids, build(app))
            } catch (_: Exception) {
            }
        }

        private fun actionIntent(ctx: Context, action: String, code: Int): PendingIntent =
            PendingIntent.getBroadcast(
                ctx,
                code,
                Intent(ctx, MediaWidgetProvider::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun build(ctx: Context): RemoteViews {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_media)
            val i = MediaRepo.info

            if (i == null) {
                rv.setTextViewText(R.id.w_title, "Çalan bir şey yok")
                rv.setTextViewText(R.id.w_artist, "Bir şarkı başlat")
                rv.setImageViewResource(R.id.w_cover, R.drawable.ic_launcher)
                rv.setViewVisibility(R.id.w_app, View.GONE)
                rv.setImageViewResource(R.id.w_play, R.drawable.ic_play)
            } else {
                rv.setTextViewText(R.id.w_title, i.title.ifBlank { "Bilinmeyen parça" })
                rv.setTextViewText(R.id.w_artist, i.artist.ifBlank { i.appName })

                val cover = i.art?.let { roundedSquare(it, 240, 0.2f) }
                if (cover != null) {
                    rv.setImageViewBitmap(R.id.w_cover, cover)
                } else {
                    rv.setImageViewResource(R.id.w_cover, R.drawable.ic_launcher)
                }

                val icon = i.appIcon?.let { roundedSquare(it, 96, 0.5f) }
                if (icon != null) {
                    rv.setImageViewBitmap(R.id.w_app, icon)
                    rv.setViewVisibility(R.id.w_app, View.VISIBLE)
                } else {
                    rv.setViewVisibility(R.id.w_app, View.GONE)
                }

                rv.setImageViewResource(
                    R.id.w_play,
                    if (i.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )
            }

            rv.setOnClickPendingIntent(R.id.w_prev, actionIntent(ctx, ACTION_PREV, 1))
            rv.setOnClickPendingIntent(R.id.w_play, actionIntent(ctx, ACTION_PLAY, 2))
            rv.setOnClickPendingIntent(R.id.w_next, actionIntent(ctx, ACTION_NEXT, 3))
            rv.setOnClickPendingIntent(
                R.id.w_cover,
                PendingIntent.getActivity(
                    ctx,
                    4,
                    Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            return rv
        }

        /** Ortadan kare keser, köşelerini yuvarlar (fraction 0.5 = daire). */
        private fun roundedSquare(src: Bitmap, size: Int, fraction: Float): Bitmap? {
            return try {
                val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val c = Canvas(out)
                val s = minOf(src.width, src.height)
                val l = (src.width - s) / 2
                val t = (src.height - s) / 2
                val dst = RectF(0f, 0f, size.toFloat(), size.toFloat())
                val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                c.drawRoundRect(dst, size * fraction, size * fraction, p)
                p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                c.drawBitmap(src, Rect(l, t, l + s, t + s), dst, p)
                out
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action != ACTION_PREV && action != ACTION_PLAY && action != ACTION_NEXT) return

        val tc = MediaRepo.controller?.transportControls
        if (tc == null) {
            // Uygulama süreci uyuyor olabilir: servisleri canlandır
            KeepAliveService.start(context)
            KeepAliveService.rebind(context)
            return
        }
        when (action) {
            ACTION_PREV -> tc.skipToPrevious()
            ACTION_NEXT -> tc.skipToNext()
            else -> if (MediaRepo.info?.isPlaying == true) tc.pause() else tc.play()
        }
    }
}
