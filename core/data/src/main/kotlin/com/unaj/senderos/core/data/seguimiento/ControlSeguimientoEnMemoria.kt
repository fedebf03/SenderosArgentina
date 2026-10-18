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
private const val MILISEGUNDOS_POR_HORA = 3_600_000.0
private const val KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO = 3.6
private const val METROS_POR_KILOMETRO = 1000.0
private const val METROS_POR_PASO_SIMULADO = 0.75
private const val SEGUNDOS_POR_MINUTO = 60.0
private const val MILISEGUNDOS_POR_MINUTO = 60_000.0

// Controlar en memoria el único recorrido activo del proceso; solo usa android.util.Log, sin servicios de Android, para poder probarse en JVM.
// Los sensores (acelerómetro y brújula) se recolectan mientras el seguimiento está activo y solo si las capacidades del equipo lo permiten;
// sin fuente o sin capacidad, los pasos, el ritmo y el rumbo de brújula quedan en null
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

    // Iniciar el seguimiento del sendero indicado, finalizando el anterior y fallando si no tiene recorrido
    override suspend fun iniciar(idSendero: String, esSimulado: Boolean) {
        val miGeneracion = ++generacion
        cerrojoInicio.withLock {
            trabajoUbicaciones?.cancelAndJoin()
            trabajoUbicaciones = null
            trabajosSensores.forEach { trabajo -> trabajo.cancelAndJoin() }
            trabajosSensores = emptyList()
            guardarRecorridoRealizado(estadoInterno.value, nombreSenderoActivo, idUsuarioActivo)
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
            pasosRealesActivos = midePasosReales && !esSimulado
            rumboMagneticoGrados = null
            declinacionCacheada = null
            val estaMidiendoPasos = esSimulado || pasosRealesActivos
            val estadoInicial = CalculadoraSeguimiento.iniciar(sendero, reloj()).copy(
                esSimulado = esSimulado,
                calidadSenal = if (esSimulado) CalidadSenal.BUENA else CalidadSenal.BUSCANDO,
                pasos = if (estaMidiendoPasos) 0 else null,
                ritmoPasosPorMinuto = if (estaMidiendoPasos) 0.0 else null
            )
            detectorPasos = DetectorPasos()
            ventanaRitmo.reiniciar()
            instanteUltimoPaso = null
            val proveedorActivo: ProveedorUbicacion = if (esSimulado) {
                ProveedorUbicacionSimulada(estadoInicial.trazado).also { simulado -> proveedorSimulado = simulado }
            } else {
                proveedorSimulado = null
                proveedorUbicacion
            }
            motivoFinInterno.value = null
            nombreSenderoActivo = sendero.nombre
            idUsuarioActivo = sendero.idUsuario
            estadoInterno.value = estadoInicial
            Log.d(TAG, "Seguimiento iniciado: $idSendero (simulado: $esSimulado)")
            trabajosSensores = (if (esSimulado) emptyList() else lanzarRecoleccionDeSensores()) +
                alcance.launch { reevaluarCalidadPeriodicamente() }
            trabajoUbicaciones = alcance.launch { recolectarUbicaciones(proveedorActivo) }
        }
    }

    // Cambiar la velocidad de la simulación en curso; se ignora si el recorrido es real
    override fun cambiarMultiplicadorSimulacion(valor: Int) {
        val proveedorActivo = proveedorSimulado ?: return
        proveedorActivo.cambiarMultiplicador(valor)
        estadoInterno.update { estadoActual -> estadoActual?.copy(multiplicadorSimulacion = valor) }
    }

    // Pedir a la simulación en curso que se desvíe del trazado; se ignora si el recorrido es real
    override fun simularDesvio() {
        proveedorSimulado?.simularDesvio()
    }

    override fun detener() {
        finalizarSeguimiento(guardar = true)
    }

    override fun cancelar() {
        finalizarSeguimiento(guardar = false)
    }

    // Capturar un punto de interés en la posición reservada o, si no viene, en la última posición válida: copiar la foto si viene y guardar el punto al instante en la base local.
    // Con la señal débil informa la antigüedad de la posición usada para que la UI la muestre, sin bloquear la captura.
    // Los fallos devueltos llevan mensajes propios en español; el detalle técnico queda solo en el registro
    override suspend fun capturarPunto(tipo: TipoPunto, nota: String, uriFoto: String?, posicion: Coordenada?): Result<ResultadoCaptura> {
        val estadoActual = estadoInterno.value
            ?: return Result.failure(IllegalStateException("No hay un recorrido en curso para agregar un punto"))
        val posicionDelPunto = posicion ?: estadoActual.posicionActual
            ?: return Result.failure(IllegalStateException("Todavía no hay una posición válida del GPS; esperá a tener señal"))
        val idUsuario = idUsuarioActivo
        val instante = reloj()
        // Una posición reservada se tomó al tocar el botón, así que no se informa como antigua aunque la señal se haya debilitado después
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

    // Calcular la antigüedad de la última lectura válida solo si la señal está débil; null si la señal es buena o no hay lectura
    private fun calcularAntiguedadSiLaSenalEsDebil(estadoActual: EstadoSeguimiento, instante: Long): Long? {
        if (estadoActual.calidadSenal != CalidadSenal.DEBIL) return null
        return estadoActual.instanteUltimaLecturaValidaMilisegundos?.let { instanteLectura -> (instante - instanteLectura).coerceAtLeast(0L) }
    }

    // Evita dejar huérfana la copia de una foto cuyo punto no se guardó
    private suspend fun borrarCopiaDeFoto(rutaFoto: String) {
        if (rutaFoto.isNotEmpty()) almacenFotos.borrar(rutaFoto)
    }

    // Terminar el seguimiento: invalida inicios en curso, guarda o descarta lo recorrido y libera ubicación y sensores
    private fun finalizarSeguimiento(guardar: Boolean) {
        generacion++
        if (guardar) {
            guardarRecorridoRealizado(estadoInterno.value, nombreSenderoActivo, idUsuarioActivo)
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

    // Reevaluar cada 5 s con el reloj la calidad de la señal (y lo derivado del tiempo, como detenido), porque el tiempo avanza aunque no lleguen lecturas
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

    // Dejar de medir pasos: los pasos y el ritmo pasan a null para no mostrar datos congelados
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

    // Solo mientras el usuario está detenido: en movimiento manda el GPS
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

    // Guardar en segundo plano, con el usuario que lo inició aunque la sesión ya se haya cerrado, el recorrido que terminó, sin bloquear ni romper la detención;
    // no guarda si no hubo ninguna posición válida (un recorrido con solo lecturas imprecisas no tiene distancia confiable)
    private fun guardarRecorridoRealizado(estadoFinal: EstadoSeguimiento?, nombreSendero: String, idUsuario: String) {
        if (estadoFinal == null || estadoFinal.ultimaPosicionAceptada == null) return
        if (idUsuario.isBlank()) {
            Log.e(TAG, "No se guarda el recorrido de ${estadoFinal.idSendero}: se desconoce el usuario que lo inició")
            return
        }
        val recorrido = construirRecorridoRealizado(estadoFinal, idUsuario, nombreSendero, reloj())
        alcance.launch {
            try {
                recorridoRepositorio.guardar(recorrido).onFailure { error ->
                    Log.e(TAG, "No se pudo guardar el recorrido ${recorrido.id}", error)
                }
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Log.e(TAG, "Fallo inesperado al guardar el recorrido ${recorrido.id}", error)
            }
        }
    }

    // Construir el recorrido realizado: el id sale del instante de inicio para que guardarlo dos veces sea idempotente
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
            // Ritmo medio de todo el recorrido (pasos totales sobre la duración), no el de la ventana de 10 s
            // En simulación se guarda null: el ritmo estaría multiplicado por la velocidad simulada
            ritmoPasosPorMinuto = estadoFinal.pasos?.takeUnless { estadoFinal.esSimulado }?.let { pasos ->
                if (duracion == 0L) 0.0 else pasos / (duracion / MILISEGUNDOS_POR_MINUTO)
            },
            trazadoCaminado = CalculadoraSeguimiento.cerrarTrazadoCaminado(estadoFinal),
            esSimulado = estadoFinal.esSimulado,
            estadoSincronizacion = EstadoSincronizacion.PENDIENTE
        )
    }
}
