package com.laros.lsp.traffics.core

import android.content.Context
import com.laros.lsp.traffics.model.AppConfig
import java.util.concurrent.TimeUnit

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
            val verify = if (last.success) verifySlotApplied(context, targetSlot) else VerifyOutcome(VerifyStatus.UNKNOWN, "none", null)
            val verifyLabel = verify.status.name.lowercase()
            val resultLabel = if (last.success) "ok" else "fail"
            val verifyDetail = "source=${verify.source} observed=${verify.observed ?: "null"}"
            trace += "${transport.name}:$resultLabel:verify=$verifyLabel:$verifyDetail:${last.message}"
            if (last.success) {
                if (verify.status == VerifyStatus.NOT_VERIFIED) {
                    last = last.copy(
                        success = false,
                        message = "verify_failed transport=${transport.name} $verifyDetail ${last.message}"
                    )
                    continue
                }
                return last.copy(
                    message = "${last.message} | verify=$verifyLabel $verifyDetail | trace=${trace.joinToString(" || ")}"
                )
            }
        }
        return last.copy(message = "all_failed trace=${trace.joinToString(" || ")}")
    }

    private enum class VerifyStatus {
        VERIFIED,
        NOT_VERIFIED,
        UNKNOWN
    }

    private data class VerifyOutcome(
        val status: VerifyStatus,
        val source: String,
        val observed: Int?
    )

    private fun verifySlotApplied(context: Context, targetSlot: Int, timeoutMs: Long = 2000L): VerifyOutcome {
        val resolver = DataSlotResolver(context)
        val start = System.currentTimeMillis()
        var lastSeen: Int? = null
        while (System.currentTimeMillis() - start < timeoutMs) {
            val current = resolver.currentDataSlot()
            if (current != null) {
                lastSeen = current
                if (current == targetSlot) return VerifyOutcome(VerifyStatus.VERIFIED, "subscription_manager", current)
            }
            TimeUnit.MILLISECONDS.sleep(250)
        }
        return when {
            lastSeen == null -> VerifyOutcome(VerifyStatus.UNKNOWN, "subscription_manager", null)
            else -> VerifyOutcome(VerifyStatus.NOT_VERIFIED, "subscription_manager", lastSeen)
        }
    }
}
