package com.mmy.g700remote.protocol

import android.net.Uri

object NavigationShareParser {
    private val coordinatePattern = Regex("""(-?\d{1,2}(?:\.\d+)?)\s*,\s*(-?\d{1,3}(?:\.\d+)?)""")

    fun parse(text: String?): RemoteCommand.Navigate? {
        val cleaned = text?.trim().orEmpty()
        if (cleaned.isBlank()) return null

        parseUri(cleaned)?.let { return it }
        coordinatePattern.find(cleaned)?.let { match ->
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                return RemoteCommand.Navigate(lat = lat, lon = lon, label = cleaned.take(80))
            }
        }
        return RemoteCommand.Navigate(query = cleaned.take(500))
    }

    private fun parseUri(text: String): RemoteCommand.Navigate? {
        val uri = runCatching { Uri.parse(text) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()

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
                    return RemoteCommand.Navigate(lat = lat, lon = lon, label = uri.getQueryParameter("query_place_id"))
                }
            }
            if (param != "ll" && param != "center") {
                return RemoteCommand.Navigate(query = value.take(500))
            }
        }

        coordinatePattern.find(uri.path.orEmpty())?.let { match ->
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                return RemoteCommand.Navigate(lat = lat, lon = lon)
            }
        }

        return if (scheme == "http" || scheme == "https") {
            RemoteCommand.Navigate(query = text.take(500))
        } else {
            null
        }
    }
}
