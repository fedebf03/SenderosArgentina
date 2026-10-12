package com.unaj.senderos.data.seguimiento

import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoProveedorUbicacion
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearSenderoDePrueba
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
    private var instante = 1_000L

    // Construir un sendero lineal de prueba con el recorrido indicado
    private fun construirSendero(id: String, recorrido: List<PuntoRecorrido>): Sendero {
        return crearSenderoDePrueba(id = id, recorrido = recorrido).copy(tipoRecorrido = TipoRecorrido.LINEAL)
    }

    // Construir el control con un alcance que ejecuta de inmediato y un reloj controlado por la prueba
    private fun TestScope.crearControl(): ControlSeguimientoEnMemoria {
        return ControlSeguimientoEnMemoria(
            proveedor,
            ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            backgroundScope + UnconfinedTestDispatcher(testScheduler)
        ) { instante }
    }

    // Construir una coordenada al sur del origen
    private fun alSur(grados: Double, rumbo: Float? = null, velocidad: Float? = null): LecturaUbicacion {
        return LecturaUbicacion(Coordenada(LATITUD_BASE - grados, LONGITUD_BASE), rumbo, velocidad, precisionMetros = null)
    }

    // Verificar que iniciar publica el estado inicial con la distancia total restante
    @Test
    fun `iniciar publica el estado inicial del sendero`() = runTest {
        val control = crearControl()

        control.iniciar("con-recorrido")

        assertEquals("con-recorrido", control.estado.value?.idSendero)
        assertEquals(0.4448, control.estado.value?.distanciaRestanteKilometros ?: -1.0, 0.004)
    }

    // Verificar que un sendero con menos de dos puntos falla con una excepción clara
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

    // Verificar que un sendero inexistente falla con una excepción
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

    // Verificar que cada ubicación recibida actualiza el estado
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

    // Verificar que los avisos del cálculo se emiten por el flujo de avisos
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

    // Verificar que detener deja el estado en null e ignora las ubicaciones siguientes
    @Test
    fun `detener deja el estado en null e ignora nuevas ubicaciones`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")

        control.detener()
        ubicaciones.emit(alSur(0.001))

        assertNull(control.estado.value)
    }

    // Verificar que iniciar otro sendero reemplaza al anterior y deja una sola recolección activa
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

    // Verificar que un error del proveedor de ubicación finaliza el seguimiento
    @Test
    fun `un error del proveedor de ubicacion deja el estado en null`() = runTest {
        proveedor.ubicaciones = flow { throw SecurityException("Sin permiso") }
        val control = crearControl()

        control.iniciar("con-recorrido")

        assertNull(control.estado.value)
    }

    // Verificar que reemplazar un recorrido activo nunca publica un estado null intermedio
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

    // Verificar que el rumbo y la velocidad de la lectura llegan al estado publicado
    @Test
    fun `el rumbo y la velocidad de la lectura llegan al estado`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")

        ubicaciones.emit(alSur(0.0, rumbo = 180f, velocidad = 1.2f))

        assertEquals(180f, control.estado.value?.rumboGrados)
        assertEquals(1.2f, control.estado.value?.velocidadMetrosPorSegundo)
    }

    // Verificar que un recorrido simulado marca el estado y avanza solo, sin usar el GPS
    @Test
    fun `un recorrido simulado marca el estado y avanza con el tiempo virtual`() = runTest {
        val control = crearControl()

        control.iniciar("con-recorrido", esSimulado = true)
        advanceTimeBy(10_500L)

        assertEquals(true, control.estado.value?.esSimulado)
        assertTrue((control.estado.value?.distanciaRecorridaKilometros ?: 0.0) >= 0.01)
        assertEquals(0, ubicaciones.subscriptionCount.value)
    }

    // Verificar que un recorrido real no está marcado como simulado
    @Test
    fun `un recorrido real no esta marcado como simulado`() = runTest {
        val control = crearControl()

        control.iniciar("con-recorrido")

        assertEquals(false, control.estado.value?.esSimulado)
        assertEquals(1, ubicaciones.subscriptionCount.value)
    }

    // Verificar que el multiplicador de la simulación acelera el avance
    @Test
    fun `el multiplicador de la simulacion acelera el avance`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido", esSimulado = true)

        control.cambiarMultiplicadorSimulacion(20)
        advanceTimeBy(5_500L)

        assertTrue((control.estado.value?.distanciaRecorridaKilometros ?: 0.0) >= 0.1)
    }

    // Verificar que simular el desvío produce el aviso de desvío
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

    // Verificar que las acciones de simulación no hacen nada en un recorrido real
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

    // Verificar que el estado refleja el multiplicador vigente y que las ubicaciones no lo pisan
    @Test
    fun `el estado refleja el multiplicador vigente`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido", esSimulado = true)
        assertEquals(1, control.estado.value?.multiplicadorSimulacion)

        control.cambiarMultiplicadorSimulacion(20)
        advanceTimeBy(3_500L)

        assertEquals(20, control.estado.value?.multiplicadorSimulacion)
    }

    // Verificar que un multiplicador inválido se rechaza y no cambia el estado
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

    // Verificar que un permiso revocado se expone como motivo de fin
    @Test
    fun `un permiso revocado se expone como motivo de fin`() = runTest {
        proveedor.ubicaciones = flow { throw SecurityException("Sin permiso") }
        val control = crearControl()

        control.iniciar("con-recorrido")

        assertNull(control.estado.value)
        assertEquals(MotivoFinSeguimiento.PERMISO_DE_UBICACION_REVOCADO, control.motivoFin.value)
    }

    // Verificar que otro fallo se expone como error de ubicación y que iniciar de nuevo limpia el motivo
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

    // Verificar que detener por pedido del usuario no deja motivo de fin
    @Test
    fun `detener por pedido del usuario no deja motivo de fin`() = runTest {
        val control = crearControl()
        control.iniciar("con-recorrido")

        control.detener()

        assertNull(control.motivoFin.value)
    }
}
