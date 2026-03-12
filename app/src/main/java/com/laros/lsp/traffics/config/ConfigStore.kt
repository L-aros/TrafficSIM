package com.laros.lsp.traffics.config

import android.content.Context
import com.laros.lsp.traffics.model.AppConfig
import com.laros.lsp.traffics.model.SwitchRule
import org.json.JSONArray
import org.json.JSONObject

class ConfigStore(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppConfig {
        val raw = prefs.getString(KEY_JSON, null) ?: defaultConfigJson()
        return parse(raw)
    }

    fun loadRawJson(): String = prefs.getString(KEY_JSON, null) ?: defaultConfigJson()

    fun save(config: AppConfig) {
        prefs.edit().putString(KEY_JSON, toJson(config)).apply()
    }

    fun saveRawJson(raw: String): Result<Unit> {
        return runCatching {
            val parsed = parse(raw)
            save(parsed)
        }
    }

    fun defaultConfigJson(): String {
        val example = AppConfig(
            rules = listOf(
                SwitchRule(
                    id = "home_wifi",
                    priority = 100,
                    ssid = "MyHomeWiFi",
                    bssid = null,
                    targetSlot = 0
                ),
                SwitchRule(
                    id = "office_ap",
                    priority = 200,
                    ssid = "CorpWiFi",
                    bssid = "AA:BB:CC:DD:EE:FF",
                    targetSlot = 1
                )
            )
        )
        return toJson(example)
    }

    fun parse(raw: String): AppConfig {
        val root = JSONObject(raw)
        val rules = mutableListOf<SwitchRule>()
        val jsonRules = root.optJSONArray("rules") ?: JSONArray()
        for (i in 0 until jsonRules.length()) {
            val r = jsonRules.optJSONObject(i) ?: continue
            val id = r.optString("id", "rule_$i")
            val priority = r.optInt("priority", 0)
            val ssid = if (r.has("ssid") && !r.isNull("ssid")) {
                r.optString("ssid").trim().trim('"').takeIf { it.isNotBlank() }
            } else {
                null
            }
            val bssid = if (r.has("bssid") && !r.isNull("bssid")) {
                r.optString("bssid").trim().replace('-', ':').uppercase().takeIf { it.isNotBlank() }
            } else {
                null
            }
            val targetSlot = r.optInt("targetSlot", 0).coerceIn(0, 1)
            rules += SwitchRule(
                id = id,
                priority = priority,
                ssid = ssid,
                bssid = bssid,
                targetSlot = targetSlot
            )
        }

        return AppConfig(
            enabled = root.optBoolean("enabled", true),
            powerSaveMode = root.optBoolean("powerSaveMode", true),
            screenOnIntervalSec = root.optInt("screenOnIntervalSec", 20).coerceIn(5, 3600),
            screenOffIntervalSec = root.optInt("screenOffIntervalSec", 90).coerceIn(10, 3600),
            cooldownSec = root.optInt("cooldownSec", 90).coerceIn(10, 3600),
            leaveDelaySec = root.optInt("leaveDelaySec", 180).coerceIn(0, 7200),
            leaveMissThreshold = root.optInt("leaveMissThreshold", 3).coerceIn(1, 20),
            revertOnLeave = root.optBoolean("revertOnLeave", true),
            fixedLeaveSlot = if (root.has("fixedLeaveSlot") && !root.isNull("fixedLeaveSlot")) {
                root.optInt("fixedLeaveSlot", -1).takeIf { it in 0..1 }
            } else {
                null
            },
            noWifiSlot = if (root.has("noWifiSlot") && !root.isNull("noWifiSlot")) {
                root.optInt("noWifiSlot", -1).takeIf { it in 0..1 }
            } else {
                null
            },
            noWifiImmediate = root.optBoolean("noWifiImmediate", false),
            logRetentionDays = root.optInt("logRetentionDays", 7).coerceIn(1, 30),
            logMaxMb = root.optInt("logMaxMb", 10).coerceIn(1, 100),
            rules = rules
        )
    }

    fun toJson(config: AppConfig): String {
        val root = JSONObject()
        root.put("enabled", config.enabled)
        root.put("powerSaveMode", config.powerSaveMode)
        root.put("screenOnIntervalSec", config.screenOnIntervalSec)
        root.put("screenOffIntervalSec", config.screenOffIntervalSec)
        root.put("cooldownSec", config.cooldownSec)
        root.put("leaveDelaySec", config.leaveDelaySec)
        root.put("leaveMissThreshold", config.leaveMissThreshold)
        root.put("revertOnLeave", config.revertOnLeave)
        root.put("fixedLeaveSlot", config.fixedLeaveSlot)
        root.put("noWifiSlot", config.noWifiSlot)
        root.put("noWifiImmediate", config.noWifiImmediate)
        root.put("logRetentionDays", config.logRetentionDays)
        root.put("logMaxMb", config.logMaxMb)

        val rules = JSONArray()
        config.rules.forEach { rule ->
            rules.put(
                JSONObject()
                    .put("id", rule.id)
                    .put("priority", rule.priority)
                    .put("ssid", rule.ssid)
                    .put("bssid", rule.bssid)
                    .put("targetSlot", rule.targetSlot)
            )
        }
        root.put("rules", rules)
        return root.toString(2)
    }

    companion object {
        private const val PREFS_NAME = "traffic_manager"
        private const val KEY_JSON = "config_json"
    }
}
