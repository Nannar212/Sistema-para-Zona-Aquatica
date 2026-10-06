package bo.zonaaquatica.movil.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.data.model.UsuarioDto
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion
import bo.zonaaquatica.movil.ui.theme.Aqua500
import bo.zonaaquatica.movil.ui.theme.Aqua700
import bo.zonaaquatica.movil.ui.theme.Aqua800

@Composable
fun LoginScreen(
    urlApi: String,
    alGuardarUrl: (String) -> Unit,
    alEntrar: (UsuarioDto) -> Unit,
) {
    val contenedor = LocalContenedor.current
    val vm: LoginViewModel = viewModel { LoginViewModel(contenedor.auth) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    var verPassword by remember { mutableStateOf(false) }
    var dialogoServidor by remember { mutableStateOf(false) }

    LaunchedEffect(ui.usuario) {
        ui.usuario?.let { alEntrar(it) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Aqua800, Aqua700, Aqua500))
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(78.dp)
                    .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(100.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Waves,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Zona Aquática",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "Sistema de gestión · Quillacollo",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
            )

            Spacer(Modifier.height(28.dp))

            TarjetaSeccion(
                modifier = Modifier.widthIn(max = 420.dp),
                relleno = 20.dp,
            ) {
                Text("Ingresar", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = ui.username,
                    onValueChange = vm::cambiarUsername,
                    label = { Text("Usuario") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = ui.password,
                    onValueChange = vm::cambiarPassword,
                    label = { Text("Contraseña") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (verPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    trailingIcon = {
                        IconButton(onClick = { verPassword = !verPassword }) {
                            Icon(
                                if (verPassword) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                if (verPassword) "Ocultar" else "Mostrar",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (ui.error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        ui.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = vm::entrar,
                    enabled = !ui.cargando && ui.username.isNotBlank() && ui.password.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    if (ui.cargando) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text("Entrar", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = { dialogoServidor = true }) {
                        Icon(Icons.Outlined.Dns, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Servidor", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                urlApi,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (dialogoServidor) {
        DialogoServidor(
            urlActual = urlApi,
            alGuardar = { alGuardarUrl(it); dialogoServidor = false },
            alCerrar = { dialogoServidor = false },
        )
    }
}

@Composable
private fun DialogoServidor(
    urlActual: String,
    alGuardar: (String) -> Unit,
    alCerrar: () -> Unit,
) {
    var texto by remember { mutableStateOf(urlActual) }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Dirección del servidor") },
        text = {
            Column {
                Text(
                    "IP de la PC donde corre Docker. Puedes escribir solo la IP: " +
                        "se completa con :5000/api automáticamente.",
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
                Spacer(Modifier.height(10.dp))
                Text(
                    "Celular por WiFi:  192.168.x.x\n" +
                        "Emulador Android:  10.0.2.2",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { alGuardar(texto) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}
