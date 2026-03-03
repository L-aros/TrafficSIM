package com.laros.lsp.traffics.model

data class SwitchRule(
    val id: String,
    val priority: Int,
    val ssid: String?,
    val bssid: String?,
    val targetSlot: Int
)
