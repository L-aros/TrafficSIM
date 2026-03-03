package com.laros.lsp.traffics.core

import android.content.Context
import com.laros.lsp.traffics.model.AppConfig
import java.util.concurrent.TimeUnit

class RootCommandTransport : SwitchTransport {
    override val name: String = "root_cmd"

    override fun switchDataSlot(
        context: Context,
        targetSlot: Int,
        targetSubId: Int?,
        reason: String,
        config: AppConfig
    ): SwitchResult {
        val templates = if (config.rootCommandTemplates.isNotEmpty()) {
            config.rootCommandTemplates
        } else {
            listOf(
                "cmd phone set-default-data-subscription {subId}",
                "cmd phone set-default-data-sub-id {subId}",
                "cmd phone set-data-subscription {subId}",
                "cmd phone set-preferred-data-subscription {subId}",
                "settings put global multi_sim_data_call {slot}"
            )
        }

        val subIdText = (targetSubId ?: -1).toString()
        val commands = templates.map {
            it.replace("{slot}", targetSlot.toString()).replace("{subId}", subIdText)
        }

        val traces = mutableListOf<String>()
        for (command in commands) {
            if (isRotationLockCommand(command)) {
                traces += "[blocked] $command -> reason=rotation_lock"
                continue
            }
            val output = runAsRoot(command)
            if (output.startsWith("ok:")) {
                val verified = waitUntilSlotApplied(context, targetSlot, timeoutMs = 3500L)
                traces += "[$command] -> $output verify=$verified"
                if (verified) {
                    return SwitchResult(true, name, traces.joinToString(" || "))
                }
            } else {
                traces += "[$command] -> $output"
            }
        }
        return SwitchResult(false, name, traces.joinToString(" || "))
    }

    private fun isRotationLockCommand(command: String): Boolean {
        val normalized = command.lowercase()
        if (!normalized.contains("settings")) return false
        if (!normalized.contains(" put ")) return false
        return normalized.contains("accelerometer_rotation") ||
            normalized.contains("user_rotation")
    }

    private fun runAsRoot(command: String): String {
        val suBins = listOf(
            "su",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/ap/bin/su",
            "/debug_ramdisk/su"
        )
        val traces = mutableListOf<String>()
        for (suBin in suBins) {
            val output = runSingleSu(suBin, command)
            traces += "$suBin=>$output"
            if (output.startsWith("ok:")) return output
        }
        return "err: ${traces.joinToString(" | ")}"
    }

    private fun runSingleSu(suBin: String, command: String): String {
        return runCatching {
            val process = ProcessBuilder(suBin, "-c", command)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(8, TimeUnit.SECONDS)
            val out = process.inputStream.bufferedReader().readText().trim()
            if (!finished) {
                process.destroyForcibly()
                return "err: timeout"
            }
            val code = process.exitValue()
            if (code == 0) "ok: $out" else "err($code): $out"
        }.getOrElse { "err: ${it.javaClass.simpleName}: ${it.message}" }
    }

    private fun waitUntilSlotApplied(context: Context, targetSlot: Int, timeoutMs: Long): Boolean {
        val resolver = DataSlotResolver(context)
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val current = resolver.currentDataSlot()
            if (current == targetSlot) return true
            if (current == null) {
                val globalSlot = readGlobalDataSlotByRoot()
                if (globalSlot == targetSlot) return true
            }
            Thread.sleep(250)
        }
        return false
    }

    private fun readGlobalDataSlotByRoot(): Int? {
        val output = runAsRoot("settings get global multi_sim_data_call")
        if (!output.startsWith("ok:")) return null
        val value = output.removePrefix("ok:").trim()
        return value.toIntOrNull()?.takeIf { it in 0..1 }
    }
}
