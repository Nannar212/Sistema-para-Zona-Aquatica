package bo.zonaaquatica.movil.data.repo

import bo.zonaaquatica.movil.core.Prefs
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.core.llamarApi
import bo.zonaaquatica.movil.data.api.PiscinaApi
import bo.zonaaquatica.movil.data.model.AsignarMembresiaRequest
import bo.zonaaquatica.movil.data.model.AsistenciaDiaDto
import bo.zonaaquatica.movil.data.model.AuditoriaDto
import bo.zonaaquatica.movil.data.model.CambiarPasswordRequest
import bo.zonaaquatica.movil.data.model.ClienteDto
import bo.zonaaquatica.movil.data.model.ClienteRequest
import bo.zonaaquatica.movil.data.model.CierreCajaDto
import bo.zonaaquatica.movil.data.model.ContadoresDto
import bo.zonaaquatica.movil.data.model.ContenidoDto
import bo.zonaaquatica.movil.data.model.EliminarClienteRequest
import bo.zonaaquatica.movil.data.model.GastoRequest
import bo.zonaaquatica.movil.data.model.GastosRespuestaDto
import bo.zonaaquatica.movil.data.model.HorarioDto
import bo.zonaaquatica.movil.data.model.HorarioRequest
import bo.zonaaquatica.movil.data.model.LoginRequest
import bo.zonaaquatica.movil.data.model.MarcarAsistenciaRequest
import bo.zonaaquatica.movil.data.model.MembresiaDto
import bo.zonaaquatica.movil.data.model.MembresiaRequest
import bo.zonaaquatica.movil.data.model.MotivoRequest
import bo.zonaaquatica.movil.data.model.PagoDto
import bo.zonaaquatica.movil.data.model.PagoEnvelope
import bo.zonaaquatica.movil.data.model.PagoRequest
import bo.zonaaquatica.movil.data.model.PersonalEnvelope
import bo.zonaaquatica.movil.data.model.PersonalRequest
import bo.zonaaquatica.movil.data.model.PersonasHorarioDto
import bo.zonaaquatica.movil.data.model.ResumenCajaDto
import bo.zonaaquatica.movil.data.model.ResumenDashboardDto
import bo.zonaaquatica.movil.data.model.ResumenFinanzasDto
import bo.zonaaquatica.movil.data.model.UsuarioDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class AuthRepo(private val api: PiscinaApi, private val prefs: Prefs) {

    suspend fun login(username: String, password: String): Resultado<UsuarioDto> {
        val r = llamarApi { api.login(LoginRequest(username.trim().lowercase(), password)) }
        return when (r) {
            is Resultado.Ok -> {
                prefs.guardarSesion(r.dato.accessToken, r.dato.refreshToken)
                Resultado.Ok(r.dato.usuario)
            }
            is Resultado.Error -> r
        }
    }

    suspend fun yo(): Resultado<UsuarioDto> = llamarApi { api.yo() }

    suspend fun cambiarPassword(actual: String, nueva: String): Resultado<String> {
        val r = llamarApi { api.cambiarPassword(CambiarPasswordRequest(actual, nueva)) }
        return when (r) {
            is Resultado.Ok -> Resultado.Ok(r.dato.mensaje ?: "Contraseña actualizada")
            is Resultado.Error -> r
        }
    }

    /**
     * Borra la sesion local y devuelve el token que tenia, para avisar al
     * servidor despues. Es sincrono a proposito: salir de la app no puede
     * depender de que el backend responda.
     */
    fun cerrarSesionLocal(): String? {
        val token = prefs.accessToken
        prefs.limpiarSesion()
        return token
    }

    /** Aviso al servidor, best-effort: registra el logout en la auditoria. */
    suspend fun avisarCierreAlServidor(token: String?) {
        if (token.isNullOrBlank()) return
        llamarApi { api.logout("Bearer $token") }
    }
}

class ClientesRepo(private val api: PiscinaApi) {

    suspend fun listar(estado: String, busqueda: String?): Resultado<List<ClienteDto>> =
        llamarApi { api.clientes(estado = estado, q = busqueda?.trim()?.ifBlank { null }) }

    suspend fun contadores(): Resultado<ContadoresDto> = llamarApi { api.contadoresClientes() }

    suspend fun detalle(id: Int): Resultado<ClienteDto> = llamarApi { api.cliente(id) }

    suspend fun crear(body: ClienteRequest): Resultado<ClienteDto?> =
        llamarApi { api.crearCliente(body).cliente }

    suspend fun actualizar(id: Int, body: ClienteRequest): Resultado<ClienteDto?> =
        llamarApi { api.actualizarCliente(id, body).cliente }

    suspend fun renovarMembresia(
        id: Int,
        membresiaId: Int,
        fechaInicio: String?,
        continuar: Boolean,
    ): Resultado<String> = llamarApi {
        api.asignarMembresia(id, AsignarMembresiaRequest(membresiaId, fechaInicio, continuar))
            .mensaje ?: "Membresía asignada"
    }

    suspend fun inactivar(id: Int, motivo: String): Resultado<Unit> =
        llamarApi { api.inactivarCliente(id, MotivoRequest(motivo)); Unit }

    suspend fun restaurar(id: Int): Resultado<Unit> =
        llamarApi { api.restaurarCliente(id); Unit }

    suspend fun eliminar(id: Int, motivo: String, eliminarPagos: Boolean): Resultado<Unit> =
        llamarApi { api.eliminarCliente(id, EliminarClienteRequest(motivo, eliminarPagos)); Unit }

    suspend fun registrarPago(body: PagoRequest): Resultado<PagoEnvelope> =
        llamarApi { api.registrarPago(body) }

    suspend fun pagosDe(clienteId: Int): Resultado<List<PagoDto>> =
        llamarApi { api.pagos(clienteId = clienteId) }
}

/** Membresias (planes) y horarios: catalogos que alimentan varias pantallas. */
class CatalogoRepo(private val api: PiscinaApi) {

    suspend fun membresias(soloActivas: Boolean = false): Resultado<List<MembresiaDto>> =
        llamarApi { api.membresias(if (soloActivas) true else null) }

    suspend fun crearMembresia(body: MembresiaRequest): Resultado<Unit> =
        llamarApi { api.crearMembresia(body); Unit }

    suspend fun actualizarMembresia(id: Int, body: MembresiaRequest): Resultado<Unit> =
        llamarApi { api.actualizarMembresia(id, body); Unit }

    suspend fun eliminarMembresia(id: Int): Resultado<Unit> =
        llamarApi { api.eliminarMembresia(id); Unit }

    suspend fun horarios(soloActivos: Boolean = false): Resultado<List<HorarioDto>> =
        llamarApi { api.horarios(if (soloActivos) true else null) }

    suspend fun personasDe(id: Int): Resultado<PersonasHorarioDto> =
        llamarApi { api.personasDeHorario(id) }

    suspend fun crearHorario(body: HorarioRequest): Resultado<Unit> =
        llamarApi { api.crearHorario(body); Unit }

    suspend fun actualizarHorario(id: Int, body: HorarioRequest): Resultado<Unit> =
        llamarApi { api.actualizarHorario(id, body); Unit }

    suspend fun eliminarHorario(id: Int): Resultado<Unit> =
        llamarApi { api.eliminarHorario(id); Unit }
}

class AsistenciaRepo(private val api: PiscinaApi) {

    suspend fun delDia(fecha: String, horarioId: Int?): Resultado<AsistenciaDiaDto> =
        llamarApi { api.asistenciaDelDia(fecha, horarioId) }

    suspend fun marcar(body: MarcarAsistenciaRequest): Resultado<Unit> =
        llamarApi { api.marcarAsistencia(body); Unit }
}

class PersonalRepo(private val api: PiscinaApi) {

    suspend fun listar(incluirInactivos: Boolean): Resultado<List<UsuarioDto>> =
        llamarApi { api.personal(if (incluirInactivos) true else null) }

    suspend fun crear(body: PersonalRequest): Resultado<PersonalEnvelope> =
        llamarApi { api.crearPersonal(body) }

    suspend fun actualizar(id: Int, body: PersonalRequest): Resultado<Unit> =
        llamarApi { api.actualizarPersonal(id, body); Unit }

    suspend fun resetPassword(id: Int): Resultado<String> = llamarApi {
        api.resetPassword(id).passwordTemporal ?: "—"
    }

    suspend fun cambiarEstado(id: Int): Resultado<Unit> =
        llamarApi { api.cambiarEstadoPersonal(id); Unit }

    suspend fun eliminar(id: Int): Resultado<Unit> =
        llamarApi { api.eliminarPersonal(id); Unit }
}

class DashboardRepo(private val api: PiscinaApi) {

    suspend fun resumen(): Resultado<ResumenDashboardDto> = llamarApi { api.resumenDashboard() }

    suspend fun cajaHoy(): Resultado<ResumenCajaDto> = llamarApi { api.resumenCajaHoy() }
}

// ─── Parte 3: finanzas, gastos, actividad y contenido público ───────────────

class FinanzasRepo(private val api: PiscinaApi) {

    suspend fun resumen(desde: String, hasta: String): Resultado<ResumenFinanzasDto> =
        llamarApi { api.resumenFinanzas(desde, hasta) }

    suspend fun cierreCaja(fecha: String?): Resultado<CierreCajaDto> =
        llamarApi { api.cierreCaja(fecha) }

    suspend fun pagosDelPeriodo(desde: String, hasta: String): Resultado<List<PagoDto>> =
        llamarApi { api.pagos(desde = desde, hasta = hasta) }
}

class GastosRepo(private val api: PiscinaApi) {

    suspend fun categorias(): Resultado<List<String>> = llamarApi { api.categoriasGasto() }

    suspend fun listar(
        desde: String,
        hasta: String,
        categoria: String?,
    ): Resultado<GastosRespuestaDto> = llamarApi { api.gastos(desde, hasta, categoria) }

    suspend fun crear(body: GastoRequest): Resultado<Unit> =
        llamarApi { api.crearGasto(body); Unit }

    suspend fun actualizar(id: Int, body: GastoRequest): Resultado<Unit> =
        llamarApi { api.actualizarGasto(id, body); Unit }

    suspend fun eliminar(id: Int): Resultado<Unit> =
        llamarApi { api.eliminarGasto(id); Unit }
}

class AuditoriaRepo(private val api: PiscinaApi) {

    suspend fun listar(modulo: String?, accion: String?): Resultado<List<AuditoriaDto>> =
        llamarApi { api.auditoria(modulo?.ifBlank { null }, accion?.ifBlank { null }) }
}

class ContenidoRepo(private val api: PiscinaApi) {

    suspend fun obtener(): Resultado<ContenidoDto> = llamarApi { api.contenido() }

    suspend fun guardar(body: ContenidoDto): Resultado<ContenidoDto?> =
        llamarApi { api.guardarContenido(body).contenido }

    /**
     * Sube una imagen ya leída a bytes (viene del selector de fotos de Android).
     * `tipo` es logo | hero | galeria, como espera el backend.
     */
    suspend fun subirImagen(
        bytes: ByteArray,
        nombreArchivo: String,
        mime: String,
        tipo: String,
    ): Resultado<String> = llamarApi {
        val parte = MultipartBody.Part.createFormData(
            "file",
            nombreArchivo,
            bytes.toRequestBody(mime.toMediaTypeOrNull()),
        )
        api.subirImagen(parte, tipo.toRequestBody("text/plain".toMediaType())).url
    }

    suspend fun subirModelo3d(
        bytes: ByteArray,
        nombreArchivo: String,
    ): Resultado<String> = llamarApi {
        val parte = MultipartBody.Part.createFormData(
            "file",
            nombreArchivo,
            bytes.toRequestBody("application/octet-stream".toMediaType()),
        )
        api.subirModelo3d(parte).url
    }
}

/** Descarga el recibo PDF de un pago y devuelve sus bytes. */
class RecibosRepo(private val api: PiscinaApi) {

    suspend fun descargar(pagoId: Int): Resultado<ByteArray> =
        llamarApi { api.reciboPdf(pagoId).use { it.bytes() } }
}
