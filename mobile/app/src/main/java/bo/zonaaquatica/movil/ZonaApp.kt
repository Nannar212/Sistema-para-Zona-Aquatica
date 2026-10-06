package bo.zonaaquatica.movil

import android.app.Application
import bo.zonaaquatica.movil.core.Prefs
import bo.zonaaquatica.movil.data.api.FabricaApi
import bo.zonaaquatica.movil.data.api.PiscinaApi
import bo.zonaaquatica.movil.data.repo.AsistenciaRepo
import bo.zonaaquatica.movil.data.repo.AuditoriaRepo
import bo.zonaaquatica.movil.data.repo.AuthRepo
import bo.zonaaquatica.movil.data.repo.CatalogoRepo
import bo.zonaaquatica.movil.data.repo.ClientesRepo
import bo.zonaaquatica.movil.data.repo.ContenidoRepo
import bo.zonaaquatica.movil.data.repo.DashboardRepo
import bo.zonaaquatica.movil.data.repo.FinanzasRepo
import bo.zonaaquatica.movil.data.repo.GastosRepo
import bo.zonaaquatica.movil.data.repo.PersonalRepo
import bo.zonaaquatica.movil.data.repo.RecibosRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contenedor de dependencias hecho a mano (sin Hilt): la app es pequeña y asi
 * no se arrastran procesadores de anotaciones al build.
 */
class Contenedor(app: Application) {

    val prefs = Prefs(app)

    /** Se pone en true cuando el refresh token tambien caduca. */
    private val _sesionExpirada = MutableStateFlow(false)
    val sesionExpirada: StateFlow<Boolean> = _sesionExpirada

    fun consumirSesionExpirada() {
        _sesionExpirada.value = false
    }

    val api: PiscinaApi = FabricaApi.crear(prefs) { _sesionExpirada.value = true }

    val auth = AuthRepo(api, prefs)
    val clientes = ClientesRepo(api)
    val catalogo = CatalogoRepo(api)
    val asistencia = AsistenciaRepo(api)
    val personal = PersonalRepo(api)
    val dashboard = DashboardRepo(api)
    val finanzas = FinanzasRepo(api)
    val gastos = GastosRepo(api)
    val auditoria = AuditoriaRepo(api)
    val contenido = ContenidoRepo(api)
    val recibos = RecibosRepo(api)
}

class ZonaApp : Application() {

    lateinit var contenedor: Contenedor
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = Contenedor(this)
    }
}
