package com.fungast.app.location

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidLinkResolver : LinkResolver {

    override suspend fun resolve(url: String): String? = withContext(Dispatchers.IO) {
        var current = url
        // Redirects are followed by hand: HttpURLConnection won't follow them across hosts,
        // and only the URLs matter, not the pages
        repeat(MAX_REDIRECTS) {
            val connection = try {
                URL(current).openConnection() as HttpURLConnection
            } catch (e: IOException) {
                return@withContext null
            }
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                val code = connection.responseCode
                val location = connection.getHeaderField("Location")
                if (code !in 300..399 || location == null) return@withContext current
                val next = URL(URL(current), location)
                // Never downgrade to plain HTTP
                if (next.protocol != "https") return@withContext current
                current = next.toString()
            } catch (e: IOException) {
                return@withContext null
            } finally {
                connection.disconnect()
            }
        }
        current
    }

    private companion object {
        const val MAX_REDIRECTS = 5
        const val TIMEOUT_MS = 10_000
    }
}
