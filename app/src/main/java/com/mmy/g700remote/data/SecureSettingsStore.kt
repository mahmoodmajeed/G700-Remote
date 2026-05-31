package com.mmy.g700remote.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import com.mmy.g700remote.ble.ConnectionPreference
import com.mmy.g700remote.ble.TransportKind
import org.json.JSONArray
import org.json.JSONObject

class SecureSettingsStore(context: Context) : SettingsStore {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("g700_secure_settings", Context.MODE_PRIVATE)

    override fun getPairedDevice(): PairedDevice? {
        val json = getEncryptedString(KEY_PAIRED_DEVICE) ?: return null
        return runCatching {
            val obj = JSONObject(json)
            PairedDevice(
                name = obj.optString("name").ifBlank { null },
                address = obj.getString("address"),
                transport = runCatching {
                    TransportKind.valueOf(obj.optString("transport", TransportKind.Ble.name))
                }.getOrDefault(TransportKind.Ble),
            )
        }.getOrNull()
    }

    override fun savePairedDevice(device: PairedDevice) {
        val json = JSONObject()
            .put("name", device.name)
            .put("address", device.address)
            .put("transport", device.transport.name)
            .toString()
        putEncryptedString(KEY_PAIRED_DEVICE, json)
    }

    override fun clearPairedDevice() {
        prefs.edit().remove(KEY_PAIRED_DEVICE).apply()
    }

    override fun getPairingCode(): String = getEncryptedString(KEY_PAIRING_CODE).orEmpty()

    override fun setPairingCode(code: String) {
        val normalized = code.filter { it.isDigit() }.take(PAIRING_CODE_MAX_LEN)
        if (normalized.isBlank()) {
            prefs.edit().remove(KEY_PAIRING_CODE).apply()
        } else {
            putEncryptedString(KEY_PAIRING_CODE, normalized)
        }
    }

    override fun isBleEnabled(): Boolean = prefs.getBoolean(KEY_BLE_ENABLED, true)

    override fun setBleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLE_ENABLED, enabled).apply()
    }

    override fun isLanEnabled(): Boolean = prefs.getBoolean(KEY_LAN_ENABLED, true)

    override fun setLanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LAN_ENABLED, enabled).apply()
    }

    override fun getConnectionPreference(): ConnectionPreference =
        runCatching {
            ConnectionPreference.valueOf(
                prefs.getString(KEY_CONNECTION_PREFERENCE, ConnectionPreference.BleFirst.name)
                    ?: ConnectionPreference.BleFirst.name,
            )
        }.getOrDefault(ConnectionPreference.BleFirst)

    override fun setConnectionPreference(preference: ConnectionPreference) {
        prefs.edit().putString(KEY_CONNECTION_PREFERENCE, preference.name).apply()
    }

    override fun getAppLanguage(): AppLanguage =
        runCatching {
            AppLanguage.valueOf(
                prefs.getString(KEY_APP_LANGUAGE, AppLanguage.English.name)
                    ?: AppLanguage.English.name,
            )
        }.getOrDefault(AppLanguage.English)

    override fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
    }

    override fun getAppTheme(): AppTheme =
        runCatching {
            AppTheme.valueOf(
                prefs.getString(KEY_APP_THEME, AppTheme.G700Horizon.name)
                    ?: AppTheme.G700Horizon.name,
            )
        }.getOrDefault(AppTheme.G700Horizon)

    override fun setAppTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_APP_THEME, theme.name).apply()
    }

    override fun getAppColorMode(): AppColorMode =
        runCatching {
            AppColorMode.valueOf(
                prefs.getString(KEY_APP_COLOR_MODE, AppColorMode.Dark.name)
                    ?: AppColorMode.Dark.name,
            )
        }.getOrDefault(AppColorMode.Dark)

    override fun setAppColorMode(mode: AppColorMode) {
        prefs.edit().putString(KEY_APP_COLOR_MODE, mode.name).apply()
    }

    override fun getNavigationHistory(): List<NavigationHistoryEntry> {
        val raw = prefs.getString(KEY_NAVIGATION_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    add(
                        NavigationHistoryEntry(
                            id = item.optLong("id"),
                            title = item.optString("title").ifBlank { "Destination" },
                            detail = item.optString("detail"),
                            originalText = item.optString("originalText"),
                            sentAtMillis = item.optLong("sentAtMillis"),
                            previewText = item.optString("previewText").ifBlank { null },
                            originalLink = item.optString("originalLink").ifBlank { null },
                        ),
                    )
                }
            }
        }.getOrElse {
            prefs.edit().remove(KEY_NAVIGATION_HISTORY).apply()
            emptyList()
        }
    }

    override fun saveNavigationHistory(history: List<NavigationHistoryEntry>) {
        val array = JSONArray()
        history.take(MAX_NAV_HISTORY).forEach { entry ->
            val item = JSONObject()
                .put("id", entry.id)
                .put("title", entry.title)
                .put("detail", entry.detail)
                .put("originalText", entry.originalText)
                .put("sentAtMillis", entry.sentAtMillis)
            entry.previewText?.let { item.put("previewText", it) }
            entry.originalLink?.let { item.put("originalLink", it) }
            array.put(item)
        }
        prefs.edit().putString(KEY_NAVIGATION_HISTORY, array.toString()).apply()
    }

    override fun areRegionalFeaturesEnabled(): Boolean =
        prefs.getBoolean(KEY_REGIONAL_FEATURES_ENABLED, false)

    override fun setRegionalFeaturesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REGIONAL_FEATURES_ENABLED, enabled).apply()
    }

    override fun isLocalAuthEnabled(): Boolean = prefs.getBoolean(KEY_LOCAL_AUTH, true)

    override fun setLocalAuthEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCAL_AUTH, enabled).apply()
    }

    override fun getLockStateMapping(): LockStateMapping =
        LockStateMapping.State1Locked

    override fun setLockStateMapping(mapping: LockStateMapping) {
        prefs.edit().putString(KEY_LOCK_MAPPING, LockStateMapping.State1Locked.name).apply()
    }

    override fun isLoggingEnabled(): Boolean = prefs.getBoolean(KEY_LOGGING_ENABLED, false)

    override fun setLoggingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOGGING_ENABLED, enabled).apply()
    }

    private fun putEncryptedString(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val cipherText = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val encodedIv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encodedCipherText = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        prefs.edit().putString(key, "$encodedIv:$encodedCipherText").apply()
    }

    private fun getEncryptedString(key: String): String? {
        val encoded = prefs.getString(key, null) ?: return null
        return runCatching {
            val parts = encoded.split(":", limit = 2)
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        }.getOrElse {
            prefs.edit().remove(key).apply()
            null
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "g700_remote_settings_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PAIRING_CODE_MAX_LEN = 8
        private const val KEY_PAIRED_DEVICE = "paired_device"
        private const val KEY_PAIRING_CODE = "pairing_code"
        private const val KEY_BLE_ENABLED = "ble_enabled"
        private const val KEY_LAN_ENABLED = "lan_enabled"
        private const val KEY_CONNECTION_PREFERENCE = "connection_preference"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_APP_COLOR_MODE = "app_color_mode"
        private const val KEY_NAVIGATION_HISTORY = "navigation_history"
        private const val KEY_REGIONAL_FEATURES_ENABLED = "regional_features_enabled"
        private const val KEY_LOCAL_AUTH = "local_auth"
        private const val KEY_LOCK_MAPPING = "lock_mapping"
        private const val KEY_LOGGING_ENABLED = "logging_enabled"
        private const val MAX_NAV_HISTORY = 50
    }
}
