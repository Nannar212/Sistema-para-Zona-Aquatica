package bo.zonaaquatica.movil.ui.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.PersonalRequest
import bo.zonaaquatica.movil.data.model.UsuarioDto
import bo.zonaaquatica.movil.data.repo.PersonalRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Espejo de VACIO en admin/PersonalPage.jsx. */
data class FormPersonal(
    val nombres: String = "",
    val apellidos: String = "",
    val ci: String = "",
    val username: String = "",
    val edad: String = "",
    val rol: String = "recepcionista",
) {
    fun aRequest() = PersonalRequest(
        nombres = nombres.trim(),
        apellidos = apellidos.trim(),
        ci = ci.trim(),
        username = username.trim().lowercase(),
        edad = edad.trim().toIntOrNull(),
        rol = rol,
    )

    companion object {
        val ROLES = listOf("admin", "recepcionista")

        fun de(u: UsuarioDto) = FormPersonal(
            nombres = u.nombres,
            apellidos = u.apellidos,
            ci = u.ci,
            username = u.username,
            edad = u.edad?.toString() ?: "",
            rol = u.rol,
        )
    }
}

data class UiPersonal(
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val lista: List<UsuarioDto> = emptyList(),
    val incluirInactivos: Boolean = true,
    val error: String? = null,
    val form: FormPersonal? = null,
    val editandoId: Int? = null,
    val errorForm: String? = null,
    /** Contraseña temporal recién generada: se muestra UNA sola vez. */
    val passwordTemporal: Pair<String, String>? = null,
    val porEliminar: UsuarioDto? = null,
    val porResetear: UsuarioDto? = null,
)

/** Equivalente a admin/PersonalPage.jsx. Solo admin. */
class PersonalViewModel(private val repo: PersonalRepo) : ViewModel() {

    private val _ui = MutableStateFlow(UiPersonal())
    val ui: StateFlow<UiPersonal> = _ui.asStateFlow()

    fun cargar() {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            when (val r = repo.listar(_ui.value.incluirInactivos)) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(cargando = false, lista = r.dato)
                is Resultado.Error -> _ui.value = _ui.value.copy(cargando = false, error = r.mensaje)
            }
        }
    }

    fun alternarInactivos() {
        _ui.value = _ui.value.copy(incluirInactivos = !_ui.value.incluirInactivos)
        cargar()
    }

    fun abrirNuevo() {
        _ui.value = _ui.value.copy(form = FormPersonal(), editandoId = null, errorForm = null)
    }

    fun abrirEditar(u: UsuarioDto) {
        _ui.value = _ui.value.copy(form = FormPersonal.de(u), editandoId = u.id, errorForm = null)
    }

    fun cerrarForm() {
        _ui.value = _ui.value.copy(form = null, editandoId = null, errorForm = null)
    }

    fun editarForm(bloque: (FormPersonal) -> FormPersonal) {
        _ui.value.form?.let { _ui.value = _ui.value.copy(form = bloque(it), errorForm = null) }
    }

    fun cerrarPasswordTemporal() {
        _ui.value = _ui.value.copy(passwordTemporal = null)
    }

    fun pedirEliminar(u: UsuarioDto) { _ui.value = _ui.value.copy(porEliminar = u) }
    fun cancelarEliminar() { _ui.value = _ui.value.copy(porEliminar = null) }
    fun pedirReset(u: UsuarioDto) { _ui.value = _ui.value.copy(porResetear = u) }
    fun cancelarReset() { _ui.value = _ui.value.copy(porResetear = null) }

    fun guardar(alTerminar: (String) -> Unit) {
        val estado = _ui.value
        val form = estado.form ?: return
        if (estado.guardando) return

        val problema = validar(form)
        if (problema != null) {
            _ui.value = estado.copy(errorForm = problema)
            return
        }

        _ui.value = estado.copy(guardando = true, errorForm = null)
        viewModelScope.launch {
            val id = estado.editandoId
            if (id == null) {
                when (val r = repo.crear(form.aRequest())) {
                    is Resultado.Ok -> {
                        val temporal = r.dato.passwordTemporal
                        _ui.value = _ui.value.copy(
                            guardando = false,
                            form = null,
                            editandoId = null,
                            passwordTemporal = if (temporal != null)
                                form.username.trim().lowercase() to temporal else null,
                        )
                        cargar()
                    }
                    is Resultado.Error ->
                        _ui.value = _ui.value.copy(guardando = false, errorForm = r.mensaje)
                }
            } else {
                when (val r = repo.actualizar(id, form.aRequest())) {
                    is Resultado.Ok -> {
                        _ui.value = _ui.value.copy(guardando = false, form = null, editandoId = null)
                        alTerminar("Usuario actualizado")
                        cargar()
                    }
                    is Resultado.Error ->
                        _ui.value = _ui.value.copy(guardando = false, errorForm = r.mensaje)
                }
            }
        }
    }

    fun resetearPassword(alTerminar: (String) -> Unit) {
        val u = _ui.value.porResetear ?: return
        _ui.value = _ui.value.copy(porResetear = null)
        viewModelScope.launch {
            when (val r = repo.resetPassword(u.id)) {
                is Resultado.Ok ->
                    _ui.value = _ui.value.copy(passwordTemporal = u.username to r.dato)
                is Resultado.Error -> alTerminar(r.mensaje)
            }
        }
    }

    fun cambiarEstado(u: UsuarioDto, alTerminar: (String) -> Unit) {
        viewModelScope.launch {
            when (val r = repo.cambiarEstado(u.id)) {
                is Resultado.Ok -> {
                    alTerminar(if (u.activo) "Usuario desactivado" else "Usuario activado")
                    cargar()
                }
                is Resultado.Error -> alTerminar(r.mensaje)
            }
        }
    }

    fun eliminar(alTerminar: (String) -> Unit) {
        val u = _ui.value.porEliminar ?: return
        _ui.value = _ui.value.copy(porEliminar = null)
        viewModelScope.launch {
            when (val r = repo.eliminar(u.id)) {
                is Resultado.Ok -> { alTerminar("Usuario eliminado"); cargar() }
                is Resultado.Error -> alTerminar(r.mensaje)
            }
        }
    }

    /** Mismas reglas que validar_username() del backend. */
    private fun validar(f: FormPersonal): String? {
        val usuario = f.username.trim()
        return when {
            f.nombres.isBlank() -> "Los nombres son obligatorios"
            f.apellidos.isBlank() -> "Los apellidos son obligatorios"
            f.ci.isBlank() -> "El CI es obligatorio"
            usuario.length < 3 -> "El usuario debe tener al menos 3 caracteres"
            !Regex("^[a-zA-Z0-9_.]+$").matches(usuario) ->
                "El usuario solo admite letras, números, punto y guion bajo"
            f.rol !in FormPersonal.ROLES -> "Rol inválido"
            else -> null
        }
    }
}
