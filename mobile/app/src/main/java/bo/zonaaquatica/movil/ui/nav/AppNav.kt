package bo.zonaaquatica.movil.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bo.zonaaquatica.movil.ui.EstadoSesion
import bo.zonaaquatica.movil.ui.LocalContenedor
import bo.zonaaquatica.movil.ui.SesionViewModel
import bo.zonaaquatica.movil.ui.common.CajaCargando
import bo.zonaaquatica.movil.ui.login.LoginScreen
import bo.zonaaquatica.movil.ui.password.CambiarPasswordScreen

/**
 * Secciones del panel. `soloAdmin` replica el filtro de roles del
 * Sidebar.jsx y de los ProtectedRoute del frontend web.
 */
enum class Seccion(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector,
    val soloAdmin: Boolean,
) {
    Panel("panel", "Panel", Icons.Outlined.SpaceDashboard, true),
    Clientes("clientes", "Clientes", Icons.Outlined.Group, false),
    Asistencia("asistencia", "Asistencia", Icons.AutoMirrored.Outlined.FactCheck, false),
    Horarios("horarios", "Horarios", Icons.Outlined.CalendarMonth, false),
    Membresias("membresias", "Membresías", Icons.Outlined.ConfirmationNumber, true),
    Finanzas("finanzas", "Ingresos y egresos", Icons.AutoMirrored.Outlined.TrendingUp, true),
    Gastos("gastos", "Gastos", Icons.Outlined.ShoppingCart, true),
    Personal("personal", "Personal", Icons.Outlined.ManageAccounts, true),
    Actividad("actividad", "Actividad", Icons.AutoMirrored.Outlined.ListAlt, true),
    Contenido("contenido", "Página pública", Icons.Outlined.Language, true);

    companion object {
        fun paraRol(esAdmin: Boolean): List<Seccion> =
            entries.filter { esAdmin || !it.soloAdmin }

        /** Sección de inicio por rol, igual que RUTA_INICIO del AuthContext web. */
        fun inicioDe(esAdmin: Boolean): Seccion = if (esAdmin) Panel else Clientes
    }
}

/**
 * Raíz de la navegación: decide entre login, cambio de contraseña forzado y
 * el panel, en función del estado de la sesión.
 */
@Composable
fun AppNav() {
    val contenedor = LocalContenedor.current
    val sesionVm: SesionViewModel = viewModel { SesionViewModel(contenedor) }
    val sesion by sesionVm.ui.collectAsStateWithLifecycle()

    when (sesion.estado) {
        EstadoSesion.Comprobando -> CajaCargando("Conectando con Zona Aquática...")

        EstadoSesion.SinSesion -> LoginScreen(
            urlApi = sesion.urlApi,
            alGuardarUrl = sesionVm::guardarUrlApi,
            alEntrar = sesionVm::aplicarUsuario,
        )

        EstadoSesion.DebeCambiarPassword -> CambiarPasswordScreen(
            obligatorio = true,
            alListo = sesionVm::passwordActualizada,
            alCerrarSesion = sesionVm::cerrarSesion,
        )

        EstadoSesion.Dentro -> {
            val usuario = sesion.usuario
            if (usuario == null) {
                CajaCargando()
            } else {
                PanelShell(
                    usuario = usuario,
                    urlApi = sesion.urlApi,
                    alCerrarSesion = sesionVm::cerrarSesion,
                    alGuardarUrl = sesionVm::guardarUrlApi,
                )
            }
        }
    }
}
