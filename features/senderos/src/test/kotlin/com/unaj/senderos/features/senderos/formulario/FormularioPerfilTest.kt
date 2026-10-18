package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.port.SinConexionElevacionException
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.domain.usecase.CalcularPerfilDeTrazadoUseCase
import com.unaj.senderos.core.domain.usecase.ImportarTrazadoUseCase
import com.unaj.senderos.core.testing.FalsoLectorArchivos
import com.unaj.senderos.core.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.testing.FalsoAlmacenFotos
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoFuenteElevacion
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Formulario: la distancia sigue al mapa mientras no se escriba a mano, y el perfil de elevación se calcula al cerrar el editor
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioPerfilTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
    private val fuente = FalsoFuenteElevacion()
    private val repositorio = FalsoSenderoRepositorio()

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

    // Crear el ViewModel del formulario con una fuente de alturas falsa
    private fun crearViewModel(idSendero: String? = null): FormularioSenderoViewModel {
        return FormularioSenderoViewModel(
            idSendero,
            ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            GuardarSenderoUseCase(repositorio, autenticacion),
            FalsoAlmacenFotos(),
            CalcularPerfilDeTrazadoUseCase(fuente),
            ImportarTrazadoUseCase(FalsoLectorArchivos())
        )
    }

    // Trazar dos puntos 1,1 km al sur: la fuente falsa da 1000 m y 1100 m de altura
    private fun trazar(viewModel: FormularioSenderoViewModel) {
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(-40.0, -70.0))
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(-40.01, -70.0))
    }

    @Test
    fun `la distancia sigue al mapa mientras se traza`() {
        val viewModel = crearViewModel()

        trazar(viewModel)
        assertEquals("1,1", viewModel.estadoVisual.value.distancia)

        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(-40.02, -70.0))
        assertEquals("2,2", viewModel.estadoVisual.value.distancia)
        assertFalse(viewModel.estadoVisual.value.distanciaEsManual)
    }

    @Test
    fun `una distancia escrita a mano no se pisa con la del mapa`() {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "5"))

        trazar(viewModel)

        assertEquals("5", viewModel.estadoVisual.value.distancia)
        assertTrue(viewModel.estadoVisual.value.distanciaEsManual)
    }

    @Test
    fun `usar la distancia del mapa vuelve a seguirlo`() {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "5"))
        trazar(viewModel)

        viewModel.procesarEvento(FormularioSenderoEvento.AlUsarDistanciaDelMapa)

        assertEquals("1,1", viewModel.estadoVisual.value.distancia)
        assertFalse(viewModel.estadoVisual.value.distanciaEsManual)
    }

    @Test
    fun `vaciar la distancia vuelve a seguir al mapa`() {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "5"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, ""))

        trazar(viewModel)

        assertEquals("1,1", viewModel.estadoVisual.value.distancia)
    }

    @Test
    fun `al cerrar el editor se calcula el perfil y completa desnivel y alturas vacios`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        trazar(viewModel)

        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        val perfil = estado.perfilElevacion
        assertTrue(perfil != null)
        assertEquals(perfil?.desnivelPositivoMetros.toString(), estado.desnivel)
        assertEquals("1100", estado.alturaMaxima)
        assertEquals("1000", estado.alturaMinima)
        assertFalse(estado.estaCalculandoPerfil)
        assertFalse(estado.perfilEstaDesactualizado)
    }

    @Test
    fun `el perfil no pisa un desnivel escrito a mano`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESNIVEL, "350"))
        trazar(viewModel)

        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()

        assertEquals("350", viewModel.estadoVisual.value.desnivel)
        assertEquals("1100", viewModel.estadoVisual.value.alturaMaxima)
    }

    @Test
    fun `sin conexion avisa que el perfil se calculara al sincronizar`() = runTest(despachadorPrueba) {
        fuente.falloDevuelto = SinConexionElevacionException(null)
        val viewModel = crearViewModel()
        trazar(viewModel)

        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertNull(estado.perfilElevacion)
        assertEquals("Sin conexión: el perfil se calculará al sincronizar", estado.avisoPerfil)
        assertFalse(estado.estaCalculandoPerfil)
    }

    @Test
    fun `con el perfil al dia cerrar el editor otra vez no vuelve a consultar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        trazar(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlAbrirEditorMapa)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()

        assertEquals(1, fuente.llamadas.size)
    }

    @Test
    fun `cambiar el trazado deja el perfil desactualizado hasta recalcularlo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        trazar(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(-40.02, -70.0))
        assertTrue(viewModel.estadoVisual.value.perfilEstaDesactualizado)

        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.perfilEstaDesactualizado)
        assertEquals(2, fuente.llamadas.size)
    }

    // Si el trazado cambió mientras se consultaban las alturas, ese perfil ya no corresponde y se descarta
    @Test
    fun `un perfil que llega tarde para otro trazado se descarta`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        trazar(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)

        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(-40.02, -70.0))
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.perfilElevacion)
        assertFalse(viewModel.estadoVisual.value.estaCalculandoPerfil)
    }

    @Test
    fun `calcular el perfil a mano reintenta despues de un fallo`() = runTest(despachadorPrueba) {
        fuente.falloDevuelto = SinConexionElevacionException(null)
        val viewModel = crearViewModel()
        trazar(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()
        fuente.falloDevuelto = null

        viewModel.procesarEvento(FormularioSenderoEvento.AlCalcularPerfil)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.perfilElevacion != null)
        assertNull(viewModel.estadoVisual.value.avisoPerfil)
    }

    @Test
    fun `al guardar el sendero viaja con su perfil`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Costanera"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.REGION, "Santa Cruz"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESCRIPCION, "Junto al lago"))
        trazar(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        advanceUntilIdle()

        val guardado = repositorio.llamadasGuardarSendero.single().second
        assertEquals(CalculadoraPerfil.firmaTrazado(guardado.recorrido, guardado.tipoRecorrido), guardado.perfilElevacion?.firmaTrazado)
    }

    // Así la sincronización sabe que el desnivel y las alturas los completó la app y puede actualizarlos con el perfil nuevo
    @Test
    fun `al guardar con el perfil desactualizado viaja el perfil que completo los campos`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, "Costanera"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.REGION, "Santa Cruz"))
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESCRIPCION, "Junto al lago"))
        trazar(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()
        val perfilQueCompleto = viewModel.estadoVisual.value.perfilElevacion
        viewModel.procesarEvento(FormularioSenderoEvento.AlAgregarPunto(-40.02, -70.0))

        viewModel.procesarEvento(FormularioSenderoEvento.AlGuardar)
        advanceUntilIdle()

        assertEquals(perfilQueCompleto, repositorio.llamadasGuardarSendero.single().second.perfilElevacion)
    }

    @Test
    fun `usar la distancia del mapa cuenta como cambio sin guardar`() {
        val viewModel = crearViewModel()
        trazar(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "5"))

        viewModel.procesarEvento(FormularioSenderoEvento.AlUsarDistanciaDelMapa)

        assertTrue(viewModel.estadoVisual.value.hayCambiosSinGuardar)
    }

    @Test
    fun `vaciar la distancia no la rellena mientras se escribe`() {
        val viewModel = crearViewModel()
        trazar(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, "5"))

        viewModel.procesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, ""))

        assertEquals("", viewModel.estadoVisual.value.distancia)
        assertFalse(viewModel.estadoVisual.value.distanciaEsManual)
    }

    @Test
    fun `cambiar el tipo de recorrido con el mapa cerrado recalcula el perfil`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        trazar(viewModel)
        viewModel.procesarEvento(FormularioSenderoEvento.AlCerrarEditorMapa)
        advanceUntilIdle()

        viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarTipoRecorrido(TipoRecorrido.IDA_Y_VUELTA))
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.perfilEstaDesactualizado)
        assertEquals(2, fuente.llamadas.size)
    }

    @Test
    fun `al editar un sendero cuya distancia es la del mapa la distancia sigue al mapa`() = runTest(despachadorPrueba) {
        val trazado = listOf(PuntoRecorrido(-40.0, -70.0), PuntoRecorrido(-40.01, -70.0))
        repositorio.senderos.value = listOf(
            crearSenderoDePrueba(id = "sendero-1", recorrido = trazado).copy(distanciaKilometros = 1.1, tipoRecorrido = TipoRecorrido.LINEAL)
        )
        val viewModel = crearViewModel("sendero-1")
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.distanciaEsManual)
    }

    @Test
    fun `al editar un sendero con otra distancia se respeta como escrita a mano`() = runTest(despachadorPrueba) {
        val trazado = listOf(PuntoRecorrido(-40.0, -70.0), PuntoRecorrido(-40.01, -70.0))
        repositorio.senderos.value = listOf(
            crearSenderoDePrueba(id = "sendero-1", recorrido = trazado).copy(distanciaKilometros = 12.0, tipoRecorrido = TipoRecorrido.LINEAL)
        )
        val viewModel = crearViewModel("sendero-1")
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.distanciaEsManual)
    }
}
