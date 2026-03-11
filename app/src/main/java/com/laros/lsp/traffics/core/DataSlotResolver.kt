package com.laros.lsp.traffics.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

class DataSlotResolver(private val context: Context) {
    @SuppressLint("MissingPermission")
    fun currentDataSlot(): Int? {
        if (!canReadPhoneState()) return null
        return runCatching {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            if (!SubscriptionManager.isValidSubscriptionId(subId)) return null
            val mgr = context.getSystemService(SubscriptionManager::class.java) ?: return null
            val info = mgr.getActiveSubscriptionInfo(subId) ?: return null
            info.simSlotIndex.takeIf { it >= 0 }
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    fun subIdForSlot(slot: Int): Int? {
        if (!canReadPhoneState()) return null
        return runCatching {
            val mgr = context.getSystemService(SubscriptionManager::class.java) ?: return null
            val list = mgr.activeSubscriptionInfoList ?: return null
            list.firstOrNull { it.simSlotIndex == slot }?.subscriptionId
        }.getOrNull()
    }

    private fun canReadPhoneState(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
    }
}
