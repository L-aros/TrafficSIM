package com.laros.lsp.traffics.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import android.util.Log
import java.util.concurrent.TimeUnit

object PowerSaveScheduler {
    private const val WORK_PERIODIC = "tm_powersave_periodic"
    private const val WORK_ONCE = "tm_powersave_once"

    fun schedule(appContext: Context) {
        val periodic = PeriodicWorkRequestBuilder<SwitchWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(appContext)
            .enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, periodic)
        val once = OneTimeWorkRequestBuilder<SwitchWorker>().build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(WORK_ONCE, ExistingWorkPolicy.REPLACE, once)
        Log.i("TrafficManager", "powersave schedule: periodic+once")
    }

    fun triggerOnce(appContext: Context) {
        val once = OneTimeWorkRequestBuilder<SwitchWorker>().build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(WORK_ONCE, ExistingWorkPolicy.REPLACE, once)
        Log.i("TrafficManager", "powersave trigger once")
    }

    fun cancel(appContext: Context) {
        val wm = WorkManager.getInstance(appContext)
        wm.cancelUniqueWork(WORK_PERIODIC)
        wm.cancelUniqueWork(WORK_ONCE)
        Log.i("TrafficManager", "powersave cancel")
    }
}
