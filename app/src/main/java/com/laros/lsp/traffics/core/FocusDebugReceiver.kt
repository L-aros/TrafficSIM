package com.laros.lsp.traffics.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import java.util.Locale

class FocusDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != SwitchEventNotifier.ACTION_DEBUG_FOCUS_NOTIFICATION) return
        if (!isDebuggable(context)) {
            Log.w(TAG, "ignore debug focus broadcast on non-debuggable build")
            return
        }

        val status = parseStatus(intent.getStringExtra(SwitchEventNotifier.EXTRA_DEBUG_STATUS))
        val targetSlot = intent.getIntExtra(SwitchEventNotifier.EXTRA_DEBUG_TARGET_SLOT, 0).coerceIn(0, 1)
        val reason = intent.getStringExtra(SwitchEventNotifier.EXTRA_DEBUG_REASON).orEmpty().ifBlank {
            "no_wifi"
        }
        val transport = intent.getStringExtra(SwitchEventNotifier.EXTRA_DEBUG_TRANSPORT).orEmpty().ifBlank {
            "debug"
        }
        val message = intent.getStringExtra(SwitchEventNotifier.EXTRA_DEBUG_MESSAGE).orEmpty().ifBlank {
            when (status) {
                XiaomiFocusNotificationCompat.FocusStatus.SUCCESS -> "debug_success"
                XiaomiFocusNotificationCompat.FocusStatus.VERIFY_FAILED -> "verify_failed"
                XiaomiFocusNotificationCompat.FocusStatus.FAILED -> "debug_failed"
            }
        }

        Log.d(TAG, "trigger debug focus notification status=$status targetSlot=$targetSlot")
        SwitchEventNotifier(context).notify(
            SwitchEvent(
                success = status == XiaomiFocusNotificationCompat.FocusStatus.SUCCESS,
                targetSlot = targetSlot,
                reason = reason,
                transport = transport,
                message = message
            )
        )
    }

    private fun isDebuggable(context: Context): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun parseStatus(raw: String?): XiaomiFocusNotificationCompat.FocusStatus {
        return when (raw?.trim()?.uppercase(Locale.ROOT)) {
            "SUCCESS" -> XiaomiFocusNotificationCompat.FocusStatus.SUCCESS
            "VERIFY_FAILED", "VERIFY" -> XiaomiFocusNotificationCompat.FocusStatus.VERIFY_FAILED
            else -> XiaomiFocusNotificationCompat.FocusStatus.FAILED
        }
    }

    companion object {
        private const val TAG = "TmFocusDebug"
    }
}
