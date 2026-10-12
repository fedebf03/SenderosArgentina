package com.unaj.senderos.presentation.detalle

import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoControlSeguimiento
import com.unaj.senderos.falsos.FalsoLanzadorSeguimiento
import com.unaj.senderos.falsos.FalsoPreferenciasAplicacion
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearSenderoDePrueba
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
class DetalleSenderoViewModelTest {

    private val despachadorPrueba = StandardTestDispatcher()
    private val lanzador = FalsoLanzadorSeguimiento()
    private val control = FalsoControlSeguimiento()
    private val preferencias = FalsoPreferenciasAplicacion()
    private val efectosRecibidos = mutableListOf<DetalleSenderoEfecto>()

    private val recorridoValido = listOf(
        PuntoRecorrido(latitud = -41.10, longitud = -71.30),
        PuntoRecorrido(latitud = -41.11, longitud = -71.31)
    )

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

    // Crear el ViewModel con un sendero de prueba y recolectar sus efectos en una lista
    private fun crearViewModel(recorrido: List<PuntoRecorrido>): DetalleSenderoViewModel {
        val repositorio = FalsoSenderoRepositorio(
            listOf(
                crearSenderoDePrueba(id = "sendero-1", recorrido = recorrido),
                crearSenderoDePrueba(id = "sendero-2", nombre = "Laguna Azul", recorrido = recorridoValido)
            )
        )
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val viewModel = DetalleSenderoViewModel(
            idSendero = "sendero-1",
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            alternarFavoritoUseCase = AlternarFavoritoUseCase(repositorio, autenticacion),
            eliminarSenderoUseCase = EliminarSenderoUseCase(repositorio, autenticacion),
            lanzadorSeguimiento = lanzador,
            controlSeguimiento = control,
            preferenciasAplicacion = preferencias
        )
        CoroutineScope(despachadorPrueba).launch {
            viewModel.efectoVisual.collect { efecto -> efectosRecibidos.add(efecto) }
        }
        return viewModel
    }

    // Verificar que un sendero con menos de dos puntos avisa y no pide permiso
    @Test
    fun `iniciar recorrido sin recorrido muestra un aviso`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorrido = listOf(recorridoValido.first()))
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertEquals(
            listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.MostrarAviso("Este sendero no tiene recorrido")),
            efectosRecibidos
        )
        assertTrue(lanzador.idsIniciados.isEmpty())
    }

    // Verificar que con recorrido se emite el pedido de permiso de ubicación
    @Test
    fun `iniciar recorrido con recorrido pide el permiso de ubicacion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
        assertTrue(lanzador.idsIniciados.isEmpty())
    }

    // Verificar que con el permiso concedido se lanza el seguimiento y se navega al recorrido
    @Test
    fun `con el permiso concedido lanza el seguimiento y navega al recorrido`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConcederPermisoUbicacion)
        advanceUntilIdle()

        assertEquals(listOf("sendero-1"), lanzador.idsIniciados)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.NavegarARecorrido("sendero-1")), efectosRecibidos)
    }

    // Verificar que con el permiso negado se explica el motivo y no se lanza nada
    @Test
    fun `con el permiso negado avisa y no lanza el seguimiento`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlDenegarPermisoUbicacion)
        advanceUntilIdle()

        assertTrue(lanzador.idsIniciados.isEmpty())
        val aviso = efectosRecibidos.single() as DetalleSenderoEfecto.MostrarAviso
        assertTrue(aviso.mensaje.contains("ubicación"))
    }

    // Verificar que conceder el permiso sin recorrido válido no lanza el seguimiento
    @Test
    fun `conceder el permiso sin recorrido no lanza el seguimiento`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorrido = emptyList())
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConcederPermisoUbicacion)
        advanceUntilIdle()

        assertTrue(lanzador.idsIniciados.isEmpty())
        assertEquals(
            listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.MostrarAviso("Este sendero no tiene recorrido")),
            efectosRecibidos
        )
    }

    // Verificar que conceder solo la ubicación aproximada avisa que hace falta la precisa y no lanza nada
    @Test
    fun `con solo la ubicacion aproximada avisa y no lanza el seguimiento`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConcederSoloUbicacionAproximada)
        advanceUntilIdle()

        assertTrue(lanzador.idsIniciados.isEmpty())
        assertEquals(
            listOf<DetalleSenderoEfecto>(
                DetalleSenderoEfecto.MostrarAviso("Para seguir el recorrido hace falta la ubicación precisa")
            ),
            efectosRecibidos
        )
    }

    // Verificar que con un recorrido activo del mismo sendero se navega sin volver a lanzarlo
    @Test
    fun `con el recorrido ya activo en este sendero navega sin lanzar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "sendero-1", trazado = recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.hayRecorridoEnCursoEnEsteSendero)
        assertTrue(lanzador.idsIniciados.isEmpty())
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.NavegarARecorrido("sendero-1")), efectosRecibidos)
    }

    // Verificar que con un recorrido activo de otro sendero se pide confirmar el reemplazo con su nombre
    @Test
    fun `con el recorrido activo de otro sendero pide confirmar el reemplazo`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "sendero-2", trazado = recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.hayRecorridoEnCursoEnEsteSendero)
        assertEquals("Laguna Azul", viewModel.estadoVisual.value.nombreSenderoEnCursoAReemplazar)
        assertTrue(efectosRecibidos.isEmpty())
    }

    // Verificar que confirmar el reemplazo continúa con el pedido de permiso
    @Test
    fun `confirmar el reemplazo pide el permiso de ubicacion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "sendero-2", trazado = recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarReemplazarRecorrido)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.nombreSenderoEnCursoAReemplazar)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    // Verificar que cancelar el reemplazo cierra la confirmación sin otros efectos
    @Test
    fun `cancelar el reemplazo no hace nada`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "sendero-2", trazado = recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlCancelarReemplazarRecorrido)
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.nombreSenderoEnCursoAReemplazar)
        assertTrue(efectosRecibidos.isEmpty())
        assertTrue(lanzador.idsIniciados.isEmpty())
    }

    // Verificar que con el modo demostración apagado el recorrido se lanza real
    @Test
    fun `sin modo demostracion el recorrido se lanza real`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConcederPermisoUbicacion)
        advanceUntilIdle()

        assertEquals(listOf(false), lanzador.iniciosSimulados)
        assertFalse(viewModel.estadoVisual.value.esModoDemostracion)
    }

    // Verificar que con el modo demostración activo el recorrido se lanza simulado
    @Test
    fun `con modo demostracion el recorrido se lanza simulado`() = runTest(despachadorPrueba) {
        preferencias.cambiarModoDemostracion(true)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlConcederPermisoUbicacion)
        advanceUntilIdle()

        assertEquals(listOf(true), lanzador.iniciosSimulados)
        assertTrue(viewModel.estadoVisual.value.esModoDemostracion)
    }

    // Verificar que un error al buscar el nombre del otro sendero usa el texto de respaldo
    @Test
    fun `si falla la busqueda del otro sendero usa el nombre de respaldo`() = runTest(despachadorPrueba) {
        val repositorio = FalsoSenderoRepositorio(
            listOf(crearSenderoDePrueba(id = "sendero-1", recorrido = recorridoValido))
        )
        val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "a@b.com"))
        val viewModel = DetalleSenderoViewModel(
            idSendero = "sendero-1",
            observarDetalleSenderoUseCase = ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            alternarFavoritoUseCase = AlternarFavoritoUseCase(repositorio, autenticacion),
            eliminarSenderoUseCase = EliminarSenderoUseCase(repositorio, autenticacion),
            lanzadorSeguimiento = lanzador,
            controlSeguimiento = control,
            preferenciasAplicacion = preferencias
        )
        advanceUntilIdle()
        control.estadoEmitido.value = EstadoSeguimiento(idSendero = "sendero-2", trazado = recorridoValido)
        repositorio.errorEnFlujo = IllegalStateException("fallo de lectura")
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertEquals("otro sendero", viewModel.estadoVisual.value.nombreSenderoEnCursoAReemplazar)
    }

    // Verificar que un recorrido simulado en curso de este sendero se distingue del real
    @Test
    fun `un recorrido simulado en este sendero se marca como simulado`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        control.estadoEmitido.value = EstadoSeguimiento(
            idSendero = "sendero-1",
            trazado = recorridoValido,
            esSimulado = true
        )
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.hayRecorridoEnCursoEnEsteSendero)
        assertTrue(viewModel.estadoVisual.value.esRecorridoEnCursoSimulado)
    }
}
