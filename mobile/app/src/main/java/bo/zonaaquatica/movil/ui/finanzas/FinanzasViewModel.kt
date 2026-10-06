package bo.zonaaquatica.movil.ui.finanzas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.CierreCajaDto
import bo.zonaaquatica.movil.data.model.IngresoClienteDto
import bo.zonaaquatica.movil.data.model.ResumenFinanzasDto
import bo.zonaaquatica.movil.data.repo.FinanzasRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiFinanzas(
    val desde: String = Formato.primerDiaDelMesIso(),
    val hasta: String = Formato.hoyIso(),
    val cargando: Boolean = true,
    /** Refetch en curso: la pantalla mantiene el render previo atenuado. */
    val refrescando: Boolean = false,
    val resumen: ResumenFinanzasDto? = null,
    val caja: CierreCajaDto? = null,
    val fechaCaja: String = Formato.hoyIso(),
    val error: String? = null,
    /** Día tocado en el gráfico de líneas. */
    val diaSeleccionado: Int? = null,
    val verTabla: Boolean = false,
    val buscarCliente: String = "",
) {
    val clientesFiltrados: List<IngresoClienteDto>
        get() = (resumen?.ingresosCliente ?: emptyList()).filter {
            buscarCliente.isBlank() || it.cliente.contains(buscarCliente, ignoreCase = true)
        }
}

/** Equivalente a admin/FinanzasPage.jsx. Solo admin. */
class FinanzasViewModel(private val repo: FinanzasRepo) : ViewModel() {

    private val _ui = MutableStateFlow(UiFinanzas())
    val ui: StateFlow<UiFinanzas> = _ui.asStateFlow()

    fun cargar() {
        val e = _ui.value
        // Primera carga muestra spinner; los refetch mantienen el render previo.
        _ui.value = e.copy(
            cargando = e.resumen == null,
            refrescando = e.resumen != null,
            error = null,
        )
        viewModelScope.launch {
            when (val r = repo.resumen(e.desde, e.hasta)) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(
                    cargando = false,
                    refrescando = false,
                    resumen = r.dato,
                    diaSeleccionado = null,
                )
                is Resultado.Error -> _ui.value = _ui.value.copy(
                    cargando = false,
                    refrescando = false,
                    error = r.mensaje,
                )
            }
            cargarCaja(_ui.value.fechaCaja)
        }
    }

    fun cargarCaja(fecha: String) {
        _ui.value = _ui.value.copy(fechaCaja = fecha)
        viewModelScope.launch {
            (repo.cierreCaja(fecha) as? Resultado.Ok)?.let {
                _ui.value = _ui.value.copy(caja = it.dato)
            }
        }
    }

    fun cambiarDesde(iso: String) {
        _ui.value = _ui.value.copy(desde = iso)
        cargar()
    }

    fun cambiarHasta(iso: String) {
        _ui.value = _ui.value.copy(hasta = iso)
        cargar()
    }

    /** Atajos de período, para no pelear con el selector de fechas en el móvil. */
    fun aplicarMesActual() {
        _ui.value = _ui.value.copy(
            desde = Formato.primerDiaDelMesIso(),
            hasta = Formato.hoyIso(),
        )
        cargar()
    }

    fun aplicarUltimosDias(dias: Long) {
        val hoy = Formato.hoyIso()
        _ui.value = _ui.value.copy(desde = Formato.desplazar(hoy, -(dias - 1)), hasta = hoy)
        cargar()
    }

    fun seleccionarDia(indice: Int?) {
        _ui.value = _ui.value.copy(diaSeleccionado = indice)
    }

    fun alternarTabla() {
        _ui.value = _ui.value.copy(verTabla = !_ui.value.verTabla)
    }

    fun cambiarBusquedaCliente(texto: String) {
        _ui.value = _ui.value.copy(buscarCliente = texto)
    }
}
