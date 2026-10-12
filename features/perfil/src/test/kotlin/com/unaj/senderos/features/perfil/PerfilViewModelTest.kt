package com.unaj.senderos.presentation.perfil

import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.ObtenerCapacidadesDispositivoUseCase
import com.unaj.senderos.falsos.FalsoDetectorCapacidades
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoControlSeguimiento
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.falsos.FalsoLanzadorSeguimiento
import com.unaj.senderos.falsos.FalsoNotificador
import com.unaj.senderos.falsos.FalsoPreferenciasAplicacion
import com.unaj.senderos.falsos.FalsoRecorridoRepositorio
import com.unaj.senderos.falsos.crearRecorridoDePrueba
import com.unaj.senderos.domain.usecase.ObservarRecorridosUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
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

    // Crear el ViewModel del perfil con un usuario activo y las preferencias falsas
    private fun crearViewModel(
        capacidades: CapacidadesDispositivo = CapacidadesDispositivo(
            tieneGps = true,
            tieneAcelerometro = true,
            tieneBrujula = true,
            tieneCamara = true
        )
    ): PerfilViewModel {
        return PerfilViewModel(
            obtenerUsuarioActualUseCase = ObtenerUsuarioActualUseCase(autenticacion),
            cerrarSesionUseCase = CerrarSesionUseCase(autenticacion, FalsoSenderoRepositorio(), FalsoLanzadorSeguimiento(), FalsoNotificador()),
            preferenciasAplicacion = preferencias,
            observarRecorridosUseCase = ObservarRecorridosUseCase(repositorioRecorridos, autenticacion),
            obtenerCapacidadesDispositivoUseCase = ObtenerCapacidadesDispositivoUseCase(FalsoDetectorCapacidades(capacidades)),
            controlSeguimiento = controlSeguimiento
        )
    }

    // Verificar que el modo demostración arranca desactivado
    @Test
    fun `el modo demostracion arranca desactivado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esModoDemostracion)
    }

    // Verificar que el interruptor guarda la preferencia y la refleja en el estado visual
    @Test
    fun `activar el modo demostracion lo guarda y lo refleja`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(PerfilEvento.AlCambiarModoDemostracion(activo = true))
        advanceUntilIdle()

        assertTrue(preferencias.esModoDemostracion.value)
        assertTrue(viewModel.estadoVisual.value.esModoDemostracion)
    }

    // Verificar que desactivar el modo lo vuelve a apagar
    @Test
    fun `desactivar el modo demostracion lo apaga`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(PerfilEvento.AlCambiarModoDemostracion(activo = true))
        advanceUntilIdle()

        viewModel.procesarEvento(PerfilEvento.AlCambiarModoDemostracion(activo = false))
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esModoDemostracion)
    }

    // Verificar que el historial llega del más reciente al más viejo
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

    // Verificar que sin recorridos la lista queda vacía
    @Test
    fun `sin recorridos el historial es vacio`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.recorridos.isEmpty())
    }

    // Verificar que las cuatro funciones del equipo se listan con su disponibilidad
    @Test
    fun `las funciones del dispositivo reflejan las capacidades detectadas`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(
            CapacidadesDispositivo(tieneGps = true, tieneAcelerometro = false, tieneBrujula = false, tieneCamara = true)
        )
        advanceUntilIdle()

        assertEquals(
            listOf(
                EstadoFuncion(FuncionDispositivo.GPS, estaDisponible = true),
                EstadoFuncion(FuncionDispositivo.ACELEROMETRO, estaDisponible = false),
                EstadoFuncion(FuncionDispositivo.BRUJULA, estaDisponible = false),
                EstadoFuncion(FuncionDispositivo.CAMARA, estaDisponible = true)
            ),
            viewModel.estadoVisual.value.funcionesDispositivo
        )
    }

    // Verificar que cada función explica qué se pierde si falta
    @Test
    fun `cada funcion explica que se pierde si falta`() {
        assertEquals(
            "Sin brújula, la dirección se calcula solo mientras caminás",
            explicarFuncionNoDisponible(FuncionDispositivo.BRUJULA)
        )
        assertEquals(
            "Sin GPS, la ubicación sale del wifi o de la red móvil: es menos precisa y no funciona sin señal",
            explicarFuncionNoDisponible(FuncionDispositivo.GPS)
        )
        FuncionDispositivo.entries.forEach { funcion ->
            assertTrue(explicarFuncionNoDisponible(funcion).isNotBlank())
        }
    }

    // Verificar que tocar un recorrido emite el efecto de navegación a su detalle
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

    // Verificar que tocar cerrar sesión pide confirmación y todavía no cierra la sesión
    @Test
    fun `tocar cerrar sesion muestra la confirmacion sin cerrar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.procesarEvento(PerfilEvento.AlTocarCerrarSesion)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esDialogoCerrarSesionVisible)
        assertEquals(0, autenticacion.cantidadCierresSesion)
    }

    // Verificar que confirmar cierra la sesión, oculta el diálogo y navega al inicio de sesión
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

    // Verificar que cancelar la confirmación conserva la sesión
    @Test
    fun `cancelar cerrar sesion conserva la sesion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        viewModel.procesarEvento(PerfilEvento.AlTocarCerrarSesion)

        viewModel.procesarEvento(PerfilEvento.AlCancelarCerrarSesion)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoCerrarSesionVisible)
        assertEquals(0, autenticacion.cantidadCierresSesion)
    }

    // Verificar que el estado avisa cuando hay un recorrido en curso y deja de avisar al terminar
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

    // Verificar que un fallo al leer el historial queda como mensaje y no como lista vacía
    @Test
    fun `un fallo al leer el historial muestra un mensaje de error`() = runTest(despachadorPrueba) {
        autenticacion.usuarioActual = null
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.estadoVisual.value.mensajeError)
    }

    // Verificar que con el historial disponible no hay mensaje de error
    @Test
    fun `con el historial disponible no hay mensaje de error`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.mensajeError)
    }
}
