package bo.zonaaquatica.movil.ui.clientes

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.data.model.ClienteDto
import bo.zonaaquatica.movil.data.model.ClienteMembresiaDto
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.AlertaVencimiento
import bo.zonaaquatica.movil.ui.common.BarraBusqueda
import bo.zonaaquatica.movil.ui.common.CajaCargando
import bo.zonaaquatica.movil.ui.common.CajaError
import bo.zonaaquatica.movil.ui.common.CajaVacia
import bo.zonaaquatica.movil.ui.common.ColorEtiqueta
import bo.zonaaquatica.movil.ui.common.Etiqueta
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion
import bo.zonaaquatica.movil.ui.nav.LocalAviso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(esAdmin: Boolean) {
    val contenedor = LocalContenedor.current
    val avisar = LocalAviso.current
    val vm: ClientesViewModel = viewModel {
        ClientesViewModel(contenedor.clientes, contenedor.catalogo)
    }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.iniciar() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            PrimaryScrollableTabRow(
                selectedTabIndex = Bucket.TODOS.indexOf(ui.tab).coerceAtLeast(0),
                edgePadding = 12.dp,
            ) {
                Bucket.TODOS.forEach { bucket ->
                    val cantidad = when (bucket) {
                        Bucket.ACTIVO -> ui.contadores.activo
                        Bucket.INACTIVO -> ui.contadores.inactivo
                        else -> ui.contadores.eliminado
                    }
                    Tab(
                        selected = ui.tab == bucket,
                        onClick = { vm.cambiarTab(bucket) },
                        text = { Text("${Bucket.titulo(bucket)} ($cantidad)") },
                    )
                }
            }

            Box(Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)) {
                BarraBusqueda(
                    valor = ui.busqueda,
                    alCambiar = vm::cambiarBusqueda,
                    marcador = "Buscar por nombre o celular",
                )
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.cargando && ui.lista.isEmpty() -> CajaCargando("Cargando clientes...")
                    ui.error != null && ui.lista.isEmpty() -> CajaError(ui.error!!) { vm.cargar() }
                    ui.lista.isEmpty() -> CajaVacia(
                        if (ui.busqueda.isBlank()) "Sin clientes en \"${Bucket.titulo(ui.tab)}\""
                        else "Sin resultados para \"${ui.busqueda}\"",
                        if (ui.tab == Bucket.ACTIVO) "Registra el primer cliente con el botón +"
                        else null,
                    )
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(ui.lista, key = { it.id }) { c ->
                            TarjetaCliente(c) { vm.verDetalle(c.id, avisar) }
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
            Icon(Icons.Default.Add, "Nuevo cliente")
        }
    }

    // Las hojas (detalle, formulario, cobro, renovación) y los diálogos viven
    // en archivos aparte para no inflar esta pantalla.
    HojasDeClientes(vm = vm, ui = ui, esAdmin = esAdmin, avisar = avisar)
}

@Composable
private fun TarjetaCliente(c: ClienteDto, alAbrir: () -> Unit) {
    TarjetaSeccion(modifier = Modifier.clickable(onClick = alAbrir), relleno = 14.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    c.nombreCompleto,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append(c.horario ?: "Sin horario")
                        if (c.celular.isNotBlank()) append(" · ${c.celular}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EtiquetaMembresia(c.membresiaActiva)
            val saldo = c.membresiaActiva?.saldoPendiente ?: 0.0
            if (saldo > 0.009) {
                Etiqueta("Debe ${Formato.bs(saldo)}", ColorEtiqueta.Amarillo)
            }
        }

        c.membresiaActiva?.takeIf { it.porVencer && c.bucket == Bucket.ACTIVO }?.let { m ->
            Spacer(Modifier.height(6.dp))
            AlertaVencimiento(m.diasRestantes)
        }

        if (c.motivo.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                c.motivo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Replica la lógica de badge de membresía de ClientesPage.jsx. */
@Composable
fun EtiquetaMembresia(m: ClienteMembresiaDto?) {
    if (m == null) {
        Etiqueta("Sin membresía", ColorEtiqueta.Gris)
        return
    }
    if (!m.vigente) {
        Etiqueta("Vencida", ColorEtiqueta.Rojo)
        return
    }

    val clasesRestantes = m.clasesRestantes
    if (clasesRestantes != null && m.clasesTotales != null) {
        val color = when {
            clasesRestantes <= 1 -> ColorEtiqueta.Rojo
            clasesRestantes <= 3 -> ColorEtiqueta.Amarillo
            else -> ColorEtiqueta.Verde
        }
        Etiqueta(
            "${m.membresiaNombre ?: "Plan"} · $clasesRestantes/${m.clasesTotales} clases",
            color,
        )
    } else {
        val color = when {
            m.diasRestantes <= 3 -> ColorEtiqueta.Rojo
            m.diasRestantes <= 7 -> ColorEtiqueta.Amarillo
            else -> ColorEtiqueta.Verde
        }
        Etiqueta("${m.membresiaNombre ?: "Plan"} · ${m.diasRestantes} días", color)
    }
}
