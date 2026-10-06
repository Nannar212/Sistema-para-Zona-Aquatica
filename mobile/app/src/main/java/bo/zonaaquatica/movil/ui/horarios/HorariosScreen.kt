package bo.zonaaquatica.movil.ui.horarios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.data.model.HorarioDto
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.BarraOcupacion
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

private val COLORES = listOf(
    "#185fa5", "#0c447c", "#378add", "#d9871a", "#f4b53f",
    "#15803d", "#b91c1c", "#7c3aed", "#0f172a",
)

/**
 * Equivalente a HorariosPage.jsx. Admin y recepcionista ven la lista y las
 * personas asignadas; solo el admin puede crear, editar y eliminar
 * (el backend ya lo exige con @rol_requerido).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorariosScreen(esAdmin: Boolean) {
    val contenedor = LocalContenedor.current
    val avisar = LocalAviso.current
    val vm: HorariosViewModel = viewModel { HorariosViewModel(contenedor.catalogo) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.cargar() }

    Box(Modifier.fillMaxSize()) {
        when {
            ui.cargando && ui.lista.isEmpty() -> CajaCargando("Cargando horarios...")
            ui.error != null && ui.lista.isEmpty() -> CajaError(ui.error!!) { vm.cargar() }
            ui.lista.isEmpty() -> CajaVacia(
                "Sin horarios",
                if (esAdmin) "Crea el primer horario con el botón +"
                else "El administrador aún no registró horarios",
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(ui.lista, key = { it.id }) { h ->
                    TarjetaHorario(
                        h = h,
                        esAdmin = esAdmin,
                        alVerPersonas = { vm.verPersonas(h) },
                        alEditar = { vm.abrirEditar(h) },
                        alEliminar = { vm.pedirEliminar(h) },
                    )
                }
            }
        }

        if (esAdmin) {
            FloatingActionButton(
                onClick = vm::abrirNuevo,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
            ) {
                Icon(Icons.Default.Add, "Nuevo horario")
            }
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
                    if (ui.editandoId == null) "Nuevo horario" else "Editar horario",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))

                CampoTexto(
                    form.nombre,
                    { v -> vm.editarForm { it.copy(nombre = v) } },
                    "Nombre",
                    requerido = true,
                    soporte = "Ej: Mañana 07:00-09:00",
                )
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(
                        form.horaInicio,
                        { v -> vm.editarForm { it.copy(horaInicio = v) } },
                        "Hora inicio",
                        tipoTeclado = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    CampoTexto(
                        form.horaFin,
                        { v -> vm.editarForm { it.copy(horaFin = v) } },
                        "Hora fin",
                        tipoTeclado = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                CampoTexto(
                    form.cupoMaximo,
                    { v -> vm.editarForm { it.copy(cupoMaximo = v.filter(Char::isDigit)) } },
                    "Cupo máximo",
                    tipoTeclado = KeyboardType.Number,
                )

                Spacer(Modifier.height(16.dp))
                Text("Días en que se dicta", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                SelectorDias(form.diasSemana) { dias -> vm.editarForm { it.copy(diasSemana = dias) } }

                Spacer(Modifier.height(16.dp))
                Text("Color", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    COLORES.forEach { hex ->
                        val seleccionado = hex.equals(form.color, ignoreCase = true)
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(colorDeHex(hex))
                                .border(
                                    width = if (seleccionado) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(100.dp),
                                )
                                .clickable { vm.editarForm { it.copy(color = hex) } },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                FilaInterruptor("Horario activo", form.activo) { v ->
                    vm.editarForm { it.copy(activo = v) }
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

    // ─── Personas asignadas ───
    val abierto = ui.horarioAbierto
    if (abierto != null) {
        val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = vm::cerrarPersonas, sheetState = estadoHoja) {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text(abierto.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${abierto.horaInicio} – ${abierto.horaFin} · " +
                        "${abierto.cuposOcupados}/${abierto.cupoMaximo} cupos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))

                when {
                    ui.cargandoPersonas -> Box(Modifier.height(140.dp)) { CajaCargando() }
                    ui.personas.isEmpty() -> CajaVacia("Sin clientes asignados")
                    else -> LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(ui.personas, key = { it.id }) { c ->
                            Column {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            c.nombreCompleto,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            c.membresiaActiva?.membresiaNombre ?: "Sin membresía",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    val m = c.membresiaActiva
                                    if (m != null && m.vigente) {
                                        Etiqueta("${m.diasRestantes} días", ColorEtiqueta.Verde)
                                    } else {
                                        Etiqueta("Vencida", ColorEtiqueta.Rojo)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    ui.porEliminar?.let { h ->
        DialogoConfirmar(
            titulo = "Eliminar horario",
            mensaje = "¿Eliminar \"${h.nombre}\"? Los clientes asignados quedarán sin horario.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            alConfirmar = { vm.eliminar(avisar) },
            alCerrar = vm::cancelarEliminar,
        )
    }
}

@Composable
private fun TarjetaHorario(
    h: HorarioDto,
    esAdmin: Boolean,
    alVerPersonas: () -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
) {
    TarjetaSeccion {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(5.dp)
                    .height(40.dp)
                    .background(colorDeHex(h.color), RoundedCornerShape(100.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        h.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.size(8.dp))
                    if (!h.activo) Etiqueta("Inactivo", ColorEtiqueta.Gris)
                }
                Text(
                    "${h.horaInicio ?: "--:--"} – ${h.horaFin ?: "--:--"} · ${Formato.dias(h.diasSemana)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (esAdmin) {
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

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Ocupación",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${h.cuposOcupados}/${h.cupoMaximo}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(4.dp))
        BarraOcupacion(h.cuposOcupados, h.cupoMaximo)

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = alVerPersonas) {
            Icon(Icons.Outlined.Group, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Ver clientes asignados")
        }
    }
}

/** "#185fa5" -> Color. Devuelve el azul del sistema si el hex es inválido. */
internal fun colorDeHex(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF185FA5))
