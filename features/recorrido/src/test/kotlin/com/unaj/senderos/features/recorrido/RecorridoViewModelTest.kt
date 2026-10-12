package com.unaj.senderos.presentation.recorrido

import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoControlSeguimiento
import com.unaj.senderos.falsos.FalsoLanzadorSeguimiento
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearSenderoDePrueba
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

@OptIn(ExperimentalCoroutinesApi::class)
class RecorridoViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val control = FalsoControlSeguimiento()
    private val lanzador = FalsoLanzadorSeguimiento()
    private val efectosRecibidos = mutableListOf<RecorridoEfecto>()
    private var relojActual = 0L

    private val mirador = PuntoRecorrido(
        latitud = -41.11,
        longitud = -71.31,
        tipo = TipoPunto.MIRADOR,
        nombre = "Mirador del lago"
    )
    private val trazado = listOf(PuntoRecorrido(latitud = -41.10, longitud = -71.30), mirador)

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

    // Crear el ViewModel de un sendero con recorrido y recolectar sus efectos en una lista
    private fun crearViewModel(): RecorridoViewModel {
        val repositorio = FalsoSenderoRepositorio(listOf(crearSenderoDePrueba(id = "sendero-1", recorrido = trazado)))
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val viewModel = RecorridoViewModel(
            idSendero = "sendero-1",
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            controlSeguimiento = control,
            lanzadorSeguimiento = lanzador,
            reloj = { relojActual }
        )
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
    }

    // Construir un estado de seguimiento del sendero indicado con valores de prueba
    private fun crearEstado(
        idSendero: String = "sendero-1",
        estaDesviado: Boolean = false,
        rumboGrados: Float? = null,
        esSimulado: Boolean = false,
        multiplicadorSimulacion: Int = 1,
        llegoAlFinal: Boolean = false
    ): EstadoSeguimiento {
        return EstadoSeguimiento(
            idSendero = idSendero,
            trazado = trazado,
            posicionActual = Coordenada(latitud = -41.105, longitud = -71.305),
            distanciaRecorridaKilometros = 1.5,
            distanciaRestanteKilometros = 3.25,
            indiceProximoPunto = 1,
            estaDesviado = estaDesviado,
            rumboGrados = rumboGrados,
            esSimulado = esSimulado,
            multiplicadorSimulacion = multiplicadorSimulacion,
            instanteInicioMilisegundos = 1_000L,
            nombreSendero = "Cerro Catedral",
            llegoAlFinal = llegoAlFinal
        )
    }

    // Verificar que mientras el servicio arranca la pantalla queda en la fase de inicio
    @Test
    fun `sin estado al abrir la fase es iniciando`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        runCurrent()

        assertEquals(FaseRecorrido.INICIANDO, viewModel.estadoVisual.value.fase)
        assertNotNull(viewModel.estadoVisual.value.sendero)
    }

    // Verificar que si el servicio nunca publica un estado se declara el recorrido inactivo
    @Test
    fun `si nunca llega un estado pasa a no activo tras el plazo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertEquals(FaseRecorrido.NO_ACTIVO, viewModel.estadoVisual.value.fase)
    }

    // Verificar que un estado del sendero se refleja en las magnitudes del panel
    @Test
    fun `un estado del sendero pasa a en curso con sus magnitudes`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        val estadoVisual = viewModel.estadoVisual.value
        assertEquals(FaseRecorrido.EN_CURSO, estadoVisual.fase)
        assertEquals(1.5, estadoVisual.distanciaRecorridaKilometros, 0.0001)
        assertEquals(3.25, estadoVisual.distanciaRestanteKilometros, 0.0001)
        assertEquals(Coordenada(latitud = -41.105, longitud = -71.305), estadoVisual.posicionUsuario)
        assertEquals(mirador, estadoVisual.proximoPunto)
        assertEquals(1_000L, estadoVisual.instanteInicioMilisegundos)
        assertFalse(estadoVisual.estaDesviado)
    }

    // Verificar que el desvío del estado llega al estado visual
    @Test
    fun `un estado desviado se refleja en el estado visual`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDesviado = true)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.estaDesviado)
    }

    // Verificar que el estado de otro sendero no se muestra en esta pantalla
    @Test
    fun `un estado de otro sendero se ignora`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(idSendero = "otro-sendero")
        advanceUntilIdle()

        assertEquals(FaseRecorrido.NO_ACTIVO, viewModel.estadoVisual.value.fase)
        assertNull(viewModel.estadoVisual.value.posicionUsuario)
    }

    // Verificar que si el seguimiento termina por fuera (por ejemplo desde la notificación) se muestra el resumen del último estado
    @Test
    fun `si el estado vuelve a null sin finalizar muestra el resumen del ultimo estado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()
        relojActual = 31_000L

        control.estadoEmitido.value = null
        advanceUntilIdle()

        val estadoVisual = viewModel.estadoVisual.value
        assertEquals(FaseRecorrido.NO_ACTIVO, estadoVisual.fase)
        assertEquals(
            ResumenRecorrido(distanciaRecorridaKilometros = 1.5, tiempoTranscurridoMilisegundos = 30_000L),
            estadoVisual.resumen
        )
        assertEquals(0, lanzador.cantidadDetenciones)
    }

    // Verificar que un aviso de proximidad se emite como aviso con el nombre del punto
    @Test
    fun `un aviso de proximidad se muestra con el nombre del punto`() = runTest(despachadorPrueba) {
        crearViewModel()
        runCurrent()

        control.avisosEmitidos.tryEmit(AvisoRecorrido.CercaDePunto(indice = 1, punto = mirador))
        advanceUntilIdle()

        assertEquals(listOf<RecorridoEfecto>(RecorridoEfecto.MostrarAviso("Cerca de Mirador del lago")), efectosRecibidos)
    }

    // Verificar que el aviso de desvío se emite como aviso
    @Test
    fun `un aviso de desvio se muestra`() = runTest(despachadorPrueba) {
        crearViewModel()
        runCurrent()

        control.avisosEmitidos.tryEmit(AvisoRecorrido.Desvio)
        advanceUntilIdle()

        assertEquals(listOf<RecorridoEfecto>(RecorridoEfecto.MostrarAviso("Te alejaste del sendero")), efectosRecibidos)
    }

    // Verificar que tocar Finalizar solo abre la confirmación y no detiene nada
    @Test
    fun `tocar finalizar abre la confirmacion sin detener`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlTocarFinalizar)

        assertTrue(viewModel.estadoVisual.value.esDialogoFinalizarVisible)
        assertEquals(0, lanzador.cantidadDetenciones)
    }

    // Verificar que cancelar la confirmación la cierra sin detener
    @Test
    fun `cancelar la confirmacion no detiene el seguimiento`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarFinalizar)

        viewModel.procesarEvento(RecorridoEvento.AlCancelarFinalizar)

        assertFalse(viewModel.estadoVisual.value.esDialogoFinalizarVisible)
        assertEquals(0, lanzador.cantidadDetenciones)
    }

    // Verificar que confirmar detiene el seguimiento y captura el resumen con distancia y tiempo
    @Test
    fun `confirmar finalizar detiene y captura el resumen`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarFinalizar)
        relojActual = 61_000L

        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        advanceUntilIdle()

        val estadoVisual = viewModel.estadoVisual.value
        assertEquals(1, lanzador.cantidadDetenciones)
        assertFalse(estadoVisual.esDialogoFinalizarVisible)
        assertEquals(
            ResumenRecorrido(distanciaRecorridaKilometros = 1.5, tiempoTranscurridoMilisegundos = 60_000L),
            estadoVisual.resumen
        )
    }

    // Verificar que el resumen sobrevive al estado null que deja el control al detenerse
    @Test
    fun `el resumen se conserva cuando el control queda en null`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        control.estadoEmitido.value = null
        advanceUntilIdle()

        assertNotNull(viewModel.estadoVisual.value.resumen)
    }

    // Verificar que cerrar el resumen vuelve al detalle
    @Test
    fun `cerrar el resumen vuelve al detalle`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)

        viewModel.procesarEvento(RecorridoEvento.AlCerrarResumen)
        advanceUntilIdle()

        assertEquals(listOf<RecorridoEfecto>(RecorridoEfecto.VolverAlDetalle), efectosRecibidos)
    }

    // Verificar que volver con Atrás deja el seguimiento corriendo
    @Test
    fun `volver no detiene el seguimiento`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlVolver)
        advanceUntilIdle()

        assertEquals(0, lanzador.cantidadDetenciones)
        assertEquals(listOf<RecorridoEfecto>(RecorridoEfecto.VolverAlDetalle), efectosRecibidos)
    }

    // Verificar que la cámara sigue al usuario desde el comienzo
    @Test
    fun `al abrir la camara esta siguiendo al usuario`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        runCurrent()

        assertTrue(viewModel.estadoVisual.value.estaSiguiendo)
    }

    // Verificar que mover el mapa con el dedo sale del modo seguimiento y Centrar lo reactiva
    @Test
    fun `el gesto del usuario deja de seguir y centrar vuelve a seguir`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        runCurrent()

        viewModel.procesarEvento(RecorridoEvento.AlMoverMapaConGesto)
        assertFalse(viewModel.estadoVisual.value.estaSiguiendo)

        viewModel.procesarEvento(RecorridoEvento.AlTocarCentrar)
        assertTrue(viewModel.estadoVisual.value.estaSiguiendo)
    }

    // Verificar que los estados nuevos no reactivan el seguimiento que el usuario apagó
    @Test
    fun `un estado nuevo no reactiva el seguimiento`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlMoverMapaConGesto)

        control.estadoEmitido.value = crearEstado(rumboGrados = 45f)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.estaSiguiendo)
    }

    // Verificar que el rumbo del estado llega al estado visual
    @Test
    fun `el rumbo del estado se refleja en el estado visual`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(rumboGrados = 90f)
        advanceUntilIdle()

        assertEquals(90f, viewModel.estadoVisual.value.rumboGrados)
    }

    // Verificar que un recorrido real no se marca como simulado
    @Test
    fun `un recorrido real no se marca como simulado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esSimulado)
    }

    // Verificar que un ViewModel nuevo muestra el multiplicador vigente del control
    @Test
    fun `un viewmodel nuevo muestra el multiplicador vigente del control`() = runTest(despachadorPrueba) {
        control.estadoEmitido.value = crearEstado(esSimulado = true, multiplicadorSimulacion = 5)
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertEquals(5, viewModel.estadoVisual.value.multiplicadorSimulacion)
    }

    // Verificar que el estado simulado se refleja en el estado visual
    @Test
    fun `un recorrido simulado se marca como simulado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(esSimulado = true)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esSimulado)
    }

    // Verificar que elegir una velocidad la pide al control y que se muestra cuando el control la publica
    @Test
    fun `elegir una velocidad la pide al control y se muestra la vigente`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(esSimulado = true)
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlSeleccionarMultiplicador(20))
        assertEquals(listOf(20), control.multiplicadoresCambiados)

        control.estadoEmitido.value = crearEstado(esSimulado = true, multiplicadorSimulacion = 20)
        advanceUntilIdle()
        assertEquals(20, viewModel.estadoVisual.value.multiplicadorSimulacion)
    }

    // Verificar que un multiplicador no permitido se ignora
    @Test
    fun `un multiplicador no permitido se ignora`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(esSimulado = true)
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlSeleccionarMultiplicador(7))

        assertTrue(control.multiplicadoresCambiados.isEmpty())
    }

    // Verificar que simular el desvío se delega al control solo en recorridos simulados
    @Test
    fun `simular desvio se delega al control solo si es simulado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(esSimulado = false)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarSimularDesvio)
        assertEquals(0, control.cantidadDesviosSimulados)

        control.estadoEmitido.value = crearEstado(esSimulado = true)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarSimularDesvio)

        assertEquals(1, control.cantidadDesviosSimulados)
    }

    // Verificar que si el seguimiento termina por un permiso revocado se muestra el resumen y el motivo
    @Test
    fun `si el permiso se revoca muestra el resumen y avisa el motivo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        control.motivoFinEmitido.value = MotivoFinSeguimiento.PERMISO_DE_UBICACION_REVOCADO
        control.estadoEmitido.value = null
        advanceUntilIdle()

        assertNotNull(viewModel.estadoVisual.value.resumen)
        assertEquals(
            listOf<RecorridoEfecto>(RecorridoEfecto.MostrarAviso("Se revocó el permiso de ubicación")),
            efectosRecibidos
        )
    }

    // Verificar que un error de ubicación se avisa con su texto
    @Test
    fun `si falla la ubicacion avisa el motivo`() = runTest(despachadorPrueba) {
        crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        control.motivoFinEmitido.value = MotivoFinSeguimiento.ERROR_DE_UBICACION
        control.estadoEmitido.value = null
        advanceUntilIdle()

        assertEquals(
            listOf<RecorridoEfecto>(RecorridoEfecto.MostrarAviso("No se pudo obtener la ubicación")),
            efectosRecibidos
        )
    }

    // Verificar el formato de tiempo en minutos y segundos
    @Test
    fun `el tiempo menor a una hora se formatea mm ss`() {
        assertEquals("01:05", formatearTiempoTranscurrido(65_000L))
        assertEquals("00:00", formatearTiempoTranscurrido(-5L))
    }

    // Verificar el formato de tiempo con horas
    @Test
    fun `el tiempo desde una hora se formatea h mm ss`() {
        assertEquals("1:02:05", formatearTiempoTranscurrido(3_725_000L))
    }

    // Verificar el formato de distancias en metros y kilómetros
    @Test
    fun `la distancia se formatea en metros o kilometros`() {
        assertEquals("850 m", formatearDistanciaRecorrido(0.85))
        assertEquals("1,5 km", formatearDistanciaRecorrido(1.5))
    }

    // Verificar la etiqueta de un punto con nombre y sin nombre
    @Test
    fun `la etiqueta usa el nombre o el tipo del punto`() {
        assertEquals("Mirador del lago", obtenerEtiquetaPunto(mirador))
        assertEquals("Refugio", obtenerEtiquetaPunto(PuntoRecorrido(0.0, 0.0, TipoPunto.REFUGIO)))
    }

    // Verificar que la llegada al final abre el diálogo y no muestra el aviso en el Snackbar
    @Test
    fun `la llegada al final muestra el dialogo sin snackbar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        control.avisosEmitidos.tryEmit(AvisoRecorrido.LlegadaAlFinal)
        control.estadoEmitido.value = crearEstado(llegoAlFinal = true)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esDialogoLlegadaVisible)
        assertEquals("Cerro Catedral", viewModel.estadoVisual.value.nombreSendero)
        assertTrue(efectosRecibidos.isEmpty())
    }

    // Verificar que "Seguir caminando" cierra el diálogo y no reaparece en este recorrido
    @Test
    fun `seguir caminando cierra el dialogo y no reaparece`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(llegoAlFinal = true)
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.esDialogoLlegadaVisible)

        viewModel.procesarEvento(RecorridoEvento.AlSeguirCaminando)
        control.estadoEmitido.value = crearEstado(llegoAlFinal = true, esSimulado = true)
        control.avisosEmitidos.tryEmit(AvisoRecorrido.LlegadaAlFinal)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoLlegadaVisible)
        assertTrue(viewModel.estadoVisual.value.llegoAlFinal)
    }

    // Verificar que "Finalizar recorrido" desde el diálogo detiene el seguimiento y muestra el resumen
    @Test
    fun `finalizar desde el dialogo de llegada muestra el resumen y detiene`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(llegoAlFinal = true)
        advanceUntilIdle()
        relojActual = 61_000L

        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        advanceUntilIdle()

        val estadoVisual = viewModel.estadoVisual.value
        assertEquals(1, lanzador.cantidadDetenciones)
        assertFalse(estadoVisual.esDialogoLlegadaVisible)
        assertNotNull(estadoVisual.resumen)
    }

    // Verificar que al recrear el ViewModel con la llegada ya registrada se muestra el diálogo
    @Test
    fun `al abrir con llegada registrada se muestra el dialogo`() = runTest(despachadorPrueba) {
        control.estadoEmitido.value = crearEstado(llegoAlFinal = true)
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esDialogoLlegadaVisible)
    }
}
