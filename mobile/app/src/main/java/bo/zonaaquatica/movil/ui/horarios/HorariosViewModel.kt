package bo.zonaaquatica.movil.ui.horarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.ClienteDto
import bo.zonaaquatica.movil.data.model.HorarioDto
import bo.zonaaquatica.movil.data.model.HorarioRequest
import bo.zonaaquatica.movil.data.repo.CatalogoRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Espejo de VACIO en HorariosPage.jsx. */
data class FormHorario(
    val nombre: String = "",
    val horaInicio: String = "07:00",
    val horaFin: String = "09:00",
    val cupoMaximo: String = "30",
    val diasSemana: List<Int> = listOf(0, 1, 2, 3, 4),
    val color: String = "#185fa5",
    val activo: Boolean = true,
) {
    fun aRequest() = HorarioRequest(
        nombre = nombre.trim(),
        horaInicio = horaInicio,
        horaFin = horaFin,
        cupoMaximo = cupoMaximo.toIntOrNull() ?: 30,
        diasSemana = diasSemana,
        color = color,
        activo = activo,
    )

    companion object {
        fun de(h: HorarioDto) = FormHorario(
            nombre = h.nombre,
            horaInicio = h.horaInicio ?: "07:00",
            horaFin = h.horaFin ?: "09:00",
            cupoMaximo = h.cupoMaximo.toString(),
            diasSemana = h.diasSemana,
            color = h.color,
            activo = h.activo,
        )
    }
}

data class UiHorarios(
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val lista: List<HorarioDto> = emptyList(),
    val error: String? = null,
    val form: FormHorario? = null,
    val editandoId: Int? = null,
    val errorForm: String? = null,
    val porEliminar: HorarioDto? = null,
    // Hoja de "ver personas"
    val horarioAbierto: HorarioDto? = null,
    val personas: List<ClienteDto> = emptyList(),
    val cargandoPersonas: Boolean = false,
)

class HorariosViewModel(private val repo: CatalogoRepo) : ViewModel() {

    private val _ui = MutableStateFlow(UiHorarios())
    val ui: StateFlow<UiHorarios> = _ui.asStateFlow()

    fun cargar() {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            when (val r = repo.horarios()) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(cargando = false, lista = r.dato)
                is Resultado.Error -> _ui.value = _ui.value.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun abrirNuevo() {
        _ui.value = _ui.value.copy(form = FormHorario(), editandoId = null, errorForm = null)
    }

    fun abrirEditar(h: HorarioDto) {
        _ui.value = _ui.value.copy(form = FormHorario.de(h), editandoId = h.id, errorForm = null)
    }

    fun cerrarForm() {
        _ui.value = _ui.value.copy(form = null, editandoId = null, errorForm = null)
    }

    fun editarForm(bloque: (FormHorario) -> FormHorario) {
        _ui.value.form?.let { _ui.value = _ui.value.copy(form = bloque(it), errorForm = null) }
    }

    fun pedirEliminar(h: HorarioDto) { _ui.value = _ui.value.copy(porEliminar = h) }
    fun cancelarEliminar() { _ui.value = _ui.value.copy(porEliminar = null) }

    fun verPersonas(h: HorarioDto) {
        _ui.value = _ui.value.copy(horarioAbierto = h, personas = emptyList(), cargandoPersonas = true)
        viewModelScope.launch {
            when (val r = repo.personasDe(h.id)) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(
                    cargandoPersonas = false,
                    personas = r.dato.personas,
                )
                is Resultado.Error -> _ui.value = _ui.value.copy(
                    cargandoPersonas = false,
                    error = r.mensaje,
                )
            }
        }
    }

    fun cerrarPersonas() {
        _ui.value = _ui.value.copy(horarioAbierto = null, personas = emptyList())
    }

    fun guardar(alTerminar: (String) -> Unit) {
        val estado = _ui.value
        val form = estado.form ?: return
        if (estado.guardando) return

        val problema = when {
            form.nombre.isBlank() -> "El nombre es obligatorio"
            !esHora(form.horaInicio) || !esHora(form.horaFin) ->
                "Las horas deben tener el formato HH:MM (ej: 07:00)"
            form.horaFin <= form.horaInicio -> "La hora de fin debe ser mayor a la de inicio"
            form.diasSemana.isEmpty() -> "Elige al menos un día"
            else -> null
        }
        if (problema != null) {
            _ui.value = estado.copy(errorForm = problema)
            return
        }

        _ui.value = estado.copy(guardando = true, errorForm = null)
        viewModelScope.launch {
            val id = estado.editandoId
            val r = if (id == null) repo.crearHorario(form.aRequest())
            else repo.actualizarHorario(id, form.aRequest())

            when (r) {
                is Resultado.Ok -> {
                    _ui.value = _ui.value.copy(guardando = false, form = null, editandoId = null)
                    alTerminar(if (id == null) "Horario creado" else "Horario actualizado")
                    cargar()
                }
                is Resultado.Error ->
                    _ui.value = _ui.value.copy(guardando = false, errorForm = r.mensaje)
            }
        }
    }

    fun eliminar(alTerminar: (String) -> Unit) {
        val h = _ui.value.porEliminar ?: return
        _ui.value = _ui.value.copy(porEliminar = null)
        viewModelScope.launch {
            when (val r = repo.eliminarHorario(h.id)) {
                is Resultado.Ok -> { alTerminar("Horario eliminado"); cargar() }
                is Resultado.Error -> alTerminar(r.mensaje)
            }
        }
    }

    /** El backend espera "HH:MM"; la comparacion de texto sirve porque es fijo. */
    private fun esHora(v: String) = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(v)
}
