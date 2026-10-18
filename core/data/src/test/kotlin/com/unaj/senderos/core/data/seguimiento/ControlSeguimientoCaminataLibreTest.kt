package com.unaj.senderos.core.data.seguimiento

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
import com.unaj.senderos.core.domain.model.LecturaUbicacion
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.testing.FalsoAlmacenFotos
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoProveedorUbicacion
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LATITUD_BASE = -41.13
private const val LONGITUD_BASE = -71.30

// Caminata libre en el control del seguimiento: arranca sin sendero, registra lo caminado y guarda el recorrido y los puntos con su propio identificador
@OptIn(ExperimentalCoroutinesApi::class)
class ControlSeguimientoCaminataLibreTest {

    private val ubicaciones = MutableSharedFlow<LecturaUbicacion>(extraBufferCapacity = 16)
    private val proveedor = FalsoProveedorUbicacion(ubicaciones = ubicaciones)
    private val senderos = FalsoSenderoRepositorio(emptyList())
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"))
    private val recorridos = FalsoRecorridoRepositorio()
    private val puntosCapturados = FalsoPuntosCapturadosRepositorio()
    private var instante = 5_000L
    private var contador = 0

    private fun TestScope.crearControl(): ControlSeguimientoEnMemoria = ControlSeguimientoEnMemoria(
        proveedor,
        ObservarDetalleSenderoUseCase(senderos, autenticacion),
        recorridos,
        puntosCapturados,
        FalsoAlmacenFotos(),
        backgroundScope + UnconfinedTestDispatcher(testScheduler),
        generarIdentificador = { "punto-${++contador}" }
    ) { instante }

    // Leer una posición a los metros indicados al este del inicio (un grado de longitud mide unos 83,9 km a esta latitud)
    private fun lectura(metrosAlEste: Double) = LecturaUbicacion(
        Coordenada(LATITUD_BASE, LONGITUD_BASE + metrosAlEste / 83_900.0), null, null, precisionMetros = 5f
    )

    @Test
    fun `iniciar una caminata libre no necesita un sendero y publica un estado de caminata libre`() = runTest {
        val control = crearControl()

        control.iniciarCaminataLibre("usuario-1", "Caminata del domingo")

        val estado = control.estado.value
        assertNotNull(estado)
        assertTrue(estado?.esCaminataLibre == true)
        assertEquals("Caminata del domingo", estado?.nombreSendero)
        assertTrue(IdentificadorCaminataLibre.esDeCaminataLibre(estado?.idSendero.orEmpty()))
        assertEquals(5_000L, estado?.instanteInicioMilisegundos)
    }

    @Test
    fun `las lecturas suman distancia y arman el trazado caminado`() = runTest {
        val control = crearControl()
        control.iniciarCaminataLibre("usuario-1", "Caminata")

        ubicaciones.emit(lectura(0.0))
        ubicaciones.emit(lectura(30.0))
        ubicaciones.emit(lectura(60.0))

        val estado = control.estado.value
        assertEquals(0.060, estado?.distanciaRecorridaKilometros ?: 0.0, 0.003)
        assertEquals(3, estado?.trazadoCaminado?.size)
    }

    @Test
    fun `un punto capturado queda guardado con el identificador de la caminata y el usuario que la inicio`() = runTest {
        val control = crearControl()
        control.iniciarCaminataLibre("usuario-1", "Caminata")
        ubicaciones.emit(lectura(0.0))
        ubicaciones.emit(lectura(30.0))

        val resultado = control.capturarPunto(TipoPunto.MIRADOR, "Vista", null)

        assertTrue(resultado.isSuccess)
        val guardado = puntosCapturados.llamadasGuardar.single()
        assertEquals(control.estado.value?.idSendero, guardado.idSendero)
        assertEquals("usuario-1", guardado.idUsuario)
    }

    @Test
    fun `detener guarda el recorrido realizado con el identificador de la caminata y su trazado`() = runTest {
        val control = crearControl()
        control.iniciarCaminataLibre("usuario-1", "Caminata del domingo")
        val idCaminata = control.estado.value?.idSendero.orEmpty()
        ubicaciones.emit(lectura(0.0))
        ubicaciones.emit(lectura(40.0))

        instante = 65_000L
        control.detener()

        val guardado = recorridos.guardados.single()
        assertEquals(idCaminata, guardado.idSendero)
        assertEquals("Caminata del domingo", guardado.nombreSendero)
        assertEquals("usuario-1", guardado.idUsuario)
        assertEquals(2, guardado.trazadoCaminado.size)
        assertNull(control.estado.value)
    }

    @Test
    fun `cancelar descarta la caminata sin guardar nada`() = runTest {
        val control = crearControl()
        control.iniciarCaminataLibre("usuario-1", "Caminata")
        ubicaciones.emit(lectura(0.0))
        ubicaciones.emit(lectura(40.0))

        control.cancelar()

        assertTrue(recorridos.guardados.isEmpty())
        assertNull(control.estado.value)
    }
}
