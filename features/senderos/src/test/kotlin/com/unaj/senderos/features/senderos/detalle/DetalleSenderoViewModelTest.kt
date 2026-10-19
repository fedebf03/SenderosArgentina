package com.unaj.senderos.features.senderos.detalle

import com.unaj.senderos.core.testing.FalsoAlmacenPronosticos
import com.unaj.senderos.core.testing.FalsaFuentePronostico
import com.unaj.senderos.core.domain.usecase.ObtenerPronosticoUseCase
import com.unaj.senderos.core.testing.FalsoEscritorArchivos
import com.unaj.senderos.core.testing.FalsoFormatoCopiaSeguridad
import com.unaj.senderos.core.domain.usecase.ExportarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.FormatoExportacion
import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.usecase.DescargarMapaSinConexionUseCase
import com.unaj.senderos.core.domain.usecase.EliminarMapaSinConexionUseCase
import com.unaj.senderos.core.domain.usecase.ObservarMapaSinConexionUseCase
import com.unaj.senderos.core.testing.FalsoGestorMapasSinConexion
import com.unaj.senderos.core.testing.FalsoMonitorConectividad
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.core.domain.usecase.DescartarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.DespublicarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderoPublicadoUseCase
import com.unaj.senderos.core.domain.usecase.PublicarSenderoUseCase
import com.unaj.senderos.core.testing.FalsoSenderoPublicoRepositorio
import com.unaj.senderos.core.testing.crearSenderoPublicoDePrueba
import com.unaj.senderos.core.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.IncorporarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.ObservarPuntosCapturadosConDistanciaUseCase
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.crearPuntoCapturadoDePrueba
import com.unaj.senderos.core.ui.revisionpuntos.RevisionPuntosEvento
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoControlSeguimiento
import com.unaj.senderos.core.testing.FalsoLanzadorSeguimiento
import com.unaj.senderos.core.testing.FalsoPreferenciasAplicacion
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoPerfil
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.domain.port.SinConexionElevacionException
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.core.domain.usecase.CalcularPerfilElevacionUseCase
import com.unaj.senderos.core.testing.FalsoFuenteElevacion
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleSenderoViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val lanzador = FalsoLanzadorSeguimiento()
    private val control = FalsoControlSeguimiento()
    private val preferencias = FalsoPreferenciasAplicacion()
    private val monitorConectividad = FalsoMonitorConectividad()
    private val gestorMapas = FalsoGestorMapasSinConexion().apply { estado.value = EstadoMapaSinConexion.Disponible(1_000L) }
    private val efectosRecibidos = mutableListOf<DetalleSenderoEfecto>()
    private val repositorioPuntos = FalsoPuntosCapturadosRepositorio()
    private val repositorioPublicos = FalsoSenderoPublicoRepositorio()
    private val fuenteElevacion = FalsoFuenteElevacion()
    private val escritorArchivos = FalsoEscritorArchivos()

    private val recorridoValido = listOf(
        PuntoRecorrido(latitud = -41.10, longitud = -71.30),
        PuntoRecorrido(latitud = -41.11, longitud = -71.31)
    )

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private var capacidades = CapacidadesDispositivo(
        tieneGps = true,
        tieneAcelerometro = true,
        tieneBrujula = true
    )

    @Test
    fun `sin gps iniciar un recorrido real muestra el aviso antes del permiso`() = runTest(despachadorPrueba) {
        capacidades = capacidades.copy(tieneGps = false)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esDialogoSinGpsVisible)
        assertTrue(efectosRecibidos.isEmpty())
    }

    @Test
    fun `iniciar igual sin gps continua con el permiso`() = runTest(despachadorPrueba) {
        capacidades = capacidades.copy(tieneGps = false)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)

        viewModel.procesarEvento(DetalleSenderoEvento.AlIniciarSinGps)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoSinGpsVisible)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    @Test
    fun `cancelar el aviso sin gps no inicia el recorrido`() = runTest(despachadorPrueba) {
        capacidades = capacidades.copy(tieneGps = false)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)

        viewModel.procesarEvento(DetalleSenderoEvento.AlCancelarSinGps)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoSinGpsVisible)
        assertTrue(efectosRecibidos.isEmpty())
    }

    @Test
    fun `en simulacion no se avisa la falta de gps`() = runTest(despachadorPrueba) {
        capacidades = capacidades.copy(tieneGps = false)
        preferencias.cambiarModoDemostracion(true)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoSinGpsVisible)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    private fun crearViewModel(
        recorrido: List<PuntoRecorrido>,
        perfilElevacion: PerfilElevacion? = null,
        tipoRecorrido: TipoRecorrido = TipoRecorrido.CIRCULAR,
        idOrigen: String = ""
    ): DetalleSenderoViewModel {
        val repositorio = FalsoSenderoRepositorio(
            listOf(
                crearSenderoDePrueba(
                    id = "sendero-1",
                    recorrido = recorrido,
                    tipoRecorrido = tipoRecorrido,
                    perfilElevacion = perfilElevacion
                ).copy(idOrigen = idOrigen),
                crearSenderoDePrueba(id = "sendero-2", nombre = "Laguna Azul", recorrido = recorridoValido)
            )
        )
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val viewModel = DetalleSenderoViewModel(
            idSendero = "sendero-1",
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            alternarFavoritoUseCase = AlternarFavoritoUseCase(repositorio, autenticacion),
            eliminarSenderoUseCase = EliminarSenderoUseCase(repositorio, autenticacion),
            lanzadorSeguimiento = lanzador,
            controlSeguimiento = control,
            preferenciasAplicacion = preferencias,
            observarMapaSinConexionUseCase = ObservarMapaSinConexionUseCase(gestorMapas, autenticacion),
            descargarMapaSinConexionUseCase = DescargarMapaSinConexionUseCase(gestorMapas, autenticacion),
            eliminarMapaSinConexionUseCase = EliminarMapaSinConexionUseCase(gestorMapas, autenticacion),
            monitorConectividad = monitorConectividad,
            capacidadesDispositivo = capacidades,
            observarPuntosCapturadosConDistanciaUseCase = ObservarPuntosCapturadosConDistanciaUseCase(repositorio, repositorioPuntos.also { falso -> falso.senderoRepositorio = repositorio }, autenticacion),
            incorporarPuntosCapturadosUseCase = IncorporarPuntosCapturadosUseCase(repositorioPuntos, autenticacion),
            descartarPuntosCapturadosUseCase = DescartarPuntosCapturadosUseCase(repositorioPuntos, autenticacion),
            observarSenderoPublicadoUseCase = ObservarSenderoPublicadoUseCase(repositorioPublicos, autenticacion),
            publicarSenderoUseCase = PublicarSenderoUseCase(repositorioPublicos, autenticacion),
            despublicarSenderoUseCase = DespublicarSenderoUseCase(repositorioPublicos, autenticacion),
            calcularPerfilElevacionUseCase = CalcularPerfilElevacionUseCase(repositorio, fuenteElevacion, autenticacion),
            exportarSenderoUseCase = ExportarSenderoUseCase(repositorio, autenticacion, escritorArchivos, FalsoFormatoCopiaSeguridad(), despachadorCalculo = despachadorPrueba),
            obtenerPronosticoUseCase = ObtenerPronosticoUseCase(FalsaFuentePronostico(), FalsoAlmacenPronosticos()) { 0L }
        )
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
    }

    @Test
    fun `un sendero propio se puede editar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEditar)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.puedeEditar)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.NavegarAEdicion("sendero-1")), efectosRecibidos)
    }

    @Test
    fun `una copia de un sendero publico no se puede editar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido, idOrigen = "publico-1")
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEditar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.puedeEditar)
        assertTrue(efectosRecibidos.isEmpty())
    }

    @Test
    fun `iniciar recorrido sin recorrido muestra un aviso`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorrido = listOf(recorridoValido.first()))
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertEquals(
            listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.MostrarAviso("Este sendero no tiene recorrido")),
            efectosRecibidos
        )
        assertTrue(lanzador.idsIniciados.isEmpty())
    }

    @Test
    fun `iniciar recorrido con recorrido pide el permiso de ubicacion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
        assertTrue(lanzador.idsIniciados.isEmpty())
    }

    @Test
    fun `con el permiso concedido lanza el seguimiento y navega al recorrido`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConcederPermisoUbicacion)
        advanceUntilIdle()

        assertEquals(listOf("sendero-1"), lanzador.idsIniciados)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.NavegarARecorrido("sendero-1")), efectosRecibidos)
    }

    @Test
    fun `con el permiso negado avisa y no lanza el seguimiento`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlDenegarPermisoUbicacion)
        advanceUntilIdle()

        assertTrue(lanzador.idsIniciados.isEmpty())
        val aviso = efectosRecibidos.single() as DetalleSenderoEfecto.MostrarAviso
        assertTrue(aviso.mensaje.contains("ubicación"))
    }

    @Test
    fun `conceder el permiso sin recorrido no lanza el seguimiento`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorrido = emptyList())
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConcederPermisoUbicacion)
        advanceUntilIdle()

        assertTrue(lanzador.idsIniciados.isEmpty())
        assertEquals(
            listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.MostrarAviso("Este sendero no tiene recorrido")),
            efectosRecibidos
        )
    }

    @Test
    fun `con solo la ubicacion aproximada avisa y no lanza el seguimiento`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConcederSoloUbicacionAproximada)
        advanceUntilIdle()

        assertTrue(lanzador.idsIniciados.isEmpty())
        assertEquals(
            listOf<DetalleSenderoEfecto>(
                DetalleSenderoEfecto.MostrarAviso("Para seguir el recorrido hace falta la ubicación precisa")
            ),
            efectosRecibidos
        )
    }

    @Test
    fun `con el recorrido ya activo en este sendero navega sin lanzar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "sendero-1", trazado = recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.hayRecorridoEnCursoEnEsteSendero)
        assertTrue(lanzador.idsIniciados.isEmpty())
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.NavegarARecorrido("sendero-1")), efectosRecibidos)
    }

    @Test
    fun `con el recorrido activo de otro sendero pide confirmar el reemplazo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "sendero-2", trazado = recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.hayRecorridoEnCursoEnEsteSendero)
        assertEquals("Laguna Azul", viewModel.estadoVisual.value.nombreSenderoEnCursoAReemplazar)
        assertTrue(efectosRecibidos.isEmpty())
    }

    @Test
    fun `confirmar el reemplazo pide el permiso de ubicacion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "sendero-2", trazado = recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarReemplazarRecorrido)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.nombreSenderoEnCursoAReemplazar)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    @Test
    fun `cancelar el reemplazo no hace nada`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "sendero-2", trazado = recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlCancelarReemplazarRecorrido)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.nombreSenderoEnCursoAReemplazar)
        assertTrue(efectosRecibidos.isEmpty())
        assertTrue(lanzador.idsIniciados.isEmpty())
    }

    @Test
    fun `sin modo demostracion el recorrido se lanza real`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConcederPermisoUbicacion)
        advanceUntilIdle()

        assertEquals(listOf(false), lanzador.iniciosSimulados)
        assertFalse(viewModel.estadoVisual.value.esModoDemostracion)
    }

    @Test
    fun `con modo demostracion el recorrido se lanza simulado`() = runTest(despachadorPrueba) {
        preferencias.cambiarModoDemostracion(true)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConcederPermisoUbicacion)
        advanceUntilIdle()

        assertEquals(listOf(true), lanzador.iniciosSimulados)
        assertTrue(viewModel.estadoVisual.value.esModoDemostracion)
    }

    @Test
    fun `si falla la busqueda del otro sendero usa el nombre de respaldo`() = runTest(despachadorPrueba) {
        val repositorio = FalsoSenderoRepositorio(
            listOf(crearSenderoDePrueba(id = "sendero-1", recorrido = recorridoValido))
        )
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val viewModel = DetalleSenderoViewModel(
            idSendero = "sendero-1",
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            alternarFavoritoUseCase = AlternarFavoritoUseCase(repositorio, autenticacion),
            eliminarSenderoUseCase = EliminarSenderoUseCase(repositorio, autenticacion),
            lanzadorSeguimiento = lanzador,
            controlSeguimiento = control,
            preferenciasAplicacion = preferencias,
            observarMapaSinConexionUseCase = ObservarMapaSinConexionUseCase(gestorMapas, autenticacion),
            descargarMapaSinConexionUseCase = DescargarMapaSinConexionUseCase(gestorMapas, autenticacion),
            eliminarMapaSinConexionUseCase = EliminarMapaSinConexionUseCase(gestorMapas, autenticacion),
            monitorConectividad = monitorConectividad,
            capacidadesDispositivo = capacidades,
            observarPuntosCapturadosConDistanciaUseCase = ObservarPuntosCapturadosConDistanciaUseCase(repositorio, repositorioPuntos.also { falso -> falso.senderoRepositorio = repositorio }, autenticacion),
            incorporarPuntosCapturadosUseCase = IncorporarPuntosCapturadosUseCase(repositorioPuntos, autenticacion),
            descartarPuntosCapturadosUseCase = DescartarPuntosCapturadosUseCase(repositorioPuntos, autenticacion),
            observarSenderoPublicadoUseCase = ObservarSenderoPublicadoUseCase(repositorioPublicos, autenticacion),
            publicarSenderoUseCase = PublicarSenderoUseCase(repositorioPublicos, autenticacion),
            despublicarSenderoUseCase = DespublicarSenderoUseCase(repositorioPublicos, autenticacion),
            calcularPerfilElevacionUseCase = CalcularPerfilElevacionUseCase(repositorio, fuenteElevacion, autenticacion),
            exportarSenderoUseCase = ExportarSenderoUseCase(repositorio, autenticacion, escritorArchivos, FalsoFormatoCopiaSeguridad(), despachadorCalculo = despachadorPrueba),
            obtenerPronosticoUseCase = ObtenerPronosticoUseCase(FalsaFuentePronostico(), FalsoAlmacenPronosticos()) { 0L }
        )
        advanceUntilIdle()
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "sendero-2", trazado = recorridoValido)
        repositorio.errorEnFlujo = IllegalStateException("fallo de lectura")
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertEquals("otro sendero", viewModel.estadoVisual.value.nombreSenderoEnCursoAReemplazar)
    }

    @Test
    fun `un recorrido simulado en este sendero se marca como simulado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        control.estadoEmitido.value = EstadoSeguimiento(
            idSendero = "sendero-1",
            trazado = recorridoValido,
            esSimulado = true
        )
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.hayRecorridoEnCursoEnEsteSendero)
        assertTrue(viewModel.estadoVisual.value.esRecorridoEnCursoSimulado)
    }

    @Test
    fun `seleccionar un punto de interes enfoca su coordenada y repite el enfoque`() = runTest(despachadorPrueba) {
        val recorridoConInteres = listOf(
            PuntoRecorrido(latitud = -41.10, longitud = -71.30),
            PuntoRecorrido(latitud = -41.11, longitud = -71.31, tipo = com.unaj.senderos.core.domain.model.TipoPunto.AGUA)
        )
        val viewModel = crearViewModel(recorridoConInteres)
        advanceUntilIdle()
        assertEquals(0, viewModel.estadoVisual.value.contadorEnfoque)
        assertEquals(listOf(1), viewModel.estadoVisual.value.puntosDeInteres.map { punto -> punto.indice })

        viewModel.procesarEvento(DetalleSenderoEvento.AlSeleccionarPuntoDeInteres(1))
        advanceUntilIdle()
        val primero = viewModel.estadoVisual.value
        viewModel.procesarEvento(DetalleSenderoEvento.AlSeleccionarPuntoDeInteres(1))
        advanceUntilIdle()
        val segundo = viewModel.estadoVisual.value

        assertEquals(com.unaj.senderos.core.domain.model.Coordenada(-41.11, -71.31), primero.puntoEnfocado)
        assertEquals(1, primero.contadorEnfoque)
        assertEquals(2, segundo.contadorEnfoque)
    }

    @Test
    fun `seleccionar un indice inexistente no cambia el enfoque`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlSeleccionarPuntoDeInteres(7))
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.puntoEnfocado)
        assertEquals(0, viewModel.estadoVisual.value.contadorEnfoque)
    }

    @Test
    fun `refleja los estados del mapa sin conexion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        assertEquals(EstadoMapaSinConexion.Disponible(1_000L), viewModel.estadoVisual.value.estadoMapaSinConexion)

        gestorMapas.estado.value = EstadoMapaSinConexion.Descargando(40, 0)
        advanceUntilIdle()
        assertEquals(EstadoMapaSinConexion.Descargando(40, 0), viewModel.estadoVisual.value.estadoMapaSinConexion)

        gestorMapas.estado.value = EstadoMapaSinConexion.ConError("Sin conexión")
        advanceUntilIdle()
        assertEquals(EstadoMapaSinConexion.ConError("Sin conexión"), viewModel.estadoVisual.value.estadoMapaSinConexion)
    }

    @Test
    fun `tocar descargar el mapa llama al caso de uso`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa)
        advanceUntilIdle()

        assertEquals(listOf("usuario-1/sendero-1"), gestorMapas.descargas)
    }

    @Test
    fun `eliminar el mapa pide confirmacion y luego elimina`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEliminarMapa)
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.esDialogoEliminarMapaVisible)
        assertTrue(gestorMapas.eliminaciones.isEmpty())

        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminarMapa)
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.esDialogoEliminarMapaVisible)
        assertEquals(listOf("usuario-1/sendero-1"), gestorMapas.eliminaciones)
    }

    @Test
    fun `cancelar la eliminacion del mapa no elimina`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEliminarMapa)

        viewModel.procesarEvento(DetalleSenderoEvento.AlCancelarEliminarMapa)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoEliminarMapaVisible)
        assertTrue(gestorMapas.eliminaciones.isEmpty())
    }

    @Test
    fun `con el mapa disponible no se sugiere descargar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    @Test
    fun `sin el mapa descargado se sugiere antes de pedir el permiso`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.NoDescargado
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
        assertTrue(efectosRecibidos.isEmpty())
    }

    @Test
    fun `con el mapa desactualizado tambien se sugiere`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.Desactualizado(500L)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
    }

    @Test
    fun `descargar e iniciar descarga y continua con el inicio`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.NoDescargado
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlDescargarEIniciar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
        assertEquals(listOf("usuario-1/sendero-1"), gestorMapas.descargas)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    @Test
    fun `iniciar sin descargar continua con el inicio sin descargar`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.NoDescargado
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlIniciarSinDescargar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
        assertTrue(gestorMapas.descargas.isEmpty())
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    @Test
    fun `cancelar o eliminar el mapa funciona en cada estado`() = runTest(despachadorPrueba) {
        val estados = listOf(
            EstadoMapaSinConexion.Descargando(30, 100L),
            EstadoMapaSinConexion.EsperandoConexion(30, 100L),
            EstadoMapaSinConexion.ConError("Falló"),
            EstadoMapaSinConexion.Desactualizado(500L),
            EstadoMapaSinConexion.Disponible(500L)
        )
        val viewModel = crearViewModel(recorridoValido)
        estados.forEach { estado ->
            gestorMapas.estado.value = estado
            advanceUntilIdle()

            viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEliminarMapa)
            assertTrue(viewModel.estadoVisual.value.esDialogoEliminarMapaVisible)
            viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminarMapa)
            advanceUntilIdle()
        }

        assertEquals(estados.size, gestorMapas.eliminaciones.size)
    }

    @Test
    fun `sin conexion no se sugiere descargar`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.NoDescargado
        monitorConectividad.hayConexionActual = false
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    @Test
    fun `el doble toque en descargar se ignora`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.NoDescargado
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa)
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa)
        advanceUntilIdle()

        assertEquals(1, gestorMapas.descargas.size)
    }

    @Test
    fun `descargar se ignora si ya hay una descarga activa`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        listOf(EstadoMapaSinConexion.Descargando(10, 0L), EstadoMapaSinConexion.EsperandoConexion(10, 0L)).forEach { estado ->
            gestorMapas.estado.value = estado
            advanceUntilIdle()

            viewModel.procesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa)
            viewModel.procesarEvento(DetalleSenderoEvento.AlDescargarEIniciar)
            advanceUntilIdle()
        }

        assertTrue(gestorMapas.descargas.isEmpty())
    }

    @Test
    fun `sin puntos capturados no hay puntos por revisar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.hayPuntosPorRevisar)
        assertEquals(0, viewModel.estadoVisual.value.revision.puntosPendientes.size)
    }

    @Test
    fun `los puntos capturados del sendero activan la tarjeta de revision`() = runTest(despachadorPrueba) {
        repositorioPuntos.puntos.value = listOf(
            crearPuntoCapturadoDePrueba(id = "a"),
            crearPuntoCapturadoDePrueba(id = "b", instanteMilisegundos = 2L),
            crearPuntoCapturadoDePrueba(id = "ajeno", idSendero = "sendero-2")
        )
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.hayPuntosPorRevisar)
        assertEquals(2, viewModel.estadoVisual.value.revision.puntosPendientes.size)
    }

    @Test
    fun `con un recorrido en curso la tarjeta de revision se oculta`() = runTest(despachadorPrueba) {
        repositorioPuntos.puntos.value = listOf(crearPuntoCapturadoDePrueba(id = "a"))
        val viewModel = crearViewModel(recorridoValido)
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "sendero-1", trazado = recorridoValido)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.hayPuntosPorRevisar)
    }

    @Test
    fun `revisar abre el dialogo y agregar incorpora los puntos`() = runTest(despachadorPrueba) {
        repositorioPuntos.puntos.value = listOf(
            crearPuntoCapturadoDePrueba(id = "a", latitud = -41.105, longitud = -71.305),
            crearPuntoCapturadoDePrueba(id = "b", latitud = -41.107, longitud = -71.307, instanteMilisegundos = 2L)
        )
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlRevisionPuntos(RevisionPuntosEvento.AlAbrir))
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.revision.esRevisionVisible)

        viewModel.procesarEvento(DetalleSenderoEvento.AlRevisionPuntos(RevisionPuntosEvento.AlAlternarPunto("b")))
        viewModel.procesarEvento(DetalleSenderoEvento.AlRevisionPuntos(RevisionPuntosEvento.AlAgregarAlSendero))
        advanceUntilIdle()

        assertTrue(repositorioPuntos.puntos.value.isEmpty())
        assertFalse(viewModel.estadoVisual.value.revision.esRevisionVisible)
        assertEquals(3, viewModel.estadoVisual.value.sendero?.recorrido?.size)
        assertTrue(efectosRecibidos.contains(DetalleSenderoEfecto.MostrarAviso("Puntos agregados al sendero")))
    }

    @Test
    fun `descartar todos desde el detalle pide confirmacion y descarta`() = runTest(despachadorPrueba) {
        repositorioPuntos.puntos.value = listOf(crearPuntoCapturadoDePrueba(id = "a"))
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlRevisionPuntos(RevisionPuntosEvento.AlTocarDescartarTodos))
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.revision.esConfirmarDescartarTodosVisible)
        assertEquals(1, repositorioPuntos.puntos.value.size)

        viewModel.procesarEvento(DetalleSenderoEvento.AlRevisionPuntos(RevisionPuntosEvento.AlConfirmarDescartarTodos))
        advanceUntilIdle()

        assertTrue(repositorioPuntos.puntos.value.isEmpty())
    }

    @Test
    fun `ampliar una foto la muestra y cerrarla la oculta`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlAmpliarFoto("/fotos/mirador.jpg"))
        assertEquals("/fotos/mirador.jpg", viewModel.estadoVisual.value.fotoAmpliada)

        viewModel.procesarEvento(DetalleSenderoEvento.AlCerrarFotoAmpliada)
        assertNull(viewModel.estadoVisual.value.fotoAmpliada)
    }

    @Test
    fun `tocar publicar con conexion pide confirmacion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlAbrirMenuOpciones)

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarPublicar)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esDialogoPublicarVisible)
        assertFalse(viewModel.estadoVisual.value.esMenuOpcionesVisible)
        assertTrue(efectosRecibidos.isEmpty())
    }

    @Test
    fun `sin conexion no se pide confirmar y se avisa`() = runTest(despachadorPrueba) {
        monitorConectividad.hayConexionActual = false
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarPublicar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoPublicarVisible)
        assertEquals(
            listOf<DetalleSenderoEfecto>(
                DetalleSenderoEfecto.MostrarAviso("Necesitás conexión")
            ),
            efectosRecibidos
        )
    }

    @Test
    fun `confirmar publica el sendero y queda marcado como publicado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarPublicar)

        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarPublicar)
        advanceUntilIdle()

        assertEquals("sendero-1", repositorioPublicos.llamadasPublicar.single().id)
        val estado = viewModel.estadoVisual.value
        assertTrue(estado.estaPublicado)
        assertFalse(estado.estaPublicando)
        assertFalse(estado.esDialogoPublicarVisible)
        assertEquals(
            listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.MostrarAviso("Sendero publicado")),
            efectosRecibidos
        )
    }

    @Test
    fun `un fallo al publicar avisa y deja el sendero privado`() = runTest(despachadorPrueba) {
        repositorioPublicos.resultadoPublicar = Result.failure(IllegalStateException("sin permiso"))
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarPublicar)

        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarPublicar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.estaPublicado)
        assertFalse(viewModel.estadoVisual.value.estaPublicando)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.MostrarAviso("sin permiso")), efectosRecibidos)
    }

    @Test
    fun `cancelar la publicacion no publica nada`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarPublicar)

        viewModel.procesarEvento(DetalleSenderoEvento.AlCancelarPublicar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoPublicarVisible)
        assertTrue(repositorioPublicos.llamadasPublicar.isEmpty())
    }

    @Test
    fun `un sendero ya publicado se reconoce y se puede hacer privado`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(crearSenderoPublicoDePrueba(crearSenderoDePrueba(id = "sendero-1")))
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.estaPublicado)

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarHacerPrivado)
        assertTrue(viewModel.estadoVisual.value.esDialogoHacerPrivadoVisible)
        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarHacerPrivado)
        advanceUntilIdle()

        assertEquals(listOf("sendero-1"), repositorioPublicos.llamadasDespublicar)
        assertFalse(viewModel.estadoVisual.value.estaPublicado)
        assertEquals(
            listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.MostrarAviso("El sendero volvió a ser privado")),
            efectosRecibidos
        )
    }

    @Test
    fun `cancelar hacer privado conserva la publicacion`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(crearSenderoPublicoDePrueba(crearSenderoDePrueba(id = "sendero-1")))
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarHacerPrivado)

        viewModel.procesarEvento(DetalleSenderoEvento.AlCancelarHacerPrivado)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoHacerPrivadoVisible)
        assertTrue(viewModel.estadoVisual.value.estaPublicado)
        assertTrue(repositorioPublicos.llamadasDespublicar.isEmpty())
    }

    @Test
    fun `eliminar un sendero publicado lo retira de Explorar primero`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(crearSenderoPublicoDePrueba(crearSenderoDePrueba(id = "sendero-1")))
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminar)
        advanceUntilIdle()

        assertEquals(listOf("sendero-1"), repositorioPublicos.llamadasDespublicar)
        assertTrue(repositorioPublicos.publicos.value.isEmpty())
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.VolverAlListado), efectosRecibidos)
    }

    @Test
    fun `eliminar un sendero publicado sin conexion avisa y no elimina`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(crearSenderoPublicoDePrueba(crearSenderoDePrueba(id = "sendero-1")))
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        monitorConectividad.hayConexionActual = false

        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminar)
        advanceUntilIdle()

        assertTrue(repositorioPublicos.llamadasDespublicar.isEmpty())
        assertEquals(
            listOf<DetalleSenderoEfecto>(
                DetalleSenderoEfecto.MostrarAviso("Necesitás conexión para eliminar un sendero publicado")
            ),
            efectosRecibidos
        )
    }

    @Test
    fun `si no se puede retirar de Explorar el sendero no se elimina`() = runTest(despachadorPrueba) {
        repositorioPublicos.publicos.value = listOf(crearSenderoPublicoDePrueba(crearSenderoDePrueba(id = "sendero-1")))
        repositorioPublicos.resultadoDespublicar = Result.failure(IllegalStateException("sin permiso"))
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminar)
        advanceUntilIdle()

        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.MostrarAviso("sin permiso")), efectosRecibidos)
        assertFalse(viewModel.estadoVisual.value.estaPublicando)
    }

    @Test
    fun `eliminar un sendero privado no toca el catalogo publico`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminar)
        advanceUntilIdle()

        assertTrue(repositorioPublicos.llamadasDespublicar.isEmpty())
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.VolverAlListado), efectosRecibidos)
    }

    private fun crearPerfilDePrueba(firmaTrazado: String) = PerfilElevacion(
        puntos = listOf(PuntoPerfil(0.0, 1000.0), PuntoPerfil(1.0, 1100.0)),
        desnivelPositivoMetros = 100,
        desnivelNegativoMetros = 0,
        alturaMaximaMetros = 1100,
        alturaMinimaMetros = 1000,
        pendienteMaximaPorcentaje = 10.0,
        firmaTrazado = firmaTrazado
    )

    @Test
    fun `calcular el perfil guarda el resultado y el sendero lo recibe por el flujo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido, tipoRecorrido = TipoRecorrido.LINEAL)
        advanceUntilIdle()
        assertNull(viewModel.estadoVisual.value.sendero?.perfilElevacion)

        viewModel.procesarEvento(DetalleSenderoEvento.AlCalcularPerfil)
        assertTrue(viewModel.estadoVisual.value.estaCalculandoPerfil)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.estaCalculandoPerfil)
        assertEquals(1, fuenteElevacion.llamadas.size)
        assertTrue(viewModel.estadoVisual.value.sendero?.perfilElevacion != null)
        assertTrue(efectosRecibidos.isEmpty())
    }

    @Test
    fun `si falla el cálculo se avisa con el mensaje del dominio y se libera el botón`() = runTest(despachadorPrueba) {
        fuenteElevacion.falloDevuelto = SinConexionElevacionException(null)
        val viewModel = crearViewModel(recorridoValido, tipoRecorrido = TipoRecorrido.LINEAL)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlCalcularPerfil)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.estaCalculandoPerfil)
        assertEquals(
            listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.MostrarAviso("Sin conexión para calcular el perfil")),
            efectosRecibidos
        )
        assertNull(viewModel.estadoVisual.value.sendero?.perfilElevacion)
    }

    @Test
    fun `un segundo toque mientras se calcula el perfil se ignora`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido, tipoRecorrido = TipoRecorrido.LINEAL)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlCalcularPerfil)
        viewModel.procesarEvento(DetalleSenderoEvento.AlCalcularPerfil)
        advanceUntilIdle()

        assertEquals(1, fuenteElevacion.llamadas.size)
    }

    @Test
    fun `el perfil calculado con otro trazado se marca como desactualizado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(
            recorridoValido,
            perfilElevacion = crearPerfilDePrueba(firmaTrazado = "otro-trazado"),
            tipoRecorrido = TipoRecorrido.LINEAL
        )
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.perfilEstaDesactualizado)
    }

    @Test
    fun `el perfil del trazado vigente o la falta de perfil no se marcan como desactualizados`() = runTest(despachadorPrueba) {
        val firmaVigente = CalculadoraPerfil.firmaTrazado(recorridoValido, TipoRecorrido.LINEAL)
        val conPerfilVigente = crearViewModel(
            recorridoValido,
            perfilElevacion = crearPerfilDePrueba(firmaTrazado = firmaVigente),
            tipoRecorrido = TipoRecorrido.LINEAL
        )
        advanceUntilIdle()
        assertFalse(conPerfilVigente.estadoVisual.value.perfilEstaDesactualizado)

        val sinPerfil = crearViewModel(recorridoValido, tipoRecorrido = TipoRecorrido.LINEAL)
        advanceUntilIdle()
        assertFalse(sinPerfil.estadoVisual.value.perfilEstaDesactualizado)
    }

    @Test
    fun `tocar el perfil en el inicio enfoca el primer punto sin desplazar la lista`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido, tipoRecorrido = TipoRecorrido.LINEAL)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlSeleccionarKilometroDelPerfil(0.0))

        val estado = viewModel.estadoVisual.value
        assertEquals(-41.10, estado.puntoEnfocado?.latitud ?: Double.NaN, 0.00001)
        assertEquals(-71.30, estado.puntoEnfocado?.longitud ?: Double.NaN, 0.00001)
        assertEquals(1, estado.contadorEnfoque)
        assertFalse(estado.debeDesplazarAlMapa)
    }

    @Test
    fun `tocar el perfil más allá del final enfoca el último punto y a mitad queda en el tramo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido, tipoRecorrido = TipoRecorrido.LINEAL)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlSeleccionarKilometroDelPerfil(1_000.0))
        assertEquals(-41.11, viewModel.estadoVisual.value.puntoEnfocado?.latitud ?: Double.NaN, 0.00001)

        val largo = CalculadoraRecorrido.distanciaKilometros(recorridoValido, TipoRecorrido.LINEAL)
        viewModel.procesarEvento(DetalleSenderoEvento.AlSeleccionarKilometroDelPerfil(largo / 2))
        assertEquals(-41.105, viewModel.estadoVisual.value.puntoEnfocado?.latitud ?: Double.NaN, 0.0005)
        assertEquals(2, viewModel.estadoVisual.value.contadorEnfoque)
    }

    @Test
    fun `seleccionar un punto de interés sí desplaza la lista hasta el mapa`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido, tipoRecorrido = TipoRecorrido.LINEAL)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlSeleccionarKilometroDelPerfil(0.0))

        viewModel.procesarEvento(DetalleSenderoEvento.AlSeleccionarPuntoDeInteres(1))

        assertTrue(viewModel.estadoVisual.value.debeDesplazarAlMapa)
    }

    @Test
    fun `tocar exportar muestra los formatos`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarExportar)

        assertTrue(viewModel.estadoVisual.value.esDialogoExportarVisible)
    }

    @Test
    fun `elegir un formato pide el destino con un nombre de archivo sugerido`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarExportar)

        viewModel.procesarEvento(DetalleSenderoEvento.AlElegirFormatoExportacion(FormatoExportacion.GPX))
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoExportarVisible)
        assertEquals(
            listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.ElegirDestinoExportacion(FormatoExportacion.GPX, "Cerro Catedral.gpx")),
            efectosRecibidos
        )
    }

    @Test
    fun `exportar escribe el archivo y avisa`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlExportarSendero(FormatoExportacion.CSV, "content://destino"))
        advanceUntilIdle()

        assertTrue(escritorArchivos.escritos.getValue("content://destino").startsWith("Latitude,Longitude,Elevation"))
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.MostrarAviso("Sendero exportado")), efectosRecibidos)
    }
}
