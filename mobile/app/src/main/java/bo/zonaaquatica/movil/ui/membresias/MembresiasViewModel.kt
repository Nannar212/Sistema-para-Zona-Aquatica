package bo.zonaaquatica.movil.ui.membresias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.MembresiaDto
import bo.zonaaquatica.movil.data.model.MembresiaRequest
import bo.zonaaquatica.movil.data.repo.CatalogoRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Formulario de plan. Espejo de VACIO en admin/MembresiasPage.jsx. */
data class FormMembresia(
    val nombre: String = "",
    val descripcion: String = "",
    val duracionDias: String = "30",
    val numeroClases: String = "",
    val precio: String = "",
    val diasAcceso: List<Int> = listOf(0, 1, 2, 3, 4),
    val activa: Boolean = true,
) {
    fun aRequest() = MembresiaRequest(
        nombre = nombre.trim(),
        descripcion = descripcion.trim(),
        duracionDias = duracionDias.toIntOrNull() ?: 30,
        numeroClases = numeroClases.trim().toIntOrNull(),
        precio = precio.replace(',', '.').toDoubleOrNull() ?: 0.0,
        diasAcceso = diasAcceso,
        activa = activa,
    )

    companion object {
        fun de(m: MembresiaDto) = FormMembresia(
            nombre = m.nombre,
            descripcion = m.descripcion,
            duracionDias = m.duracionDias.toString(),
            numeroClases = m.numeroClases?.toString() ?: "",
            precio = m.precio.toString(),
            diasAcceso = m.diasAcceso,
            activa = m.activa,
        )
    }
}

data class UiMembresias(
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val lista: List<MembresiaDto> = emptyList(),
    val error: String? = null,
    val form: FormMembresia? = null,
    val editandoId: Int? = null,
    val errorForm: String? = null,
    val porEliminar: MembresiaDto? = null,
)

class MembresiasViewModel(private val repo: CatalogoRepo) : ViewModel() {

    private val _ui = MutableStateFlow(UiMembresias())
    val ui: StateFlow<UiMembresias> = _ui.asStateFlow()

    fun cargar() {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            when (val r = repo.membresias()) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(cargando = false, lista = r.dato)
                is Resultado.Error -> _ui.value = _ui.value.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun abrirNueva() {
        _ui.value = _ui.value.copy(form = FormMembresia(), editandoId = null, errorForm = null)
    }

    fun abrirEditar(m: MembresiaDto) {
        _ui.value = _ui.value.copy(form = FormMembresia.de(m), editandoId = m.id, errorForm = null)
    }

    fun cerrarForm() {
        _ui.value = _ui.value.copy(form = null, editandoId = null, errorForm = null)
    }

    fun editarForm(bloque: (FormMembresia) -> FormMembresia) {
        _ui.value.form?.let { _ui.value = _ui.value.copy(form = bloque(it), errorForm = null) }
    }

    fun pedirEliminar(m: MembresiaDto) { _ui.value = _ui.value.copy(porEliminar = m) }
    fun cancelarEliminar() { _ui.value = _ui.value.copy(porEliminar = null) }

    fun guardar(alTerminar: (String) -> Unit) {
        val estado = _ui.value
        val form = estado.form ?: return
        if (estado.guardando) return

        if (form.nombre.isBlank()) {
            _ui.value = estado.copy(errorForm = "El nombre es obligatorio")
            return
        }
        if ((form.precio.replace(',', '.').toDoubleOrNull() ?: -1.0) < 0.0) {
            _ui.value = estado.copy(errorForm = "El precio no es válido")
            return
        }

        _ui.value = estado.copy(guardando = true, errorForm = null)
        viewModelScope.launch {
            val id = estado.editandoId
            val r = if (id == null) repo.crearMembresia(form.aRequest())
            else repo.actualizarMembresia(id, form.aRequest())

            when (r) {
                is Resultado.Ok -> {
                    _ui.value = _ui.value.copy(guardando = false, form = null, editandoId = null)
                    alTerminar(if (id == null) "Plan creado" else "Plan actualizado")
                    cargar()
                }
                is Resultado.Error ->
                    _ui.value = _ui.value.copy(guardando = false, errorForm = r.mensaje)
            }
        }
    }

    fun eliminar(alTerminar: (String) -> Unit) {
        val m = _ui.value.porEliminar ?: return
        _ui.value = _ui.value.copy(porEliminar = null)
        viewModelScope.launch {
            when (val r = repo.eliminarMembresia(m.id)) {
                is Resultado.Ok -> { alTerminar("Plan eliminado"); cargar() }
                is Resultado.Error -> alTerminar(r.mensaje)
            }
        }
    }
}
