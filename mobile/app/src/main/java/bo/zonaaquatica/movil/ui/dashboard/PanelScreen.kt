package bo.zonaaquatica.movil.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.BarraOcupacion
import bo.zonaaquatica.movil.ui.common.CajaCargando
import bo.zonaaquatica.movil.ui.common.CajaError
import bo.zonaaquatica.movil.ui.common.Etiqueta
import bo.zonaaquatica.movil.ui.common.FilaDato
import bo.zonaaquatica.movil.ui.common.TarjetaIndicador
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion
import bo.zonaaquatica.movil.ui.theme.Sol100
import bo.zonaaquatica.movil.ui.theme.Sol500
import bo.zonaaquatica.movil.ui.theme.VerdeOk
import bo.zonaaquatica.movil.ui.theme.VerdeOkSuave

/** Equivalente a admin/DashboardPage.jsx. Solo visible para el rol admin. */
@Composable
fun PanelScreen() {
    val contenedor = LocalContenedor.current
    val vm: PanelViewModel = viewModel { PanelViewModel(contenedor.dashboard) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.cargar() }

    when {
        ui.cargando && ui.resumen == null -> CajaCargando("Cargando indicadores...")
        ui.error != null && ui.resumen == null -> CajaError(ui.error!!) { vm.cargar() }
        else -> {
            val r = ui.resumen ?: return
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TarjetaIndicador(
                            titulo = "Clientes activos",
                            valor = r.clientesActivos.toString(),
                            icono = Icons.Outlined.Group,
                            modifier = Modifier.weight(1f),
                        )
                        TarjetaIndicador(
                            titulo = "Membresías vigentes",
                            valor = r.membresiasVigentes.toString(),
                            icono = Icons.Outlined.ConfirmationNumber,
                            colorIcono = VerdeOk,
                            fondoIcono = VerdeOkSuave,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TarjetaIndicador(
                            titulo = "Por vencer (3 días)",
                            valor = r.membresiasPorVencer.toString(),
                            icono = Icons.Outlined.WarningAmber,
                            colorIcono = Sol500,
                            fondoIcono = Sol100,
                            modifier = Modifier.weight(1f),
                        )
                        TarjetaIndicador(
                            titulo = "Ingresos de hoy",
                            valor = Formato.bs(r.ingresosHoy),
                            icono = Icons.Outlined.Payments,
                            pie = "Mes: ${Formato.bs(r.ingresosMes)}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ─── Cierre de caja del día ───
                item {
                    TarjetaSeccion {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Caja de hoy", style = MaterialTheme.typography.titleSmall)
                            Etiqueta("${ui.caja?.cantidad ?: 0} pagos")
                        }
                        Spacer(Modifier.height(10.dp))
                        FilaDato("Total cobrado", Formato.bs(ui.caja?.total))
                        FilaDato("Efectivo", Formato.bs(ui.caja?.totalEfectivo))
                        FilaDato("QR", Formato.bs(ui.caja?.totalQr))
                    }
                }

                // ─── Ocupación por horario ───
                item {
                    TarjetaSeccion {
                        Text("Ocupación por horario", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(12.dp))
                        if (r.ocupacionHorarios.isEmpty()) {
                            Text(
                                "Aún no hay horarios registrados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            r.ocupacionHorarios.forEach { o ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(o.horario, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${o.ocupados}/${o.cupo}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                BarraOcupacion(o.ocupados, o.cupo)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }

                // ─── Distribución de planes ───
                item {
                    TarjetaSeccion {
                        Text(
                            "Clientes por plan",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(10.dp))
                        if (r.distribucionMembresias.isEmpty()) {
                            Text(
                                "Sin membresías asignadas todavía.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            r.distribucionMembresias.forEach { d ->
                                FilaDato(d.membresia, "${d.cantidad}")
                            }
                        }
                    }
                }

                item {
                    TarjetaSeccion {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Ingresos del mes",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                Formato.bs(r.ingresosMes),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "El detalle completo con gráficos, egresos y balance está en " +
                                "Ingresos y egresos del panel web (llega en la Parte 3).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
