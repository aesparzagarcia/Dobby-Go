package com.ares.ewe_man.core

import android.content.Context
import com.ares.ewe_man.BuildConfig
import java.net.URI

/**
 * En el flavor `dev`, la IP guardada en Perfil reemplaza [BuildConfig.BASE_URL]
 * al siguiente arranque. Producción ignora lo guardado.
 */
object DevServerStore {
    private const val PREFS = "dobbygo_dev_server"
    private const val KEY_HOST = "api_host"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun savedRaw(): String {
        if (BuildConfig.ENVIRONMENT != "dev") return ""
        return prefs()?.getString(KEY_HOST, null)?.trim().orEmpty()
    }

    fun baseUrl(): String {
        val fallback = BuildConfig.BASE_URL
        val raw = savedRaw()
        if (raw.isEmpty()) return fallback
        return normalize(raw) ?: fallback
    }

    fun displayHost(): String {
        return try {
            val uri = URI(baseUrl())
            val host = uri.host ?: return baseUrl()
            if (uri.port > 0) "$host:${uri.port}" else host
        } catch (_: Exception) {
            baseUrl()
        }
    }

    fun save(raw: String): Boolean {
        if (BuildConfig.ENVIRONMENT != "dev") return false
        val trimmed = raw.trim()
        if (normalize(trimmed) == null) return false
        prefs()?.edit()?.putString(KEY_HOST, trimmed)?.commit()
        return true
    }

    fun clear() {
        prefs()?.edit()?.remove(KEY_HOST)?.commit()
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** `192.168.1.10` → `http://192.168.1.10:3001/api/`. */
    private fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "http://$trimmed"
        }
        val uri = try {
            URI(withScheme)
        } catch (_: Exception) {
            return null
        }
        val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val port = when {
            uri.port > 0 -> uri.port
            scheme == "http" -> 3001
            else -> -1
        }
        val rawPath = uri.path.orEmpty()
        val path = when {
            rawPath.isEmpty() || rawPath == "/" -> "/api/"
            rawPath == "/api" || rawPath.endsWith("/api") -> "$rawPath/"
            rawPath.contains("/api/") -> if (rawPath.endsWith("/")) rawPath else "$rawPath/"
            rawPath.endsWith("/") -> "${rawPath}api/"
            else -> "$rawPath/api/"
        }
        val portPart = if (port > 0) ":$port" else ""
        return "$scheme://$host$portPart$path"
    }
}
