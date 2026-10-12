package com.unaj.senderos.presentation.detalle

import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.usecase.DescargarMapaSinConexionUseCase
import com.unaj.senderos.domain.usecase.EliminarMapaSinConexionUseCase
import com.unaj.senderos.domain.usecase.ObservarMapaSinConexionUseCase
import com.unaj.senderos.falsos.FalsoGestorMapasSinConexion
import com.unaj.senderos.falsos.FalsoMonitorConectividad
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
    private val monitorConectividad = FalsoMonitorConectividad()
    private val gestorMapas = FalsoGestorMapasSinConexion().apply { estado.value = EstadoMapaSinConexion.Disponible(1_000L) }
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

    private var capacidades = CapacidadesDispositivo(
        tieneGps = true,
        tieneAcelerometro = true,
        tieneBrujula = true,
        tieneCamara = true
    )

    // Verificar que en un equipo sin GPS iniciar un recorrido real pide confirmar antes del permiso
    @Test
    fun `sin gps iniciar un recorrido real muestra el aviso antes del permiso`() = runTest(despachadorPrueba) {
        capacidades = capacidades.copy(tieneGps = false)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esDialogoSinGpsVisible)
        assertTrue(efectosRecibidos.isEmpty())
    }

    // Verificar que "Iniciar igual" cierra el aviso y sigue con el pedido de permiso
    @Test
    fun `iniciar igual sin gps continua con el permiso`() = runTest(despachadorPrueba) {
        capacidades = capacidades.copy(tieneGps = false)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)

        viewModel.procesarEvento(DetalleSenderoEvento.AlIniciarSinGps)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoSinGpsVisible)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    // Verificar que cancelar el aviso no inicia nada
    @Test
    fun `cancelar el aviso sin gps no inicia el recorrido`() = runTest(despachadorPrueba) {
        capacidades = capacidades.copy(tieneGps = false)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)

        viewModel.procesarEvento(DetalleSenderoEvento.AlCancelarSinGps)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoSinGpsVisible)
        assertTrue(efectosRecibidos.isEmpty())
    }

    // Verificar que en simulación no se muestra el aviso aunque el equipo no tenga GPS
    @Test
    fun `en simulacion no se avisa la falta de gps`() = runTest(despachadorPrueba) {
        capacidades = capacidades.copy(tieneGps = false)
        preferencias.cambiarModoDemostracion(true)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoSinGpsVisible)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
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
            preferenciasAplicacion = preferencias,
            observarMapaSinConexionUseCase = ObservarMapaSinConexionUseCase(gestorMapas, autenticacion),
            descargarMapaSinConexionUseCase = DescargarMapaSinConexionUseCase(gestorMapas, autenticacion),
            eliminarMapaSinConexionUseCase = EliminarMapaSinConexionUseCase(gestorMapas, autenticacion),
            monitorConectividad = monitorConectividad,
            capacidadesDispositivo = capacidades
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
            preferenciasAplicacion = preferencias,
            observarMapaSinConexionUseCase = ObservarMapaSinConexionUseCase(gestorMapas, autenticacion),
            descargarMapaSinConexionUseCase = DescargarMapaSinConexionUseCase(gestorMapas, autenticacion),
            eliminarMapaSinConexionUseCase = EliminarMapaSinConexionUseCase(gestorMapas, autenticacion),
            monitorConectividad = monitorConectividad,
            capacidadesDispositivo = capacidades
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

    // Verificar que elegir un punto de interés guarda su coordenada y repite el enfoque sobre el mismo punto
    @Test
    fun `seleccionar un punto de interes enfoca su coordenada y repite el enfoque`() = runTest(despachadorPrueba) {
        val recorridoConInteres = listOf(
            PuntoRecorrido(latitud = -41.10, longitud = -71.30),
            PuntoRecorrido(latitud = -41.11, longitud = -71.31, tipo = com.unaj.senderos.domain.model.TipoPunto.AGUA)
        )
        val viewModel = crearViewModel(recorridoConInteres)
        advanceUntilIdle()
        assertEquals(0, viewModel.estadoVisual.value.contadorEnfoque)
        assertEquals(listOf(1), viewModel.estadoVisual.value.puntosDeInteres.map { punto -> punto.indice })

        viewModel.procesarEvento(DetalleSenderoEvento.AlSeleccionarPuntoDeInteres(1))
        advanceUntilIdle()
        val primero = viewModel.estadoVisual.value
        viewModel.procesarEvento(DetalleSenderoEvento.AlSeleccionarPuntoDeInteres(1))
        advanceUntilIdle()
        val segundo = viewModel.estadoVisual.value

        assertEquals(com.unaj.senderos.domain.model.Coordenada(-41.11, -71.31), primero.puntoEnfocado)
        assertEquals(1, primero.contadorEnfoque)
        assertEquals(2, segundo.contadorEnfoque)
    }

    // Verificar que un índice inexistente no cambia el enfoque
    @Test
    fun `seleccionar un indice inexistente no cambia el enfoque`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlSeleccionarPuntoDeInteres(7))
        advanceUntilIdle()

        assertNull(viewModel.estadoVisual.value.puntoEnfocado)
        assertEquals(0, viewModel.estadoVisual.value.contadorEnfoque)
    }

    // Verificar que el estado del mapa sin conexión se refleja y cambia con el flujo
    @Test
    fun `refleja los estados del mapa sin conexion`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        assertEquals(EstadoMapaSinConexion.Disponible(1_000L), viewModel.estadoVisual.value.estadoMapaSinConexion)

        gestorMapas.estado.value = EstadoMapaSinConexion.Descargando(40, 0)
        advanceUntilIdle()
        assertEquals(EstadoMapaSinConexion.Descargando(40, 0), viewModel.estadoVisual.value.estadoMapaSinConexion)

        gestorMapas.estado.value = EstadoMapaSinConexion.ConError("Sin conexión")
        advanceUntilIdle()
        assertEquals(EstadoMapaSinConexion.ConError("Sin conexión"), viewModel.estadoVisual.value.estadoMapaSinConexion)
    }

    // Verificar que tocar descargar llama al caso de uso con el sendero
    @Test
    fun `tocar descargar el mapa llama al caso de uso`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa)
        advanceUntilIdle()

        assertEquals(listOf("usuario-1/sendero-1"), gestorMapas.descargas)
    }

    // Verificar que eliminar pide confirmación y solo al confirmar llama al caso de uso
    @Test
    fun `eliminar el mapa pide confirmacion y luego elimina`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEliminarMapa)
        advanceUntilIdle()
        assertTrue(viewModel.estadoVisual.value.esDialogoEliminarMapaVisible)
        assertTrue(gestorMapas.eliminaciones.isEmpty())

        viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminarMapa)
        advanceUntilIdle()
        assertFalse(viewModel.estadoVisual.value.esDialogoEliminarMapaVisible)
        assertEquals(listOf("usuario-1/sendero-1"), gestorMapas.eliminaciones)
    }

    // Verificar que cancelar la eliminación del mapa no elimina nada
    @Test
    fun `cancelar la eliminacion del mapa no elimina`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEliminarMapa)

        viewModel.procesarEvento(DetalleSenderoEvento.AlCancelarEliminarMapa)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esDialogoEliminarMapaVisible)
        assertTrue(gestorMapas.eliminaciones.isEmpty())
    }

    // Verificar que con el mapa disponible iniciar no sugiere descargar
    @Test
    fun `con el mapa disponible no se sugiere descargar`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    // Verificar que sin el mapa descargado se sugiere antes de pedir el permiso
    @Test
    fun `sin el mapa descargado se sugiere antes de pedir el permiso`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.NoDescargado
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
        assertTrue(efectosRecibidos.isEmpty())
    }

    // Verificar que un mapa desactualizado también dispara la sugerencia
    @Test
    fun `con el mapa desactualizado tambien se sugiere`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.Desactualizado(500L)
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertTrue(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
    }

    // Verificar que "Descargar e iniciar" lanza la descarga y continúa con el permiso
    @Test
    fun `descargar e iniciar descarga y continua con el inicio`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.NoDescargado
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlDescargarEIniciar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
        assertEquals(listOf("usuario-1/sendero-1"), gestorMapas.descargas)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    // Verificar que "Iniciar sin descargar" continúa con el permiso sin descargar
    @Test
    fun `iniciar sin descargar continua con el inicio sin descargar`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.NoDescargado
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlIniciarSinDescargar)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
        assertTrue(gestorMapas.descargas.isEmpty())
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    // Verificar que se puede cancelar o eliminar el mapa en cada estado que lo permite
    @Test
    fun `cancelar o eliminar el mapa funciona en cada estado`() = runTest(despachadorPrueba) {
        val estados = listOf(
            EstadoMapaSinConexion.Descargando(30, 100L),
            EstadoMapaSinConexion.EsperandoConexion(30, 100L),
            EstadoMapaSinConexion.ConError("Falló"),
            EstadoMapaSinConexion.Desactualizado(500L),
            EstadoMapaSinConexion.Disponible(500L)
        )
        val viewModel = crearViewModel(recorridoValido)
        estados.forEach { estado ->
            gestorMapas.estado.value = estado
            advanceUntilIdle()

            viewModel.procesarEvento(DetalleSenderoEvento.AlTocarEliminarMapa)
            assertTrue(viewModel.estadoVisual.value.esDialogoEliminarMapaVisible)
            viewModel.procesarEvento(DetalleSenderoEvento.AlConfirmarEliminarMapa)
            advanceUntilIdle()
        }

        assertEquals(estados.size, gestorMapas.eliminaciones.size)
    }

    // Verificar que sin conexión no se sugiere descargar y se sigue directo con el inicio
    @Test
    fun `sin conexion no se sugiere descargar`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.NoDescargado
        monitorConectividad.hayConexionActual = false
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarIniciarRecorrido)
        advanceUntilIdle()

        assertFalse(viewModel.estadoVisual.value.esSugerenciaDescargaVisible)
        assertEquals(listOf<DetalleSenderoEfecto>(DetalleSenderoEfecto.SolicitarPermisoUbicacion), efectosRecibidos)
    }

    // Verificar que un doble toque en descargar pide una sola descarga
    @Test
    fun `el doble toque en descargar se ignora`() = runTest(despachadorPrueba) {
        gestorMapas.estado.value = EstadoMapaSinConexion.NoDescargado
        val viewModel = crearViewModel(recorridoValido)
        advanceUntilIdle()

        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa)
        viewModel.procesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa)
        advanceUntilIdle()

        assertEquals(1, gestorMapas.descargas.size)
    }

    // Verificar que descargar se ignora mientras el estado indica una descarga activa
    @Test
    fun `descargar se ignora si ya hay una descarga activa`() = runTest(despachadorPrueba) {
        val viewModel = crearViewModel(recorridoValido)
        listOf(EstadoMapaSinConexion.Descargando(10, 0L), EstadoMapaSinConexion.EsperandoConexion(10, 0L)).forEach { estado ->
            gestorMapas.estado.value = estado
            advanceUntilIdle()

            viewModel.procesarEvento(DetalleSenderoEvento.AlTocarDescargarMapa)
            viewModel.procesarEvento(DetalleSenderoEvento.AlDescargarEIniciar)
            advanceUntilIdle()
        }

        assertTrue(gestorMapas.descargas.isEmpty())
    }
}
