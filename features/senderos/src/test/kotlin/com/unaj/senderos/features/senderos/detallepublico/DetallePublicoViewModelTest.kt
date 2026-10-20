package com.unaj.senderos.features.senderos.detallepublico

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.GuardarCopiaSenderoPublicoUseCase
import com.unaj.senderos.core.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderoPublicoUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoPublicoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import com.unaj.senderos.core.testing.crearSenderoPublicoDePrueba
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
class DetallePublicoViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val repositorioPublico = FalsoSenderoPublicoRepositorio()
    private val repositorioPropio = FalsoSenderoRepositorio()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
    private val efectosRecibidos = mutableListOf<DetallePublicoEfecto>()

    private val ajeno = crearSenderoPublicoDePrueba(
        crearSenderoDePrueba(id = "publico-1", idUsuario = "autor-1", nombre = "Huella Andina"),
        nombreAutor = "Luis"
    )

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(idSendero: String = "publico-1"): DetallePublicoViewModel {
        val guardar = GuardarSenderoUseCase(repositorioPropio, autenticacion, generarIdentificador = { "id-copia" })
        val viewModel = DetallePublicoViewModel(
            idSendero = idSendero,
            observarSenderoPublicoUseCase = ObservarSenderoPublicoUseCase(repositorioPublico, autenticacion),
            observarSenderosDelUsuarioUseCase = ObservarSenderosDelUsuarioUseCase(repositorioPropio, autenticacion),
            obtenerUsuarioActualUseCase = ObtenerUsuarioActualUseCase(autenticacion),
            guardarCopiaSenderoPublicoUseCase = GuardarCopiaSenderoPublicoUseCase(guardar)
        )
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
    }

    @Test
    fun `muestra el sendero publicado por otro autor`() = runTest(despachadorPrueba) {
        repositorioPublico.publicos.value = listOf(ajeno)
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.estadoVisual.value
        assertFalse(estado.estaCargando)
        assertEquals(ajeno, estado.senderoPublico)
        assertFalse(estado.esPropio)
        assertNull(estado.mensajeError)
    }

    @Test
    fun `reconoce el sendero que publique yo`() = runTest(despachadorPrueba) {
        repositorioPublico.publicos.value = listOf(
            crearSenderoPublicoDePrueba(crearSenderoDePrueba(id = "publico-1", idUsuario = "usuario-1"))
        )
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esPropio)
    }

    @Test
    fun `un sendero que ya no esta publicado muestra el aviso`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertEquals("Este sendero ya no está publicado", viewModel.estadoVisual.value.mensajeError)
        assertNull(viewModel.estadoVisual.value.senderoPublico)
    }

    @Test
    fun `un fallo de lectura muestra el error`() = runTest(despachadorPrueba) {
        repositorioPublico.errorEnFlujo = IllegalStateException("sin red")
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertEquals("No se pudo cargar el sendero", viewModel.estadoVisual.value.mensajeError)
    }

    @Test
    fun `guardar crea la copia en mis senderos y navega a ella`() = runTest(despachadorPrueba) {
        repositorioPublico.publicos.value = listOf(ajeno)
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(DetallePublicoEvento.AlGuardarEnMisSenderos)
        advanceUntilIdle()

        assertEquals(listOf<DetallePublicoEfecto>(DetallePublicoEfecto.NavegarAMiSendero("id-copia")), efectosRecibidos)
        val (idUsuario, copia) = repositorioPropio.llamadasGuardarSendero.single()
        assertEquals("usuario-1", idUsuario)
        assertEquals("id-copia", copia.id)
        assertEquals("Huella Andina", copia.nombre)
        assertFalse(viewModel.estadoVisual.value.estaGuardando)
    }

    @Test
    fun `un fallo al guardar avisa y no navega`() = runTest(despachadorPrueba) {
        repositorioPublico.publicos.value = listOf(ajeno)
        repositorioPropio.resultadoGuardarSendero = Result.failure(IllegalStateException("disco lleno"))
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(DetallePublicoEvento.AlGuardarEnMisSenderos)
        advanceUntilIdle()

        assertEquals(listOf<DetallePublicoEfecto>(DetallePublicoEfecto.MostrarAviso("disco lleno")), efectosRecibidos)
        assertFalse(viewModel.estadoVisual.value.estaGuardando)
    }

    @Test
    fun `no se guarda una copia de un sendero propio`() = runTest(despachadorPrueba) {
        repositorioPublico.publicos.value = listOf(
            crearSenderoPublicoDePrueba(crearSenderoDePrueba(id = "publico-1", idUsuario = "usuario-1"))
        )
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(DetallePublicoEvento.AlGuardarEnMisSenderos)
        advanceUntilIdle()

        assertTrue(repositorioPropio.llamadasGuardarSendero.isEmpty())
        assertTrue(efectosRecibidos.isEmpty())
    }

    @Test
    fun `guardar la copia recuerda de que sendero publico viene`() = runTest(despachadorPrueba) {
        repositorioPublico.publicos.value = listOf(ajeno)
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(DetallePublicoEvento.AlGuardarEnMisSenderos)
        advanceUntilIdle()

        assertEquals("publico-1", repositorioPropio.llamadasGuardarSendero.single().second.idOrigen)
        assertEquals("id-copia", viewModel.estadoVisual.value.idCopia)
    }

    @Test
    fun `si ya hay una copia ofrece verla y no guarda otra`() = runTest(despachadorPrueba) {
        repositorioPublico.publicos.value = listOf(ajeno)
        val copia = crearSenderoDePrueba(id = "copia-1", idUsuario = "usuario-1").copy(idOrigen = "publico-1")
        repositorioPropio.senderos.value = listOf(copia)
        val viewModel = crearViewModel()
        advanceUntilIdle()
        assertEquals("copia-1", viewModel.estadoVisual.value.idCopia)

        viewModel.procesarEvento(DetallePublicoEvento.AlGuardarEnMisSenderos)
        advanceUntilIdle()
        assertTrue(repositorioPropio.llamadasGuardarSendero.isEmpty())
        assertTrue(efectosRecibidos.isEmpty())

        viewModel.procesarEvento(DetallePublicoEvento.AlVerMiCopia)
        advanceUntilIdle()
        assertEquals(listOf<DetallePublicoEfecto>(DetallePublicoEfecto.NavegarAMiSendero("copia-1")), efectosRecibidos)
    }

    @Test
    fun `volver pide regresar a Explorar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(DetallePublicoEvento.AlVolver)
        advanceUntilIdle()

        assertEquals(listOf<DetallePublicoEfecto>(DetallePublicoEfecto.VolverAExplorar), efectosRecibidos)
    }
}
