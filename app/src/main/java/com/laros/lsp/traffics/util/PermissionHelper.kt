package com.laros.lsp.traffics.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.laros.lsp.traffics.R

object PermissionHelper {
    fun missingRuntimePermissions(context: Context): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.NEARBY_WIFI_DEVICES
        }
        return permissions.filterNot { hasPermission(context, it) }
    }

    fun hasForegroundLocation(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    fun hasBackgroundLocation(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    fun needsBackgroundLocationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            hasForegroundLocation(context) &&
            !hasBackgroundLocation(context)
    }

    fun needsNotificationRuntimePermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    }

    fun notificationPermissionGranted(context: Context): Boolean {
        return !needsNotificationRuntimePermission(context)
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun hasBlockingPermissions(context: Context): Boolean {
        return missingRuntimePermissions(context).isNotEmpty() ||
            needsNotificationRuntimePermission(context) ||
            needsBackgroundLocationPermission(context)
    }

    fun blockingPermissionLabels(context: Context): List<String> {
        val labels = missingRuntimePermissions(context).map { permissionLabel(context, it) }.toMutableList()
        if (needsNotificationRuntimePermission(context)) {
            labels += context.getString(R.string.perm_post_notifications)
        }
        if (needsBackgroundLocationPermission(context)) {
            labels += context.getString(R.string.perm_background_location)
        }
        return labels.distinct()
    }

    fun requiredPermissionsForSelfCheck(context: Context): List<String> {
        val permissions = missingRuntimePermissions(context).toMutableList()
        if (needsNotificationRuntimePermission(context)) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        if (needsBackgroundLocationPermission(context)) {
            permissions += Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }
        return permissions.distinct()
    }

    fun permissionLabel(context: Context, permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> context.getString(R.string.perm_fine_location)
            Manifest.permission.ACCESS_COARSE_LOCATION -> context.getString(R.string.perm_coarse_location)
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> context.getString(R.string.perm_background_location)
            Manifest.permission.NEARBY_WIFI_DEVICES -> context.getString(R.string.perm_nearby_wifi)
            Manifest.permission.READ_PHONE_STATE -> context.getString(R.string.perm_read_phone_state)
            Manifest.permission.POST_NOTIFICATIONS -> context.getString(R.string.perm_post_notifications)
            else -> permission.substringAfterLast('.')
        }
    }

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}
