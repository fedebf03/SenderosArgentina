package com.unaj.senderos.presentation.perfil

import app.cash.turbine.test
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
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
class PerfilViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana")

    private lateinit var falsoAutenticacion: FalsoAutenticacionRepositorio
    private lateinit var falsoSendero: FalsoSenderoRepositorio

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        falsoSendero = FalsoSenderoRepositorio()
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): PerfilViewModel {
        return PerfilViewModel(
            obtenerUsuarioActualUseCase = ObtenerUsuarioActualUseCase(falsoAutenticacion),
            cerrarSesionUseCase = CerrarSesionUseCase(falsoAutenticacion, falsoSendero)
        )
    }

    @Test
    fun `expone el usuario de la sesion actual`() {
        val viewModel = crearViewModel()

        assertEquals(usuario, viewModel.estadoVisual.value.usuario)
    }

    @Test
    fun `AlCerrarSesion cierra la sesion y emite NavegarALogin`() = runTest {
        val viewModel = crearViewModel()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(PerfilEvento.AlCerrarSesion)
            advanceUntilIdle()

            assertEquals(PerfilEfecto.NavegarALogin, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, falsoAutenticacion.cantidadCierresSesion)
    }
}
