package bo.zonaaquatica.movil.data.api

import bo.zonaaquatica.movil.core.Prefs
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Reescribe esquema/host/puerto de cada peticion con la URL configurada en
 * Prefs. Asi la URL del backend se puede cambiar desde la app sin reconstruir
 * Retrofit ni reiniciar nada. El prefijo de ruta siempre es `/api/` porque
 * Prefs.normalizarUrl lo garantiza.
 */
class InterceptorUrlBase(private val prefs: Prefs) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val configurada = prefs.urlApi.toHttpUrlOrNull()
            ?: return chain.proceed(original)

        val nueva = original.url.newBuilder()
            .scheme(configurada.scheme)
            .host(configurada.host)
            .port(configurada.port)
            .build()

        return chain.proceed(original.newBuilder().url(nueva).build())
    }
}

/** Agrega `Authorization: Bearer <access_token>` salvo en login y refresh. */
class InterceptorAuth(private val prefs: Prefs) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        val ruta = peticion.url.encodedPath
        if (ruta.endsWith("/auth/login") || ruta.endsWith("/auth/refresh")) {
            return chain.proceed(peticion)
        }
        val token = prefs.accessToken ?: return chain.proceed(peticion)
        return chain.proceed(
            peticion.newBuilder().header("Authorization", "Bearer $token").build()
        )
    }
}

/**
 * Ante un 401 renueva el access token con el refresh token y reintenta la
 * peticion original, igual que el interceptor de axios del frontend web.
 */
class AutenticadorJwt(
    private val prefs: Prefs,
    private val alCerrarSesion: () -> Unit,
) : Authenticator {

    private val clienteSimple = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        val ruta = response.request.url.encodedPath
        if (ruta.endsWith("/auth/login") || ruta.endsWith("/auth/refresh")) return null
        if (contarReintentos(response) >= 2) return null

        val refresh = prefs.refreshToken
        if (refresh.isNullOrBlank()) {
            alCerrarSesion()
            return null
        }

        val tokenUsado = response.request.header("Authorization")?.removePrefix("Bearer ")

        val nuevoToken = synchronized(this) {
            // Si otro hilo ya lo renovo mientras esperabamos, usamos ese.
            val actual = prefs.accessToken
            if (!actual.isNullOrBlank() && actual != tokenUsado) {
                actual
            } else {
                renovar(refresh)?.also { prefs.accessToken = it }
            }
        }

        if (nuevoToken == null) {
            prefs.limpiarSesion()
            alCerrarSesion()
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $nuevoToken")
            .build()
    }

    private fun renovar(refreshToken: String): String? {
        val url = (prefs.urlApi + "auth/refresh").toHttpUrlOrNull() ?: return null
        val peticion = Request.Builder()
            .url(url)
            .post("".toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $refreshToken")
            .build()

        return runCatching {
            clienteSimple.newCall(peticion).execute().use { r ->
                if (!r.isSuccessful) return@use null
                val cuerpo = r.body?.string() ?: return@use null
                JSONObject(cuerpo).optString("access_token").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private fun contarReintentos(response: Response): Int {
        var n = 1
        var previa = response.priorResponse
        while (previa != null) {
            n++
            previa = previa.priorResponse
        }
        return n
    }
}

object FabricaApi {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * El host del baseUrl es un marcador: InterceptorUrlBase lo sustituye en
     * cada peticion por lo que el usuario tenga configurado.
     */
    private const val URL_MARCADOR = "http://localhost/api/"

    fun crear(prefs: Prefs, alCerrarSesion: () -> Unit): PiscinaApi {
        val registro = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val cliente = OkHttpClient.Builder()
            .addInterceptor(InterceptorUrlBase(prefs))
            .addInterceptor(InterceptorAuth(prefs))
            .addInterceptor(registro)
            .authenticator(AutenticadorJwt(prefs, alCerrarSesion))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(URL_MARCADOR)
            .client(cliente)
            // withNullSerialization: el backend distingue "campo ausente" de
            // "campo en null" (`if campo in data`), igual que el frontend web.
            .addConverterFactory(MoshiConverterFactory.create(moshi).withNullSerialization())
            .build()
            .create(PiscinaApi::class.java)
    }
}
