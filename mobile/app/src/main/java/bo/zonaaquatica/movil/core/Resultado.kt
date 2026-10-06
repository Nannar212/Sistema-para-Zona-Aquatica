package bo.zonaaquatica.movil.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/** Resultado de una llamada a la API: o el dato, o un mensaje para mostrar. */
sealed interface Resultado<out T> {
    data class Ok<out T>(val dato: T) : Resultado<T>
    data class Error(val mensaje: String, val codigo: Int? = null) : Resultado<Nothing>
}

inline fun <T> Resultado<T>.alOk(bloque: (T) -> Unit): Resultado<T> {
    if (this is Resultado.Ok) bloque(dato)
    return this
}

inline fun <T> Resultado<T>.alError(bloque: (String) -> Unit): Resultado<T> {
    if (this is Resultado.Error) bloque(mensaje)
    return this
}

/**
 * Envuelve una llamada suspend de Retrofit: la corre en IO y traduce los
 * errores a mensajes en español. El backend siempre responde `{"error": "..."}`.
 */
suspend fun <T> llamarApi(bloque: suspend () -> T): Resultado<T> = withContext(Dispatchers.IO) {
    try {
        Resultado.Ok(bloque())
    } catch (e: HttpException) {
        Resultado.Error(mensajeDeHttp(e), e.code())
    } catch (e: IOException) {
        Resultado.Error(
            "No se pudo conectar con el servidor.\n\n" +
                "Revisa que:\n" +
                "• Docker esté levantado (docker compose up)\n" +
                "• La IP configurada sea la de tu PC\n" +
                "• El celular esté en la misma red WiFi"
        )
    } catch (e: Exception) {
        Resultado.Error(e.message ?: "Ocurrió un error inesperado")
    }
}

private fun mensajeDeHttp(e: HttpException): String {
    val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val delBackend = cuerpo
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { JSONObject(it).optString("error") }.getOrNull() }
        ?.takeIf { it.isNotBlank() && it != "null" }

    if (delBackend != null) return delBackend

    return when (e.code()) {
        401 -> "Tu sesión expiró. Inicia sesión nuevamente."
        403 -> "No tienes permiso para esta acción."
        404 -> "No se encontró el recurso solicitado."
        else -> "Error del servidor (${e.code()})"
    }
}
