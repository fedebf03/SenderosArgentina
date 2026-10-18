package com.unaj.senderos.features.senderos.listado

import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.FalsoAlmacenBorradorRecorrido
import com.unaj.senderos.core.domain.usecase.RecuperarRecorridoInterrumpidoUseCase
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderosPublicosUseCase
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoControlSeguimiento
import com.unaj.senderos.core.testing.FalsoLanzadorSeguimiento
import com.unaj.senderos.core.testing.FalsoMonitorConectividad
import com.unaj.senderos.core.testing.FalsoProveedorUbicacion
import com.unaj.senderos.core.testing.FalsoSenderoPublicoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Botón "Caminar ahora" de Explorar: inicia una caminata libre de una, sin elegir sendero, y abre la pantalla del recorrido
@OptIn(ExperimentalCoroutinesApi::class)
class SenderosCaminarAhoraTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val control = FalsoControlSeguimiento()
    private val lanzador = FalsoLanzadorSeguimiento()
    private val efectosRecibidos = mutableListOf<SenderosEfecto>()
    private var relojActual = 1_700_000_000_000L

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel de Explorar con un usuario con sesión, o sin ella
    private fun crearViewModel(conSesion: Boolean = true): SenderosViewModel {
        val autenticacion = FalsoAutenticacionRepositorio(if (conSesion) Usuario(id = "usuario-1", email = "a@b.com") else null)
        val viewModel = SenderosViewModel(
            ModoListado.EXPLORAR,
            ObservarSenderosDelUsuarioUseCase(FalsoSenderoRepositorio(), autenticacion),
            ObservarSenderosPublicosUseCase(FalsoSenderoPublicoRepositorio(), autenticacion),
            FalsoProveedorUbicacion(),
            FalsoMonitorConectividad(),
            control,
            lanzador,
            ObtenerUsuarioActualUseCase(autenticacion),
            RecuperarRecorridoInterrumpidoUseCase(FalsoAlmacenBorradorRecorrido(), FalsoRecorridoRepositorio(), FalsoPuntosCapturadosRepositorio(), autenticacion),
            { relojActual }
        )
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) } }
        return viewModel
    }

    private fun estadoActivo(id: String, esLibre: Boolean) = EstadoSeguimiento(
        idSendero = id,
        trazado = emptyList(),
        posicionActual = Coordenada(-41.13, -71.30),
        esCaminataLibre = esLibre
    )

    @Test
    fun `caminar ahora inicia una caminata libre con un identificador nuevo y abre su pantalla`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlCaminarAhora)
        advanceUntilIdle()

        val iniciada = lanzador.caminatasLibresIniciadas.single()
        assertTrue(IdentificadorCaminataLibre.esDeCaminataLibre(iniciada.first))
        assertEquals("usuario-1", iniciada.second)
        assertEquals("Caminata libre", iniciada.third)
        assertTrue(efectosRecibidos.contains(SenderosEfecto.NavegarACaminata(iniciada.first)))
    }

    @Test
    fun `dos caminatas en momentos distintos no comparten identificador`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlCaminarAhora)
        relojActual += 60_000L
        control.estadoEmitido.value = null
        viewModel.procesarEvento(SenderosEvento.AlCaminarAhora)
        advanceUntilIdle()

        assertEquals(2, lanzador.caminatasLibresIniciadas.map { iniciada -> iniciada.first }.toSet().size)
    }

    @Test
    fun `con una caminata libre en curso vuelve a esa pantalla en lugar de empezar otra`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoActivo("libre-123", esLibre = true)
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlCaminarAhora)
        advanceUntilIdle()

        assertTrue(lanzador.caminatasLibresIniciadas.isEmpty())
        assertTrue(efectosRecibidos.contains(SenderosEfecto.NavegarACaminata("libre-123")))
    }

    @Test
    fun `con un recorrido de sendero en curso no empieza una caminata y avisa`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoActivo("sendero-1", esLibre = false)
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlCaminarAhora)
        advanceUntilIdle()

        assertTrue(lanzador.caminatasLibresIniciadas.isEmpty())
        assertTrue(efectosRecibidos.any { efecto -> efecto is SenderosEfecto.MostrarAviso })
    }

    @Test
    fun `sin sesion no inicia nada y avisa`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(conSesion = false)
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlCaminarAhora)
        advanceUntilIdle()

        assertTrue(lanzador.caminatasLibresIniciadas.isEmpty())
        assertTrue(efectosRecibidos.any { efecto -> efecto is SenderosEfecto.MostrarAviso })
    }

    @Test
    fun `tocar caminar ahora pide primero el permiso de ubicacion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlTocarCaminarAhora)
        advanceUntilIdle()

        assertTrue(efectosRecibidos.contains(SenderosEfecto.SolicitarPermisoUbicacionParaCaminar))
        assertTrue(lanzador.caminatasLibresIniciadas.isEmpty())
    }

    @Test
    fun `si se deniega el permiso de ubicacion avisa y no inicia`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlDenegarPermisoParaCaminar)
        advanceUntilIdle()

        assertTrue(lanzador.caminatasLibresIniciadas.isEmpty())
        assertTrue(efectosRecibidos.any { efecto -> efecto is SenderosEfecto.MostrarAviso })
    }
}
