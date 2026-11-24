package com.glasspanel.media

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.media.session.PlaybackState
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast

/**
 * İnce ana ekran widget'ı: kapaktan renk alan zemin, kapak, uygulama simgesi, şarkı, sanatçı, süre çubuğu,
 * karıştır / tekrar ve önceki / oynat-durdur / sonraki. Dokununca çalan müzik uygulaması açılır.
 */
class MediaWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV = "com.glasspanel.media.WIDGET_PREV"
        const val ACTION_PLAY = "com.glasspanel.media.WIDGET_PLAY"
        const val ACTION_NEXT = "com.glasspanel.media.WIDGET_NEXT"
        const val ACTION_SHUFFLE = "com.glasspanel.media.WIDGET_SHUFFLE"
        const val ACTION_REPEAT = "com.glasspanel.media.WIDGET_REPEAT"

        fun refresh(ctx: Context) {
            try {
                val app = ctx.applicationContext
                val mgr = AppWidgetManager.getInstance(app)
                val ids = mgr.getAppWidgetIds(ComponentName(app, MediaWidgetProvider::class.java))
                for (id in ids) {
                    val o = mgr.getAppWidgetOptions(id)
                    val wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
                    val hDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 80)
                    mgr.updateAppWidget(id, build(app, wDp, hDp))
                }
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

        /** Dokununca çalan müzik uygulamasını (yoksa Medya Paneli'ni) açan PendingIntent. */
        private fun openAppIntent(ctx: Context): PendingIntent {
            val session = MediaRepo.controller?.sessionActivity
            if (session != null) return session
            val pkg = MediaRepo.info?.packageName
            val launch = if (pkg != null) ctx.packageManager.getLaunchIntentForPackage(pkg) else null
            val intent = launch ?: Intent(ctx, MainActivity::class.java)
            return PendingIntent.getActivity(
                ctx,
                10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun mix(a: Int, b: Int, t: Float): Int {
            val r = (Color.red(a) * (1 - t) + Color.red(b) * t).toInt()
            val g = (Color.green(a) * (1 - t) + Color.green(b) * t).toInt()
            val bl = (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt()
            return Color.rgb(r, g, bl)
        }

        /** Widget'ın gerçek boyutunda, kapağın renginde yuvarlak köşeli zemin. */
        private fun background(ctx: Context, wDp: Int, hDp: Int, base: Int): Bitmap? {
            return try {
                val d = ctx.resources.displayMetrics.density
                var w = (wDp.coerceIn(120, 700) * d).toInt()
                var h = (hDp.coerceIn(48, 300) * d).toInt()
                val area = w.toLong() * h
                val maxPx = 900_000L
                if (area > maxPx) {
                    val s = Math.sqrt(maxPx.toDouble() / area).toFloat()
                    w = (w * s).toInt()
                    h = (h * s).toInt()
                }
                w = w.coerceAtLeast(2)
                h = h.coerceAtLeast(2)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
                val radius = h * 0.30f
                val p = Paint(Paint.ANTI_ALIAS_FLAG)
                p.shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    withAlpha(mix(base, Color.WHITE, 0.14f), 238),
                    withAlpha(base, 244),
                    Shader.TileMode.CLAMP
                )
                c.drawRoundRect(rect, radius, radius, p)
                val sp = Paint(Paint.ANTI_ALIAS_FLAG)
                sp.style = Paint.Style.STROKE
                sp.strokeWidth = (1f * d).coerceAtLeast(1f)
                sp.color = Color.argb(46, 255, 255, 255)
                val half = sp.strokeWidth / 2f
                c.drawRoundRect(
                    RectF(half, half, w - half, h - half),
                    radius - half, radius - half, sp
                )
                bmp
            } catch (_: Exception) {
                null
            }
        }

        private fun build(ctx: Context, wDp: Int, hDp: Int): RemoteViews {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_media)
            val i = MediaRepo.info
            val art = i?.art

            val bg = background(ctx, wDp, hDp, CoverPalette.base(art))
            if (bg != null) {
                rv.setImageViewBitmap(R.id.w_bg, bg)
            } else {
                rv.setInt(R.id.w_bg, "setBackgroundResource", R.drawable.widget_bg)
            }

            if (i == null) {
                rv.setTextViewText(R.id.w_title, "Çalan bir şey yok")
                rv.setTextViewText(R.id.w_artist, "Bir şarkı başlat")
                rv.setImageViewResource(R.id.w_cover, R.drawable.ic_launcher)
                rv.setViewVisibility(R.id.w_app, View.GONE)
                rv.setImageViewResource(R.id.w_play, R.drawable.ic_play)
                rv.setProgressBar(R.id.w_progress, 1000, 0, false)
            } else {
                rv.setTextViewText(R.id.w_title, i.title.ifBlank { "Bilinmeyen parça" })
                rv.setTextViewText(R.id.w_artist, i.artist.ifBlank { i.appName })

                val cover = art?.let { roundedSquare(it, 220, 0.22f) }
                if (cover != null) {
                    rv.setImageViewBitmap(R.id.w_cover, cover)
                } else {
                    rv.setImageViewResource(R.id.w_cover, R.drawable.ic_launcher)
                }

                val icon = i.appIcon?.let { roundedSquare(it, 80, 0.5f) }
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

                val progress = if (i.durationMs > 0) {
                    ((i.currentPosition().toFloat() / i.durationMs) * 1000f).toInt().coerceIn(0, 1000)
                } else {
                    0
                }
                rv.setProgressBar(R.id.w_progress, 1000, progress, false)
            }

            // karıştır / tekrar yalnızca yeterince geniş widget'ta görünür; kapalıyken soluk, açıkken vurgu renginde
            Controls.sync()
            val wide = wDp >= 340
            val vis = if (wide) View.VISIBLE else View.GONE
            rv.setViewVisibility(R.id.w_shuffle, vis)
            rv.setViewVisibility(R.id.w_repeat, vis)
            val accent = CoverPalette.accent(art)
            val off = Color.argb(110, 255, 255, 255)
            rv.setInt(R.id.w_shuffle, "setColorFilter", if (Controls.shuffle) accent else off)
            rv.setImageViewResource(
                R.id.w_repeat,
                if (Controls.repeat == 2) R.drawable.ic_repeat_one else R.drawable.ic_repeat
            )
            rv.setInt(R.id.w_repeat, "setColorFilter", if (Controls.repeat == 0) off else accent)

            rv.setOnClickPendingIntent(R.id.w_prev, actionIntent(ctx, ACTION_PREV, 1))
            rv.setOnClickPendingIntent(R.id.w_play, actionIntent(ctx, ACTION_PLAY, 2))
            rv.setOnClickPendingIntent(R.id.w_next, actionIntent(ctx, ACTION_NEXT, 3))
            rv.setOnClickPendingIntent(R.id.w_shuffle, actionIntent(ctx, ACTION_SHUFFLE, 4))
            rv.setOnClickPendingIntent(R.id.w_repeat, actionIntent(ctx, ACTION_REPEAT, 5))

            val open = openAppIntent(ctx)
            rv.setOnClickPendingIntent(R.id.w_cover, open)
            rv.setOnClickPendingIntent(R.id.w_info, open)
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action != ACTION_PREV && action != ACTION_PLAY && action != ACTION_NEXT &&
            action != ACTION_SHUFFLE && action != ACTION_REPEAT
        ) return

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
            ACTION_SHUFFLE -> {
                Controls.tapShuffle(context)
                refresh(context)
            }
            ACTION_REPEAT -> {
                Controls.tapRepeat(context)
                refresh(context)
            }
            else -> if (MediaRepo.info?.isPlaying == true) tc.pause() else tc.play()
        }
    }
}
