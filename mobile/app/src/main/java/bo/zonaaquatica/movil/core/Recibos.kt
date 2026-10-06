package bo.zonaaquatica.movil.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Guarda el PDF que devuelve `GET /api/pagos/{id}/recibo` en la caché de la app
 * y lo abre con el visor de PDF del teléfono (o deja compartirlo si no hay uno).
 *
 * El archivo se expone por FileProvider: pasar un `file://` a otra app lanzaría
 * FileUriExposedException desde Android 7.
 */
object Recibos {

    private const val CARPETA = "recibos"

    /** Resultado del intento de abrir el recibo, para avisar al usuario. */
    enum class Resultado { Abierto, Compartido, SinVisor, Fallo }

    fun abrir(contexto: Context, pagoId: Int, bytes: ByteArray): Resultado {
        val uri = try {
            val carpeta = File(contexto.cacheDir, CARPETA).apply { mkdirs() }
            val archivo = File(carpeta, "recibo_$pagoId.pdf")
            archivo.writeBytes(bytes)
            FileProvider.getUriForFile(
                contexto,
                "${contexto.packageName}.fileprovider",
                archivo,
            )
        } catch (e: Exception) {
            return Resultado.Fallo
        }

        val ver = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            contexto.startActivity(ver)
            Resultado.Abierto
        } catch (e: ActivityNotFoundException) {
            // Sin visor de PDF instalado: se ofrece compartir o guardar.
            val compartir = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Recibo N° $pagoId — Zona Aquática")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                contexto.startActivity(
                    Intent.createChooser(compartir, "Compartir recibo").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                Resultado.Compartido
            } catch (e2: Exception) {
                Resultado.SinVisor
            }
        }
    }
}
