package com.ares.ewe_man.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object OrderDateFormat {
    fun format(createdAt: String): String {
        val zoned = runCatching {
            Instant.parse(createdAt.trim()).atZone(ZoneId.systemDefault())
        }.getOrNull() ?: return createdAt
        return DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(zoned)
    }
}
