package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.domain.configuracion.ProvinciasArgentinas
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.CalcularPerfilDeTrazadoUseCase
import com.unaj.senderos.core.domain.usecase.ImportarTrazadoUseCase
import com.unaj.senderos.core.testing.FalsoLectorArchivos
import com.unaj.senderos.core.testing.FalsoAlmacenFotos
import com.unaj.senderos.core.testing.FalsoFuenteElevacion
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Sugerencias del formulario: recomendaciones y servicios frecuentes, provincias, épocas y la duración estimada
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioSugerenciasTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
    private val efectosRecibidos = mutableListOf<FormularioSenderoEfecto>()

    // Usar el despachador de prueba como principal
    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    // Devolver el despachador principal original
    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel del formulario y juntar sus efectos
    private fun crearViewModel(idSendero: String? = null, repositorio: FalsoSenderoRepositorio = FalsoSenderoRepositorio()): FormularioSenderoViewModel {
        val viewModel = FormularioSenderoViewModel(
            idSendero,
            ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            GuardarSenderoUseCase(repositorio, autenticacion),
            FalsoAlmacenFotos(),
            CalcularPerfilDeTrazadoUseCase(FalsoFuenteElevacion()),
            ImportarTrazadoUseCase(FalsoLectorArchivos())
        )
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
    }

    @Test
    fun `un sendero nuevo trae las recomendaciones basicas sin contar como cambio`() {
        val viewModel = crearViewModel()

        val estado = viewModel.estadoVisual.value
        assertEquals(RECOMENDACIONES_POR_DEFECTO, estado.recomendaciones)
        assertFalse(estado.hayCambiosSinGuardar)
    }

    @Test
    fun `al editar un sendero no se agregan las recomendaciones basicas`() = runTest(despachadorPrueba) {
        val repositorio = FalsoSenderoRepositorio(listOf(crearSenderoDePrueba(id = "sendero-1").copy(recomendaciones = listOf("Llevar bastones"))))
        val viewModel = crearViewModel("sendero-1", repositorio)
        advanceUntilIdle()

        assertEquals(listOf("Llevar bastones"), viewModel.estadoVisual.value.recomendaciones)
    }

    @Test
    fun `tocar una recomendacion sugerida la agrega una sola vez`() {
        val viewModel = crearViewModel()

        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarRecomendacionSugerida("Protector solar"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarRecomendacionSugerida("Protector solar"))

        val estado = viewModel.estadoVisual.value
        assertEquals(1, estado.recomendaciones.count { recomendacion -> recomendacion == "Protector solar" })
        assertTrue(estado.hayCambiosSinGuardar)
    }

    @Test
    fun `tocar un servicio sugerido lo agrega una sola vez sin importar mayusculas`() {
        val viewModel = crearViewModel()

        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarServicioSugerido("Baños"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarServicioSugerido("baños"))

        assertEquals(listOf("Baños"), viewModel.estadoVisual.value.servicios)
    }

    @Test
    fun `calcular la duracion usa la distancia y el desnivel cargados`() {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "10,0"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESNIVEL, "600"))

        viewModel.procesarEvento(FormularioSenderoEvento.AlCalcularDuracion)

        assertEquals("3 h 30 min", viewModel.estadoVisual.value.duracionEstimada)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `sin distancia cargada la duracion usa la del recorrido marcado`() {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0359))

        viewModel.procesarEvento(FormularioSenderoEvento.AlCalcularDuracion)

        assertEquals("1 h", viewModel.estadoVisual.value.duracionEstimada)
    }

    @Test
    fun `un desnivel con decimales se redondea`() {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "10"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESNIVEL, "599,6"))

        viewModel.procesarEvento(FormularioSenderoEvento.AlCalcularDuracion)

        assertEquals("3 h 30 min", viewModel.estadoVisual.value.duracionEstimada)
    }

    @Test
    fun `un desnivel que no es un numero avisa en lugar de tomarlo como cero`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "10"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESNIVEL, "mucho"))

        viewModel.procesarEvento(FormularioSenderoEvento.AlCalcularDuracion)
        advanceUntilIdle()

        assertEquals("", viewModel.estadoVisual.value.duracionEstimada)
        assertEquals(
            listOf<FormularioSenderoEfecto>(FormularioSenderoEfecto.MostrarAviso("Revisá el desnivel: tiene que ser un número de metros")),
            efectosRecibidos
        )
    }

    @Test
    fun `sin distancia ni recorrido calcular la duracion avisa`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()

        viewModel.procesarEvento(FormularioSenderoEvento.AlCalcularDuracion)
        advanceUntilIdle()

        assertEquals("", viewModel.estadoVisual.value.duracionEstimada)
        assertEquals(
            listOf<FormularioSenderoEfecto>(
                FormularioSenderoEfecto.MostrarAviso("Cargá la distancia o marcá el recorrido para calcular la duración")
            ),
            efectosRecibidos
        )
    }

    @Test
    fun `la duracion se muestra en horas y minutos`() {
        assertEquals("45 min", formatearDuracionEstimada(45))
        assertEquals("2 h", formatearDuracionEstimada(120))
        assertEquals("3 h 30 min", formatearDuracionEstimada(210))
    }

    @Test
    fun `las provincias son las veinticuatro jurisdicciones sin repetir`() {
        assertEquals(24, ProvinciasArgentinas.NOMBRES.size)
        assertEquals(24, ProvinciasArgentinas.NOMBRES.toSet().size)
        assertTrue("Ciudad Autónoma de Buenos Aires" in ProvinciasArgentinas.NOMBRES)
    }

    @Test
    fun `las sugerencias filtran por lo escrito sin importar mayusculas ni tildes`() {
        assertEquals(listOf("Neuquén"), filtrarSugerencias(ProvinciasArgentinas.NOMBRES, "neuqu"))
        assertEquals(listOf("Río Negro"), filtrarSugerencias(ProvinciasArgentinas.NOMBRES, "rio ne"))
        assertEquals(ProvinciasArgentinas.NOMBRES, filtrarSugerencias(ProvinciasArgentinas.NOMBRES, ""))
    }
}
