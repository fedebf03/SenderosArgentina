package com.unaj.senderos.presentation.mapasenderos

import app.cash.turbine.test
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.ObservarSenderosDelUsuarioUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapaSenderosViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana")
    private val recorrido = listOf(PuntoRecorrido(-41.1, -71.3), PuntoRecorrido(-41.2, -71.4))
    private val conRecorrido = Sendero("1", usuario.id, "Huella Andina", "Neuquén", 570.0, Dificultad.MEDIA, recorrido = recorrido)
    private val sinRecorrido = Sendero("2", usuario.id, "Huella Neuquina", "Neuquén", 120.0, Dificultad.BAJA)

    private lateinit var falsoAutenticacion: FalsoAutenticacionRepositorio
    private lateinit var falsoSendero: FalsoSenderoRepositorio

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        falsoSendero = FalsoSenderoRepositorio(listOf(conRecorrido, sinRecorrido))
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): MapaSenderosViewModel {
        return MapaSenderosViewModel(ObservarSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion))
    }

    @Test
    fun `al cargar ubica solo los senderos con recorrido`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals(listOf(conRecorrido), estado.senderosEnMapa)
    }

    @Test
    fun `un sendero nuevo con recorrido aparece en el mapa`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        val nuevo = Sendero("3", usuario.id, "Cerro Catedral", "Río Negro", 12.0, Dificultad.ALTA, recorrido = recorrido)

        falsoSendero.senderos.value = listOf(conRecorrido, sinRecorrido, nuevo)
        advanceUntilIdle()

        assertEquals(listOf(conRecorrido, nuevo), viewModel.estadoVisual.value.senderosEnMapa)
    }

    @Test
    fun `seleccionar un sendero lo expone y nulo lo deselecciona`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(MapaSenderosEvento.AlSeleccionarSendero("1"))
        assertEquals(conRecorrido, viewModel.estadoVisual.value.senderoSeleccionado)

        viewModel.procesarEvento(MapaSenderosEvento.AlSeleccionarSendero(null))
        assertNull(viewModel.estadoVisual.value.senderoSeleccionado)
    }

    @Test
    fun `si se elimina el seleccionado deja de estar seleccionado`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(MapaSenderosEvento.AlSeleccionarSendero("1"))

        falsoSendero.senderos.value = listOf(sinRecorrido)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.senderoSeleccionado)
    }

    @Test
    fun `AlAbrirDetalle emite NavegarADetalle con el id`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(MapaSenderosEvento.AlAbrirDetalle("1"))
            advanceUntilIdle()

            assertEquals(MapaSenderosEfecto.NavegarADetalle("1"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sin sesion activa expone mensajeError y deja de cargar`() = runTest {
        falsoAutenticacion.usuarioActual = null
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.estaCargando)
        assertEquals("No hay una sesión activa", viewModel.estadoVisual.value.mensajeError)
    }

    @Test
    fun `AlSolicitarUbicacion recuerda que ya se pidio el permiso`() = runTest {
        val viewModel = crearViewModel()
        assertFalse(viewModel.estadoVisual.value.seSolicitoUbicacion)

        viewModel.procesarEvento(MapaSenderosEvento.AlSolicitarUbicacion)

        assertTrue(viewModel.estadoVisual.value.seSolicitoUbicacion)
    }
}
