package com.unaj.senderos.features.recorrido

import com.unaj.senderos.core.ui.comun.formatearTiempoTranscurrido
import com.unaj.senderos.core.ui.componentes.interpolarAltitud
import com.unaj.senderos.core.domain.model.AvisoRecorrido
import com.unaj.senderos.core.domain.model.CalidadSenal
import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.DescartarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.IncorporarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarPuntosCapturadosConDistanciaUseCase
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoControlSeguimiento
import com.unaj.senderos.core.testing.FalsoLanzadorSeguimiento
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoPerfil
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
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
    private val registroLlamadas = mutableListOf<String>()
    private val control = FalsoControlSeguimiento(registroLlamadas)
    private val lanzador = FalsoLanzadorSeguimiento(registroLlamadas)
    private val efectosRecibidos = mutableListOf<RecorridoEfecto>()
    private var relojActual = 0L

    private val mirador = PuntoRecorrido(
        latitud = -41.11,
        longitud = -71.31,
        tipo = TipoPunto.MIRADOR,
        nombre = "Mirador del lago"
    )
    private val capacidadesCompletas = CapacidadesDispositivo(
        tieneGps = true,
        tieneAcelerometro = true,
        tieneBrujula = true
    )
    private val trazado = listOf(PuntoRecorrido(latitud = -41.10, longitud = -71.30), mirador)

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(
        capacidades: CapacidadesDispositivo = capacidadesCompletas,
        sendero: Sendero = crearSenderoDePrueba(id = "sendero-1", recorrido = trazado)
    ): RecorridoViewModel {
        val repositorio = FalsoSenderoRepositorio(listOf(sendero))
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val viewModel = RecorridoViewModel(
            idSendero = "sendero-1",
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            controlSeguimiento = control,
            lanzadorSeguimiento = lanzador,
            capacidadesDispositivo = capacidades,
            observarPuntosCapturadosConDistanciaUseCase = ObservarPuntosCapturadosConDistanciaUseCase(repositorio, FalsoPuntosCapturadosRepositorio(), autenticacion),
            incorporarPuntosCapturadosUseCase = IncorporarPuntosCapturadosUseCase(FalsoPuntosCapturadosRepositorio(), autenticacion),
            descartarPuntosCapturadosUseCase = DescartarPuntosCapturadosUseCase(FalsoPuntosCapturadosRepositorio(), autenticacion),
            reloj = { relojActual }
        )
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
    }

    private fun crearEstado(
        idSendero: String = "sendero-1",
        estaDesviado: Boolean = false,
        rumboGrados: Float? = null,
        esSimulado: Boolean = false,
        multiplicadorSimulacion: Int = 1,
        llegoAlFinal: Boolean = false,
        velocidadMetrosPorSegundo: Float? = null,
        pasos: Int? = null,
        ritmoPasosPorMinuto: Double? = null,
        estaDetenido: Boolean = false,
        rumboBrujulaGrados: Float? = null,
        tiempoTranscurridoMilisegundos: Long = 0L,
        calidadSenal: CalidadSenal = CalidadSenal.BUENA
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
            llegoAlFinal = llegoAlFinal,
            velocidadMetrosPorSegundo = velocidadMetrosPorSegundo,
            pasos = pasos,
            ritmoPasosPorMinuto = ritmoPasosPorMinuto,
            estaDetenido = estaDetenido,
            rumboBrujulaGrados = rumboBrujulaGrados,
            tiempoTranscurridoMilisegundos = tiempoTranscurridoMilisegundos,
            calidadSenal = calidadSenal
        )
    }

    @Test
    fun `sin estado al abrir la fase es iniciando`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        runCurrent()

        assertEquals(FaseRecorrido.INICIANDO, viewModel.estadoVisual.value.fase)
        assertNotNull(viewModel.estadoVisual.value.sendero)
    }

    @Test
    fun `si nunca llega un estado pasa a no activo tras el plazo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertEquals(FaseRecorrido.NO_ACTIVO, viewModel.estadoVisual.value.fase)
    }

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

    @Test
    fun `el trazado caminado del seguimiento se refleja para dibujarlo en vivo`() = runTest(despachadorPrueba) {
        val trazadoCaminado = listOf(
            Coordenada(latitud = -41.100, longitud = -71.300),
            Coordenada(latitud = -41.105, longitud = -71.305)
        )
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado().copy(trazadoCaminado = trazadoCaminado)
        advanceUntilIdle()

        assertEquals(trazadoCaminado, viewModel.estadoVisual.value.trazadoCaminado)
    }

    @Test
    fun `un estado desviado se refleja en el estado visual`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(estaDesviado = true)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.estaDesviado)
    }

    @Test
    fun `un estado de otro sendero se ignora`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(idSendero = "otro-sendero")
        advanceUntilIdle()

        assertEquals(FaseRecorrido.NO_ACTIVO, viewModel.estadoVisual.value.fase)
        assertNull(viewModel.estadoVisual.value.posicionUsuario)
    }

    // El seguimiento puede terminar por fuera, por ejemplo desde la notificación
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

    @Test
    fun `un aviso de proximidad se muestra con el nombre del punto`() = runTest(despachadorPrueba) {
        crearViewModel()
        runCurrent()

        control.avisosEmitidos.tryEmit(AvisoRecorrido.CercaDePunto(indice = 1, punto = mirador))
        advanceUntilIdle()

        assertEquals(listOf<RecorridoEfecto>(RecorridoEfecto.MostrarAviso("Cerca de Mirador del lago")), efectosRecibidos)
    }

    @Test
    fun `un aviso de desvio se muestra`() = runTest(despachadorPrueba) {
        crearViewModel()
        runCurrent()

        control.avisosEmitidos.tryEmit(AvisoRecorrido.Desvio)
        advanceUntilIdle()

        assertEquals(listOf<RecorridoEfecto>(RecorridoEfecto.MostrarAviso("Te alejaste del sendero")), efectosRecibidos)
    }

    @Test
    fun `tocar finalizar abre la confirmacion sin detener`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlTocarFinalizar)

        assertTrue(viewModel.estadoVisual.value.esDialogoFinalizarVisible)
        assertEquals(0, lanzador.cantidadDetenciones)
    }

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

    @Test
    fun `al abrir la camara esta siguiendo al usuario`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        runCurrent()

        assertTrue(viewModel.estadoVisual.value.estaSiguiendo)
    }

    @Test
    fun `el gesto del usuario deja de seguir y centrar vuelve a seguir`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        runCurrent()

        viewModel.procesarEvento(RecorridoEvento.AlMoverMapaConGesto)
        assertFalse(viewModel.estadoVisual.value.estaSiguiendo)

        viewModel.procesarEvento(RecorridoEvento.AlTocarCentrar)
        assertTrue(viewModel.estadoVisual.value.estaSiguiendo)
    }

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

    @Test
    fun `el rumbo del estado se refleja en el estado visual`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(rumboGrados = 90f)
        advanceUntilIdle()

        assertEquals(90f, viewModel.estadoVisual.value.rumboGrados)
    }

    @Test
    fun `un recorrido real no se marca como simulado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esSimulado)
    }

    @Test
    fun `un viewmodel nuevo muestra el multiplicador vigente del control`() = runTest(despachadorPrueba) {
        control.estadoEmitido.value = crearEstado(esSimulado = true, multiplicadorSimulacion = 5)
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertEquals(5, viewModel.estadoVisual.value.multiplicadorSimulacion)
    }

    @Test
    fun `un recorrido simulado se marca como simulado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(esSimulado = true)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esSimulado)
    }

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

    @Test
    fun `un multiplicador no permitido se ignora`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(esSimulado = true)
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlSeleccionarMultiplicador(7))

        assertTrue(control.multiplicadoresCambiados.isEmpty())
    }

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

    @Test
    fun `el tiempo menor a una hora se formatea mm ss`() {
        assertEquals("01:05", formatearTiempoTranscurrido(65_000L))
        assertEquals("00:00", formatearTiempoTranscurrido(-5L))
    }

    @Test
    fun `el tiempo desde una hora se formatea h mm ss`() {
        assertEquals("1:02:05", formatearTiempoTranscurrido(3_725_000L))
    }

    @Test
    fun `la etiqueta usa el nombre o el tipo del punto`() {
        assertEquals("Mirador del lago", obtenerEtiquetaPunto(mirador))
        assertEquals("Refugio", obtenerEtiquetaPunto(PuntoRecorrido(0.0, 0.0, TipoPunto.REFUGIO)))
    }

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

    @Test
    fun `los sensores del estado se reflejan y detenido usa el rumbo de la brujula`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(
            rumboGrados = 90f,
            velocidadMetrosPorSegundo = 1.5f,
            pasos = 120,
            ritmoPasosPorMinuto = 95.0,
            estaDetenido = true,
            rumboBrujulaGrados = 270f,
            tiempoTranscurridoMilisegundos = 3_600_000L
        )
        advanceUntilIdle()

        val estadoVisual = viewModel.estadoVisual.value
        assertEquals(270f, estadoVisual.rumboGrados)
        assertEquals(1.5f, estadoVisual.velocidadActualMetrosPorSegundo)
        assertEquals(1.5, estadoVisual.velocidadPromedioKilometrosPorHora, 0.0001)
        assertEquals(120, estadoVisual.pasos)
        assertEquals(95.0, estadoVisual.ritmoPasosPorMinuto)
        assertTrue(estadoVisual.estaDetenido)
    }

    @Test
    fun `en movimiento el rumbo es el del gps`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(rumboGrados = 90f, estaDetenido = false, rumboBrujulaGrados = 270f)
        advanceUntilIdle()

        assertEquals(90f, viewModel.estadoVisual.value.rumboGrados)
        assertFalse(viewModel.estadoVisual.value.estaDetenido)
    }

    @Test
    fun `las capacidades del equipo se reflejan en el estado visual`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(capacidadesCompletas.copy(tieneAcelerometro = false, tieneBrujula = false))
        runCurrent()

        assertFalse(viewModel.estadoVisual.value.tieneAcelerometro)
        assertFalse(viewModel.estadoVisual.value.tieneBrujula)
    }

    @Test
    fun `tocar cancelar recorrido abre la confirmacion sin cancelar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlTocarCancelarRecorrido)

        assertEquals(0, lanzador.cantidadCancelaciones)
        assertTrue(viewModel.estadoVisual.value.esDialogoCancelarVisible)
    }

    @Test
    fun `descartar la confirmacion de cancelar no cancela`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarCancelarRecorrido)

        viewModel.procesarEvento(RecorridoEvento.AlDescartarCancelacion)

        assertFalse(viewModel.estadoVisual.value.esDialogoCancelarVisible)
        assertEquals(0, lanzador.cantidadCancelaciones)
    }

    @Test
    fun `confirmar cancelar descarta sin resumen y vuelve al detalle`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlTocarCancelarRecorrido)

        viewModel.procesarEvento(RecorridoEvento.AlConfirmarCancelarRecorrido)
        control.estadoEmitido.value = null
        advanceUntilIdle()

        val estadoVisual = viewModel.estadoVisual.value
        assertEquals(1, control.cantidadCancelaciones)
        assertEquals(0, lanzador.cantidadCancelaciones)
        assertNull(estadoVisual.resumen)
        assertFalse(estadoVisual.esDialogoCancelarVisible)
        assertEquals(listOf<RecorridoEfecto>(RecorridoEfecto.VolverAlDetalle), efectosRecibidos)
    }

    // Se cancela en el control antes de detener el servicio para que no quede nada por guardar
    @Test
    fun `cancelar descarta en el control antes de detener el servicio`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlConfirmarCancelarRecorrido)
        advanceUntilIdle()

        assertEquals(listOf("control.cancelar", "lanzador.detener"), registroLlamadas)
    }

    @Test
    fun `una cancelacion externa vuelve sin resumen y avisa`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        control.cancelar()
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.resumen)
        assertEquals(
            listOf<RecorridoEfecto>(
                RecorridoEfecto.VolverAlDetalle,
                RecorridoEfecto.MostrarAviso("El recorrido fue cancelado")
            ),
            efectosRecibidos
        )
    }

    @Test
    fun `la cancelacion propia no emite aviso`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado()
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlConfirmarCancelarRecorrido)
        advanceUntilIdle()

        assertEquals(listOf<RecorridoEfecto>(RecorridoEfecto.VolverAlDetalle), efectosRecibidos)
    }

    @Test
    fun `buscando senal se refleja y los datos de distancia no son confiables`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(calidadSenal = CalidadSenal.BUSCANDO)
        advanceUntilIdle()

        assertEquals(CalidadSenal.BUSCANDO, viewModel.estadoVisual.value.calidadSenal)
        assertFalse(viewModel.estadoVisual.value.sonConfiablesLosDatosDeDistancia)
    }

    @Test
    fun `senal debil se refleja y los datos de distancia no son confiables`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(calidadSenal = CalidadSenal.DEBIL)
        advanceUntilIdle()

        assertEquals(CalidadSenal.DEBIL, viewModel.estadoVisual.value.calidadSenal)
        assertFalse(viewModel.estadoVisual.value.sonConfiablesLosDatosDeDistancia)
    }

    @Test
    fun `buena senal deja los datos de distancia confiables`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = crearEstado(calidadSenal = CalidadSenal.DEBIL)
        advanceUntilIdle()
        control.estadoEmitido.value = crearEstado(calidadSenal = CalidadSenal.BUENA)
        advanceUntilIdle()

        assertEquals(CalidadSenal.BUENA, viewModel.estadoVisual.value.calidadSenal)
        assertTrue(viewModel.estadoVisual.value.sonConfiablesLosDatosDeDistancia)
    }

    @Test
    fun `en simulacion no se marca la falta de acelerometro`() {
        val estadoVisual = RecorridoEstadoVisual(tieneAcelerometro = false, esSimulado = true)

        assertFalse(estadoVisual.faltaAcelerometro)
        assertTrue(estadoVisual.copy(esSimulado = false).faltaAcelerometro)
    }

    @Test
    fun `el rumbo se formatea como punto cardinal y grados`() {
        assertEquals("NE · 45°", formatearRumbo(45f))
        assertEquals("N · 0°", formatearRumbo(359.6f))
        assertEquals("—", formatearRumbo(null))
    }

    @Test
    fun `al abrir con llegada registrada se muestra el dialogo`() = runTest(despachadorPrueba) {
        control.estadoEmitido.value = crearEstado(llegoAlFinal = true)
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esDialogoLlegadaVisible)
    }

    private val firmaVigente = CalculadoraPerfil.firmaTrazado(trazado, TipoRecorrido.LINEAL)
    private val largoDelTrazado = CalculadoraRecorrido.distanciaKilometros(trazado, TipoRecorrido.LINEAL)

    private fun crearSenderoConPerfil(conPerfil: Boolean, firmaDelTrazado: String = firmaVigente): Sendero {
        val perfil = PerfilElevacion(
            puntos = (0..10).map { indice ->
                PuntoPerfil(largoDelTrazado * indice / 10, 1000.0 + indice * (if (indice % 2 == 0) 30.0 else 10.0))
            },
            desnivelPositivoMetros = 100,
            desnivelNegativoMetros = 0,
            alturaMaximaMetros = 1300,
            alturaMinimaMetros = 1000,
            pendienteMaximaPorcentaje = 12.0,
            firmaTrazado = firmaDelTrazado
        )
        return crearSenderoDePrueba(
            id = "sendero-1",
            recorrido = trazado,
            tipoRecorrido = TipoRecorrido.LINEAL,
            perfilElevacion = if (conPerfil) perfil else null
        )
    }

    private fun crearEstadoConAvance(kilometrosRecorridos: Double) =
        crearEstado().copy(distanciaRestanteKilometros = largoDelTrazado - kilometrosRecorridos)

    @Test
    fun `con perfil el kilometro en el trazado es el largo menos lo que falta`() = runTest(despachadorPrueba) {
        val sendero = crearSenderoConPerfil(conPerfil = true)
        val viewModel = crearViewModel(sendero = sendero)
        control.estadoEmitido.value = crearEstadoConAvance(kilometrosRecorridos = 0.5)
        advanceUntilIdle()

        val estadoVisual = viewModel.estadoVisual.value
        assertEquals(0.5, estadoVisual.kilometroEnTrazado ?: Double.NaN, 0.0001)
        assertEquals(CalculadoraPerfil.calcularSubidaRestanteMetros(requireNotNull(sendero.perfilElevacion), 0.5), estadoVisual.subidaRestanteMetros)
        assertTrue((estadoVisual.subidaRestanteMetros ?: 0) > 0)
    }

    @Test
    fun `con perfil la altura actual es la del perfil en el kilometro recorrido`() = runTest(despachadorPrueba) {
        val sendero = crearSenderoConPerfil(conPerfil = true)
        val viewModel = crearViewModel(sendero = sendero)
        control.estadoEmitido.value = crearEstadoConAvance(kilometrosRecorridos = 0.5)
        advanceUntilIdle()

        val esperada = interpolarAltitud(requireNotNull(sendero.perfilElevacion).puntos, 0.5)
        assertEquals(esperada ?: Double.NaN, viewModel.estadoVisual.value.altitudActualMetros ?: Double.NaN, 0.0001)
    }

    @Test
    fun `sin perfil no hay altura actual`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(sendero = crearSenderoConPerfil(conPerfil = false))
        control.estadoEmitido.value = crearEstadoConAvance(kilometrosRecorridos = 0.5)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.altitudActualMetros)
    }

    @Test
    fun `sin perfil no hay kilometro en el trazado ni subida restante`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(sendero = crearSenderoConPerfil(conPerfil = false))
        control.estadoEmitido.value = crearEstadoConAvance(kilometrosRecorridos = 0.5)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.kilometroEnTrazado)
        assertNull(viewModel.estadoVisual.value.subidaRestanteMetros)
    }

    @Test
    fun `antes de que el recorrido esté en curso no se muestra la posición en el perfil`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(sendero = crearSenderoConPerfil(conPerfil = true))
        runCurrent()

        assertNull(viewModel.estadoVisual.value.kilometroEnTrazado)
        assertNull(viewModel.estadoVisual.value.subidaRestanteMetros)
    }

    @Test
    fun `al llegar al final la subida restante es cero`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(sendero = crearSenderoConPerfil(conPerfil = true))
        control.estadoEmitido.value = crearEstadoConAvance(kilometrosRecorridos = largoDelTrazado)
        advanceUntilIdle()

        assertEquals(0, viewModel.estadoVisual.value.subidaRestanteMetros)
    }

    @Test
    fun `con el perfil desactualizado no se muestra la posicion ni la subida restante`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(sendero = crearSenderoConPerfil(conPerfil = true, firmaDelTrazado = "otro-trazado"))
        control.estadoEmitido.value = crearEstadoConAvance(kilometrosRecorridos = 0.5)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.kilometroEnTrazado)
        assertNull(viewModel.estadoVisual.value.subidaRestanteMetros)
    }

    @Test
    fun `con la senal no buena no se muestra la posicion ni la subida restante`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(sendero = crearSenderoConPerfil(conPerfil = true))
        control.estadoEmitido.value = crearEstadoConAvance(kilometrosRecorridos = 0.5).copy(calidadSenal = CalidadSenal.DEBIL)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.kilometroEnTrazado)
        assertNull(viewModel.estadoVisual.value.subidaRestanteMetros)
    }
}
