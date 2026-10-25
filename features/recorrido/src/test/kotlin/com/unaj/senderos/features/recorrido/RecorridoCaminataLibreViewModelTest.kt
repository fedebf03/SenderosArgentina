package com.unaj.senderos.features.recorrido

import com.unaj.senderos.core.testing.FalsoAlmacenPronosticos
import com.unaj.senderos.core.testing.FalsaFuentePronostico
import com.unaj.senderos.core.domain.usecase.ObtenerPronosticoUseCase
import com.unaj.senderos.core.domain.model.CalidadSenal
import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.testing.crearPuntoCapturadoDePrueba
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.DescartarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.IncorporarPuntosCapturadosUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarPuntosCapturadosConDistanciaUseCase
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoControlSeguimiento
import com.unaj.senderos.core.testing.FalsoLanzadorSeguimiento
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val ID_CAMINATA = "libre-5000"

// Pantalla del recorrido en modo caminata libre: no hay sendero, se muestra lo caminado y al finalizar se ofrece convertirlo en un sendero nuevo
@OptIn(ExperimentalCoroutinesApi::class)
class RecorridoCaminataLibreViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val registro = mutableListOf<String>()
    private val control = FalsoControlSeguimiento(registro)
    private val lanzador = FalsoLanzadorSeguimiento(registro)
    private val efectosRecibidos = mutableListOf<RecorridoEfecto>()
    private val puntosCapturados = FalsoPuntosCapturadosRepositorio()
    private var relojActual = 0L

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel para el identificador indicado y juntar sus efectos
    private fun crearViewModel(idSendero: String = ID_CAMINATA): RecorridoViewModel {
        val repositorio = FalsoSenderoRepositorio()
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val viewModel = RecorridoViewModel(
            idSendero = idSendero,
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            controlSeguimiento = control,
            lanzadorSeguimiento = lanzador,
            capacidadesDispositivo = CapacidadesDispositivo(tieneGps = true, tieneAcelerometro = true, tieneBrujula = true),
            observarPuntosCapturadosConDistanciaUseCase = ObservarPuntosCapturadosConDistanciaUseCase(repositorio, puntosCapturados, autenticacion),
            incorporarPuntosCapturadosUseCase = IncorporarPuntosCapturadosUseCase(puntosCapturados, autenticacion),
            descartarPuntosCapturadosUseCase = DescartarPuntosCapturadosUseCase(puntosCapturados, autenticacion),
            obtenerPronosticoUseCase = ObtenerPronosticoUseCase(FalsaFuentePronostico(), FalsoAlmacenPronosticos()) { 0L },
            reloj = { relojActual }
        )
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) } }
        return viewModel
    }

    // Armar el estado que publicaría el control para una caminata libre en curso
    private fun estadoDeCaminata(kilometros: Double = 1.5, id: String = ID_CAMINATA) = EstadoSeguimiento(
        idSendero = id,
        trazado = emptyList(),
        nombreSendero = "Caminata libre",
        posicionActual = Coordenada(-41.13, -71.30),
        distanciaRecorridaKilometros = kilometros,
        trazadoCaminado = listOf(Coordenada(-41.13, -71.31), Coordenada(-41.13, -71.30)),
        instanteInicioMilisegundos = 1_000L,
        calidadSenal = CalidadSenal.BUENA,
        esCaminataLibre = true
    )

    @Test
    fun `una caminata libre en curso se refleja en pantalla sin necesitar un sendero`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoDeCaminata(kilometros = 1.5)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertTrue(estado.esCaminataLibre)
        assertEquals(FaseRecorrido.EN_CURSO, estado.fase)
        assertNull(estado.sendero)
        assertEquals(1.5, estado.distanciaRecorridaKilometros, 0.0)
        assertEquals(2, estado.trazadoCaminado.size)
    }

    @Test
    fun `un recorrido de sendero no se marca como caminata libre`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(idSendero = "sendero-1")
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esCaminataLibre)
    }

    @Test
    fun `finalizar una caminata libre detiene el servicio y muestra el resumen con la opcion de convertirla`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoDeCaminata(kilometros = 2.0)
        advanceUntilIdle()
        relojActual = 61_000L

        viewModel.procesarEvento(RecorridoEvento.AlTocarFinalizar)
        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        advanceUntilIdle()

        assertTrue(registro.contains("lanzador.detener"))
        val resumen = viewModel.estadoVisual.value.resumen
        assertNotNull(resumen)
        assertEquals(2.0, resumen?.distanciaRecorridaKilometros ?: 0.0, 0.0)
        assertEquals(60_000L, resumen?.tiempoTranscurridoMilisegundos)
    }

    @Test
    fun `convertir en sendero pide abrir el formulario con el identificador de la caminata`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoDeCaminata()
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlConvertirEnSendero)
        advanceUntilIdle()

        assertTrue(efectosRecibidos.contains(RecorridoEfecto.NavegarAFormularioDesdeCaminata(ID_CAMINATA)))
    }

    @Test
    fun `una caminata de mas de 50 metros se puede convertir en sendero`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoDeCaminata(kilometros = 0.2)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.sePuedeConvertirEnSendero)
    }

    @Test
    fun `una caminata de menos de 50 metros no ofrece convertirla`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoDeCaminata(kilometros = 0.01)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.sePuedeConvertirEnSendero)
    }

    @Test
    fun `un recorrido de sendero nunca ofrece convertirse en sendero`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(idSendero = "sendero-1")
        control.estadoEmitido.value = estadoDeCaminata(kilometros = 5.0, id = "sendero-1").copy(esCaminataLibre = false)
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.sePuedeConvertirEnSendero)
    }

    @Test
    fun `los puntos marcados durante la caminata se exponen para dibujarlos en el mapa`() = runTest(despachadorPrueba) {
        puntosCapturados.puntos.value = listOf(
            crearPuntoCapturadoDePrueba(id = "p1", idSendero = ID_CAMINATA, latitud = -41.13, longitud = -71.30, tipo = TipoPunto.AGUA, nota = "Arroyo", foto = "/fotos/a.jpg"),
            crearPuntoCapturadoDePrueba(id = "p2", idSendero = "libre-9999", latitud = -41.20, longitud = -71.40)
        )
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoDeCaminata()
        advanceUntilIdle()

        val marcados = viewModel.estadoVisual.value.puntosMarcados
        assertEquals(1, marcados.size)
        assertEquals(TipoPunto.AGUA, marcados.single().tipo)
        assertEquals("Arroyo", marcados.single().nombre)
        assertEquals("/fotos/a.jpg", marcados.single().foto)
        assertEquals(-41.13, marcados.single().latitud, 0.0)
    }

    @Test
    fun `sin puntos marcados el mapa no dibuja ninguno`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoDeCaminata()
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.puntosMarcados.isEmpty())
    }

    @Test
    fun `cerrar el resumen sin convertir vuelve atras y no abre el formulario`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoDeCaminata()
        advanceUntilIdle()
        viewModel.procesarEvento(RecorridoEvento.AlConfirmarFinalizar)
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlCerrarResumen)
        advanceUntilIdle()

        assertTrue(efectosRecibidos.contains(RecorridoEfecto.VolverAlDetalle))
        assertTrue(efectosRecibidos.none { efecto -> efecto is RecorridoEfecto.NavegarAFormularioDesdeCaminata })
    }

    @Test
    fun `cancelar una caminata libre la descarta y vuelve atras`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoDeCaminata()
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlConfirmarCancelarRecorrido)
        advanceUntilIdle()

        assertEquals(1, control.cantidadCancelaciones)
        assertTrue(efectosRecibidos.contains(RecorridoEfecto.VolverAlDetalle))
    }

    @Test
    fun `tocar un punto del mapa sin sendero no falla`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoDeCaminata()
        advanceUntilIdle()

        viewModel.procesarEvento(RecorridoEvento.AlSeleccionarPuntoEnMapa(0))

        assertNull(viewModel.estadoVisual.value.indicePuntoSeleccionado)
    }

    @Test
    fun `el estado de otra caminata no se refleja en esta pantalla`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        control.estadoEmitido.value = estadoDeCaminata(id = "libre-9999")
        advanceUntilIdle()

        // No toma el estado ajeno: sin el suyo, vence el plazo de inicio y queda no activo, sin distancia ni trazado de la otra caminata
        assertEquals(FaseRecorrido.NO_ACTIVO, viewModel.estadoVisual.value.fase)
        assertTrue(viewModel.estadoVisual.value.trazadoCaminado.isEmpty())
    }
}
