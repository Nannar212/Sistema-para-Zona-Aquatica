package bo.zonaaquatica.movil.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.ResumenCajaDto
import bo.zonaaquatica.movil.data.model.ResumenDashboardDto
import bo.zonaaquatica.movil.data.repo.DashboardRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiPanel(
    val cargando: Boolean = true,
    val resumen: ResumenDashboardDto? = null,
    val caja: ResumenCajaDto? = null,
    val error: String? = null,
)

class PanelViewModel(private val repo: DashboardRepo) : ViewModel() {

    private val _ui = MutableStateFlow(UiPanel())
    val ui: StateFlow<UiPanel> = _ui.asStateFlow()

    fun cargar() {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            when (val r = repo.resumen()) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(cargando = false, resumen = r.dato)
                is Resultado.Error -> _ui.value = _ui.value.copy(cargando = false, error = r.mensaje)
            }
            // La caja es complementaria: si falla no se bloquea el panel.
            (repo.cajaHoy() as? Resultado.Ok)?.let {
                _ui.value = _ui.value.copy(caja = it.dato)
            }
        }
    }
}
