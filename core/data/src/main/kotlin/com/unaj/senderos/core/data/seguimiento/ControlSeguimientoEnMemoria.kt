package com.unaj.senderos.data.seguimiento

import android.util.Log
import com.unaj.senderos.domain.CalculadoraSeguimiento
import com.unaj.senderos.domain.CalculadoraRecorrido
import com.unaj.senderos.domain.DetectorMovimiento
import com.unaj.senderos.domain.DetectorPasos
import com.unaj.senderos.domain.VentanaRitmoPasos
import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.repository.FuenteAcelerometro
import com.unaj.senderos.domain.repository.FuenteBrujula
import com.unaj.senderos.domain.repository.ProveedorDeclinacion
import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.CalidadSenal
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.domain.model.RecorridoRealizado
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.repository.ControlSeguimiento
import com.unaj.senderos.domain.repository.ProveedorUbicacion
import com.unaj.senderos.domain.repository.RecorridoRepositorio
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
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
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "SEGUIMIENTO"
private const val CAPACIDAD_AVISOS = 16
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
    private val alcance: CoroutineScope,
    private val capacidades: CapacidadesDispositivo = CapacidadesDispositivo.SIN_FUNCIONES,
    private val fuenteAcelerometro: FuenteAcelerometro? = null,
    private val fuenteBrujula: FuenteBrujula? = null,
    private val proveedorDeclinacion: ProveedorDeclinacion? = null,
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

    // Detener el seguimiento activo guardando el recorrido realizado y dejar el estado en null
    override fun detener() {
        finalizarSeguimiento(guardar = true)
    }

    // Cancelar el seguimiento activo sin guardar nada en el historial y dejar el estado en null
    override fun cancelar() {
        finalizarSeguimiento(guardar = false)
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
                estadoActual.copy(calidadSenal = CalculadoraSeguimiento.evaluarCalidad(estadoActual, ahora))
            }
        }
    }

    // Lanzar la recolección del acelerómetro y de la brújula según lo que ofrece el equipo
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

    // Recolectar las muestras del acelerómetro y registrar cada paso detectado; si el sensor falla o termina se dejan de medir pasos sin cortar el seguimiento
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

    // Registrar un paso en la ventana de ritmo con el instante en que el sensor lo midió (no el de llegada, que puede venir por lotes) y reflejarlo en el estado
    private fun registrarPaso(instantePasoMilisegundos: Long) {
        ventanaRitmo.registrarPaso(instantePasoMilisegundos)
        instanteUltimoPaso = maxOf(instanteUltimoPaso ?: instantePasoMilisegundos, instantePasoMilisegundos)
        actualizarConSensores(reloj()) { estadoActual -> estadoActual.copy(pasos = (estadoActual.pasos ?: 0) + 1) }
    }

    // Recolectar el rumbo de la brújula solo mientras el usuario está detenido (en movimiento manda el GPS) y reflejarlo en el estado
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

    // Aplicar un cambio al estado activo y completar los datos de sensores; el efecto sobre el último paso simulado se resuelve fuera del estado publicado
    private fun actualizarConSensores(ahora: Long, transformar: (EstadoSeguimiento) -> EstadoSeguimiento) {
        val anterior = estadoInterno.value ?: return
        val transformado = transformar(anterior)
        if (transformado.esSimulado && calcularPasosSimulados(transformado) > (anterior.pasos ?: 0)) {
            instanteUltimoPaso = ahora
        }
        estadoInterno.value = completarSensores(transformado, ahora, instanteUltimoPaso)
    }

    // Calcular los pasos simulados: uno cada 0,75 m de la distancia recorrida
    private fun calcularPasosSimulados(estado: EstadoSeguimiento): Int {
        return (estado.distanciaRecorridaKilometros * METROS_POR_KILOMETRO / METROS_POR_PASO_SIMULADO).toInt()
    }

    // Completar sin efectos secundarios los datos derivados de sensores: pasos, ritmo, detenido y rumbo de brújula verdadero
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
            midePasos = estado.esSimulado || pasosRealesActivos
        )
        return estado.copy(
            pasos = pasos,
            ritmoPasosPorMinuto = ritmo,
            estaDetenido = estaDetenido,
            rumboBrujulaGrados = calcularRumboVerdadero(estado.posicionActual, ahora)
        )
    }

    // Convertir el último rumbo magnético a norte verdadero sumando la declinación de la última posición conocida (sin posición o sin proveedor queda magnético)
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

    // Obtener el sendero del usuario con sesión activa o fallar con un mensaje claro
    private suspend fun obtenerSendero(idSendero: String): Sendero {
        val flujoSendero = observarDetalleSendero(idSendero).getOrElse { error ->
            throw IllegalStateException("No se puede iniciar el recorrido: ${error.message}", error)
        }
        return flujoSendero.first() ?: throw IllegalStateException("No se encontró el sendero $idSendero")
    }

    // Recolectar las ubicaciones y aplicar cada una al estado, finalizando el seguimiento si el proveedor falla
    private suspend fun recolectarUbicaciones(proveedorActivo: ProveedorUbicacion) {
        try {
            proveedorActivo.observarUbicaciones().collect { lectura ->
                val estadoActual = estadoInterno.value
                if (estadoActual != null) {
                    val ahora = reloj()
                    val resultado = CalculadoraSeguimiento.actualizar(estadoActual, lectura, ahora)
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

    // Guardar en segundo plano, con el usuario que lo inició aunque la sesión ya se haya cerrado, el recorrido que terminó, sin bloquear ni romper la detención; no guarda si no hubo ninguna posición
    private fun guardarRecorridoRealizado(estadoFinal: EstadoSeguimiento?, nombreSendero: String, idUsuario: String) {
        if (estadoFinal == null || estadoFinal.trazadoCaminado.isEmpty()) return
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
            trazadoCaminado = estadoFinal.trazadoCaminado,
            esSimulado = estadoFinal.esSimulado,
            estadoSincronizacion = EstadoSincronizacion.PENDIENTE
        )
    }
}
