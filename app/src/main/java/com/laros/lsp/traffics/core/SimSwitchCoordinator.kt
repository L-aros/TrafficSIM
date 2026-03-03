package com.laros.lsp.traffics.core

import android.content.Context
import com.laros.lsp.traffics.config.SwitchStateStore
import com.laros.lsp.traffics.log.LogStore
import com.laros.lsp.traffics.model.AppConfig
import com.laros.lsp.traffics.model.SwitchRule

class SimSwitchCoordinator(
    private val context: Context,
    private val resolver: DataSlotResolver,
    private val transportChain: SwitchTransportChain,
    private val logStore: LogStore,
    private val stateStore: SwitchStateStore? = null,
    private val onSwitchEvent: ((SwitchEvent) -> Unit)? = null
) {
    private var activeRuleId: String? = null
    private var lastMatchedAtMs: Long = 0L
    private var lastSwitchAtMs: Long = stateStore?.getLastSwitchAtMs() ?: 0L
    private var previousSlotBeforeRule: Int? = null
    private var consecutiveNoMatchTicks: Int = 0
    private var consecutiveNoWifiTicks: Int = 0
    private var lastNoWifiAtMs: Long = 0L

    fun onTick(
        config: AppConfig,
        matchedRule: SwitchRule?,
        snapshot: WifiSnapshot?,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val noWifi = snapshot == null || (snapshot.ssid == null && snapshot.bssid == null)
        if (matchedRule != null) {
            resetNoWifiState()
            consecutiveNoMatchTicks = 0
            lastMatchedAtMs = nowMs
            handleRuleMatch(config, matchedRule, nowMs)
            return
        }
        if (noWifi) {
            consecutiveNoMatchTicks = 0
            handleNoWifi(config, nowMs)
            return
        }
        resetNoWifiState()
        consecutiveNoMatchTicks += 1
        handleNoRuleMatch(config, nowMs)
    }

    private fun handleRuleMatch(config: AppConfig, rule: SwitchRule, nowMs: Long) {
        val currentSlot = resolver.currentDataSlot()
        if (activeRuleId != rule.id && previousSlotBeforeRule == null) {
            previousSlotBeforeRule = currentSlot
        }
        activeRuleId = rule.id

        if (currentSlot == rule.targetSlot) {
            return
        }
        if (!withinCooldown(config, nowMs)) {
            switchTo(config, rule.targetSlot, "rule=${rule.id}")
        }
    }

    private fun handleNoRuleMatch(config: AppConfig, nowMs: Long) {
        val isInRuleSession = activeRuleId != null
        if (!isInRuleSession) return

        val enoughMiss = consecutiveNoMatchTicks >= config.leaveMissThreshold
        if (!enoughMiss) return

        val shouldLeave = nowMs - lastMatchedAtMs >= config.leaveDelaySec * 1000L
        if (!shouldLeave) return

        val target = when {
            config.revertOnLeave && previousSlotBeforeRule != null -> previousSlotBeforeRule
            config.fixedLeaveSlot != null -> config.fixedLeaveSlot
            else -> null
        }

        if (target != null && !withinCooldown(config, nowMs)) {
            switchTo(config, target, "leave_rule=$activeRuleId")
        }

        activeRuleId = null
        previousSlotBeforeRule = null
        lastMatchedAtMs = 0L
        consecutiveNoMatchTicks = 0
    }

    private fun handleNoWifi(config: AppConfig, nowMs: Long) {
        val target = config.noWifiSlot ?: return
        if (lastNoWifiAtMs == 0L) {
            lastNoWifiAtMs = nowMs
        }
        consecutiveNoWifiTicks += 1
        if (!config.noWifiImmediate) {
            val enoughMiss = consecutiveNoWifiTicks >= config.leaveMissThreshold
            val shouldSwitch = nowMs - lastNoWifiAtMs >= config.leaveDelaySec * 1000L
            if (!enoughMiss || !shouldSwitch) return
        }
        if (!config.noWifiImmediate && withinCooldown(config, nowMs)) return
        switchTo(config, target, "no_wifi")
        resetRuleSession()
    }

    private fun switchTo(config: AppConfig, targetSlot: Int, reason: String) {
        val subId = resolver.subIdForSlot(targetSlot)
        val result = transportChain.switchDataSlot(
            context = context,
            targetSlot = targetSlot,
            targetSubId = subId,
            reason = reason,
            config = config
        )
        if (result.success) {
            lastSwitchAtMs = System.currentTimeMillis()
            stateStore?.setLastSwitchAtMs(lastSwitchAtMs)
            logStore.append("switch success slot=$targetSlot transport=${result.transport} reason=$reason msg=${result.message}")
        } else {
            logStore.append("switch failed slot=$targetSlot transport=${result.transport} reason=$reason msg=${result.message}")
        }
        onSwitchEvent?.invoke(
            SwitchEvent(
                success = result.success,
                targetSlot = targetSlot,
                reason = reason,
                transport = result.transport,
                message = result.message
            )
        )
    }

    private fun withinCooldown(config: AppConfig, nowMs: Long): Boolean {
        return nowMs - lastSwitchAtMs < config.cooldownSec * 1000L
    }

    private fun resetNoWifiState() {
        consecutiveNoWifiTicks = 0
        lastNoWifiAtMs = 0L
    }

    private fun resetRuleSession() {
        activeRuleId = null
        previousSlotBeforeRule = null
        lastMatchedAtMs = 0L
        consecutiveNoMatchTicks = 0
    }
}
