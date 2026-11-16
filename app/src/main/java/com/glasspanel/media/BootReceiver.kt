package com.glasspanel.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Telefon açılınca veya uygulama güncellenince dinleyiciyi ve ön plan servisini canlandırır. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        KeepAliveService.start(context)
        KeepAliveService.rebind(context)
    }
}
