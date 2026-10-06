package bo.zonaaquatica.movil.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.Contenedor
import bo.zonaaquatica.movil.core.Prefs
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.UsuarioDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** En qué punto del arranque/sesión estamos. */
enum class EstadoSesion { Comprobando, SinSesion, DebeCambiarPassword, Dentro }

data class UiSesion(
    val estado: EstadoSesion = EstadoSesion.Comprobando,
    val usuario: UsuarioDto? = null,
    val urlApi: String = "",
)

/**
 * Equivalente al AuthContext del frontend web: mantiene el usuario, decide si
 * hay que forzar el cambio de contraseña y expone la URL del backend.
 */
class SesionViewModel(private val contenedor: Contenedor) : ViewModel() {

    private val _ui = MutableStateFlow(UiSesion(urlApi = contenedor.prefs.urlApi))
    val ui: StateFlow<UiSesion> = _ui.asStateFlow()

    init {
        comprobarSesionGuardada()
        observarExpiracion()
    }

    private fun observarExpiracion() {
        viewModelScope.launch {
            contenedor.sesionExpirada.collect { expirada ->
                if (expirada) {
                    contenedor.prefs.limpiarSesion()
                    _ui.value = _ui.value.copy(
                        estado = EstadoSesion.SinSesion,
                        usuario = null,
                    )
                    contenedor.consumirSesionExpirada()
                }
            }
        }
    }

    private fun comprobarSesionGuardada() {
        if (!contenedor.prefs.haySesion) {
            _ui.value = _ui.value.copy(estado = EstadoSesion.SinSesion)
            return
        }
        viewModelScope.launch {
            when (val r = contenedor.auth.yo()) {
                is Resultado.Ok -> aplicarUsuario(r.dato)
                is Resultado.Error -> {
                    contenedor.prefs.limpiarSesion()
                    _ui.value = _ui.value.copy(estado = EstadoSesion.SinSesion)
                }
            }
        }
    }

    fun aplicarUsuario(usuario: UsuarioDto) {
        _ui.value = _ui.value.copy(
            usuario = usuario,
            estado = if (usuario.esPasswordTemporal) EstadoSesion.DebeCambiarPassword
            else EstadoSesion.Dentro,
        )
    }

    /** Tras cambiar la contraseña ya puede entrar normalmente. */
    fun passwordActualizada() {
        val u = _ui.value.usuario?.copy(esPasswordTemporal = false)
        _ui.value = _ui.value.copy(usuario = u, estado = EstadoSesion.Dentro)
    }

    /**
     * Sale de inmediato y avisa al servidor en segundo plano. Antes se
     * esperaba la respuesta del backend: con el servidor inalcanzable el boton
     * tardaba hasta 20 s sin dar senal y parecia que no funcionaba.
     */
    fun cerrarSesion() {
        val token = contenedor.auth.cerrarSesionLocal()
        _ui.value = _ui.value.copy(estado = EstadoSesion.SinSesion, usuario = null)
        viewModelScope.launch { contenedor.auth.avisarCierreAlServidor(token) }
    }

    fun guardarUrlApi(nueva: String) {
        contenedor.prefs.urlApi = nueva
        _ui.value = _ui.value.copy(urlApi = contenedor.prefs.urlApi)
    }

    fun normalizarUrl(cruda: String): String = Prefs.normalizarUrl(cruda)
}
