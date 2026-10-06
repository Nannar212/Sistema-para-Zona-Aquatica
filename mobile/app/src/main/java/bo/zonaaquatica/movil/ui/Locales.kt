package bo.zonaaquatica.movil.ui

import androidx.compose.runtime.staticCompositionLocalOf
import bo.zonaaquatica.movil.Contenedor

/**
 * Da acceso al contenedor de dependencias desde cualquier Composable:
 *   val contenedor = LocalContenedor.current
 *   val vm: XViewModel = viewModel { XViewModel(contenedor.clientes) }
 */
val LocalContenedor = staticCompositionLocalOf<Contenedor> {
    error("No hay Contenedor en el árbol de composición")
}
