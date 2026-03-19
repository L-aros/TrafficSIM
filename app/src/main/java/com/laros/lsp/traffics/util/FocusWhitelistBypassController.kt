package com.laros.lsp.traffics.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object FocusWhitelistBypassController {
    fun isEnabled(context: Context?): Boolean {
        val packageManager = context?.packageManager ?: return true
        val state = runCatching {
            packageManager.getComponentEnabledSetting(componentName())
        }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
            state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER &&
            state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
    }

    fun sync(context: Context, enabled: Boolean) {
        val packageManager = context.packageManager
        val componentName = componentName()
        val desiredState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        val currentState = runCatching {
            packageManager.getComponentEnabledSetting(componentName)
        }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
        if (currentState == desiredState) return
        runCatching {
            packageManager.setComponentEnabledSetting(
                componentName,
                desiredState,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun componentName(): ComponentName {
        return ComponentName(TARGET_PACKAGE, RECEIVER_CLASS_NAME)
    }

    private const val TARGET_PACKAGE = "com.laros.lsp.traffics"
    private const val RECEIVER_CLASS_NAME =
        "com.laros.lsp.traffics.receiver.FocusWhitelistToggleReceiver"
}
