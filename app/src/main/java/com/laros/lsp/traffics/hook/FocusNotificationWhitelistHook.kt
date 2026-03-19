package com.laros.lsp.traffics.hook

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.laros.lsp.traffics.util.FocusWhitelistBypassController
import io.github.libxposed.api.XposedInterface
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object FocusNotificationWhitelistHook {
    private val hookedLoaders = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val applicationHookInstalled = AtomicBoolean(false)
    private val pluginContextHookInstalled = AtomicBoolean(false)

    @Volatile
    private var xposed: XposedInterface? = null

    @Volatile
    private var systemUiContext: Context? = null

    fun install(xposedApi: XposedInterface, classLoader: ClassLoader, packageName: String) {
        xposed = xposedApi
        if (packageName == SYSTEM_UI_PACKAGE) {
            installApplicationHook(xposedApi)
            installPluginContextHook(xposedApi, classLoader)
        }
        installSettingsHooks(xposedApi, classLoader, "package=$packageName")
    }

    private fun installApplicationHook(xposedApi: XposedInterface) {
        if (!applicationHookInstalled.compareAndSet(false, true)) return
        val onCreate = runCatching {
            Application::class.java.getDeclaredMethod("onCreate")
        }.getOrElse {
            log(Log.WARN, "Application.onCreate unavailable", it)
            return
        }
        runCatching {
            xposedApi.hook(onCreate).intercept { chain ->
                val result = chain.proceed()
                val app = chain.thisObject as? Application
                if (app?.packageName == SYSTEM_UI_PACKAGE) {
                    systemUiContext = app.applicationContext
                }
                result
            }
        }.onFailure {
            log(Log.WARN, "hook Application.onCreate failed", it)
        }
    }

    private fun installPluginContextHook(xposedApi: XposedInterface, classLoader: ClassLoader) {
        if (!pluginContextHookInstalled.compareAndSet(false, true)) return
        val pluginFactoryClass = runCatching {
            Class.forName(PLUGIN_FACTORY_CLASS, false, classLoader)
        }.getOrElse {
            log(Log.WARN, "plugin factory class missing: ${it.message}")
            return
        }
        val methods = (pluginFactoryClass.declaredMethods + pluginFactoryClass.methods)
            .filter { it.name == "createPluginContext" }
            .distinct()
        if (methods.isEmpty()) {
            log(Log.WARN, "createPluginContext not found on $PLUGIN_FACTORY_CLASS")
            return
        }
        methods.forEach { method ->
            runCatching {
                method.isAccessible = true
                xposedApi.hook(method).intercept { chain ->
                    val result = chain.proceed()
                    val wrapper = result as? ContextWrapper
                    val pluginLoader = wrapper?.classLoader
                    if (pluginLoader != null) {
                        installSettingsHooks(xposedApi, pluginLoader, "pluginContext")
                    }
                    result
                }
            }.onFailure {
                log(Log.WARN, "hook createPluginContext failed", it)
            }
        }
        log(Log.INFO, "plugin context hook installed")
    }

    private fun installSettingsHooks(
        xposedApi: XposedInterface,
        classLoader: ClassLoader,
        source: String
    ) {
        val loaderKey = buildLoaderKey(classLoader)
        if (!hookedLoaders.add(loaderKey)) return

        val settingsClass = runCatching {
            Class.forName(NOTIFICATION_SETTINGS_MANAGER_CLASS, false, classLoader)
        }.getOrElse {
            log(Log.INFO, "skip focus whitelist hook, class missing for $source")
            return
        }

        var hookedCount = 0
        (settingsClass.declaredMethods + settingsClass.methods)
            .filter { it.name in TARGET_METHODS }
            .distinct()
            .forEach { method ->
                val stringArgIndexes = method.parameterTypes.mapIndexedNotNull { index, type ->
                    index.takeIf { type == String::class.java }
                }
                if (stringArgIndexes.isEmpty()) return@forEach
                runCatching {
                    method.isAccessible = true
                    xposedApi.hook(method).intercept { chain ->
                        val targetPackageMatched =
                            stringArgIndexes.any { chain.getArg(it) == TARGET_APP_PACKAGE }
                        if (targetPackageMatched && FocusWhitelistBypassController.isEnabled(resolveSystemUiContext())) {
                            true
                        } else {
                            chain.proceed()
                        }
                    }
                    hookedCount += 1
                }.onFailure {
                    log(Log.WARN, "hook ${method.declaringClass.name}.${method.name} failed", it)
                }
            }

        if (hookedCount == 0) {
            log(Log.WARN, "focus whitelist methods not found for $source")
            return
        }
        log(Log.INFO, "focus whitelist hook installed for $source methods=$hookedCount")
    }

    private fun buildLoaderKey(classLoader: ClassLoader): String {
        return "${classLoader.javaClass.name}@${System.identityHashCode(classLoader)}"
    }

    private fun resolveSystemUiContext(): Context? {
        systemUiContext?.let { return it }
        val app = runCatching {
            val activityThread = Class.forName("android.app.ActivityThread")
            val method = activityThread.getDeclaredMethod("currentApplication").apply {
                isAccessible = true
            }
            method.invoke(null) as? Application
        }.getOrNull()
        if (app?.packageName == SYSTEM_UI_PACKAGE) {
            systemUiContext = app.applicationContext
        }
        return systemUiContext
    }

    private fun log(priority: Int, message: String, throwable: Throwable? = null) {
        val api = xposed ?: return
        if (throwable == null) {
            api.log(priority, TAG, message)
        } else {
            api.log(priority, TAG, message, throwable)
        }
    }

    private const val TAG = "TrafficManager"
    private const val TARGET_APP_PACKAGE = "com.laros.lsp.traffics"
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val PLUGIN_FACTORY_CLASS = "com.android.systemui.shared.plugins.PluginInstance\$PluginFactory"
    private const val NOTIFICATION_SETTINGS_MANAGER_CLASS =
        "miui.systemui.notification.NotificationSettingsManager"
    private val TARGET_METHODS = setOf("canShowFocus", "canCustomFocus", "canPassXMSPermission")
}
