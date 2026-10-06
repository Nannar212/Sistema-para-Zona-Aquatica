package bo.zonaaquatica.movil.data.api

import bo.zonaaquatica.movil.data.model.AsignarMembresiaRequest
import bo.zonaaquatica.movil.data.model.AsistenciaDiaDto
import bo.zonaaquatica.movil.data.model.AuditoriaDto
import bo.zonaaquatica.movil.data.model.CambiarPasswordRequest
import bo.zonaaquatica.movil.data.model.ClienteDto
import bo.zonaaquatica.movil.data.model.ClienteEnvelope
import bo.zonaaquatica.movil.data.model.ClienteRequest
import bo.zonaaquatica.movil.data.model.CierreCajaDto
import bo.zonaaquatica.movil.data.model.ContadoresDto
import bo.zonaaquatica.movil.data.model.ContenidoDto
import bo.zonaaquatica.movil.data.model.ContenidoEnvelope
import bo.zonaaquatica.movil.data.model.EliminarClienteRequest
import bo.zonaaquatica.movil.data.model.GastoDto
import bo.zonaaquatica.movil.data.model.GastoRequest
import bo.zonaaquatica.movil.data.model.GastosRespuestaDto
import bo.zonaaquatica.movil.data.model.HorarioDto
import bo.zonaaquatica.movil.data.model.HorarioEnvelope
import bo.zonaaquatica.movil.data.model.HorarioRequest
import bo.zonaaquatica.movil.data.model.LoginRequest
import bo.zonaaquatica.movil.data.model.LoginResponse
import bo.zonaaquatica.movil.data.model.MarcarAsistenciaRequest
import bo.zonaaquatica.movil.data.model.MembresiaDto
import bo.zonaaquatica.movil.data.model.MembresiaEnvelope
import bo.zonaaquatica.movil.data.model.MembresiaRequest
import bo.zonaaquatica.movil.data.model.MensajeResponse
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
import bo.zonaaquatica.movil.data.model.UploadRespuestaDto
import bo.zonaaquatica.movil.data.model.UsuarioDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Contrato con el backend Flask. Las rutas van SIN slash inicial para que se
 * resuelvan contra el baseUrl, que siempre termina en `/api/`.
 */
interface PiscinaApi {

    // ─── Auth ───────────────────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun yo(): UsuarioDto

    @POST("auth/cambiar-password")
    suspend fun cambiarPassword(@Body body: CambiarPasswordRequest): MensajeResponse

    // La cabecera va explicita porque el cierre de sesion limpia los tokens
    // ANTES de avisar al servidor, para que el usuario no espere a la red.
    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") autorizacion: String): MensajeResponse

    // ─── Clientes ───────────────────────────────────────────────────────────

    @GET("clientes")
    suspend fun clientes(
        @Query("estado") estado: String,
        @Query("q") q: String? = null,
        @Query("horario_id") horarioId: Int? = null,
    ): List<ClienteDto>

    @GET("clientes/contadores")
    suspend fun contadoresClientes(): ContadoresDto

    @GET("clientes/{id}")
    suspend fun cliente(@Path("id") id: Int): ClienteDto

    @POST("clientes")
    suspend fun crearCliente(@Body body: ClienteRequest): ClienteEnvelope

    @PUT("clientes/{id}")
    suspend fun actualizarCliente(@Path("id") id: Int, @Body body: ClienteRequest): ClienteEnvelope

    @POST("clientes/{id}/membresia")
    suspend fun asignarMembresia(
        @Path("id") id: Int,
        @Body body: AsignarMembresiaRequest,
    ): MensajeResponse

    @PATCH("clientes/{id}/inactivar")
    suspend fun inactivarCliente(@Path("id") id: Int, @Body body: MotivoRequest): ClienteEnvelope

    @PATCH("clientes/{id}/restaurar")
    suspend fun restaurarCliente(@Path("id") id: Int): ClienteEnvelope

    // DELETE con cuerpo: el backend lee motivo + eliminar_pagos del body.
    @HTTP(method = "DELETE", path = "clientes/{id}", hasBody = true)
    suspend fun eliminarCliente(
        @Path("id") id: Int,
        @Body body: EliminarClienteRequest,
    ): MensajeResponse

    // ─── Pagos ──────────────────────────────────────────────────────────────

    @GET("pagos")
    suspend fun pagos(
        @Query("cliente_id") clienteId: Int? = null,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
    ): List<PagoDto>

    @POST("pagos")
    suspend fun registrarPago(@Body body: PagoRequest): PagoEnvelope

    @GET("pagos/resumen-hoy")
    suspend fun resumenCajaHoy(): ResumenCajaDto

    // ─── Membresias (planes) ────────────────────────────────────────────────

    @GET("membresias")
    suspend fun membresias(@Query("activas") soloActivas: Boolean? = null): List<MembresiaDto>

    @POST("membresias")
    suspend fun crearMembresia(@Body body: MembresiaRequest): MembresiaEnvelope

    @PUT("membresias/{id}")
    suspend fun actualizarMembresia(
        @Path("id") id: Int,
        @Body body: MembresiaRequest,
    ): MembresiaEnvelope

    @DELETE("membresias/{id}")
    suspend fun eliminarMembresia(@Path("id") id: Int): MensajeResponse

    // ─── Horarios ───────────────────────────────────────────────────────────

    @GET("horarios")
    suspend fun horarios(@Query("activos") soloActivos: Boolean? = null): List<HorarioDto>

    @GET("horarios/{id}/personas")
    suspend fun personasDeHorario(@Path("id") id: Int): PersonasHorarioDto

    @POST("horarios")
    suspend fun crearHorario(@Body body: HorarioRequest): HorarioEnvelope

    @PUT("horarios/{id}")
    suspend fun actualizarHorario(@Path("id") id: Int, @Body body: HorarioRequest): HorarioEnvelope

    @DELETE("horarios/{id}")
    suspend fun eliminarHorario(@Path("id") id: Int): MensajeResponse

    // ─── Asistencias ────────────────────────────────────────────────────────

    @GET("asistencias/dia")
    suspend fun asistenciaDelDia(
        @Query("fecha") fecha: String,
        @Query("horario_id") horarioId: Int? = null,
    ): AsistenciaDiaDto

    @POST("asistencias")
    suspend fun marcarAsistencia(@Body body: MarcarAsistenciaRequest): MensajeResponse

    // ─── Personal ───────────────────────────────────────────────────────────

    @GET("personal")
    suspend fun personal(@Query("inactivos") incluirInactivos: Boolean? = null): List<UsuarioDto>

    @POST("personal")
    suspend fun crearPersonal(@Body body: PersonalRequest): PersonalEnvelope

    @PUT("personal/{id}")
    suspend fun actualizarPersonal(
        @Path("id") id: Int,
        @Body body: PersonalRequest,
    ): PersonalEnvelope

    @POST("personal/{id}/reset-password")
    suspend fun resetPassword(@Path("id") id: Int): PersonalEnvelope

    @PATCH("personal/{id}/estado")
    suspend fun cambiarEstadoPersonal(@Path("id") id: Int): PersonalEnvelope

    @DELETE("personal/{id}")
    suspend fun eliminarPersonal(@Path("id") id: Int): MensajeResponse

    // ─── Dashboard ──────────────────────────────────────────────────────────

    @GET("dashboard/resumen")
    suspend fun resumenDashboard(): ResumenDashboardDto

    // ─── Finanzas ───────────────────────────────────────────────────────────

    @GET("finanzas/resumen")
    suspend fun resumenFinanzas(
        @Query("desde") desde: String,
        @Query("hasta") hasta: String,
    ): ResumenFinanzasDto

    @GET("finanzas/cierre-caja")
    suspend fun cierreCaja(@Query("fecha") fecha: String? = null): CierreCajaDto

    // ─── Gastos ─────────────────────────────────────────────────────────────

    @GET("gastos/categorias")
    suspend fun categoriasGasto(): List<String>

    @GET("gastos")
    suspend fun gastos(
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
        @Query("categoria") categoria: String? = null,
    ): GastosRespuestaDto

    @POST("gastos")
    suspend fun crearGasto(@Body body: GastoRequest): MensajeResponse

    @PUT("gastos/{id}")
    suspend fun actualizarGasto(@Path("id") id: Int, @Body body: GastoRequest): MensajeResponse

    @DELETE("gastos/{id}")
    suspend fun eliminarGasto(@Path("id") id: Int): MensajeResponse

    // ─── Registro de actividad ──────────────────────────────────────────────

    @GET("auditoria")
    suspend fun auditoria(
        @Query("modulo") modulo: String? = null,
        @Query("accion") accion: String? = null,
        @Query("limit") limite: Int = 200,
    ): List<AuditoriaDto>

    // ─── Contenido de la pagina publica ─────────────────────────────────────

    @GET("contenido")
    suspend fun contenido(): ContenidoDto

    @PUT("contenido")
    suspend fun guardarContenido(@Body body: ContenidoDto): ContenidoEnvelope

    // El backend lee request.files['file'] y request.form['tipo']
    // (tipo: logo | hero | galeria), igual que el editor web.
    @Multipart
    @POST("contenido/upload")
    suspend fun subirImagen(
        @Part archivo: MultipartBody.Part,
        @Part("tipo") tipo: RequestBody,
    ): UploadRespuestaDto

    @Multipart
    @POST("contenido/upload-modelo")
    suspend fun subirModelo3d(@Part archivo: MultipartBody.Part): UploadRespuestaDto

    // ─── Recibo de pago en PDF ──────────────────────────────────────────────

    @Streaming
    @GET("pagos/{id}/recibo")
    suspend fun reciboPdf(@Path("id") id: Int): ResponseBody
}
