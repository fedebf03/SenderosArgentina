package com.unaj.senderos.features.perfil.historial

import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.domain.usecase.EliminarRecorridoUseCase
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarRecorridoUseCase
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearRecorridoDePrueba
import com.unaj.senderos.core.testing.crearSenderoDePrueba
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialDetalleViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val repositorioRecorridos = FalsoRecorridoRepositorio()
    private val efectosRecibidos = mutableListOf<HistorialDetalleEfecto>()

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(idRecorrido: String, idsSenderosExistentes: List<String>): HistorialDetalleViewModel {
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val repositorioSenderos = FalsoSenderoRepositorio(
            idsSenderosExistentes.map { id -> crearSenderoDePrueba(id = id) }
        )
        val viewModel = HistorialDetalleViewModel(
            idRecorrido = idRecorrido,
            observarRecorridoUseCase = ObservarRecorridoUseCase(repositorioRecorridos, autenticacion),
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(repositorioSenderos, autenticacion),
            eliminarRecorridoUseCase = EliminarRecorridoUseCase(repositorioRecorridos, FalsoPuntosCapturadosRepositorio(), autenticacion)
        )
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
    }

    @Test
    fun `carga el recorrido y el sendero planificado`() = runTest(despachadorPrueba) {
        repositorioRecorridos.recorridos.value = listOf(crearRecorridoDePrueba(id = "r1", idSendero = "sendero-1"))
        val viewModel = crearViewModel("r1", listOf("sendero-1"))
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertNull(estado.mensajeError)
        assertEquals("r1", estado.recorrido?.id)
        assertNotNull(estado.sendero)
    }

    @Test
    fun `si el sendero no existe queda sin sendero planificado`() = runTest(despachadorPrueba) {
        repositorioRecorridos.recorridos.value = listOf(crearRecorridoDePrueba(id = "r1", idSendero = "borrado"))
        val viewModel = crearViewModel("r1", listOf("sendero-1"))
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertEquals("r1", estado.recorrido?.id)
        assertNull(estado.sendero)
        assertNull(estado.mensajeError)
    }

    @Test
    fun `un recorrido inexistente informa que no se encontro`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel("no-existe", listOf("sendero-1"))
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertNull(estado.recorrido)
        assertEquals("No se encontró el recorrido", estado.mensajeError)
    }

    @Test
    fun `volver emite el efecto de regreso al perfil`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel("r1", emptyList())
        advanceUntilIdle()

        viewModel.procesarEvento(HistorialDetalleEvento.AlVolver)
        advanceUntilIdle()

        assertEquals(listOf<HistorialDetalleEfecto>(HistorialDetalleEfecto.VolverAlPerfil), efectosRecibidos)
    }
}
