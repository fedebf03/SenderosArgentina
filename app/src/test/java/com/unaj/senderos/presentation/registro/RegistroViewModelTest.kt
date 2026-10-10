package com.unaj.senderos.presentation.registro

import app.cash.turbine.test
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.RegistrarUsuarioUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegistroViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana")

    private lateinit var falsoAutenticacion: FalsoAutenticacionRepositorio

    // Configurar el despachador principal de prueba y el falso antes de cada caso
    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
    }

    // Restaurar el despachador principal original después de cada caso
    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel con el caso de uso armado sobre el repositorio falso
    private fun crearViewModel(): RegistroViewModel {
        return RegistroViewModel(RegistrarUsuarioUseCase(falsoAutenticacion))
    }

    // Completar el formulario con datos válidos y visibilidad de clave activada
    private fun completarFormulario(viewModel: RegistroViewModel) {
        viewModel.procesarEvento(RegistroEvento.AlCambiarEmail("ana@correo.com"))
        viewModel.procesarEvento(RegistroEvento.AlCambiarClave("secreta123"))
        viewModel.procesarEvento(RegistroEvento.AlCambiarConfirmacion("secreta123"))
        viewModel.procesarEvento(RegistroEvento.AlAlternarVisibilidadClave)
    }

    // Probar que un registro exitoso limpia el formulario y emite la navegación
    @Test
    fun `exito limpia todos los campos y emite NavegarAInicio`() = runTest {
        val viewModel = crearViewModel()
        completarFormulario(viewModel)

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(RegistroEvento.AlRegistrarse)
            advanceUntilIdle()

            assertEquals(RegistroEfecto.NavegarAInicio, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(RegistroEstadoVisual(), viewModel.estadoVisual.value)
    }

    // Probar que un fallo conserva lo escrito y expone el mensaje de error
    @Test
    fun `fallo conserva los campos y muestra mensajeError`() = runTest {
        falsoAutenticacion.usuarioActual = null
        val viewModel = crearViewModel()
        completarFormulario(viewModel)

        viewModel.procesarEvento(RegistroEvento.AlRegistrarse)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertEquals("ana@correo.com", estado.email)
        assertEquals("secreta123", estado.clave)
        assertEquals("secreta123", estado.confirmacion)
        assertEquals(true, estado.esClaveVisible)
        assertEquals(false, estado.estaCargando)
        assertEquals("Sin usuario configurado", estado.mensajeError)
    }
}
