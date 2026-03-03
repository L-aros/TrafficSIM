package com.laros.lsp.traffics.config

import android.content.Context

class SwitchStateStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLastSwitchAtMs(): Long = prefs.getLong(KEY_LAST_SWITCH_AT, 0L)

    fun setLastSwitchAtMs(value: Long) {
        prefs.edit().putLong(KEY_LAST_SWITCH_AT, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "traffic_manager_state"
        private const val KEY_LAST_SWITCH_AT = "last_switch_at_ms"
    }
}
