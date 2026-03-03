package com.laros.lsp.traffics.core

import android.content.Context
import com.laros.lsp.traffics.model.AppConfig

interface SwitchTransport {
    val name: String

    fun switchDataSlot(
        context: Context,
        targetSlot: Int,
        targetSubId: Int?,
        reason: String,
        config: AppConfig
    ): SwitchResult
}
