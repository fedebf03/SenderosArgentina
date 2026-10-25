package com.unaj.senderos.features.recorrido

import com.unaj.senderos.core.domain.clima.AlertaClima
import com.unaj.senderos.core.domain.clima.CondicionClima
import com.unaj.senderos.core.domain.clima.PronosticoDia
import com.unaj.senderos.core.domain.model.CalidadSenal
import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.port.SinConexionPronosticoException
import com.unaj.senderos.core.domain.usecase.DescartarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.IncorporarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarPuntosCapturadosConDistanciaUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerPronosticoUseCase
import com.unaj.senderos.core.testing.FalsaFuentePronostico
import com.unaj.senderos.core.testing.FalsoAlmacenPronosticos
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoControlSeguimiento
import com.unaj.senderos.core.testing.FalsoLanzadorSeguimiento
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearPronosticoDePrueba
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val ID_CAMINATA = "libre-5000"
private const val DIEZ_MINUTOS = 10 * 60_000L

// Clima durante el recorrido: se pide el del lugar donde está el usuario, se renueva cada tanto y sus avisos se muestran en pantalla
@OptIn(ExperimentalCoroutinesApi::class)
class RecorridoClimaTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val control = FalsoControlSeguimiento()
    private val fuente = FalsaFuentePronostico()
    private val almacen = FalsoAlmacenPronosticos()
    private val efectosRecibidos = mutableListOf<RecorridoEfecto>()
    private var relojActual = 1_000L
    private var fuentePresion = com.unaj.senderos.core.domain.port.FuentePresion { kotlinx.coroutines.flow.flowOf(1013.4, 1013.2, 1012.8) }

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel de una caminata libre con el clima conectado a las fuentes falsas
    private fun crearViewModel(): RecorridoViewModel {
        val repositorio = FalsoSenderoRepositorio()
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val puntos = FalsoPuntosCapturadosRepositorio()
        val viewModel = RecorridoViewModel(
            idSendero = ID_CAMINATA,
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            controlSeguimiento = control,
            lanzadorSeguimiento = FalsoLanzadorSeguimiento(),
            capacidadesDispositivo = CapacidadesDispositivo(tieneGps = true, tieneAcelerometro = true, tieneBrujula = true),
            observarPuntosCapturadosConDistanciaUseCase = ObservarPuntosCapturadosConDistanciaUseCase(repositorio, puntos, autenticacion),
            incorporarPuntosCapturadosUseCase = IncorporarPuntosCapturadosUseCase(puntos, autenticacion),
            descartarPuntosCapturadosUseCase = DescartarPuntosCapturadosUseCase(puntos, autenticacion),
            obtenerPronosticoUseCase = ObtenerPronosticoUseCase(fuente, almacen) { relojActual },
            fuentePresion = fuentePresion,
            reloj = { relojActual }
        )
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) } }
        return viewModel
    }

    // Publicar el estado de una caminata libre con la posición indicada (o sin posición todavía)
    private fun publicarEstado(posicion: Coordenada? = Coordenada(-41.13, -71.30)) {
        control.estadoEmitido.value = EstadoSeguimiento(
            idSendero = ID_CAMINATA,
            trazado = emptyList(),
            posicionActual = posicion,
            calidadSenal = CalidadSenal.BUENA,
            esCaminataLibre = true
        )
    }

    @Test
    fun `al conocer la posicion pide el clima de ese lugar`() = runTest(despachadorPrueba) {
        crearViewModel()
        publicarEstado(Coordenada(-41.13, -71.30))
        advanceUntilIdle()

        assertEquals(1, fuente.lugaresPedidos.size)
        assertEquals(-41.13, fuente.lugaresPedidos.single().latitud, 0.0)
    }

    @Test
    fun `sin posicion todavia no pide el clima`() = runTest(despachadorPrueba) {
        crearViewModel()
        publicarEstado(posicion = null)
        advanceUntilIdle()

        assertTrue(fuente.lugaresPedidos.isEmpty())
    }

    @Test
    fun `el clima obtenido se refleja en el estado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        publicarEstado()
        advanceUntilIdle()

        assertNotNull(viewModel.estadoVisual.value.clima)
        assertTrue(viewModel.estadoVisual.value.alertasClima.isEmpty())
    }

    @Test
    fun `no vuelve a pedir el clima antes de pasados 10 minutos aunque cambie la posicion`() = runTest(despachadorPrueba) {
        crearViewModel()
        publicarEstado(Coordenada(-41.13, -71.30))
        advanceUntilIdle()
        relojActual += 5 * 60_000L

        publicarEstado(Coordenada(-41.15, -71.30))
        advanceUntilIdle()

        assertEquals(1, fuente.lugaresPedidos.size)
    }

    @Test
    fun `pasados 10 minutos vuelve a consultar el clima del lugar donde esta`() = runTest(despachadorPrueba) {
        crearViewModel()
        publicarEstado(Coordenada(-41.13, -71.30))
        advanceUntilIdle()
        relojActual += DIEZ_MINUTOS + 1_000L
        fuente.resultado = Result.success(crearPronosticoDePrueba(obtenidoMilisegundos = relojActual))

        publicarEstado(Coordenada(-41.50, -71.30))
        advanceUntilIdle()

        assertEquals(2, fuente.lugaresPedidos.size)
        assertEquals(-41.50, fuente.lugaresPedidos.last().latitud, 0.0)
    }

    @Test
    fun `sin conexion y sin copia el clima queda sin dato y no se molesta al usuario`() = runTest(despachadorPrueba) {
        fuente.resultado = Result.failure(SinConexionPronosticoException(null))
        val viewModel = crearViewModel()
        publicarEstado()
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.clima)
        assertTrue(efectosRecibidos.isEmpty())
    }

    @Test
    fun `un clima con viento fuerte se informa como aviso en el estado y una sola vez en pantalla`() = runTest(despachadorPrueba) {
        fuente.resultado = Result.success(
            crearPronosticoDePrueba(
                obtenidoMilisegundos = relojActual,
                actual = crearPronosticoDePrueba().actual.copy(vientoKilometrosPorHora = 55.0)
            )
        )
        val viewModel = crearViewModel()
        publicarEstado()
        advanceUntilIdle()
        relojActual += DIEZ_MINUTOS + 1_000L
        fuente.resultado = Result.success(
            crearPronosticoDePrueba(obtenidoMilisegundos = relojActual, actual = crearPronosticoDePrueba().actual.copy(vientoKilometrosPorHora = 60.0))
        )
        publicarEstado(Coordenada(-41.14, -71.30))
        advanceUntilIdle()

        assertEquals(setOf(AlertaClima.VIENTO_FUERTE), viewModel.estadoVisual.value.alertasClima)
        val avisos = efectosRecibidos.filterIsInstance<RecorridoEfecto.MostrarAviso>().filter { aviso -> aviso.mensaje.contains("Viento fuerte") }
        assertEquals(1, avisos.size)
    }

    @Test
    fun `las alertas del dia de hoy tambien cuentan durante el recorrido`() = runTest(despachadorPrueba) {
        fuente.resultado = Result.success(
            crearPronosticoDePrueba(
                obtenidoMilisegundos = relojActual,
                dias = listOf(
                    PronosticoDia("2026-10-19", 17.0, 6.0, 90, 25.0, 20.0, 30.0, CondicionClima.LLUVIA_FUERTE),
                    PronosticoDia("2026-10-20", 17.0, 5.0, 20, 0.5, 18.0, 30.0, CondicionClima.TORMENTA)
                )
            )
        )
        val viewModel = crearViewModel()
        publicarEstado()
        advanceUntilIdle()

        assertEquals(setOf(AlertaClima.LLUVIA_INTENSA), viewModel.estadoVisual.value.alertasClima)
    }

    @Test
    fun `la presion del barometro se muestra en hectopascales enteros`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertEquals(1013, viewModel.estadoVisual.value.presionHectopascales)
    }

    @Test
    fun `sin barometro la presion queda sin dato y la pantalla sigue funcionando`() = runTest(despachadorPrueba) {
        fuentePresion = com.unaj.senderos.core.domain.port.FuentePresion { kotlinx.coroutines.flow.flow { throw IllegalStateException("Sin barómetro") } }
        val viewModel = crearViewModel()
        publicarEstado()
        advanceUntilIdle()

        assertEquals(null, viewModel.estadoVisual.value.presionHectopascales)
        assertEquals(1, fuente.lugaresPedidos.size)
    }
}
