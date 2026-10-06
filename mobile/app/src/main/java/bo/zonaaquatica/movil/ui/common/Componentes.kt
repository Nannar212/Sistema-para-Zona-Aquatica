package bo.zonaaquatica.movil.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bo.zonaaquatica.movil.core.Formato
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

// ─── Tarjetas y bloques de estado ───────────────────────────────────────────

@Composable
fun TarjetaSeccion(
    modifier: Modifier = Modifier,
    relleno: Dp = 16.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(relleno), content = contenido)
    }
}

@Composable
fun TarjetaIndicador(
    titulo: String,
    valor: String,
    icono: ImageVector,
    colorIcono: Color = MaterialTheme.colorScheme.primary,
    fondoIcono: Color = MaterialTheme.colorScheme.primaryContainer,
    pie: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(fondoIcono, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icono, null, tint = colorIcono, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                titulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pie != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    pie,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CajaCargando(etiqueta: String = "Cargando...") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(strokeWidth = 3.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                etiqueta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CajaVacia(titulo: String, detalle: String? = null, icono: ImageVector = Icons.Outlined.Inbox) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icono,
            null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(10.dp))
        Text(titulo, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        if (detalle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun CajaError(mensaje: String, alReintentar: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (alReintentar != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = alReintentar) { Text("Reintentar") }
        }
    }
}

// ─── Etiquetas tipo "Badge" del web ─────────────────────────────────────────

enum class ColorEtiqueta { Azul, Verde, Amarillo, Rojo, Gris }

@Composable
fun Etiqueta(texto: String, color: ColorEtiqueta = ColorEtiqueta.Gris) {
    val esquema = MaterialTheme.colorScheme
    val (fondo, tinta) = when (color) {
        ColorEtiqueta.Azul -> esquema.primaryContainer to esquema.onPrimaryContainer
        ColorEtiqueta.Verde -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        ColorEtiqueta.Amarillo -> esquema.secondaryContainer to esquema.onSecondaryContainer
        ColorEtiqueta.Rojo -> esquema.errorContainer to esquema.onErrorContainer
        ColorEtiqueta.Gris -> esquema.surfaceVariant to esquema.onSurfaceVariant
    }
    Box(
        Modifier
            .background(fondo, RoundedCornerShape(100.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(texto, style = MaterialTheme.typography.labelSmall, color = tinta)
    }
}

// ─── Campos de formulario ───────────────────────────────────────────────────

@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    soporte: String? = null,
    requerido: Boolean = false,
    lineas: Int = 1,
    habilitado: Boolean = true,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(if (requerido) "$etiqueta *" else etiqueta) },
        modifier = modifier.fillMaxWidth(),
        singleLine = lineas == 1,
        minLines = lineas,
        enabled = habilitado,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado),
        supportingText = soporte?.let { { Text(it) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CampoSelector(
    etiqueta: String,
    opciones: List<T>,
    seleccion: T?,
    textoDe: (T) -> String,
    alSeleccionar: (T?) -> Unit,
    modifier: Modifier = Modifier,
    textoVacio: String = "— Sin asignar —",
    permiteVacio: Boolean = true,
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { abierto = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = seleccion?.let(textoDe) ?: textoVacio,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            if (permiteVacio) {
                DropdownMenuItem(
                    text = { Text(textoVacio) },
                    onClick = { alSeleccionar(null); abierto = false },
                )
            }
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoDe(opcion)) },
                    onClick = { alSeleccionar(opcion); abierto = false },
                )
            }
        }
    }
}

/** Campo de fecha que abre el DatePicker de Material 3. Trabaja en ISO (yyyy-MM-dd). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    etiqueta: String,
    fechaIso: String,
    modifier: Modifier = Modifier,
    alCambiar: (String) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = Formato.fecha(fechaIso),
        onValueChange = {},
        readOnly = true,
        label = { Text(etiqueta) },
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            IconButton(onClick = { abierto = true }) {
                Icon(Icons.Outlined.CalendarMonth, "Elegir fecha")
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable { abierto = true },
    )

    if (abierto) {
        val inicial = runCatching {
            LocalDate.parse(fechaIso.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()

        val estado = rememberDatePickerState(initialSelectedDateMillis = inicial)
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { millis ->
                        val fecha = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        alCambiar(fecha.toString())
                    }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    }
}

/** Selector de dias de la semana (0=lunes ... 6=domingo), como en el web. */
@Composable
fun SelectorDias(
    seleccionados: List<Int>,
    cantidadDias: Int = 7,
    alCambiar: (List<Int>) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0 until cantidadDias).forEach { dia ->
            val activo = dia in seleccionados
            FilterChip(
                selected = activo,
                onClick = {
                    alCambiar(
                        if (activo) seleccionados - dia else (seleccionados + dia).sorted()
                    )
                },
                label = { Text(Formato.diasCortos[dia], style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
fun FilaInterruptor(etiqueta: String, valor: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = valor, onCheckedChange = alCambiar)
    }
}

@Composable
fun BarraBusqueda(
    valor: String,
    alCambiar: (String) -> Unit,
    marcador: String = "Buscar...",
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        placeholder = { Text(marcador) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (valor.isNotEmpty()) {
                IconButton(onClick = { alCambiar("") }) { Icon(Icons.Default.Close, "Limpiar") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(100.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

// ─── Dialogos ───────────────────────────────────────────────────────────────

@Composable
fun DialogoConfirmar(
    titulo: String,
    mensaje: String,
    textoConfirmar: String = "Confirmar",
    peligroso: Boolean = false,
    alConfirmar: () -> Unit,
    alCerrar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = { alConfirmar(); alCerrar() }) {
                Text(
                    textoConfirmar,
                    color = if (peligroso) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}

// ─── Misceláneos ────────────────────────────────────────────────────────────

@Composable
fun FilaDato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

/** Barra de ocupacion (usada en Horarios y en el Panel). */
@Composable
fun BarraOcupacion(ocupados: Int, cupo: Int, modifier: Modifier = Modifier) {
    val fraccion = if (cupo > 0) (ocupados.toFloat() / cupo).coerceIn(0f, 1f) else 0f
    val color = when {
        fraccion >= 1f -> MaterialTheme.colorScheme.error
        fraccion >= 0.8f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    Column(modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { fraccion },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
        )
    }
}

@Composable
fun ChipInfo(texto: String, icono: ImageVector? = null) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(texto, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = icono?.let { { Icon(it, null, modifier = Modifier.size(16.dp)) } },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
