package bo.zonaaquatica.movil.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.UsuarioDto
import bo.zonaaquatica.movil.data.repo.AuthRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiLogin(
    val username: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val usuario: UsuarioDto? = null,
)

class LoginViewModel(private val auth: AuthRepo) : ViewModel() {

    private val _ui = MutableStateFlow(UiLogin())
    val ui: StateFlow<UiLogin> = _ui.asStateFlow()

    fun cambiarUsername(v: String) {
        _ui.value = _ui.value.copy(username = v, error = null)
    }

    fun cambiarPassword(v: String) {
        _ui.value = _ui.value.copy(password = v, error = null)
    }

    fun entrar() {
        val estado = _ui.value
        if (estado.cargando) return
        _ui.value = estado.copy(cargando = true, error = null)

        viewModelScope.launch {
            when (val r = auth.login(estado.username, estado.password)) {
                is Resultado.Ok ->
                    _ui.value = _ui.value.copy(cargando = false, usuario = r.dato, password = "")
                is Resultado.Error ->
                    _ui.value = _ui.value.copy(cargando = false, error = r.mensaje)
            }
        }
    }
}
