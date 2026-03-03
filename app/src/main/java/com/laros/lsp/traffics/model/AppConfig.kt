package com.laros.lsp.traffics.model

data class AppConfig(
    val enabled: Boolean = true,
    val powerSaveMode: Boolean = true,
    val screenOnIntervalSec: Int = 20,
    val screenOffIntervalSec: Int = 90,
    val cooldownSec: Int = 90,
    val leaveDelaySec: Int = 180,
    val leaveMissThreshold: Int = 3,
    val revertOnLeave: Boolean = true,
    val fixedLeaveSlot: Int? = null,
    val noWifiSlot: Int? = null,
    val noWifiImmediate: Boolean = false,
    val logRetentionDays: Int = 7,
    val logMaxMb: Int = 10,
    val rootCommandTemplates: List<String> = listOf(
        "cmd phone set-default-data-subscription {subId}",
        "cmd phone set-default-data-sub-id {subId}",
        "cmd phone set-data-subscription {subId}",
        "cmd phone set-preferred-data-subscription {subId}",
        "settings put global multi_sim_data_call {slot}"
    ),
    val rules: List<SwitchRule> = emptyList()
)
