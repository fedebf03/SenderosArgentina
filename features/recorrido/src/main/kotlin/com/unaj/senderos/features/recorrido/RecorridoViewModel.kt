package com.unaj.senderos.presentation.recorrido

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.repository.ControlSeguimiento
import com.unaj.senderos.domain.repository.LanzadorSeguimiento
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "RECORRIDO"
private const val PLAZO_INICIO_MILISEGUNDOS = 5_000L

// Mostrar el recorrido en curso a partir del control de seguimiento de ámbito de proceso
class RecorridoViewModel(
    private val idSendero: String,
    private val observarDetalleSenderoUseCase: ObservarDetalleSenderoUseCase,
    private val controlSeguimiento: ControlSeguimiento,
    private val lanzadorSeguimiento: LanzadorSeguimiento,
    capacidadesDispositivo: CapacidadesDispositivo,
    private val reloj: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(
        RecorridoEstadoVisual(
            tieneAcelerometro = capacidadesDispositivo.tieneAcelerometro,
            tieneBrujula = capacidadesDispositivo.tieneBrujula
        )
    )
    val estadoVisual: StateFlow<RecorridoEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<RecorridoEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    private var ultimoEstadoSeguimiento: EstadoSeguimiento? = null
    private var haVistoEstadoActivo = false
    private var estaFinalizando = false

    init {
        Log.d(TAG, "ViewModel → creado (${hashCode()})")
        observarSendero()
        observarSeguimiento()
        observarAvisos()
        esperarInicioDelServicio()
    }

    // Registrar la destrucción del ViewModel; el seguimiento sigue en el servicio
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel → onCleared")
    }

    // Procesar cada evento de la pantalla del recorrido
    fun procesarEvento(evento: RecorridoEvento) {
        when (evento) {
            is RecorridoEvento.AlVolver, is RecorridoEvento.AlCerrarResumen -> {
                emitirEfecto(RecorridoEfecto.VolverAlDetalle)
            }
            is RecorridoEvento.AlTocarFinalizar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoFinalizarVisible = true) }
            }
            is RecorridoEvento.AlCancelarFinalizar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoFinalizarVisible = false) }
            }
            is RecorridoEvento.AlConfirmarFinalizar -> finalizar()
            is RecorridoEvento.AlTocarCancelarRecorrido -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoCancelarVisible = true) }
            }
            is RecorridoEvento.AlDescartarCancelacion -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoCancelarVisible = false) }
            }
            is RecorridoEvento.AlConfirmarCancelarRecorrido -> cancelarRecorrido()
            is RecorridoEvento.AlSeguirCaminando -> {
                Log.d(TAG, "El usuario sigue caminando tras llegar al final")
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(seDescartoLlegada = true, esDialogoLlegadaVisible = false)
                }
            }
            is RecorridoEvento.AlMoverMapaConGesto -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(estaSiguiendo = false) }
            }
            is RecorridoEvento.AlSeleccionarMultiplicador -> seleccionarMultiplicador(evento.valor)
            is RecorridoEvento.AlTocarSimularDesvio -> {
                if (_estadoVisual.value.esSimulado) controlSeguimiento.simularDesvio()
            }
            is RecorridoEvento.AlTocarCentrar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(estaSiguiendo = true) }
            }
        }
    }

    // Observar el sendero para dibujar su recorrido en el mapa
    private fun observarSendero() {
        observarDetalleSenderoUseCase(idSendero).fold(
            onSuccess = { flujoSendero ->
                viewModelScope.launch {
                    flujoSendero
                        .catch { error -> Log.e(TAG, "Error al observar el sendero", error) }
                        .collect { senderoEmitido -> reflejarSendero(senderoEmitido) }
                }
            },
            onFailure = { error -> Log.e(TAG, "No se pudo observar el sendero", error) }
        )
    }

    // Guardar el sendero emitido en el estado visual
    private fun reflejarSendero(senderoEmitido: Sendero?) {
        _estadoVisual.update { estadoActual -> estadoActual.copy(sendero = senderoEmitido) }
    }

    // Observar el estado del control de seguimiento
    private fun observarSeguimiento() {
        viewModelScope.launch {
            controlSeguimiento.estado.collect { estado -> reflejarSeguimiento(estado) }
        }
    }

    // Reflejar el estado del seguimiento si es de este sendero; si se apaga por fuera, pasar a no activo
    private fun reflejarSeguimiento(estado: EstadoSeguimiento?) {
        val estadoDelSendero = estado?.takeIf { estadoEmitido -> estadoEmitido.idSendero == idSendero }
        if (estadoDelSendero != null) {
            haVistoEstadoActivo = true
            ultimoEstadoSeguimiento = estadoDelSendero
            if (estaFinalizando) return
            _estadoVisual.update { estadoActual ->
                estadoActual.copy(
                    fase = FaseRecorrido.EN_CURSO,
                    posicionUsuario = estadoDelSendero.posicionActual,
                    rumboGrados = estadoDelSendero.rumboEfectivoGrados,
                    velocidadActualMetrosPorSegundo = estadoDelSendero.velocidadMetrosPorSegundo,
                    velocidadPromedioKilometrosPorHora = estadoDelSendero.velocidadPromedioKilometrosPorHora,
                    pasos = estadoDelSendero.pasos,
                    ritmoPasosPorMinuto = estadoDelSendero.ritmoPasosPorMinuto,
                    estaDetenido = estadoDelSendero.estaDetenido,
                    calidadSenal = estadoDelSendero.calidadSenal,
                    esSimulado = estadoDelSendero.esSimulado,
                    multiplicadorSimulacion = estadoDelSendero.multiplicadorSimulacion,
                    distanciaRecorridaKilometros = estadoDelSendero.distanciaRecorridaKilometros,
                    distanciaRestanteKilometros = estadoDelSendero.distanciaRestanteKilometros,
                    instanteInicioMilisegundos = estadoDelSendero.instanteInicioMilisegundos,
                    proximoPunto = estadoDelSendero.trazado.getOrNull(estadoDelSendero.indiceProximoPunto),
                    estaDesviado = estadoDelSendero.estaDesviado,
                    nombreSendero = estadoDelSendero.nombreSendero,
                    llegoAlFinal = estadoDelSendero.llegoAlFinal,
                    esDialogoLlegadaVisible = estadoDelSendero.llegoAlFinal && !estadoActual.seDescartoLlegada
                )
            }
        } else if (haVistoEstadoActivo && !estaFinalizando) {
            // El seguimiento terminó por fuera de la pantalla (por ejemplo desde la notificación)
            val motivoFin = controlSeguimiento.motivoFin.value
            if (motivoFin == MotivoFinSeguimiento.CANCELADO_POR_EL_USUARIO) {
                Log.d(TAG, "El recorrido se canceló fuera de la pantalla; se vuelve al detalle sin resumen")
                estaFinalizando = true
                _estadoVisual.update { estadoActual -> estadoActual.copy(fase = FaseRecorrido.NO_ACTIVO) }
                emitirEfecto(RecorridoEfecto.VolverAlDetalle)
                emitirEfecto(RecorridoEfecto.MostrarAviso(describirMotivoFin(motivoFin)))
                return
            }
            Log.d(TAG, "El seguimiento terminó fuera de la pantalla; se muestra el resumen del último estado")
            ultimoEstadoSeguimiento?.let { estadoFinal -> mostrarResumen(estadoFinal) }
            motivoFin?.let { motivo ->
                emitirEfecto(RecorridoEfecto.MostrarAviso(describirMotivoFin(motivo)))
            }
        }
    }

    // Pedir al control la velocidad elegida; la pantalla muestra la que el control publica en su estado
    private fun seleccionarMultiplicador(valor: Int) {
        if (!_estadoVisual.value.esSimulado || valor !in MULTIPLICADORES_SIMULACION) return
        Log.d(TAG, "Velocidad de la simulación: x$valor")
        controlSeguimiento.cambiarMultiplicadorSimulacion(valor)
    }

    // Observar los avisos de proximidad y desvío para mostrarlos en pantalla
    private fun observarAvisos() {
        viewModelScope.launch {
            controlSeguimiento.avisos.collect { aviso ->
                Log.d(TAG, "Aviso del recorrido: $aviso")
                if (aviso is AvisoRecorrido.LlegadaAlFinal) {
                    // La llegada se avisa con un diálogo, no con el Snackbar
                    mostrarDialogoDeLlegada()
                } else {
                    emitirEfecto(RecorridoEfecto.MostrarAviso(describirAviso(aviso)))
                }
            }
        }
    }

    // Mostrar el diálogo de llegada salvo que ya se haya descartado o se esté finalizando
    private fun mostrarDialogoDeLlegada() {
        if (estaFinalizando) return
        _estadoVisual.update { estadoActual ->
            if (estadoActual.seDescartoLlegada) estadoActual else estadoActual.copy(esDialogoLlegadaVisible = true)
        }
    }

    // Dar un plazo al servicio para publicar el primer estado antes de declarar el recorrido inactivo
    private fun esperarInicioDelServicio() {
        viewModelScope.launch {
            delay(PLAZO_INICIO_MILISEGUNDOS)
            if (!haVistoEstadoActivo) {
                Log.d(TAG, "El servicio no publicó un estado dentro del plazo")
                _estadoVisual.update { estadoActual -> estadoActual.copy(fase = FaseRecorrido.NO_ACTIVO) }
            }
        }
    }

    // Detener el seguimiento y mostrar el resumen del último estado conocido
    private fun finalizar() {
        val estadoFinal = ultimoEstadoSeguimiento
        if (estaFinalizando || estadoFinal == null) {
            _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoFinalizarVisible = false) }
            return
        }
        lanzadorSeguimiento.detener()
        mostrarResumen(estadoFinal)
    }

    // Descartar el recorrido sin guardarlo ni mostrar resumen y volver al detalle
    private fun cancelarRecorrido() {
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(esDialogoCancelarVisible = false, esDialogoFinalizarVisible = false, esDialogoLlegadaVisible = false)
        }
        if (estaFinalizando) return
        estaFinalizando = true
        Log.d(TAG, "Recorrido cancelado por el usuario; se descarta sin guardar")
        // Primero se descarta en el control (síncrono) y después se detiene el servicio: al destruirse no queda nada por guardar
        controlSeguimiento.cancelar()
        lanzadorSeguimiento.detener()
        emitirEfecto(RecorridoEfecto.VolverAlDetalle)
    }

    // Capturar distancia y tiempo del estado indicado y mostrarlos en el diálogo de resumen
    private fun mostrarResumen(estadoFinal: EstadoSeguimiento) {
        estaFinalizando = true
        val resumen = ResumenRecorrido(
            distanciaRecorridaKilometros = estadoFinal.distanciaRecorridaKilometros,
            tiempoTranscurridoMilisegundos = (reloj() - estadoFinal.instanteInicioMilisegundos).coerceAtLeast(0L)
        )
        Log.d(TAG, "Recorrido finalizado: $resumen")
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(
                fase = FaseRecorrido.NO_ACTIVO,
                esDialogoFinalizarVisible = false,
                esDialogoLlegadaVisible = false,
                resumen = resumen
            )
        }
    }

    // Enviar un efecto único a la pantalla
    private fun emitirEfecto(efecto: RecorridoEfecto) {
        viewModelScope.launch {
            canalEfectos.send(efecto)
        }
    }
}
