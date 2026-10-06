package bo.zonaaquatica.movil.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formateo de fechas y montos, igual que en el frontend web. */
object Formato {

    private val bolivia: Locale = Locale.forLanguageTag("es-BO")
    private val dmy = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dmyHm = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val diasCortos = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do")
    val diasLargos = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    fun hoyIso(): String = LocalDate.now().toString()

    fun primerDiaDelMesIso(): String = LocalDate.now().withDayOfMonth(1).toString()

    /** "2026-10-05" -> "05/10/2026". Devuelve "—" si no se puede parsear. */
    fun fecha(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return runCatching { LocalDate.parse(iso.take(10)).format(dmy) }.getOrDefault("—")
    }

    /** "2026-10-05T14:30:00" -> "05/10/2026 14:30". */
    fun fechaHora(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return runCatching { LocalDateTime.parse(iso.take(19)).format(dmyHm) }
            .getOrElse { fecha(iso) }
    }

    /** "2026-10-05" -> "Lunes 05/10". */
    fun fechaConDia(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return runCatching {
            val d = LocalDate.parse(iso.take(10))
            "${diasLargos[d.dayOfWeek.value - 1]} ${d.format(DateTimeFormatter.ofPattern("dd/MM"))}"
        }.getOrDefault(fecha(iso))
    }

    /** 120.5 -> "Bs 120.50" */
    fun bs(monto: Double?): String = "Bs " + String.format(bolivia, "%.2f", monto ?: 0.0)

    /** [0,1,2,3,4] -> "Lu, Ma, Mi, Ju, Vi" */
    fun dias(indices: List<Int>?): String {
        if (indices.isNullOrEmpty()) return "—"
        return indices.sorted().mapNotNull { diasCortos.getOrNull(it) }.joinToString(", ")
    }

    /** Suma o resta dias a una fecha ISO, devolviendo ISO. */
    fun desplazar(iso: String, dias: Long): String =
        runCatching { LocalDate.parse(iso.take(10)).plusDays(dias).toString() }.getOrDefault(iso)
}
