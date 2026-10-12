package com.unaj.senderos.presentation.perfil

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoLanzadorSeguimiento
import com.unaj.senderos.falsos.FalsoPreferenciasAplicacion
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val preferencias = FalsoPreferenciasAplicacion()

    // Configurar el despachador principal de las corrutinas de prueba
    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    // Restaurar el despachador principal original
    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel del perfil con un usuario activo y las preferencias falsas
    private fun crearViewModel(): PerfilViewModel {
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        return PerfilViewModel(
            obtenerUsuarioActualUseCase = ObtenerUsuarioActualUseCase(autenticacion),
            cerrarSesionUseCase = CerrarSesionUseCase(autenticacion, FalsoSenderoRepositorio(), FalsoLanzadorSeguimiento()),
            preferenciasAplicacion = preferencias
        )
    }

    // Verificar que el modo demostración arranca desactivado
    @Test
    fun `el modo demostracion arranca desactivado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esModoDemostracion)
    }

    // Verificar que el interruptor guarda la preferencia y la refleja en el estado visual
    @Test
    fun `activar el modo demostracion lo guarda y lo refleja`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(PerfilEvento.AlCambiarModoDemostracion(activo = true))
        advanceUntilIdle()

        assertTrue(preferencias.esModoDemostracion.value)
        assertTrue(viewModel.estadoVisual.value.esModoDemostracion)
    }

    // Verificar que desactivar el modo lo vuelve a apagar
    @Test
    fun `desactivar el modo demostracion lo apaga`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(PerfilEvento.AlCambiarModoDemostracion(activo = true))
        advanceUntilIdle()

        viewModel.procesarEvento(PerfilEvento.AlCambiarModoDemostracion(activo = false))
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esModoDemostracion)
    }
}
