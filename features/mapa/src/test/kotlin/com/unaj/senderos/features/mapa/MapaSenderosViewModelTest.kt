package com.unaj.senderos.features.mapa

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderosPublicosUseCase
import com.unaj.senderos.core.testing.FalsoSenderoPublicoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import com.unaj.senderos.core.testing.crearSenderoPublicoDePrueba
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoMonitorConectividad
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
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
    private val repositorioPublicos = FalsoSenderoPublicoRepositorio()
    private val efectosRecibidos = mutableListOf<MapaSenderosEfecto>()
    private val trazado = listOf(PuntoRecorrido(-41.10, -71.30), PuntoRecorrido(-41.11, -71.31))

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(propios: List<Sendero> = emptyList()): MapaSenderosViewModel {
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val casoDeUso = ObservarSenderosDelUsuarioUseCase(FalsoSenderoRepositorio(propios), autenticacion)
        val publicos = ObservarSenderosPublicosUseCase(repositorioPublicos, autenticacion)
        val viewModel = MapaSenderosViewModel(casoDeUso, publicos, monitorConectividad)
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
    }

    private val propio = crearSenderoDePrueba(id = "propio-1", idUsuario = "usuario-1", recorrido = trazado)
    private val sinRecorrido = crearSenderoDePrueba(id = "propio-2", idUsuario = "usuario-1")
    private val deOtro = crearSenderoPublicoDePrueba(
        crearSenderoDePrueba(id = "publico-1", idUsuario = "autor-1", recorrido = trazado),
        nombreAutor = "Luis"
    )

    private fun idsEnMapa(viewModel: MapaSenderosViewModel) = viewModel.estadoVisual.value.senderosEnMapa.map { it.id }

    @Test
    fun `el mapa muestra los senderos propios y los publicos de otros`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(deOtro)
        val viewModel = crearViewModel(listOf(propio))
        advanceUntilIdle()

        assertEquals(listOf("propio-1", "publico-1"), idsEnMapa(viewModel))
    }

    @Test
    fun `el mapa omite los senderos sin recorrido`() = runTest(despachadorPrueba) {
        val sinTrazadoPublico = crearSenderoPublicoDePrueba(crearSenderoDePrueba(id = "publico-2", idUsuario = "autor-1"))
        repositorioPublicos.publicos.value = listOf(sinTrazadoPublico)
        val viewModel = crearViewModel(listOf(propio, sinRecorrido))
        advanceUntilIdle()

        assertEquals(listOf("propio-1"), idsEnMapa(viewModel))
    }

    @Test
    fun `no se repite un sendero propio ya publicado ni uno ya guardado como copia`() = runTest(despachadorPrueba) {
        val miPublicacion = crearSenderoPublicoDePrueba(propio, nombreAutor = "Ana")
        val copia = crearSenderoDePrueba(id = "copia-1", idUsuario = "usuario-1", recorrido = trazado).copy(idOrigen = "publico-1")
        repositorioPublicos.publicos.value = listOf(miPublicacion, deOtro)
        val viewModel = crearViewModel(listOf(propio, copia))
        advanceUntilIdle()

        assertEquals(listOf("propio-1", "copia-1"), idsEnMapa(viewModel))
    }

    @Test
    fun `si fallan los publicos el mapa sigue mostrando los propios`() = runTest(despachadorPrueba) {
        repositorioPublicos.errorEnFlujo = IllegalStateException("PERMISSION_DENIED")
        val viewModel = crearViewModel(listOf(propio))
        advanceUntilIdle()

        assertEquals(listOf("propio-1"), idsEnMapa(viewModel))
        assertNull(viewModel.estadoVisual.value.mensajeError)
    }

    @Test
    fun `solo los publicos de otros muestran autor`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(deOtro)
        val viewModel = crearViewModel(listOf(propio))
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertEquals("Luis", estado.nombreAutorDe(deOtro.sendero))
        assertNull(estado.nombreAutorDe(propio))
    }

    @Test
    fun `abrir un sendero publico de otro navega al detalle publico`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(deOtro)
        val viewModel = crearViewModel(listOf(propio))
        advanceUntilIdle()

        viewModel.procesarEvento(MapaSenderosEvento.AlAbrirDetalle("publico-1"))
        advanceUntilIdle()

        assertEquals(listOf<MapaSenderosEfecto>(MapaSenderosEfecto.NavegarADetallePublico("publico-1")), efectosRecibidos)
    }

    @Test
    fun `abrir un sendero propio navega al detalle propio`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(deOtro)
        val viewModel = crearViewModel(listOf(propio))
        advanceUntilIdle()

        viewModel.procesarEvento(MapaSenderosEvento.AlAbrirDetalle("propio-1"))
        advanceUntilIdle()

        assertEquals(listOf<MapaSenderosEfecto>(MapaSenderosEfecto.NavegarADetalle("propio-1")), efectosRecibidos)
    }

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
