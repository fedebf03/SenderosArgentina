package com.unaj.senderos.features.senderos.listado

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.core.domain.usecase.ObservarSenderosPublicosUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.domain.usecase.RecuperarRecorridoInterrumpidoUseCase
import com.unaj.senderos.core.testing.FalsoAlmacenBorradorRecorrido
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoControlSeguimiento
import com.unaj.senderos.core.testing.FalsoLanzadorSeguimiento
import com.unaj.senderos.core.testing.FalsoMonitorConectividad
import com.unaj.senderos.core.testing.FalsoProveedorUbicacion
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoPublicoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearRecorridoDePrueba
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Aviso de Explorar al volver a abrir la app: había un recorrido a medias porque Android la cerró
@OptIn(ExperimentalCoroutinesApi::class)
class SenderosRecuperarRecorridoTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val control = FalsoControlSeguimiento()
    private val almacen = FalsoAlmacenBorradorRecorrido()
    private val recorridos = FalsoRecorridoRepositorio()
    private val efectosRecibidos = mutableListOf<SenderosEfecto>()

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // Crear el ViewModel de Explorar o de Mis senderos, con el borrador que haya en el almacén
    private fun crearViewModel(modo: ModoListado = ModoListado.EXPLORAR): SenderosViewModel {
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val viewModel = SenderosViewModel(
            modo,
            ObservarSenderosDelUsuarioUseCase(FalsoSenderoRepositorio(), autenticacion),
            ObservarSenderosPublicosUseCase(FalsoSenderoPublicoRepositorio(), autenticacion),
            FalsoProveedorUbicacion(),
            FalsoMonitorConectividad(),
            control,
            FalsoLanzadorSeguimiento(),
            ObtenerUsuarioActualUseCase(autenticacion),
            RecuperarRecorridoInterrumpidoUseCase(almacen, recorridos, FalsoPuntosCapturadosRepositorio(), autenticacion)
        ) { 0L }
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) } }
        return viewModel
    }

    private fun dejarBorrador() {
        almacen.borrador = crearRecorridoDePrueba(id = "recorrido-1000", idUsuario = "usuario-1", idSendero = "libre-1000")
    }

    @Test
    fun `al abrir Explorar con un recorrido a medias se ofrece recuperarlo`() = runTest(despachadorPrueba) {
        dejarBorrador()
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertEquals("recorrido-1000", viewModel.estadoVisual.value.recorridoInterrumpido?.id)
    }

    @Test
    fun `sin borrador no se ofrece nada`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.recorridoInterrumpido)
    }

    @Test
    fun `con un recorrido en marcha no se ofrece recuperar nada`() = runTest(despachadorPrueba) {
        dejarBorrador()
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "libre-1000", trazado = emptyList(), esCaminataLibre = true)
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.recorridoInterrumpido)
    }

    @Test
    fun `Mis senderos no ofrece recuperar para no repetir el aviso`() = runTest(despachadorPrueba) {
        dejarBorrador()
        val viewModel = crearViewModel(ModoListado.MIS_SENDEROS)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.recorridoInterrumpido)
    }

    @Test
    fun `recuperar guarda el recorrido en el historial y lo abre`() = runTest(despachadorPrueba) {
        dejarBorrador()
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlRecuperarRecorrido)
        advanceUntilIdle()

        assertEquals("recorrido-1000", recorridos.guardados.single().id)
        assertNull(viewModel.estadoVisual.value.recorridoInterrumpido)
        assertTrue(efectosRecibidos.contains(SenderosEfecto.NavegarAHistorialDetalle("recorrido-1000")))
    }

    @Test
    fun `descartar borra el borrador y cierra el aviso`() = runTest(despachadorPrueba) {
        dejarBorrador()
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlDescartarRecorridoInterrumpido)
        advanceUntilIdle()

        assertNull(almacen.borrador)
        assertNull(viewModel.estadoVisual.value.recorridoInterrumpido)
        assertTrue(recorridos.guardados.isEmpty())
    }

    @Test
    fun `si recuperar falla avisa y el borrador se conserva para reintentar`() = runTest(despachadorPrueba) {
        dejarBorrador()
        recorridos.resultadoGuardar = Result.failure(IllegalStateException("Sin espacio"))
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(SenderosEvento.AlRecuperarRecorrido)
        advanceUntilIdle()

        assertNotNull(almacen.borrador)
        assertTrue(efectosRecibidos.any { efecto -> efecto is SenderosEfecto.MostrarAviso })
    }
}
