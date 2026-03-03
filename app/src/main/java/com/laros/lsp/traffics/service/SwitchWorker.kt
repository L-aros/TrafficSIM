package com.laros.lsp.traffics.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log
import com.laros.lsp.traffics.config.ConfigStore
import com.laros.lsp.traffics.log.LogStore
import com.laros.lsp.traffics.core.SwitchRunner

class SwitchWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val config = ConfigStore(applicationContext).load()
        if (!config.enabled || !config.powerSaveMode) {
            Log.i("TrafficManager", "workmanager skip: enabled=${config.enabled} powerSave=${config.powerSaveMode}")
            return Result.success()
        }
        LogStore(applicationContext).append("powersave: workmanager tick")
        Log.i("TrafficManager", "workmanager tick")
        SwitchRunner(applicationContext).runOnce("workmanager")
        return Result.success()
    }
}
