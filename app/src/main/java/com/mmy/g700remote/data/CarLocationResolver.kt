package com.mmy.g700remote.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CarLocationProvider {
    suspend fun withResolvedAddress(location: CarLocation): CarLocation
    suspend fun phoneLocationWhenAllowed(): CarLocation?
}

object NoopCarLocationProvider : CarLocationProvider {
    override suspend fun withResolvedAddress(location: CarLocation): CarLocation = location
    override suspend fun phoneLocationWhenAllowed(): CarLocation? = null
}

class CarLocationResolver(context: Context) : CarLocationProvider {
    private val appContext = context.applicationContext

    override suspend fun withResolvedAddress(location: CarLocation): CarLocation =
        withContext(Dispatchers.IO) {
            val address = resolveAddress(location.lat, location.lon)
            if (address.isNullOrBlank() || address == location.address) location else location.copy(address = address)
        }

    override suspend fun phoneLocationWhenAllowed(): CarLocation? =
        withContext(Dispatchers.IO) {
            if (!hasLocationPermission()) return@withContext null
            val manager = appContext.getSystemService(LocationManager::class.java) ?: return@withContext null
            val candidates = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            ).mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            candidates.maxByOrNull(Location::getTime)?.let {
                CarLocation(
                    lat = it.latitude,
                    lon = it.longitude,
                    source = CarLocationSource.PhoneBle,
                    updatedAtMillis = System.currentTimeMillis(),
                )
            }
        }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    private fun resolveAddress(lat: Double, lon: Double): String? =
        runCatching {
            val geocoder = Geocoder(appContext, Locale.getDefault())
            val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull() ?: return null
            val parts = listOfNotNull(
                address.featureName?.takeUnless { it.matches(Regex("""\d+[A-Z]?""")) },
                address.thoroughfare,
                address.subLocality,
                address.locality,
                address.countryName,
            )
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            parts.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: address.getAddressLine(0)
        }.getOrNull()
}
