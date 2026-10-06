package bo.zonaaquatica.movil.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.dp
import bo.zonaaquatica.movil.core.Formato
import kotlin.math.abs
import kotlin.math.max

/*
 * Gráficos dibujados con Canvas de Compose, siguiendo las especificaciones de
 * marcas de la guía de visualización:
 *
 *  · Líneas de 2 dp con unión y extremo redondeados; marcadores de 8 dp con
 *    anillo de 2 dp del color de la superficie.
 *  · Barras de 24 dp como máximo, extremo de datos redondeado 4 dp y cuadrado
 *    en la línea base, con 2 dp de aire entre barras vecinas.
 *  · Rejilla y ejes: hairline de 1 dp, SÓLIDO (nunca punteado), un paso por
 *    encima de la superficie.
 *  · Un solo eje: ingresos y egresos están los dos en Bs, así que comparten
 *    escala (nunca doble eje).
 *  · Leyenda siempre presente con 2 series, más etiqueta directa en el último
 *    punto. El valor exacto de cada día se consigue tocando el gráfico y
 *    también en la vista de tabla, así que ningún dato depende del toque.
 */

// ─── Leyenda ────────────────────────────────────────────────────────────────

data class SerieLeyenda(val nombre: String, val color: Color, val total: String? = null)

@Composable
fun Leyenda(series: List<SerieLeyenda>, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(s.color),
                )
                Spacer(Modifier.width(6.dp))
                // El texto usa tinta de texto, nunca el color de la serie.
                Text(
                    s.nombre,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (s.total != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        s.total,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ─── Gráfico de líneas: 2 series en un mismo eje ────────────────────────────

data class PuntoSerie(val etiqueta: String, val valorA: Double, val valorB: Double)

/**
 * Evolución diaria de dos medidas de la misma unidad (Bs). Al tocar se marca
 * el día y se devuelve su índice para que la pantalla muestre los valores.
 */
@Composable
fun GraficoLineasDoble(
    puntos: List<PuntoSerie>,
    colorA: Color,
    colorB: Color,
    colorRejilla: Color,
    colorSuperficie: Color,
    seleccionado: Int?,
    alSeleccionar: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    altura: Int = 170,
) {
    if (puntos.isEmpty()) {
        CajaVacia("Sin movimientos en el período")
        return
    }

    val maximo = remember(puntos) {
        max(
            puntos.maxOf { max(it.valorA, it.valorB) },
            1.0,
        )
    }
    var anchoCanvas by remember { mutableStateOf(1f) }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(altura.dp)
            .pointerInput(puntos.size) {
                detectTapGestures { toque ->
                    if (puntos.size < 2) {
                        alSeleccionar(if (puntos.isEmpty()) null else 0)
                        return@detectTapGestures
                    }
                    val paso = size.width.toFloat() / (puntos.size - 1)
                    val indice = (toque.x / paso).toInt().coerceIn(0, puntos.lastIndex)
                    // Ajusta al punto realmente más cercano (el hit area es toda
                    // la franja vertical, muy por encima del mínimo de 24 dp).
                    val vecino = (indice + 1).coerceAtMost(puntos.lastIndex)
                    val mejor = if (abs(toque.x - indice * paso) <= abs(toque.x - vecino * paso))
                        indice else vecino
                    alSeleccionar(if (mejor == seleccionado) null else mejor)
                }
            },
    ) {
        anchoCanvas = size.width
        val relleno = 6.dp.toPx()
        val alto = size.height - relleno * 2
        val paso = if (puntos.size > 1) size.width / (puntos.size - 1) else size.width

        // Rejilla: 3 hairlines sólidas, recesivas.
        val grosorRejilla = 1.dp.toPx()
        repeat(3) { i ->
            val y = relleno + alto * i / 2f
            drawLine(
                color = colorRejilla,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = grosorRejilla,
            )
        }

        fun yDe(valor: Double): Float =
            relleno + alto - (valor / maximo).toFloat().coerceIn(0f, 1f) * alto

        fun trazar(valorDe: (PuntoSerie) -> Double, color: Color) {
            val camino = Path()
            puntos.forEachIndexed { i, p ->
                val x = i * paso
                val y = yDe(valorDe(p))
                if (i == 0) camino.moveTo(x, y) else camino.lineTo(x, y)
            }
            drawPath(
                path = camino,
                color = color,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            // Marcador en el último punto (etiqueta directa selectiva) con
            // anillo de 2 dp en el color de la superficie.
            val ultimo = puntos.lastIndex
            val cx = ultimo * paso
            val cy = yDe(valorDe(puntos[ultimo]))
            drawCircle(colorSuperficie, radius = 6.dp.toPx(), center = Offset(cx, cy))
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(cx, cy))
        }

        trazar({ it.valorA }, colorA)
        trazar({ it.valorB }, colorB)

        // Día seleccionado: guía vertical sólida + marcadores con anillo.
        seleccionado?.let { i ->
            val x = i * paso
            drawLine(
                color = colorRejilla,
                start = Offset(x, relleno),
                end = Offset(x, relleno + alto),
                strokeWidth = 1.dp.toPx(),
            )
            listOf(puntos[i].valorA to colorA, puntos[i].valorB to colorB).forEach { (v, c) ->
                val y = yDe(v)
                drawCircle(colorSuperficie, radius = 6.dp.toPx(), center = Offset(x, y))
                drawCircle(c, radius = 4.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}

// ─── Barras horizontales de una sola serie ──────────────────────────────────

data class BarraDato(val etiqueta: String, val valor: Double, val detalle: String? = null)

/**
 * Magnitud por categoría. Una sola serie, así que **un solo color** (nunca una
 * rampa por tamaño ni una paleta categórica): la longitud ya codifica el valor.
 * El valor va siempre como etiqueta visible, por eso no hace falta vista de
 * tabla aparte.
 */
@Composable
fun BarrasHorizontales(
    datos: List<BarraDato>,
    color: Color,
    modifier: Modifier = Modifier,
    maximoVisible: Int = 8,
) {
    if (datos.isEmpty()) {
        CajaVacia("Sin datos para mostrar")
        return
    }

    val maximo = remember(datos) { max(datos.maxOf { it.valor }, 1.0) }
    val visibles = datos.take(maximoVisible)

    Column(modifier.fillMaxWidth()) {
        visibles.forEach { d ->
            Row(
                Modifier
                    .fillMaxWidth()
                    // 2 dp de aire entre barras vecinas: la superficie separa,
                    // no un borde dibujado.
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            d.etiqueta,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                        Text(
                            Formato.bs(d.valor),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                    ) {
                        val radio = 4.dp.toPx()
                        val ancho = (d.valor / maximo).toFloat().coerceIn(0f, 1f) * size.width
                        // Carril recesivo
                        drawRoundRect(
                            color = color.copy(alpha = 0.12f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radio),
                        )
                        if (ancho > 0f) {
                            drawRoundRect(
                                color = color,
                                size = androidx.compose.ui.geometry.Size(
                                    width = max(ancho, radio * 2),
                                    height = size.height,
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radio),
                            )
                        }
                    }
                    if (d.detalle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            d.detalle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (datos.size > maximoVisible) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Mostrando los ${visibles.size} mayores de ${datos.size}.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
