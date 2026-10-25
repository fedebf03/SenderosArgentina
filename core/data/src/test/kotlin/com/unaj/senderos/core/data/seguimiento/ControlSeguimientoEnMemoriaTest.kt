package com.unaj.senderos.core.data.seguimiento

import com.unaj.senderos.core.domain.model.AvisoRecorrido
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.LecturaUbicacion
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoProveedorUbicacion
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07

@OptIn(ExperimentalCoroutinesApi::class)
class ControlSeguimientoEnMemoriaTest {

    private val ubicaciones = MutableSharedFlow<LecturaUbicacion>(extraBufferCapacity = 16)
    private val proveedor = FalsoProveedorUbicacion(ubicaciones = ubicaciones)
    private val mirador = PuntoRecorrido(LATITUD_BASE - 0.002, LONGITUD_BASE, TipoPunto.MIRADOR)
    private val recorridoValido = listOf(
        PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE),
        mirador,
        PuntoRecorrido(LATITUD_BASE - 0.004, LONGITUD_BASE)
    )
    private val repositorio = FalsoSenderoRepositorio(
        listOf(
            construirSendero("con-recorrido", recorridoValido),
            construirSendero("otro", recorridoValido),
            construirSendero("sin-recorrido", recorridoValido.take(1))
        )
    )
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"))
    private val repositorioRecorridos = FalsoRecorridoRepositorio()
    private var instante = 1_000L

    private fun construirSendero(id: String, recorrido: List<PuntoRecorrido>): Sendero {
        return crearSenderoDePrueba(id = id, recorrido = recorrido).copy(tipoRecorrido = TipoRecorrido.LINEAL)
    }

    private fun TestScope.crearControl(): ControlSeguimientoEnMemoria {
        return ControlSeguimientoEnMemoria(
            proveedor,
            ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            repositorioRecorridos,
            com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio(),
            com.unaj.senderos.core.testing.FalsoAlmacenFotos(),
            backgroundScope + UnconfinedTestDispatcher(testScheduler)
        ) { instante }
    }

    private fun alSur(grados: Double, rumbo: Float? = null, velocidad: Float? = null): LecturaUbicacion {
        return LecturaUbicacion(Coordenada(LATITUD_BASE - grados, LONGITUD_BASE), rumbo, velocidad, precisionMetros = null)
    }

    @Test
    fun `iniciar publica el estado inicial del sendero`() = runTest {
        val control = crearControl()

        control.iniciar("con-recorrido")

        assertEquals("con-recorrido", control.estado.value?.idSendero)
        assertEquals(0.4448, control.estado.value?.distanciaRestanteKilometros ?: -1.0, 0.004)
    }

    @Test
    fun `iniciar un sendero con menos de dos puntos falla y no deja estado`() = runTest {
        val control = crearControl()

        try {
            control.iniciar("sin-recorrido")
            fail("Se esperaba una excepción")
        } catch (excepcion: IllegalStateException) {
            assertTrue(excepcion.message.orEmpty().contains("recorrido"))
        }
        assertNull(control.estado.value)
    }

    @Test
    fun `iniciar un sendero inexistente falla`() = runTest {
        val control = crearControl()

        try {
            control.iniciar("no-existe")
            fail("Se esperaba una excepción")
        } catch (excepcion: IllegalStateException) {
            assertNotNull(excepcion.message)
        }
        assertNull(control.estado.value)
    }

    @Test
    fun `las ubicaciones actualizan el estado con el reloj inyectado`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")

        ubicaciones.emit(alSur(0.0))
        instante = 61_000L
        ubicaciones.emit(alSur(0.001))

        assertEquals(0.1112, control.estado.value?.distanciaRecorridaKilometros ?: -1.0, 0.004)
        assertEquals(60_000L, control.estado.value?.tiempoTranscurridoMilisegundos)
    }

    @Test
    fun `los avisos de proximidad se emiten por el flujo de avisos`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")

        val avisosRecibidos = mutableListOf<AvisoRecorrido>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            control.avisos.collect { aviso -> avisosRecibidos.add(aviso) }
        }

        ubicaciones.emit(alSur(0.0))
        ubicaciones.emit(alSur(0.0017))
        ubicaciones.emit(alSur(0.0019))

        assertEquals(listOf<AvisoRecorrido>(AvisoRecorrido.CercaDePunto(indice = 1, punto = mirador)), avisosRecibidos)
    }

    @Test
    fun `detener deja el estado en null e ignora nuevas ubicaciones`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")

        control.detener()
        ubicaciones.emit(alSur(0.001))

        assertNull(control.estado.value)
    }

    @Test
    fun `iniciar otro sendero finaliza el anterior`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")
        ubicaciones.emit(alSur(0.001))

        control.iniciar("otro")

        assertEquals("otro", control.estado.value?.idSendero)
        assertEquals(0.0, control.estado.value?.distanciaRecorridaKilometros ?: -1.0, 0.0001)
        assertEquals(1, ubicaciones.subscriptionCount.value)
    }

    @Test
    fun `un error del proveedor de ubicacion deja el estado en null`() = runTest {
        proveedor.ubicaciones = flow { throw SecurityException("Sin permiso") }
        val control = crearControl()

        control.iniciar("con-recorrido")

        assertNull(control.estado.value)
    }

    @Test
    fun `iniciar otro sendero con uno activo nunca publica null`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")
        val estadosRecibidos = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            control.estado.collect { estadoActual -> estadosRecibidos.add(estadoActual?.idSendero) }
        }

        control.iniciar("otro")

        assertEquals(listOf<String?>("con-recorrido", "otro"), estadosRecibidos)
    }

    @Test
    fun `el rumbo y la velocidad de la lectura llegan al estado`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")

        ubicaciones.emit(alSur(0.0, rumbo = 180f, velocidad = 1.2f))

        assertEquals(180f, control.estado.value?.rumboGrados)
        assertEquals(1.2f, control.estado.value?.velocidadMetrosPorSegundo)
    }

    @Test
    fun `un recorrido simulado marca el estado y avanza con el tiempo virtual`() = runTest {
        val control = crearControl()

        control.iniciar("con-recorrido", esSimulado = true)
        advanceTimeBy(10_500L)

        assertEquals(true, control.estado.value?.esSimulado)
        assertTrue((control.estado.value?.distanciaRecorridaKilometros ?: 0.0) >= 0.01)
        assertEquals(0, ubicaciones.subscriptionCount.value)
    }

    @Test
    fun `un recorrido real no esta marcado como simulado`() = runTest {
        val control = crearControl()

        control.iniciar("con-recorrido")

        assertEquals(false, control.estado.value?.esSimulado)
        assertEquals(1, ubicaciones.subscriptionCount.value)
    }

    @Test
    fun `el multiplicador de la simulacion acelera el avance`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido", esSimulado = true)

        control.cambiarMultiplicadorSimulacion(20)
        advanceTimeBy(5_500L)

        assertTrue((control.estado.value?.distanciaRecorridaKilometros ?: 0.0) >= 0.1)
    }

    @Test
    fun `simular el desvio emite el aviso de desvio`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido", esSimulado = true)
        val avisosRecibidos = mutableListOf<AvisoRecorrido>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            control.avisos.collect { aviso -> avisosRecibidos.add(aviso) }
        }

        control.simularDesvio()
        advanceTimeBy(3_500L)

        assertTrue(avisosRecibidos.contains(AvisoRecorrido.Desvio))
    }

    @Test
    fun `las acciones de simulacion no afectan a un recorrido real`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")

        control.cambiarMultiplicadorSimulacion(20)
        control.simularDesvio()
        ubicaciones.emit(alSur(0.0))

        assertEquals(false, control.estado.value?.estaDesviado)
        assertEquals(0.0, control.estado.value?.distanciaRecorridaKilometros ?: -1.0, 0.0001)
    }

    @Test
    fun `el estado refleja el multiplicador vigente`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido", esSimulado = true)
        assertEquals(1, control.estado.value?.multiplicadorSimulacion)

        control.cambiarMultiplicadorSimulacion(20)
        advanceTimeBy(3_500L)

        assertEquals(20, control.estado.value?.multiplicadorSimulacion)
    }

    @Test
    fun `un multiplicador invalido se rechaza`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido", esSimulado = true)

        try {
            control.cambiarMultiplicadorSimulacion(3)
            fail("Se esperaba una excepción")
        } catch (excepcion: IllegalArgumentException) {
            assertNotNull(excepcion.message)
        }
        assertEquals(1, control.estado.value?.multiplicadorSimulacion)
    }

    @Test
    fun `un permiso revocado se expone como motivo de fin`() = runTest {
        proveedor.ubicaciones = flow { throw SecurityException("Sin permiso") }
        val control = crearControl()

        control.iniciar("con-recorrido")

        assertNull(control.estado.value)
        assertEquals(MotivoFinSeguimiento.PERMISO_DE_UBICACION_REVOCADO, control.motivoFin.value)
    }

    @Test
    fun `otro fallo se expone como error de ubicacion y se limpia al reiniciar`() = runTest {
        proveedor.ubicaciones = flow { throw IllegalStateException("Fallo") }
        val control = crearControl()
        control.iniciar("con-recorrido")
        assertEquals(MotivoFinSeguimiento.ERROR_DE_UBICACION, control.motivoFin.value)

        proveedor.ubicaciones = ubicaciones
        control.iniciar("con-recorrido")

        assertNull(control.motivoFin.value)
    }

    @Test
    fun `detener por pedido del usuario no deja motivo de fin`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")

        control.detener()

        assertNull(control.motivoFin.value)
    }

    @Test
    fun `detener guarda el recorrido realizado con sus datos`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")
        ubicaciones.emit(alSur(0.0, velocidad = 1.0f))
        instante = 61_000L
        ubicaciones.emit(alSur(0.001, velocidad = 2.0f))
        instante = 121_000L

        control.detener()

        assertEquals(1, repositorioRecorridos.guardados.size)
        val recorrido = repositorioRecorridos.guardados.single()
        assertEquals("recorrido-1000", recorrido.id)
        assertEquals("usuario-1", recorrido.idUsuario)
        assertEquals("con-recorrido", recorrido.idSendero)
        assertEquals("Cerro Catedral", recorrido.nombreSendero)
        assertEquals(1_000L, recorrido.inicioMilisegundos)
        assertEquals(121_000L, recorrido.finMilisegundos)
        assertEquals(120_000L, recorrido.duracionMilisegundos)
        assertEquals(0.1112, recorrido.distanciaKilometros, 0.004)
        assertEquals(recorrido.distanciaKilometros / (120.0 / 3600.0), recorrido.velocidadPromedioKilometrosPorHora, 0.0001)
        assertEquals(7.2, recorrido.velocidadMaximaKilometrosPorHora, 0.001)
        assertNull(recorrido.pasos)
        assertNull(recorrido.ritmoPasosPorMinuto)
        assertEquals(2, recorrido.trazadoCaminado.size)
        assertEquals(false, recorrido.esSimulado)
        assertEquals(EstadoSincronizacion.PENDIENTE, recorrido.estadoSincronizacion)
    }

    @Test
    fun `detener cierra el trazado guardado en la ultima posicion recibida`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")
        ubicaciones.emit(alSur(0.0))
        // A unos 2 m: no entra al trazado mientras se camina, pero es donde terminó el usuario
        ubicaciones.emit(LecturaUbicacion(Coordenada(LATITUD_BASE - 0.00002, LONGITUD_BASE), null, null, precisionMetros = 5f))
        ubicaciones.emit(LecturaUbicacion(Coordenada(LATITUD_BASE - 0.0006, LONGITUD_BASE), null, null, precisionMetros = 400f))

        control.detener()

        val trazado = repositorioRecorridos.guardados.single().trazadoCaminado
        assertEquals(listOf(Coordenada(LATITUD_BASE, LONGITUD_BASE), Coordenada(LATITUD_BASE - 0.00002, LONGITUD_BASE)), trazado)
    }

    @Test
    fun `detener sin posiciones no guarda recorrido`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")

        control.detener()

        assertTrue(repositorioRecorridos.guardados.isEmpty())
    }

    @Test
    fun `detener dos veces guarda una sola vez`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")
        ubicaciones.emit(alSur(0.0))

        control.detener()
        control.detener()

        assertEquals(1, repositorioRecorridos.guardados.size)
    }

    @Test
    fun `un error de ubicacion con posiciones guarda el recorrido`() = runTest {
        proveedor.ubicaciones = flow {
            emit(alSur(0.0))
            emit(alSur(0.001))
            throw IllegalStateException("Fallo")
        }
        val control = crearControl()

        control.iniciar("con-recorrido")

        assertNull(control.estado.value)
        assertEquals(1, repositorioRecorridos.guardados.size)
        assertEquals(2, repositorioRecorridos.guardados.single().trazadoCaminado.size)
    }

    @Test
    fun `iniciar otro sendero guarda el recorrido anterior`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")
        ubicaciones.emit(alSur(0.0))

        control.iniciar("otro")

        assertEquals(1, repositorioRecorridos.guardados.size)
        assertEquals("con-recorrido", repositorioRecorridos.guardados.single().idSendero)
    }

    @Test
    fun `un guardado fallido no rompe la detencion`() = runTest {
        repositorioRecorridos.resultadoGuardar = Result.failure(IllegalStateException("Sin espacio"))
        val control = crearControl()
        control.iniciar("con-recorrido")
        ubicaciones.emit(alSur(0.0))

        control.detener()

        assertNull(control.estado.value)
        assertEquals(1, repositorioRecorridos.guardados.size)
    }

    @Test
    fun `una excepcion al guardar no rompe la detencion`() = runTest {
        repositorioRecorridos.errorAlGuardar = IllegalStateException("Fallo inesperado")
        val control = crearControl()
        control.iniciar("con-recorrido")
        ubicaciones.emit(alSur(0.0))

        control.detener()

        assertNull(control.estado.value)
    }

    // Se guarda con el usuario que lo inició
    @Test
    fun `cerrar sesion con recorrido activo guarda el recorrido`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")
        ubicaciones.emit(alSur(0.0))
        autenticacion.usuarioActual = null

        control.detener()

        assertEquals(1, repositorioRecorridos.guardados.size)
        assertEquals("usuario-1", repositorioRecorridos.guardados.single().idUsuario)
        assertNull(control.estado.value)
    }

    @Test
    fun `un recorrido simulado se guarda marcado como simulado`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido", esSimulado = true)
        advanceTimeBy(10_500L)

        control.detener()

        assertEquals(1, repositorioRecorridos.guardados.size)
        assertEquals(true, repositorioRecorridos.guardados.single().esSimulado)
    }

    @Test
    fun `la simulacion que llega al final avisa la llegada una sola vez`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido", esSimulado = true)
        control.cambiarMultiplicadorSimulacion(20)
        val avisosRecibidos = mutableListOf<AvisoRecorrido>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            control.avisos.collect { aviso -> avisosRecibidos.add(aviso) }
        }

        advanceTimeBy(30_000L)

        assertEquals(1, avisosRecibidos.count { aviso -> aviso == AvisoRecorrido.LlegadaAlFinal })
        assertTrue(control.estado.value?.llegoAlFinal == true)
    }
}
