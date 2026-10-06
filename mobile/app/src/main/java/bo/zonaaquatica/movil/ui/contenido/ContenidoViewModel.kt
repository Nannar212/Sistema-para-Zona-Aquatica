package bo.zonaaquatica.movil.ui.contenido

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.ContenidoDto
import bo.zonaaquatica.movil.data.model.DestacadoDto
import bo.zonaaquatica.movil.data.model.EstadisticaDto
import bo.zonaaquatica.movil.data.repo.ContenidoRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Iconos admitidos por la web pública (ICONOS en ContenidoPublicoPage.jsx). */
val ICONOS_DESTACADO = listOf(
    "temperature", "kid", "lifebuoy", "droplet", "clock",
    "users", "wave", "sun", "star", "shield",
)

/** Destino de una imagen subida: el backend lo usa para recortar/redimensionar. */
enum class TipoImagen(val valor: String) {
    Logo("logo"),
    Hero("hero"),
    Galeria("galeria"),
}

data class UiContenido(
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val subiendo: TipoImagen? = null,
    val original: ContenidoDto? = null,
    val editado: ContenidoDto? = null,
    val error: String? = null,
) {
    /** Hay cambios sin guardar. */
    val sucio: Boolean get() = original != null && editado != null && original != editado
}

/** Equivalente a admin/ContenidoPublicoPage.jsx. Solo admin. */
class ContenidoViewModel(private val repo: ContenidoRepo) : ViewModel() {

    private val _ui = MutableStateFlow(UiContenido())
    val ui: StateFlow<UiContenido> = _ui.asStateFlow()

    fun cargar() {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            when (val r = repo.obtener()) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(
                    cargando = false,
                    original = r.dato,
                    editado = r.dato,
                )
                is Resultado.Error -> _ui.value = _ui.value.copy(
                    cargando = false,
                    error = r.mensaje,
                )
            }
        }
    }

    fun editar(bloque: (ContenidoDto) -> ContenidoDto) {
        _ui.value.editado?.let { _ui.value = _ui.value.copy(editado = bloque(it)) }
    }

    fun descartar() {
        _ui.value = _ui.value.copy(editado = _ui.value.original)
    }

    fun guardar(alTerminar: (String) -> Unit) {
        val estado = _ui.value
        val cuerpo = estado.editado ?: return
        if (estado.guardando) return

        _ui.value = estado.copy(guardando = true)
        viewModelScope.launch {
            when (val r = repo.guardar(cuerpo)) {
                is Resultado.Ok -> {
                    val guardado = r.dato ?: cuerpo
                    _ui.value = _ui.value.copy(
                        guardando = false,
                        original = guardado,
                        editado = guardado,
                    )
                    alTerminar("Página pública actualizada")
                }
                is Resultado.Error -> {
                    _ui.value = _ui.value.copy(guardando = false)
                    alTerminar(r.mensaje)
                }
            }
        }
    }

    /**
     * Sube una imagen y la coloca en el campo correspondiente. La URL que
     * devuelve el backend (/uploads/publico/...) queda en el borrador: hay que
     * pulsar Guardar para que la web pública la tome.
     */
    fun subirImagen(
        bytes: ByteArray,
        nombreArchivo: String,
        mime: String,
        tipo: TipoImagen,
        alTerminar: (String) -> Unit,
    ) {
        if (_ui.value.subiendo != null) return
        _ui.value = _ui.value.copy(subiendo = tipo)
        viewModelScope.launch {
            when (val r = repo.subirImagen(bytes, nombreArchivo, mime, tipo.valor)) {
                is Resultado.Ok -> {
                    editar { c ->
                        when (tipo) {
                            TipoImagen.Logo -> c.copy(logoUrl = r.dato)
                            TipoImagen.Hero -> c.copy(heroImagenUrl = r.dato)
                            TipoImagen.Galeria -> c.copy(galeria = c.galeria + r.dato)
                        }
                    }
                    _ui.value = _ui.value.copy(subiendo = null)
                    alTerminar("Imagen subida · recuerda Guardar")
                }
                is Resultado.Error -> {
                    _ui.value = _ui.value.copy(subiendo = null)
                    alTerminar(r.mensaje)
                }
            }
        }
    }

    fun subirModelo3d(bytes: ByteArray, nombreArchivo: String, alTerminar: (String) -> Unit) {
        if (_ui.value.subiendo != null) return
        _ui.value = _ui.value.copy(subiendo = TipoImagen.Galeria)
        viewModelScope.launch {
            when (val r = repo.subirModelo3d(bytes, nombreArchivo)) {
                is Resultado.Ok -> {
                    editar { it.copy(modelo3dUrl = r.dato) }
                    _ui.value = _ui.value.copy(subiendo = null)
                    alTerminar("Modelo 3D subido · recuerda Guardar")
                }
                is Resultado.Error -> {
                    _ui.value = _ui.value.copy(subiendo = null)
                    alTerminar(r.mensaje)
                }
            }
        }
    }

    // ─── Listas editables ───────────────────────────────────────────────────

    fun agregarDestacado() = editar {
        it.copy(destacados = it.destacados + DestacadoDto(titulo = "Nuevo", texto = ""))
    }

    fun editarDestacado(indice: Int, bloque: (DestacadoDto) -> DestacadoDto) = editar { c ->
        c.copy(
            destacados = c.destacados.mapIndexed { i, d -> if (i == indice) bloque(d) else d }
        )
    }

    fun quitarDestacado(indice: Int) = editar { c ->
        c.copy(destacados = c.destacados.filterIndexed { i, _ -> i != indice })
    }

    fun agregarEstadistica() = editar {
        it.copy(estadisticas = it.estadisticas + EstadisticaDto(valor = "", etiqueta = ""))
    }

    fun editarEstadistica(indice: Int, bloque: (EstadisticaDto) -> EstadisticaDto) = editar { c ->
        c.copy(
            estadisticas = c.estadisticas.mapIndexed { i, e -> if (i == indice) bloque(e) else e }
        )
    }

    fun quitarEstadistica(indice: Int) = editar { c ->
        c.copy(estadisticas = c.estadisticas.filterIndexed { i, _ -> i != indice })
    }

    fun agregarImagenPorUrl(url: String) {
        val limpia = url.trim()
        if (limpia.isNotEmpty()) editar { it.copy(galeria = it.galeria + limpia) }
    }

    fun quitarImagenGaleria(indice: Int) = editar { c ->
        c.copy(galeria = c.galeria.filterIndexed { i, _ -> i != indice })
    }
}
