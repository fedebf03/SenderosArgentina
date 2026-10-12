package com.unaj.senderos.data.seguimiento

import android.util.Log
import com.unaj.senderos.domain.CalculadoraSeguimiento
import com.unaj.senderos.domain.model.AvisoRecorrido
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "SEGUIMIENTO"
private const val CAPACIDAD_AVISOS = 16
private const val MILISEGUNDOS_POR_HORA = 3_600_000.0
private const val KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO = 3.6

// Controlar en memoria el único recorrido activo del proceso; solo usa android.util.Log, sin servicios de Android, para poder probarse en JVM
class ControlSeguimientoEnMemoria(
    private val proveedorUbicacion: ProveedorUbicacion,
    private val observarDetalleSendero: ObservarDetalleSenderoUseCase,
    private val recorridoRepositorio: RecorridoRepositorio,
    private val alcance: CoroutineScope,
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
            val estadoInicial = CalculadoraSeguimiento.iniciar(sendero, reloj()).copy(esSimulado = esSimulado)
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

    // Detener el seguimiento activo y dejar el estado en null
    override fun detener() {
        generacion++
        guardarRecorridoRealizado(estadoInterno.value, nombreSenderoActivo, idUsuarioActivo)
        trabajoUbicaciones?.cancel()
        trabajoUbicaciones = null
        proveedorSimulado = null
        motivoFinInterno.value = null
        estadoInterno.value = null
        Log.d(TAG, "Seguimiento detenido")
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
                    val resultado = CalculadoraSeguimiento.actualizar(estadoActual, lectura, reloj())
                    estadoInterno.update { actual -> actual?.let { resultado.estado.copy(multiplicadorSimulacion = it.multiplicadorSimulacion) } }
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
            pasos = null,
            ritmoPasosPorMinuto = null,
            trazadoCaminado = estadoFinal.trazadoCaminado,
            esSimulado = estadoFinal.esSimulado,
            estadoSincronizacion = EstadoSincronizacion.PENDIENTE
        )
    }
}
