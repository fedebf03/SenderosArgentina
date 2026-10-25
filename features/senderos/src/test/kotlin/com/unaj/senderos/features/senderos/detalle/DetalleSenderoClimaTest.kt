package com.unaj.senderos.features.senderos.detalle

import com.unaj.senderos.core.domain.clima.AlertaClima
import com.unaj.senderos.core.domain.clima.CondicionClima
import com.unaj.senderos.core.domain.clima.PronosticoDia
import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.port.SinConexionPronosticoException
import com.unaj.senderos.core.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.core.domain.usecase.CalcularPerfilElevacionUseCase
import com.unaj.senderos.core.domain.usecase.DescargarMapaSinConexionUseCase
import com.unaj.senderos.core.domain.usecase.DescartarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.DespublicarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.EliminarMapaSinConexionUseCase
import com.unaj.senderos.core.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ExportarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.IncorporarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarMapaSinConexionUseCase
import com.unaj.senderos.core.domain.usecase.ObservarPuntosCapturadosConDistanciaUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderoPublicadoUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerPronosticoUseCase
import com.unaj.senderos.core.domain.usecase.PublicarSenderoUseCase
import com.unaj.senderos.core.testing.FalsaFuentePronostico
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoControlSeguimiento
import com.unaj.senderos.core.testing.FalsoEscritorArchivos
import com.unaj.senderos.core.testing.FalsoFormatoCopiaSeguridad
import com.unaj.senderos.core.testing.FalsoFuenteElevacion
import com.unaj.senderos.core.testing.FalsoGestorMapasSinConexion
import com.unaj.senderos.core.testing.FalsoLanzadorSeguimiento
import com.unaj.senderos.core.testing.FalsoMonitorConectividad
import com.unaj.senderos.core.testing.FalsoAlmacenPronosticos
import com.unaj.senderos.core.testing.FalsoPreferenciasAplicacion
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoPublicoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearPronosticoDePrueba
import com.unaj.senderos.core.testing.crearSenderoDePrueba
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Sección de clima del detalle del sendero: pronóstico en el punto de inicio, avisos y aviso de datos viejos sin conexión
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleSenderoClimaTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val fuentePronostico = FalsaFuentePronostico()
    private val almacenPronosticos = FalsoAlmacenPronosticos()
    private var ahora = 5_000L
    private val recorrido = listOf(PuntoRecorrido(-41.10, -71.30), PuntoRecorrido(-41.11, -71.31))

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel del detalle de un sendero con el recorrido indicado (o sin recorrido)
    private fun crearViewModel(recorridoDelSendero: List<PuntoRecorrido> = recorrido): DetalleSenderoViewModel {
        val repositorio = FalsoSenderoRepositorio(listOf(crearSenderoDePrueba(id = "sendero-1", recorrido = recorridoDelSendero)))
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val gestorMapas = FalsoGestorMapasSinConexion()
        val repositorioPuntos = FalsoPuntosCapturadosRepositorio()
        val repositorioPublicos = FalsoSenderoPublicoRepositorio()
        return DetalleSenderoViewModel(
            idSendero = "sendero-1",
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            alternarFavoritoUseCase = AlternarFavoritoUseCase(repositorio, autenticacion),
            eliminarSenderoUseCase = EliminarSenderoUseCase(repositorio, autenticacion),
            lanzadorSeguimiento = FalsoLanzadorSeguimiento(),
            controlSeguimiento = FalsoControlSeguimiento(),
            preferenciasAplicacion = FalsoPreferenciasAplicacion(),
            observarMapaSinConexionUseCase = ObservarMapaSinConexionUseCase(gestorMapas, autenticacion),
            descargarMapaSinConexionUseCase = DescargarMapaSinConexionUseCase(gestorMapas, autenticacion),
            eliminarMapaSinConexionUseCase = EliminarMapaSinConexionUseCase(gestorMapas, autenticacion),
            monitorConectividad = FalsoMonitorConectividad(),
            capacidadesDispositivo = CapacidadesDispositivo(tieneGps = true, tieneAcelerometro = true, tieneBrujula = true),
            observarPuntosCapturadosConDistanciaUseCase = ObservarPuntosCapturadosConDistanciaUseCase(repositorio, repositorioPuntos, autenticacion),
            incorporarPuntosCapturadosUseCase = IncorporarPuntosCapturadosUseCase(repositorioPuntos, autenticacion),
            descartarPuntosCapturadosUseCase = DescartarPuntosCapturadosUseCase(repositorioPuntos, autenticacion),
            observarSenderoPublicadoUseCase = ObservarSenderoPublicadoUseCase(repositorioPublicos, autenticacion),
            publicarSenderoUseCase = PublicarSenderoUseCase(repositorioPublicos, autenticacion),
            despublicarSenderoUseCase = DespublicarSenderoUseCase(repositorioPublicos, autenticacion),
            calcularPerfilElevacionUseCase = CalcularPerfilElevacionUseCase(repositorio, FalsoFuenteElevacion(), autenticacion),
            exportarSenderoUseCase = ExportarSenderoUseCase(repositorio, autenticacion, FalsoEscritorArchivos(), FalsoFormatoCopiaSeguridad(), despachadorCalculo = despachadorPrueba),
            obtenerPronosticoUseCase = ObtenerPronosticoUseCase(fuentePronostico, almacenPronosticos) { ahora }
        )
    }

    @Test
    fun `al abrir el detalle pide el clima del punto de inicio del sendero`() = runTest(despachadorPrueba) {
        crearViewModel()
        advanceUntilIdle()

        assertEquals(1, fuentePronostico.lugaresPedidos.size)
        assertEquals(-41.10, fuentePronostico.lugaresPedidos.single().latitud, 0.0)
        assertEquals(-71.30, fuentePronostico.lugaresPedidos.single().longitud, 0.0)
    }

    @Test
    fun `el clima obtenido se refleja en el estado sin avisos si el dia es tranquilo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertNotNull(estado.clima)
        assertFalse(estado.estaCargandoClima)
        assertNull(estado.errorClima)
        assertTrue(estado.alertasClimaAhora.isEmpty())
        assertTrue(estado.alertasClimaPorDia.isEmpty())
    }

    @Test
    fun `un sendero sin recorrido no tiene lugar y no pide el clima`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoDelSendero = emptyList())
        advanceUntilIdle()

        assertTrue(fuentePronostico.lugaresPedidos.isEmpty())
        assertNull(viewModel.estadoVisual.value.clima)
        assertFalse(viewModel.estadoVisual.value.estaCargandoClima)
    }

    @Test
    fun `el viento fuerte de ahora y las tormentas de los proximos dias se informan como avisos`() = runTest(despachadorPrueba) {
        fuentePronostico.resultado = Result.success(
            crearPronosticoDePrueba(
                obtenidoMilisegundos = ahora,
                actual = crearPronosticoDePrueba().actual.copy(vientoKilometrosPorHora = 55.0),
                dias = listOf(
                    PronosticoDia("2026-10-19", 18.0, 6.0, 10, 0.0, 15.0, 25.0, CondicionClima.DESPEJADO),
                    PronosticoDia("2026-10-20", 15.0, 5.0, 90, 4.0, 20.0, 35.0, CondicionClima.TORMENTA)
                )
            )
        )
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertEquals(setOf(AlertaClima.VIENTO_FUERTE), estado.alertasClimaAhora)
        assertEquals(setOf("2026-10-20"), estado.alertasClimaPorDia.keys)
    }

    @Test
    fun `sin conexion y sin copia guardada se informa el error`() = runTest(despachadorPrueba) {
        fuentePronostico.resultado = Result.failure(SinConexionPronosticoException(null))
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertNull(estado.clima)
        assertEquals("Sin conexión para consultar el clima", estado.errorClima)
        assertFalse(estado.estaCargandoClima)
    }

    @Test
    fun `sin conexion pero con una copia guardada se muestra la copia marcada como vieja`() = runTest(despachadorPrueba) {
        almacenPronosticos.guardados[ObtenerPronosticoUseCase.claveDe(com.unaj.senderos.core.domain.model.Coordenada(-41.10, -71.30))] =
            crearPronosticoDePrueba(obtenidoMilisegundos = ahora - 3 * 3_600_000L)
        fuentePronostico.resultado = Result.failure(SinConexionPronosticoException(null))
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertNotNull(estado.clima)
        assertTrue(estado.clima?.estaDesactualizado == true)
        assertEquals(3 * 3_600_000L, estado.clima?.antiguedadMilisegundos)
    }

    @Test
    fun `actualizar el clima vuelve a pedirlo a la red aunque haya uno reciente`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlActualizarClima)
        advanceUntilIdle()

        assertEquals(2, fuentePronostico.lugaresPedidos.size)
    }

    @Test
    fun `un toque repetido en actualizar mientras carga se ignora`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlActualizarClima)
        viewModel.procesarEvento(DetalleSenderoEvento.AlActualizarClima)
        advanceUntilIdle()

        assertEquals(2, fuentePronostico.lugaresPedidos.size)
    }
}
