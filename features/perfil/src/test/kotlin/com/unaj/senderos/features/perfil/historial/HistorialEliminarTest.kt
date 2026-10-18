package com.unaj.senderos.features.historial

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.EliminarRecorridoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarRecorridoUseCase
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearRecorridoDePrueba
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

// Eliminar un recorrido desde su detalle: pide confirmación, lo borra y vuelve al perfil
@OptIn(ExperimentalCoroutinesApi::class)
class HistorialEliminarTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val recorridos = FalsoRecorridoRepositorio()
    private val efectosRecibidos = mutableListOf<HistorialDetalleEfecto>()

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
        recorridos.recorridos.value = listOf(crearRecorridoDePrueba(id = "r1", idSendero = "sendero-1"))
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel del detalle del recorrido r1 con el caso de uso de eliminar conectado
    private fun crearViewModel(): HistorialDetalleViewModel {
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val viewModel = HistorialDetalleViewModel(
            idRecorrido = "r1",
            observarRecorridoUseCase = ObservarRecorridoUseCase(recorridos, autenticacion),
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(FalsoSenderoRepositorio(), autenticacion),
            eliminarRecorridoUseCase = EliminarRecorridoUseCase(recorridos, FalsoPuntosCapturadosRepositorio(), autenticacion)
        )
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) } }
        return viewModel
    }

    @Test
    fun `tocar eliminar pide confirmar antes de borrar nada`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(HistorialDetalleEvento.AlTocarEliminar)

        assertTrue(viewModel.estadoVisual.value.esDialogoEliminarVisible)
        assertTrue(recorridos.eliminados.isEmpty())
    }

    @Test
    fun `cancelar la confirmacion cierra el dialogo y no borra`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(HistorialDetalleEvento.AlTocarEliminar)

        viewModel.procesarEvento(HistorialDetalleEvento.AlCancelarEliminar)

        assertFalse(viewModel.estadoVisual.value.esDialogoEliminarVisible)
        assertTrue(recorridos.eliminados.isEmpty())
    }

    @Test
    fun `confirmar elimina el recorrido y vuelve al perfil`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(HistorialDetalleEvento.AlTocarEliminar)

        viewModel.procesarEvento(HistorialDetalleEvento.AlConfirmarEliminar)
        advanceUntilIdle()

        assertEquals(listOf("usuario-1" to "r1"), recorridos.eliminados)
        assertTrue(efectosRecibidos.contains(HistorialDetalleEfecto.VolverAlPerfil))
        assertFalse(viewModel.estadoVisual.value.esDialogoEliminarVisible)
    }

    @Test
    fun `si no se puede eliminar se queda en la pantalla y muestra el error`() = runTest(despachadorPrueba) {
        recorridos.resultadoEliminar = Result.failure(IllegalStateException("Disco lleno"))
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(HistorialDetalleEvento.AlTocarEliminar)

        viewModel.procesarEvento(HistorialDetalleEvento.AlConfirmarEliminar)
        advanceUntilIdle()

        assertTrue(efectosRecibidos.none { efecto -> efecto is HistorialDetalleEfecto.VolverAlPerfil })
        assertNotNull(viewModel.estadoVisual.value.errorAlEliminar)
        assertFalse(viewModel.estadoVisual.value.estaEliminando)
        assertNotNull(viewModel.estadoVisual.value.recorrido)
    }

    @Test
    fun `un doble toque en confirmar elimina una sola vez`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(HistorialDetalleEvento.AlTocarEliminar)

        viewModel.procesarEvento(HistorialDetalleEvento.AlConfirmarEliminar)
        viewModel.procesarEvento(HistorialDetalleEvento.AlConfirmarEliminar)
        advanceUntilIdle()

        assertEquals(1, recorridos.eliminados.size)
    }

    @Test
    fun `volver a abrir el dialogo despues de un error limpia el mensaje anterior`() = runTest(despachadorPrueba) {
        recorridos.resultadoEliminar = Result.failure(IllegalStateException("Disco lleno"))
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.procesarEvento(HistorialDetalleEvento.AlTocarEliminar)
        viewModel.procesarEvento(HistorialDetalleEvento.AlConfirmarEliminar)
        advanceUntilIdle()

        viewModel.procesarEvento(HistorialDetalleEvento.AlTocarEliminar)

        assertNull(viewModel.estadoVisual.value.errorAlEliminar)
    }
}
