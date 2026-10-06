package com.unaj.senderos.presentation.explorar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.ObtenerUsuarioActualUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "EXPLORAR"

class ExplorarViewModel(
    private val obtenerUsuarioActualUseCase: ObtenerUsuarioActualUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(ExplorarEstadoVisual())
    val estadoVisual: StateFlow<ExplorarEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<ExplorarEfecto>()
    val efectoVisual = canalEfectos.receiveAsFlow()

    init {
        Log.d("VIDA", "ExplorarViewModel → creado (${hashCode()})")
        cargarDatosUsuario()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("VIDA", "ExplorarViewModel → onCleared")
    }

    // Procesar los eventos emitidos desde la interfaz de usuario
    fun procesarEvento(evento: ExplorarEvento) {
        when (evento) {
            is ExplorarEvento.AlCerrarSesion -> ejecutarCierreSesion()
            is ExplorarEvento.AlSeleccionarTab -> seleccionarTab(evento.indice)
        }
    }

    // Cargar la información del usuario autenticado actualmente
    fun cargarDatosUsuario() {
        Log.d(TAG, "Cargando información del usuario actual")
        val usuario = obtenerUsuarioActualUseCase()
        _estadoVisual.update { it.copy(usuario = usuario) }
        Log.d(TAG, "Usuario cargado: ${usuario?.email}, nombre: ${usuario?.nombre}")
    }

    // Actualizar la pestaña seleccionada en la barra de navegación inferior
    private fun seleccionarTab(indice: Int) {
        _estadoVisual.update { it.copy(tabSeleccionado = indice) }
    }

    // Ejecutar el cierre de sesión y notificar a la vista para navegar al login
    private fun ejecutarCierreSesion() {
        Log.d(TAG, "Ejecutando cierre de sesión")
        cerrarSesionUseCase()
        _estadoVisual.update { it.copy(usuario = null) }
        viewModelScope.launch {
            canalEfectos.send(ExplorarEfecto.NavegarALogin)
        }
    }
}
