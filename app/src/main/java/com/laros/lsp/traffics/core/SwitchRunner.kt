package com.laros.lsp.traffics.core

import android.content.Context
import android.util.Log
import com.laros.lsp.traffics.config.ConfigStore
import com.laros.lsp.traffics.config.SwitchStateStore
import com.laros.lsp.traffics.log.LogStore

class SwitchRunner(private val context: Context) {
    private val configStore = ConfigStore(context)
    private val logStore = LogStore(context)
    private val notifier = SwitchEventNotifier(context)

    fun runOnce(source: String) {
        val config = configStore.load()
        if (!config.enabled) {
            logStore.append("oneshot: skipped, config enabled=false source=$source")
            Log.i(TAG, "oneshot skipped: enabled=false source=$source")
            return
        }
        Log.i(TAG, "oneshot run: source=$source")
        val snapshot = WifiSnapshotProvider(context).current()
        val matchedRule = RuleMatcher.findBest(snapshot, config.rules)
        val currentSlot = DataSlotResolver(context).currentDataSlot()
        logStore.append(
            "oneshot: source=$source rules=${config.rules.size} currentSlot=$currentSlot ssid=${snapshot?.ssid} bssid=${snapshot?.bssid} matched=${matchedRule?.id ?: "none"}"
        )
        Log.i(
            TAG,
            "oneshot state: slot=$currentSlot ssid=${snapshot?.ssid} bssid=${snapshot?.bssid} matched=${matchedRule?.id ?: "none"}"
        )
        if (snapshot != null && matchedRule == null && config.rules.isNotEmpty()) {
            val rulePreview = config.rules.joinToString(" | ") {
                "id=${it.id},ssid=${it.ssid},bssid=${it.bssid},slot=${it.targetSlot}"
            }
            logStore.append("oneshot: no-match-rules $rulePreview")
        }
        val coordinator = SimSwitchCoordinator(
            context = context,
            resolver = DataSlotResolver(context),
            transportChain = SwitchTransportChain(
                listOf(
                    XposedBroadcastTransport()
                )
            ),
            logStore = logStore,
            stateStore = SwitchStateStore(context),
            onSwitchEvent = { notifier.notify(it) }
        )
        coordinator.onTick(config, matchedRule, snapshot)
        logStore.trim(config)
    }

    companion object {
        private const val TAG = "TrafficManager"
    }
}
