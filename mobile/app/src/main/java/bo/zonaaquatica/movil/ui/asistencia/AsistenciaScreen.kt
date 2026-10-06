package bo.zonaaquatica.movil.ui.asistencia

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.data.model.FilaAsistenciaDto
import bo.zonaaquatica.movil.data.model.HorarioDto
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.CajaCargando
import bo.zonaaquatica.movil.ui.common.CajaError
import bo.zonaaquatica.movil.ui.common.CajaVacia
import bo.zonaaquatica.movil.ui.common.CampoFecha
import bo.zonaaquatica.movil.ui.common.CampoSelector
import bo.zonaaquatica.movil.ui.common.CampoTexto
import bo.zonaaquatica.movil.ui.common.ColorEtiqueta
import bo.zonaaquatica.movil.ui.common.Etiqueta
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion
import bo.zonaaquatica.movil.ui.nav.LocalAviso
import bo.zonaaquatica.movil.ui.theme.Sol100
import bo.zonaaquatica.movil.ui.theme.Sol700
import bo.zonaaquatica.movil.ui.theme.VerdeOk
import bo.zonaaquatica.movil.ui.theme.VerdeOkSuave

/** Equivalente a AsistenciaPage.jsx: pasar lista del día. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenciaScreen() {
    val contenedor = LocalContenedor.current
    val avisar = LocalAviso.current
    val vm: AsistenciaViewModel = viewModel {
        AsistenciaViewModel(contenedor.asistencia, contenedor.catalogo)
    }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.iniciar() }

    Column(Modifier.fillMaxSize()) {

        // ─── Cabecera: fecha + filtro de horario + contadores ───
        Column(Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.desplazarFecha(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Día anterior")
                }
                Box(Modifier.weight(1f)) {
                    CampoFecha("Fecha", ui.fecha) { vm.cambiarFecha(it) }
                }
                IconButton(onClick = { vm.desplazarFecha(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Día siguiente")
                }
            }
            Text(
                Formato.fechaConDia(ui.fecha),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )

            Spacer(Modifier.height(10.dp))

            CampoSelector(
                etiqueta = "Horario",
                opciones = ui.horarios,
                seleccion = ui.horarioFiltro,
                textoDe = { h: HorarioDto -> "${h.nombre} (${h.horaInicio}–${h.horaFin})" },
                alSeleccionar = vm::cambiarHorario,
                textoVacio = "Todos los horarios",
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta("${ui.presentes} presentes", ColorEtiqueta.Verde)
                Etiqueta("${ui.faltas} faltas", ColorEtiqueta.Rojo)
                Etiqueta("${ui.justificadas} justif.", ColorEtiqueta.Amarillo)
                Etiqueta("${ui.sinMarcar} sin marcar", ColorEtiqueta.Gris)
            }
        }

        // ─── Lista ───
        Box(Modifier.fillMaxSize()) {
            when {
                ui.cargando && ui.filas.isEmpty() -> CajaCargando("Cargando lista...")
                ui.error != null && ui.filas.isEmpty() -> CajaError(ui.error!!) { vm.cargar() }
                ui.filas.isEmpty() -> CajaVacia(
                    "Sin clientes para este día",
                    "Revisa el filtro de horario o registra clientes primero",
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ui.filas, key = { it.clienteId }) { fila ->
                        FilaAsistencia(
                            fila = fila,
                            enviando = fila.clienteId in ui.enviando,
                            alMarcar = { estado ->
                                if (estado == EstadoAsistencia.JUSTIFICADA) {
                                    vm.pedirJustificacion(fila)
                                } else {
                                    vm.marcar(fila, estado, alTerminar = avisar)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    // ─── Diálogo de ausencia justificada ───
    ui.justificando?.let { fila ->
        DialogoJustificar(
            fila = fila,
            alCerrar = vm::cerrarJustificacion,
            alConfirmar = { dias, observacion ->
                vm.marcar(
                    fila,
                    EstadoAsistencia.JUSTIFICADA,
                    diasJustificados = dias,
                    observacion = observacion,
                    alTerminar = avisar,
                )
            },
        )
    }
}

@Composable
private fun FilaAsistencia(
    fila: FilaAsistenciaDto,
    enviando: Boolean,
    alMarcar: (String) -> Unit,
) {
    TarjetaSeccion(relleno = 12.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    fila.nombreCompleto,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    fila.horario ?: "Sin horario asignado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (fila.estado == EstadoAsistencia.JUSTIFICADA && fila.diasJustificados > 0) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "+${fila.diasJustificados} día(s) de extensión",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sol700,
                    )
                }
                if (fila.observacion.isNotBlank()) {
                    Text(
                        fila.observacion,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (enviando) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipEstado(
                texto = "Presente",
                icono = Icons.Default.Check,
                activo = fila.estado == EstadoAsistencia.PRESENTE,
                colorActivo = VerdeOkSuave,
                tintaActiva = VerdeOk,
                habilitado = !enviando,
                alPulsar = { alMarcar(EstadoAsistencia.PRESENTE) },
                modifier = Modifier.weight(1f),
            )
            ChipEstado(
                texto = "Falta",
                icono = Icons.Default.Close,
                activo = fila.estado == EstadoAsistencia.FALTA,
                colorActivo = MaterialTheme.colorScheme.errorContainer,
                tintaActiva = MaterialTheme.colorScheme.onErrorContainer,
                habilitado = !enviando,
                alPulsar = { alMarcar(EstadoAsistencia.FALTA) },
                modifier = Modifier.weight(1f),
            )
            ChipEstado(
                texto = "Justif.",
                icono = Icons.Default.EventAvailable,
                activo = fila.estado == EstadoAsistencia.JUSTIFICADA,
                colorActivo = Sol100,
                tintaActiva = Sol700,
                habilitado = !enviando,
                alPulsar = { alMarcar(EstadoAsistencia.JUSTIFICADA) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChipEstado(
    texto: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    activo: Boolean,
    colorActivo: androidx.compose.ui.graphics.Color,
    tintaActiva: androidx.compose.ui.graphics.Color,
    habilitado: Boolean,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = activo,
        enabled = habilitado,
        onClick = alPulsar,
        label = {
            Text(
                texto,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        },
        leadingIcon = { Icon(icono, null, modifier = Modifier.size(15.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colorActivo,
            selectedLabelColor = tintaActiva,
            selectedLeadingIconColor = tintaActiva,
        ),
        modifier = modifier.height(36.dp),
    )
}

@Composable
private fun DialogoJustificar(
    fila: FilaAsistenciaDto,
    alCerrar: () -> Unit,
    alConfirmar: (Int, String) -> Unit,
) {
    var dias by remember { mutableStateOf(fila.diasJustificados.coerceAtLeast(1).toString()) }
    var observacion by remember { mutableStateOf(fila.observacion) }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Ausencia justificada") },
        text = {
            Column {
                Text(
                    fila.nombreCompleto,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Los días justificados se suman como extensión a la membresía: " +
                        "corren la fecha de vencimiento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                CampoTexto(
                    dias,
                    { dias = it.filter(Char::isDigit) },
                    "Días a extender",
                    tipoTeclado = KeyboardType.Number,
                )
                Spacer(Modifier.height(10.dp))
                CampoTexto(
                    observacion,
                    { observacion = it },
                    "Observación",
                    lineas = 2,
                    soporte = "Ej: certificado médico",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                alConfirmar(dias.toIntOrNull() ?: 0, observacion.trim())
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}
