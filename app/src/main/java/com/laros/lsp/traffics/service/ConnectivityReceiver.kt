package com.laros.lsp.traffics.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.laros.lsp.traffics.config.ConfigStore
import com.laros.lsp.traffics.core.WifiSnapshot
import com.laros.lsp.traffics.core.WifiSnapshotCacheStore
import com.laros.lsp.traffics.log.LogStore

class ConnectivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context?.applicationContext ?: return
        val config = ConfigStore(appContext).load()
        val logStore = LogStore(appContext)
        val action = intent?.action ?: "unknown"
        updateWifiSnapshotCache(appContext, intent, logStore)
        if (!config.enabled) return

        if (config.powerSaveMode) {
            logStore.append("powersave: broadcast=$action")
            Log.i("TrafficManager", "powersave broadcast=$action")
            PowerSaveScheduler.triggerOnce(appContext)
            return
        }

        logStore.append("service: broadcast=$action")
        Log.i("TrafficManager", "service broadcast=$action")
        AutoSwitchService.requestRunOnce(appContext)
    }

    @SuppressLint("MissingPermission")
    private fun updateWifiSnapshotCache(
        appContext: Context,
        intent: Intent?,
        logStore: LogStore
    ) {
        val action = intent?.action ?: return
        val cacheStore = WifiSnapshotCacheStore(appContext)

        if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
            val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
            if (wifiState == WifiManager.WIFI_STATE_DISABLED || wifiState == WifiManager.WIFI_STATE_DISABLING) {
                cacheStore.clear()
                logStore.append("wifi_cache: cleared action=$action wifiState=$wifiState")
                return
            }
        }

        val networkInfo = readNetworkInfo(intent)
        if (networkInfo != null && !networkInfo.isConnected) {
            cacheStore.clear()
            logStore.append("wifi_cache: cleared action=$action connected=false")
            return
        }

        val wifiInfo = readWifiInfo(intent)
        val snapshot = WifiSnapshot(
            ssid = normalizeSsid(wifiInfo?.ssid),
            bssid = normalizeBssid(wifiInfo?.bssid ?: intent.getStringExtra(WifiManager.EXTRA_BSSID))
        )
        if (snapshot.ssid == null && snapshot.bssid == null) return

        cacheStore.save(snapshot)
        logStore.append("wifi_cache: saved action=$action ssid=${snapshot.ssid} bssid=${snapshot.bssid}")
    }

    private fun normalizeSsid(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim().trim('"')
        return value.takeUnless { it.equals("<unknown ssid>", ignoreCase = true) || it.isBlank() }
    }

    private fun normalizeBssid(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim().replace('-', ':').uppercase()
        return value.takeUnless { it == "02:00:00:00:00:00" || it.isBlank() }
    }

    @Suppress("DEPRECATION")
    private fun readWifiInfo(intent: Intent): WifiInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO, WifiInfo::class.java)
        } else {
            intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO)
        }
    }

    @Suppress("DEPRECATION")
    private fun readNetworkInfo(intent: Intent): NetworkInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
        } else {
            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
        }
    }
}
