package com.laros.lsp.traffics.core

import com.laros.lsp.traffics.model.SwitchRule

object RuleMatcher {
    fun findBest(snapshot: WifiSnapshot?, rules: List<SwitchRule>): SwitchRule? {
        if (snapshot == null) return null
        return rules
            .asSequence()
            .filter { isMatch(snapshot, it) }
            .sortedByDescending { it.priority }
            .firstOrNull()
    }

    private fun isMatch(snapshot: WifiSnapshot, rule: SwitchRule): Boolean {
        val ruleSsid = normalizeSsid(rule.ssid)
        val snapSsid = normalizeSsid(snapshot.ssid)
        val ruleBssid = normalizeBssid(rule.bssid)
        val snapBssid = normalizeBssid(snapshot.bssid)

        val ssidOk = ruleSsid == null || ruleSsid.equals(snapSsid, ignoreCase = true)
        val bssidOk = ruleBssid == null || ruleBssid.equals(snapBssid, ignoreCase = true)
        return ssidOk && bssidOk
    }

    private fun normalizeSsid(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.trim().trim('"')
        return normalized.takeIf { it.isNotBlank() }
    }

    private fun normalizeBssid(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.trim().replace('-', ':').uppercase()
        return normalized.takeIf { it.isNotBlank() }
    }
}
