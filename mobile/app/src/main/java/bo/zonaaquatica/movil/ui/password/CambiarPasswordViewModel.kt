package bo.zonaaquatica.movil.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.repo.AuthRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiCambiarPassword(
    val actual: String = "",
    val nueva: String = "",
    val repetir: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val listo: Boolean = false,
)

class CambiarPasswordViewModel(private val auth: AuthRepo) : ViewModel() {

    private val _ui = MutableStateFlow(UiCambiarPassword())
    val ui: StateFlow<UiCambiarPassword> = _ui.asStateFlow()

    fun cambiarActual(v: String) { _ui.value = _ui.value.copy(actual = v, error = null) }
    fun cambiarNueva(v: String) { _ui.value = _ui.value.copy(nueva = v, error = null) }
    fun cambiarRepetir(v: String) { _ui.value = _ui.value.copy(repetir = v, error = null) }

    fun guardar() {
        val e = _ui.value
        if (e.cargando) return

        val problema = validar(e)
        if (problema != null) {
            _ui.value = e.copy(error = problema)
            return
        }

        _ui.value = e.copy(cargando = true, error = null)
        viewModelScope.launch {
            when (val r = auth.cambiarPassword(e.actual, e.nueva)) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(cargando = false, listo = true)
                is Resultado.Error -> _ui.value = _ui.value.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    /** Mismas reglas que backend/app/utils/validators.py para avisar antes de la red. */
    private fun validar(e: UiCambiarPassword): String? = when {
        e.actual.isBlank() -> "Ingresa tu contraseña actual"
        e.nueva.length < 6 -> "La nueva contraseña debe tener al menos 6 caracteres"
        e.nueva.none { it.isLetter() } -> "La nueva contraseña debe incluir al menos una letra"
        e.nueva.none { it.isDigit() } -> "La nueva contraseña debe incluir al menos un número"
        e.nueva != e.repetir -> "Las contraseñas nuevas no coinciden"
        else -> null
    }
}
