package bo.zonaaquatica.movil.ui.personal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.data.model.UsuarioDto
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.CajaCargando
import bo.zonaaquatica.movil.ui.common.CajaError
import bo.zonaaquatica.movil.ui.common.CajaVacia
import bo.zonaaquatica.movil.ui.common.CampoSelector
import bo.zonaaquatica.movil.ui.common.CampoTexto
import bo.zonaaquatica.movil.ui.common.ColorEtiqueta
import bo.zonaaquatica.movil.ui.common.DialogoConfirmar
import bo.zonaaquatica.movil.ui.common.Etiqueta
import bo.zonaaquatica.movil.ui.common.FilaDato
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion
import bo.zonaaquatica.movil.ui.nav.LocalAviso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScreen(usuarioActualId: Int) {
    val contenedor = LocalContenedor.current
    val avisar = LocalAviso.current
    val contexto = LocalContext.current
    val vm: PersonalViewModel = viewModel { PersonalViewModel(contenedor.personal) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.cargar() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 14.dp, 16.dp, 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${ui.lista.size} usuario(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = vm::alternarInactivos) {
                    Text(if (ui.incluirInactivos) "Ocultar inactivos" else "Mostrar inactivos")
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.cargando && ui.lista.isEmpty() -> CajaCargando("Cargando personal...")
                    ui.error != null && ui.lista.isEmpty() -> CajaError(ui.error!!) { vm.cargar() }
                    ui.lista.isEmpty() -> CajaVacia("Sin usuarios registrados")
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(ui.lista, key = { it.id }) { u ->
                            TarjetaUsuario(
                                u = u,
                                esYoMismo = u.id == usuarioActualId,
                                alEditar = { vm.abrirEditar(u) },
                                alResetear = { vm.pedirReset(u) },
                                alCambiarEstado = { vm.cambiarEstado(u, avisar) },
                                alEliminar = { vm.pedirEliminar(u) },
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
            Icon(Icons.Default.Add, "Nuevo usuario")
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
                    if (ui.editandoId == null) "Nuevo usuario" else "Editar usuario",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (ui.editandoId == null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Se generará una contraseña temporal que se muestra una sola vez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                    CampoTexto(
                        form.ci,
                        { v -> vm.editarForm { it.copy(ci = v) } },
                        "CI",
                        requerido = true,
                        tipoTeclado = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
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
                    form.username,
                    { v -> vm.editarForm { it.copy(username = v) } },
                    "Usuario",
                    requerido = true,
                    soporte = "Mínimo 3 caracteres: letras, números, punto y guion bajo",
                )
                Spacer(Modifier.height(10.dp))
                CampoSelector(
                    etiqueta = "Rol",
                    opciones = FormPersonal.ROLES,
                    seleccion = form.rol,
                    textoDe = { r: String -> if (r == "admin") "Administrador" else "Recepcionista" },
                    alSeleccionar = { r -> vm.editarForm { it.copy(rol = r ?: "recepcionista") } },
                    permiteVacio = false,
                )

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

    // ─── Contraseña temporal (se muestra una sola vez) ───
    ui.passwordTemporal?.let { (usuario, password) ->
        AlertDialog(
            onDismissRequest = vm::cerrarPasswordTemporal,
            title = { Text("Contraseña temporal") },
            text = {
                Column {
                    Text(
                        "Entrégala a @$usuario. No se puede volver a mostrar: el sistema " +
                            "guarda las contraseñas encriptadas con bcrypt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp),
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            password,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                        IconButton(onClick = {
                            copiar(contexto, password)
                            avisar("Contraseña copiada")
                        }) {
                            Icon(Icons.Outlined.ContentCopy, "Copiar")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "En su primer ingreso la app le pedirá definir una contraseña propia.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = vm::cerrarPasswordTemporal) { Text("Listo") }
            },
        )
    }

    ui.porResetear?.let { u ->
        DialogoConfirmar(
            titulo = "Restablecer contraseña",
            mensaje = "Se generará una nueva contraseña temporal para @${u.username}. " +
                "La actual dejará de funcionar.",
            textoConfirmar = "Restablecer",
            alConfirmar = { vm.resetearPassword(avisar) },
            alCerrar = vm::cancelarReset,
        )
    }

    ui.porEliminar?.let { u ->
        DialogoConfirmar(
            titulo = "Eliminar usuario",
            mensaje = "¿Eliminar a @${u.username}? Si tiene actividad registrada, " +
                "conviene desactivarlo en lugar de eliminarlo.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            alConfirmar = { vm.eliminar(avisar) },
            alCerrar = vm::cancelarEliminar,
        )
    }
}

@Composable
private fun TarjetaUsuario(
    u: UsuarioDto,
    esYoMismo: Boolean,
    alEditar: () -> Unit,
    alResetear: () -> Unit,
    alCambiarEstado: () -> Unit,
    alEliminar: () -> Unit,
) {
    TarjetaSeccion {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    u.nombreCompleto.ifBlank { u.username },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "@${u.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Etiqueta(
                    if (u.esAdmin) "Administrador" else "Recepcionista",
                    if (u.esAdmin) ColorEtiqueta.Azul else ColorEtiqueta.Gris,
                )
                Spacer(Modifier.height(5.dp))
                if (u.activo) Etiqueta("Activo", ColorEtiqueta.Verde)
                else Etiqueta("Inactivo", ColorEtiqueta.Rojo)
            }
        }

        Spacer(Modifier.height(10.dp))
        FilaDato("CI", u.ci.ifBlank { "—" })
        FilaDato("Último ingreso", Formato.fechaHora(u.ultimoLogin))
        if (u.esPasswordTemporal) {
            Spacer(Modifier.height(6.dp))
            Etiqueta("Debe cambiar su contraseña", ColorEtiqueta.Amarillo)
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = alEditar) { Icon(Icons.Outlined.Edit, "Editar") }
            IconButton(onClick = alResetear) {
                Icon(Icons.Outlined.LockReset, "Restablecer contraseña")
            }
            IconButton(onClick = alCambiarEstado, enabled = !esYoMismo) {
                Icon(
                    if (u.activo) Icons.Outlined.ToggleOn else Icons.Outlined.ToggleOff,
                    if (u.activo) "Desactivar" else "Activar",
                )
            }
            IconButton(onClick = alEliminar, enabled = !esYoMismo) {
                Icon(
                    Icons.Outlined.Delete,
                    "Eliminar",
                    tint = if (esYoMismo) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.error,
                )
            }
        }
        if (esYoMismo) {
            Text(
                "Es tu propia cuenta: no puedes desactivarla ni eliminarla.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun copiar(contexto: Context, texto: String) {
    val portapapeles = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    portapapeles.setPrimaryClip(ClipData.newPlainText("Zona Aquática", texto))
}
