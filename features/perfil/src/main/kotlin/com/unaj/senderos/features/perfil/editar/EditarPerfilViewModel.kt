package com.unaj.senderos.features.perfil.editar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.senderos.core.domain.port.AlmacenFotos
import com.unaj.senderos.core.domain.usecase.CambiarContrasenaUseCase
import com.unaj.senderos.core.domain.usecase.GuardarPerfilUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerPerfilUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.domain.usecase.PerfilInvalidoException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "EDITAR_PERFIL"
private const val MENSAJE_PERFIL_GUARDADO = "Perfil guardado"
private const val MENSAJE_PERFIL_PENDIENTE = "Perfil guardado en el dispositivo; se sube a tu cuenta cuando haya conexión"
private const val MENSAJE_ERROR_AL_GUARDAR = "No se pudo guardar el perfil. Probá de nuevo"
private const val MENSAJE_REVISAR_CAMPOS = "Revisá los datos marcados"
private const val MENSAJE_ERROR_FOTO = "No se pudo usar esa foto"
private const val MENSAJE_ERROR_AL_CARGAR = "No se pudo cargar tu perfil"
private const val MENSAJE_CONTRASENA_CAMBIADA = "Contraseña cambiada"
private const val MENSAJE_CLAVES_DISTINTAS = "Las contraseñas nuevas no coinciden"

@HiltViewModel
class EditarPerfilViewModel @Inject constructor(
    private val obtenerPerfilUseCase: ObtenerPerfilUseCase,
    private val guardarPerfilUseCase: GuardarPerfilUseCase,
    private val cambiarContrasenaUseCase: CambiarContrasenaUseCase,
    private val almacenFotos: AlmacenFotos,
    obtenerUsuarioActualUseCase: ObtenerUsuarioActualUseCase
) : ViewModel() {

    private val _estadoVisual = MutableStateFlow(
        EditarPerfilEstadoVisual(
            email = obtenerUsuarioActualUseCase()?.email.orEmpty(),
            puedeCambiarContrasena = cambiarContrasenaUseCase.estaDisponible()
        )
    )
    val estadoVisual: StateFlow<EditarPerfilEstadoVisual> = _estadoVisual.asStateFlow()

    private val canalEfectos = Channel<EditarPerfilEfecto>(Channel.BUFFERED)
    val efectoVisual = canalEfectos.receiveAsFlow()

    init {
        Log.d(TAG, "ViewModel → creado (${hashCode()})")
        cargarPerfil()
    }

    // Procesar cada evento de la pantalla
    fun procesarEvento(evento: EditarPerfilEvento) {
        when (evento) {
            is EditarPerfilEvento.AlCambiarTexto -> cambiarTexto(evento.campo, evento.texto)
            is EditarPerfilEvento.AlSeleccionarFoto -> seleccionarFoto(evento.uri)
            is EditarPerfilEvento.AlQuitarFoto -> {
                _estadoVisual.update { estado -> estado.copy(foto = "", hayCambiosSinGuardar = true) }
            }
            is EditarPerfilEvento.AlGuardar -> guardar()
            is EditarPerfilEvento.AlVolver -> volver()
            is EditarPerfilEvento.AlConfirmarDescartar -> {
                _estadoVisual.update { estado -> estado.copy(esDialogoDescartarVisible = false) }
                emitirEfecto(EditarPerfilEfecto.VolverAlPerfil)
            }
            is EditarPerfilEvento.AlCancelarDescartar -> {
                _estadoVisual.update { estado -> estado.copy(esDialogoDescartarVisible = false) }
            }
            is EditarPerfilEvento.AlCambiarClave -> cambiarClave(evento.campo, evento.texto)
            is EditarPerfilEvento.AlCambiarContrasena -> cambiarContrasena()
        }
    }

    // Cargar el perfil guardado en los campos del formulario
    private fun cargarPerfil() {
        viewModelScope.launch {
            val resultado = try {
                obtenerPerfilUseCase()
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Result.failure(error)
            }
            resultado.fold(
                onSuccess = { perfil ->
                    _estadoVisual.update { estado ->
                        estado.copy(
                            estaCargando = false,
                            nombre = perfil.nombre,
                            pais = perfil.pais,
                            provincia = perfil.provincia,
                            localidad = perfil.localidad,
                            foto = perfil.foto
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo cargar el perfil", error)
                    _estadoVisual.update { estado -> estado.copy(estaCargando = false) }
                    emitirEfecto(EditarPerfilEfecto.MostrarAviso(MENSAJE_ERROR_AL_CARGAR))
                }
            )
        }
    }

    // Actualizar el campo de texto y marcar que hay cambios; al escribir en un campo con error, el error se limpia
    private fun cambiarTexto(campo: CampoEdicionPerfil, texto: String) {
        _estadoVisual.update { estado ->
            val actualizado = when (campo) {
                CampoEdicionPerfil.NOMBRE -> estado.copy(nombre = texto)
                CampoEdicionPerfil.PAIS -> estado.copy(pais = texto)
                CampoEdicionPerfil.PROVINCIA -> estado.copy(provincia = texto)
                CampoEdicionPerfil.LOCALIDAD -> estado.copy(localidad = texto)
            }
            actualizado.copy(hayCambiosSinGuardar = true, camposConError = emptySet())
        }
    }

    // Copiar la foto elegida a la carpeta privada de la app, achicada, y usarla como foto del perfil
    private fun seleccionarFoto(uri: String) {
        if (_estadoVisual.value.estaProcesandoFoto) return
        _estadoVisual.update { estado -> estado.copy(estaProcesandoFoto = true) }
        viewModelScope.launch {
            val resultado = try {
                almacenFotos.guardarCopia(uri)
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Result.failure(error)
            }
            resultado.fold(
                onSuccess = { ruta ->
                    _estadoVisual.update { estado -> estado.copy(foto = ruta, estaProcesandoFoto = false, hayCambiosSinGuardar = true) }
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo copiar la foto del perfil", error)
                    _estadoVisual.update { estado -> estado.copy(estaProcesandoFoto = false) }
                    emitirEfecto(EditarPerfilEfecto.MostrarAviso(MENSAJE_ERROR_FOTO))
                }
            )
        }
    }

    // Validar y guardar el perfil; el aviso final dice si ya llegó a la cuenta o espera conexión
    private fun guardar() {
        val estadoActual = _estadoVisual.value
        if (!estadoActual.puedeGuardar) return
        _estadoVisual.update { estado -> estado.copy(estaGuardando = true, camposConError = emptySet()) }
        viewModelScope.launch {
            val resultado = try {
                guardarPerfilUseCase(estadoActual.aPerfil())
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Result.failure(error)
            }
            resultado.fold(
                onSuccess = { guardado ->
                    Log.d(TAG, "Perfil guardado (en la nube: ${guardado.estaEnLaNube})")
                    _estadoVisual.update { estado -> estado.copy(estaGuardando = false, hayCambiosSinGuardar = false) }
                    emitirEfecto(
                        EditarPerfilEfecto.PerfilGuardado(if (guardado.estaEnLaNube) MENSAJE_PERFIL_GUARDADO else MENSAJE_PERFIL_PENDIENTE)
                    )
                },
                onFailure = { error ->
                    if (error is PerfilInvalidoException) {
                        _estadoVisual.update { estado -> estado.copy(estaGuardando = false, camposConError = error.camposInvalidos) }
                        emitirEfecto(EditarPerfilEfecto.MostrarAviso(MENSAJE_REVISAR_CAMPOS))
                    } else {
                        Log.e(TAG, "Error al guardar el perfil", error)
                        _estadoVisual.update { estado -> estado.copy(estaGuardando = false) }
                        emitirEfecto(EditarPerfilEfecto.MostrarAviso(MENSAJE_ERROR_AL_GUARDAR))
                    }
                }
            )
        }
    }

    // Volver al perfil; con cambios sin guardar pide confirmar antes de descartarlos
    private fun volver() {
        if (_estadoVisual.value.hayCambiosSinGuardar) {
            _estadoVisual.update { estado -> estado.copy(esDialogoDescartarVisible = true) }
        } else {
            emitirEfecto(EditarPerfilEfecto.VolverAlPerfil)
        }
    }

    // Guardar lo escrito en uno de los campos de contraseña y limpiar el mensaje de error anterior
    private fun cambiarClave(campo: CampoContrasena, texto: String) {
        _estadoVisual.update { estado ->
            val actualizado = when (campo) {
                CampoContrasena.ACTUAL -> estado.copy(claveActual = texto)
                CampoContrasena.NUEVA -> estado.copy(claveNueva = texto)
                CampoContrasena.REPETIDA -> estado.copy(claveRepetida = texto)
            }
            actualizado.copy(mensajeErrorContrasena = null)
        }
    }

    // Cambiar la contraseña con la actual como confirmación; al terminar bien se vacían los campos
    private fun cambiarContrasena() {
        val estadoActual = _estadoVisual.value
        if (estadoActual.estaCambiandoContrasena) return
        if (estadoActual.claveNueva != estadoActual.claveRepetida) {
            _estadoVisual.update { estado -> estado.copy(mensajeErrorContrasena = MENSAJE_CLAVES_DISTINTAS) }
            return
        }
        _estadoVisual.update { estado -> estado.copy(estaCambiandoContrasena = true, mensajeErrorContrasena = null) }
        viewModelScope.launch {
            val resultado = try {
                cambiarContrasenaUseCase(estadoActual.claveActual, estadoActual.claveNueva)
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Result.failure(error)
            }
            resultado.fold(
                onSuccess = {
                    Log.d(TAG, "Contraseña cambiada")
                    _estadoVisual.update { estado ->
                        estado.copy(estaCambiandoContrasena = false, claveActual = "", claveNueva = "", claveRepetida = "")
                    }
                    emitirEfecto(EditarPerfilEfecto.MostrarAviso(MENSAJE_CONTRASENA_CAMBIADA))
                },
                onFailure = { error ->
                    Log.e(TAG, "No se pudo cambiar la contraseña", error)
                    _estadoVisual.update { estado ->
                        estado.copy(estaCambiandoContrasena = false, mensajeErrorContrasena = error.message ?: "No se pudo cambiar la contraseña")
                    }
                }
            )
        }
    }

    // Enviar el efecto a la pantalla sin bloquear
    private fun emitirEfecto(efecto: EditarPerfilEfecto) {
        canalEfectos.trySend(efecto)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel → onCleared")
    }
}
