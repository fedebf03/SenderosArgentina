package com.unaj.senderos.presentation.perfil

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.repository.PreferenciasAplicacion
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.ObservarRecorridosUseCase
import com.unaj.senderos.domain.usecase.ObtenerCapacidadesDispositivoUseCase
import com.unaj.senderos.domain.usecase.ObtenerUsuarioActualUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PERFIL"

class PerfilViewModel(
    obtenerUsuarioActualUseCase: ObtenerUsuarioActualUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase,
    private val preferenciasAplicacion: PreferenciasAplicacion,
    private val observarRecorridosUseCase: ObservarRecorridosUseCase,
    obtenerCapacidadesDispositivoUseCase: ObtenerCapacidadesDispositivoUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(
        PerfilEstadoVisual(
            usuario = obtenerUsuarioActualUseCase(),
            funcionesDispositivo = listarFuncionesDispositivo(obtenerCapacidadesDispositivoUseCase())
        )
    )
    val estadoVisual: StateFlow<PerfilEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<PerfilEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    init {
        observarModoDemostracion()
        observarRecorridos()
    }

    // Procesar cada evento de la pantalla de perfil
    fun procesarEvento(evento: PerfilEvento) {
        when (evento) {
            is PerfilEvento.AlCambiarModoDemostracion -> {
                Log.d(TAG, "Modo demostración: ${evento.activo}")
                preferenciasAplicacion.cambiarModoDemostracion(evento.activo)
            }
            is PerfilEvento.AlSeleccionarRecorrido -> {
                Log.d(TAG, "Abriendo el recorrido ${evento.idRecorrido}")
                viewModelScope.launch { canalEfectos.send(PerfilEfecto.NavegarAHistorialDetalle(evento.idRecorrido)) }
            }
            is PerfilEvento.AlCerrarSesion -> cerrarSesion()
        }
    }

    // Reflejar en el estado visual el modo demostración guardado en el dispositivo
    private fun observarModoDemostracion() {
        viewModelScope.launch {
            preferenciasAplicacion.esModoDemostracion.collect { activo ->
                _estadoVisual.update { estadoActual -> estadoActual.copy(esModoDemostracion = activo) }
            }
        }
    }

    // Reflejar en el estado visual el historial de recorridos, del más reciente al más viejo
    private fun observarRecorridos() {
        observarRecorridosUseCase().fold(
            onSuccess = { flujoRecorridos ->
                viewModelScope.launch {
                    flujoRecorridos
                        .catch { error -> Log.e(TAG, "Error al leer el historial de recorridos", error) }
                        .collect { recorridos ->
                            _estadoVisual.update { estadoActual -> estadoActual.copy(recorridos = recorridos) }
                        }
                }
            },
            onFailure = { error -> Log.e(TAG, "No se pudo observar el historial de recorridos", error) }
        )
    }

    // Cerrar la sesión y pedir a la pantalla que vuelva al inicio de sesión
    private fun cerrarSesion() {
        Log.d(TAG, "Cerrando la sesión")
        cerrarSesionUseCase()
        viewModelScope.launch {
            canalEfectos.send(PerfilEfecto.NavegarALogin)
        }
    }
}
