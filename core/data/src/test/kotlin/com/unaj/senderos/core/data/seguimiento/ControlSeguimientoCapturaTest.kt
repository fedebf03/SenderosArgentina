package com.unaj.senderos.data.seguimiento

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.falsos.FalsoAlmacenFotos
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoProveedorUbicacion
import com.unaj.senderos.falsos.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.falsos.FalsoRecorridoRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearSenderoDePrueba
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

private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07

@OptIn(ExperimentalCoroutinesApi::class)
class ControlSeguimientoCapturaTest {

    private val ubicaciones = MutableSharedFlow<LecturaUbicacion>(extraBufferCapacity = 16)
    private val proveedor = FalsoProveedorUbicacion(ubicaciones = ubicaciones)
    private val recorrido = listOf(
        PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE),
        PuntoRecorrido(LATITUD_BASE - 0.004, LONGITUD_BASE)
    )
    private val senderos = FalsoSenderoRepositorio(
        listOf(crearSenderoDePrueba(id = "s1", recorrido = recorrido).copy(tipoRecorrido = TipoRecorrido.LINEAL))
    )
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"))
    private val puntosCapturados = FalsoPuntosCapturadosRepositorio()
    private val almacenFotos = FalsoAlmacenFotos()
    private var instante = 5_000L
    private var contador = 0

    private fun TestScope.crearControl(): ControlSeguimientoEnMemoria {
        return ControlSeguimientoEnMemoria(
            proveedor,
            ObservarDetalleSenderoUseCase(senderos, autenticacion),
            FalsoRecorridoRepositorio(),
            puntosCapturados,
            almacenFotos,
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
            generarIdentificador = { "punto-${++contador}" }
        ) { instante }
    }

    private fun lectura(gradosAlSur: Double, precision: Float? = null): LecturaUbicacion {
        return LecturaUbicacion(Coordenada(LATITUD_BASE - gradosAlSur, LONGITUD_BASE), null, null, precisionMetros = precision)
    }

    @Test
    fun `capturar sin recorrido activo falla`() = runTest {
        val control = crearControl()

        val resultado = control.capturarPunto(TipoPunto.MIRADOR, "nota", null)

        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()?.message.orEmpty().contains("recorrido"))
        assertTrue(puntosCapturados.llamadasGuardar.isEmpty())
    }

    @Test
    fun `capturar sin posicion valida falla`() = runTest {
        val control = crearControl()
        control.iniciar("s1")

        val resultado = control.capturarPunto(TipoPunto.MIRADOR, "nota", null)

        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()?.message.orEmpty().contains("posición"))
        assertTrue(puntosCapturados.llamadasGuardar.isEmpty())
    }

    @Test
    fun `capturar guarda el punto con la ultima posicion`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.002))
        instante = 9_000L

        val resultado = control.capturarPunto(TipoPunto.AGUA, "Arroyo", null)

        val guardado = puntosCapturados.llamadasGuardar.single()
        assertEquals(guardado, resultado.getOrThrow().punto)
        assertNull(resultado.getOrThrow().antiguedadPosicionMilisegundos)
        assertEquals("punto-1", guardado.id)
        assertEquals("usuario-1", guardado.idUsuario)
        assertEquals("s1", guardado.idSendero)
        assertEquals(LATITUD_BASE - 0.002, guardado.latitud, 0.0)
        assertEquals(LONGITUD_BASE, guardado.longitud, 0.0)
        assertEquals(TipoPunto.AGUA, guardado.tipo)
        assertEquals("Arroyo", guardado.nota)
        assertEquals("", guardado.foto)
        assertEquals(9_000L, guardado.instanteMilisegundos)
    }

    @Test
    fun `capturar usa la ultima posicion valida y no la imprecisa`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.001))
        ubicaciones.emit(lectura(0.003, precision = 150f))

        control.capturarPunto(TipoPunto.PELIGRO, "", null)

        assertEquals(LATITUD_BASE - 0.001, puntosCapturados.llamadasGuardar.single().latitud, 0.0)
    }

    @Test
    fun `capturar con foto la copia y guarda la ruta local`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.002))

        val resultado = control.capturarPunto(TipoPunto.MIRADOR, "Vista", "content://camara/foto")

        assertEquals(listOf("content://camara/foto"), almacenFotos.urisCopiadas)
        assertEquals("/fotos/copia-1.jpg", resultado.getOrThrow().punto.foto)
        assertEquals("/fotos/copia-1.jpg", puntosCapturados.llamadasGuardar.single().foto)
    }

    @Test
    fun `capturar con foto que no se puede copiar falla sin guardar`() = runTest {
        almacenFotos.error = IllegalStateException("sin espacio")
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.002))

        val resultado = control.capturarPunto(TipoPunto.MIRADOR, "Vista", "content://camara/foto")

        assertTrue(resultado.isFailure)
        assertTrue(puntosCapturados.llamadasGuardar.isEmpty())
    }

    @Test
    fun `capturar devuelve failure si el repositorio no guarda`() = runTest {
        puntosCapturados.resultadoGuardar = Result.failure(IllegalStateException("disco lleno"))
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.002))

        val resultado = control.capturarPunto(TipoPunto.MIRADOR, "", null)

        assertTrue(resultado.isFailure)
    }

    // Requiere que el avance simulado ya haya producido una posición
    @Test
    fun `capturar funciona en simulacion`() = runTest {
        val control = crearControl()
        control.iniciar("s1", esSimulado = true)
        advanceTimeBy(3_500L)

        val resultado = control.capturarPunto(TipoPunto.REFUGIO, "Simulado", null)

        assertTrue(resultado.isSuccess)
        assertEquals("s1", puntosCapturados.llamadasGuardar.single().idSendero)
    }

    // Persiste porque el punto ya está en el repositorio
    @Test
    fun `el punto capturado persiste aunque se detenga el recorrido`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.002))
        control.capturarPunto(TipoPunto.MIRADOR, "Vista", null)

        control.detener()

        assertEquals(1, puntosCapturados.puntos.value.size)
    }

    @Test
    fun `capturar borra la copia de la foto si no se pudo guardar el punto`() = runTest {
        puntosCapturados.resultadoGuardar = Result.failure(IllegalStateException("disco lleno"))
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.002))

        control.capturarPunto(TipoPunto.MIRADOR, "", "content://camara/foto")

        assertEquals(listOf("/fotos/copia-1.jpg"), almacenFotos.rutasBorradas)
    }

    @Test
    fun `capturar sin foto no borra archivos al fallar`() = runTest {
        puntosCapturados.resultadoGuardar = Result.failure(IllegalStateException("disco lleno"))
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.002))

        control.capturarPunto(TipoPunto.MIRADOR, "", null)

        assertTrue(almacenFotos.rutasBorradas.isEmpty())
    }

    @Test
    fun `capturar con senal debil informa la antiguedad de la posicion`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.002))
        instante = 30_000L
        advanceTimeBy(5_001L)

        val resultado = control.capturarPunto(TipoPunto.MIRADOR, "nota", null).getOrThrow()

        assertEquals(25_000L, resultado.antiguedadPosicionMilisegundos)
        assertEquals(1, puntosCapturados.llamadasGuardar.size)
    }

    @Test
    fun `los fallos llevan mensajes propios en español`() = runTest {
        almacenFotos.error = IllegalStateException("java.io.IOException: ENOSPC")
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.002))

        val mensaje = control.capturarPunto(TipoPunto.MIRADOR, "", "content://camara/foto").exceptionOrNull()?.message.orEmpty()

        assertEquals("No se pudo guardar la foto del punto", mensaje)
    }
}
