package com.mmy.g700remote.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.mmy.g700remote.BuildConfig
import com.mmy.g700remote.ble.RemoteConnectionState
import com.mmy.g700remote.data.RemoteUiState

object G700Analytics {
    private const val MAX_PARAM_LENGTH = 96
    private var analytics: FirebaseAnalytics? = null
    private var sessionStartedAt: Long = 0L

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        analytics = FirebaseAnalytics.getInstance(appContext).apply {
            setAnalyticsCollectionEnabled(true)
            setUserProperty("app_version", BuildConfig.VERSION_NAME)
        }
        FirebaseCrashlytics.getInstance().setCustomKey("app_version", BuildConfig.VERSION_NAME)
    }

    fun screen(name: String) {
        log(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            mapOf(
                FirebaseAnalytics.Param.SCREEN_NAME to name.safeParam(),
                FirebaseAnalytics.Param.SCREEN_CLASS to "Compose",
            ),
        )
    }

    fun appForeground(state: RemoteUiState) {
        sessionStartedAt = System.currentTimeMillis()
        updateUserProperties(state)
        log("app_foreground", commonStateParams(state))
    }

    fun appBackground(state: RemoteUiState) {
        val durationSeconds = if (sessionStartedAt > 0L) {
            ((System.currentTimeMillis() - sessionStartedAt) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }
        log(
            "app_background",
            commonStateParams(state) + ("duration_sec" to durationSeconds),
        )
        sessionStartedAt = 0L
    }

    fun connectionState(state: RemoteUiState) {
        updateCrashKeys(state)
        log("connection_state", commonStateParams(state))
    }

    fun command(name: String, screen: String, state: RemoteUiState) {
        log(
            "vehicle_command",
            commonStateParams(state) + mapOf(
                "command" to name.safeParam(),
                "screen" to screen.safeParam(),
            ),
        )
    }

    fun settingChanged(name: String, value: String) {
        log(
            "setting_changed",
            mapOf(
                "setting" to name.safeParam(),
                "value" to value.safeParam(),
            ),
        )
    }

    fun navigationShare(sent: Boolean, saved: Boolean, source: String) {
        log(
            "navigation_share",
            mapOf(
                "sent" to sent,
                "saved" to saved,
                "source_type" to source.safeParam(),
            ),
        )
    }

    fun mapOpened(source: String) {
        log("map_opened", mapOf("source" to source.safeParam()))
    }

    fun nonFatal(throwable: Throwable, area: String) {
        FirebaseCrashlytics.getInstance().setCustomKey("last_nonfatal_area", area.safeParam())
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    fun newTrace(name: String): Trace =
        FirebasePerformance.getInstance().newTrace(name.safeParam().replace(' ', '_'))

    fun updateUserProperties(state: RemoteUiState) {
        analytics?.apply {
            setUserProperty("language", state.appLanguage.name)
            setUserProperty("theme", state.appTheme.name)
            setUserProperty("color_mode", state.appColorMode.name)
            setUserProperty("connection_preference", state.connectionPreference.name)
            setUserProperty("ble_enabled", state.bleEnabled.toString())
            setUserProperty("lan_enabled", state.lanEnabled.toString())
            setUserProperty("regional_features", state.regionalFeaturesEnabled.toString())
            setUserProperty("wake_nearby_enabled", state.bleWakeEnabled.toString())
            setUserProperty("connected_notification", state.connectedNotificationEnabled.toString())
            setUserProperty("demo_mode", state.demoMode.toString())
        }
        updateCrashKeys(state)
    }

    private fun updateCrashKeys(state: RemoteUiState) {
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("connection_state", state.connectionState.analyticsLabel())
            setCustomKey("transport", state.activeTransportLabel())
            setCustomKey("demo_mode", state.demoMode)
            setCustomKey("paired", state.pairedDevice != null)
            setCustomKey("language", state.appLanguage.name)
            setCustomKey("theme", state.appTheme.name)
        }
    }

    private fun commonStateParams(state: RemoteUiState): Map<String, Any> =
        mapOf(
            "connection_state" to state.connectionState.analyticsLabel(),
            "transport" to state.activeTransportLabel(),
            "paired" to (state.pairedDevice != null),
            "demo_mode" to state.demoMode,
            "language" to state.appLanguage.name,
            "theme" to state.appTheme.name,
        )

    private fun log(name: String, params: Map<String, Any?> = emptyMap()) {
        analytics?.logEvent(name.safeEventName(), params.toBundle())
    }

    private fun Map<String, Any?>.toBundle(): Bundle =
        Bundle().also { bundle ->
            forEach { (key, value) ->
                when (value) {
                    is Boolean -> bundle.putString(key.safeEventName(), value.toString())
                    is Int -> bundle.putLong(key.safeEventName(), value.toLong())
                    is Long -> bundle.putLong(key.safeEventName(), value)
                    is Double -> bundle.putDouble(key.safeEventName(), value)
                    is Float -> bundle.putDouble(key.safeEventName(), value.toDouble())
                    null -> Unit
                    else -> bundle.putString(key.safeEventName(), value.toString().safeParam())
                }
            }
        }

    private fun RemoteUiState.activeTransportLabel(): String =
        (connectionState as? RemoteConnectionState.Ready)?.transport?.name ?: "none"

    private fun RemoteConnectionState.analyticsLabel(): String =
        when (this) {
            RemoteConnectionState.Disconnected -> "disconnected"
            RemoteConnectionState.Scanning -> "scanning"
            is RemoteConnectionState.Connecting -> "connecting"
            RemoteConnectionState.DiscoveringServices -> "discovering"
            RemoteConnectionState.EnablingNotifications -> "subscribing"
            RemoteConnectionState.Handshaking -> "handshaking"
            is RemoteConnectionState.Ready -> "ready"
            is RemoteConnectionState.Error -> "error"
        }

    private fun String.safeEventName(): String =
        lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { "event" }
            .take(40)

    private fun String.safeParam(): String =
        take(MAX_PARAM_LENGTH)
}
