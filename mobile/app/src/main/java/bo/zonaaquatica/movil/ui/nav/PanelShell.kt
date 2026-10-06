package bo.zonaaquatica.movil.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bo.zonaaquatica.movil.data.model.UsuarioDto
import bo.zonaaquatica.movil.ui.actividad.ActividadScreen
import bo.zonaaquatica.movil.ui.asistencia.AsistenciaScreen
import bo.zonaaquatica.movil.ui.clientes.ClientesScreen
import bo.zonaaquatica.movil.ui.common.DialogoConfirmar
import bo.zonaaquatica.movil.ui.contenido.ContenidoScreen
import bo.zonaaquatica.movil.ui.dashboard.PanelScreen
import bo.zonaaquatica.movil.ui.finanzas.FinanzasScreen
import bo.zonaaquatica.movil.ui.gastos.GastosScreen
import bo.zonaaquatica.movil.ui.horarios.HorariosScreen
import bo.zonaaquatica.movil.ui.membresias.MembresiasScreen
import bo.zonaaquatica.movil.ui.personal.PersonalScreen
import bo.zonaaquatica.movil.ui.theme.Aqua600
import bo.zonaaquatica.movil.ui.theme.Aqua700
import kotlinx.coroutines.launch

/** Permite a cualquier pantalla mostrar un mensaje en el Snackbar del shell. */
val LocalAviso = staticCompositionLocalOf<(String) -> Unit> { {} }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelShell(
    usuario: UsuarioDto,
    urlApi: String,
    alCerrarSesion: () -> Unit,
    alGuardarUrl: (String) -> Unit,
) {
    val secciones = remember(usuario.rol) { Seccion.paraRol(usuario.esAdmin) }
    var actual by remember(usuario.rol) { mutableStateOf(Seccion.inicioDe(usuario.esAdmin)) }

    val estadoCajon = rememberDrawerState(DrawerValue.Closed)
    val alcance = rememberCoroutineScope()
    val estadoSnackbar = remember { SnackbarHostState() }
    var confirmarSalida by remember { mutableStateOf(false) }
    var dialogoServidor by remember { mutableStateOf(false) }

    val avisar: (String) -> Unit = { mensaje ->
        alcance.launch {
            estadoSnackbar.currentSnackbarData?.dismiss()
            estadoSnackbar.showSnackbar(mensaje)
        }
    }

    ModalNavigationDrawer(
        drawerState = estadoCajon,
        drawerContent = {
            ModalDrawerSheet {
                CabeceraCajon(usuario, urlApi)
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                secciones.forEach { seccion ->
                    NavigationDrawerItem(
                        icon = { Icon(seccion.icono, null) },
                        label = { Text(seccion.titulo) },
                        selected = seccion == actual,
                        onClick = {
                            actual = seccion
                            alcance.launch { estadoCajon.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Dns, null) },
                    label = { Text("Servidor") },
                    selected = false,
                    onClick = {
                        alcance.launch { estadoCajon.close() }
                        dialogoServidor = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                    label = { Text("Cerrar sesión") },
                    selected = false,
                    onClick = {
                        alcance.launch { estadoCajon.close() }
                        confirmarSalida = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Lunes a viernes · Quillacollo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp, bottom = 16.dp),
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(actual.titulo, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (usuario.esAdmin) "Administrador" else "Recepcionista",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { alcance.launch { estadoCajon.open() } }) {
                            Icon(Icons.Default.Menu, "Menú")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Aqua600,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                )
            },
            snackbarHost = { SnackbarHost(estadoSnackbar) },
        ) { relleno ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(relleno)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                CompositionLocalProvider(LocalAviso provides avisar) {
                    when (actual) {
                        Seccion.Panel -> PanelScreen()
                        Seccion.Clientes -> ClientesScreen(esAdmin = usuario.esAdmin)
                        Seccion.Asistencia -> AsistenciaScreen()
                        Seccion.Horarios -> HorariosScreen(esAdmin = usuario.esAdmin)
                        Seccion.Membresias -> MembresiasScreen()
                        Seccion.Finanzas -> FinanzasScreen()
                        Seccion.Gastos -> GastosScreen()
                        Seccion.Personal -> PersonalScreen(usuarioActualId = usuario.id)
                        Seccion.Actividad -> ActividadScreen()
                        Seccion.Contenido -> ContenidoScreen()
                    }
                }
            }
        }
    }

    if (dialogoServidor) {
        var texto by remember { mutableStateOf(urlApi) }
        AlertDialog(
            onDismissRequest = { dialogoServidor = false },
            title = { Text("Dirección del servidor") },
            text = {
                Column {
                    Text(
                        "IP de la PC donde corre Docker. Si el router le cambia la " +
                            "IP, corrígela aquí sin cerrar sesión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        label = { Text("URL o IP") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    alGuardarUrl(texto)
                    dialogoServidor = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { dialogoServidor = false }) { Text("Cancelar") }
            },
        )
    }

    if (confirmarSalida) {
        DialogoConfirmar(
            titulo = "Cerrar sesión",
            mensaje = "¿Quieres salir de tu cuenta?",
            textoConfirmar = "Cerrar sesión",
            alConfirmar = alCerrarSesion,
            alCerrar = { confirmarSalida = false },
        )
    }
}

@Composable
private fun CabeceraCajon(usuario: UsuarioDto, urlApi: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Aqua700)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(100.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Waves, null, tint = Color.White)
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    "Zona Aquática",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    "Sistema de gestión",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            usuario.nombreCompleto.ifBlank { usuario.username },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
        Text(
            "@${usuario.username}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            urlApi,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f),
        )
    }
}
