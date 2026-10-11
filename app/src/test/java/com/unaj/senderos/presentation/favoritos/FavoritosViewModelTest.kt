package com.unaj.senderos.presentation.favoritos

import app.cash.turbine.test
import com.unaj.senderos.domain.model.Dificultad
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritosViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana")
    private val andinaFavorita = Sendero("1", usuario.id, "Huella Andina", "Mendoza", 12.5, Dificultad.ALTA, esFavorito = true)
    private val neuquina = Sendero("2", usuario.id, "Huella Neuquina", "Neuquén", 8.0, Dificultad.BAJA)

    private lateinit var falsoAutenticacion: FalsoAutenticacionRepositorio
    private lateinit var falsoSendero: FalsoSenderoRepositorio

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        falsoAutenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
        falsoSendero = FalsoSenderoRepositorio(listOf(andinaFavorita, neuquina))
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): FavoritosViewModel {
        return FavoritosViewModel(ObservarSenderosDelUsuarioUseCase(falsoSendero, falsoAutenticacion))
    }

    @Test
    fun `al cargar muestra solo los senderos favoritos`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals(listOf(andinaFavorita), estado.favoritosFiltrados)
    }

    @Test
    fun `la busqueda no devuelve senderos que no son favoritos`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(FavoritosEvento.AlCambiarBusqueda("Neuqu"))
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.favoritosFiltrados.isEmpty())
    }

    @Test
    fun `la lista se actualiza al marcar un nuevo favorito`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        val neuquinaFavorita = neuquina.copy(esFavorito = true)

        falsoSendero.senderos.value = listOf(andinaFavorita, neuquinaFavorita)
        advanceUntilIdle()

        assertEquals(listOf(andinaFavorita, neuquinaFavorita), viewModel.estadoVisual.value.favoritosFiltrados)
    }

    @Test
    fun `sin sesion activa expone mensajeError y deja de cargar`() = runTest {
        falsoAutenticacion.usuarioActual = null
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals("No hay una sesión activa", estado.mensajeError)
    }

    @Test
    fun `AlSeleccionarSendero emite NavegarADetalle con el id`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.efectoVisual.test {
            viewModel.procesarEvento(FavoritosEvento.AlSeleccionarSendero("1"))
            advanceUntilIdle()

            assertEquals(FavoritosEfecto.NavegarADetalle("1"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
