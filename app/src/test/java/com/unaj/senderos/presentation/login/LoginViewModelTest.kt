package com.unaj.senderos.presentation.login

import com.unaj.senderos.domain.usecase.EnviarCorreoRecuperacionUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionConGoogleUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val autenticacion = FalsoAutenticacionRepositorio()

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): LoginViewModel {
        return LoginViewModel(
            IniciarSesionUseCase(autenticacion),
            IniciarSesionConGoogleUseCase(autenticacion),
            EnviarCorreoRecuperacionUseCase(autenticacion)
        )
    }

    @Test
    fun `olvidar la clave abre el dialogo con el correo precargado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(LoginEvento.AlCambiarEmail("ana@correo.com"))

        viewModel.procesarEvento(LoginEvento.AlOlvidarClave)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.esDialogoRecuperacionVisible)
        assertEquals("ana@correo.com", estado.emailRecuperacion)
    }

    // El aviso no revela si el correo existe
    @Test
    fun `enviar el enlace con exito cierra el dialogo y avisa`() = runTest(despachadorPrueba) {
        val efectos = mutableListOf<LoginEfecto>()
        val viewModel = crearViewModel()
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectos.add(efecto) } }
        viewModel.procesarEvento(LoginEvento.AlCambiarEmail("ana@correo.com"))
        viewModel.procesarEvento(LoginEvento.AlOlvidarClave)

        viewModel.procesarEvento(LoginEvento.AlEnviarEnlaceRecuperacion)
        advanceUntilIdle()

        assertEquals(listOf("ana@correo.com"), autenticacion.correosDeRecuperacion)
        assertFalse(viewModel.estadoVisual.value.esDialogoRecuperacionVisible)
        assertFalse(viewModel.estadoVisual.value.estaEnviandoRecuperacion)
        assertEquals(
            listOf<LoginEfecto>(
                LoginEfecto.MostrarAviso("Si el correo está registrado, te enviamos un enlace para restablecer la contraseña")
            ),
            efectos
        )
    }

    @Test
    fun `se envia el correo editado en el dialogo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(LoginEvento.AlOlvidarClave)
        viewModel.procesarEvento(LoginEvento.AlCambiarEmailRecuperacion("otra@correo.com"))

        viewModel.procesarEvento(LoginEvento.AlEnviarEnlaceRecuperacion)
        advanceUntilIdle()

        assertEquals(listOf("otra@correo.com"), autenticacion.correosDeRecuperacion)
    }

    @Test
    fun `un correo invalido muestra el mensaje del dominio y mantiene el dialogo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(LoginEvento.AlOlvidarClave)
        viewModel.procesarEvento(LoginEvento.AlCambiarEmailRecuperacion("sin-arroba"))

        viewModel.procesarEvento(LoginEvento.AlEnviarEnlaceRecuperacion)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.esDialogoRecuperacionVisible)
        assertEquals("El correo no tiene un formato válido", estado.mensajeErrorRecuperacion)
        assertFalse(estado.estaEnviandoRecuperacion)
        assertTrue(autenticacion.correosDeRecuperacion.isEmpty())
    }

    @Test
    fun `un fallo del repositorio se muestra en el dialogo`() = runTest(despachadorPrueba) {
        autenticacion.resultadoRecuperacion = Result.failure(Exception("Sin conexión a internet"))
        val viewModel = crearViewModel()
        viewModel.procesarEvento(LoginEvento.AlCambiarEmail("ana@correo.com"))
        viewModel.procesarEvento(LoginEvento.AlOlvidarClave)

        viewModel.procesarEvento(LoginEvento.AlEnviarEnlaceRecuperacion)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esDialogoRecuperacionVisible)
        assertEquals("Sin conexión a internet", viewModel.estadoVisual.value.mensajeErrorRecuperacion)
    }

    @Test
    fun `cerrar el dialogo limpia el error`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(LoginEvento.AlOlvidarClave)
        viewModel.procesarEvento(LoginEvento.AlCambiarEmailRecuperacion("sin-arroba"))
        viewModel.procesarEvento(LoginEvento.AlEnviarEnlaceRecuperacion)
        advanceUntilIdle()
        assertNotNull(viewModel.estadoVisual.value.mensajeErrorRecuperacion)

        viewModel.procesarEvento(LoginEvento.AlCerrarDialogoRecuperacion)

        assertFalse(viewModel.estadoVisual.value.esDialogoRecuperacionVisible)
        assertNull(viewModel.estadoVisual.value.mensajeErrorRecuperacion)
    }

    @Test
    fun `cerrar el dialogo durante el envio cancela el envio y no cierra el dialogo reabierto`() = runTest(despachadorPrueba) {
        val efectos = mutableListOf<LoginEfecto>()
        val viewModel = crearViewModel()
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectos.add(efecto) } }
        viewModel.procesarEvento(LoginEvento.AlCambiarEmail("ana@correo.com"))
        viewModel.procesarEvento(LoginEvento.AlOlvidarClave)
        viewModel.procesarEvento(LoginEvento.AlEnviarEnlaceRecuperacion)

        viewModel.procesarEvento(LoginEvento.AlCerrarDialogoRecuperacion)
        viewModel.procesarEvento(LoginEvento.AlOlvidarClave)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esDialogoRecuperacionVisible)
        assertFalse(viewModel.estadoVisual.value.estaEnviandoRecuperacion)
        assertTrue(efectos.isEmpty())
    }

    @Test
    fun `cerrar y reabrir durante el envio no permite enviar dos correos`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(LoginEvento.AlCambiarEmail("ana@correo.com"))
        viewModel.procesarEvento(LoginEvento.AlOlvidarClave)
        viewModel.procesarEvento(LoginEvento.AlEnviarEnlaceRecuperacion)
        viewModel.procesarEvento(LoginEvento.AlCerrarDialogoRecuperacion)
        viewModel.procesarEvento(LoginEvento.AlOlvidarClave)

        viewModel.procesarEvento(LoginEvento.AlEnviarEnlaceRecuperacion)
        advanceUntilIdle()

        assertEquals(listOf("ana@correo.com"), autenticacion.correosDeRecuperacion)
        assertFalse(viewModel.estadoVisual.value.esDialogoRecuperacionVisible)
    }
}
