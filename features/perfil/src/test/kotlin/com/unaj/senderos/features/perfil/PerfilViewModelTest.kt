package com.unaj.senderos.features.perfil

import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.domain.usecase.SincronizarPerfilPendienteUseCase
import com.unaj.senderos.core.testing.FalsoPerfilRepositorio
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoControlSeguimiento
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.testing.FalsoLanzadorSeguimiento
import com.unaj.senderos.core.testing.FalsoNotificador
import com.unaj.senderos.core.testing.FalsoPreferenciasAplicacion
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.crearRecorridoDePrueba
import com.unaj.senderos.core.domain.usecase.ObservarRecorridosUseCase
import com.unaj.senderos.core.domain.usecase.ExportarCopiaSeguridadUseCase
import com.unaj.senderos.core.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ImportarCopiaSeguridadUseCase
import com.unaj.senderos.core.testing.FalsoEscritorArchivos
import com.unaj.senderos.core.testing.FalsoFormatoCopiaSeguridad
import com.unaj.senderos.core.testing.FalsoLectorArchivos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val preferencias = FalsoPreferenciasAplicacion()
    private val repositorioRecorridos = FalsoRecorridoRepositorio()
    private val controlSeguimiento = FalsoControlSeguimiento()
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))

    @Before
    fun configurar() {
        Dispatchers.setMain(despachadorPrueba)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private val perfiles = FalsoPerfilRepositorio()

    private fun crearViewModel(): PerfilViewModel {
        return PerfilViewModel(
            obtenerUsuarioActualUseCase = ObtenerUsuarioActualUseCase(autenticacion),
            cerrarSesionUseCase = CerrarSesionUseCase(autenticacion, FalsoSenderoRepositorio(), FalsoLanzadorSeguimiento(), FalsoNotificador()),
            preferenciasAplicacion = preferencias,
            observarRecorridosUseCase = ObservarRecorridosUseCase(repositorioRecorridos, autenticacion),
            controlSeguimiento = controlSeguimiento,
            exportarCopiaSeguridadUseCase = ExportarCopiaSeguridadUseCase(
                FalsoSenderoRepositorio(), autenticacion, FalsoEscritorArchivos(), FalsoFormatoCopiaSeguridad()
            ),
            importarCopiaSeguridadUseCase = ImportarCopiaSeguridadUseCase(
                FalsoSenderoRepositorio(), autenticacion, FalsoLectorArchivos(), FalsoFormatoCopiaSeguridad(),
                GuardarSenderoUseCase(FalsoSenderoRepositorio(), autenticacion)
            ),
            sincronizarPerfilPendienteUseCase = SincronizarPerfilPendienteUseCase(perfiles, autenticacion)
        )
    }

    @Test
    fun `el modo demostracion arranca desactivado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esModoDemostracion)
    }

    @Test
    fun `activar el modo demostracion lo guarda y lo refleja`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(PerfilEvento.AlCambiarModoDemostracion(activo = true))
        advanceUntilIdle()

        assertTrue(preferencias.esModoDemostracion.value)
        assertTrue(viewModel.estadoVisual.value.esModoDemostracion)
    }

    @Test
    fun `desactivar el modo demostracion lo apaga`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(PerfilEvento.AlCambiarModoDemostracion(activo = true))
        advanceUntilIdle()

        viewModel.procesarEvento(PerfilEvento.AlCambiarModoDemostracion(activo = false))
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esModoDemostracion)
    }

    @Test
    fun `el historial refleja los recorridos del mas reciente al mas viejo`() = runTest(despachadorPrueba) {
        repositorioRecorridos.recorridos.value = listOf(
            crearRecorridoDePrueba(id = "viejo", inicioMilisegundos = 1_000L),
            crearRecorridoDePrueba(id = "nuevo", inicioMilisegundos = 9_000L, finMilisegundos = 12_000L)
        )
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertEquals(listOf("nuevo", "viejo"), viewModel.estadoVisual.value.recorridos.map { recorrido -> recorrido.id })
    }

    @Test
    fun `sin recorridos el historial es vacio`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.recorridos.isEmpty())
    }

    @Test
    fun `seleccionar un recorrido navega a su detalle`() = runTest(despachadorPrueba) {
        val efectos = mutableListOf<PerfilEfecto>()
        val viewModel = crearViewModel()
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectos.add(efecto) } }
        advanceUntilIdle()

        viewModel.procesarEvento(PerfilEvento.AlSeleccionarRecorrido("r1"))
        advanceUntilIdle()

        assertEquals(listOf<PerfilEfecto>(PerfilEfecto.NavegarAHistorialDetalle("r1")), efectos)
    }

    @Test
    fun `tocar cerrar sesion muestra la confirmacion sin cerrar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(PerfilEvento.AlTocarCerrarSesion)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esDialogoCerrarSesionVisible)
        assertEquals(0, autenticacion.cantidadCierresSesion)
    }

    @Test
    fun `confirmar cerrar sesion cierra la sesion y navega al login`() = runTest(despachadorPrueba) {
        val efectos = mutableListOf<PerfilEfecto>()
        val viewModel = crearViewModel()
        CoroutineScope(despachadorPrueba).launch { viewModel.efectoVisual.collect { efecto -> efectos.add(efecto) } }
        viewModel.procesarEvento(PerfilEvento.AlTocarCerrarSesion)

        viewModel.procesarEvento(PerfilEvento.AlConfirmarCerrarSesion)
        advanceUntilIdle()

        assertEquals(1, autenticacion.cantidadCierresSesion)
        assertFalse(viewModel.estadoVisual.value.esDialogoCerrarSesionVisible)
        assertEquals(listOf<PerfilEfecto>(PerfilEfecto.NavegarALogin), efectos)
    }

    @Test
    fun `cancelar cerrar sesion conserva la sesion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(PerfilEvento.AlTocarCerrarSesion)

        viewModel.procesarEvento(PerfilEvento.AlCancelarCerrarSesion)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoCerrarSesionVisible)
        assertEquals(0, autenticacion.cantidadCierresSesion)
    }

    @Test
    fun `el estado indica si hay un recorrido en curso`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.hayRecorridoEnCurso)

        controlSeguimiento.estadoEmitido.value = EstadoSeguimiento(idSendero = "s1", trazado = emptyList())
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.hayRecorridoEnCurso)

        controlSeguimiento.estadoEmitido.value = null
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.hayRecorridoEnCurso)
    }

    @Test
    fun `un fallo al leer el historial muestra un mensaje de error`() = runTest(despachadorPrueba) {
        autenticacion.usuarioActual = null
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.estadoVisual.value.mensajeError)
    }

    @Test
    fun `con el historial disponible no hay mensaje de error`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.mensajeError)
    }
}
