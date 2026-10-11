package com.unaj.senderos.presentation.login

import app.cash.turbine.test
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.IniciarSesionConGoogleUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionUseCase
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
class LoginViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana")

    private lateinit var falsoAutenticacion: FalsoAutenticacionRepositorio

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): LoginViewModel {
        return LoginViewModel(
            iniciarSesionUseCase = IniciarSesionUseCase(falsoAutenticacion),
            iniciarSesionConGoogleUseCase = IniciarSesionConGoogleUseCase(falsoAutenticacion)
        )
    }

    @Test
    fun `exito con correo limpia el formulario y emite NavegarAInicio`() = runTest {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(LoginEvento.AlCambiarEmail("ana@correo.com"))
        viewModel.procesarEvento(LoginEvento.AlCambiarClave("secreta123"))
        viewModel.procesarEvento(LoginEvento.AlAlternarVisibilidadClave)

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(LoginEvento.AlIniciarSesion)
            advanceUntilIdle()

            assertEquals(LoginEfecto.NavegarAInicio, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(LoginEstadoVisual(), viewModel.estadoVisual.value)
    }

    @Test
    fun `exito con Google limpia el formulario y emite NavegarAInicio`() = runTest {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(LoginEvento.AlCambiarEmail("ana@correo.com"))
        viewModel.procesarEvento(LoginEvento.AlCambiarClave("secreta123"))

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(LoginEvento.AlContinuarConGoogle("token-google"))
            advanceUntilIdle()

            assertEquals(LoginEfecto.NavegarAInicio, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(LoginEstadoVisual(), viewModel.estadoVisual.value)
    }

    @Test
    fun `fallo conserva correo y clave y muestra mensajeError`() = runTest {
        falsoAutenticacion.usuarioActual = null
        val viewModel = crearViewModel()
        viewModel.procesarEvento(LoginEvento.AlCambiarEmail("ana@correo.com"))
        viewModel.procesarEvento(LoginEvento.AlCambiarClave("secreta123"))

        viewModel.procesarEvento(LoginEvento.AlIniciarSesion)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertEquals("ana@correo.com", estado.email)
        assertEquals("secreta123", estado.clave)
        assertEquals(false, estado.estaCargando)
        assertEquals("Sin usuario configurado", estado.mensajeError)
    }
}
