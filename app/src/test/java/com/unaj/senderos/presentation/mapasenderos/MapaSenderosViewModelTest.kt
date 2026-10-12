package com.unaj.senderos.presentation.mapasenderos

import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoMonitorConectividad
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
class MapaSenderosViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val monitorConectividad = FalsoMonitorConectividad()

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

    // Crear el ViewModel del mapa con un usuario activo y el monitor de conectividad falso
    private fun crearViewModel(): MapaSenderosViewModel {
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val casoDeUso = ObservarSenderosDelUsuarioUseCase(FalsoSenderoRepositorio(), autenticacion)
        return MapaSenderosViewModel(casoDeUso, monitorConectividad)
    }

    // Verificar que el estado refleja la pérdida y la vuelta de la conexión
    @Test
    fun `el estado sigue la conexion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.estaSinConexion)

        monitorConectividad.hayConexionActual = false
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.estaSinConexion)

        monitorConectividad.hayConexionActual = true
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.estaSinConexion)
    }
}
