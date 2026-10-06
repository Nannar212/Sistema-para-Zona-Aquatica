package bo.zonaaquatica.movil.ui.asistencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.FilaAsistenciaDto
import bo.zonaaquatica.movil.data.model.HorarioDto
import bo.zonaaquatica.movil.data.model.MarcarAsistenciaRequest
import bo.zonaaquatica.movil.data.repo.AsistenciaRepo
import bo.zonaaquatica.movil.data.repo.CatalogoRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados validos del backend (models/asistencia.py). */
object EstadoAsistencia {
    const val PRESENTE = "presente"
    const val FALTA = "falta"
    const val JUSTIFICADA = "justificada"
}

data class UiAsistencia(
    val fecha: String = Formato.hoyIso(),
    val horarioFiltro: HorarioDto? = null,
    val horarios: List<HorarioDto> = emptyList(),
    val filas: List<FilaAsistenciaDto> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    /** ids de clientes con una marca en vuelo, para desactivar sus botones. */
    val enviando: Set<Int> = emptySet(),
    /** Cliente para el que se está pidiendo los días justificados. */
    val justificando: FilaAsistenciaDto? = null,
) {
    val presentes: Int get() = filas.count { it.estado == EstadoAsistencia.PRESENTE }
    val faltas: Int get() = filas.count { it.estado == EstadoAsistencia.FALTA }
    val justificadas: Int get() = filas.count { it.estado == EstadoAsistencia.JUSTIFICADA }
    val sinMarcar: Int get() = filas.count { it.estado == null }
}

/**
 * Pantalla de "pasar lista". Equivalente a AsistenciaPage.jsx:
 * GET /asistencias/dia y POST /asistencias por cada marca.
 */
class AsistenciaViewModel(
    private val repo: AsistenciaRepo,
    private val catalogo: CatalogoRepo,
) : ViewModel() {

    private val _ui = MutableStateFlow(UiAsistencia())
    val ui: StateFlow<UiAsistencia> = _ui.asStateFlow()

    fun iniciar() {
        viewModelScope.launch {
            (catalogo.horarios(soloActivos = true) as? Resultado.Ok)?.let {
                _ui.value = _ui.value.copy(horarios = it.dato)
            }
        }
        cargar()
    }

    fun cargar() {
        val e = _ui.value
        _ui.value = e.copy(cargando = true, error = null)
        viewModelScope.launch {
            when (val r = repo.delDia(e.fecha, e.horarioFiltro?.id)) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(
                    cargando = false,
                    filas = r.dato.clientes,
                )
                is Resultado.Error -> _ui.value = _ui.value.copy(
                    cargando = false,
                    error = r.mensaje,
                )
            }
        }
    }

    fun cambiarFecha(iso: String) {
        _ui.value = _ui.value.copy(fecha = iso)
        cargar()
    }

    fun desplazarFecha(dias: Long) = cambiarFecha(Formato.desplazar(_ui.value.fecha, dias))

    fun cambiarHorario(h: HorarioDto?) {
        _ui.value = _ui.value.copy(horarioFiltro = h)
        cargar()
    }

    fun pedirJustificacion(fila: FilaAsistenciaDto) {
        _ui.value = _ui.value.copy(justificando = fila)
    }

    fun cerrarJustificacion() {
        _ui.value = _ui.value.copy(justificando = null)
    }

    /**
     * Marca el estado de un cliente. En "justificada" se envían los días que
     * se suman como extensión a la membresía (el backend corre la fecha de fin).
     */
    fun marcar(
        fila: FilaAsistenciaDto,
        estado: String,
        diasJustificados: Int = 0,
        observacion: String = "",
        alTerminar: (String) -> Unit,
    ) {
        val e = _ui.value
        if (fila.clienteId in e.enviando) return

        _ui.value = e.copy(
            enviando = e.enviando + fila.clienteId,
            justificando = null,
        )

        viewModelScope.launch {
            val cuerpo = MarcarAsistenciaRequest(
                clienteId = fila.clienteId,
                fecha = e.fecha,
                estado = estado,
                diasJustificados = if (estado == EstadoAsistencia.JUSTIFICADA) diasJustificados else 0,
                observacion = observacion,
                horarioId = fila.horarioId,
            )

            when (val r = repo.marcar(cuerpo)) {
                is Resultado.Ok -> {
                    // Actualización optimista: se evita recargar toda la lista.
                    _ui.value = _ui.value.copy(
                        enviando = _ui.value.enviando - fila.clienteId,
                        filas = _ui.value.filas.map {
                            if (it.clienteId == fila.clienteId) it.copy(
                                estado = estado,
                                diasJustificados = cuerpo.diasJustificados,
                                observacion = observacion,
                            ) else it
                        },
                    )
                }
                is Resultado.Error -> {
                    _ui.value = _ui.value.copy(enviando = _ui.value.enviando - fila.clienteId)
                    alTerminar(r.mensaje)
                }
            }
        }
    }
}
