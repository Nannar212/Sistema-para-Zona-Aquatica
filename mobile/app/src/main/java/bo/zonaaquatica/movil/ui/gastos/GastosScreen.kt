package bo.zonaaquatica.movil.ui.gastos

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import bo.zonaaquatica.movil.data.model.GastoDto
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.CajaCargando
import bo.zonaaquatica.movil.ui.common.CajaError
import bo.zonaaquatica.movil.ui.common.CajaVacia
import bo.zonaaquatica.movil.ui.common.CampoFecha
import bo.zonaaquatica.movil.ui.common.CampoSelector
import bo.zonaaquatica.movil.ui.common.CampoTexto
import bo.zonaaquatica.movil.ui.common.ColorEtiqueta
import bo.zonaaquatica.movil.ui.common.DialogoConfirmar
import bo.zonaaquatica.movil.ui.common.Etiqueta
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion
import bo.zonaaquatica.movil.ui.nav.LocalAviso

/** Equivalente a admin/GastosPage.jsx. Solo admin. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosScreen() {
    val contenedor = LocalContenedor.current
    val avisar = LocalAviso.current
    val vm: GastosViewModel = viewModel { GastosViewModel(contenedor.gastos) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.iniciar() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            // ─── Filtros, en una sola franja arriba ───
            Column(Modifier.padding(16.dp, 14.dp, 16.dp, 6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        CampoFecha("Desde", ui.desde) { vm.cambiarDesde(it) }
                    }
                    Box(Modifier.weight(1f)) {
                        CampoFecha("Hasta", ui.hasta) { vm.cambiarHasta(it) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = ui.categoriaFiltro == null,
                        onClick = { vm.cambiarCategoriaFiltro(null) },
                        label = { Text("Todas", style = MaterialTheme.typography.labelSmall) },
                    )
                    ui.categorias.forEach { c ->
                        FilterChip(
                            selected = ui.categoriaFiltro == c,
                            onClick = {
                                vm.cambiarCategoriaFiltro(if (ui.categoriaFiltro == c) null else c)
                            },
                            label = { Text(c, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${ui.lista.size} gasto(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Total ${Formato.bs(ui.total)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.cargando && ui.lista.isEmpty() -> CajaCargando("Cargando gastos...")
                    ui.error != null && ui.lista.isEmpty() -> CajaError(ui.error!!) { vm.cargar() }
                    ui.lista.isEmpty() -> CajaVacia(
                        "Sin gastos en el período",
                        "Registra uno con el botón +",
                    )
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(ui.lista, key = { it.id }) { g ->
                            TarjetaGasto(
                                g = g,
                                alEditar = { vm.abrirEditar(g) },
                                alEliminar = { vm.pedirEliminar(g) },
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = vm::abrirNuevo,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, "Nuevo gasto")
        }
    }

    // ─── Formulario ───
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
                    if (ui.editandoId == null) "Nuevo gasto" else "Editar gasto",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))

                CampoSelector(
                    etiqueta = "Categoría",
                    opciones = ui.categorias,
                    seleccion = form.categoria,
                    textoDe = { c: String -> c },
                    alSeleccionar = { c -> vm.editarForm { it.copy(categoria = c ?: "Otros") } },
                    permiteVacio = false,
                )
                Spacer(Modifier.height(10.dp))
                CampoTexto(
                    form.descripcion,
                    { v -> vm.editarForm { it.copy(descripcion = v) } },
                    "Descripción",
                    lineas = 2,
                    soporte = "Ej: 2 bidones de cloro",
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(
                        form.monto,
                        { v -> vm.editarForm { it.copy(monto = v) } },
                        "Monto (Bs)",
                        requerido = true,
                        tipoTeclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.weight(1f)) {
                        CampoSelector(
                            etiqueta = "Método",
                            opciones = FormGasto.METODOS,
                            seleccion = form.metodo,
                            textoDe = { m: String -> m.replaceFirstChar { it.uppercase() } },
                            alSeleccionar = { m ->
                                vm.editarForm { it.copy(metodo = m ?: "efectivo") }
                            },
                            permiteVacio = false,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                CampoFecha("Fecha", form.fecha) { f -> vm.editarForm { it.copy(fecha = f) } }

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

    ui.porEliminar?.let { g ->
        DialogoConfirmar(
            titulo = "Eliminar gasto",
            mensaje = "¿Eliminar el gasto de ${Formato.bs(g.monto)} en ${g.categoria}? " +
                "Dejará de contar en el balance.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            alConfirmar = { vm.eliminar(avisar) },
            alCerrar = vm::cancelarEliminar,
        )
    }
}

@Composable
private fun TarjetaGasto(
    g: GastoDto,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
) {
    TarjetaSeccion(relleno = 14.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Etiqueta(g.categoria, ColorEtiqueta.Azul)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        Formato.fecha(g.fecha),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (g.descripcion.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(g.descripcion, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Formato.bs(g.monto),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        g.metodo.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
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
    }
}
