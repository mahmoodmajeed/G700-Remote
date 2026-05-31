package com.mmy.g700remote.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mmy.g700remote.BuildConfig
import com.mmy.g700remote.data.AppUpdateInfo
import com.mmy.g700remote.data.AppUpdateState
import com.mmy.g700remote.data.UpdateGateReason
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AppUpdateManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(snapshotState(prefs))
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    suspend fun checkIfDue() {
        if (isUpdateCheckStale(appContext) || readStoredUpdateInfo(appContext) != null) {
            checkNow()
        } else {
            _state.update { snapshotState(prefs).copy(message = it.message) }
        }
    }

    suspend fun checkNow() {
        _state.update { it.copy(isChecking = true, message = null) }
        val result = runCatching { fetchLatestUpdate(appContext) }
        _state.update { state ->
            result.fold(
                onSuccess = { info ->
                    recordSuccessfulCheck(appContext, info)
                    val snapshot = snapshotState(prefs)
                    state.copy(
                        isChecking = false,
                        availableUpdate = info,
                        lastCheckedMillis = snapshot.lastCheckedMillis,
                        gateReason = snapshot.gateReason,
                        message = if (info == null) "G700 Remote is up to date" else "Version ${info.versionName} is available",
                    )
                },
                onFailure = { throwable ->
                    val gate = if (isUpdateCheckStale(appContext)) {
                        UpdateGateReason.StaleCheck
                    } else {
                        state.gateReason
                    }
                    state.copy(
                        isChecking = false,
                        lastCheckedMillis = readLastSuccessfulCheckMillis(appContext),
                        gateReason = gate,
                        message = throwable.message ?: "Update check failed",
                    )
                },
            )
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    suspend fun downloadAndInstall(activity: Activity, info: AppUpdateInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            _state.update { it.copy(message = "Allow this app to install downloaded updates, then tap Download again.") }
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}"),
                ),
            )
            return
        }

        _state.update { it.copy(isDownloading = true, downloadProgress = 0, message = null) }
        val result = runCatching {
            downloadApk(appContext, info) { progress ->
                _state.update { it.copy(downloadProgress = progress) }
            }
        }
        result.onSuccess { file ->
            _state.update { it.copy(isDownloading = false, downloadProgress = null, message = "Opening Android installer") }
            launchInstaller(activity, file)
        }.onFailure { throwable ->
            _state.update {
                it.copy(
                    isDownloading = false,
                    downloadProgress = null,
                    message = throwable.message ?: "Update download failed",
                )
            }
        }
    }

    companion object {
        private const val RELEASE_API = "https://api.github.com/repos/mahmoodmajeed/G700-Remote/releases/latest"
        private val USER_AGENT = "G700-Remote/${BuildConfig.VERSION_NAME}"
        private const val UPDATE_WORK = "g700_remote_update_check"
        private const val UPDATE_PREFS = "g700_update_state"
        private const val KEY_LAST_SUCCESSFUL_CHECK = "last_successful_check"
        private const val KEY_AVAILABLE_UPDATE = "available_update"
        private const val MAX_CHECK_AGE_MS = 7L * 24L * 60L * 60L * 1000L

        fun scheduleBackgroundChecks(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UPDATE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        suspend fun checkLatestAndRecord(context: Context): AppUpdateInfo? {
            val info = fetchLatestUpdate(context)
            recordSuccessfulCheck(context, info)
            return info
        }

        suspend fun fetchLatestUpdate(context: Context): AppUpdateInfo? =
            withContext(Dispatchers.IO) {
                val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", USER_AGENT)
                }
                try {
                    if (connection.responseCode !in 200..299) {
                        error("GitHub update check failed (${connection.responseCode})")
                    }
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val tag = json.optString("tag_name").ifBlank { json.optString("name") }
                    val version = normalizeVersion(tag)
                    if (!isNewer(version, BuildConfig.VERSION_NAME)) return@withContext null

                    val assets = json.optJSONArray("assets")
                    var apkUrl: String? = null
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.optJSONObject(i) ?: continue
                            val name = asset.optString("name").lowercase(Locale.US)
                            if (name.endsWith(".apk")) {
                                apkUrl = asset.optString("browser_download_url")
                                break
                            }
                        }
                    }
                    if (apkUrl.isNullOrBlank()) {
                        error("Latest GitHub release has no APK asset")
                    }

                    AppUpdateInfo(
                        versionName = version,
                        tagName = tag,
                        apkUrl = apkUrl,
                        releaseUrl = json.optString("html_url"),
                        body = json.optString("body").ifBlank { null },
                    )
                } finally {
                    connection.disconnect()
                }
            }

        fun isUpdateCheckStale(context: Context): Boolean {
            val last = readLastSuccessfulCheckMillis(context) ?: return true
            return System.currentTimeMillis() - last > MAX_CHECK_AGE_MS
        }

        fun readLastSuccessfulCheckMillis(context: Context): Long? =
            context.applicationContext
                .getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SUCCESSFUL_CHECK, 0L)
                .takeIf { it > 0L }

        fun readStoredUpdateInfo(context: Context): AppUpdateInfo? {
            val prefs = context.applicationContext.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_AVAILABLE_UPDATE, null) ?: return null
            return runCatching {
                val json = JSONObject(raw)
                AppUpdateInfo(
                    versionName = json.getString("versionName"),
                    tagName = json.getString("tagName"),
                    apkUrl = json.getString("apkUrl"),
                    releaseUrl = json.getString("releaseUrl"),
                    body = json.optString("body").ifBlank { null },
                )
            }.getOrElse {
                prefs.edit().remove(KEY_AVAILABLE_UPDATE).apply()
                null
            }
        }

        fun recordSuccessfulCheck(context: Context, info: AppUpdateInfo?) {
            val prefs = context.applicationContext.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
            val editor = prefs.edit()
                .putLong(KEY_LAST_SUCCESSFUL_CHECK, System.currentTimeMillis())
            if (info == null) {
                editor.remove(KEY_AVAILABLE_UPDATE)
            } else {
                editor.putString(
                    KEY_AVAILABLE_UPDATE,
                    JSONObject()
                        .put("versionName", info.versionName)
                        .put("tagName", info.tagName)
                        .put("apkUrl", info.apkUrl)
                        .put("releaseUrl", info.releaseUrl)
                        .put("body", info.body)
                        .toString(),
                )
            }
            editor.apply()
        }

        private fun snapshotState(prefs: SharedPreferences): AppUpdateState {
            val last = prefs.getLong(KEY_LAST_SUCCESSFUL_CHECK, 0L).takeIf { it > 0L }
            val update = prefs.getString(KEY_AVAILABLE_UPDATE, null)?.let { raw ->
                runCatching {
                    val json = JSONObject(raw)
                    AppUpdateInfo(
                        versionName = json.getString("versionName"),
                        tagName = json.getString("tagName"),
                        apkUrl = json.getString("apkUrl"),
                        releaseUrl = json.getString("releaseUrl"),
                        body = json.optString("body").ifBlank { null },
                    )
                }.getOrNull()
            }
            val gate = when {
                update != null -> UpdateGateReason.UpdateAvailable
                last == null || System.currentTimeMillis() - last > MAX_CHECK_AGE_MS -> UpdateGateReason.StaleCheck
                else -> UpdateGateReason.None
            }
            return AppUpdateState(
                availableUpdate = update,
                lastCheckedMillis = last,
                gateReason = gate,
            )
        }

        private suspend fun downloadApk(
            context: Context,
            info: AppUpdateInfo,
            onProgress: (Int) -> Unit,
        ): File = withContext(Dispatchers.IO) {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates")
                .apply { mkdirs() }
            val target = File(dir, "G700Remote-${info.versionName}.apk")
            val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try {
                if (connection.responseCode !in 200..299) {
                    error("APK download failed (${connection.responseCode})")
                }
                val total = connection.contentLengthLong.takeIf { it > 0L }
                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var copied = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            if (total != null) {
                                onProgress(((copied * 100) / total).toInt().coerceIn(0, 100))
                            }
                        }
                    }
                }
                target
            } finally {
                connection.disconnect()
            }
        }

        private fun launchInstaller(context: Context, file: File) {
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        private fun normalizeVersion(value: String): String =
            value.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore('-')
                .ifBlank { value.trim() }

        private fun isNewer(remote: String, local: String): Boolean {
            val r = remote.split('.').map { it.toIntOrNull() ?: 0 }
            val l = local.split('.').map { it.toIntOrNull() ?: 0 }
            val max = maxOf(r.size, l.size)
            for (i in 0 until max) {
                val rv = r.getOrElse(i) { 0 }
                val lv = l.getOrElse(i) { 0 }
                if (rv != lv) return rv > lv
            }
            return false
        }
    }
}
