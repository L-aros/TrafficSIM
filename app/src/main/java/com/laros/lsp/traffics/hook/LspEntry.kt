package com.laros.lsp.traffics.hook

import android.util.Log
import com.laros.lsp.traffics.core.BridgeContract
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

class LspEntry : XposedModule() {
    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        val packageName = param.packageName
        runCatching {
            when {
                BridgeContract.PHONE_PACKAGES.contains(packageName) -> {
                    PhoneProcessBridge.install(this, param.classLoader, packageName)
                }

                FOCUS_HOOK_PACKAGES.contains(packageName) -> {
                    FocusNotificationWhitelistHook.install(this, param.classLoader, packageName)
                }

                else -> return
            }
        }.onFailure {
            log(Log.ERROR, TAG, "install hooks failed for $packageName", it)
        }
    }

    private companion object {
        const val TAG = "TrafficManager"
        val FOCUS_HOOK_PACKAGES = setOf("com.android.systemui")
    }
}
