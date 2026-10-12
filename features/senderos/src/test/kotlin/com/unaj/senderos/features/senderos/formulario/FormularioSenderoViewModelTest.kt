package com.unaj.senderos.presentation.formulario

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.falsos.FalsoAlmacenFotos
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearSenderoDePrueba
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
class FormularioSenderoViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))

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

    // Crear el ViewModel del formulario con repositorios falsos
    private fun crearViewModel(idSendero: String?, repositorio: FalsoSenderoRepositorio): FormularioSenderoViewModel {
        return FormularioSenderoViewModel(
            idSendero,
            ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            GuardarSenderoUseCase(repositorio, autenticacion),
            FalsoAlmacenFotos()
        )
    }

    // Verificar que la distancia copiada del mapa se escribe con coma decimal
    @Test
    fun `la distancia copiada del mapa se escribe con coma`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(null, FalsoSenderoRepositorio())
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.1))

        viewModel.procesarEvento(FormularioSenderoEvento.AlUsarDistanciaDelMapa)
        advanceUntilIdle()

        assertEquals("11,1", viewModel.estadoVisual.value.distancia)
    }

    // Verificar que la distancia de un sendero en edición se muestra con coma decimal
    @Test
    fun `la distancia de un sendero en edicion se muestra con coma`() = runTest(despachadorPrueba) {
        val repositorio = FalsoSenderoRepositorio(listOf(crearSenderoDePrueba(distanciaKilometros = 12.5)))
        val viewModel = crearViewModel("sendero-1", repositorio)

        advanceUntilIdle()

        assertEquals("12,5", viewModel.estadoVisual.value.distancia)
    }
}
