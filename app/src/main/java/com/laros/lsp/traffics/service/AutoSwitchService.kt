package com.laros.lsp.traffics.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.laros.lsp.traffics.R
import com.laros.lsp.traffics.config.ConfigStore
import com.laros.lsp.traffics.core.DataSlotResolver
import com.laros.lsp.traffics.core.RuleMatcher
import com.laros.lsp.traffics.core.SimSwitchCoordinator
import com.laros.lsp.traffics.core.SwitchTransportChain
import com.laros.lsp.traffics.core.WifiSnapshotProvider
import com.laros.lsp.traffics.core.XposedBroadcastTransport
import com.laros.lsp.traffics.core.AutoSwitchServiceChannels
import com.laros.lsp.traffics.core.SwitchEventNotifier
import com.laros.lsp.traffics.log.LogStore
import com.laros.lsp.traffics.config.SwitchStateStore

class AutoSwitchService : Service() {
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var configStore: ConfigStore
    private lateinit var wifiSnapshotProvider: WifiSnapshotProvider
    private lateinit var logStore: LogStore
    private lateinit var coordinator: SimSwitchCoordinator
    private lateinit var slotResolver: DataSlotResolver
    private lateinit var eventNotifier: SwitchEventNotifier

    private val tickRunnable = object : Runnable {
        override fun run() {
            runCatching { tickOnce() }
                .onFailure { logStore.append("tick failed: ${it.message}") }
            handler.postDelayed(this, nextIntervalMs())
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            logStore.append("screen event: ${intent?.action}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        configStore = ConfigStore(this)
        wifiSnapshotProvider = WifiSnapshotProvider(this)
        logStore = LogStore(this)
        eventNotifier = SwitchEventNotifier(this)
        SwitchEventNotifier.cancelPendingTransitions(this)

        val chain = SwitchTransportChain(
            listOf(
                XposedBroadcastTransport()
            )
        )
        coordinator = SimSwitchCoordinator(
            context = this,
            resolver = DataSlotResolver(this),
            transportChain = chain,
            logStore = logStore,
            stateStore = SwitchStateStore(this),
            onSwitchEvent = { eventNotifier.notify(it) }
        )
        slotResolver = DataSlotResolver(this)

        handlerThread = HandlerThread("tm-worker")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenReceiver, filter)
        }

        startForeground(AutoSwitchServiceChannels.NOTIFY_ID, buildNotification(getString(R.string.service_running)))
        handler.post(tickRunnable)
        logStore.append("service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        handlerThread.quitSafely()
        runCatching { unregisterReceiver(screenReceiver) }
        logStore.append("service stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun tickOnce() {
        val config = configStore.load()
        if (!config.enabled) {
            logStore.append("tick: skipped, config enabled=false")
            return
        }

        val snapshot = wifiSnapshotProvider.current()
        val matchedRule = RuleMatcher.findBest(snapshot, config.rules)
        val currentSlot = slotResolver.currentDataSlot()
        logStore.append(
            "tick: rules=${config.rules.size} currentSlot=$currentSlot ssid=${snapshot?.ssid} bssid=${snapshot?.bssid} matched=${matchedRule?.id ?: "none"}"
        )
        if (snapshot != null && matchedRule == null && config.rules.isNotEmpty()) {
            val rulePreview = config.rules.joinToString(" | ") {
                "id=${it.id},ssid=${it.ssid},bssid=${it.bssid},slot=${it.targetSlot}"
            }
            logStore.append("tick: no-match-rules $rulePreview")
        }
        coordinator.onTick(config, matchedRule, snapshot)
        logStore.trim(config)
    }

    private fun nextIntervalMs(): Long {
        val cfg = configStore.load()
        val pm = getSystemService(PowerManager::class.java)
        val interactive = pm?.isInteractive ?: true
        val sec = if (interactive) cfg.screenOnIntervalSec else cfg.screenOffIntervalSec
        return sec * 1000L
    }

    private fun buildNotification(text: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    AutoSwitchServiceChannels.CHANNEL_ID,
                    getString(R.string.notify_channel_service),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    AutoSwitchServiceChannels.EVENT_CHANNEL_ID,
                    getString(R.string.notify_channel_events),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        return NotificationCompat.Builder(this, AutoSwitchServiceChannels.CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
    }
}
