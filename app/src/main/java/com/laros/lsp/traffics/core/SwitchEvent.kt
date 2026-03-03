package com.laros.lsp.traffics.core

data class SwitchEvent(
    val success: Boolean,
    val targetSlot: Int,
    val reason: String,
    val transport: String,
    val message: String
)
