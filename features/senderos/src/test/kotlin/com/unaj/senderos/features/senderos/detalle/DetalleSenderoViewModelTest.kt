package com.unaj.senderos.presentation.detalle

import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.usecase.DescargarMapaSinConexionUseCase
import com.unaj.senderos.domain.usecase.EliminarMapaSinConexionUseCase
import com.unaj.senderos.domain.usecase.ObservarMapaSinConexionUseCase
import com.unaj.senderos.falsos.FalsoGestorMapasSinConexion
import com.unaj.senderos.falsos.FalsoMonitorConectividad
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.domain.usecase.DescartarPuntosCapturadosUseCase
import com.unaj.senderos.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.domain.usecase.IncorporarPuntosCapturadosUseCase
import com.unaj.senderos.domain.usecase.ObservarPuntosCapturadosConDistanciaUseCase
import com.unaj.senderos.falsos.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.falsos.crearPuntoCapturadoDePrueba
import com.unaj.senderos.presentation.revisionpuntos.RevisionPuntosEvento
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoControlSeguimiento
import com.unaj.senderos.falsos.FalsoLanzadorSeguimiento
import com.unaj.senderos.falsos.FalsoPreferenciasAplicacion
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearSenderoDePrueba
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
        tieneBrujula = true,
        tieneCamara = true
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

    private fun crearViewModel(recorrido: List<PuntoRecorrido>): DetalleSenderoViewModel {
        val repositorio = FalsoSenderoRepositorio(
            listOf(
                crearSenderoDePrueba(id = "sendero-1", recorrido = recorrido),
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
            descartarPuntosCapturadosUseCase = DescartarPuntosCapturadosUseCase(repositorioPuntos, autenticacion)
        )
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
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
            descartarPuntosCapturadosUseCase = DescartarPuntosCapturadosUseCase(repositorioPuntos, autenticacion)
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
            PuntoRecorrido(latitud = -41.11, longitud = -71.31, tipo = com.unaj.senderos.domain.model.TipoPunto.AGUA)
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

        assertEquals(com.unaj.senderos.domain.model.Coordenada(-41.11, -71.31), primero.puntoEnfocado)
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
}
