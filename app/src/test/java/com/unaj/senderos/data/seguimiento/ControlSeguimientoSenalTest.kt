package com.unaj.senderos.data.seguimiento

import com.unaj.senderos.domain.model.CalidadSenal
import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.model.PuntoRecorrido
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07

@OptIn(ExperimentalCoroutinesApi::class)
class ControlSeguimientoSenalTest {

    private val ubicaciones = MutableSharedFlow<LecturaUbicacion>(extraBufferCapacity = 16)
    private val proveedor = FalsoProveedorUbicacion(ubicaciones = ubicaciones)
    private val senderos = FalsoSenderoRepositorio(
        listOf(
            crearSenderoDePrueba(
                id = "s1",
                recorrido = listOf(PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE), PuntoRecorrido(LATITUD_BASE - 0.004, LONGITUD_BASE))
            ).copy(tipoRecorrido = TipoRecorrido.LINEAL)
        )
    )
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"))
    private var instante = 5_000L

    private fun TestScope.crearControl(capacidades: CapacidadesDispositivo): ControlSeguimientoEnMemoria {
        return ControlSeguimientoEnMemoria(
            proveedor,
            ObservarDetalleSenderoUseCase(senderos, autenticacion),
            FalsoRecorridoRepositorio(),
            FalsoPuntosCapturadosRepositorio(),
            FalsoAlmacenFotos(),
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
            capacidades = capacidades
        ) { instante }
    }

    private fun lectura(gradosAlSur: Double, precision: Float?, velocidad: Float? = null): LecturaUbicacion {
        return LecturaUbicacion(Coordenada(LATITUD_BASE - gradosAlSur, LONGITUD_BASE), null, velocidad, precision)
    }

    private val conGps = CapacidadesDispositivo(tieneGps = true, tieneAcelerometro = false, tieneBrujula = false, tieneCamara = false)
    private val sinGps = CapacidadesDispositivo(tieneGps = false, tieneAcelerometro = false, tieneBrujula = false, tieneCamara = false)

    @Test
    fun `con GPS una lectura de cincuenta metros se descarta`() = runTest {
        val control = crearControl(conGps)
        control.iniciar("s1")

        ubicaciones.emit(lectura(0.001, precision = 50f))

        assertNull(control.estado.value?.posicionActual)
    }

    @Test
    fun `sin GPS una lectura de cincuenta metros se acepta`() = runTest {
        val control = crearControl(sinGps)
        control.iniciar("s1")

        ubicaciones.emit(lectura(0.001, precision = 50f))

        assertNotNull(control.estado.value?.posicionActual)
    }

    @Test
    fun `sin GPS una lectura de ciento cincuenta metros se descarta`() = runTest {
        val control = crearControl(sinGps)
        control.iniciar("s1")

        ubicaciones.emit(lectura(0.001, precision = 150f))

        assertNull(control.estado.value?.posicionActual)
    }

    @Test
    fun `con senal debil y sin acelerometro la velocidad es desconocida y esta detenido`() = runTest {
        val control = crearControl(conGps)
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0005, precision = 5f, velocidad = 1.5f))
        assertEquals(false, control.estado.value?.estaDetenido)

        instante = 30_000L
        advanceTimeBy(5_001L)

        val estado = control.estado.value
        assertEquals(CalidadSenal.DEBIL, estado?.calidadSenal)
        assertNull(estado?.velocidadMetrosPorSegundo)
        assertTrue(estado?.estaDetenido == true)
    }
}
