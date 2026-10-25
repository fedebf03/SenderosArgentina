package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.port.SinConexionElevacionException
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.testing.FalsoFuenteElevacion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Calcular el perfil de un trazado que todavía no se guardó, por ejemplo mientras se crea el sendero
class CalcularPerfilDeTrazadoUseCaseTest {

    private val fuente = FalsoFuenteElevacion()
    private val casoDeUso = CalcularPerfilDeTrazadoUseCase(fuente)
    private val trazado = listOf(PuntoRecorrido(-40.0, -70.0), PuntoRecorrido(-40.01, -70.0))

    @Test
    fun `calcula el perfil del trazado con su firma`() = runTest {
        val perfil = casoDeUso(trazado, TipoRecorrido.LINEAL).getOrThrow()

        assertEquals(CalculadoraPerfil.firmaTrazado(trazado, TipoRecorrido.LINEAL), perfil.firmaTrazado)
        assertEquals(1100, perfil.alturaMaximaMetros)
        assertEquals(1000, perfil.alturaMinimaMetros)
        assertEquals(1, fuente.llamadas.size)
    }

    @Test
    fun `con menos de dos puntos falla sin consultar la red`() = runTest {
        val resultado = casoDeUso(trazado.take(1), TipoRecorrido.LINEAL)

        assertTrue(resultado.isFailure)
        assertTrue(fuente.llamadas.isEmpty())
    }

    @Test
    fun `sin conexion devuelve el fallo de la fuente`() = runTest {
        fuente.falloDevuelto = SinConexionElevacionException(null)

        val resultado = casoDeUso(trazado, TipoRecorrido.LINEAL)

        assertTrue(resultado.exceptionOrNull() is SinConexionElevacionException)
    }
}
