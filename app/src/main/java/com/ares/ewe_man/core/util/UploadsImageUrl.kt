package com.ares.ewe_man.core.util

import com.ares.ewe_man.core.DevServerStore

/** Base del servidor sin `/api/` (imágenes están en `/uploads/...`). */
fun uploadsOrigin(): String = DevServerStore.baseUrl().removeSuffix("api/").trimEnd('/')

/** Ruta devuelta por el API, p.ej. `/uploads/products/xyz.jpg`. */
fun absoluteUploadUrl(path: String): String {
    if (path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)) {
        return path
    }
    val p = if (path.startsWith("/")) path else "/$path"
    return uploadsOrigin() + p
}
