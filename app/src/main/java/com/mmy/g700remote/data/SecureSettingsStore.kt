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
import com.mmy.g700remote.cloud.BoundCar
import com.mmy.g700remote.cloud.CloudAccount
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

    override fun getCloudAccount(): CloudAccount? {
        val json = getEncryptedString(KEY_CLOUD_ACCOUNT) ?: return null
        return runCatching {
            val obj = JSONObject(json)
            val token = obj.optString("token")
            if (token.isBlank()) return null
            CloudAccount(
                userId = obj.optString("userId"),
                email = obj.optString("email"),
                token = token,
            )
        }.getOrNull()
    }

    override fun saveCloudAccount(account: CloudAccount) {
        val json = JSONObject()
            .put("userId", account.userId)
            .put("email", account.email)
            .put("token", account.token)
            .toString()
        putEncryptedString(KEY_CLOUD_ACCOUNT, json)
    }

    override fun clearCloudAccount() {
        prefs.edit().remove(KEY_CLOUD_ACCOUNT).apply()
    }

    override fun getBoundCar(): BoundCar? {
        val json = getEncryptedString(KEY_BOUND_CAR) ?: return null
        return runCatching {
            val obj = JSONObject(json)
            val carId = obj.optString("carId")
            if (carId.isBlank()) return null
            BoundCar(
                carId = carId,
                apiBase = obj.optString("apiBase"),
                relayBase = obj.optString("relayBase"),
                pairingCode = obj.optString("pairingCode"),
                pairToken = obj.optString("pairToken"),
                name = obj.optString("name").ifBlank { null },
                boundAtMillis = obj.optLong("boundAtMillis").takeIf { it > 0L } ?: System.currentTimeMillis(),
            )
        }.getOrNull()
    }

    override fun saveBoundCar(car: BoundCar) {
        val json = JSONObject()
            .put("carId", car.carId)
            .put("apiBase", car.apiBase)
            .put("relayBase", car.relayBase)
            .put("pairingCode", car.pairingCode)
            .put("pairToken", car.pairToken)
            .put("name", car.name)
            .put("boundAtMillis", car.boundAtMillis)
            .toString()
        putEncryptedString(KEY_BOUND_CAR, json)
    }

    override fun clearBoundCar() {
        prefs.edit().remove(KEY_BOUND_CAR).apply()
    }

    override fun isCloudEnabled(): Boolean = prefs.getBoolean(KEY_CLOUD_ENABLED, true)

    override fun setCloudEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_ENABLED, enabled).apply()
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
                prefs.getString(KEY_APP_THEME, AppTheme.HimalayaSlate.name)
                    ?: AppTheme.HimalayaSlate.name,
            )
        }.getOrDefault(AppTheme.HimalayaSlate)

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

    override fun getAppIconTheme(): AppIconTheme =
        runCatching {
            AppIconTheme.valueOf(
                prefs.getString(KEY_APP_ICON_THEME, AppIconTheme.GtBlack.name)
                    ?: AppIconTheme.GtBlack.name,
            )
        }.getOrDefault(AppIconTheme.GtBlack)

    override fun setAppIconTheme(theme: AppIconTheme) {
        prefs.edit().putString(KEY_APP_ICON_THEME, theme.name).apply()
    }

    override fun isBleWakeEnabled(): Boolean = prefs.getBoolean(KEY_BLE_WAKE_ENABLED, true)

    override fun setBleWakeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLE_WAKE_ENABLED, enabled).apply()
    }

    override fun getCompanionAssociationId(): Int? =
        prefs.getInt(KEY_COMPANION_ASSOCIATION_ID, NO_ASSOCIATION_ID).takeIf { it > 0 }

    override fun setCompanionAssociationId(id: Int?) {
        prefs.edit().apply {
            if (id != null && id > 0) {
                putInt(KEY_COMPANION_ASSOCIATION_ID, id)
            } else {
                remove(KEY_COMPANION_ASSOCIATION_ID)
            }
        }.apply()
    }

    fun isBleWakeScanRegistered(): Boolean = prefs.getBoolean(KEY_BLE_WAKE_SCAN_REGISTERED, false)

    fun setBleWakeScanRegistered(registered: Boolean) {
        prefs.edit().putBoolean(KEY_BLE_WAKE_SCAN_REGISTERED, registered).apply()
    }

    fun getLastBleWakeMillis(): Long? =
        prefs.getLong(KEY_LAST_BLE_WAKE_MILLIS, 0L).takeIf { it > 0L }

    fun setLastBleWakeMillis(value: Long) {
        prefs.edit().putLong(KEY_LAST_BLE_WAKE_MILLIS, value).apply()
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
                            navLat = item.optNullableDouble("navLat"),
                            navLon = item.optNullableDouble("navLon"),
                            navLabel = item.optString("navLabel").ifBlank { null },
                            navQuery = item.optString("navQuery").ifBlank { null },
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
            entry.navLat?.let { item.put("navLat", it) }
            entry.navLon?.let { item.put("navLon", it) }
            entry.navLabel?.let { item.put("navLabel", it) }
            entry.navQuery?.let { item.put("navQuery", it) }
            array.put(item)
        }
        prefs.edit().putString(KEY_NAVIGATION_HISTORY, array.toString()).apply()
    }

    override fun getLastVehicleStatus(): VehicleStatusSnapshot? {
        val raw = prefs.getString(KEY_LAST_VEHICLE_STATUS, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            VehicleStatusSnapshot(
                telemetry = vehicleTelemetryFromJson(obj.getJSONObject("telemetry")),
                lastRefreshMillis = obj.optLong("lastRefreshMillis").takeIf { it > 0L } ?: return null,
                carLocation = obj.optJSONObject("carLocation")?.toCarLocation(),
            )
        }.getOrElse {
            prefs.edit().remove(KEY_LAST_VEHICLE_STATUS).apply()
            null
        }
    }

    override fun saveLastVehicleStatus(snapshot: VehicleStatusSnapshot) {
        val json = JSONObject()
            .put("lastRefreshMillis", snapshot.lastRefreshMillis)
            .put("telemetry", snapshot.telemetry.toJson())
        snapshot.carLocation?.let { json.put("carLocation", it.toJson()) }
        prefs.edit().putString(KEY_LAST_VEHICLE_STATUS, json.toString()).apply()
    }

    override fun getCarLocationPreference(): CarLocationPreference =
        runCatching {
            CarLocationPreference.valueOf(
                prefs.getString(KEY_CAR_LOCATION_PREFERENCE, CarLocationPreference.DisplayMirror.name)
                    ?: CarLocationPreference.DisplayMirror.name,
            )
        }.getOrDefault(CarLocationPreference.DisplayMirror)

    override fun setCarLocationPreference(preference: CarLocationPreference) {
        prefs.edit().putString(KEY_CAR_LOCATION_PREFERENCE, preference.name).apply()
    }

    override fun isConnectedNotificationEnabled(): Boolean =
        prefs.getBoolean(KEY_CONNECTED_NOTIFICATION_ENABLED, true)

    override fun setConnectedNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONNECTED_NOTIFICATION_ENABLED, enabled).apply()
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

    override fun getLastSeenReleaseNotesVersion(): String? =
        prefs.getString(KEY_LAST_SEEN_RELEASE_NOTES_VERSION, null)

    override fun setLastSeenReleaseNotesVersion(version: String) {
        prefs.edit().putString(KEY_LAST_SEEN_RELEASE_NOTES_VERSION, version).apply()
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
        private const val KEY_CLOUD_ACCOUNT = "cloud_account"
        private const val KEY_BOUND_CAR = "bound_car"
        private const val KEY_CLOUD_ENABLED = "cloud_enabled"
        private const val KEY_BLE_ENABLED = "ble_enabled"
        private const val KEY_LAN_ENABLED = "lan_enabled"
        private const val KEY_CONNECTION_PREFERENCE = "connection_preference"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_APP_COLOR_MODE = "app_color_mode"
        private const val KEY_APP_ICON_THEME = "app_icon_theme"
        private const val KEY_BLE_WAKE_ENABLED = "ble_wake_enabled"
        private const val KEY_BLE_WAKE_SCAN_REGISTERED = "ble_wake_scan_registered"
        private const val KEY_LAST_BLE_WAKE_MILLIS = "last_ble_wake_millis"
        private const val KEY_COMPANION_ASSOCIATION_ID = "companion_association_id"
        private const val KEY_NAVIGATION_HISTORY = "navigation_history"
        private const val KEY_LAST_VEHICLE_STATUS = "last_vehicle_status"
        private const val KEY_CAR_LOCATION_PREFERENCE = "car_location_preference"
        private const val KEY_CONNECTED_NOTIFICATION_ENABLED = "connected_notification_enabled"
        private const val KEY_REGIONAL_FEATURES_ENABLED = "regional_features_enabled"
        private const val KEY_LOCAL_AUTH = "local_auth"
        private const val KEY_LOCK_MAPPING = "lock_mapping"
        private const val KEY_LOGGING_ENABLED = "logging_enabled"
        private const val KEY_LAST_SEEN_RELEASE_NOTES_VERSION = "last_seen_release_notes_version"
        private const val MAX_NAV_HISTORY = 50
        private const val NO_ASSOCIATION_ID = -1
    }
}

private fun JSONObject.optNullableDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key).takeUnless { it.isNaN() }
}

private fun JSONObject.optNullableInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private fun JSONObject.optNullableBoolean(key: String): Boolean? {
    if (!has(key) || isNull(key)) return null
    return optBoolean(key)
}

private fun VehicleTelemetry.toJson(): JSONObject =
    JSONObject()
        .putNullable("lockState", lockState)
        .putNullable("cabinTemp", cabinTemp)
        .putNullable("outdoorTemp", outdoorTemp)
        .putNullable("coolantTemp", coolantTemp)
        .putNullable("batterySoc", batterySoc)
        .putNullable("fuelPercent", fuelPercent)
        .putNullable("acOn", acOn)
        .putNullable("chargingState", chargingState)
        .putNullable("chargeRemainTime", chargeRemainTime)
        .putNullable("packVoltage", packVoltage)
        .putNullable("packCurrent", packCurrent)
        .putNullable("packPower", packPower)
        .putNullable("chargeMode", chargeMode)
        .putNullable("parkingChargeTargetSoc", parkingChargeTargetSoc)
        .putNullable("parkingChargeEtaMin", parkingChargeEtaMin)
        .putNullable("dischargeEtaMin", dischargeEtaMin)
        .putNullable("safetySocFloor", safetySocFloor)
        .putNullable("raceChargeActive", raceChargeActive)
        .putNullable("raceChargeTarget", raceChargeTarget)
        .putNullable("raceChargeEtaMin", raceChargeEtaMin)
        .putNullable("tempLeft", tempLeft)
        .putNullable("tempRight", tempRight)
        .putNullable("fanSpeed", fanSpeed)
        .putNullable("circulation", circulation)
        .putNullable("fastCool", fastCool)
        .putNullable("fastHeat", fastHeat)
        .putNullable("autoDefrost", autoDefrost)
        .putNullable("rearDefrost", rearDefrost)
        .putNullable("parkingChargeSwitchState", parkingChargeSwitchState)
        .putNullable("parkingChargeMode", parkingChargeMode)
        .putNullable("hazardsOn", hazardsOn)
        .putNullable("drlOn", drlOn)
        .put("seatHeatLevels", seatHeatLevels.toJsonObject())
        .put("seatVentLevels", seatVentLevels.toJsonObject())

private fun vehicleTelemetryFromJson(obj: JSONObject): VehicleTelemetry =
    VehicleTelemetry(
        lockState = obj.optNullableInt("lockState"),
        cabinTemp = obj.optNullableDouble("cabinTemp"),
        outdoorTemp = obj.optNullableDouble("outdoorTemp"),
        coolantTemp = obj.optNullableDouble("coolantTemp"),
        batterySoc = obj.optNullableInt("batterySoc"),
        fuelPercent = obj.optNullableInt("fuelPercent"),
        acOn = obj.optNullableBoolean("acOn"),
        chargingState = obj.optNullableInt("chargingState"),
        chargeRemainTime = obj.optNullableInt("chargeRemainTime"),
        packVoltage = obj.optNullableDouble("packVoltage"),
        packCurrent = obj.optNullableDouble("packCurrent"),
        packPower = obj.optNullableDouble("packPower"),
        chargeMode = obj.optString("chargeMode").ifBlank { null },
        parkingChargeTargetSoc = obj.optNullableInt("parkingChargeTargetSoc"),
        parkingChargeEtaMin = obj.optNullableInt("parkingChargeEtaMin"),
        dischargeEtaMin = obj.optNullableInt("dischargeEtaMin"),
        safetySocFloor = obj.optNullableInt("safetySocFloor"),
        raceChargeActive = obj.optNullableBoolean("raceChargeActive"),
        raceChargeTarget = obj.optNullableInt("raceChargeTarget"),
        raceChargeEtaMin = obj.optNullableInt("raceChargeEtaMin"),
        tempLeft = obj.optNullableDouble("tempLeft"),
        tempRight = obj.optNullableDouble("tempRight"),
        fanSpeed = obj.optNullableInt("fanSpeed"),
        circulation = obj.optNullableInt("circulation"),
        fastCool = obj.optNullableBoolean("fastCool"),
        fastHeat = obj.optNullableBoolean("fastHeat"),
        autoDefrost = obj.optNullableBoolean("autoDefrost"),
        rearDefrost = obj.optNullableBoolean("rearDefrost"),
        parkingChargeSwitchState = obj.optNullableInt("parkingChargeSwitchState"),
        parkingChargeMode = obj.optNullableInt("parkingChargeMode"),
        hazardsOn = obj.optNullableBoolean("hazardsOn"),
        drlOn = obj.optNullableBoolean("drlOn"),
        seatHeatLevels = obj.optJSONObject("seatHeatLevels").toStringIntMap(),
        seatVentLevels = obj.optJSONObject("seatVentLevels").toStringIntMap(),
    )

private fun CarLocation.toJson(): JSONObject =
    JSONObject()
        .put("lat", lat)
        .put("lon", lon)
        .putNullable("address", address)
        .put("source", source.name)
        .put("updatedAtMillis", updatedAtMillis)

private fun JSONObject.toCarLocation(): CarLocation? {
    val lat = optNullableDouble("lat") ?: return null
    val lon = optNullableDouble("lon") ?: return null
    return CarLocation(
        lat = lat,
        lon = lon,
        address = optString("address").ifBlank { null },
        source = runCatching {
            CarLocationSource.valueOf(optString("source", CarLocationSource.DisplayMirror.name))
        }.getOrDefault(CarLocationSource.DisplayMirror),
        updatedAtMillis = optLong("updatedAtMillis").takeIf { it > 0L } ?: System.currentTimeMillis(),
    )
}

private fun JSONObject.putNullable(key: String, value: Any?): JSONObject {
    if (value != null) put(key, value)
    return this
}

private fun Map<String, Int>.toJsonObject(): JSONObject =
    JSONObject().also { obj ->
        forEach { (key, value) -> obj.put(key, value) }
    }

private fun JSONObject?.toStringIntMap(): Map<String, Int> {
    val obj = this ?: return emptyMap()
    return buildMap {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            put(key, obj.optInt(key))
        }
    }
}
