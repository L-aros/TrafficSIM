package com.laros.lsp.traffics.util

import android.app.Activity
import android.app.ActivityManager

object TaskVisibilityController {
    fun sync(activity: Activity, hideFromRecents: Boolean) {
        val manager = activity.getSystemService(ActivityManager::class.java) ?: return
        val currentTask = manager.appTasks.firstOrNull { task ->
            runCatching { task.taskInfo.taskId == activity.taskId }.getOrDefault(false)
        } ?: manager.appTasks.firstOrNull()
        runCatching { currentTask?.setExcludeFromRecents(hideFromRecents) }
    }
}
