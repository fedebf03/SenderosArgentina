package com.unaj.senderos.presentation.recorrido

import com.unaj.senderos.domain.model.ResultadoCaptura
import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.DescartarPuntosCapturadosUseCase
import com.unaj.senderos.domain.usecase.IncorporarPuntosCapturadosUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarPuntosCapturadosConDistanciaUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoControlSeguimiento
import com.unaj.senderos.falsos.FalsoLanzadorSeguimiento
import com.unaj.senderos.falsos.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearPuntoCapturadoDePrueba
import com.unaj.senderos.falsos.crearSenderoDePrueba
import com.unaj.senderos.presentation.revisionpuntos.RevisionPuntosEvento
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
class RecorridoPuntosViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val control = FalsoControlSeguimiento()
    private val lanzador = FalsoLanzadorSeguimiento()
    private val efectosRecibidos = mutableListOf<RecorridoEfecto>()
    private val trazado = listOf(
        PuntoRecorrido(latitud = -41.10, longitud = -71.30),
        PuntoRecorrido(latitud = -41.20, longitud = -71.30)
    )
    private val repositorioSenderos = FalsoSenderoRepositorio(listOf(crearSenderoDePrueba(id = "sendero-1", recorrido = trazado)))
    private val repositorioPuntos = FalsoPuntosCapturadosRepositorio().also { falso -> falso.senderoRepositorio = repositorioSenderos }

    // Dar al falso de puntos el repositorio de senderos para que incorporar emule la inserción real
    init {
        repositorioPuntos.senderoRepositorio = repositorioSenderos
    }

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): RecorridoViewModel {
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val viewModel = RecorridoViewModel(
            idSendero = "sendero-1",
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(repositorioSenderos, autenticacion),
            controlSeguimiento = control,
            lanzadorSeguimiento = lanzador,
            capacidadesDispositivo = CapacidadesDispositivo(
                tieneGps = true,
                tieneAcelerometro = true,
                tieneBrujula = true
            ),
            observarPuntosCapturadosConDistanciaUseCase = ObservarPuntosCapturadosConDistanciaUseCase(repositorioSenderos, repositorioPuntos, autenticacion),
            incorporarPuntosCapturadosUseCase = IncorporarPuntosCapturadosUseCase(repositorioPuntos, autenticacion),
            descartarPuntosCapturadosUseCase = DescartarPuntosCapturadosUseCase(repositorioPuntos, autenticacion)
        )
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
    }

    private fun crearEstado(estaDetenido: Boolean = false, esSimulado: Boolean = false): EstadoSeguimiento {
        return EstadoSeguimiento(
            idSendero = "sendero-1",
            trazado = trazado,
            posicionActual = Coordenada(latitud = -41.12, longitud = -71.30),
            instanteInicioMilisegundos = 1_000L,
            nombreSendero = "Cerro Catedral",
            estaDetenido = estaDetenido,
            esSimulado = esSimulado
        )
    }

    @Test
    fun `agregar punto esta disponible detenido o simulado y no en movimiento`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.puedeAgregarPunto)

        control.estadoEmitido.value = crearEstado(estaDetenido = false)
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.puedeAgregarPunto)

        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.puedeAgregarPunto)

        control.estadoEmitido.value = crearEstado(estaDetenido = false, esSimulado = true)
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.puedeAgregarPunto)
    }

    @Test
    fun `tocar agregar punto abre la hoja con el formulario limpio`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.esHojaPuntoVisible)
        assertEquals(TipoPunto.MIRADOR, estado.tipoPuntoElegido)
        assertEquals("", estado.notaPunto)
        assertNull(estado.fotoPunto)
        assertNull(estado.mensajeErrorPunto)
    }

    @Test
    fun `en movimiento tocar agregar punto no abre la hoja`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = false)
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        assertFalse(viewModel.estadoVisual.value.esHojaPuntoVisible)
    }

    @Test
    fun `el formulario del punto refleja tipo nota y foto`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        viewModel.procesarEvento(RecorridoEvento.AlElegirTipoPunto(TipoPunto.AGUA))
        viewModel.procesarEvento(RecorridoEvento.AlCambiarNotaPunto("Arroyo helado"))
        viewModel.procesarEvento(RecorridoEvento.AlSeleccionarFotoPunto("content://foto/1"))

        val estado = viewModel.estadoVisual.value
        assertEquals(TipoPunto.AGUA, estado.tipoPuntoElegido)
        assertEquals("Arroyo helado", estado.notaPunto)
        assertEquals("content://foto/1", estado.fotoPunto)

        viewModel.procesarEvento(RecorridoEvento.AlQuitarFotoPunto)
        assertNull(viewModel.estadoVisual.value.fotoPunto)
    }

    @Test
    fun `guardar con exito captura el punto cierra la hoja y avisa`() = runTest(despachadorPrueba) {
        control.resultadoCapturarPunto = Result.success(ResultadoCaptura(crearPuntoCapturadoDePrueba(tipo = TipoPunto.AGUA), null))
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)
        viewModel.procesarEvento(RecorridoEvento.AlElegirTipoPunto(TipoPunto.AGUA))
        viewModel.procesarEvento(RecorridoEvento.AlCambiarNotaPunto("  Arroyo helado  "))
        viewModel.procesarEvento(RecorridoEvento.AlSeleccionarFotoPunto("content://foto/1"))

        viewModel.procesarEvento(RecorridoEvento.AlGuardarPunto)
        advanceUntilIdle()

        assertEquals(listOf(Triple(TipoPunto.AGUA, "Arroyo helado", "content://foto/1" as String?)), control.capturasPedidas)
        assertFalse(viewModel.estadoVisual.value.esHojaPuntoVisible)
        assertFalse(viewModel.estadoVisual.value.estaGuardandoPunto)
        assertEquals(listOf<RecorridoEfecto>(RecorridoEfecto.MostrarAviso("Punto guardado")), efectosRecibidos)
    }

    @Test
    fun `guardar sin foto pide la captura con foto nula`() = runTest(despachadorPrueba) {
        control.resultadoCapturarPunto = Result.success(ResultadoCaptura(crearPuntoCapturadoDePrueba(), null))
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        viewModel.procesarEvento(RecorridoEvento.AlGuardarPunto)
        advanceUntilIdle()

        assertEquals(listOf(Triple(TipoPunto.MIRADOR, "", null as String?)), control.capturasPedidas)
    }

    @Test
    fun `guardar con fallo muestra el mensaje y deja la hoja abierta`() = runTest(despachadorPrueba) {
        control.resultadoCapturarPunto = Result.failure(IllegalStateException("Todavía no hay una posición GPS"))
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)
        viewModel.procesarEvento(RecorridoEvento.AlCambiarNotaPunto("Mirador"))

        viewModel.procesarEvento(RecorridoEvento.AlGuardarPunto)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.esHojaPuntoVisible)
        assertEquals("Todavía no hay una posición GPS", estado.mensajeErrorPunto)
        assertEquals("Mirador", estado.notaPunto)
        assertFalse(estado.estaGuardandoPunto)
        assertTrue(efectosRecibidos.isEmpty())
    }

    @Test
    fun `un fallo de la camara se muestra en la hoja`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        viewModel.procesarEvento(RecorridoEvento.AlFallarFotoPunto("No hay una aplicación de cámara disponible"))

        assertEquals("No hay una aplicación de cámara disponible", viewModel.estadoVisual.value.mensajeErrorPunto)
    }

    @Test
    fun `el doble toque en guardar captura una sola vez`() = runTest(despachadorPrueba) {
        control.resultadoCapturarPunto = Result.success(ResultadoCaptura(crearPuntoCapturadoDePrueba(), null))
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        viewModel.procesarEvento(RecorridoEvento.AlGuardarPunto)
        viewModel.procesarEvento(RecorridoEvento.AlGuardarPunto)
        advanceUntilIdle()

        assertEquals(1, control.capturasPedidas.size)
    }

    @Test
    fun `cerrar la hoja la oculta y limpia el error`() = runTest(despachadorPrueba) {
        control.resultadoCapturarPunto = Result.failure(IllegalStateException("Sin posición"))
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)
        viewModel.procesarEvento(RecorridoEvento.AlGuardarPunto)
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlCerrarHojaPunto)

        assertFalse(viewModel.estadoVisual.value.esHojaPuntoVisible)
        assertNull(viewModel.estadoVisual.value.mensajeErrorPunto)
    }

    @Test
    fun `el contador refleja los puntos capturados del sendero`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        assertEquals(0, viewModel.estadoVisual.value.cantidadPuntosCapturados)

        repositorioPuntos.puntos.value = listOf(
            crearPuntoCapturadoDePrueba(id = "a"),
            crearPuntoCapturadoDePrueba(id = "b", instanteMilisegundos = 2L),
            crearPuntoCapturadoDePrueba(id = "ajeno", idSendero = "otro")
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.estadoVisual.value.cantidadPuntosCapturados)
    }

    @Test
    fun `el resumen ofrece los puntos y agregarlos los incorpora al sendero`() = runTest(despachadorPrueba) {
        repositorioPuntos.puntos.value = listOf(
            crearPuntoCapturadoDePrueba(id = "a", latitud = -41.12, longitud = -71.30, instanteMilisegundos = 1L),
            crearPuntoCapturadoDePrueba(id = "b", latitud = -41.15, longitud = -71.30, instanteMilisegundos = 2L)
        )
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        advanceUntilIdle()

        val estadoResumen = viewModel.estadoVisual.value
        assertTrue(estadoResumen.resumen != null)
        assertEquals(setOf("a", "b"), estadoResumen.revision.idsMarcados)

        viewModel.procesarEvento(RecorridoEvento.AlRevisarPuntos(RevisionPuntosEvento.AlAlternarPunto("b")))
        viewModel.procesarEvento(RecorridoEvento.AlRevisarPuntos(RevisionPuntosEvento.AlAgregarAlSendero))
        advanceUntilIdle()

        assertEquals(3, repositorioSenderos.llamadasGuardarSendero.single().second.recorrido.size)
        assertTrue(repositorioPuntos.puntos.value.isEmpty())
        assertTrue(efectosRecibidos.contains(RecorridoEfecto.MostrarAviso("Puntos agregados al sendero")))
    }

    @Test
    fun `sin puntos capturados el resumen no ofrece revision`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.resumen != null)
        assertFalse(viewModel.estadoVisual.value.revision.hayPuntosPorRevisar)
    }

    @Test
    fun `guardar con senal debil avisa la antiguedad de la posicion`() = runTest(despachadorPrueba) {
        control.resultadoCapturarPunto = Result.success(ResultadoCaptura(crearPuntoCapturadoDePrueba(), 12_400L))
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        viewModel.procesarEvento(RecorridoEvento.AlGuardarPunto)
        advanceUntilIdle()

        assertEquals(
            listOf<RecorridoEfecto>(RecorridoEfecto.MostrarAviso("Punto guardado con la posición de hace 12 s (señal débil)")),
            efectosRecibidos
        )
    }

    @Test
    fun `un error tecnico al guardar muestra el texto de respaldo`() = runTest(despachadorPrueba) {
        control.resultadoCapturarPunto = Result.failure(RuntimeException("SQLiteException: disk I/O error"))
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        viewModel.procesarEvento(RecorridoEvento.AlGuardarPunto)
        advanceUntilIdle()

        assertEquals("No se pudo guardar el punto", viewModel.estadoVisual.value.mensajeErrorPunto)
    }

    @Test
    fun `finalizar cierra la hoja de captura`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esHojaPuntoVisible)
    }

    @Test
    fun `cancelar el recorrido cierra la hoja de captura`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        viewModel.procesarEvento(RecorridoEvento.AlConfirmarCancelarRecorrido)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esHojaPuntoVisible)
    }

    @Test
    fun `una cancelacion externa cierra la hoja de captura`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        control.motivoFinEmitido.value = MotivoFinSeguimiento.CANCELADO_POR_EL_USUARIO
        control.estadoEmitido.value = null
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esHojaPuntoVisible)
    }

    @Test
    fun `un fin externo con resumen cierra la hoja de captura`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDetenido = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarAgregarPunto)

        control.estadoEmitido.value = null
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.resumen != null)
        assertFalse(viewModel.estadoVisual.value.esHojaPuntoVisible)
    }
}
