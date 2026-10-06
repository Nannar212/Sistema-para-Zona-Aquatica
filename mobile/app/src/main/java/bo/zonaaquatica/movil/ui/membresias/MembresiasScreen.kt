package bo.zonaaquatica.movil.ui.membresias

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.data.model.MembresiaDto
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.CajaCargando
import bo.zonaaquatica.movil.ui.common.CajaError
import bo.zonaaquatica.movil.ui.common.CajaVacia
import bo.zonaaquatica.movil.ui.common.CampoTexto
import bo.zonaaquatica.movil.ui.common.ColorEtiqueta
import bo.zonaaquatica.movil.ui.common.DialogoConfirmar
import bo.zonaaquatica.movil.ui.common.Etiqueta
import bo.zonaaquatica.movil.ui.common.FilaInterruptor
import bo.zonaaquatica.movil.ui.common.SelectorDias
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion
import bo.zonaaquatica.movil.ui.nav.LocalAviso

/** Equivalente a admin/MembresiasPage.jsx — CRUD de planes. Solo admin. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembresiasScreen() {
    val contenedor = LocalContenedor.current
    val avisar = LocalAviso.current
    val vm: MembresiasViewModel = viewModel { MembresiasViewModel(contenedor.catalogo) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.cargar() }

    Box(Modifier.fillMaxSize()) {
        when {
            ui.cargando && ui.lista.isEmpty() -> CajaCargando("Cargando planes...")
            ui.error != null && ui.lista.isEmpty() -> CajaError(ui.error!!) { vm.cargar() }
            ui.lista.isEmpty() -> CajaVacia(
                "Sin planes todavía",
                "Crea el primer plan con el botón +",
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(ui.lista, key = { it.id }) { m ->
                    TarjetaMembresia(
                        m = m,
                        alEditar = { vm.abrirEditar(m) },
                        alEliminar = { vm.pedirEliminar(m) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = vm::abrirNueva,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, "Nuevo plan")
        }
    }

    val form = ui.form
    if (form != null) {
        val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = vm::cerrarForm, sheetState = estadoHoja) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text(
                    if (ui.editandoId == null) "Nuevo plan" else "Editar plan",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))

                CampoTexto(form.nombre, { v -> vm.editarForm { it.copy(nombre = v) } }, "Nombre", requerido = true)
                Spacer(Modifier.height(10.dp))
                CampoTexto(
                    form.descripcion,
                    { v -> vm.editarForm { it.copy(descripcion = v) } },
                    "Descripción",
                    lineas = 2,
                )
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(
                        form.duracionDias,
                        { v -> vm.editarForm { it.copy(duracionDias = v.filter(Char::isDigit)) } },
                        "Duración (días)",
                        tipoTeclado = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    CampoTexto(
                        form.precio,
                        { v -> vm.editarForm { it.copy(precio = v) } },
                        "Precio (Bs)",
                        tipoTeclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                CampoTexto(
                    form.numeroClases,
                    { v -> vm.editarForm { it.copy(numeroClases = v.filter(Char::isDigit)) } },
                    "N° de clases del paquete",
                    tipoTeclado = KeyboardType.Number,
                    soporte = "Déjalo vacío para planes por días de calendario (mensual)",
                )

                Spacer(Modifier.height(16.dp))
                Text("Días de acceso", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                SelectorDias(form.diasAcceso) { dias -> vm.editarForm { it.copy(diasAcceso = dias) } }

                Spacer(Modifier.height(16.dp))
                FilaInterruptor("Plan activo", form.activa) { v ->
                    vm.editarForm { it.copy(activa = v) }
                }

                if (ui.errorForm != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        ui.errorForm!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = vm::cerrarForm, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = { vm.guardar(avisar) },
                        enabled = !ui.guardando,
                        modifier = Modifier.weight(1f),
                    ) { Text("Guardar") }
                }
            }
        }
    }

    ui.porEliminar?.let { m ->
        DialogoConfirmar(
            titulo = "Eliminar plan",
            mensaje = "¿Eliminar \"${m.nombre}\"? Si hay clientes con este plan, el backend " +
                "lo rechazará para no romper el historial.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            alConfirmar = { vm.eliminar(avisar) },
            alCerrar = vm::cancelarEliminar,
        )
    }
}

@Composable
private fun TarjetaMembresia(
    m: MembresiaDto,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
) {
    TarjetaSeccion {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        m.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.size(8.dp))
                    if (m.activa) Etiqueta("Activo", ColorEtiqueta.Verde)
                    else Etiqueta("Inactivo", ColorEtiqueta.Gris)
                }
                if (m.descripcion.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        m.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = alEditar) { Icon(Icons.Outlined.Edit, "Editar") }
            IconButton(onClick = alEliminar) {
                Icon(
                    Icons.Outlined.Delete,
                    "Eliminar",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Etiqueta(Formato.bs(m.precio), ColorEtiqueta.Azul)
            Etiqueta("${m.duracionDias} días")
            m.numeroClases?.let { Etiqueta("$it clases", ColorEtiqueta.Amarillo) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Acceso: ${Formato.dias(m.diasAcceso)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
