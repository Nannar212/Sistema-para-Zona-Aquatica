package bo.zonaaquatica.movil.ui.actividad

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.AuditoriaDto
import bo.zonaaquatica.movil.data.repo.AuditoriaRepo
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.CajaCargando
import bo.zonaaquatica.movil.ui.common.CajaError
import bo.zonaaquatica.movil.ui.common.CajaVacia
import bo.zonaaquatica.movil.ui.common.ColorEtiqueta
import bo.zonaaquatica.movil.ui.common.Etiqueta
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Mismas listas que admin/RegistroActividadPage.jsx
private val MODULOS = listOf(
    "auth", "personal", "clientes", "membresias", "horarios",
    "asistencias", "pagos", "gastos", "contenido",
)
private val ACCIONES = listOf("crear", "editar", "eliminar", "login", "logout")

private fun colorDeAccion(accion: String) = when (accion) {
    "crear" -> ColorEtiqueta.Verde
    "editar" -> ColorEtiqueta.Azul
    "eliminar" -> ColorEtiqueta.Rojo
    else -> ColorEtiqueta.Gris
}

data class UiActividad(
    val cargando: Boolean = true,
    val lista: List<AuditoriaDto> = emptyList(),
    val error: String? = null,
    val modulo: String? = null,
    val accion: String? = null,
)

class ActividadViewModel(private val repo: AuditoriaRepo) : ViewModel() {

    private val _ui = MutableStateFlow(UiActividad())
    val ui: StateFlow<UiActividad> = _ui.asStateFlow()

    fun cargar() {
        val e = _ui.value
        _ui.value = e.copy(cargando = true, error = null)
        viewModelScope.launch {
            when (val r = repo.listar(e.modulo, e.accion)) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(cargando = false, lista = r.dato)
                is Resultado.Error -> _ui.value = _ui.value.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun cambiarModulo(m: String?) { _ui.value = _ui.value.copy(modulo = m); cargar() }
    fun cambiarAccion(a: String?) { _ui.value = _ui.value.copy(accion = a); cargar() }
}

/** Equivalente a admin/RegistroActividadPage.jsx. Solo admin, solo lectura. */
@Composable
fun ActividadScreen() {
    val contenedor = LocalContenedor.current
    val vm: ActividadViewModel = viewModel { ActividadViewModel(contenedor.auditoria) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.cargar() }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp, 14.dp, 16.dp, 6.dp)) {
            Text(
                "Acción",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = ui.accion == null,
                    onClick = { vm.cambiarAccion(null) },
                    label = { Text("Todas", style = MaterialTheme.typography.labelSmall) },
                )
                ACCIONES.forEach { a ->
                    FilterChip(
                        selected = ui.accion == a,
                        onClick = { vm.cambiarAccion(if (ui.accion == a) null else a) },
                        label = {
                            Text(
                                a.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Módulo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = ui.modulo == null,
                    onClick = { vm.cambiarModulo(null) },
                    label = { Text("Todos", style = MaterialTheme.typography.labelSmall) },
                )
                MODULOS.forEach { m ->
                    FilterChip(
                        selected = ui.modulo == m,
                        onClick = { vm.cambiarModulo(if (ui.modulo == m) null else m) },
                        label = {
                            Text(
                                m.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "${ui.lista.size} registro(s) · más recientes primero",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(Modifier.fillMaxSize()) {
            when {
                ui.cargando && ui.lista.isEmpty() -> CajaCargando("Cargando actividad...")
                ui.error != null && ui.lista.isEmpty() -> CajaError(ui.error!!) { vm.cargar() }
                ui.lista.isEmpty() -> CajaVacia(
                    "Sin registros",
                    "No hay actividad con esos filtros",
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ui.lista, key = { it.id }) { registro ->
                        TarjetaSeccion(relleno = 13.dp) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Etiqueta(registro.accion, colorDeAccion(registro.accion))
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    registro.modulo.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    Formato.fechaHora(registro.fecha),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (registro.descripcion.isNotBlank()) {
                                Spacer(Modifier.height(7.dp))
                                Text(
                                    registro.descripcion,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                buildString {
                                    append(registro.nombreUsuario ?: "—")
                                    registro.rolUsuario?.takeIf { it.isNotBlank() }?.let {
                                        append(" · $it")
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
