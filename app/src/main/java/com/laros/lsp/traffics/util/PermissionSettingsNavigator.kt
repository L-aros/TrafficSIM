package com.laros.lsp.traffics.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object PermissionSettingsNavigator {
    fun isXiaomiFamily(): Boolean {
        val brand = Build.BRAND.orEmpty()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        return brand.contains("xiaomi", ignoreCase = true) ||
            brand.contains("redmi", ignoreCase = true) ||
            brand.contains("poco", ignoreCase = true) ||
            manufacturer.contains("xiaomi", ignoreCase = true)
    }

    fun openNotificationSettings(context: Context): Boolean {
        val notificationIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        return openFirstAvailable(
            context,
            listOf(notificationIntent, appDetailsIntent(context))
        )
    }

    fun openBatterySettings(context: Context): Boolean {
        val intents = buildList {
            if (isXiaomiFamily()) {
                add(
                    Intent().apply {
                        component = ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.powercenter.legacypowerrank.PowerDetailActivity"
                        )
                        putExtra("package_name", context.packageName)
                        putExtra("package_label", appLabel(context))
                    }
                )
                add(
                    Intent("miui.intent.action.POWER_BATTERY")
                )
            }
            add(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                )
            )
            add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            add(appDetailsIntent(context))
        }
        return openFirstAvailable(context, intents)
    }

    fun openAutoStartSettings(context: Context): Boolean {
        val intents = buildList {
            if (isXiaomiFamily()) {
                add(
                    Intent().apply {
                        component = ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                    }
                )
            }
            add(appDetailsIntent(context))
        }
        return openFirstAvailable(context, intents)
    }

    fun openBackgroundPopupSettings(context: Context): Boolean {
        val intents = buildList {
            if (isXiaomiFamily()) {
                add(
                    Intent().apply {
                        component = ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.permissions.PermissionsEditorActivity"
                        )
                        putExtra("extra_pkgname", context.packageName)
                    }
                )
            }
            add(appDetailsIntent(context))
        }
        return openFirstAvailable(context, intents)
    }

    fun openAppDetails(context: Context): Boolean {
        return openFirstAvailable(context, listOf(appDetailsIntent(context)))
    }

    private fun appDetailsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
    }

    private fun appLabel(context: Context): String {
        return context.applicationInfo.loadLabel(context.packageManager).toString()
    }

    private fun openFirstAvailable(context: Context, intents: List<Intent>): Boolean {
        intents.forEach { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) == null) {
                return@forEach
            }
            val started = runCatching {
                context.startActivity(intent)
            }.isSuccess
            if (started) {
                return true
            }
            Log.w("PermissionNavigator", "failed to open settings intent: $intent")
        }
        return false
    }
}
