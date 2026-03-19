package com.laros.lsp.traffics

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.laros.lsp.traffics.databinding.ActivityPermissionConfigBinding
import com.laros.lsp.traffics.util.PermissionHelper
import com.laros.lsp.traffics.util.PermissionSettingsNavigator

class PermissionConfigActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionConfigBinding
    private var autoRequestEnabled = false
    private val attemptedAutoRequestSteps = mutableSetOf<String>()

    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshState()
        continueAutoRequest()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshState()
        continueAutoRequest()
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshState()
        continueAutoRequest()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        autoRequestEnabled = intent.getBooleanExtra(EXTRA_AUTO_REQUEST, false)
        setupSystemBars()
        bindClicks()
        refreshState()
        binding.root.post { continueAutoRequest() }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
        binding.root.post { continueAutoRequest() }
    }

    private fun bindClicks() {
        binding.permissionConfigBackButton.setOnClickListener { finish() }
        binding.permissionConfigRefreshButton.setOnClickListener { refreshState() }
        binding.runtimePermissionActionButton.setOnClickListener { requestRuntimePermissions() }
        binding.notificationPermissionActionButton.setOnClickListener { handleNotificationAction() }
        binding.backgroundLocationActionButton.setOnClickListener { handleBackgroundLocationAction() }
        binding.autoStartActionButton.setOnClickListener {
            showHintToast(autoStartHint())
            PermissionSettingsNavigator.openAutoStartSettings(this)
        }
        binding.batteryActionButton.setOnClickListener {
            showHintToast(batteryHint())
            PermissionSettingsNavigator.openBatterySettings(this)
        }
        binding.backgroundPopupActionButton.setOnClickListener {
            showHintToast(backgroundPopupHint())
            PermissionSettingsNavigator.openBackgroundPopupSettings(this)
        }
    }

    private fun refreshState() {
        val vendorSupported = PermissionSettingsNavigator.isXiaomiFamily()
        binding.permissionConfigSystemSummary.text = getString(
            R.string.permission_config_system_summary,
            Build.MANUFACTURER.orEmpty().ifBlank { getString(R.string.label_unknown) },
            Build.MODEL.orEmpty().ifBlank { getString(R.string.label_unknown) },
            Build.VERSION.RELEASE.orEmpty().ifBlank { getString(R.string.label_unknown) },
            Build.VERSION.SDK_INT,
            getString(
                if (vendorSupported) {
                    R.string.permission_config_vendor_supported
                } else {
                    R.string.permission_config_vendor_generic
                }
            )
        )

        val runtimeMissing = PermissionHelper.missingRuntimePermissions(this)
        if (runtimeMissing.isEmpty()) {
            binding.runtimePermissionStatusText.text = getString(R.string.permission_config_status_granted)
            binding.runtimePermissionActionButton.text = getString(R.string.permission_config_action_done)
            binding.runtimePermissionActionButton.isEnabled = false
        } else {
            val labels = runtimeMissing.joinToString(", ") { permissionLabel(it) }
            binding.runtimePermissionStatusText.text = getString(
                R.string.permission_config_status_missing,
                labels
            )
            binding.runtimePermissionActionButton.text =
                getString(R.string.permission_config_action_request_runtime)
            binding.runtimePermissionActionButton.isEnabled = true
        }
        binding.runtimePermissionHintText.text = getString(R.string.permission_config_hint_runtime)

        val notificationsGranted = PermissionHelper.notificationPermissionGranted(this)
        val notificationsEnabled = PermissionHelper.areNotificationsEnabled(this)
        when {
            notificationsGranted && notificationsEnabled -> {
                binding.notificationPermissionStatusText.text =
                    getString(R.string.permission_config_status_notification_ready)
                binding.notificationPermissionActionButton.text =
                    getString(R.string.permission_config_action_done)
                binding.notificationPermissionActionButton.isEnabled = false
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted -> {
                binding.notificationPermissionStatusText.text = getString(
                    R.string.permission_config_status_missing,
                    getString(R.string.permission_config_label_notifications)
                )
                binding.notificationPermissionActionButton.text =
                    getString(R.string.permission_config_action_request_notification)
                binding.notificationPermissionActionButton.isEnabled = true
            }
            else -> {
                binding.notificationPermissionStatusText.text =
                    getString(R.string.permission_config_status_notification_settings_required)
                binding.notificationPermissionActionButton.text =
                    getString(R.string.permission_config_action_open_notification_settings)
                binding.notificationPermissionActionButton.isEnabled = true
            }
        }
        binding.notificationPermissionHintText.text =
            getString(R.string.permission_config_hint_notification)

        val foregroundLocationGranted = PermissionHelper.hasForegroundLocation(this)
        val backgroundLocationGranted = PermissionHelper.hasBackgroundLocation(this)
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                binding.backgroundLocationStatusText.text =
                    getString(R.string.permission_config_status_not_required)
                binding.backgroundLocationActionButton.text =
                    getString(R.string.permission_config_action_done)
                binding.backgroundLocationActionButton.isEnabled = false
            }
            backgroundLocationGranted -> {
                binding.backgroundLocationStatusText.text =
                    getString(R.string.permission_config_status_granted)
                binding.backgroundLocationActionButton.text =
                    getString(R.string.permission_config_action_done)
                binding.backgroundLocationActionButton.isEnabled = false
            }
            !foregroundLocationGranted -> {
                binding.backgroundLocationStatusText.text =
                    getString(R.string.permission_config_status_foreground_required)
                binding.backgroundLocationActionButton.text =
                    getString(R.string.permission_config_action_request_runtime)
                binding.backgroundLocationActionButton.isEnabled = true
            }
            else -> {
                binding.backgroundLocationStatusText.text = getString(
                    R.string.permission_config_status_missing,
                    getString(R.string.permission_config_label_background_location)
                )
                binding.backgroundLocationActionButton.text =
                    getString(R.string.permission_config_action_request_background_location)
                binding.backgroundLocationActionButton.isEnabled = true
            }
        }
        binding.backgroundLocationHintText.text =
            getString(R.string.permission_config_hint_background_location)

        binding.autoStartStatusText.text =
            getString(R.string.permission_config_status_manual_check)
        binding.autoStartHintText.text = autoStartHint()
        binding.autoStartActionButton.text = getString(R.string.permission_config_action_open_auto_start)
        binding.autoStartActionButton.isEnabled = true

        val powerManager = getSystemService(PowerManager::class.java)
        val ignoreBatteryOptimizations =
            powerManager?.isIgnoringBatteryOptimizations(packageName) == true
        binding.batteryStatusText.text = if (vendorSupported) {
            getString(R.string.permission_config_status_manual_check)
        } else {
            getString(
                if (ignoreBatteryOptimizations) {
                    R.string.permission_config_status_battery_ignored
                } else {
                    R.string.permission_config_status_battery_restricted
                }
            )
        }
        binding.batteryHintText.text = batteryHint()
        binding.batteryActionButton.text = getString(R.string.permission_config_action_open_battery)
        binding.batteryActionButton.isEnabled = true

        binding.backgroundPopupStatusText.text =
            getString(R.string.permission_config_status_manual_check)
        binding.backgroundPopupHintText.text = backgroundPopupHint()
        binding.backgroundPopupActionButton.text =
            getString(R.string.permission_config_action_open_background_popup)
        binding.backgroundPopupActionButton.isEnabled = true
    }

    private fun requestRuntimePermissions() {
        val need = PermissionHelper.missingRuntimePermissions(this)
        if (need.isEmpty()) {
            refreshState()
            return
        }
        runtimePermissionLauncher.launch(need.toTypedArray())
    }

    private fun handleNotificationAction() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            PermissionHelper.needsNotificationRuntimePermission(this)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        PermissionSettingsNavigator.openNotificationSettings(this)
    }

    private fun handleBackgroundLocationAction() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            refreshState()
            return
        }
        val foregroundLocationGranted = PermissionHelper.hasForegroundLocation(this)
        if (!foregroundLocationGranted) {
            Toast.makeText(
                this,
                R.string.permission_config_toast_foreground_location_first,
                Toast.LENGTH_SHORT
            ).show()
            requestRuntimePermissions()
            return
        }
        backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun permissionLabel(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> getString(R.string.permission_config_label_fine_location)
            Manifest.permission.ACCESS_COARSE_LOCATION -> getString(R.string.permission_config_label_coarse_location)
            Manifest.permission.READ_PHONE_STATE -> getString(R.string.permission_config_label_phone_state)
            Manifest.permission.NEARBY_WIFI_DEVICES -> getString(R.string.permission_config_label_nearby_wifi)
            else -> PermissionHelper.permissionLabel(this, permission)
        }
    }

    private fun continueAutoRequest() {
        if (!autoRequestEnabled) return
        when {
            PermissionHelper.missingRuntimePermissions(this).isNotEmpty() &&
                attemptedAutoRequestSteps.add(AUTO_STEP_RUNTIME) -> {
                requestRuntimePermissions()
            }

            PermissionHelper.needsNotificationRuntimePermission(this) &&
                attemptedAutoRequestSteps.add(AUTO_STEP_NOTIFICATION) -> {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            PermissionHelper.needsBackgroundLocationPermission(this) &&
                attemptedAutoRequestSteps.add(AUTO_STEP_BACKGROUND_LOCATION) -> {
                handleBackgroundLocationAction()
            }

            else -> {
                autoRequestEnabled = false
            }
        }
    }

    private fun autoStartHint(): String {
        return getString(
            if (PermissionSettingsNavigator.isXiaomiFamily()) {
                R.string.permission_config_hint_auto_start_xiaomi
            } else {
                R.string.permission_config_hint_auto_start_generic
            }
        )
    }

    private fun batteryHint(): String {
        return getString(
            if (PermissionSettingsNavigator.isXiaomiFamily()) {
                R.string.permission_config_hint_battery_xiaomi
            } else {
                R.string.permission_config_hint_battery_generic
            }
        )
    }

    private fun backgroundPopupHint(): String {
        return getString(
            if (PermissionSettingsNavigator.isXiaomiFamily()) {
                R.string.permission_config_hint_background_popup_xiaomi
            } else {
                R.string.permission_config_hint_background_popup_generic
            }
        )
    }

    private fun showHintToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setupSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.permissionConfigHeader.updatePadding(top = bars.top + 8.dp())
            binding.permissionConfigScroll.updatePadding(bottom = bars.bottom + 12.dp())
            insets
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_AUTO_REQUEST = "auto_request"
        private const val AUTO_STEP_RUNTIME = "runtime"
        private const val AUTO_STEP_NOTIFICATION = "notification"
        private const val AUTO_STEP_BACKGROUND_LOCATION = "background_location"
    }
}
