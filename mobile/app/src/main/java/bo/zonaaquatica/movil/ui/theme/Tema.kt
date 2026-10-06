package bo.zonaaquatica.movil.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta Zona Aquática — copiada de frontend/tailwind.config.js
val Aqua50 = Color(0xFFE6F1FB)
val Aqua100 = Color(0xFFB5D4F4)
val Aqua200 = Color(0xFF85B7EB)
val Aqua300 = Color(0xFF57A0E0)
val Aqua400 = Color(0xFF378ADD)
val Aqua500 = Color(0xFF185FA5)
val Aqua600 = Color(0xFF0C447C)
val Aqua700 = Color(0xFF093763)
val Aqua800 = Color(0xFF062949)
val Aqua900 = Color(0xFF031A30)

val Sol100 = Color(0xFFFDE7B0)
val Sol300 = Color(0xFFF4B53F)
val Sol500 = Color(0xFFD9871A)
val Sol700 = Color(0xFF7C4C0D)

val Tinta50 = Color(0xFFF5F7FA)
val Tinta100 = Color(0xFFE9EDF2)
val Tinta700 = Color(0xFF334155)
val Tinta800 = Color(0xFF1E293B)
val Tinta900 = Color(0xFF0F172A)

val VerdeOk = Color(0xFF15803D)
val VerdeOkSuave = Color(0xFFDCFCE7)
val RojoError = Color(0xFFB91C1C)
val RojoErrorSuave = Color(0xFFFEE2E2)

private val esquemaClaro = lightColorScheme(
    primary = Aqua600,
    onPrimary = Color.White,
    primaryContainer = Aqua50,
    onPrimaryContainer = Aqua700,
    secondary = Sol500,
    onSecondary = Color.White,
    secondaryContainer = Sol100,
    onSecondaryContainer = Sol700,
    tertiary = Aqua400,
    background = Tinta50,
    onBackground = Tinta900,
    surface = Color.White,
    onSurface = Tinta900,
    surfaceVariant = Tinta100,
    onSurfaceVariant = Tinta700,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = RojoError,
    onError = Color.White,
    errorContainer = RojoErrorSuave,
    onErrorContainer = Color(0xFF7F1D1D),
)

private val esquemaOscuro = darkColorScheme(
    primary = Aqua300,
    onPrimary = Aqua900,
    primaryContainer = Aqua800,
    onPrimaryContainer = Aqua100,
    secondary = Sol300,
    onSecondary = Color(0xFF2E1C04),
    secondaryContainer = Sol700,
    onSecondaryContainer = Sol100,
    tertiary = Aqua200,
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE8EDF4),
    surface = Color(0xFF131C2B),
    onSurface = Color(0xFFE8EDF4),
    surfaceVariant = Color(0xFF1E2A3D),
    onSurfaceVariant = Color(0xFFB9C4D4),
    outline = Color(0xFF3A4860),
    outlineVariant = Color(0xFF2A3850),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF4A0E0E),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val tipografia = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun TemaZonaAquatica(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (oscuro) esquemaOscuro else esquemaClaro,
        typography = tipografia,
        content = content,
    )
}

// ─── Paleta de visualización de datos ────────────────────────────────────────
// Slots categóricos 1 y 2, validados con el verificador de la guía de dataviz:
// claro  (#2a78d6 / #eb6834) y oscuro (#3987e5 / #d95926) pasan los 5 checks
// (banda de luminosidad, piso de croma, separación CVD, piso de visión normal y
// contraste contra la superficie). No cambiar sin volver a validar.
val VizSerie1Claro = Color(0xFF2A78D6)
val VizSerie2Claro = Color(0xFFEB6834)
val VizSerie1Oscuro = Color(0xFF3987E5)
val VizSerie2Oscuro = Color(0xFFD95926)

/** Colores de gráficos resueltos según el tema activo. */
data class ColoresViz(
    val serie1: Color,
    val serie2: Color,
    val rejilla: Color,
    val superficie: Color,
)

@Composable
fun coloresViz(): ColoresViz {
    val oscuro = !MaterialTheme.colorScheme.background.esClaro()
    return ColoresViz(
        serie1 = if (oscuro) VizSerie1Oscuro else VizSerie1Claro,
        serie2 = if (oscuro) VizSerie2Oscuro else VizSerie2Claro,
        rejilla = MaterialTheme.colorScheme.outlineVariant,
        superficie = MaterialTheme.colorScheme.surface,
    )
}

private fun Color.esClaro(): Boolean = (red * 0.299f + green * 0.587f + blue * 0.114f) > 0.5f
