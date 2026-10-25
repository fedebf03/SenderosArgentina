package com.unaj.senderos.core.data.seguimiento

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.LecturaUbicacion
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.testing.FalsoAlmacenBorradorRecorrido
import com.unaj.senderos.core.testing.FalsoAlmacenFotos
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoProveedorUbicacion
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LATITUD_BASE = -41.13
private const val LONGITUD_BASE = -71.30
private const val ID_CAMINATA = "libre-5000"

// Autoguardado del recorrido en curso: cada 30 s queda una copia local para no perderlo si Android cierra la app, y se borra al terminar
@OptIn(ExperimentalCoroutinesApi::class)
class ControlSeguimientoAutoguardadoTest {

    private val ubicaciones = MutableSharedFlow<LecturaUbicacion>(extraBufferCapacity = 16)
    private val proveedor = FalsoProveedorUbicacion(ubicaciones = ubicaciones)
    private val recorrido = listOf(PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE), PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE + 0.01))
    private val senderos = FalsoSenderoRepositorio(
        listOf(crearSenderoDePrueba(id = "s1", recorrido = recorrido).copy(tipoRecorrido = TipoRecorrido.LINEAL))
    )
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"))
    private val recorridos = FalsoRecorridoRepositorio()
    private val almacen = FalsoAlmacenBorradorRecorrido()
    private var instante = 5_000L

    private fun TestScope.crearControl(): ControlSeguimientoEnMemoria = ControlSeguimientoEnMemoria(
        proveedor,
        ObservarDetalleSenderoUseCase(senderos, autenticacion),
        recorridos,
        FalsoPuntosCapturadosRepositorio(),
        FalsoAlmacenFotos(),
        backgroundScope + UnconfinedTestDispatcher(testScheduler),
        almacenBorrador = almacen
    ) { instante }

    // Leer una posición a los metros indicados al este del inicio (un grado de longitud mide unos 83,9 km a esta latitud)
    private fun lectura(metrosAlEste: Double) = LecturaUbicacion(
        Coordenada(LATITUD_BASE, LONGITUD_BASE + metrosAlEste / 83_900.0), null, null, precisionMetros = 5f
    )

    @Test
    fun `cada 30 segundos guarda una copia del recorrido con lo caminado hasta ahora`() = runTest {
        val control = crearControl()
        control.iniciarCaminataLibre(ID_CAMINATA, "usuario-1", "Caminata del domingo")
        ubicaciones.emit(lectura(0.0))
        ubicaciones.emit(lectura(40.0))

        instante = 35_000L
        advanceTimeBy(30_001)

        val guardado = almacen.guardados.single()
        assertEquals(ID_CAMINATA, guardado.idSendero)
        assertEquals("usuario-1", guardado.idUsuario)
        assertEquals("Caminata del domingo", guardado.nombreSendero)
        assertEquals(2, guardado.trazadoCaminado.size)
        assertEquals(35_000L, guardado.finMilisegundos)
    }

    @Test
    fun `cada nuevo autoguardado reemplaza al anterior`() = runTest {
        val control = crearControl()
        control.iniciarCaminataLibre(ID_CAMINATA, "usuario-1", "Caminata")
        ubicaciones.emit(lectura(0.0))
        ubicaciones.emit(lectura(40.0))

        advanceTimeBy(30_001)
        ubicaciones.emit(lectura(80.0))
        advanceTimeBy(30_000)

        assertEquals(2, almacen.guardados.size)
        assertEquals(3, almacen.borrador?.trazadoCaminado?.size)
    }

    @Test
    fun `sin ninguna posicion aceptada todavia no guarda nada`() = runTest {
        val control = crearControl()
        control.iniciarCaminataLibre(ID_CAMINATA, "usuario-1", "Caminata")

        advanceTimeBy(65_000)

        assertTrue(almacen.guardados.isEmpty())
    }

    @Test
    fun `un recorrido de sendero tambien se autoguarda`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0))
        ubicaciones.emit(lectura(40.0))

        advanceTimeBy(30_001)

        assertEquals("s1", almacen.guardados.single().idSendero)
    }

    @Test
    fun `una simulacion no se autoguarda`() = runTest {
        val control = crearControl()
        control.iniciar("s1", esSimulado = true)

        advanceTimeBy(65_000)

        assertTrue(almacen.guardados.isEmpty())
    }

    @Test
    fun `al finalizar el recorrido se guarda en el historial y se borra el borrador`() = runTest {
        val control = crearControl()
        control.iniciarCaminataLibre(ID_CAMINATA, "usuario-1", "Caminata")
        ubicaciones.emit(lectura(0.0))
        ubicaciones.emit(lectura(40.0))
        advanceTimeBy(30_001)

        control.detener()

        assertEquals(1, recorridos.guardados.size)
        assertNull(almacen.borrador)
        assertTrue(almacen.cantidadBorrados >= 1)
    }

    @Test
    fun `al cancelar el recorrido se borra el borrador y no queda nada en el historial`() = runTest {
        val control = crearControl()
        control.iniciarCaminataLibre(ID_CAMINATA, "usuario-1", "Caminata")
        ubicaciones.emit(lectura(0.0))
        ubicaciones.emit(lectura(40.0))
        advanceTimeBy(30_001)

        control.cancelar()

        assertTrue(recorridos.guardados.isEmpty())
        assertNull(almacen.borrador)
    }

    @Test
    fun `despues de terminar ya no sigue autoguardando`() = runTest {
        val control = crearControl()
        control.iniciarCaminataLibre(ID_CAMINATA, "usuario-1", "Caminata")
        ubicaciones.emit(lectura(0.0))
        ubicaciones.emit(lectura(40.0))
        control.detener()

        advanceTimeBy(95_000)

        assertTrue(almacen.guardados.isEmpty())
    }

    @Test
    fun `un fallo al guardar el borrador no corta el seguimiento`() = runTest {
        almacen.resultadoGuardar = Result.failure(IllegalStateException("Sin espacio"))
        val control = crearControl()
        control.iniciarCaminataLibre(ID_CAMINATA, "usuario-1", "Caminata")
        ubicaciones.emit(lectura(0.0))
        ubicaciones.emit(lectura(40.0))

        advanceTimeBy(30_001)
        ubicaciones.emit(lectura(80.0))

        assertEquals(3, control.estado.value?.trazadoCaminado?.size)
    }
}
