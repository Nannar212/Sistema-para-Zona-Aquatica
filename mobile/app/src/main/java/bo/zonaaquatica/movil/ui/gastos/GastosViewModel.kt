package bo.zonaaquatica.movil.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.GastoDto
import bo.zonaaquatica.movil.data.model.GastoRequest
import bo.zonaaquatica.movil.data.repo.GastosRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Espejo de VACIO en admin/GastosPage.jsx. */
data class FormGasto(
    val categoria: String = "Cloro",
    val descripcion: String = "",
    val monto: String = "",
    val metodo: String = "efectivo",
    val fecha: String = Formato.hoyIso(),
) {
    fun aRequest() = GastoRequest(
        categoria = categoria,
        descripcion = descripcion.trim(),
        monto = monto.replace(',', '.').toDoubleOrNull() ?: 0.0,
        metodo = metodo,
        fecha = fecha,
    )

    companion object {
        /** Igual que METODOS_GASTO en models/gasto.py. */
        val METODOS = listOf("efectivo", "qr", "transferencia")

        fun de(g: GastoDto) = FormGasto(
            categoria = g.categoria,
            descripcion = g.descripcion,
            monto = if (g.monto % 1.0 == 0.0) g.monto.toLong().toString() else g.monto.toString(),
            metodo = g.metodo,
            fecha = g.fecha?.take(10) ?: Formato.hoyIso(),
        )
    }
}

data class UiGastos(
    val desde: String = Formato.primerDiaDelMesIso(),
    val hasta: String = Formato.hoyIso(),
    val categoriaFiltro: String? = null,
    val categorias: List<String> = emptyList(),
    val lista: List<GastoDto> = emptyList(),
    val total: Double = 0.0,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null,
    val form: FormGasto? = null,
    val editandoId: Int? = null,
    val errorForm: String? = null,
    val porEliminar: GastoDto? = null,
)

/** Equivalente a admin/GastosPage.jsx. Solo admin. */
class GastosViewModel(private val repo: GastosRepo) : ViewModel() {

    private val _ui = MutableStateFlow(UiGastos())
    val ui: StateFlow<UiGastos> = _ui.asStateFlow()

    fun iniciar() {
        viewModelScope.launch {
            (repo.categorias() as? Resultado.Ok)?.let {
                _ui.value = _ui.value.copy(categorias = it.dato)
            }
        }
        cargar()
    }

    fun cargar() {
        val e = _ui.value
        _ui.value = e.copy(cargando = true, error = null)
        viewModelScope.launch {
            when (val r = repo.listar(e.desde, e.hasta, e.categoriaFiltro)) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(
                    cargando = false,
                    lista = r.dato.gastos,
                    total = r.dato.total,
                )
                is Resultado.Error -> _ui.value = _ui.value.copy(
                    cargando = false,
                    error = r.mensaje,
                )
            }
        }
    }

    fun cambiarDesde(iso: String) { _ui.value = _ui.value.copy(desde = iso); cargar() }
    fun cambiarHasta(iso: String) { _ui.value = _ui.value.copy(hasta = iso); cargar() }

    fun cambiarCategoriaFiltro(categoria: String?) {
        _ui.value = _ui.value.copy(categoriaFiltro = categoria)
        cargar()
    }

    fun abrirNuevo() {
        val primera = _ui.value.categorias.firstOrNull() ?: "Cloro"
        _ui.value = _ui.value.copy(
            form = FormGasto(categoria = primera),
            editandoId = null,
            errorForm = null,
        )
    }

    fun abrirEditar(g: GastoDto) {
        _ui.value = _ui.value.copy(form = FormGasto.de(g), editandoId = g.id, errorForm = null)
    }

    fun cerrarForm() {
        _ui.value = _ui.value.copy(form = null, editandoId = null, errorForm = null)
    }

    fun editarForm(bloque: (FormGasto) -> FormGasto) {
        _ui.value.form?.let { _ui.value = _ui.value.copy(form = bloque(it), errorForm = null) }
    }

    fun pedirEliminar(g: GastoDto) { _ui.value = _ui.value.copy(porEliminar = g) }
    fun cancelarEliminar() { _ui.value = _ui.value.copy(porEliminar = null) }

    fun guardar(alTerminar: (String) -> Unit) {
        val estado = _ui.value
        val form = estado.form ?: return
        if (estado.guardando) return

        if ((form.monto.replace(',', '.').toDoubleOrNull() ?: 0.0) <= 0.0) {
            _ui.value = estado.copy(errorForm = "El monto debe ser mayor a 0")
            return
        }

        _ui.value = estado.copy(guardando = true, errorForm = null)
        viewModelScope.launch {
            val id = estado.editandoId
            val r = if (id == null) repo.crear(form.aRequest())
            else repo.actualizar(id, form.aRequest())

            when (r) {
                is Resultado.Ok -> {
                    _ui.value = _ui.value.copy(guardando = false, form = null, editandoId = null)
                    alTerminar(if (id == null) "Gasto registrado" else "Gasto actualizado")
                    cargar()
                }
                is Resultado.Error ->
                    _ui.value = _ui.value.copy(guardando = false, errorForm = r.mensaje)
            }
        }
    }

    fun eliminar(alTerminar: (String) -> Unit) {
        val g = _ui.value.porEliminar ?: return
        _ui.value = _ui.value.copy(porEliminar = null)
        viewModelScope.launch {
            when (val r = repo.eliminar(g.id)) {
                is Resultado.Ok -> { alTerminar("Gasto eliminado"); cargar() }
                is Resultado.Error -> alTerminar(r.mensaje)
            }
        }
    }
}
