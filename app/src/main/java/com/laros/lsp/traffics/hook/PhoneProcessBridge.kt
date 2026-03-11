package com.laros.lsp.traffics.hook

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.laros.lsp.traffics.core.BridgeContract
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object PhoneProcessBridge {
    private val installed = AtomicBoolean(false)
    @Volatile
    private var processClassLoader: ClassLoader? = null

    fun install(classLoader: ClassLoader) {
        processClassLoader = classLoader
        runCatching {
            XposedHelpers.findAndHookMethod(
            "com.android.phone.PhoneGlobals",
            classLoader,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val context = param.thisObject as? Context ?: return
                    registerReceiver(context)
                }
            }
        )
        }.onFailure {
            XposedBridge.log("TrafficManager: PhoneGlobals hook failed, fallback to Application hook: ${it.message}")
        }

        XposedBridge.hookAllMethods(
            Application::class.java,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val app = param.thisObject as? Application ?: return
                    if (!BridgeContract.PHONE_PACKAGES.contains(app.packageName)) return
                    processClassLoader = app.classLoader
                    registerReceiver(app)
                }
            }
        )
    }

    private fun registerReceiver(context: Context) {
        if (!installed.compareAndSet(false, true)) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent?) {
                if (intent?.action != BridgeContract.ACTION_SWITCH_DATA) return
                val token = intent.getStringExtra(BridgeContract.EXTRA_TOKEN) ?: return
                val pending = goAsync()
                val requestPackage = intent.getStringExtra(BridgeContract.EXTRA_REQUEST_PACKAGE)
                BRIDGE_EXECUTOR.execute {
                    try {
                        val slot = intent.getIntExtra(BridgeContract.EXTRA_TARGET_SLOT, -1)
                        var subId = intent.getIntExtra(BridgeContract.EXTRA_TARGET_SUB_ID, -1)
                        XposedBridge.log("TrafficManager: bridge recv token=$token slot=$slot subId=$subId")
                        if (subId < 0 && slot >= 0) {
                            subId = resolveSubIdBySlot(ctx, slot)
                        }
                        val (ok, msg) = if (subId >= 0) {
                            switchDefaultDataSubId(ctx, subId)
                        } else {
                            false to "invalid subId for slot=$slot"
                        }
                        sendResult(ctx, requestPackage, token, ok, msg)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }

        val filter = IntentFilter(BridgeContract.ACTION_SWITCH_DATA)
        registerBridgeReceiver(context, receiver, filter)
        XposedBridge.log("TrafficManager: phone bridge receiver installed")
    }

    private fun registerBridgeReceiver(
        context: Context,
        receiver: BroadcastReceiver,
        filter: IntentFilter
    ) {
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            BridgeContract.PERMISSION_INTERNAL_BRIDGE,
            null,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun sendResult(
        context: Context,
        requestPackage: String?,
        token: String,
        success: Boolean,
        message: String
    ) {
        val result = Intent(BridgeContract.ACTION_SWITCH_RESULT).apply {
            requestPackage?.takeIf { it.isNotBlank() }?.let { `package` = it }
            putExtra(BridgeContract.EXTRA_TOKEN, token)
            putExtra(BridgeContract.EXTRA_SUCCESS, success)
            putExtra(BridgeContract.EXTRA_MESSAGE, message)
            putExtra(BridgeContract.EXTRA_SOURCE_PACKAGE, context.packageName)
        }
        context.sendBroadcast(result)
    }

    @SuppressLint("MissingPermission")
    private fun resolveSubIdBySlot(context: Context, slot: Int): Int {
        return runCatching {
            val sm = context.getSystemService(SubscriptionManager::class.java)
            val infos = sm?.activeSubscriptionInfoList ?: emptyList()
            infos.firstOrNull { it.simSlotIndex == slot }?.subscriptionId ?: -1
        }.getOrDefault(-1)
    }

    private fun switchDefaultDataSubId(context: Context, subId: Int): Pair<Boolean, String> {
        val classes = listOf(
            "com.android.internal.telephony.SubscriptionController" to listOf(
                "setDefaultDataSubId",
                "setDefaultDataSubIdWithReason"
            ),
            "com.android.internal.telephony.SubscriptionManagerService" to listOf(
                "setDefaultDataSubId",
                "setDefaultDataSubIdWithReason"
            ),
            "com.android.internal.telephony.subscription.SubscriptionManagerService" to listOf(
                "setDefaultDataSubId",
                "setDefaultDataSubIdWithReason"
            ),
            "android.telephony.SubscriptionManager" to listOf(
                "setDefaultDataSubId"
            ),
            "com.android.internal.telephony.PhoneSwitcher" to listOf(
                "setPreferredDataSubscriptionId"
            )
        )

        val traces = mutableListOf<String>()
        for ((className, methodNames) in classes) {
            val r = tryInvokeClass(context, className, methodNames, subId)
            traces += "$className:${r.second}"
            if (r.first) {
                val verified = verifyDefaultDataSubId(subId)
                val msg = "${r.second}, verify=$verified"
                if (verified) return true to msg
                traces += "$className:verify_failed"
            }
        }
        val all = traces.joinToString(" || ")
        XposedBridge.log("TrafficManager switchDefaultDataSubId failed: $all")
        return false to all
    }

    private fun tryInvokeClass(
        context: Context,
        className: String,
        methodNames: List<String>,
        subId: Int
    ): Pair<Boolean, String> {
        val clazz = loadClass(className) ?: return false to "class_not_found"

        val methods = (clazz.declaredMethods + clazz.methods)
            .filter { methodNames.contains(it.name) }
            .sortedBy { it.parameterTypes.size }

        if (methods.isEmpty()) return false to "method_not_found:${methodNames.joinToString(",")}"

        val methodTraces = mutableListOf<String>()
        for (method in methods) {
            val r = tryInvokeMethod(context, clazz, method, subId)
            methodTraces += r.second
            if (r.first) return true to r.second
        }
        return false to methodTraces.joinToString(" ; ")
    }

    private fun tryInvokeMethod(
        context: Context,
        clazz: Class<*>,
        method: Method,
        subId: Int
    ): Pair<Boolean, String> {
        return runCatching {
            method.isAccessible = true
            val target = if (Modifier.isStatic(method.modifiers)) {
                null
            } else {
                obtainInstance(clazz, context)
            }
            if (!Modifier.isStatic(method.modifiers) && target == null) {
                return false to "${method.name}(p=${method.parameterTypes.size}) no_instance"
            }
            val args = buildArgs(method.parameterTypes, subId, context)
                ?: return false to "${method.name}(p=${method.parameterTypes.size}) unsupported_signature"
            val result = method.invoke(target, *args)
            val ok = when (result) {
                is Boolean -> result
                else -> true
            }
            ok to "${method.name}(p=${method.parameterTypes.size}) ok result=${result ?: "void"}"
        }.getOrElse {
            false to "${method.name}(p=${method.parameterTypes.size}) ex=${it.javaClass.simpleName}:${it.message}"
        }
    }

    private fun obtainInstance(clazz: Class<*>, context: Context): Any? {
        if (clazz.name == "android.telephony.SubscriptionManager") {
            context.getSystemService(SubscriptionManager::class.java)?.let { return it }
        }
        runCatching {
            val m = clazz.getMethod("getInstance")
            return m.invoke(null)
        }
        runCatching {
            val f = clazz.getDeclaredField("sInstance")
            f.isAccessible = true
            return f.get(null)
        }
        return null
    }

    private fun loadClass(name: String): Class<*>? {
        val candidates = listOfNotNull(
            processClassLoader,
            PhoneProcessBridge::class.java.classLoader
        ).distinct()

        for (loader in candidates) {
            val c = runCatching { Class.forName(name, false, loader) }.getOrNull()
            if (c != null) return c
        }
        return runCatching { Class.forName(name) }.getOrNull()
    }

    private fun buildArgs(types: Array<Class<*>>, subId: Int, context: Context): Array<Any?>? {
        val args = arrayOfNulls<Any>(types.size)
        for (i in types.indices) {
            val t = types[i]
            args[i] = when {
                t == Int::class.javaPrimitiveType || t == Int::class.javaObjectType -> subId
                t == Boolean::class.javaPrimitiveType || t == Boolean::class.javaObjectType -> false
                t == Long::class.javaPrimitiveType || t == Long::class.javaObjectType -> 0L
                t == String::class.java -> context.packageName
                t.name == "android.content.Context" -> context
                t.isPrimitive -> return null
                else -> null
            }
        }
        return args
    }

    private fun verifyDefaultDataSubId(targetSubId: Int): Boolean {
        repeat(12) {
            val current = SubscriptionManager.getDefaultDataSubscriptionId()
            if (current == targetSubId) return true
            Thread.sleep(250)
        }
        return false
    }

    private val BRIDGE_EXECUTOR = Executors.newSingleThreadExecutor()
}
