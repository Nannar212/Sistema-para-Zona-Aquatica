package bo.zonaaquatica.movil.ui.finanzas

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.core.Recibos
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.BarraBusqueda
import bo.zonaaquatica.movil.ui.common.BarraDato
import bo.zonaaquatica.movil.ui.common.BarrasHorizontales
import bo.zonaaquatica.movil.ui.common.CajaCargando
import bo.zonaaquatica.movil.ui.common.CajaError
import bo.zonaaquatica.movil.ui.common.CampoFecha
import bo.zonaaquatica.movil.ui.common.ColorEtiqueta
import bo.zonaaquatica.movil.ui.common.Etiqueta
import bo.zonaaquatica.movil.ui.common.FilaDato
import bo.zonaaquatica.movil.ui.common.GraficoLineasDoble
import bo.zonaaquatica.movil.ui.common.Leyenda
import bo.zonaaquatica.movil.ui.common.PuntoSerie
import bo.zonaaquatica.movil.ui.common.SerieLeyenda
import bo.zonaaquatica.movil.ui.common.TarjetaIndicador
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion
import bo.zonaaquatica.movil.ui.nav.LocalAviso
import bo.zonaaquatica.movil.ui.theme.Sol100
import bo.zonaaquatica.movil.ui.theme.Sol500
import bo.zonaaquatica.movil.ui.theme.VerdeOk
import bo.zonaaquatica.movil.ui.theme.VerdeOkSuave
import bo.zonaaquatica.movil.ui.theme.coloresViz
import kotlinx.coroutines.launch

/** Equivalente a admin/FinanzasPage.jsx: ingresos, egresos, balance y caja. */
@Composable
fun FinanzasScreen() {
    val contenedor = LocalContenedor.current
    val avisar = LocalAviso.current
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val vm: FinanzasViewModel = viewModel { FinanzasViewModel(contenedor.finanzas) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val viz = coloresViz()

    var descargando by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { vm.cargar() }

    val abrirRecibo: (Int) -> Unit = { pagoId ->
        if (descargando == 0) {
            descargando = pagoId
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
                descargando = 0
            }
        }
    }

    if (ui.cargando && ui.resumen == null) {
        CajaCargando("Cargando finanzas...")
        return
    }
    if (ui.error != null && ui.resumen == null) {
        CajaError(ui.error!!) { vm.cargar() }
        return
    }
    val r = ui.resumen ?: return

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ─── Una sola fila de filtros, arriba de todo lo que afecta ───
        item {
            TarjetaSeccion(relleno = 14.dp) {
                Text("Período", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        CampoFecha("Desde", ui.desde) { vm.cambiarDesde(it) }
                    }
                    Box(Modifier.weight(1f)) {
                        CampoFecha("Hasta", ui.hasta) { vm.cambiarHasta(it) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { vm.aplicarUltimosDias(7) },
                        label = { Text("7 días", style = MaterialTheme.typography.labelSmall) },
                    )
                    AssistChip(
                        onClick = { vm.aplicarUltimosDias(30) },
                        label = { Text("30 días", style = MaterialTheme.typography.labelSmall) },
                    )
                    AssistChip(
                        onClick = vm::aplicarMesActual,
                        label = { Text("Este mes", style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        // El refetch mantiene el render previo atenuado, sin salto de layout.
        val opacidad = if (ui.refrescando) 0.6f else 1f

        // ─── Cifras del período: la historia es un número, no un gráfico ───
        item {
            Row(
                Modifier.alpha(opacidad),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TarjetaIndicador(
                    titulo = "Ingresos",
                    valor = Formato.bs(r.ingresosTotal),
                    icono = Icons.Outlined.Payments,
                    colorIcono = viz.serie1,
                    fondoIcono = viz.serie1.copy(alpha = 0.12f),
                    pie = "Efec. ${Formato.bs(r.ingresosEfectivo)} · QR ${Formato.bs(r.ingresosQr)}",
                    modifier = Modifier.weight(1f),
                )
                TarjetaIndicador(
                    titulo = "Egresos",
                    valor = Formato.bs(r.egresosTotal),
                    icono = Icons.Outlined.ShoppingCart,
                    colorIcono = viz.serie2,
                    fondoIcono = viz.serie2.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            val positivo = r.balance >= 0
            TarjetaIndicador(
                titulo = if (positivo) "Balance del período" else "Balance del período (negativo)",
                valor = Formato.bs(r.balance),
                icono = Icons.Outlined.Balance,
                // Color de estado: aquí el color SÍ significa bien/mal, y va
                // siempre acompañado de icono y texto.
                colorIcono = if (positivo) VerdeOk else MaterialTheme.colorScheme.error,
                fondoIcono = if (positivo) VerdeOkSuave else MaterialTheme.colorScheme.errorContainer,
                pie = "${Formato.fecha(r.desde)} – ${Formato.fecha(r.hasta)}",
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(opacidad),
            )
        }

        // ─── Evolución diaria: dos series, un solo eje (ambas en Bs) ───
        item {
            TarjetaSeccion(modifier = Modifier.alpha(opacidad)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Día por día", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = vm::alternarTabla) {
                        Text(
                            if (ui.verTabla) "Ver gráfico" else "Ver tabla",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Leyenda(
                    listOf(
                        SerieLeyenda("Ingresos", viz.serie1, Formato.bs(r.ingresosTotal)),
                        SerieLeyenda("Egresos", viz.serie2, Formato.bs(r.egresosTotal)),
                    )
                )
                Spacer(Modifier.height(12.dp))

                if (ui.verTabla) {
                    // Vista de tabla: el gemelo accesible del gráfico.
                    Column {
                        r.serie.reversed().take(31).forEach { d ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(Formato.fecha(d.fecha), style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(
                                        Formato.bs(d.ingresos),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Text(
                                        Formato.bs(d.egresos),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                        if (r.serie.size > 31) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Mostrando los 31 días más recientes de ${r.serie.size}.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    GraficoLineasDoble(
                        puntos = r.serie.map {
                            PuntoSerie(it.fecha, it.ingresos, it.egresos)
                        },
                        colorA = viz.serie1,
                        colorB = viz.serie2,
                        colorRejilla = viz.rejilla,
                        colorSuperficie = viz.superficie,
                        seleccionado = ui.diaSeleccionado,
                        alSeleccionar = vm::seleccionarDia,
                    )
                    Spacer(Modifier.height(8.dp))

                    val sel = ui.diaSeleccionado?.let { r.serie.getOrNull(it) }
                    if (sel != null) {
                        FilaDato(Formato.fechaConDia(sel.fecha), "")
                        FilaDato("  Ingresos", Formato.bs(sel.ingresos))
                        FilaDato("  Egresos", Formato.bs(sel.egresos))
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                Formato.fecha(r.serie.firstOrNull()?.fecha),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Toca un día para ver el detalle",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                Formato.fecha(r.serie.lastOrNull()?.fecha),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // ─── Egresos por categoría: una serie, un solo color ───
        item {
            TarjetaSeccion(modifier = Modifier.alpha(opacidad)) {
                Text("Egresos por categoría", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))
                BarrasHorizontales(
                    datos = r.egresosCategoria
                        .sortedByDescending { it.monto }
                        .map { BarraDato(it.categoria, it.monto) },
                    color = viz.serie2,
                )
            }
        }

        // ─── Ingresos por cliente ───
        item {
            TarjetaSeccion(modifier = Modifier.alpha(opacidad)) {
                Text("Ingresos por cliente", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                BarraBusqueda(
                    valor = ui.buscarCliente,
                    alCambiar = vm::cambiarBusquedaCliente,
                    marcador = "Filtrar por nombre",
                )
                Spacer(Modifier.height(12.dp))
                BarrasHorizontales(
                    datos = ui.clientesFiltrados.map {
                        BarraDato(
                            etiqueta = it.cliente,
                            valor = it.total,
                            detalle = "${it.cantidad} pago(s)",
                        )
                    },
                    color = viz.serie1,
                    maximoVisible = 10,
                )
            }
        }

        // ─── Cierre de caja ───
        item {
            TarjetaSeccion {
                Text("Cierre de caja", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                CampoFecha("Día", ui.fechaCaja) { vm.cargarCaja(it) }
                Spacer(Modifier.height(10.dp))

                val caja = ui.caja
                if (caja == null) {
                    Text(
                        "Cargando el cierre del día...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            Formato.fechaConDia(caja.fecha),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Etiqueta("${caja.cantidad} pagos")
                    }
                    Spacer(Modifier.height(8.dp))
                    FilaDato("Total cobrado", Formato.bs(caja.total))
                    FilaDato("Efectivo", Formato.bs(caja.totalEfectivo))
                    FilaDato("QR", Formato.bs(caja.totalQr))

                    if (caja.pagos.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Pagos del día · toca para abrir el recibo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        caja.pagos.forEach { p ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { abrirRecibo(p.id) }
                                    .padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        p.clienteNombre ?: "Cliente eliminado",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        "${p.concepto.replaceFirstChar { it.uppercase() }} · " +
                                            "${p.metodo} · ${Formato.fechaHora(p.fecha)}",
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
                                if (descargando == p.id) {
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
                    }
                }
            }
        }
    }
}
