package com.unaj.senderos.features.perfil

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.port.PreferenciasAplicacion
import com.unaj.senderos.core.domain.port.ControlSeguimiento
import com.unaj.senderos.core.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.core.domain.usecase.ObservarRecorridosUseCase
import com.unaj.senderos.core.domain.usecase.AnalisisCopiaSeguridad
import com.unaj.senderos.core.domain.usecase.ExportarCopiaSeguridadUseCase
import com.unaj.senderos.core.domain.usecase.ImportarCopiaSeguridadUseCase
import com.unaj.senderos.core.domain.usecase.ModoRestauracion
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.domain.usecase.SincronizarPerfilPendienteUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val MENSAJE_ERROR_COPIA = "No se pudo completar la copia de seguridad"
private const val TAG = "PERFIL"
private const val MENSAJE_ERROR_HISTORIAL = "No pudimos cargar tu historial de recorridos"

@HiltViewModel
class PerfilViewModel @Inject constructor(
    obtenerUsuarioActualUseCase: ObtenerUsuarioActualUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase,
    private val preferenciasAplicacion: PreferenciasAplicacion,
    private val observarRecorridosUseCase: ObservarRecorridosUseCase,
    private val controlSeguimiento: ControlSeguimiento,
    private val exportarCopiaSeguridadUseCase: ExportarCopiaSeguridadUseCase,
    private val importarCopiaSeguridadUseCase: ImportarCopiaSeguridadUseCase,
    private val sincronizarPerfilPendienteUseCase: SincronizarPerfilPendienteUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(
        PerfilEstadoVisual(
            usuario = obtenerUsuarioActualUseCase()
        )
    )
    val estadoVisual: StateFlow<PerfilEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<PerfilEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    init {
        sincronizarPerfilPendiente()
        observarModoDemostracion()
        observarRecorridos()
        observarRecorridoEnCurso()
    }

    // Si el perfil se editó sin conexión se reintenta la subida. Un fallo lo deja pendiente sin molestar
    private fun sincronizarPerfilPendiente() {
        viewModelScope.launch {
            try {
                sincronizarPerfilPendienteUseCase()
            } catch (excepcion: kotlinx.coroutines.CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Log.e(TAG, "No se pudo sincronizar el perfil pendiente", error)
            }
        }
    }

    fun procesarEvento(evento: PerfilEvento) {
        when (evento) {
            // RF5 - Simulación: con el modo demostración activo los recorridos se simulan sobre el trazado sin caminar
            is PerfilEvento.AlCambiarModoDemostracion -> {
                Log.d(TAG, "Modo demostración: ${evento.activo}")
                preferenciasAplicacion.cambiarModoDemostracion(evento.activo)
            }
            is PerfilEvento.AlSeleccionarRecorrido -> {
                Log.d(TAG, "Abriendo el recorrido ${evento.idRecorrido}")
                viewModelScope.launch { canalEfectos.send(PerfilEfecto.NavegarAHistorialDetalle(evento.idRecorrido)) }
            }
            is PerfilEvento.AlTocarEditarPerfil -> canalEfectos.trySend(PerfilEfecto.NavegarAEditarPerfil)
            is PerfilEvento.AlTocarCerrarSesion -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoCerrarSesionVisible = true) }
            }
            is PerfilEvento.AlCancelarCerrarSesion -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoCerrarSesionVisible = false) }
            }
            is PerfilEvento.AlConfirmarCerrarSesion -> cerrarSesion()
            is PerfilEvento.AlExportarCopiaSeguridad -> exportarCopia(evento.uri)
            is PerfilEvento.AlImportarCopiaSeguridad -> analizarCopia(evento.uri)
            is PerfilEvento.AlElegirModoRestauracion -> {
                _estadoVisual.value.copiaPendiente?.let { analisis -> restaurar(analisis, evento.modo) }
            }
            is PerfilEvento.AlCancelarRestauracion -> {
                _estadoVisual.update { estadoActual -> estadoActual.copy(copiaPendiente = null) }
            }
        }
    }

    // RF4 - Exportar: guardamos todos los senderos del usuario en el archivo que eligió como copia de seguridad
    private fun exportarCopia(uri: String) {
        if (_estadoVisual.value.estaProcesandoCopia) return
        _estadoVisual.update { estadoActual -> estadoActual.copy(estaProcesandoCopia = true) }
        viewModelScope.launch {
            val mensaje = exportarCopiaSeguridadUseCase(uri).fold(
                onSuccess = { cantidad -> "Copia guardada: $cantidad senderos" },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo exportar la copia de seguridad", error)
                    MENSAJE_ERROR_COPIA
                }
            )
            _estadoVisual.update { estadoActual -> estadoActual.copy(estaProcesandoCopia = false) }
            canalEfectos.send(PerfilEfecto.MostrarAviso(mensaje))
        }
    }

    // RF4 - Exportar: se lee la copia y se pregunta antes de escribir (puede traer senderos ya borrados)
    private fun analizarCopia(uri: String) {
        if (_estadoVisual.value.estaProcesandoCopia) return
        _estadoVisual.update { estadoActual -> estadoActual.copy(estaProcesandoCopia = true) }
        viewModelScope.launch {
            importarCopiaSeguridadUseCase.analizar(uri).fold(
                onSuccess = { analisis ->
                    _estadoVisual.update { estadoActual -> estadoActual.copy(estaProcesandoCopia = false, copiaPendiente = analisis) }
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo leer la copia de seguridad", error)
                    _estadoVisual.update { estadoActual -> estadoActual.copy(estaProcesandoCopia = false) }
                    val mensaje = if (error is IllegalArgumentException || error is IllegalStateException) error.message else null
                    canalEfectos.send(PerfilEfecto.MostrarAviso(mensaje?.takeIf { texto -> texto.isNotBlank() } ?: MENSAJE_ERROR_COPIA))
                }
            )
        }
    }

    private fun restaurar(analisis: AnalisisCopiaSeguridad, modo: ModoRestauracion) {
        _estadoVisual.update { estadoActual -> estadoActual.copy(estaProcesandoCopia = true, copiaPendiente = null) }
        viewModelScope.launch {
            val total = analisis.nuevos.size + if (modo == ModoRestauracion.SOLO_NUEVOS) 0 else analisis.existentes.size
            val guardados = importarCopiaSeguridadUseCase.aplicar(analisis, modo) { sendero, error ->
                Log.e(TAG, "No se restauró el sendero ${sendero.id} de la copia", error)
            }
            Log.d(TAG, "Copia restaurada ($modo): $guardados de $total senderos")
            _estadoVisual.update { estadoActual -> estadoActual.copy(estaProcesandoCopia = false) }
            canalEfectos.send(PerfilEfecto.MostrarAviso("Se restauraron $guardados de $total senderos"))
        }
    }

    private fun observarModoDemostracion() {
        viewModelScope.launch {
            preferenciasAplicacion.esModoDemostracion.collect { activo ->
                _estadoVisual.update { estadoActual -> estadoActual.copy(esModoDemostracion = activo) }
            }
        }
    }

    // RF5 - Historial: observamos los recorridos realizados para listarlos en el perfil junto con las estadísticas
    private fun observarRecorridos() {
        observarRecorridosUseCase().fold(
            onSuccess = { flujoRecorridos ->
                viewModelScope.launch {
                    flujoRecorridos
                        .catch { error ->
                            Log.e(TAG, "Error al leer el historial de recorridos", error)
                            _estadoVisual.update { estadoActual -> estadoActual.copy(mensajeError = MENSAJE_ERROR_HISTORIAL) }
                        }
                        .collect { recorridos ->
                            _estadoVisual.update { estadoActual ->
                                estadoActual.copy(recorridos = recorridos, mensajeError = null)
                            }
                        }
                }
            },
            onFailure = { error ->
                Log.e(TAG, "No se pudo observar el historial de recorridos", error)
                _estadoVisual.update { estadoActual -> estadoActual.copy(mensajeError = MENSAJE_ERROR_HISTORIAL) }
            }
        )
    }

    private fun observarRecorridoEnCurso() {
        viewModelScope.launch {
            controlSeguimiento.estado.collect { estadoSeguimiento ->
                _estadoVisual.update { estadoActual ->
                    estadoActual.copy(hayRecorridoEnCurso = estadoSeguimiento != null)
                }
            }
        }
    }

    // RF1 - Sesión: al confirmar se cierra la sesión y se vuelve a la pantalla de inicio de sesión
    private fun cerrarSesion() {
        Log.d(TAG, "Cerrando la sesión")
        _estadoVisual.update { estadoActual -> estadoActual.copy(esDialogoCerrarSesionVisible = false) }
        cerrarSesionUseCase()
        viewModelScope.launch {
            canalEfectos.send(PerfilEfecto.NavegarALogin)
        }
    }
}
