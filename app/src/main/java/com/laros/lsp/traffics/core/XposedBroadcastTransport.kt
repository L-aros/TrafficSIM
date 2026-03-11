package com.laros.lsp.traffics.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.laros.lsp.traffics.model.AppConfig
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class XposedBroadcastTransport : SwitchTransport {
    override val name: String = "xposed_broadcast"

    override fun switchDataSlot(
        context: Context,
        targetSlot: Int,
        targetSubId: Int?,
        reason: String,
        config: AppConfig
    ): SwitchResult {
        val traces = mutableListOf<String>()
        for (phonePackage in BridgeContract.PHONE_PACKAGES) {
            val r = attemptWithPackage(
                context = context,
                phonePackage = phonePackage,
                targetSlot = targetSlot,
                targetSubId = targetSubId,
                reason = reason
            )
            traces += "${phonePackage}:${if (r.success) "ok" else "fail"}:${r.message}"
            if (r.success) {
                return r.copy(message = "${r.message} | ${traces.joinToString(" || ")}")
            }
        }
        return SwitchResult(false, name, "no_bridge_response ${traces.joinToString(" || ")}")
    }

    private fun attemptWithPackage(
        context: Context,
        phonePackage: String,
        targetSlot: Int,
        targetSubId: Int?,
        reason: String
    ): SwitchResult {
        val token = UUID.randomUUID().toString()
        val latch = CountDownLatch(1)
        val result = AtomicReference(
            SwitchResult(
                success = false,
                transport = name,
                message = "No result"
            )
        )

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != BridgeContract.ACTION_SWITCH_RESULT) return
                if (intent.getStringExtra(BridgeContract.EXTRA_TOKEN) != token) return
                val source = intent.getStringExtra(BridgeContract.EXTRA_SOURCE_PACKAGE)
                val message = intent.getStringExtra(BridgeContract.EXTRA_MESSAGE) ?: "Unknown"
                result.set(
                    SwitchResult(
                        success = intent.getBooleanExtra(BridgeContract.EXTRA_SUCCESS, false),
                        transport = name,
                        message = "source=$source msg=$message"
                    )
                )
                latch.countDown()
            }
        }

        val filter = IntentFilter(BridgeContract.ACTION_SWITCH_RESULT)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        try {
            val req = Intent(BridgeContract.ACTION_SWITCH_DATA).apply {
                `package` = phonePackage
                putExtra(BridgeContract.EXTRA_TOKEN, token)
                putExtra(BridgeContract.EXTRA_TARGET_SLOT, targetSlot)
                putExtra(BridgeContract.EXTRA_TARGET_SUB_ID, targetSubId ?: -1)
                putExtra(BridgeContract.EXTRA_REASON, reason)
                putExtra(BridgeContract.EXTRA_REQUEST_PACKAGE, context.packageName)
            }
            context.sendBroadcast(req)

            val ok = latch.await(1500, TimeUnit.MILLISECONDS)
            return if (ok) {
                result.get()
            } else {
                SwitchResult(false, name, "Timed out waiting bridge from $phonePackage")
            }
        } finally {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
}
