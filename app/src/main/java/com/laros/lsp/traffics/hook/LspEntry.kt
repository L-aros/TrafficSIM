package com.laros.lsp.traffics.hook

import com.laros.lsp.traffics.core.BridgeContract
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class LspEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!BridgeContract.PHONE_PACKAGES.contains(lpparam.packageName)) return
        runCatching { PhoneProcessBridge.install(lpparam.classLoader) }
            .onFailure { XposedBridge.log("TrafficManager install bridge failed: ${it.message}") }
    }
}
