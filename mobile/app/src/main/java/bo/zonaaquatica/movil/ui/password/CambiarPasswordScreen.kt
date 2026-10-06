package bo.zonaaquatica.movil.ui.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion

/**
 * Pantalla obligatoria la primera vez que entra un usuario creado por el admin
 * (el backend devuelve es_password_temporal = true).
 *
 * Reglas del backend (utils/validators.py): mínimo 6 caracteres,
 * al menos una letra y al menos un número.
 */
@Composable
fun CambiarPasswordScreen(
    obligatorio: Boolean,
    alListo: () -> Unit,
    alCerrarSesion: () -> Unit,
) {
    val contenedor = LocalContenedor.current
    val vm: CambiarPasswordViewModel = viewModel { CambiarPasswordViewModel(contenedor.auth) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.listo) {
        if (ui.listo) alListo()
    }

    Scaffold { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Outlined.Key,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (obligatorio) "Crea tu contraseña" else "Cambiar contraseña",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (obligatorio)
                    "Estás usando una contraseña temporal. Define una propia para continuar."
                else
                    "Ingresa tu contraseña actual y la nueva.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(22.dp))

            TarjetaSeccion(relleno = 20.dp) {
                CampoPassword(
                    valor = ui.actual,
                    alCambiar = vm::cambiarActual,
                    etiqueta = if (obligatorio) "Contraseña temporal" else "Contraseña actual",
                )
                Spacer(Modifier.height(10.dp))
                CampoPassword(
                    valor = ui.nueva,
                    alCambiar = vm::cambiarNueva,
                    etiqueta = "Contraseña nueva",
                    soporte = "Mínimo 6 caracteres, con al menos una letra y un número",
                )
                Spacer(Modifier.height(10.dp))
                CampoPassword(
                    valor = ui.repetir,
                    alCambiar = vm::cambiarRepetir,
                    etiqueta = "Repetir contraseña nueva",
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
                    onClick = vm::guardar,
                    enabled = !ui.cargando,
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
                        Text("Guardar contraseña")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            TextButton(onClick = alCerrarSesion) { Text("Cerrar sesión") }
        }
    }
}

@Composable
private fun CampoPassword(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    soporte: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        supportingText = soporte?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
        modifier = Modifier.fillMaxWidth(),
    )
}
