package com.laros.lsp.traffics.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class FocusSummaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != SwitchEventNotifier.ACTION_POST_SUMMARY_FOCUS_NOTIFICATION) {
            Log.d(TAG, "ignore unrelated focus transition action=${intent?.action}")
            return
        }
        Log.d(TAG, "handle focus summary transition")
        SwitchEventNotifier(context).notifySummaryFromIntent(intent)
    }

    companion object {
        private const val TAG = "TmFocusSummary"
    }
}
