package com.laros.lsp.traffics.core

import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object RootShell {
    private val suBins = listOf(
        "su",
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/data/adb/ksu/bin/su",
        "/data/adb/ap/bin/su",
        "/debug_ramdisk/su"
    )

    fun runAsRoot(command: String, timeoutMs: Long = 8_000L): String {
        val traces = mutableListOf<String>()
        for (suBin in suBins) {
            val output = runSingleSu(suBin, command, timeoutMs)
            traces += "$suBin=>$output"
            if (output.startsWith("ok:")) return output
        }
        return "err: ${traces.joinToString(" | ")}"
    }

    fun hasRootAccess(timeoutMs: Long = 2_000L): Boolean {
        return runAsRoot("id", timeoutMs).startsWith("ok:")
    }

    fun readGlobalDataSlot(): Int? {
        val output = runAsRoot("settings get global multi_sim_data_call")
        if (!output.startsWith("ok:")) return null
        val value = output.removePrefix("ok:").trim()
        return value.toIntOrNull()?.takeIf { it in 0..1 }
    }

    private fun runSingleSu(suBin: String, command: String, timeoutMs: Long): String {
        return runCatching {
            val process = ProcessBuilder(suBin, "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = StringBuilder()
            val readerThread = thread(start = true, isDaemon = true, name = "tm-root-reader") {
                runCatching {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            output.appendLine(line)
                        }
                    }
                }
            }
            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                runCatching { process.inputStream.close() }
                readerThread.join(200)
                return "err: timeout"
            }
            readerThread.join(500)
            val out = output.toString().trim()
            val code = process.exitValue()
            if (code == 0) "ok: $out" else "err($code): $out"
        }.getOrElse { "err: ${it.javaClass.simpleName}: ${it.message}" }
    }
}

