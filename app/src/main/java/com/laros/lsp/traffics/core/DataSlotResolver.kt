package com.laros.lsp.traffics.core

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager

class DataSlotResolver(private val context: Context) {
    @SuppressLint("MissingPermission")
    fun currentDataSlot(): Int? {
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return null
        val mgr = context.getSystemService(SubscriptionManager::class.java) ?: return null
        val info = mgr.getActiveSubscriptionInfo(subId) ?: return null
        return info.simSlotIndex.takeIf { it >= 0 }
    }

    @SuppressLint("MissingPermission")
    fun subIdForSlot(slot: Int): Int? {
        val mgr = context.getSystemService(SubscriptionManager::class.java) ?: return null
        val list = mgr.activeSubscriptionInfoList ?: return null
        return list.firstOrNull { it.simSlotIndex == slot }?.subscriptionId
    }
}
