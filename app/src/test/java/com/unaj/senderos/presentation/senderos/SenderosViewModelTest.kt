package com.unaj.senderos.presentation.senderos

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoMonitorConectividad
import com.unaj.senderos.falsos.FalsoProveedorUbicacion
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class SenderosViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val proveedorUbicacion = FalsoProveedorUbicacion()
    private val monitorConectividad = FalsoMonitorConectividad()

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): SenderosViewModel {
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val casoDeUso = ObservarSenderosDelUsuarioUseCase(FalsoSenderoRepositorio(), autenticacion)
        return SenderosViewModel(casoDeUso, proveedorUbicacion, monitorConectividad)
    }

    @Test
    fun `al obtener la ubicacion con exito la guarda en el estado`() = runTest(despachadorPrueba) {
        val ubicacion = Coordenada(latitud = -34.6, longitud = -58.4)
        proveedorUbicacion.resultado = Result.success(ubicacion)
        val viewModel = crearViewModel()

        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(ubicacion, viewModel.estadoVisual.value.ubicacionActual)
        assertEquals(1, proveedorUbicacion.cantidadConsultas)
    }

    @Test
    fun `al obtener una ubicacion nula el estado queda sin ubicacion`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.success(null)
        val viewModel = crearViewModel()

        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.ubicacionActual)
        assertEquals(1, proveedorUbicacion.cantidadConsultas)
    }

    @Test
    fun `al fallar la ubicacion no se rompe y el estado queda sin ubicacion`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.failure(IllegalStateException("GPS apagado"))
        val viewModel = crearViewModel()

        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.ubicacionActual)
        assertEquals(1, proveedorUbicacion.cantidadConsultas)
    }

    @Test
    fun `no consulta la ubicacion antes de recibir el evento`() = runTest(despachadorPrueba) {
        crearViewModel()
        advanceUntilIdle()

        assertEquals(0, proveedorUbicacion.cantidadConsultas)
    }

    @Test
    fun `al cambiar el orden por cercania se actualiza el estado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        assertFalse(viewModel.estadoVisual.value.ordenarPorCercania)

        viewModel.procesarEvento(SenderosEvento.AlCambiarOrdenCercania(activado = true))
        assertTrue(viewModel.estadoVisual.value.ordenarPorCercania)

        viewModel.procesarEvento(SenderosEvento.AlCambiarOrdenCercania(activado = false))
        assertFalse(viewModel.estadoVisual.value.ordenarPorCercania)
    }

    @Test
    fun `alternar una dificultad la agrega y al repetirla la quita`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        assertTrue(viewModel.estadoVisual.value.dificultadesSeleccionadas.isEmpty())

        viewModel.procesarEvento(SenderosEvento.AlAlternarDificultad(Dificultad.MEDIA))
        viewModel.procesarEvento(SenderosEvento.AlAlternarDificultad(Dificultad.ALTA))
        assertEquals(setOf(Dificultad.MEDIA, Dificultad.ALTA), viewModel.estadoVisual.value.dificultadesSeleccionadas)

        viewModel.procesarEvento(SenderosEvento.AlAlternarDificultad(Dificultad.MEDIA))
        assertEquals(setOf(Dificultad.ALTA), viewModel.estadoVisual.value.dificultadesSeleccionadas)
    }

    private fun TestScope.recolectarEfectos(viewModel: SenderosViewModel): List<SenderosEfecto> {
        val efectos = mutableListOf<SenderosEfecto>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.efectoVisual.collect { efecto -> efectos.add(efecto) } }
        return efectos
    }

    @Test
    fun `una ubicacion nula no reemplaza una ubicacion valida`() = runTest(despachadorPrueba) {
        val ubicacion = Coordenada(latitud = -34.6, longitud = -58.4)
        proveedorUbicacion.resultado = Result.success(ubicacion)
        val viewModel = crearViewModel()
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        proveedorUbicacion.resultado = Result.success(null)
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(ubicacion, viewModel.estadoVisual.value.ubicacionActual)
    }

    @Test
    fun `ignora el pedido de ubicacion si ya hay uno en curso`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.success(Coordenada(latitud = 1.0, longitud = 1.0))
        val viewModel = crearViewModel()

        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(1, proveedorUbicacion.cantidadConsultas)
    }

    @Test
    fun `sin ubicacion y con orden por cercania activo emite un aviso`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.success(null)
        val viewModel = crearViewModel()
        viewModel.procesarEvento(SenderosEvento.AlCambiarOrdenCercania(activado = true))

        val efectos = recolectarEfectos(viewModel)
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(listOf<SenderosEfecto>(SenderosEfecto.MostrarAviso("No pudimos obtener tu ubicación")), efectos)
    }

    @Test
    fun `un fallo de ubicacion con orden por cercania activo emite un aviso`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.failure(IllegalStateException("GPS apagado"))
        val viewModel = crearViewModel()
        viewModel.procesarEvento(SenderosEvento.AlCambiarOrdenCercania(activado = true))

        val efectos = recolectarEfectos(viewModel)
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(listOf<SenderosEfecto>(SenderosEfecto.MostrarAviso("No pudimos obtener tu ubicación")), efectos)
    }

    @Test
    fun `sin orden por cercania no emite aviso si falta la ubicacion`() = runTest(despachadorPrueba) {
        proveedorUbicacion.resultado = Result.success(null)
        val viewModel = crearViewModel()

        val efectos = recolectarEfectos(viewModel)
        viewModel.procesarEvento(SenderosEvento.AlObtenerUbicacion)
        advanceUntilIdle()

        assertEquals(emptyList<SenderosEfecto>(), efectos)
    }

    @Test
    fun `al solicitar el permiso de notificaciones queda registrado en el estado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        assertFalse(viewModel.estadoVisual.value.seSolicitoPermisoNotificaciones)

        viewModel.procesarEvento(SenderosEvento.AlSolicitarPermisoNotificaciones)

        assertTrue(viewModel.estadoVisual.value.seSolicitoPermisoNotificaciones)
    }

    @Test
    fun `el estado refleja la falta de conexion`() = runTest(despachadorPrueba) {
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
