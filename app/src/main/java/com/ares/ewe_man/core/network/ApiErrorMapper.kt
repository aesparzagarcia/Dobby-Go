package com.ares.ewe_man.core.network

import com.google.gson.JsonParser
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import retrofit2.HttpException

/**
 * High-level classification for API / network failures shown in UI.
 */
enum class ApiErrorKind {
    CONNECTION,
    AUTHORIZATION,
    CLIENT,
    SERVER,
    UNKNOWN,
}

data class UserFacingApiError(
    val kind: ApiErrorKind,
    val label: String,
    val detail: String,
) {
    /** Un solo bloque en español; el título resume el tipo de fallo. */
    fun formattedMessage(): String = "$label. $detail"
}

private const val MSG_NETWORK_TITLE = "Problema de red"
private const val MSG_NETWORK_BODY =
    "No tienes internet o la conexión es inestable. Comprueba tu Wi-Fi o datos móviles e inténtalo de nuevo."

private const val MSG_SERVER_TITLE = "Servicio no disponible"
private const val MSG_SERVER_BODY =
    "Hay un problema de conexión con el servicio en este momento. Vuelve a intentarlo más tarde."

private const val MSG_SSL_BODY =
    "No pudimos establecer una conexión segura. Revisa la fecha y hora del teléfono y tu red Wi-Fi o datos móviles."

fun Throwable.unwrap(): Throwable {
    var current = this
    while (current.cause != null && current.cause !== current) {
        current = current.cause!!
    }
    return current
}

private fun parseHttpErrorBody(e: HttpException): String? {
    return try {
        val body = e.response()?.errorBody()?.string().orEmpty()
        if (body.isBlank()) return null
        val el = JsonParser.parseString(body).asJsonObject
        when {
            el.has("error") && !el.get("error").isJsonNull -> el.get("error").asString.trim()
            el.has("message") && !el.get("message").isJsonNull -> el.get("message").asString.trim()
            else -> null
        }?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }
}

fun Throwable.toUserFacingApiError(): UserFacingApiError {
    val t = unwrap()
    return when (t) {
        is HttpException -> mapHttpException(t)
        is SocketTimeoutException,
        is UnknownHostException,
        is ConnectException,
        -> UserFacingApiError(ApiErrorKind.CONNECTION, MSG_NETWORK_TITLE, MSG_NETWORK_BODY)
        is SSLException -> UserFacingApiError(
            ApiErrorKind.CONNECTION,
            MSG_NETWORK_TITLE,
            MSG_SSL_BODY,
        )
        is IOException -> UserFacingApiError(ApiErrorKind.CONNECTION, MSG_NETWORK_TITLE, MSG_NETWORK_BODY)
        else -> UserFacingApiError(
            ApiErrorKind.UNKNOWN,
            "No pudimos completar la acción",
            "Ocurrió un error inesperado. Inténtalo de nuevo en un momento.",
        )
    }
}

private fun mapHttpException(e: HttpException): UserFacingApiError {
    val code = e.code()
    val serverMsg = parseHttpErrorBody(e)
    return when (code) {
        401, 403 -> UserFacingApiError(
            ApiErrorKind.AUTHORIZATION,
            "Sesión o permisos",
            serverMsg ?: "Tu sesión expiró o no tienes permiso para esta acción. Vuelve a iniciar sesión.",
        )
        in 500..599 -> UserFacingApiError(
            ApiErrorKind.SERVER,
            MSG_SERVER_TITLE,
            MSG_SERVER_BODY,
        )
        404 -> UserFacingApiError(
            ApiErrorKind.CLIENT,
            "No encontrado",
            serverMsg ?: "No encontramos lo que pediste. Puede que ya no exista.",
        )
        in 400..499 -> UserFacingApiError(
            ApiErrorKind.CLIENT,
            "No se pudo procesar la solicitud",
            serverMsg ?: "Los datos enviados no son válidos o la operación no está permitida.",
        )
        else -> UserFacingApiError(
            ApiErrorKind.UNKNOWN,
            "Respuesta inesperada",
            serverMsg ?: "El servicio devolvió una respuesta que no esperábamos. Inténtalo más tarde.",
        )
    }
}

fun Throwable.toUserFacingMessage(): String = toUserFacingApiError().formattedMessage()
