package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoPerfil
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.domain.port.SinConexionElevacionException
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoFuenteElevacion
import com.unaj.senderos.core.testing.FalsoSenderoRepositorio
import com.unaj.senderos.core.testing.crearSenderoDePrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class CalcularPerfilElevacionUseCaseTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com")
    private val autenticacion = FalsoAutenticacionRepositorio(usuarioActual = usuario)
    private val fuente = FalsoFuenteElevacion()
    private val trazado = listOf(PuntoRecorrido(-40.0, -70.0), PuntoRecorrido(-40.01, -70.0))

    private fun crearCasoDeUso(repositorio: FalsoSenderoRepositorio) =
        CalcularPerfilElevacionUseCase(repositorio, fuente, autenticacion)

    private fun senderoConTrazado(
        desnivelMetros: Int = 0,
        alturaMaximaMetros: Int = 0,
        alturaMinimaMetros: Int = 0,
        perfilElevacion: PerfilElevacion? = null,
        recorrido: List<PuntoRecorrido> = trazado
    ) = crearSenderoDePrueba(
        recorrido = recorrido,
        tipoRecorrido = TipoRecorrido.LINEAL,
        desnivelMetros = desnivelMetros,
        alturaMaximaMetros = alturaMaximaMetros,
        alturaMinimaMetros = alturaMinimaMetros,
        perfilElevacion = perfilElevacion
    )

    private fun perfilConFirma(firma: String) = PerfilElevacion(
        puntos = listOf(PuntoPerfil(0.0, 1.0), PuntoPerfil(1.0, 2.0)),
        desnivelPositivoMetros = 1,
        desnivelNegativoMetros = 0,
        alturaMaximaMetros = 2,
        alturaMinimaMetros = 1,
        pendienteMaximaPorcentaje = 0.1,
        firmaTrazado = firma
    )

    @Test
    fun `un sendero sin perfil pide las alturas del muestreo y guarda el perfil`() = runTest {
        val repositorio = FalsoSenderoRepositorio(listOf(senderoConTrazado()))

        val resultado = crearCasoDeUso(repositorio)("sendero-1")

        assertTrue(resultado.isSuccess)
        val muestras = CalculadoraPerfil.muestrearTrazado(trazado, TipoRecorrido.LINEAL)
        assertEquals(1, fuente.llamadas.size)
        assertEquals(muestras.size, fuente.llamadas.single().size)
        val guardado = repositorio.senderos.value.single()
        val perfil = requireNotNull(guardado.perfilElevacion)
        assertEquals(muestras.size, perfil.puntos.size)
        assertEquals(CalculadoraPerfil.firmaTrazado(trazado, TipoRecorrido.LINEAL), perfil.firmaTrazado)
        assertEquals(100.0, perfil.desnivelPositivoMetros.toDouble(), 1.0)
    }

    @Test
    fun `un perfil con la firma vigente no vuelve a pedir alturas`() = runTest {
        val firma = CalculadoraPerfil.firmaTrazado(trazado, TipoRecorrido.LINEAL)
        val repositorio = FalsoSenderoRepositorio(listOf(senderoConTrazado(perfilElevacion = perfilConFirma(firma))))

        val resultado = crearCasoDeUso(repositorio)("sendero-1")

        assertTrue(resultado.isSuccess)
        assertTrue(fuente.llamadas.isEmpty())
        assertTrue(repositorio.llamadasGuardarPerfil.isEmpty())
    }

    @Test
    fun `un perfil con otra firma se recalcula`() = runTest {
        val repositorio = FalsoSenderoRepositorio(listOf(senderoConTrazado(perfilElevacion = perfilConFirma("vieja"))))

        val resultado = crearCasoDeUso(repositorio)("sendero-1")

        assertTrue(resultado.isSuccess)
        assertEquals(1, fuente.llamadas.size)
        val firmaNueva = CalculadoraPerfil.firmaTrazado(trazado, TipoRecorrido.LINEAL)
        assertEquals(firmaNueva, repositorio.senderos.value.single().perfilElevacion?.firmaTrazado)
    }

    @Test
    fun `sin red falla con el mensaje y no guarda nada`() = runTest {
        fuente.falloDevuelto = SinConexionElevacionException(null)
        val repositorio = FalsoSenderoRepositorio(listOf(senderoConTrazado()))

        val resultado = crearCasoDeUso(repositorio)("sendero-1")

        assertEquals("Sin conexión para calcular el perfil", resultado.exceptionOrNull()?.message)
        assertTrue(repositorio.llamadasGuardarPerfil.isEmpty())
        assertNull(repositorio.senderos.value.single().perfilElevacion)
    }

    @Test
    fun `completa desnivel y alturas que estan en cero`() = runTest {
        val repositorio = FalsoSenderoRepositorio(listOf(senderoConTrazado()))

        crearCasoDeUso(repositorio)("sendero-1")

        val sendero = repositorio.senderos.value.single()
        assertEquals(100.0, sendero.desnivelMetros.toDouble(), 1.0)
        assertEquals(1100, sendero.alturaMaximaMetros)
        assertEquals(1000, sendero.alturaMinimaMetros)
    }

    @Test
    fun `no pisa los valores que el usuario ya cargo`() = runTest {
        val repositorio = FalsoSenderoRepositorio(
            listOf(senderoConTrazado(desnivelMetros = 650, alturaMaximaMetros = 2100, alturaMinimaMetros = 0))
        )

        crearCasoDeUso(repositorio)("sendero-1")

        val sendero = repositorio.senderos.value.single()
        assertEquals(650, sendero.desnivelMetros)
        assertEquals(2100, sendero.alturaMaximaMetros)
        assertEquals(1000, sendero.alturaMinimaMetros)
    }

    @Test
    fun `sin sesion activa falla sin pedir alturas`() = runTest {
        autenticacion.usuarioActual = null
        val repositorio = FalsoSenderoRepositorio(listOf(senderoConTrazado()))

        val resultado = crearCasoDeUso(repositorio)("sendero-1")

        assertTrue(resultado.isFailure)
        assertTrue(fuente.llamadas.isEmpty())
    }

    @Test
    fun `un sendero inexistente falla`() = runTest {
        val resultado = crearCasoDeUso(FalsoSenderoRepositorio())("no-existe")

        assertEquals("No se encontró el sendero", resultado.exceptionOrNull()?.message)
        assertTrue(fuente.llamadas.isEmpty())
    }

    @Test
    fun `un sendero con menos de dos puntos falla sin pedir alturas`() = runTest {
        val repositorio = FalsoSenderoRepositorio(listOf(senderoConTrazado(recorrido = listOf(PuntoRecorrido(-40.0, -70.0)))))

        val resultado = crearCasoDeUso(repositorio)("sendero-1")

        assertEquals("El sendero necesita al menos dos puntos de recorrido para calcular el perfil", resultado.exceptionOrNull()?.message)
        assertTrue(fuente.llamadas.isEmpty())
    }

    @Test
    fun `si fallo el guardado local devuelve el fallo`() = runTest {
        val repositorio = FalsoSenderoRepositorio(listOf(senderoConTrazado()))
        repositorio.resultadoGuardarPerfil = Result.failure(IllegalStateException("disco lleno"))

        val resultado = crearCasoDeUso(repositorio)("sendero-1")

        assertEquals("disco lleno", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `la cancelacion se relanza`() = runTest {
        fuente.excepcionLanzada = CancellationException("cancelado")
        val repositorio = FalsoSenderoRepositorio(listOf(senderoConTrazado()))

        try {
            crearCasoDeUso(repositorio)("sendero-1")
            fail("Debía relanzar la cancelación")
        } catch (excepcion: CancellationException) {
            assertEquals("cancelado", excepcion.message)
        }
    }
}
