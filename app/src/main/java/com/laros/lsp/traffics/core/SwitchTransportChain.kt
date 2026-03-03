package com.laros.lsp.traffics.core

import android.content.Context
import com.laros.lsp.traffics.model.AppConfig

class SwitchTransportChain(private val transports: List<SwitchTransport>) {
    fun switchDataSlot(
        context: Context,
        targetSlot: Int,
        targetSubId: Int?,
        reason: String,
        config: AppConfig
    ): SwitchResult {
        var last = SwitchResult(false, "none", "No transport configured")
        val trace = mutableListOf<String>()
        for (transport in transports) {
            last = transport.switchDataSlot(context, targetSlot, targetSubId, reason, config)
            trace += "${transport.name}:${if (last.success) "ok" else "fail"}:${last.message}"
            if (last.success) {
                return last.copy(message = "${last.message} | trace=${trace.joinToString(" || ")}")
            }
        }
        return last.copy(message = "all_failed trace=${trace.joinToString(" || ")}")
    }
}
