package com.unaj.senderos.presentation.formulario

import com.unaj.senderos.domain.model.Bioma
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.falsos.FalsoAlmacenFotos
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearSenderoDePrueba
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormularioSenderoViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(idSendero: String?, repositorio: FalsoSenderoRepositorio): FormularioSenderoViewModel {
        return FormularioSenderoViewModel(
            idSendero,
            ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            GuardarSenderoUseCase(repositorio, autenticacion),
            FalsoAlmacenFotos()
        )
    }

    @Test
    fun `al cerrar el editor con la distancia vacia se completa con la del mapa y con coma`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(null, FalsoSenderoRepositorio())
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.1))

        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()

        assertEquals("11,1", viewModel.estadoVisual.value.distancia)
    }

    @Test
    fun `al cerrar el editor no pisa una distancia ya cargada`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(null, FalsoSenderoRepositorio())
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "570"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.1))

        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()

        assertEquals("570", viewModel.estadoVisual.value.distancia)
    }

    @Test
    fun `al cerrar el editor con menos de dos puntos no completa la distancia`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(null, FalsoSenderoRepositorio())
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(0.0, 0.0))

        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()

        assertEquals("", viewModel.estadoVisual.value.distancia)
    }

    @Test
    fun `la distancia de un sendero en edicion se muestra con coma`() = runTest(despachadorPrueba) {
        val repositorio = FalsoSenderoRepositorio(listOf(crearSenderoDePrueba(distanciaKilometros = 12.5)))
        val viewModel = crearViewModel("sendero-1", repositorio)

        advanceUntilIdle()

        assertEquals("12,5", viewModel.estadoVisual.value.distancia)
    }

    // Completar los campos obligatorios del formulario con valores válidos
    private fun completarCamposObligatorios(viewModel: FormularioSenderoViewModel) {
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Cerro Nuevo"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.REGION, "Jujuy"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESCRIPCION, "Camino de altura"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "8"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESNIVEL, "300"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.ALTURA_MAXIMA, "4000"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.ALTURA_MINIMA, "3500"))
    }

    @Test
    fun `un formulario nuevo arranca con el bioma sin definir`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(null, FalsoSenderoRepositorio())

        assertEquals(Bioma.SIN_DEFINIR, viewModel.estadoVisual.value.bioma)
    }

    @Test
    fun `seleccionar un bioma lo guarda en el estado y marca cambios sin guardar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(null, FalsoSenderoRepositorio())

        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarBioma(Bioma.PUNA))

        assertEquals(Bioma.PUNA, viewModel.estadoVisual.value.bioma)
        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `crear un sendero con bioma lo envia al repositorio`() = runTest(despachadorPrueba) {
        val repositorio = FalsoSenderoRepositorio()
        val viewModel = crearViewModel(null, repositorio)
        completarCamposObligatorios(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarBioma(Bioma.YUNGAS))

        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        advanceUntilIdle()

        assertEquals(1, repositorio.llamadasGuardarSendero.size)
        assertEquals(Bioma.YUNGAS, repositorio.llamadasGuardarSendero.single().second.bioma)
    }

    @Test
    fun `crear un sendero sin elegir bioma lo guarda como sin definir`() = runTest(despachadorPrueba) {
        val repositorio = FalsoSenderoRepositorio()
        val viewModel = crearViewModel(null, repositorio)
        completarCamposObligatorios(viewModel)

        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        advanceUntilIdle()

        assertEquals(Bioma.SIN_DEFINIR, repositorio.llamadasGuardarSendero.single().second.bioma)
    }

    @Test
    fun `editar un sendero carga su bioma en el estado`() = runTest(despachadorPrueba) {
        val repositorio = FalsoSenderoRepositorio(listOf(crearSenderoDePrueba(bioma = Bioma.MONTE)))
        val viewModel = crearViewModel("sendero-1", repositorio)

        advanceUntilIdle()

        assertEquals(Bioma.MONTE, viewModel.estadoVisual.value.bioma)
        assertFalse(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `editar un sendero sin tocar el bioma lo conserva al guardar`() = runTest(despachadorPrueba) {
        val repositorio = FalsoSenderoRepositorio(listOf(crearSenderoDePrueba(bioma = Bioma.SIERRAS)))
        val viewModel = crearViewModel("sendero-1", repositorio)
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Otro nombre"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        advanceUntilIdle()

        assertEquals(Bioma.SIERRAS, repositorio.llamadasGuardarSendero.single().second.bioma)
    }

    @Test
    fun `editar un sendero y cambiar el bioma guarda el nuevo`() = runTest(despachadorPrueba) {
        val repositorio = FalsoSenderoRepositorio(listOf(crearSenderoDePrueba(bioma = Bioma.SIERRAS)))
        val viewModel = crearViewModel("sendero-1", repositorio)
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarBioma(Bioma.COSTA_MARITIMA))
        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        advanceUntilIdle()

        assertEquals(Bioma.COSTA_MARITIMA, repositorio.llamadasGuardarSendero.single().second.bioma)
    }
}
