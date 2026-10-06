package bo.zonaaquatica.movil.ui.contenido

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.common.CajaCargando
import bo.zonaaquatica.movil.ui.common.CajaError
import bo.zonaaquatica.movil.ui.common.CampoSelector
import bo.zonaaquatica.movil.ui.common.CampoTexto
import bo.zonaaquatica.movil.ui.common.ColorEtiqueta
import bo.zonaaquatica.movil.ui.common.Etiqueta
import bo.zonaaquatica.movil.ui.common.FilaInterruptor
import bo.zonaaquatica.movil.ui.common.TarjetaSeccion
import bo.zonaaquatica.movil.ui.nav.LocalAviso

/** Límite de subida, para no mandar una foto de 20 MB por WiFi local. */
private const val MAX_BYTES = 8 * 1024 * 1024

/**
 * Equivalente a admin/ContenidoPublicoPage.jsx: edita el contenido de la
 * landing pública. Las imágenes se suben desde la galería del teléfono con el
 * selector de fotos del sistema (no pide permiso de almacenamiento).
 */
@Composable
fun ContenidoScreen() {
    val contenedor = LocalContenedor.current
    val avisar = LocalAviso.current
    val contexto = LocalContext.current
    val vm: ContenidoViewModel = viewModel { ContenidoViewModel(contenedor.contenido) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    var tipoPendiente by remember { mutableStateOf(TipoImagen.Galeria) }
    var urlManual by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.cargar() }

    val selectorFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val leido = leerArchivo(contexto, uri)
        when {
            leido == null -> avisar("No se pudo leer la imagen")
            leido.bytes.isEmpty() -> avisar("La imagen pesa más de 8 MB")
            else -> vm.subirImagen(
                bytes = leido.bytes,
                nombreArchivo = leido.nombre,
                mime = leido.mime,
                tipo = tipoPendiente,
                alTerminar = avisar,
            )
        }
    }

    val selectorModelo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // El .glb puede ser más grande que una foto, así que se permite más.
        val leido = leerArchivo(contexto, uri, maxBytes = 40 * 1024 * 1024)
        val extension = leido?.nombre?.substringAfterLast('.', "")?.lowercase()
        when {
            leido == null -> avisar("No se pudo leer el archivo")
            extension !in listOf("glb", "gltf") -> avisar("Usa un archivo .glb o .gltf")
            leido.bytes.isEmpty() -> avisar("El modelo pesa más de 40 MB")
            else -> vm.subirModelo3d(leido.bytes, leido.nombre, avisar)
        }
    }

    val elegirImagen: (TipoImagen) -> Unit = { tipo ->
        tipoPendiente = tipo
        selectorFoto.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    if (ui.cargando && ui.editado == null) {
        CajaCargando("Cargando contenido...")
        return
    }
    if (ui.error != null && ui.editado == null) {
        CajaError(ui.error!!) { vm.cargar() }
        return
    }
    val c = ui.editado ?: return

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ─── Identidad ───
            item {
                TarjetaSeccion {
                    Text("Identidad", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    CampoTexto(c.nombre, { v -> vm.editar { it.copy(nombre = v) } }, "Nombre")
                    Spacer(Modifier.height(10.dp))
                    FilaImagen(
                        etiqueta = "Logo",
                        url = c.logoUrl,
                        subiendo = ui.subiendo == TipoImagen.Logo,
                        ayuda = "Se recorta a cuadrado automáticamente",
                        alElegir = { elegirImagen(TipoImagen.Logo) },
                        alQuitar = { vm.editar { it.copy(logoUrl = "") } },
                    )
                }
            }

            // ─── Portada ───
            item {
                TarjetaSeccion {
                    Text("Portada", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    CampoTexto(
                        c.heroBadge,
                        { v -> vm.editar { it.copy(heroBadge = v) } },
                        "Etiqueta superior",
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        c.heroTitulo,
                        { v -> vm.editar { it.copy(heroTitulo = v) } },
                        "Título",
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        c.heroSubtitulo,
                        { v -> vm.editar { it.copy(heroSubtitulo = v) } },
                        "Subtítulo",
                        lineas = 3,
                    )
                    Spacer(Modifier.height(10.dp))
                    FilaImagen(
                        etiqueta = "Imagen de portada",
                        url = c.heroImagenUrl,
                        subiendo = ui.subiendo == TipoImagen.Hero,
                        alElegir = { elegirImagen(TipoImagen.Hero) },
                        alQuitar = { vm.editar { it.copy(heroImagenUrl = "") } },
                    )
                }
            }

            // ─── Sobre nosotros ───
            item {
                TarjetaSeccion {
                    Text("Sobre la piscina", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    CampoTexto(
                        c.sobreTexto,
                        { v -> vm.editar { it.copy(sobreTexto = v) } },
                        "Texto",
                        lineas = 4,
                    )
                }
            }

            // ─── Destacados ───
            item {
                TarjetaSeccion {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Destacados", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = vm::agregarDestacado) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar")
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    c.destacados.forEachIndexed { i, d ->
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "#${i + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { vm.quitarDestacado(i) }) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        "Quitar",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            CampoSelector(
                                etiqueta = "Icono",
                                opciones = ICONOS_DESTACADO,
                                seleccion = d.icono,
                                textoDe = { it },
                                alSeleccionar = { icono ->
                                    vm.editarDestacado(i) { it.copy(icono = icono ?: "droplet") }
                                },
                                permiteVacio = false,
                            )
                            Spacer(Modifier.height(8.dp))
                            CampoTexto(
                                d.titulo,
                                { v -> vm.editarDestacado(i) { it.copy(titulo = v) } },
                                "Título",
                            )
                            Spacer(Modifier.height(8.dp))
                            CampoTexto(
                                d.texto,
                                { v -> vm.editarDestacado(i) { it.copy(texto = v) } },
                                "Texto",
                            )
                        }
                        HorizontalDivider()
                    }
                    if (c.destacados.isEmpty()) {
                        Text(
                            "Sin destacados. La web mostrará esa sección vacía.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ─── Estadísticas ───
            item {
                TarjetaSeccion {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Estadísticas", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = vm::agregarEstadistica) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar")
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    c.estadisticas.forEachIndexed { i, e ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CampoTexto(
                                e.valor,
                                { v -> vm.editarEstadistica(i) { it.copy(valor = v) } },
                                "Valor",
                                modifier = Modifier.weight(0.8f),
                            )
                            Spacer(Modifier.width(8.dp))
                            CampoTexto(
                                e.etiqueta,
                                { v -> vm.editarEstadistica(i) { it.copy(etiqueta = v) } },
                                "Etiqueta",
                                modifier = Modifier.weight(1.4f),
                            )
                            IconButton(onClick = { vm.quitarEstadistica(i) }) {
                                Icon(
                                    Icons.Outlined.Close,
                                    "Quitar",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            // ─── Galería ───
            item {
                TarjetaSeccion {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Galería", style = MaterialTheme.typography.titleSmall)
                        Etiqueta("${c.galeria.size} imágenes")
                    }
                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { elegirImagen(TipoImagen.Galeria) },
                        enabled = ui.subiendo == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (ui.subiendo == TipoImagen.Galeria) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Icon(Icons.Outlined.CloudUpload, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Subir foto del teléfono")
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        CampoTexto(
                            urlManual,
                            { urlManual = it },
                            "…o pegar una URL",
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = { vm.agregarImagenPorUrl(urlManual); urlManual = "" },
                            enabled = urlManual.isNotBlank(),
                        ) { Text("Añadir") }
                    }

                    Spacer(Modifier.height(6.dp))
                    c.galeria.forEachIndexed { i, url ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                url,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { vm.quitarImagenGaleria(i) }) {
                                Icon(
                                    Icons.Outlined.Close,
                                    "Quitar",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            // ─── Contacto ───
            item {
                TarjetaSeccion {
                    Text("Contacto", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    CampoTexto(c.direccion, { v -> vm.editar { it.copy(direccion = v) } }, "Dirección")
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        c.telefono,
                        { v -> vm.editar { it.copy(telefono = v) } },
                        "Teléfono",
                        tipoTeclado = KeyboardType.Phone,
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        c.whatsapp,
                        { v -> vm.editar { it.copy(whatsapp = v) } },
                        "WhatsApp (número)",
                        tipoTeclado = KeyboardType.Phone,
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        c.whatsappLink,
                        { v -> vm.editar { it.copy(whatsappLink = v) } },
                        "Enlace de WhatsApp",
                        soporte = "Ej: https://wa.me/591XXXXXXXX",
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        c.googleMapsLink,
                        { v -> vm.editar { it.copy(googleMapsLink = v) } },
                        "Enlace de Google Maps",
                    )
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(
                        c.horarioTexto,
                        { v -> vm.editar { it.copy(horarioTexto = v) } },
                        "Horario (texto)",
                    )
                }
            }

            // ─── Redes sociales ───
            item {
                TarjetaSeccion {
                    Text("Redes sociales", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    CampoTexto(c.facebook, { v -> vm.editar { it.copy(facebook = v) } }, "Facebook")
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(c.instagram, { v -> vm.editar { it.copy(instagram = v) } }, "Instagram")
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(c.tiktok, { v -> vm.editar { it.copy(tiktok = v) } }, "TikTok")
                    Spacer(Modifier.height(10.dp))
                    CampoTexto(c.youtube, { v -> vm.editar { it.copy(youtube = v) } }, "YouTube")
                }
            }

            // ─── Secciones visibles y modelo 3D ───
            item {
                TarjetaSeccion {
                    Text("Secciones de la web", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    FilaInterruptor("Mostrar planes", c.mostrarPlanes) { v ->
                        vm.editar { it.copy(mostrarPlanes = v) }
                    }
                    Spacer(Modifier.height(4.dp))
                    FilaInterruptor("Mostrar horarios", c.mostrarHorarios) { v ->
                        vm.editar { it.copy(mostrarHorarios = v) }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Recorrido 3D", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (c.modelo3dUrl.isBlank()) "Sin modelo cargado"
                        else c.modelo3dUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { selectorModelo.launch(arrayOf("*/*")) },
                            enabled = ui.subiendo == null,
                            modifier = Modifier.weight(1f),
                        ) { Text("Subir .glb") }
                        if (c.modelo3dUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = { vm.editar { it.copy(modelo3dUrl = "") } },
                                modifier = Modifier.weight(1f),
                            ) { Text("Quitar") }
                        }
                    }
                }
            }
        }

        // ─── Barra de guardado, siempre visible ───
        if (ui.sucio) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = vm::descartar,
                    enabled = !ui.guardando,
                    modifier = Modifier.weight(1f),
                ) { Text("Descartar") }
                Button(
                    onClick = { vm.guardar(avisar) },
                    enabled = !ui.guardando,
                    modifier = Modifier.weight(1.4f),
                ) {
                    if (ui.guardando) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text("Guardar cambios")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaImagen(
    etiqueta: String,
    url: String,
    subiendo: Boolean,
    alElegir: () -> Unit,
    alQuitar: () -> Unit,
    ayuda: String? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(etiqueta, style = MaterialTheme.typography.labelLarge)
            if (url.isNotBlank()) Etiqueta("Cargada", ColorEtiqueta.Verde)
        }
        if (ayuda != null) {
            Text(
                ayuda,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (url.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = alElegir,
                enabled = !subiendo,
                modifier = Modifier.weight(1f),
            ) {
                if (subiendo) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Outlined.CloudUpload, null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Elegir foto")
            }
            if (url.isNotBlank()) {
                OutlinedButton(onClick = alQuitar, modifier = Modifier.weight(1f)) {
                    Text("Quitar")
                }
            }
        }
    }
}

private data class ArchivoLeido(val bytes: ByteArray, val nombre: String, val mime: String)

/**
 * Lee un Uri del selector del sistema a memoria, con su nombre y tipo MIME.
 * Primero consulta el tamaño y aborta si pasa el límite: leer una foto de
 * 50 MB entera antes de comprobarlo podría agotar la memoria de la app.
 */
private fun leerArchivo(contexto: Context, uri: Uri, maxBytes: Int = MAX_BYTES): ArchivoLeido? = try {
    val resolver = contexto.contentResolver
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    val meta = metadatosDeUri(contexto, uri)
    val nombre = meta?.first ?: "archivo.${extensionDeMime(mime)}"
    val tamano = meta?.second ?: -1L

    if (tamano > maxBytes) {
        // Se devuelve un marcador vacío para que la pantalla avise del tamaño.
        ArchivoLeido(ByteArray(0), nombre, mime)
    } else {
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) null else ArchivoLeido(bytes, nombre, mime)
    }
} catch (e: Exception) {
    null
}

/** Devuelve (nombre, tamaño en bytes) del Uri, o null si no se pueden leer. */
private fun metadatosDeUri(contexto: Context, uri: Uri): Pair<String?, Long>? =
    contexto.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val iNombre = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        val iTamano = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
        val nombre = if (iNombre >= 0) cursor.getString(iNombre) else null
        val tamano = if (iTamano >= 0 && !cursor.isNull(iTamano)) cursor.getLong(iTamano) else -1L
        nombre to tamano
    }

/** El backend valida por extensión, así que el nombre debe llevar una válida. */
private fun extensionDeMime(mime: String) = when (mime) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    "image/gif" -> "gif"
    else -> "jpg"
}
