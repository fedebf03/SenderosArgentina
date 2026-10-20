package com.unaj.senderos.core.data.seguimiento

import android.util.Log
import com.unaj.senderos.core.domain.recorrido.CalculadoraSeguimiento
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.core.domain.sensores.DetectorMovimiento
import com.unaj.senderos.core.domain.sensores.DetectorPasos
import com.unaj.senderos.core.domain.sensores.VentanaRitmoPasos
import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.port.FuenteAcelerometro
import com.unaj.senderos.core.domain.port.FuenteBrujula
import com.unaj.senderos.core.domain.port.ProveedorDeclinacion
import com.unaj.senderos.core.domain.model.AvisoRecorrido
import com.unaj.senderos.core.domain.model.CalidadSenal
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.core.domain.model.PuntoCapturado
import com.unaj.senderos.core.domain.model.ResultadoCaptura
import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.port.AlmacenBorradorRecorrido
import com.unaj.senderos.core.domain.port.AlmacenFotos
import com.unaj.senderos.core.domain.port.ControlSeguimiento
import com.unaj.senderos.core.domain.port.ProveedorUbicacion
import com.unaj.senderos.core.domain.repository.PuntosCapturadosRepositorio
import com.unaj.senderos.core.domain.repository.RecorridoRepositorio
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "SEGUIMIENTO"
private const val CAPACIDAD_AVISOS = 16
private const val PRECISION_MAXIMA_CON_GPS_METROS = 30f
private const val PRECISION_MAXIMA_SIN_GPS_METROS = 100f
private const val INTERVALO_REEVALUACION_MILISEGUNDOS = 5_000L
private const val INTERVALO_AUTOGUARDADO_MILISEGUNDOS = 30_000L
private const val MILISEGUNDOS_POR_HORA = 3_600_000.0
private const val KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO = 3.6
private const val METROS_POR_KILOMETRO = 1000.0
private const val METROS_POR_PASO_SIMULADO = 0.75
private const val SEGUNDOS_POR_MINUTO = 60.0
private const val MILISEGUNDOS_POR_MINUTO = 60_000.0

// Solo usa android.util.Log y ningún servicio de Android, así se puede probar en la JVM. Los sensores se recolectan
// mientras hay seguimiento y si el equipo los tiene. Sin ellos, los pasos, el ritmo y el rumbo de brújula quedan en null
class ControlSeguimientoEnMemoria(
    private val proveedorUbicacion: ProveedorUbicacion,
    private val observarDetalleSendero: ObservarDetalleSenderoUseCase,
    private val recorridoRepositorio: RecorridoRepositorio,
    private val puntosCapturadosRepositorio: PuntosCapturadosRepositorio,
    private val almacenFotos: AlmacenFotos,
    private val alcance: CoroutineScope,
    private val capacidades: CapacidadesDispositivo = CapacidadesDispositivo.SIN_FUNCIONES,
    private val fuenteAcelerometro: FuenteAcelerometro? = null,
    private val fuenteBrujula: FuenteBrujula? = null,
    private val proveedorDeclinacion: ProveedorDeclinacion? = null,
    private val almacenBorrador: AlmacenBorradorRecorrido? = null,
    private val generarIdentificador: () -> String = { UUID.randomUUID().toString() },
    private val reloj: () -> Long = System::currentTimeMillis
) : ControlSeguimiento {

    private val estadoInterno = MutableStateFlow<EstadoSeguimiento?>(null)
    private val avisosInternos = MutableSharedFlow<AvisoRecorrido>(
        extraBufferCapacity = CAPACIDAD_AVISOS,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val motivoFinInterno = MutableStateFlow<MotivoFinSeguimiento?>(null)
    private val cerrojoInicio = Mutex()
    private var trabajoUbicaciones: Job? = null
    private var trabajosSensores: List<Job> = emptyList()
    private var detectorPasos = DetectorPasos()
    private val ventanaRitmo = VentanaRitmoPasos()
    private var instanteUltimoPaso: Long? = null
    private val midePasosReales = capacidades.tieneAcelerometro && fuenteAcelerometro != null
    private var pasosRealesActivos = false
    private var rumboMagneticoGrados: Float? = null
    private var declinacionCacheada: Pair<Coordenada, Float>? = null
    private var generacion = 0
    private var proveedorSimulado: ProveedorUbicacionSimulada? = null
    private var nombreSenderoActivo: String = ""
    private var idUsuarioActivo: String = ""
    private val precisionMaximaMetros = if (capacidades.tieneGps) PRECISION_MAXIMA_CON_GPS_METROS else PRECISION_MAXIMA_SIN_GPS_METROS

    override val estado: StateFlow<EstadoSeguimiento?> = estadoInterno
    override val motivoFin: StateFlow<MotivoFinSeguimiento?> = motivoFinInterno
    override val avisos: SharedFlow<AvisoRecorrido> = avisosInternos

    // RF5 - Seguimiento: cerramos el recorrido anterior, cargamos el trazado y empezamos a recolectar el GPS y los sensores
    override suspend fun iniciar(idSendero: String, esSimulado: Boolean) {
        val miGeneracion = ++generacion
        cerrojoInicio.withLock {
            cerrarSeguimientoAnterior()
            val sendero = try {
                obtenerSendero(idSendero).also { encontrado ->
                    check(encontrado.recorrido.size >= 2) { "El sendero no tiene recorrido para seguir" }
                }
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                estadoInterno.value = null
                throw error
            }
            if (miGeneracion != generacion) {
                Log.d(TAG, "Inicio descartado: se pidió detener mientras se cargaba el sendero")
                return
            }
            val estaMidiendoPasos = prepararSensores(esSimulado)
            val estadoInicial = CalculadoraSeguimiento.iniciar(sendero, reloj()).copy(
                esSimulado = esSimulado,
                calidadSenal = if (esSimulado) CalidadSenal.BUENA else CalidadSenal.BUSCANDO,
                pasos = if (estaMidiendoPasos) 0 else null,
                ritmoPasosPorMinuto = if (estaMidiendoPasos) 0.0 else null
            )
            val proveedorActivo: ProveedorUbicacion = if (esSimulado) {
                ProveedorUbicacionSimulada(estadoInicial.trazado).also { simulado -> proveedorSimulado = simulado }
            } else {
                proveedorSimulado = null
                proveedorUbicacion
            }
            activarSeguimiento(estadoInicial, proveedorActivo, sendero.nombre, sendero.idUsuario)
        }
    }

    // En una caminata libre no hay sendero que seguir. Se registra solo lo caminado y siempre con el GPS real
    override suspend fun iniciarCaminataLibre(idCaminata: String, idUsuario: String, nombre: String) {
        val miGeneracion = ++generacion
        cerrojoInicio.withLock {
            cerrarSeguimientoAnterior()
            if (miGeneracion != generacion) {
                Log.d(TAG, "Inicio descartado: se pidió detener mientras se cerraba el recorrido anterior")
                return
            }
            val estaMidiendoPasos = prepararSensores(esSimulado = false)
            val instanteInicio = reloj()
            val estadoInicial = CalculadoraSeguimiento
                .iniciarCaminataLibre(idCaminata, nombre, instanteInicio)
                .copy(
                    calidadSenal = CalidadSenal.BUSCANDO,
                    pasos = if (estaMidiendoPasos) 0 else null,
                    ritmoPasosPorMinuto = if (estaMidiendoPasos) 0.0 else null
                )
            proveedorSimulado = null
            activarSeguimiento(estadoInicial, proveedorUbicacion, nombre, idUsuario)
        }
    }

    private suspend fun cerrarSeguimientoAnterior() {
        trabajoUbicaciones?.cancelAndJoin()
        trabajoUbicaciones = null
        trabajosSensores.forEach { trabajo -> trabajo.cancelAndJoin() }
        trabajosSensores = emptyList()
        guardarRecorridoRealizado(estadoInterno.value, nombreSenderoActivo, idUsuarioActivo)
    }

    // Devuelve si se van a medir pasos (reales, o estimados en la simulación)
    private fun prepararSensores(esSimulado: Boolean): Boolean {
        pasosRealesActivos = midePasosReales && !esSimulado
        rumboMagneticoGrados = null
        declinacionCacheada = null
        detectorPasos = DetectorPasos()
        ventanaRitmo.reiniciar()
        instanteUltimoPaso = null
        return esSimulado || pasosRealesActivos
    }

    private fun activarSeguimiento(estadoInicial: EstadoSeguimiento, proveedorActivo: ProveedorUbicacion, nombre: String, idUsuario: String) {
        motivoFinInterno.value = null
        nombreSenderoActivo = nombre
        idUsuarioActivo = idUsuario
        estadoInterno.value = estadoInicial
        Log.d(TAG, "Seguimiento iniciado: ${estadoInicial.idSendero} (simulado: ${estadoInicial.esSimulado}, libre: ${estadoInicial.esCaminataLibre})")
        trabajosSensores = (if (estadoInicial.esSimulado) emptyList() else lanzarRecoleccionDeSensores()) +
            alcance.launch { reevaluarCalidadPeriodicamente() } +
            listOfNotNull(if (estadoInicial.esSimulado) null else alcance.launch { autoguardarPeriodicamente() })
        trabajoUbicaciones = alcance.launch { recolectarUbicaciones(proveedorActivo) }
    }

    override fun cambiarMultiplicadorSimulacion(valor: Int) {
        val proveedorActivo = proveedorSimulado ?: return
        proveedorActivo.cambiarMultiplicador(valor)
        estadoInterno.update { estadoActual -> estadoActual?.copy(multiplicadorSimulacion = valor) }
    }

    override fun simularDesvio() {
        proveedorSimulado?.simularDesvio()
    }

    override fun detener() {
        finalizarSeguimiento(guardar = true)
    }

    override fun cancelar() {
        finalizarSeguimiento(guardar = false)
    }

    // Con la señal débil se informa la antigüedad de la posición usada, pero no se bloquea la captura.
    // Los fallos devuelven mensajes propios en español y el detalle técnico queda solo en el registro
    override suspend fun capturarPunto(tipo: TipoPunto, nota: String, uriFoto: String?, posicion: Coordenada?): Result<ResultadoCaptura> {
        val estadoActual = estadoInterno.value
            ?: return Result.failure(IllegalStateException("No hay un recorrido en curso para agregar un punto"))
        val posicionDelPunto = posicion ?: estadoActual.posicionActual
            ?: return Result.failure(IllegalStateException("Todavía no hay señal de GPS. Esperá unos segundos"))
        val idUsuario = idUsuarioActivo
        val instante = reloj()
        // La posición reservada se tomó al tocar el botón. Por lo tanto no se informa como antigua
        // aunque la señal se haya debilitado después
        val antiguedad = if (posicion != null) null else calcularAntiguedadSiLaSenalEsDebil(estadoActual, instante)
        var rutaFoto = ""
        return try {
            if (!uriFoto.isNullOrBlank()) {
                rutaFoto = almacenFotos.guardarCopia(uriFoto).getOrElse { error ->
                    Log.e(TAG, "No se pudo copiar la foto del punto", error)
                    return Result.failure(IllegalStateException("No se pudo guardar la foto del punto"))
                }
            }
            val punto = PuntoCapturado(
                id = generarIdentificador(),
                idUsuario = idUsuario,
                idSendero = estadoActual.idSendero,
                latitud = posicionDelPunto.latitud,
                longitud = posicionDelPunto.longitud,
                tipo = tipo,
                nota = nota,
                foto = rutaFoto,
                instanteMilisegundos = instante
            )
            val resultadoGuardado = puntosCapturadosRepositorio.guardar(punto)
            if (resultadoGuardado.isFailure) {
                borrarCopiaDeFoto(rutaFoto)
                return Result.failure(resultadoGuardado.exceptionOrNull() ?: IllegalStateException("No se pudo guardar el punto"))
            }
            Log.d(TAG, "Punto ${punto.id} capturado en ${estadoActual.idSendero}")
            Result.success(ResultadoCaptura(punto, antiguedad))
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo inesperado al capturar el punto", error)
            borrarCopiaDeFoto(rutaFoto)
            Result.failure(IllegalStateException("No se pudo guardar el punto"))
        }
    }

    private fun calcularAntiguedadSiLaSenalEsDebil(estadoActual: EstadoSeguimiento, instante: Long): Long? {
        if (estadoActual.calidadSenal != CalidadSenal.DEBIL) return null
        return estadoActual.instanteUltimaLecturaValidaMilisegundos?.let { instanteLectura -> (instante - instanteLectura).coerceAtLeast(0L) }
    }

    // Así no queda huérfana la copia de una foto cuyo punto no se guardó
    private suspend fun borrarCopiaDeFoto(rutaFoto: String) {
        if (rutaFoto.isNotEmpty()) almacenFotos.borrar(rutaFoto)
    }

    // Al aumentar la generación se descarta un inicio que todavía esté cargando el sendero
    private fun finalizarSeguimiento(guardar: Boolean) {
        generacion++
        if (guardar) {
            guardarRecorridoRealizado(estadoInterno.value, nombreSenderoActivo, idUsuarioActivo)
        } else {
            descartarPuntosDeCaminataLibre(estadoInterno.value, idUsuarioActivo)
            estadoInterno.value?.let { cancelado -> alcance.launch { borrarBorradorDe("recorrido-${cancelado.instanteInicioMilisegundos}") } }
        }
        trabajoUbicaciones?.cancel()
        trabajoUbicaciones = null
        trabajosSensores.forEach { trabajo -> trabajo.cancel() }
        trabajosSensores = emptyList()
        proveedorSimulado = null
        motivoFinInterno.value = if (guardar) null else MotivoFinSeguimiento.CANCELADO_POR_EL_USUARIO
        estadoInterno.value = null
        Log.d(TAG, if (guardar) "Seguimiento detenido" else "Seguimiento cancelado sin guardar")
    }

    // Los puntos de una caminata libre cancelada no tienen sendero al que pertenecer, por eso se descartan con sus fotos.
    // En cambio los de un recorrido sobre un sendero se conservan para revisarlos e incorporarlos
    private fun descartarPuntosDeCaminataLibre(estadoCancelado: EstadoSeguimiento?, idUsuario: String) {
        if (estadoCancelado == null || !estadoCancelado.esCaminataLibre || idUsuario.isBlank()) return
        alcance.launch {
            try {
                val ids = puntosCapturadosRepositorio.observarPendientes(idUsuario, estadoCancelado.idSendero).first().map { punto -> punto.id }
                if (ids.isNotEmpty()) puntosCapturadosRepositorio.descartar(idUsuario, ids)
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Log.e(TAG, "No se pudieron descartar los puntos de la caminata cancelada", error)
            }
        }
    }

    // Cada 30 s se guarda una copia local del recorrido para recuperarlo si Android cierra la app.
    // No se guarda hasta tener una posición aceptada, y la simulación no se autoguarda
    private suspend fun autoguardarPeriodicamente() {
        val almacen = almacenBorrador ?: return
        while (true) {
            delay(INTERVALO_AUTOGUARDADO_MILISEGUNDOS)
            val estadoActual = estadoInterno.value ?: continue
            if (estadoActual.ultimaPosicionAceptada == null || idUsuarioActivo.isBlank()) continue
            val copia = construirRecorridoRealizado(estadoActual, idUsuarioActivo, nombreSenderoActivo, reloj())
            almacen.guardar(copia).onFailure { error -> Log.e(TAG, "No se pudo autoguardar el recorrido", error) }
        }
    }

    // Un recorrido nuevo puede haber empezado a autoguardarse mientras el anterior terminaba. Por eso solo se borra
    // el borrador si es el del recorrido indicado
    private suspend fun borrarBorradorDe(idRecorrido: String) {
        val almacen = almacenBorrador ?: return
        if (almacen.leer()?.id == idRecorrido) almacen.borrar()
    }

    // El tiempo avanza aunque no lleguen lecturas, por eso cada 5 s se reevalúa la señal (y si el usuario está detenido)
    private suspend fun reevaluarCalidadPeriodicamente() {
        while (true) {
            delay(INTERVALO_REEVALUACION_MILISEGUNDOS)
            val ahora = reloj()
            actualizarConSensores(ahora) { estadoActual ->
                CalculadoraSeguimiento.reevaluarSenal(estadoActual, ahora)
            }
        }
    }

    private fun lanzarRecoleccionDeSensores(): List<Job> {
        val trabajos = mutableListOf<Job>()
        val acelerometro = fuenteAcelerometro
        if (pasosRealesActivos && acelerometro != null) {
            trabajos.add(alcance.launch { recolectarPasos(acelerometro) })
        }
        val brujula = fuenteBrujula
        if (capacidades.tieneBrujula && brujula != null) {
            trabajos.add(alcance.launch { recolectarBrujula(brujula) })
        }
        return trabajos
    }

    // Si el sensor falla o termina se dejan de medir pasos sin cortar el seguimiento
    private suspend fun recolectarPasos(fuente: FuenteAcelerometro) {
        try {
            fuente.observarMuestras().collect { muestra ->
                if (detectorPasos.procesar(muestra)) registrarPaso(muestra.instanteMilisegundos)
            }
            marcarPasosNoDisponibles()
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "El acelerómetro falló; el seguimiento continúa sin contar pasos", error)
            marcarPasosNoDisponibles()
        }
    }

    // Los pasos y el ritmo pasan a null para no mostrar datos congelados
    private fun marcarPasosNoDisponibles() {
        pasosRealesActivos = false
        actualizarConSensores(reloj()) { estadoActual -> estadoActual }
    }

    // Se usa el instante en que el sensor midió el paso, no el de llegada, que puede venir por lotes
    private fun registrarPaso(instantePasoMilisegundos: Long) {
        ventanaRitmo.registrarPaso(instantePasoMilisegundos)
        instanteUltimoPaso = maxOf(instanteUltimoPaso ?: instantePasoMilisegundos, instantePasoMilisegundos)
        actualizarConSensores(reloj()) { estadoActual -> estadoActual.copy(pasos = (estadoActual.pasos ?: 0) + 1) }
    }

    // La brújula se usa solo con el usuario detenido, porque en movimiento el rumbo sale del GPS
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun recolectarBrujula(fuente: FuenteBrujula) {
        try {
            estadoInterno
                .map { estadoActual -> estadoActual?.estaDetenido == true }
                .distinctUntilChanged()
                .flatMapLatest { estaDetenido -> if (estaDetenido) fuente.observarRumbo() else emptyFlow() }
                .collect { rumboMagnetico ->
                    rumboMagneticoGrados = rumboMagnetico
                    actualizarConSensores(reloj()) { estadoActual -> estadoActual }
                }
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "La brújula falló; el seguimiento continúa sin rumbo magnético", error)
        }
    }

    // El efecto sobre el último paso simulado se resuelve fuera del estado publicado
    private fun actualizarConSensores(ahora: Long, transformar: (EstadoSeguimiento) -> EstadoSeguimiento) {
        val anterior = estadoInterno.value ?: return
        val transformado = transformar(anterior)
        if (transformado.esSimulado && calcularPasosSimulados(transformado) > (anterior.pasos ?: 0)) {
            instanteUltimoPaso = ahora
        }
        estadoInterno.value = completarSensores(transformado, ahora, instanteUltimoPaso)
    }

    private fun calcularPasosSimulados(estado: EstadoSeguimiento): Int {
        return (estado.distanciaRecorridaKilometros * METROS_POR_KILOMETRO / METROS_POR_PASO_SIMULADO).toInt()
    }

    private fun completarSensores(estado: EstadoSeguimiento, ahora: Long, ultimoPaso: Long?): EstadoSeguimiento {
        val pasos: Int?
        val ritmo: Double?
        when {
            estado.esSimulado -> {
                pasos = calcularPasosSimulados(estado)
                ritmo = (estado.velocidadMetrosPorSegundo ?: 0f) / METROS_POR_PASO_SIMULADO * SEGUNDOS_POR_MINUTO
            }
            pasosRealesActivos -> {
                pasos = estado.pasos
                ritmo = ventanaRitmo.ritmoPasosPorMinuto(ahora)
            }
            else -> {
                pasos = null
                ritmo = null
            }
        }
        val estaDetenido = DetectorMovimiento.estaDetenido(
            velocidadMetrosPorSegundo = estado.velocidadMetrosPorSegundo,
            instanteUltimoPaso = ultimoPaso,
            instanteReferencia = estado.instanteInicioMilisegundos,
            ahora = ahora,
            midePasos = estado.esSimulado || pasosRealesActivos,
            senalDebil = estado.calidadSenal == CalidadSenal.DEBIL
        )
        return estado.copy(
            pasos = pasos,
            ritmoPasosPorMinuto = ritmo,
            estaDetenido = estaDetenido,
            rumboBrujulaGrados = calcularRumboVerdadero(estado.posicionActual, ahora)
        )
    }

    // Sin posición o sin proveedor de declinación el rumbo queda magnético
    private fun calcularRumboVerdadero(posicion: Coordenada?, ahora: Long): Float? {
        val magnetico = rumboMagneticoGrados ?: return null
        val declinacion = if (posicion == null || proveedorDeclinacion == null) {
            0f
        } else {
            val cacheada = declinacionCacheada
            if (cacheada != null && cacheada.first == posicion) {
                cacheada.second
            } else {
                proveedorDeclinacion.obtenerDeclinacionGrados(posicion, ahora).also { calculada ->
                    declinacionCacheada = posicion to calculada
                }
            }
        }
        return CalculadoraRecorrido.normalizarRumbo(magnetico + declinacion)
    }

    private suspend fun obtenerSendero(idSendero: String): Sendero {
        val flujoSendero = observarDetalleSendero(idSendero).getOrElse { error ->
            throw IllegalStateException("No se puede iniciar el recorrido: ${error.message}", error)
        }
        return flujoSendero.first() ?: throw IllegalStateException("No se encontró el sendero $idSendero")
    }

    private suspend fun recolectarUbicaciones(proveedorActivo: ProveedorUbicacion) {
        try {
            proveedorActivo.observarUbicaciones().collect { lectura ->
                val estadoActual = estadoInterno.value
                if (estadoActual != null) {
                    val ahora = reloj()
                    val resultado = CalculadoraSeguimiento.actualizar(estadoActual, lectura, ahora, precisionMaximaMetros)
                    actualizarConSensores(ahora) { anterior ->
                        resultado.estado.copy(multiplicadorSimulacion = anterior.multiplicadorSimulacion)
                    }
                    resultado.avisos.forEach { aviso -> avisosInternos.tryEmit(aviso) }
                }
            }
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "El flujo de ubicaciones falló; se finaliza el seguimiento", error)
            motivoFinInterno.value = if (error is SecurityException) {
                MotivoFinSeguimiento.PERMISO_DE_UBICACION_REVOCADO
            } else {
                MotivoFinSeguimiento.ERROR_DE_UBICACION
            }
            guardarRecorridoRealizado(estadoInterno.value, nombreSenderoActivo, idUsuarioActivo)
            trabajosSensores.forEach { trabajo -> trabajo.cancel() }
            trabajosSensores = emptyList()
            estadoInterno.value = null
        }
    }

    // Se guarda con el usuario que inició el recorrido aunque la sesión ya se haya cerrado. Sin ninguna posición válida
    // no se guarda, porque un recorrido con solo lecturas imprecisas no tiene una distancia confiable
    private fun guardarRecorridoRealizado(estadoFinal: EstadoSeguimiento?, nombreSendero: String, idUsuario: String) {
        if (estadoFinal == null || estadoFinal.ultimaPosicionAceptada == null) return
        if (idUsuario.isBlank()) {
            Log.e(TAG, "No se guarda el recorrido de ${estadoFinal.idSendero}: se desconoce el usuario que lo inició")
            return
        }
        val recorrido = construirRecorridoRealizado(estadoFinal, idUsuario, nombreSendero, reloj())
        alcance.launch {
            try {
                recorridoRepositorio.guardar(recorrido)
                    .onSuccess { borrarBorradorDe(recorrido.id) }
                    .onFailure { error -> Log.e(TAG, "No se pudo guardar el recorrido ${recorrido.id}", error) }
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Log.e(TAG, "Fallo inesperado al guardar el recorrido ${recorrido.id}", error)
            }
        }
    }

    // El id sale del instante de inicio, así guardar dos veces el mismo recorrido no lo duplica
    private fun construirRecorridoRealizado(
        estadoFinal: EstadoSeguimiento,
        idUsuario: String,
        nombreSendero: String,
        instanteFin: Long
    ): RecorridoRealizado {
        val duracion = (instanteFin - estadoFinal.instanteInicioMilisegundos).coerceAtLeast(0L)
        val velocidadPromedio = if (duracion == 0L) {
            0.0
        } else {
            estadoFinal.distanciaRecorridaKilometros / (duracion / MILISEGUNDOS_POR_HORA)
        }
        return RecorridoRealizado(
            id = "recorrido-${estadoFinal.instanteInicioMilisegundos}",
            idUsuario = idUsuario,
            idSendero = estadoFinal.idSendero,
            nombreSendero = nombreSendero,
            inicioMilisegundos = estadoFinal.instanteInicioMilisegundos,
            finMilisegundos = instanteFin,
            duracionMilisegundos = duracion,
            distanciaKilometros = estadoFinal.distanciaRecorridaKilometros,
            velocidadPromedioKilometrosPorHora = velocidadPromedio,
            velocidadMaximaKilometrosPorHora = estadoFinal.velocidadMaximaMetrosPorSegundo * KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO,
            pasos = estadoFinal.pasos,
            // Es el ritmo medio de todo el recorrido y no el de la ventana de 10 s. En la simulación se guarda null
            // porque el ritmo quedaría multiplicado por la velocidad simulada
            ritmoPasosPorMinuto = estadoFinal.pasos?.takeUnless { estadoFinal.esSimulado }?.let { pasos ->
                if (duracion == 0L) 0.0 else pasos / (duracion / MILISEGUNDOS_POR_MINUTO)
            },
            trazadoCaminado = CalculadoraSeguimiento.cerrarTrazadoCaminado(estadoFinal),
            esSimulado = estadoFinal.esSimulado,
            estadoSincronizacion = EstadoSincronizacion.PENDIENTE
        )
    }
}
