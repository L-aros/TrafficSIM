package com.laros.lsp.traffics.model

data class AppConfig(
    val enabled: Boolean = true,
    val powerSaveMode: Boolean = true,
    val hideBackgroundTask: Boolean = false,
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
    val rules: List<SwitchRule> = emptyList()
)
