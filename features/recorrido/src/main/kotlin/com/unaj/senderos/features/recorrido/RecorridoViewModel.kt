package com.unaj.senderos.features.recorrido

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.AvisoRecorrido
import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
import com.unaj.senderos.core.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.port.ControlSeguimiento
import com.unaj.senderos.core.domain.port.LanzadorSeguimiento
import com.unaj.senderos.core.domain.usecase.DescartarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.IncorporarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarPuntosCapturadosConDistanciaUseCase
import com.unaj.senderos.core.ui.comun.describirErrorParaElUsuario
import com.unaj.senderos.core.ui.revisionpuntos.ControladorRevisionPuntos
import com.unaj.senderos.core.ui.revisionpuntos.describirPuntoGuardadoConPosicionAntigua
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.unaj.senderos.core.domain.port.Reloj

private const val TAG = "RECORRIDO"
private const val PLAZO_INICIO_MILISEGUNDOS = 5_000L
private const val MENSAJE_PUNTO_GUARDADO = "Punto guardado"
private const val MENSAJE_ERROR_PUNTO = "No se pudo guardar el punto"
private const val MENSAJE_SIN_POSICION_PARA_PUNTO = "Todavía no hay una posición válida del GPS; esperá a tener señal"

@HiltViewModel(assistedFactory = RecorridoViewModel.Fabrica::class)
class RecorridoViewModel @AssistedInject constructor(
    @Assisted private val idSendero: String,
    private val observarDetalleSenderoUseCase: ObservarDetalleSenderoUseCase,
    private val controlSeguimiento: ControlSeguimiento,
    private val lanzadorSeguimiento: LanzadorSeguimiento,
    capacidadesDispositivo: CapacidadesDispositivo,
    observarPuntosCapturadosConDistanciaUseCase: ObservarPuntosCapturadosConDistanciaUseCase,
    incorporarPuntosCapturadosUseCase: IncorporarPuntosCapturadosUseCase,
    descartarPuntosCapturadosUseCase: DescartarPuntosCapturadosUseCase,
    private val reloj: Reloj = Reloj(System::currentTimeMillis)
) : ViewModel() {

    @AssistedFactory
    interface Fabrica {
        fun crear(idSendero: String): RecorridoViewModel
    }

    private val _estadoVisual = MutableStateFlow(
        RecorridoEstadoVisual(
            tieneAcelerometro = capacidadesDispositivo.tieneAcelerometro,
            tieneBrujula = capacidadesDispositivo.tieneBrujula,
            esCaminataLibre = IdentificadorCaminataLibre.esDeCaminataLibre(idSendero)
        )
    )
    val estadoVisual: StateFlow<RecorridoEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<RecorridoEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    private val controladorRevision = ControladorRevisionPuntos(
        idSendero = idSendero,
        observarPuntosCapturadosConDistanciaUseCase = observarPuntosCapturadosConDistanciaUseCase,
        incorporarPuntosCapturadosUseCase = incorporarPuntosCapturadosUseCase,
        descartarPuntosCapturadosUseCase = descartarPuntosCapturadosUseCase,
        alcance = viewModelScope,
        alEmitirAviso = { mensaje -> emitirEfecto(RecorridoEfecto.MostrarAviso(mensaje)) }
    )

    private var estaCapturandoPunto = false
    // Posición del momento en que se tocó el botón "+": el usuario sigue caminando mientras elige el tipo o saca la foto
    private var posicionReservadaParaPunto: Coordenada? = null
    private var ultimoEstadoSeguimiento: EstadoSeguimiento? = null
    private var haVistoEstadoActivo = false
    private var estaFinalizando = false

    init {
        Log.d(TAG, "ViewModel → creado (${hashCode()})")
        // Una caminata libre no tiene sendero que observar
        if (!IdentificadorCaminataLibre.esDeCaminataLibre(idSendero)) observarSendero()
        observarSeguimiento()
        observarAvisos()
        observarRevisionDePuntos()
        esperarInicioDelServicio()
    }

    // El seguimiento sigue en el servicio
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel → onCleared")
    }

    // Procesar cada evento de la pantalla y traducirlo en cambios de estado o acciones sobre el seguimiento
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
            is RecorridoEvento.AlSeleccionarPuntoEnMapa -> {
                _estadoVisual.update { estadoActual ->
                    val punto = evento.indice?.let { indice -> estadoActual.sendero?.recorrido?.getOrNull(indice) }
                    estadoActual.copy(indicePuntoSeleccionado = evento.indice.takeIf { punto != null && punto.tipo != TipoPunto.PASO })
                }
            }
            is RecorridoEvento.AlSeleccionarMultiplicador -> seleccionarMultiplicador(evento.valor)
            is RecorridoEvento.AlTocarSimularDesvio -> {
                if (_estadoVisual.value.esSimulado) controlSeguimiento.simularDesvio()
            }
            is RecorridoEvento.AlTocarAgregarPunto -> abrirHojaPunto()
            is RecorridoEvento.AlCerrarHojaPunto -> cerrarHojaPunto()
            is RecorridoEvento.AlElegirTipoPunto -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(tipoPuntoElegido = evento.tipo) }
            }
            is RecorridoEvento.AlCambiarNotaPunto -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(notaPunto = evento.nota) }
            }
            is RecorridoEvento.AlSeleccionarFotoPunto -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(fotoPunto = evento.uri) }
            }
            is RecorridoEvento.AlQuitarFotoPunto -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(fotoPunto = null) }
            }
            is RecorridoEvento.AlFallarFotoPunto -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(mensajeErrorPunto = evento.mensaje) }
            }
            is RecorridoEvento.AlGuardarPunto -> guardarPunto()
            is RecorridoEvento.AlRevisarPuntos -> controladorRevision.procesarEvento(evento.evento)
            is RecorridoEvento.AlConvertirEnSendero -> emitirEfecto(RecorridoEfecto.NavegarAFormularioDesdeCaminata(idSendero))
            is RecorridoEvento.AlTocarCentrar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(estaSiguiendo = true) }
            }
        }
    }

    private fun observarRevisionDePuntos() {
        viewModelScope.launch {
            controladorRevision.estado.collect { revision ->
                _estadoVisual.update { estadoActual -> estadoActual.copy(revision = revision) }
            }
        }
    }

    // Reservar la posición actual y abrir la hoja limpia; sin posición válida avisa en lugar de abrirla
    private fun abrirHojaPunto() {
        val estadoActual = _estadoVisual.value
        if (!estadoActual.puedeAgregarPunto || estadoActual.esHojaPuntoVisible) return
        val posicion = estadoActual.posicionUsuario
        if (posicion == null) {
            emitirEfecto(RecorridoEfecto.MostrarAviso(MENSAJE_SIN_POSICION_PARA_PUNTO))
            return
        }
        Log.d(TAG, "Se reserva la posición para un punto y se abre la hoja")
        posicionReservadaParaPunto = posicion
        _estadoVisual.update { estadoNuevo ->
            estadoNuevo.copy(
                esHojaPuntoVisible = true,
                tipoPuntoElegido = TIPO_PUNTO_INICIAL,
                notaPunto = "",
                fotoPunto = null,
                mensajeErrorPunto = null
            )
        }
    }

    // Cerrar la hoja y soltar la posición reservada
    private fun cerrarHojaPunto() {
        posicionReservadaParaPunto = null
        _estadoVisual.update { estadoActual -> estadoActual.copy(esHojaPuntoVisible = false, mensajeErrorPunto = null) }
    }

    // Guardar el punto con el tipo, la nota y la foto de la hoja, en la posición reservada al tocar el botón; un doble toque guarda una sola vez
    private fun guardarPunto() {
        if (estaCapturandoPunto) return
        val estadoActual = _estadoVisual.value
        estaCapturandoPunto = true
        _estadoVisual.update { estadoNuevo -> estadoNuevo.copy(estaGuardandoPunto = true, mensajeErrorPunto = null) }
        viewModelScope.launch {
            val resultado = controlSeguimiento.capturarPunto(
                tipo = estadoActual.tipoPuntoElegido,
                nota = estadoActual.notaPunto.trim(),
                uriFoto = estadoActual.fotoPunto,
                posicion = posicionReservadaParaPunto
            )
            estaCapturandoPunto = false
            resultado.fold(
                onSuccess = { resultadoCaptura ->
                    Log.d(TAG, "Punto capturado: ${resultadoCaptura.punto.tipo}")
                    posicionReservadaParaPunto = null
                    _estadoVisual.update { estadoNuevo ->
                        estadoNuevo.copy(esHojaPuntoVisible = false, estaGuardandoPunto = false)
                    }
                    val mensajeGuardado = resultadoCaptura.antiguedadPosicionMilisegundos
                        ?.let { antiguedad -> describirPuntoGuardadoConPosicionAntigua(antiguedad) }
                        ?: MENSAJE_PUNTO_GUARDADO
                    emitirEfecto(RecorridoEfecto.MostrarAviso(mensajeGuardado))
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo capturar el punto", error)
                    val mensaje = describirErrorParaElUsuario(error, MENSAJE_ERROR_PUNTO)
                    _estadoVisual.update { estadoNuevo ->
                        estadoNuevo.copy(estaGuardandoPunto = false, mensajeErrorPunto = mensaje)
                    }
                }
            )
        }
    }

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

    private fun reflejarSendero(senderoEmitido: Sendero?) {
        val largoTrazado = senderoEmitido
            ?.let { sendero -> CalculadoraRecorrido.distanciaKilometros(sendero.recorrido, sendero.tipoRecorrido) }
            ?: 0.0
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(
                sendero = senderoEmitido,
                largoTrazadoKilometros = largoTrazado,
                perfilEstaDesactualizado = senderoEmitido?.let { sendero ->
                    sendero.perfilElevacion != null && CalculadoraPerfil.necesitaPerfil(sendero)
                } ?: false
            )
        }
    }

    private fun observarSeguimiento() {
        viewModelScope.launch {
            controlSeguimiento.estado.collect { estado -> reflejarSeguimiento(estado) }
        }
    }

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
                    trazadoCaminado = estadoDelSendero.trazadoCaminado,
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
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(fase = FaseRecorrido.NO_ACTIVO, esHojaPuntoVisible = false)
                }
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

    // La pantalla muestra la velocidad que el control publica en su estado
    private fun seleccionarMultiplicador(valor: Int) {
        if (!_estadoVisual.value.esSimulado || valor !in MULTIPLICADORES_SIMULACION) return
        Log.d(TAG, "Velocidad de la simulación: x$valor")
        controlSeguimiento.cambiarMultiplicadorSimulacion(valor)
    }

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
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(fase = FaseRecorrido.NO_ACTIVO, esHojaPuntoVisible = false)
                }
            }
        }
    }

    private fun finalizar() {
        val estadoFinal = ultimoEstadoSeguimiento
        if (estaFinalizando || estadoFinal == null) {
            _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoFinalizarVisible = false) }
            return
        }
        lanzadorSeguimiento.detener()
        mostrarResumen(estadoFinal)
    }

    private fun cancelarRecorrido() {
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(
                esDialogoCancelarVisible = false,
                esDialogoFinalizarVisible = false,
                esDialogoLlegadaVisible = false,
                esHojaPuntoVisible = false
            )
        }
        if (estaFinalizando) return
        estaFinalizando = true
        Log.d(TAG, "Recorrido cancelado por el usuario; se descarta sin guardar")
        // Primero se descarta en el control (síncrono) y después se detiene el servicio: al destruirse no queda nada por guardar
        controlSeguimiento.cancelar()
        lanzadorSeguimiento.detener()
        emitirEfecto(RecorridoEfecto.VolverAlDetalle)
    }

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
                esHojaPuntoVisible = false,
                resumen = resumen
            )
        }
    }

    private fun emitirEfecto(efecto: RecorridoEfecto) {
        viewModelScope.launch {
            canalEfectos.send(efecto)
        }
    }
}
