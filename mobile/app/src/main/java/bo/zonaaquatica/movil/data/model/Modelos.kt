package bo.zonaaquatica.movil.data.model

import com.squareup.moshi.Json

/*
 * Espejo de los `to_dict()` del backend Flask (backend/app/models/).
 * Todo campo que el backend pueda omitir se declara con valor por defecto
 * para que Moshi no falle si la API cambia.
 */

// ─── Auth / usuarios ────────────────────────────────────────────────────────

data class UsuarioDto(
    val id: Int,
    val nombres: String = "",
    val apellidos: String = "",
    @param:Json(name = "nombre_completo") val nombreCompleto: String = "",
    val ci: String = "",
    val username: String = "",
    val edad: Int? = null,
    val rol: String = "recepcionista",
    val activo: Boolean = true,
    @param:Json(name = "es_password_temporal") val esPasswordTemporal: Boolean = false,
    @param:Json(name = "ultimo_login") val ultimoLogin: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
) {
    val esAdmin: Boolean get() = rol == "admin"
}

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(
    @param:Json(name = "access_token") val accessToken: String,
    @param:Json(name = "refresh_token") val refreshToken: String,
    val usuario: UsuarioDto,
)

data class CambiarPasswordRequest(
    @param:Json(name = "password_actual") val passwordActual: String,
    @param:Json(name = "password_nueva") val passwordNueva: String,
)

data class MensajeResponse(val mensaje: String? = null, val error: String? = null)

data class ErrorResponse(val error: String? = null)

// ─── Membresias (planes) ────────────────────────────────────────────────────

data class MembresiaDto(
    val id: Int,
    val nombre: String = "",
    val descripcion: String = "",
    @param:Json(name = "duracion_dias") val duracionDias: Int = 30,
    @param:Json(name = "numero_clases") val numeroClases: Int? = null,
    val precio: Double = 0.0,
    @param:Json(name = "dias_acceso") val diasAcceso: List<Int> = listOf(0, 1, 2, 3, 4),
    val activa: Boolean = true,
)

data class MembresiaRequest(
    val nombre: String,
    val descripcion: String,
    @param:Json(name = "duracion_dias") val duracionDias: Int,
    @param:Json(name = "numero_clases") val numeroClases: Int?,
    val precio: Double,
    @param:Json(name = "dias_acceso") val diasAcceso: List<Int>,
    val activa: Boolean,
)

data class MembresiaEnvelope(val mensaje: String? = null, val membresia: MembresiaDto? = null)

// ─── Membresia asignada a un cliente ────────────────────────────────────────

data class ClienteMembresiaDto(
    val id: Int,
    @param:Json(name = "cliente_id") val clienteId: Int = 0,
    @param:Json(name = "membresia_id") val membresiaId: Int? = null,
    @param:Json(name = "membresia_nombre") val membresiaNombre: String? = null,
    val precio: Double = 0.0,
    @param:Json(name = "fecha_inicio") val fechaInicio: String? = null,
    @param:Json(name = "fecha_fin") val fechaFin: String? = null,
    @param:Json(name = "fecha_fin_efectiva") val fechaFinEfectiva: String? = null,
    @param:Json(name = "dias_extension") val diasExtension: Int = 0,
    @param:Json(name = "dias_restantes") val diasRestantes: Int = 0,
    val vigente: Boolean = false,
    val activa: Boolean = false,
    val total: Double = 0.0,
    @param:Json(name = "monto_pagado") val montoPagado: Double = 0.0,
    @param:Json(name = "saldo_pendiente") val saldoPendiente: Double = 0.0,
    val pagada: Boolean = false,
    @param:Json(name = "clases_totales") val clasesTotales: Int? = null,
    @param:Json(name = "clases_transcurridas") val clasesTranscurridas: Int? = null,
    @param:Json(name = "clases_restantes") val clasesRestantes: Int? = null,
)

// ─── Clientes ───────────────────────────────────────────────────────────────

data class ClienteDto(
    val id: Int,
    val nombres: String = "",
    val apellidos: String = "",
    @param:Json(name = "nombre_completo") val nombreCompleto: String = "",
    val sexo: String = "M",
    val edad: Int? = null,
    val celular: String = "",
    @param:Json(name = "horario_id") val horarioId: Int? = null,
    val horario: String? = null,
    val activo: Boolean = true,
    val estado: String = "activo",
    val bucket: String = "activo",
    val motivo: String = "",
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "membresia_activa") val membresiaActiva: ClienteMembresiaDto? = null,
    // Solo llegan en GET /clientes/{id}
    val membresias: List<ClienteMembresiaDto>? = null,
    val pagos: List<PagoDto>? = null,
)

/**
 * Se envian SIEMPRE todos los campos (incluidos los nulos) porque el backend
 * decide con `if campo in data`, igual que hace el frontend web.
 */
data class ClienteRequest(
    val nombres: String,
    val apellidos: String,
    val sexo: String,
    val edad: Int?,
    val celular: String,
    @param:Json(name = "horario_id") val horarioId: Int?,
    @param:Json(name = "membresia_id") val membresiaId: Int?,
    @param:Json(name = "fecha_inicio") val fechaInicio: String?,
)

data class ClienteEnvelope(val mensaje: String? = null, val cliente: ClienteDto? = null)

data class ContadoresDto(
    val activo: Int = 0,
    val inactivo: Int = 0,
    val eliminado: Int = 0,
)

data class AsignarMembresiaRequest(
    @param:Json(name = "membresia_id") val membresiaId: Int,
    @param:Json(name = "fecha_inicio") val fechaInicio: String?,
    val continuar: Boolean = false,
)

data class MotivoRequest(val motivo: String)

data class EliminarClienteRequest(
    val motivo: String,
    @param:Json(name = "eliminar_pagos") val eliminarPagos: Boolean,
)

// ─── Pagos ──────────────────────────────────────────────────────────────────

data class PagoDto(
    val id: Int,
    @param:Json(name = "cliente_id") val clienteId: Int = 0,
    @param:Json(name = "cliente_nombre") val clienteNombre: String? = null,
    @param:Json(name = "cliente_membresia_id") val clienteMembresiaId: Int? = null,
    val monto: Double = 0.0,
    val metodo: String = "efectivo",
    @param:Json(name = "monto_efectivo") val montoEfectivo: Double = 0.0,
    @param:Json(name = "monto_qr") val montoQr: Double = 0.0,
    val concepto: String = "pago",
    val fecha: String? = null,
    @param:Json(name = "registrado_por") val registradoPor: Int? = null,
)

data class PagoRequest(
    @param:Json(name = "cliente_id") val clienteId: Int,
    @param:Json(name = "cliente_membresia_id") val clienteMembresiaId: Int?,
    val monto: Double,
    val metodo: String,
    @param:Json(name = "monto_efectivo") val montoEfectivo: Double,
    @param:Json(name = "monto_qr") val montoQr: Double,
    val fecha: String?,
)

data class PagoEnvelope(
    val mensaje: String? = null,
    val pago: PagoDto? = null,
    @param:Json(name = "saldo_pendiente") val saldoPendiente: Double? = null,
)

data class ResumenCajaDto(
    val fecha: String? = null,
    val cantidad: Int = 0,
    val total: Double = 0.0,
    @param:Json(name = "total_efectivo") val totalEfectivo: Double = 0.0,
    @param:Json(name = "total_qr") val totalQr: Double = 0.0,
)

// ─── Horarios ───────────────────────────────────────────────────────────────

data class HorarioDto(
    val id: Int,
    val nombre: String = "",
    @param:Json(name = "hora_inicio") val horaInicio: String? = null,
    @param:Json(name = "hora_fin") val horaFin: String? = null,
    @param:Json(name = "cupo_maximo") val cupoMaximo: Int = 30,
    @param:Json(name = "dias_semana") val diasSemana: List<Int> = listOf(0, 1, 2, 3, 4),
    val color: String = "#185fa5",
    val activo: Boolean = true,
    @param:Json(name = "cupos_ocupados") val cuposOcupados: Int = 0,
    @param:Json(name = "cupos_disponibles") val cuposDisponibles: Int = 0,
)

data class HorarioRequest(
    val nombre: String,
    @param:Json(name = "hora_inicio") val horaInicio: String,
    @param:Json(name = "hora_fin") val horaFin: String,
    @param:Json(name = "cupo_maximo") val cupoMaximo: Int,
    @param:Json(name = "dias_semana") val diasSemana: List<Int>,
    val color: String,
    val activo: Boolean,
)

data class HorarioEnvelope(val mensaje: String? = null, val horario: HorarioDto? = null)

data class PersonasHorarioDto(
    val horario: HorarioDto? = null,
    val personas: List<ClienteDto> = emptyList(),
)

// ─── Asistencias ────────────────────────────────────────────────────────────

data class FilaAsistenciaDto(
    @param:Json(name = "cliente_id") val clienteId: Int,
    @param:Json(name = "nombre_completo") val nombreCompleto: String = "",
    @param:Json(name = "horario_id") val horarioId: Int? = null,
    val horario: String? = null,
    val estado: String? = null,
    @param:Json(name = "dias_justificados") val diasJustificados: Int = 0,
    val observacion: String = "",
)

data class AsistenciaDiaDto(
    val fecha: String? = null,
    val clientes: List<FilaAsistenciaDto> = emptyList(),
)

data class MarcarAsistenciaRequest(
    @param:Json(name = "cliente_id") val clienteId: Int,
    val fecha: String,
    val estado: String,
    @param:Json(name = "dias_justificados") val diasJustificados: Int,
    val observacion: String,
    @param:Json(name = "horario_id") val horarioId: Int?,
)

// ─── Personal ───────────────────────────────────────────────────────────────

data class PersonalRequest(
    val nombres: String,
    val apellidos: String,
    val ci: String,
    val username: String,
    val edad: Int?,
    val rol: String,
)

data class PersonalEnvelope(
    val mensaje: String? = null,
    val usuario: UsuarioDto? = null,
    @param:Json(name = "password_temporal") val passwordTemporal: String? = null,
)

// ─── Dashboard ──────────────────────────────────────────────────────────────

data class DistribucionDto(
    val membresia: String = "",
    val cantidad: Int = 0,
)

data class OcupacionDto(
    val horario: String = "",
    val ocupados: Int = 0,
    val cupo: Int = 0,
)

data class ResumenDashboardDto(
    @param:Json(name = "clientes_activos") val clientesActivos: Int = 0,
    @param:Json(name = "membresias_vigentes") val membresiasVigentes: Int = 0,
    @param:Json(name = "membresias_por_vencer") val membresiasPorVencer: Int = 0,
    @param:Json(name = "ingresos_hoy") val ingresosHoy: Double = 0.0,
    @param:Json(name = "ingresos_mes") val ingresosMes: Double = 0.0,
    @param:Json(name = "distribucion_membresias") val distribucionMembresias: List<DistribucionDto> = emptyList(),
    @param:Json(name = "ocupacion_horarios") val ocupacionHorarios: List<OcupacionDto> = emptyList(),
)

// ─── Finanzas (ingresos y egresos) ──────────────────────────────────────────

data class SerieDiaDto(
    val fecha: String = "",
    val ingresos: Double = 0.0,
    val egresos: Double = 0.0,
)

data class EgresoCategoriaDto(
    val categoria: String = "",
    val monto: Double = 0.0,
)

data class IngresoClienteDto(
    @param:Json(name = "cliente_id") val clienteId: Int? = null,
    val cliente: String = "",
    val total: Double = 0.0,
    val cantidad: Int = 0,
)

data class ResumenFinanzasDto(
    val desde: String? = null,
    val hasta: String? = null,
    @param:Json(name = "ingresos_total") val ingresosTotal: Double = 0.0,
    @param:Json(name = "ingresos_efectivo") val ingresosEfectivo: Double = 0.0,
    @param:Json(name = "ingresos_qr") val ingresosQr: Double = 0.0,
    @param:Json(name = "egresos_total") val egresosTotal: Double = 0.0,
    val balance: Double = 0.0,
    val serie: List<SerieDiaDto> = emptyList(),
    @param:Json(name = "egresos_categoria") val egresosCategoria: List<EgresoCategoriaDto> = emptyList(),
    @param:Json(name = "ingresos_cliente") val ingresosCliente: List<IngresoClienteDto> = emptyList(),
)

/** Cierre de caja de un día: igual que ResumenCajaDto pero con el detalle. */
data class CierreCajaDto(
    val fecha: String? = null,
    val cantidad: Int = 0,
    val total: Double = 0.0,
    @param:Json(name = "total_efectivo") val totalEfectivo: Double = 0.0,
    @param:Json(name = "total_qr") val totalQr: Double = 0.0,
    val pagos: List<PagoDto> = emptyList(),
)

// ─── Gastos (egresos) ───────────────────────────────────────────────────────

data class GastoDto(
    val id: Int,
    val categoria: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val metodo: String = "efectivo",
    val fecha: String? = null,
    @param:Json(name = "registrado_por") val registradoPor: Int? = null,
)

data class GastosRespuestaDto(
    val gastos: List<GastoDto> = emptyList(),
    val total: Double = 0.0,
)

data class GastoRequest(
    val categoria: String,
    val descripcion: String,
    val monto: Double,
    val metodo: String,
    val fecha: String,
)

// ─── Registro de actividad (auditoría) ─────────────────────────────────────

data class AuditoriaDto(
    val id: Int,
    @param:Json(name = "usuario_id") val usuarioId: Int? = null,
    @param:Json(name = "nombre_usuario") val nombreUsuario: String? = null,
    @param:Json(name = "rol_usuario") val rolUsuario: String? = null,
    val accion: String = "",
    val modulo: String = "",
    val descripcion: String = "",
    @param:Json(name = "ip_address") val ipAddress: String? = null,
    val fecha: String? = null,
)

// ─── Contenido de la página pública ─────────────────────────────────────────

data class DestacadoDto(
    val icono: String = "droplet",
    val titulo: String = "",
    val texto: String = "",
)

data class EstadisticaDto(
    val valor: String = "",
    val etiqueta: String = "",
)

data class ContenidoDto(
    val nombre: String = "",
    @param:Json(name = "hero_badge") val heroBadge: String = "",
    @param:Json(name = "hero_titulo") val heroTitulo: String = "",
    @param:Json(name = "hero_subtitulo") val heroSubtitulo: String = "",
    @param:Json(name = "sobre_texto") val sobreTexto: String = "",
    @param:Json(name = "logo_url") val logoUrl: String = "",
    @param:Json(name = "hero_imagen_url") val heroImagenUrl: String = "",
    @param:Json(name = "modelo_3d_url") val modelo3dUrl: String = "",
    val destacados: List<DestacadoDto> = emptyList(),
    val estadisticas: List<EstadisticaDto> = emptyList(),
    val galeria: List<String> = emptyList(),
    val direccion: String = "",
    val telefono: String = "",
    val whatsapp: String = "",
    @param:Json(name = "whatsapp_link") val whatsappLink: String = "",
    @param:Json(name = "google_maps_link") val googleMapsLink: String = "",
    @param:Json(name = "horario_texto") val horarioTexto: String = "",
    val tiktok: String = "",
    val instagram: String = "",
    val youtube: String = "",
    val facebook: String = "",
    @param:Json(name = "mostrar_planes") val mostrarPlanes: Boolean = true,
    @param:Json(name = "mostrar_horarios") val mostrarHorarios: Boolean = true,
)

data class ContenidoEnvelope(
    val mensaje: String? = null,
    val contenido: ContenidoDto? = null,
)

data class UploadRespuestaDto(val url: String = "")
