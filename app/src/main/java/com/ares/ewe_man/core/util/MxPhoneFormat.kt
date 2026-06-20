package com.ares.ewe_man.core.util

/**
 * Parity with backend [normalizeMxPhone] / [parseRequiredMxPhone] (México +52).
 */
object MxPhoneFormat {
    private const val COUNTRY_CODE = "52"
    private const val NATIONAL_LENGTH = 10

    /** Returns E.164 (+52XXXXXXXXXX) or null if invalid. */
    fun toE164(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return null

        val national = when (digits.length) {
            NATIONAL_LENGTH -> digits
            12 -> if (digits.startsWith(COUNTRY_CODE)) digits.drop(2) else return null
            else -> return null
        }
        if (national.length != NATIONAL_LENGTH) return null
        return "+$COUNTRY_CODE$national"
    }

    /** National 10 digits for UI, or null if invalid. */
    fun toNationalDigits(raw: String): String? = toE164(raw)?.removePrefix("+$COUNTRY_CODE")
}
