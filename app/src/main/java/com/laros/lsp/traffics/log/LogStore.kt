package com.laros.lsp.traffics.log

import android.content.Context
import com.laros.lsp.traffics.model.AppConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogStore(private val context: Context) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    fun append(line: String) {
        runCatching {
            val dir = logDir()
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "traffic-${fileDateFormat.format(Date())}.log")
            file.appendText("${dateFormat.format(Date())} $line\n")
        }
    }

    fun trim(config: AppConfig) {
        val dir = logDir()
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return

        val now = System.currentTimeMillis()
        val retentionMs = config.logRetentionDays * 24L * 60L * 60L * 1000L
        files.forEach { f ->
            if (now - f.lastModified() > retentionMs) {
                runCatching { f.delete() }
            }
        }

        val remaining = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var totalBytes = remaining.sumOf { it.length() }
        val limit = config.logMaxMb * 1024L * 1024L
        for (f in remaining) {
            if (totalBytes <= limit) break
            val len = f.length()
            if (runCatching { f.delete() }.getOrDefault(false)) {
                totalBytes -= len
            }
        }
    }

    fun exportAll(): File {
        val dir = logDir()
        if (!dir.exists()) dir.mkdirs()
        val export = File(context.cacheDir, "traffic-manager-export-${System.currentTimeMillis()}.txt")
        export.writeText("")
        val files = dir.listFiles()?.sortedBy { it.name } ?: emptyList()
        files.forEach { file ->
            export.appendText("===== ${file.name} =====\n")
            export.appendText(file.readText())
            export.appendText("\n")
        }
        return export
    }

    private fun logDir(): File = File(context.filesDir, "logs")
}
