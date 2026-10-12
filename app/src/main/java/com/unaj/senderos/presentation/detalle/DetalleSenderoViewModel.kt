package com.unaj.senderos.presentation.detalle

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.repository.ControlSeguimiento
import com.unaj.senderos.domain.repository.LanzadorSeguimiento
import com.unaj.senderos.domain.repository.PreferenciasAplicacion
import com.unaj.senderos.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.presentation.componentes.obtenerTextoDificultad
import kotlin.math.roundToInt
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
    private val preferenciasAplicacion: PreferenciasAplicacion
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(DetalleSenderoEstadoVisual())
    val estadoVisual: StateFlow<DetalleSenderoEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<DetalleSenderoEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    private var estaEliminando = false

    private var trabajoAlternarFavorito: Job? = null

    init {
        observarSendero()
        observarRecorridoEnCurso()
        observarModoDemostracion()
    }

    // Procesar cada evento de la pantalla de detalle
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
        }
    }

    // Observar el sendero del usuario y reflejar sus cambios en el estado visual
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

    // Guardar el sendero emitido, o informar que no existe si no se está eliminando
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
        } else if (!estaEliminando) {
            mostrarError("No se encontró el sendero")
        }
    }

    // Registrar el error y mostrarlo en el estado visual
    private fun mostrarError(mensajeOriginal: String?) {
        val mensaje = mensajeOriginal?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_POR_DEFECTO
        Log.e(TAG, "Error en el detalle del sendero: $mensaje")
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(estaCargando = false, mensajeError = mensaje)
        }
    }

    // Alternar el favorito ignorando toques mientras hay una operación en curso
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

    // Eliminar el sendero ignorando llamadas repetidas y volver al listado si sale bien
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

    // Armar el texto del sendero cargado y pedir a la pantalla que lo comparta
    private fun compartir() {
        val sendero = _estadoVisual.value.sendero ?: return
        val texto = "${sendero.nombre} (${sendero.region})\n" +
            "Distancia: ${sendero.distanciaKilometros.roundToInt()} km · " +
            "Desnivel: ${sendero.desnivelMetros} m · Dificultad: ${obtenerTextoDificultad(sendero.dificultad)}\n" +
            "Compartido desde SenderosArgentina"
        emitirEfecto(DetalleSenderoEfecto.CompartirSendero(texto))
    }

    // Expandir o contraer una sección desplegable del detalle
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

    // Enfocar el mapa en el punto elegido, incrementando el contador para repetir el enfoque sobre el mismo punto
    private fun enfocarPuntoDeInteres(indice: Int) {
        val coordenada = _estadoVisual.value.coordenadaDelPunto(indice) ?: return
        Log.d(TAG, "Enfocando el punto de interés $indice del sendero $idSendero")
        _estadoVisual.update { estadoActual ->
            estadoActual.copy(puntoEnfocado = coordenada, contadorEnfoque = estadoActual.contadorEnfoque + 1)
        }
    }

    // Reflejar el modo demostración para que el botón diga "Simular recorrido"
    private fun observarModoDemostracion() {
        viewModelScope.launch {
            preferenciasAplicacion.esModoDemostracion.collect { activo ->
                _estadoVisual.update { estadoActual -> estadoActual.copy(esModoDemostracion = activo) }
            }
        }
    }

    // Observar si hay un recorrido activo en este sendero para mostrar "Ver recorrido en curso"
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

    // Resolver el toque en iniciar: ver el recorrido activo, confirmar el reemplazo de otro o pedir el permiso
    private fun pedirPermisoParaIniciarRecorrido() {
        if (!tieneRecorridoSeguible()) {
            emitirEfecto(DetalleSenderoEfecto.MostrarAviso(MENSAJE_SIN_RECORRIDO))
            return
        }
        val recorridoActivo = controlSeguimiento.estado.value
        when {
            recorridoActivo == null -> emitirEfecto(DetalleSenderoEfecto.SolicitarPermisoUbicacion)
            recorridoActivo.idSendero == idSendero -> emitirEfecto(DetalleSenderoEfecto.NavegarARecorrido(idSendero))
            else -> pedirConfirmarReemplazo(recorridoActivo.idSendero)
        }
    }

    // Mostrar la confirmación para finalizar el recorrido de otro sendero antes de iniciar este
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

    // Lanzar el seguimiento en segundo plano y navegar a la pantalla del recorrido en curso
    private fun iniciarRecorrido() {
        if (!tieneRecorridoSeguible()) {
            emitirEfecto(DetalleSenderoEfecto.MostrarAviso(MENSAJE_SIN_RECORRIDO))
            return
        }
        Log.d(TAG, "Iniciando el recorrido del sendero $idSendero")
        lanzadorSeguimiento.iniciar(idSendero, esSimulado = preferenciasAplicacion.esModoDemostracion.value)
        emitirEfecto(DetalleSenderoEfecto.NavegarARecorrido(idSendero))
    }

    // Verificar si el sendero cargado tiene al menos dos puntos para poder seguirlo
    private fun tieneRecorridoSeguible(): Boolean {
        val cantidadPuntos = _estadoVisual.value.sendero?.recorrido?.size ?: 0
        return cantidadPuntos >= CANTIDAD_MINIMA_PUNTOS_RECORRIDO
    }

    // Enviar un efecto único a la pantalla
    private fun emitirEfecto(efecto: DetalleSenderoEfecto) {
        viewModelScope.launch {
            canalEfectos.send(efecto)
        }
    }
}
