package bo.zonaaquatica.movil.core

import android.content.Context
import bo.zonaaquatica.movil.BuildConfig

/**
 * Guarda los tokens JWT y la URL del backend.
 *
 * Se usa SharedPreferences (almacenamiento privado de la app) porque el
 * Authenticator de OkHttp necesita leer el token de forma sincrona, cosa que
 * DataStore no permite sin bloquear corrutinas.
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("zona_aquatica", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = sp.getString(CLAVE_ACCESS, null)
        set(valor) = sp.edit().putString(CLAVE_ACCESS, valor).apply()

    var refreshToken: String?
        get() = sp.getString(CLAVE_REFRESH, null)
        set(valor) = sp.edit().putString(CLAVE_REFRESH, valor).apply()

    /** URL base, siempre normalizada a `http(s)://host:puerto/api/`. */
    var urlApi: String
        get() = sp.getString(CLAVE_URL, null) ?: normalizarUrl(BuildConfig.DEFAULT_API_URL)
        set(valor) = sp.edit().putString(CLAVE_URL, normalizarUrl(valor)).apply()

    fun guardarSesion(access: String, refresh: String) {
        sp.edit()
            .putString(CLAVE_ACCESS, access)
            .putString(CLAVE_REFRESH, refresh)
            .apply()
    }

    fun limpiarSesion() {
        sp.edit().remove(CLAVE_ACCESS).remove(CLAVE_REFRESH).apply()
    }

    val haySesion: Boolean get() = !accessToken.isNullOrBlank()

    companion object {
        private const val CLAVE_ACCESS = "access_token"
        private const val CLAVE_REFRESH = "refresh_token"
        private const val CLAVE_URL = "api_url"

        /**
         * Acepta lo que el usuario escriba ("10.7.3.147:5000", "192.168.1.5",
         * "http://10.0.2.2:5000/api") y devuelve una URL valida para Retrofit.
         */
        fun normalizarUrl(cruda: String): String {
            var url = cruda.trim()
            if (url.isEmpty()) return normalizarUrl(BuildConfig.DEFAULT_API_URL)
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            url = url.trimEnd('/')
            val esquema = url.substringBefore("://")
            val sinEsquema = url.substringAfter("://")
            val autoridad = sinEsquema.substringBefore('/')
            // Sin puerto explicito se asume el 5000 del backend Flask, pero solo
            // en http: con https lo normal es un proxy en el 443 (ver Caddyfile),
            // asi que forzar el 5000 rompería ese caso.
            if (!autoridad.contains(':') && esquema == "http") {
                val resto = sinEsquema.removePrefix(autoridad)
                url = "$esquema://$autoridad:5000$resto"
            }
            if (!url.endsWith("/api")) {
                url = "$url/api"
            }
            return "$url/"
        }
    }
}
