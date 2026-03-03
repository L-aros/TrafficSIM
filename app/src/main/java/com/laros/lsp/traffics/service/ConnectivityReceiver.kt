package com.laros.lsp.traffics.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.laros.lsp.traffics.config.ConfigStore
import com.laros.lsp.traffics.log.LogStore

class ConnectivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context?.applicationContext ?: return
        val config = ConfigStore(appContext).load()
        if (!config.enabled || !config.powerSaveMode) return
        val action = intent?.action ?: "unknown"
        LogStore(appContext).append("powersave: broadcast=$action")
        Log.i("TrafficManager", "powersave broadcast=$action")
        PowerSaveScheduler.triggerOnce(appContext)
    }
}
