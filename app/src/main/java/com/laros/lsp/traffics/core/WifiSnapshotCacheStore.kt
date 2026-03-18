package com.laros.lsp.traffics.core

import android.content.Context

class WifiSnapshotCacheStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFresh(maxAgeMs: Long = CACHE_TTL_MS): WifiSnapshot? {
        val atMs = prefs.getLong(KEY_LAST_SEEN_AT, 0L)
        if (atMs <= 0L) return null
        if (System.currentTimeMillis() - atMs > maxAgeMs) return null

        val ssid = prefs.getString(KEY_SSID, null)?.takeIf { it.isNotBlank() }
        val bssid = prefs.getString(KEY_BSSID, null)?.takeIf { it.isNotBlank() }
        if (ssid == null && bssid == null) return null
        return WifiSnapshot(ssid = ssid, bssid = bssid)
    }

    fun save(snapshot: WifiSnapshot) {
        prefs.edit()
            .putString(KEY_SSID, snapshot.ssid)
            .putString(KEY_BSSID, snapshot.bssid)
            .putLong(KEY_LAST_SEEN_AT, System.currentTimeMillis())
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_SSID)
            .remove(KEY_BSSID)
            .remove(KEY_LAST_SEEN_AT)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "traffic_manager_wifi_snapshot_cache"
        private const val KEY_SSID = "last_wifi_ssid"
        private const val KEY_BSSID = "last_wifi_bssid"
        private const val KEY_LAST_SEEN_AT = "last_wifi_seen_at_ms"
        private const val CACHE_TTL_MS = 3L * 60L * 1000L
    }
}
