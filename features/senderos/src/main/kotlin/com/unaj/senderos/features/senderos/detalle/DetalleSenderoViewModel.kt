package com.unaj.senderos.presentation.detalle

import com.unaj.senderos.presentation.comun.formatearDistanciaSendero
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.port.ControlSeguimiento
import com.unaj.senderos.domain.port.LanzadorSeguimiento
import com.unaj.senderos.domain.port.MonitorConectividad
import com.unaj.senderos.domain.port.PreferenciasAplicacion
import com.unaj.senderos.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.domain.usecase.DescargarMapaSinConexionUseCase
import com.unaj.senderos.domain.usecase.DescartarPuntosCapturadosUseCase
import com.unaj.senderos.domain.usecase.EliminarMapaSinConexionUseCase
import com.unaj.senderos.domain.usecase.IncorporarPuntosCapturadosUseCase
import com.unaj.senderos.domain.usecase.ObservarPuntosCapturadosConDistanciaUseCase
import com.unaj.senderos.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarMapaSinConexionUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.presentation.componentes.obtenerTextoDificultad
import com.unaj.senderos.presentation.revisionpuntos.ControladorRevisionPuntos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DETALLE_SENDERO"
private const val MENSAJE_ERROR_POR_DEFECTO = "Ocurrió un error inesperado"
private const val MENSAJE_SIN_RECORRIDO = "Este sendero no tiene recorrido"
private const val MENSAJE_PERMISO_DENEGADO =
    "Necesitamos el permiso de ubicación para seguir tu recorrido. Podés concederlo desde los ajustes del teléfono."
private const val MENSAJE_UBICACION_PRECISA = "Para seguir el recorrido hace falta la ubicación precisa"
private const val CANTIDAD_MINIMA_PUNTOS_RECORRIDO = 2

class DetalleSenderoViewModel(
    private val idSendero: String,
    private val observarDetalleSenderoUseCase: ObservarDetalleSenderoUseCase,
    private val alternarFavoritoUseCase: AlternarFavoritoUseCase,
    private val eliminarSenderoUseCase: EliminarSenderoUseCase,
    private val lanzadorSeguimiento: LanzadorSeguimiento,
    private val controlSeguimiento: ControlSeguimiento,
    private val preferenciasAplicacion: PreferenciasAplicacion,
    private val observarMapaSinConexionUseCase: ObservarMapaSinConexionUseCase,
    private val descargarMapaSinConexionUseCase: DescargarMapaSinConexionUseCase,
    private val eliminarMapaSinConexionUseCase: EliminarMapaSinConexionUseCase,
    private val monitorConectividad: MonitorConectividad,
    private val capacidadesDispositivo: CapacidadesDispositivo,
    observarPuntosCapturadosConDistanciaUseCase: ObservarPuntosCapturadosConDistanciaUseCase,
    incorporarPuntosCapturadosUseCase: IncorporarPuntosCapturadosUseCase,
    descartarPuntosCapturadosUseCase: DescartarPuntosCapturadosUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(DetalleSenderoEstadoVisual())
    val estadoVisual: StateFlow<DetalleSenderoEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<DetalleSenderoEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    private val controladorRevision = ControladorRevisionPuntos(
        idSendero = idSendero,
        observarPuntosCapturadosConDistanciaUseCase = observarPuntosCapturadosConDistanciaUseCase,
        incorporarPuntosCapturadosUseCase = incorporarPuntosCapturadosUseCase,
        descartarPuntosCapturadosUseCase = descartarPuntosCapturadosUseCase,
        alcance = viewModelScope,
        alEmitirAviso = { mensaje -> emitirEfecto(DetalleSenderoEfecto.MostrarAviso(mensaje)) }
    )

    private var estaEliminando = false

    private var trabajoAlternarFavorito: Job? = null

    private var trabajoMapaSinConexion: Job? = null
    private var trabajoDescargaMapa: Job? = null
    private var recorridoObservadoDelMapa: List<PuntoRecorrido>? = null

    init {
        observarSendero()
        observarRecorridoEnCurso()
        observarModoDemostracion()
        observarRevisionDePuntos()
    }

    fun procesarEvento(evento: DetalleSenderoEvento) {
        when (evento) {
            is DetalleSenderoEvento.AlVolver -> emitirEfecto(DetalleSenderoEfecto.VolverAlListado)
            is DetalleSenderoEvento.AlAlternarFavorito -> alternarFavorito()
            is DetalleSenderoEvento.AlCompartir -> compartir()
            is DetalleSenderoEvento.AlAlternarSeccion -> alternarSeccion(evento.seccion)
            is DetalleSenderoEvento.AlSeleccionarPuntoDeInteres -> enfocarPuntoDeInteres(evento.indice)
            is DetalleSenderoEvento.AlTocarIniciarRecorrido -> pedirPermisoParaIniciarRecorrido()
            is DetalleSenderoEvento.AlConcederPermisoUbicacion -> iniciarRecorrido()
            is DetalleSenderoEvento.AlDenegarPermisoUbicacion -> {
                Log.d(TAG, "Permiso de ubicación denegado para iniciar el recorrido")
                emitirEfecto(DetalleSenderoEfecto.MostrarAviso(MENSAJE_PERMISO_DENEGADO))
            }
            is DetalleSenderoEvento.AlConcederSoloUbicacionAproximada -> {
                Log.d(TAG, "Solo se concedió la ubicación aproximada; no alcanza para el recorrido")
                emitirEfecto(DetalleSenderoEfecto.MostrarAviso(MENSAJE_UBICACION_PRECISA))
            }
            is DetalleSenderoEvento.AlConfirmarReemplazarRecorrido -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(nombreSenderoEnCursoAReemplazar = null) }
                avisarSinGpsOSugerirDescarga()
            }
            is DetalleSenderoEvento.AlIniciarSinGps -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoSinGpsVisible = false) }
                sugerirDescargaOSolicitarPermiso()
            }
            is DetalleSenderoEvento.AlCancelarSinGps -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoSinGpsVisible = false) }
            }
            is DetalleSenderoEvento.AlTocarDescargarMapa -> descargarMapa()
            is DetalleSenderoEvento.AlTocarEliminarMapa -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoEliminarMapaVisible = true) }
            }
            is DetalleSenderoEvento.AlCancelarEliminarMapa -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoEliminarMapaVisible = false) }
            }
            is DetalleSenderoEvento.AlConfirmarEliminarMapa -> eliminarMapa()
            is DetalleSenderoEvento.AlDescargarEIniciar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esSugerenciaDescargaVisible = false) }
                descargarMapa()
                emitirEfecto(DetalleSenderoEfecto.SolicitarPermisoUbicacion)
            }
            is DetalleSenderoEvento.AlIniciarSinDescargar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esSugerenciaDescargaVisible = false) }
                emitirEfecto(DetalleSenderoEfecto.SolicitarPermisoUbicacion)
            }
            is DetalleSenderoEvento.AlCancelarReemplazarRecorrido -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(nombreSenderoEnCursoAReemplazar = null) }
            }
            is DetalleSenderoEvento.AlAbrirMenuOpciones -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esMenuOpcionesVisible = true) }
            }
            is DetalleSenderoEvento.AlCerrarMenuOpciones -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esMenuOpcionesVisible = false) }
            }
            is DetalleSenderoEvento.AlTocarEditar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esMenuOpcionesVisible = false) }
                emitirEfecto(DetalleSenderoEfecto.NavegarAEdicion(idSendero))
            }
            is DetalleSenderoEvento.AlTocarEliminar -> {
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(esMenuOpcionesVisible = false, esDialogoEliminarVisible = true)
                }
            }
            is DetalleSenderoEvento.AlCancelarEliminar -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoEliminarVisible = false) }
            }
            is DetalleSenderoEvento.AlConfirmarEliminar -> eliminar()
            is DetalleSenderoEvento.AlRevisionPuntos -> controladorRevision.procesarEvento(evento.evento)
            is DetalleSenderoEvento.AlAmpliarFoto -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(fotoAmpliada = evento.foto) }
            }
            is DetalleSenderoEvento.AlCerrarFotoAmpliada -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(fotoAmpliada = null) }
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

    private fun observarSendero() {
        observarDetalleSenderoUseCase(idSendero).fold(
            onSuccess = { flujoSendero ->
                viewModelScope.launch {
                    flujoSendero
                        .catch { error -> mostrarError(error.message) }
                        .collect { senderoEmitido -> reflejarSendero(senderoEmitido) }
                }
            },
            onFailure = { error -> mostrarError(error.message) }
        )
    }

    // Si el sendero no existe se informa, salvo que se esté eliminando
    private fun reflejarSendero(senderoEmitido: Sendero?) {
        if (senderoEmitido != null) {
            _estadoVisual.update { estadoActual ->
                estadoActual.copy(
                    sendero = senderoEmitido,
                    puntosDeInteres = obtenerPuntosDeInteres(senderoEmitido),
                    estaCargando = false,
                    mensajeError = null
                )
            }
            observarMapaSinConexion(senderoEmitido)
        } else if (!estaEliminando) {
            mostrarError("No se encontró el sendero")
        }
    }

    private fun mostrarError(mensajeOriginal: String?) {
        val mensaje = mensajeOriginal?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO
        Log.e(TAG, "Error en el detalle del sendero: $mensaje")
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(estaCargando = false, mensajeError = mensaje)
        }
    }

    private fun alternarFavorito() {
        if (trabajoAlternarFavorito?.isActive == true) return
        trabajoAlternarFavorito = viewModelScope.launch {
            alternarFavoritoUseCase(idSendero).onFailure { error ->
                Log.e(TAG, "Error al alternar favorito", error)
                canalEfectos.send(
                    DetalleSenderoEfecto.MostrarAviso(error.message?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO)
                )
            }
        }
    }

    private fun eliminar() {
        if (estaEliminando) return
        _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoEliminarVisible = false) }
        estaEliminando = true
        viewModelScope.launch {
            eliminarSenderoUseCase(idSendero).fold(
                onSuccess = { canalEfectos.send(DetalleSenderoEfecto.VolverAlListado) },
                onFailure = { error ->
                    estaEliminando = false
                    Log.e(TAG, "Error al eliminar el sendero", error)
                    canalEfectos.send(
                        DetalleSenderoEfecto.MostrarAviso(error.message?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO)
                    )
                }
            )
        }
    }

    private fun compartir() {
        val sendero = _estadoVisual.value.sendero ?: return
        val texto = "${sendero.nombre} (${sendero.region})\n" +
            "Distancia: ${formatearDistanciaSendero(sendero.distanciaKilometros)} · " +
            "Desnivel: ${sendero.desnivelMetros} m · Dificultad: ${obtenerTextoDificultad(sendero.dificultad)}\n" +
            "Compartido desde SenderosArgentina"
        emitirEfecto(DetalleSenderoEfecto.CompartirSendero(texto))
    }

    private fun alternarSeccion(seccion: SeccionDetalle) {
        _estadoVisual.update { estadoActual ->
            val seccionesNuevas = if (seccion in estadoActual.seccionesExpandidas) {
                estadoActual.seccionesExpandidas - seccion
            } else {
                estadoActual.seccionesExpandidas + seccion
            }
            estadoActual.copy(seccionesExpandidas = seccionesNuevas)
        }
    }

    // El contador se incrementa para poder repetir el enfoque sobre el mismo punto
    private fun enfocarPuntoDeInteres(indice: Int) {
        val coordenada = _estadoVisual.value.coordenadaDelPunto(indice) ?: return
        Log.d(TAG, "Enfocando el punto de interés $indice del sendero $idSendero")
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(puntoEnfocado = coordenada, contadorEnfoque = estadoActual.contadorEnfoque + 1)
        }
    }

    private fun observarModoDemostracion() {
        viewModelScope.launch {
            preferenciasAplicacion.esModoDemostracion.collect { activo ->
                _estadoVisual.update { estadoActual -> estadoActual.copy(esModoDemostracion = activo) }
            }
        }
    }

    private fun observarRecorridoEnCurso() {
        viewModelScope.launch {
            controlSeguimiento.estado.collect { estadoSeguimiento ->
                val esDeEsteSendero = estadoSeguimiento?.idSendero == idSendero
                val esSimulado = esDeEsteSendero && estadoSeguimiento?.esSimulado == true
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(
                        hayRecorridoEnCursoEnEsteSendero = esDeEsteSendero,
                        esRecorridoEnCursoSimulado = esSimulado
                    )
                }
            }
        }
    }

    private fun pedirPermisoParaIniciarRecorrido() {
        if (!tieneRecorridoSeguible()) {
            emitirEfecto(DetalleSenderoEfecto.MostrarAviso(MENSAJE_SIN_RECORRIDO))
            return
        }
        val recorridoActivo = controlSeguimiento.estado.value
        when {
            recorridoActivo == null -> avisarSinGpsOSugerirDescarga()
            recorridoActivo.idSendero == idSendero -> emitirEfecto(DetalleSenderoEfecto.NavegarARecorrido(idSendero))
            else -> pedirConfirmarReemplazo(recorridoActivo.idSendero)
        }
    }

    private fun pedirConfirmarReemplazo(idSenderoEnCurso: String) {
        viewModelScope.launch {
            val nombre = try {
                observarDetalleSenderoUseCase(idSenderoEnCurso).getOrNull()?.first()?.nombre
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Log.e(TAG, "No se pudo leer el sendero con el recorrido en curso", error)
                null
            } ?: "otro sendero"
            _estadoVisual.update { estadoActual -> estadoActual.copy(nombreSenderoEnCursoAReemplazar = nombre) }
        }
    }

    private fun iniciarRecorrido() {
        if (!tieneRecorridoSeguible()) {
            emitirEfecto(DetalleSenderoEfecto.MostrarAviso(MENSAJE_SIN_RECORRIDO))
            return
        }
        Log.d(TAG, "Iniciando el recorrido del sendero $idSendero")
        lanzadorSeguimiento.iniciar(idSendero, esSimulado = preferenciasAplicacion.esModoDemostracion.value)
        emitirEfecto(DetalleSenderoEfecto.NavegarARecorrido(idSendero))
    }

    private fun tieneRecorridoSeguible(): Boolean {
        val cantidadPuntos = _estadoVisual.value.sendero?.recorrido?.size ?: 0
        return cantidadPuntos >= CANTIDAD_MINIMA_PUNTOS_RECORRIDO
    }

    private fun observarMapaSinConexion(sendero: Sendero) {
        if (sendero.recorrido == recorridoObservadoDelMapa) return
        recorridoObservadoDelMapa = sendero.recorrido
        trabajoMapaSinConexion?.cancel()
        if (sendero.recorrido.isEmpty()) {
            _estadoVisual.update { estadoActual ->
                estadoActual.copy(estadoMapaSinConexion = EstadoMapaSinConexion.NoDescargado)
            }
            return
        }
        observarMapaSinConexionUseCase(sendero).fold(
            onSuccess = { flujoEstado ->
                trabajoMapaSinConexion = viewModelScope.launch {
                    flujoEstado
                        .catch { error -> Log.e(TAG, "Error al observar el mapa sin conexión", error) }
                        .collect { estadoMapa ->
                            _estadoVisual.update { estadoActual -> estadoActual.copy(estadoMapaSinConexion = estadoMapa) }
                        }
                }
            },
            onFailure = { error -> Log.e(TAG, "No se pudo observar el mapa sin conexión", error) }
        )
    }

    // El avance llega por el flujo de estado
    private fun descargarMapa() {
        val sendero = _estadoVisual.value.sendero ?: return
        val estadoMapa = _estadoVisual.value.estadoMapaSinConexion
        val hayDescargaActiva = estadoMapa is EstadoMapaSinConexion.Descargando ||
            estadoMapa is EstadoMapaSinConexion.EsperandoConexion
        if (hayDescargaActiva || trabajoDescargaMapa?.isActive == true) return
        Log.d(TAG, "Descargando el mapa sin conexión del sendero $idSendero")
        trabajoDescargaMapa = viewModelScope.launch {
            descargarMapaSinConexionUseCase(sendero).onFailure { error ->
                Log.e(TAG, "Error al descargar el mapa sin conexión", error)
                canalEfectos.send(
                    DetalleSenderoEfecto.MostrarAviso(error.message?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO)
                )
            }
        }
    }

    private fun eliminarMapa() {
        _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoEliminarMapaVisible = false) }
        Log.d(TAG, "Eliminando el mapa sin conexión del sendero $idSendero")
        viewModelScope.launch {
            eliminarMapaSinConexionUseCase(idSendero).onFailure { error ->
                Log.e(TAG, "Error al eliminar el mapa sin conexión", error)
                canalEfectos.send(
                    DetalleSenderoEfecto.MostrarAviso(error.message?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO)
                )
            }
        }
    }

    private fun avisarSinGpsOSugerirDescarga() {
        val esSimulado = preferenciasAplicacion.esModoDemostracion.value
        if (!capacidadesDispositivo.tieneGps && !esSimulado) {
            _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoSinGpsVisible = true) }
        } else {
            sugerirDescargaOSolicitarPermiso()
        }
    }

    private fun sugerirDescargaOSolicitarPermiso() {
        val estadoMapa = _estadoVisual.value.estadoMapaSinConexion
        val conviene = estadoMapa is EstadoMapaSinConexion.NoDescargado || estadoMapa is EstadoMapaSinConexion.Desactualizado
        if (conviene && monitorConectividad.hayConexion()) {
            _estadoVisual.update { estadoActual -> estadoActual.copy(esSugerenciaDescargaVisible = true) }
        } else {
            emitirEfecto(DetalleSenderoEfecto.SolicitarPermisoUbicacion)
        }
    }

    private fun emitirEfecto(efecto: DetalleSenderoEfecto) {
        viewModelScope.launch {
            canalEfectos.send(efecto)
        }
    }
}
