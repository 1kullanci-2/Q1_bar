package com.glasspanel.media

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/** İzin durumları ve ilgili ayar ekranlarını açan kısayollar (Ana ekran ve sihirbaz ortak kullanır). */
object Perms {

    fun notificationAccess(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { ComponentName.unflattenFromString(it)?.packageName == ctx.packageName }
    }

    fun overlay(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(ctx)

    fun battery(ctx: Context): Boolean =
        ctx.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(ctx.packageName) ?: false

    /** Android 13+ bildirim gönderme izni; eski sürümlerde gerekmez. */
    fun postNotificationsNeeded(): Boolean = Build.VERSION.SDK_INT >= 33

    fun postNotifications(ctx: Context): Boolean =
        !postNotificationsNeeded() || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun isHuawei(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        return m.contains("huawei") || m.contains("honor")
    }

    fun openNotificationAccess(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openOverlay(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
    }

    fun openBattery(ctx: Context) {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
            )
        } catch (_: Exception) {
            try {
                ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
            }
        }
    }

    /** Huawei'nin "Uygulama başlatma" ekranını açmayı dener, olmazsa uygulama ayrıntılarına gider. */
    fun openStartupSettings(ctx: Context) {
        val targets = listOf(
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
        )
        for (c in targets) {
            try {
                ctx.startActivity(Intent().setComponent(c))
                return
            } catch (_: Exception) {
            }
        }
        try {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
        } catch (_: Exception) {
        }
    }
}
