package com.ares.ewe_man.core.util

import com.ares.ewe_man.BuildConfig

/** Base del servidor sin `/api/` (imágenes están en `/uploads/...`). */
fun uploadsOrigin(): String = BuildConfig.BASE_URL.removeSuffix("api/").trimEnd('/')

/** Ruta devuelta por el API, p.ej. `/uploads/products/xyz.jpg`. */
fun absoluteUploadUrl(path: String): String {
    if (path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)) {
        return path
    }
    val p = if (path.startsWith("/")) path else "/$path"
    return uploadsOrigin() + p
}
