package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.testing.FalsoAutenticacionRepositorio
import com.unaj.senderos.core.testing.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.core.testing.FalsoRecorridoRepositorio
import com.unaj.senderos.core.testing.crearPuntoCapturadoDePrueba
import com.unaj.senderos.core.testing.crearRecorridoDePrueba
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ID_CAMINATA = "libre-1000"

// Pasar una caminata libre ya guardada al formulario de un sendero nuevo, y limpiar los puntos cuando el sendero se guardó
@OptIn(ExperimentalCoroutinesApi::class)
class CaminataLibreUseCasesTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana")
    private val autenticacion = FalsoAutenticacionRepositorio(usuario)
    private val recorridos = FalsoRecorridoRepositorio()
    private val puntos = FalsoPuntosCapturadosRepositorio()

    private val convertir = ConvertirCaminataEnBorradorUseCase(recorridos, puntos, autenticacion)
    private val retirar = RetirarPuntosIncorporadosUseCase(puntos, autenticacion)

    // Guardar una caminata recta hacia el este de unos 700 m, con el recorrido realizado que dejaría el control al finalizar
    private fun guardarCaminata() {
        val trazado = List(40) { indice -> Coordenada(-41.13, -71.30 + indice * 0.0002) }
        recorridos.recorridos.value = listOf(
            crearRecorridoDePrueba(id = "recorrido-1000", idSendero = ID_CAMINATA, nombreSendero = "Caminata libre", inicioMilisegundos = 1_000L)
                .copy(trazadoCaminado = trazado)
        )
    }

    @Test
    fun `convertir arma el borrador con el trazado caminado y devuelve los ids de los puntos`() = runTest {
        guardarCaminata()
        puntos.puntos.value = listOf(
            crearPuntoCapturadoDePrueba(id = "p1", idSendero = ID_CAMINATA, latitud = -41.13, longitud = -71.2955, tipo = TipoPunto.AGUA, nota = "Arroyo"),
            crearPuntoCapturadoDePrueba(id = "p2", idSendero = "otro-sendero", latitud = -41.13, longitud = -71.2955)
        )

        val borrador = convertir(ID_CAMINATA).getOrThrow()

        assertEquals(TipoRecorrido.LINEAL, borrador.trazado.tipoRecorrido)
        assertEquals(setOf("p1"), borrador.idsPuntosCapturados)
        val interes = borrador.trazado.recorrido.filter { punto -> punto.tipo != TipoPunto.PASO }
        assertEquals(listOf("Arroyo"), interes.map { punto -> punto.nombre })
    }

    @Test
    fun `el nombre sugerido lleva la fecha de la caminata`() = runTest {
        guardarCaminata()

        val borrador = convertir(ID_CAMINATA).getOrThrow()

        assertTrue(borrador.trazado.nombre.startsWith("Caminata del "))
    }

    @Test
    fun `convertir sin sesion falla`() = runTest {
        guardarCaminata()
        autenticacion.usuarioActual = null

        assertTrue(convertir(ID_CAMINATA).isFailure)
    }

    @Test
    fun `convertir una caminata que no existe falla con un mensaje claro`() = runTest {
        val resultado = convertir("libre-999")

        assertTrue(resultado.isFailure)
        assertEquals("No se encontró la caminata", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun `convertir una caminata sin recorrido caminado falla`() = runTest {
        recorridos.recorridos.value = listOf(
            crearRecorridoDePrueba(idSendero = ID_CAMINATA).copy(trazadoCaminado = listOf(Coordenada(-41.13, -71.30)))
        )

        assertTrue(convertir(ID_CAMINATA).isFailure)
    }

    @Test
    fun `retirar los puntos incorporados los saca de los pendientes del usuario`() = runTest {
        puntos.puntos.value = listOf(crearPuntoCapturadoDePrueba(id = "p1", idSendero = ID_CAMINATA))

        val resultado = retirar(setOf("p1"))

        assertTrue(resultado.isSuccess)
        assertEquals(listOf("usuario-1"), puntos.llamadasRetirar.map { llamada -> llamada.first })
        assertTrue(puntos.puntos.value.isEmpty())
    }

    @Test
    fun `retirar sin sesion falla y no toca nada`() = runTest {
        autenticacion.usuarioActual = null

        assertTrue(retirar(setOf("p1")).isFailure)
        assertTrue(puntos.llamadasRetirar.isEmpty())
    }
}
