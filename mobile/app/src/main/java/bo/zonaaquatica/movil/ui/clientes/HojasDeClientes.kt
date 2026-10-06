package bo.zonaaquatica.movil.ui.clientes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.core.Recibos
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.ClienteDto
import bo.zonaaquatica.movil.data.model.HorarioDto
import bo.zonaaquatica.movil.data.model.MembresiaDto
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.CajaCargando
import bo.zonaaquatica.movil.ui.common.CampoFecha
import bo.zonaaquatica.movil.ui.common.CampoSelector
import bo.zonaaquatica.movil.ui.common.CampoTexto
import bo.zonaaquatica.movil.ui.common.ColorEtiqueta
import bo.zonaaquatica.movil.ui.common.DialogoConfirmar
import bo.zonaaquatica.movil.ui.common.Etiqueta
import bo.zonaaquatica.movil.ui.common.FilaDato
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion
import kotlinx.coroutines.launch

/**
 * Todas las hojas y diálogos de la pantalla de clientes.
 * `esAdmin` solo oculta botones: el backend es la autoridad real
 * (inactivar / eliminar / restaurar son @rol_requerido('admin')).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojasDeClientes(
    vm: ClientesViewModel,
    ui: UiClientes,
    esAdmin: Boolean,
    avisar: (String) -> Unit,
) {
    val contenedor = LocalContenedor.current
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    // id del pago cuyo recibo se esta descargando (0 = ninguno)
    var reciboEnCurso by remember { mutableIntStateOf(0) }

    val abrirRecibo: (Int) -> Unit = { pagoId ->
        if (reciboEnCurso == 0) {
            reciboEnCurso = pagoId
            alcance.launch {
                when (val r = contenedor.recibos.descargar(pagoId)) {
                    is Resultado.Ok -> when (Recibos.abrir(contexto, pagoId, r.dato)) {
                        Recibos.Resultado.Abierto -> Unit
                        Recibos.Resultado.Compartido -> avisar("No hay visor de PDF: elige una app")
                        Recibos.Resultado.SinVisor ->
                            avisar("Instala un visor de PDF para ver el recibo")
                        Recibos.Resultado.Fallo -> avisar("No se pudo guardar el recibo")
                    }
                    is Resultado.Error -> avisar(r.mensaje)
                }
                reciboEnCurso = 0
            }
        }
    }

    // ─── Detalle del cliente ────────────────────────────────────────────────
    if (ui.detalle != null || ui.cargandoDetalle) {
        val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = vm::cerrarDetalle, sheetState = estadoHoja) {
            val c = ui.detalle
            if (c == null) {
                Box(Modifier.height(220.dp)) { CajaCargando("Cargando ficha...") }
            } else {
                DetalleCliente(
                    c = c,
                    esAdmin = esAdmin,
                    reciboEnCurso = reciboEnCurso,
                    alAbrirRecibo = abrirRecibo,
                    alCobrar = { vm.abrirPago(c) },
                    alRenovar = { vm.abrirRenovar(c) },
                    alEditar = { vm.abrirEditar(c) },
                    alInactivar = { vm.pedirInactivar(c) },
                    alEliminar = { vm.pedirEliminar(c) },
                    alRestaurar = { vm.pedirRestaurar(c) },
                )
            }
        }
    }

    // ─── Formulario de cliente ──────────────────────────────────────────────
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
                    if (ui.editandoId == null) "Nuevo cliente" else "Editar cliente",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoTexto(
                        form.nombres,
                        { v -> vm.editarForm { it.copy(nombres = v) } },
                        "Nombres",
                        requerido = true,
                        modifier = Modifier.weight(1f),
                    )
                    CampoTexto(
                        form.apellidos,
                        { v -> vm.editarForm { it.copy(apellidos = v) } },
                        "Apellidos",
                        requerido = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        CampoSelector(
                            etiqueta = "Sexo",
                            opciones = FormCliente.SEXOS,
                            seleccion = form.sexo,
                            textoDe = { s: String -> FormCliente.etiquetaSexo(s) },
                            alSeleccionar = { s -> vm.editarForm { it.copy(sexo = s ?: "M") } },
                            permiteVacio = false,
                        )
                    }
                    CampoTexto(
                        form.edad,
                        { v -> vm.editarForm { it.copy(edad = v.filter(Char::isDigit)) } },
                        "Edad",
                        tipoTeclado = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                CampoTexto(
                    form.celular,
                    { v -> vm.editarForm { it.copy(celular = v) } },
                    "Celular",
                    tipoTeclado = KeyboardType.Phone,
                )
                Spacer(Modifier.height(10.dp))
                CampoSelector(
                    etiqueta = "Horario",
                    opciones = ui.horarios,
                    seleccion = ui.horarios.firstOrNull { it.id == form.horarioId },
                    textoDe = { h: HorarioDto -> "${h.nombre} (${h.horaInicio}–${h.horaFin})" },
                    alSeleccionar = { h -> vm.editarForm { it.copy(horarioId = h?.id) } },
                )

                if (ui.editandoId == null) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    Text("Membresía inicial", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    CampoSelector(
                        etiqueta = "Plan",
                        opciones = ui.membresias,
                        seleccion = ui.membresias.firstOrNull { it.id == form.membresiaId },
                        textoDe = { m: MembresiaDto -> "${m.nombre} · ${Formato.bs(m.precio)}" },
                        alSeleccionar = { m -> vm.editarForm { it.copy(membresiaId = m?.id) } },
                        textoVacio = "— Sin membresía por ahora —",
                    )
                    if (form.membresiaId != null) {
                        Spacer(Modifier.height(10.dp))
                        CampoFecha(
                            "Inicio de la membresía",
                            form.fechaInicio,
                        ) { f -> vm.editarForm { it.copy(fechaInicio = f) } }
                    }
                }

                if (ui.errorForm != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        ui.errorForm,
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

    // ─── Cobro ──────────────────────────────────────────────────────────────
    val pago = ui.pago
    if (pago != null) {
        val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = vm::cerrarPago, sheetState = estadoHoja) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text("Registrar pago", style = MaterialTheme.typography.titleMedium)
                Text(
                    pago.cliente.nombreCompleto,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val m = pago.cliente.membresiaActiva
                if (m != null) {
                    Spacer(Modifier.height(12.dp))
                    TarjetaSeccion(relleno = 12.dp) {
                        FilaDato("Plan", m.membresiaNombre ?: "—")
                        FilaDato("Total del plan", Formato.bs(m.total))
                        FilaDato("Ya pagado", Formato.bs(m.montoPagado))
                        FilaDato("Saldo pendiente", Formato.bs(m.saldoPendiente))
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Este cliente no tiene membresía activa: el pago se registrará " +
                            "como ingreso suelto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("Método", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormPago.METODOS.forEach { metodo ->
                        FilterChip(
                            selected = pago.metodo == metodo,
                            onClick = { vm.editarPago { it.copy(metodo = metodo) } },
                            label = {
                                Text(
                                    when (metodo) {
                                        "efectivo" -> "Efectivo"
                                        "qr" -> "QR"
                                        else -> "Mixto"
                                    }
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (pago.metodo == "mixto") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CampoTexto(
                            pago.montoEfectivo,
                            { v -> vm.editarPago { it.copy(montoEfectivo = v) } },
                            "Efectivo (Bs)",
                            tipoTeclado = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                        )
                        CampoTexto(
                            pago.montoQr,
                            { v -> vm.editarPago { it.copy(montoQr = v) } },
                            "QR (Bs)",
                            tipoTeclado = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    val suma = (pago.montoEfectivo.replace(',', '.').toDoubleOrNull() ?: 0.0) +
                        (pago.montoQr.replace(',', '.').toDoubleOrNull() ?: 0.0)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Total: ${Formato.bs(suma)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    CampoTexto(
                        pago.monto,
                        { v -> vm.editarPago { it.copy(monto = v) } },
                        "Monto (Bs)",
                        tipoTeclado = KeyboardType.Decimal,
                        requerido = true,
                        soporte = if (pago.saldoPendiente > 0.009)
                            "Puedes cobrar menos: queda registrado como abono (fianza)"
                        else null,
                    )
                }

                Spacer(Modifier.height(10.dp))
                CampoFecha("Fecha del pago", pago.fecha) { f ->
                    vm.editarPago { it.copy(fecha = f) }
                }

                if (ui.errorPago != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        ui.errorPago,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = vm::cerrarPago, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = { vm.registrarPago(avisar) },
                        enabled = !ui.guardando,
                        modifier = Modifier.weight(1f),
                    ) { Text("Cobrar") }
                }
            }
        }
    }

    // ─── Renovar / asignar membresía ────────────────────────────────────────
    val renovar = ui.renovar
    if (renovar != null) {
        val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = vm::cerrarRenovar, sheetState = estadoHoja) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text("Asignar / renovar membresía", style = MaterialTheme.typography.titleMedium)
                Text(
                    renovar.cliente.nombreCompleto,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                CampoSelector(
                    etiqueta = "Plan",
                    opciones = ui.membresias,
                    seleccion = ui.membresias.firstOrNull { it.id == renovar.membresiaId },
                    textoDe = { m: MembresiaDto ->
                        "${m.nombre} · ${Formato.bs(m.precio)} · ${m.duracionDias} días"
                    },
                    alSeleccionar = { m -> vm.editarRenovar { it.copy(membresiaId = m?.id) } },
                    permiteVacio = false,
                    textoVacio = "— Elige un plan —",
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = renovar.continuar,
                        onCheckedChange = { v -> vm.editarRenovar { it.copy(continuar = v) } },
                    )
                    Column {
                        Text("Continuar al vencer la actual", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Empieza el día siguiente al fin de la membresía vigente",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (!renovar.continuar) {
                    Spacer(Modifier.height(10.dp))
                    CampoFecha("Fecha de inicio", renovar.fechaInicio) { f ->
                        vm.editarRenovar { it.copy(fechaInicio = f) }
                    }
                }

                if (ui.errorRenovar != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        ui.errorRenovar,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = vm::cerrarRenovar, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = { vm.confirmarRenovar(avisar) },
                        enabled = !ui.guardando,
                        modifier = Modifier.weight(1f),
                    ) { Text("Asignar") }
                }
            }
        }
    }

    // ─── Inactivar (con motivo) ─────────────────────────────────────────────
    ui.inactivar?.let { (cliente, motivo) ->
        AlertDialog(
            onDismissRequest = vm::cancelarInactivar,
            title = { Text("Marcar como inactivo") },
            text = {
                Column {
                    Text(
                        cliente.nombreCompleto,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        motivo,
                        vm::cambiarMotivoInactivar,
                        "Motivo",
                        lineas = 2,
                        soporte = "Ej: se mudó de ciudad",
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.confirmarInactivar(avisar) }) { Text("Marcar inactivo") }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelarInactivar) { Text("Cancelar") }
            },
        )
    }

    // ─── Eliminar (con motivo y devolución opcional) ────────────────────────
    ui.eliminar?.let { (cliente, motivo, borrarPagos) ->
        AlertDialog(
            onDismissRequest = vm::cancelarEliminar,
            title = { Text("Eliminar cliente") },
            text = {
                Column {
                    Text(
                        cliente.nombreCompleto,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pasa a la pestaña Eliminados: no se borra el historial.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(motivo, vm::cambiarMotivoEliminar, "Motivo", lineas = 2)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = borrarPagos, onCheckedChange = vm::cambiarEliminarPagos)
                        Column {
                            Text(
                                "Borrar también sus pagos (devolución)",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "El dinero deja de contar como ingreso",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.confirmarEliminar(avisar) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelarEliminar) { Text("Cancelar") }
            },
        )
    }

    ui.porRestaurar?.let { c ->
        DialogoConfirmar(
            titulo = "Recuperar cliente",
            mensaje = "¿Devolver a ${c.nombreCompleto} a la pestaña de activos?",
            textoConfirmar = "Recuperar",
            alConfirmar = { vm.confirmarRestaurar(avisar) },
            alCerrar = vm::cancelarRestaurar,
        )
    }
}

// ─── Contenido de la ficha del cliente ──────────────────────────────────────

@Composable
private fun DetalleCliente(
    c: ClienteDto,
    esAdmin: Boolean,
    reciboEnCurso: Int,
    alAbrirRecibo: (Int) -> Unit,
    alCobrar: () -> Unit,
    alRenovar: () -> Unit,
    alEditar: () -> Unit,
    alInactivar: () -> Unit,
    alEliminar: () -> Unit,
    alRestaurar: () -> Unit,
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
    ) {
        Text(
            c.nombreCompleto,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EtiquetaMembresia(c.membresiaActiva)
            Etiqueta(
                when (c.bucket) {
                    Bucket.ACTIVO -> "Activo"
                    Bucket.INACTIVO -> "Inactivo"
                    else -> "Eliminado"
                },
                when (c.bucket) {
                    Bucket.ACTIVO -> ColorEtiqueta.Verde
                    Bucket.INACTIVO -> ColorEtiqueta.Amarillo
                    else -> ColorEtiqueta.Rojo
                },
            )
        }
        if (c.motivo.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                c.motivo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ─── Datos ───
        TarjetaSeccion(relleno = 14.dp) {
            Text("Datos", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            FilaDato("Sexo", FormCliente.etiquetaSexo(c.sexo))
            FilaDato("Edad", c.edad?.toString() ?: "—")
            FilaDato("Celular", c.celular.ifBlank { "—" })
            FilaDato("Horario", c.horario ?: "Sin asignar")
            FilaDato("Registrado", Formato.fecha(c.createdAt))
        }

        // ─── Membresía activa ───
        val m = c.membresiaActiva
        Spacer(Modifier.height(12.dp))
        TarjetaSeccion(relleno = 14.dp) {
            Text("Membresía activa", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            if (m == null) {
                Text(
                    "Sin membresía asignada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FilaDato("Plan", m.membresiaNombre ?: "—")
                FilaDato("Desde", Formato.fecha(m.fechaInicio))
                FilaDato("Vence", Formato.fecha(m.fechaFinEfectiva))
                if (m.diasExtension > 0) {
                    FilaDato("Extensión", "+${m.diasExtension} día(s) por justificaciones")
                }
                FilaDato("Días restantes", m.diasRestantes.toString())
                if (m.clasesTotales != null) {
                    FilaDato(
                        "Clases",
                        "${m.clasesRestantes ?: 0} de ${m.clasesTotales} restantes",
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                FilaDato("Total", Formato.bs(m.total))
                FilaDato("Pagado", Formato.bs(m.montoPagado))
                FilaDato("Saldo", Formato.bs(m.saldoPendiente))
                if (m.saldoPendiente > 0.009) {
                    Spacer(Modifier.height(6.dp))
                    Etiqueta("Pago pendiente (fianza)", ColorEtiqueta.Amarillo)
                }
            }
        }

        // ─── Pagos ───
        val pagos = c.pagos.orEmpty()
        Spacer(Modifier.height(12.dp))
        TarjetaSeccion(relleno = 14.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pagos", style = MaterialTheme.typography.titleSmall)
                Etiqueta("${pagos.size}")
            }
            if (pagos.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Toca un pago para abrir su recibo en PDF",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (pagos.isEmpty()) {
                Text(
                    "Sin pagos registrados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(Modifier.heightIn(max = 260.dp)) {
                    pagos.take(12).forEach { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { alAbrirRecibo(p.id) }
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    Formato.fechaHora(p.fecha),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    "${p.concepto.replaceFirstChar { it.uppercase() }} · ${p.metodo}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                Formato.bs(p.monto),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.width(8.dp))
                            if (reciboEnCurso == p.id) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.PictureAsPdf,
                                    "Abrir recibo",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                    if (pagos.size > 12) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Mostrando los 12 pagos más recientes de ${pagos.size}.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ─── Acciones ───
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = alCobrar, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Payments, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cobrar")
            }
            OutlinedButton(onClick = alRenovar, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Autorenew, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Renovar")
            }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = alEditar, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Editar datos")
        }

        if (esAdmin) {
            Spacer(Modifier.height(10.dp))
            if (c.bucket == Bucket.ACTIVO) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = alInactivar, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.PersonOff, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Inactivar")
                    }
                    OutlinedButton(onClick = alEliminar, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Outlined.Delete,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                OutlinedButton(onClick = alRestaurar, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Restore, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Recuperar cliente")
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                "Inactivar, eliminar y recuperar son acciones del administrador.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
