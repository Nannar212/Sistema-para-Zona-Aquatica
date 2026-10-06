package bo.zonaaquatica.movil.ui.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.zonaaquatica.movil.core.Formato
import bo.zonaaquatica.movil.core.Resultado
import bo.zonaaquatica.movil.data.model.ClienteDto
import bo.zonaaquatica.movil.data.model.ClienteRequest
import bo.zonaaquatica.movil.data.model.ContadoresDto
import bo.zonaaquatica.movil.data.model.HorarioDto
import bo.zonaaquatica.movil.data.model.MembresiaDto
import bo.zonaaquatica.movil.data.model.PagoRequest
import bo.zonaaquatica.movil.data.repo.CatalogoRepo
import bo.zonaaquatica.movil.data.repo.ClientesRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Pestañas del backend: el "bucket" se calcula allá (los vencidos caen en inactivo). */
object Bucket {
    const val ACTIVO = "activo"
    const val INACTIVO = "inactivo"
    const val ELIMINADO = "eliminado"
    val TODOS = listOf(ACTIVO, INACTIVO, ELIMINADO)

    fun titulo(b: String) = when (b) {
        ACTIVO -> "Activos"
        INACTIVO -> "Inactivos"
        else -> "Eliminados"
    }
}

/** Espejo de VACIO en ClientesPage.jsx. */
data class FormCliente(
    val nombres: String = "",
    val apellidos: String = "",
    val sexo: String = "M",
    val edad: String = "",
    val celular: String = "",
    val horarioId: Int? = null,
    val membresiaId: Int? = null,
    val fechaInicio: String = Formato.hoyIso(),
) {
    fun aRequest() = ClienteRequest(
        nombres = nombres.trim(),
        apellidos = apellidos.trim(),
        sexo = sexo,
        edad = edad.trim().toIntOrNull(),
        celular = celular.trim(),
        horarioId = horarioId,
        membresiaId = membresiaId,
        fechaInicio = fechaInicio,
    )

    companion object {
        val SEXOS = listOf("M", "F", "Otro")

        fun etiquetaSexo(s: String) = when (s) {
            "M" -> "Masculino"
            "F" -> "Femenino"
            else -> "Otro"
        }

        fun de(c: ClienteDto) = FormCliente(
            nombres = c.nombres,
            apellidos = c.apellidos,
            sexo = c.sexo,
            edad = c.edad?.toString() ?: "",
            celular = c.celular,
            horarioId = c.horarioId,
            membresiaId = null,
            fechaInicio = Formato.hoyIso(),
        )
    }
}

/** Estado del formulario de cobro. */
data class FormPago(
    val cliente: ClienteDto,
    val monto: String = "",
    val metodo: String = "efectivo",
    val montoEfectivo: String = "",
    val montoQr: String = "",
    val fecha: String = Formato.hoyIso(),
) {
    val saldoPendiente: Double get() = cliente.membresiaActiva?.saldoPendiente ?: 0.0

    companion object {
        val METODOS = listOf("efectivo", "qr", "mixto")

        /** Igual que el web: sugiere el saldo pendiente, o el total si ya está pagada. */
        fun de(cliente: ClienteDto): FormPago {
            val m = cliente.membresiaActiva
            val sugerido = when {
                m == null -> 0.0
                m.saldoPendiente > 0 -> m.saldoPendiente
                else -> m.total
            }
            return FormPago(
                cliente = cliente,
                monto = if (sugerido > 0) formatearMonto(sugerido) else "",
            )
        }

        fun formatearMonto(v: Double): String =
            if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.2f", v)
    }
}

/** Estado del formulario de renovación de membresía. */
data class FormRenovar(
    val cliente: ClienteDto,
    val membresiaId: Int? = null,
    val fechaInicio: String = Formato.hoyIso(),
    val continuar: Boolean = false,
)

data class UiClientes(
    val tab: String = Bucket.ACTIVO,
    val busqueda: String = "",
    val lista: List<ClienteDto> = emptyList(),
    val contadores: ContadoresDto = ContadoresDto(),
    val cargando: Boolean = true,
    val error: String? = null,

    val membresias: List<MembresiaDto> = emptyList(),
    val horarios: List<HorarioDto> = emptyList(),

    val form: FormCliente? = null,
    val editandoId: Int? = null,
    val errorForm: String? = null,
    val guardando: Boolean = false,

    val detalle: ClienteDto? = null,
    val cargandoDetalle: Boolean = false,

    val pago: FormPago? = null,
    val errorPago: String? = null,
    val renovar: FormRenovar? = null,
    val errorRenovar: String? = null,

    val inactivar: Pair<ClienteDto, String>? = null,
    val eliminar: Triple<ClienteDto, String, Boolean>? = null,
    val porRestaurar: ClienteDto? = null,
)

/**
 * Equivalente a ClientesPage.jsx: pestañas por estado, búsqueda, alta/edición,
 * cobros (efectivo / QR / mixto con saldo) y renovación de membresía.
 */
class ClientesViewModel(
    private val repo: ClientesRepo,
    private val catalogo: CatalogoRepo,
) : ViewModel() {

    private val _ui = MutableStateFlow(UiClientes())
    val ui: StateFlow<UiClientes> = _ui.asStateFlow()

    private var trabajoBusqueda: Job? = null

    fun iniciar() {
        viewModelScope.launch {
            (catalogo.membresias(soloActivas = true) as? Resultado.Ok)?.let {
                _ui.value = _ui.value.copy(membresias = it.dato)
            }
            (catalogo.horarios(soloActivos = true) as? Resultado.Ok)?.let {
                _ui.value = _ui.value.copy(horarios = it.dato)
            }
        }
        cargar()
    }

    fun cargar() {
        val e = _ui.value
        _ui.value = e.copy(cargando = true, error = null)
        viewModelScope.launch {
            when (val r = repo.listar(e.tab, e.busqueda)) {
                is Resultado.Ok -> _ui.value = _ui.value.copy(cargando = false, lista = r.dato)
                is Resultado.Error -> _ui.value = _ui.value.copy(cargando = false, error = r.mensaje)
            }
            (repo.contadores() as? Resultado.Ok)?.let {
                _ui.value = _ui.value.copy(contadores = it.dato)
            }
        }
    }

    fun cambiarTab(tab: String) {
        _ui.value = _ui.value.copy(tab = tab)
        cargar()
    }

    /** Búsqueda con rebote de 350 ms para no golpear la API en cada tecla. */
    fun cambiarBusqueda(texto: String) {
        _ui.value = _ui.value.copy(busqueda = texto)
        trabajoBusqueda?.cancel()
        trabajoBusqueda = viewModelScope.launch {
            delay(350)
            cargar()
        }
    }

    // ─── Alta / edición ─────────────────────────────────────────────────────

    fun abrirNuevo() {
        _ui.value = _ui.value.copy(form = FormCliente(), editandoId = null, errorForm = null)
    }

    fun abrirEditar(c: ClienteDto) {
        _ui.value = _ui.value.copy(
            form = FormCliente.de(c),
            editandoId = c.id,
            errorForm = null,
        )
    }

    fun cerrarForm() {
        _ui.value = _ui.value.copy(form = null, editandoId = null, errorForm = null)
    }

    fun editarForm(bloque: (FormCliente) -> FormCliente) {
        _ui.value.form?.let { _ui.value = _ui.value.copy(form = bloque(it), errorForm = null) }
    }

    fun guardar(alTerminar: (String) -> Unit) {
        val estado = _ui.value
        val form = estado.form ?: return
        if (estado.guardando) return

        if (form.nombres.isBlank() || form.apellidos.isBlank()) {
            _ui.value = estado.copy(errorForm = "Nombres y apellidos son obligatorios")
            return
        }

        _ui.value = estado.copy(guardando = true, errorForm = null)
        viewModelScope.launch {
            val id = estado.editandoId
            val r = if (id == null) repo.crear(form.aRequest())
            else repo.actualizar(id, form.aRequest())

            when (r) {
                is Resultado.Ok -> {
                    _ui.value = _ui.value.copy(guardando = false, form = null, editandoId = null)
                    alTerminar(if (id == null) "Cliente registrado" else "Cliente actualizado")
                    cargar()
                    // Al crear, se abre el detalle para cobrar de inmediato (como el web).
                    r.dato?.id?.let { if (id == null) verDetalle(it, alTerminar) }
                }
                is Resultado.Error ->
                    _ui.value = _ui.value.copy(guardando = false, errorForm = r.mensaje)
            }
        }
    }

    // ─── Detalle ────────────────────────────────────────────────────────────

    /**
     * `alError` es necesario: si falla, la hoja se cierra, y sin avisar el
     * usuario vería el toque simplemente ignorado.
     */
    fun verDetalle(id: Int, alError: (String) -> Unit = {}) {
        _ui.value = _ui.value.copy(cargandoDetalle = true, detalle = null)
        viewModelScope.launch {
            when (val r = repo.detalle(id)) {
                is Resultado.Ok ->
                    _ui.value = _ui.value.copy(cargandoDetalle = false, detalle = r.dato)
                is Resultado.Error -> {
                    _ui.value = _ui.value.copy(cargandoDetalle = false, detalle = null)
                    alError(r.mensaje)
                }
            }
        }
    }

    fun cerrarDetalle() {
        _ui.value = _ui.value.copy(detalle = null, cargandoDetalle = false)
    }

    private fun recargarDetalle(alError: (String) -> Unit = {}) {
        _ui.value.detalle?.id?.let { verDetalle(it, alError) }
    }

    // ─── Cobro ──────────────────────────────────────────────────────────────

    fun abrirPago(c: ClienteDto) {
        _ui.value = _ui.value.copy(pago = FormPago.de(c), errorPago = null)
    }

    fun cerrarPago() {
        _ui.value = _ui.value.copy(pago = null, errorPago = null)
    }

    fun editarPago(bloque: (FormPago) -> FormPago) {
        _ui.value.pago?.let { _ui.value = _ui.value.copy(pago = bloque(it), errorPago = null) }
    }

    fun registrarPago(alTerminar: (String) -> Unit) {
        val estado = _ui.value
        val p = estado.pago ?: return

        val efectivo = p.montoEfectivo.replace(',', '.').toDoubleOrNull() ?: 0.0
        val qr = p.montoQr.replace(',', '.').toDoubleOrNull() ?: 0.0
        val monto = p.monto.replace(',', '.').toDoubleOrNull() ?: 0.0

        val problema = when {
            p.metodo == "mixto" && (efectivo <= 0.0 || qr <= 0.0) ->
                "En pago mixto, efectivo y QR deben ser mayores a 0"
            p.metodo != "mixto" && monto <= 0.0 -> "El monto debe ser mayor a 0"
            else -> null
        }
        if (problema != null) {
            _ui.value = estado.copy(errorPago = problema)
            return
        }

        val cuerpo = PagoRequest(
            clienteId = p.cliente.id,
            clienteMembresiaId = p.cliente.membresiaActiva?.id,
            monto = if (p.metodo == "mixto") efectivo + qr else monto,
            metodo = p.metodo,
            montoEfectivo = if (p.metodo == "mixto") efectivo else 0.0,
            montoQr = if (p.metodo == "mixto") qr else 0.0,
            fecha = p.fecha,
        )

        _ui.value = estado.copy(guardando = true, errorPago = null)
        viewModelScope.launch {
            when (val r = repo.registrarPago(cuerpo)) {
                is Resultado.Ok -> {
                    _ui.value = _ui.value.copy(guardando = false, pago = null)
                    val saldo = r.dato.saldoPendiente
                    alTerminar(
                        if (saldo != null && saldo > 0.009)
                            "Abono registrado. Saldo pendiente: ${Formato.bs(saldo)}"
                        else "Pago registrado",
                    )
                    cargar()
                    recargarDetalle(alTerminar)
                }
                is Resultado.Error ->
                    _ui.value = _ui.value.copy(guardando = false, errorPago = r.mensaje)
            }
        }
    }

    // ─── Renovación de membresía ────────────────────────────────────────────

    fun abrirRenovar(c: ClienteDto) {
        _ui.value = _ui.value.copy(
            renovar = FormRenovar(cliente = c, membresiaId = c.membresiaActiva?.membresiaId),
            errorRenovar = null,
        )
    }

    fun cerrarRenovar() {
        _ui.value = _ui.value.copy(renovar = null, errorRenovar = null)
    }

    fun editarRenovar(bloque: (FormRenovar) -> FormRenovar) {
        _ui.value.renovar?.let { _ui.value = _ui.value.copy(renovar = bloque(it), errorRenovar = null) }
    }

    fun confirmarRenovar(alTerminar: (String) -> Unit) {
        val estado = _ui.value
        val r0 = estado.renovar ?: return
        val membresiaId = r0.membresiaId
        if (membresiaId == null) {
            _ui.value = estado.copy(errorRenovar = "Elige un plan")
            return
        }

        _ui.value = estado.copy(guardando = true, errorRenovar = null)
        viewModelScope.launch {
            val r = repo.renovarMembresia(
                id = r0.cliente.id,
                membresiaId = membresiaId,
                // Si "continuar" está activo el backend calcula el inicio solo.
                fechaInicio = if (r0.continuar) null else r0.fechaInicio,
                continuar = r0.continuar,
            )
            when (r) {
                is Resultado.Ok -> {
                    _ui.value = _ui.value.copy(guardando = false, renovar = null)
                    alTerminar("Membresía asignada")
                    cargar()
                    recargarDetalle(alTerminar)
                }
                is Resultado.Error ->
                    _ui.value = _ui.value.copy(guardando = false, errorRenovar = r.mensaje)
            }
        }
    }

    // ─── Estado del cliente (solo admin en el backend) ──────────────────────

    fun pedirInactivar(c: ClienteDto) { _ui.value = _ui.value.copy(inactivar = c to "") }
    fun cambiarMotivoInactivar(m: String) {
        _ui.value.inactivar?.let { _ui.value = _ui.value.copy(inactivar = it.first to m) }
    }
    fun cancelarInactivar() { _ui.value = _ui.value.copy(inactivar = null) }

    fun confirmarInactivar(alTerminar: (String) -> Unit) {
        val (c, motivo) = _ui.value.inactivar ?: return
        _ui.value = _ui.value.copy(inactivar = null)
        viewModelScope.launch {
            when (val r = repo.inactivar(c.id, motivo)) {
                is Resultado.Ok -> {
                    alTerminar("Cliente marcado como inactivo")
                    cerrarDetalle(); cargar()
                }
                is Resultado.Error -> alTerminar(r.mensaje)
            }
        }
    }

    fun pedirEliminar(c: ClienteDto) {
        _ui.value = _ui.value.copy(eliminar = Triple(c, "", false))
    }
    fun cambiarMotivoEliminar(m: String) {
        _ui.value.eliminar?.let {
            _ui.value = _ui.value.copy(eliminar = Triple(it.first, m, it.third))
        }
    }
    fun cambiarEliminarPagos(v: Boolean) {
        _ui.value.eliminar?.let {
            _ui.value = _ui.value.copy(eliminar = Triple(it.first, it.second, v))
        }
    }
    fun cancelarEliminar() { _ui.value = _ui.value.copy(eliminar = null) }

    fun confirmarEliminar(alTerminar: (String) -> Unit) {
        val (c, motivo, borrarPagos) = _ui.value.eliminar ?: return
        _ui.value = _ui.value.copy(eliminar = null)
        viewModelScope.launch {
            when (val r = repo.eliminar(c.id, motivo, borrarPagos)) {
                is Resultado.Ok -> {
                    alTerminar("Cliente eliminado")
                    cerrarDetalle(); cargar()
                }
                is Resultado.Error -> alTerminar(r.mensaje)
            }
        }
    }

    fun pedirRestaurar(c: ClienteDto) { _ui.value = _ui.value.copy(porRestaurar = c) }
    fun cancelarRestaurar() { _ui.value = _ui.value.copy(porRestaurar = null) }

    fun confirmarRestaurar(alTerminar: (String) -> Unit) {
        val c = _ui.value.porRestaurar ?: return
        _ui.value = _ui.value.copy(porRestaurar = null)
        viewModelScope.launch {
            when (val r = repo.restaurar(c.id)) {
                is Resultado.Ok -> {
                    alTerminar("Cliente recuperado")
                    cerrarDetalle(); cargar()
                }
                is Resultado.Error -> alTerminar(r.mensaje)
            }
        }
    }
}
