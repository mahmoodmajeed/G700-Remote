package com.mmy.g700remote.protocol

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object NavigationShareParser {
    private val coordinatePattern = Regex("""(-?\d{1,2}(?:\.\d+)?)\s*,\s*(-?\d{1,3}(?:\.\d+)?)""")
    private val googleDataCoordinatePattern = Regex("""!3d(-?\d{1,2}(?:\.\d+)?)!4d(-?\d{1,3}(?:\.\d+)?)""")
    private val urlPattern = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val shareUriPattern = Regex("""\b(?:geo|google\.navigation):[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val googlePlacePathPattern = Regex("""/maps/(?:place|search)/([^/?#]+)""", RegexOption.IGNORE_CASE)
    private val googleQueryPattern = Regex("""[?&](?:query|q)=([^&#]+)""", RegexOption.IGNORE_CASE)

    fun parse(text: String?): RemoteCommand.Navigate? {
        val cleaned = text?.trim().orEmpty()
        if (cleaned.isBlank()) return null

        firstUrl(cleaned)?.let { url ->
            parseGoogleDataCoordinates(url, googlePlaceName(url) ?: cleaned.lineSequence().firstOrNull()?.take(80))?.let { return it }
            parseUri(url, fallbackLabel = cleaned.lineSequence().firstOrNull()?.take(80))?.let { return it }
        }
        parseUri(cleaned)?.let { return it }
        parseGoogleDataCoordinates(cleaned, googlePlaceName(cleaned) ?: cleaned.lineSequence().firstOrNull()?.take(80))?.let { return it }
        coordinatePattern.find(cleaned)?.let { match ->
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                return RemoteCommand.Navigate(lat = lat, lon = lon, label = cleaned.take(80))
            }
        }
        return RemoteCommand.Navigate(query = cleaned.take(500))
    }

    fun firstUrl(text: String): String? =
        urlPattern.find(text)?.value
            ?.trimEnd('.', ',', ';', ')', ']', '}')

    fun firstShareUri(text: String): String? =
        firstUrl(text) ?: shareUriPattern.find(text)?.value
            ?.trimEnd('.', ',', ';', ')', ']', '}')

    fun isGoogleMapsUrl(text: String): Boolean {
        val host = runCatching { Uri.parse(text).host?.lowercase() }.getOrNull() ?: return false
        return host == "maps.app.goo.gl" ||
            host == "goo.gl" ||
            host.endsWith("google.com") ||
            host.endsWith("google.com.bh") ||
            host.endsWith("googleusercontent.com")
    }

    fun googlePlaceName(text: String?): String? {
        val raw = text?.takeIf { it.isNotBlank() } ?: return null
        if (!raw.contains("google", ignoreCase = true) && !raw.contains("goo.gl", ignoreCase = true)) return null
        googlePlacePathPattern.find(raw)?.groupValues?.getOrNull(1)?.let { encoded ->
            cleanPlaceLabel(encoded)?.let { return it }
        }
        return googleQueryPattern.find(raw)?.groupValues?.getOrNull(1)?.let { cleanPlaceLabel(it) }
    }

    private fun parseUri(text: String, fallbackLabel: String? = null): RemoteCommand.Navigate? {
        val uri = runCatching { Uri.parse(text) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        val placeName = googlePlaceName(text)

        if (scheme == "geo") {
            val raw = uri.schemeSpecificPart.substringBefore('?')
            val coords = coordinatePattern.find(raw)
            if (coords != null) {
                val lat = coords.groupValues[1].toDoubleOrNull()
                val lon = coords.groupValues[2].toDoubleOrNull()
                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    val label = uri.getQueryParameter("q")?.substringAfter('(')?.substringBefore(')')
                    return RemoteCommand.Navigate(lat = lat, lon = lon, label = label)
                }
            }
            uri.getQueryParameter("q")?.takeIf { it.isNotBlank() }?.let {
                parse(it)?.let { parsed -> return parsed }
            }
        }

        if (scheme == "google.navigation") {
            val query = uri.getQueryParameter("q") ?: uri.schemeSpecificPart
            return parse(query)
        }

        val candidateParams = listOf("query", "q", "destination", "daddr", "ll", "center")
        for (param in candidateParams) {
            val value = uri.getQueryParameter(param)?.takeIf { it.isNotBlank() } ?: continue
            coordinatePattern.find(value)?.let { match ->
                val lat = match.groupValues[1].toDoubleOrNull()
                val lon = match.groupValues[2].toDoubleOrNull()
                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    return RemoteCommand.Navigate(lat = lat, lon = lon, label = placeName ?: uri.getQueryParameter("query_place_id"))
                }
            }
            if (param != "ll" && param != "center") {
                return RemoteCommand.Navigate(query = value.take(500))
            }
        }

        googleDataCoordinatePattern.find(text)?.let { match ->
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                return RemoteCommand.Navigate(
                    lat = lat,
                    lon = lon,
                    label = placeName ?: fallbackLabel?.takeUnless { it.startsWith("http") },
                )
            }
        }

        coordinatePattern.find(uri.path.orEmpty())?.let { match ->
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                return RemoteCommand.Navigate(
                    lat = lat,
                    lon = lon,
                    label = placeName ?: fallbackLabel?.takeUnless { it.startsWith("http") },
                )
            }
        }

        return if ((scheme == "http" || scheme == "https") && isGoogleMapsUrl(text)) {
            placeName?.let { RemoteCommand.Navigate(query = it.take(500)) }
        } else {
            null
        }
    }

    private fun parseGoogleDataCoordinates(text: String, label: String?): RemoteCommand.Navigate? =
        googleDataCoordinatePattern.find(text)?.let { match ->
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                RemoteCommand.Navigate(lat = lat, lon = lon, label = label)
            } else {
                null
            }
        }

    private fun cleanPlaceLabel(encoded: String): String? {
        val decoded = runCatching {
            URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
        }.getOrDefault(encoded)
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""\s*,\s*"""), ", ")
            .trim()
            .trim('-', '|')
        return decoded.takeIf {
            it.length > 2 &&
                !it.startsWith("data=", ignoreCase = true) &&
                !it.startsWith("@") &&
                !it.startsWith("http", ignoreCase = true)
        }
    }
}
