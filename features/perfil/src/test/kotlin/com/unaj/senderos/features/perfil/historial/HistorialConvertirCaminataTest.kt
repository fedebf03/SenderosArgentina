package com.unaj.senderos.features.historial

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarRecorridoUseCase
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Convertir en sendero nuevo una caminata libre ya guardada, desde el detalle del historial
@OptIn(ExperimentalCoroutinesApi::class)
class HistorialConvertirCaminataTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val recorridos = FalsoRecorridoRepositorio()
    private val efectosRecibidos = mutableListOf<HistorialDetalleEfecto>()

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel del detalle de un recorrido y juntar sus efectos
    private fun crearViewModel(idRecorrido: String): HistorialDetalleViewModel {
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val viewModel = HistorialDetalleViewModel(
            idRecorrido = idRecorrido,
            observarRecorridoUseCase = ObservarRecorridoUseCase(recorridos, autenticacion),
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(FalsoSenderoRepositorio(), autenticacion)
        )
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) } }
        return viewModel
    }

    // Guardar un recorrido con la distancia indicada sobre el identificador de sendero dado
    private fun guardarRecorrido(idSendero: String, kilometros: Double) {
        recorridos.recorridos.value = listOf(
            crearRecorridoDePrueba(id = "r1", idSendero = idSendero).copy(distanciaKilometros = kilometros)
        )
    }

    @Test
    fun `una caminata libre de mas de 50 metros se puede convertir en sendero`() = runTest(despachadorPrueba) {
        guardarRecorrido("libre-1000", 1.2)
        val viewModel = crearViewModel("r1")
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.sePuedeConvertirEnSendero)
    }

    @Test
    fun `una caminata libre muy corta no ofrece la conversion`() = runTest(despachadorPrueba) {
        guardarRecorrido("libre-1000", 0.02)
        val viewModel = crearViewModel("r1")
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.sePuedeConvertirEnSendero)
    }

    @Test
    fun `un recorrido sobre un sendero no ofrece la conversion`() = runTest(despachadorPrueba) {
        guardarRecorrido("sendero-1", 8.0)
        val viewModel = crearViewModel("r1")
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.sePuedeConvertirEnSendero)
    }

    @Test
    fun `convertir pide abrir el formulario con el identificador de la caminata`() = runTest(despachadorPrueba) {
        guardarRecorrido("libre-1000", 1.2)
        val viewModel = crearViewModel("r1")
        advanceUntilIdle()

        viewModel.procesarEvento(HistorialDetalleEvento.AlConvertirEnSendero)
        advanceUntilIdle()

        assertTrue(efectosRecibidos.contains(HistorialDetalleEfecto.NavegarAFormularioDesdeCaminata("libre-1000")))
    }

    @Test
    fun `convertir un recorrido que no es una caminata libre no hace nada`() = runTest(despachadorPrueba) {
        guardarRecorrido("sendero-1", 8.0)
        val viewModel = crearViewModel("r1")
        advanceUntilIdle()

        viewModel.procesarEvento(HistorialDetalleEvento.AlConvertirEnSendero)
        advanceUntilIdle()

        assertTrue(efectosRecibidos.none { efecto -> efecto is HistorialDetalleEfecto.NavegarAFormularioDesdeCaminata })
    }
}
