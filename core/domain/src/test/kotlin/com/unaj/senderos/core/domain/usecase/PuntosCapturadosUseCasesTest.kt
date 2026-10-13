package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.DISTANCIA_MAXIMA_RECOMENDADA_METROS
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoPuntosCapturadosRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearPuntoCapturadoDePrueba
import com.unaj.senderos.falsos.crearSenderoDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07

class PuntosCapturadosUseCasesTest {

    private val usuario = Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana")
    private val autenticacion = FalsoAutenticacionRepositorio(usuario)
    private val recorrido = listOf(
        PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE, nombre = "A"),
        PuntoRecorrido(LATITUD_BASE - 0.004, LONGITUD_BASE, nombre = "B"),
        PuntoRecorrido(LATITUD_BASE - 0.008, LONGITUD_BASE, nombre = "C")
    )
    private val senderos = FalsoSenderoRepositorio(
        listOf(crearSenderoDePrueba(id = "sendero-1", recorrido = recorrido).copy(tipoRecorrido = TipoRecorrido.LINEAL))
    )

    @Test
    fun `incorporar delega en el repositorio con el usuario de la sesion`() = runTest {
        val puntos = FalsoPuntosCapturadosRepositorio()
        val sendero = crearSenderoDePrueba(id = "sendero-1")
        puntos.resultadoIncorporar = Result.success(sendero)

        val resultado = IncorporarPuntosCapturadosUseCase(puntos, autenticacion)("sendero-1", setOf("p1"))

        assertEquals(sendero, resultado.getOrThrow())
        assertEquals(Triple("usuario-1", "sendero-1", setOf("p1")), puntos.llamadasIncorporar.single())
    }

    @Test
    fun `incorporar sin sesion activa falla`() = runTest {
        autenticacion.usuarioActual = null
        val puntos = FalsoPuntosCapturadosRepositorio()

        val resultado = IncorporarPuntosCapturadosUseCase(puntos, autenticacion)("sendero-1", setOf("p1"))

        assertTrue(resultado.isFailure)
        assertTrue(puntos.llamadasIncorporar.isEmpty())
    }

    @Test
    fun `descartar borra los puntos indicados`() = runTest {
        val puntos = FalsoPuntosCapturadosRepositorio(
            listOf(crearPuntoCapturadoDePrueba(id = "p1"), crearPuntoCapturadoDePrueba(id = "p2"))
        )

        val resultado = DescartarPuntosCapturadosUseCase(puntos, autenticacion)(setOf("p1"))

        assertTrue(resultado.isSuccess)
        assertEquals(listOf("p2"), puntos.puntos.value.map { punto -> punto.id })
        assertEquals("usuario-1", puntos.llamadasDescartar.single().first)
    }

    @Test
    fun `descartar sin sesion activa falla`() = runTest {
        autenticacion.usuarioActual = null

        val resultado = DescartarPuntosCapturadosUseCase(FalsoPuntosCapturadosRepositorio(), autenticacion)(setOf("p1"))

        assertTrue(resultado.isFailure)
    }

    @Test
    fun `observar devuelve los pendientes del sendero`() = runTest {
        val puntos = FalsoPuntosCapturadosRepositorio(
            listOf(crearPuntoCapturadoDePrueba(id = "p1"), crearPuntoCapturadoDePrueba(id = "p2", idSendero = "otro"))
        )

        val flujo = ObservarPuntosCapturadosUseCase(puntos, autenticacion)("sendero-1").getOrThrow()

        assertEquals(listOf("p1"), flujo.first().map { punto -> punto.id })
    }

    @Test
    fun `observar sin sesion activa falla`() {
        autenticacion.usuarioActual = null

        val resultado = ObservarPuntosCapturadosUseCase(FalsoPuntosCapturadosRepositorio(), autenticacion)("sendero-1")

        assertTrue(resultado.isFailure)
    }

    @Test
    fun `observar con distancia marca los puntos lejanos`() = runTest {
        val puntos = FalsoPuntosCapturadosRepositorio(
            listOf(
                crearPuntoCapturadoDePrueba(id = "cerca", latitud = LATITUD_BASE - 0.006, longitud = LONGITUD_BASE + 0.0001),
                crearPuntoCapturadoDePrueba(id = "lejos", latitud = LATITUD_BASE - 0.006, longitud = LONGITUD_BASE + 0.01, instanteMilisegundos = 2_000L)
            )
        )

        val lista = ObservarPuntosCapturadosConDistanciaUseCase(senderos, puntos, autenticacion)("sendero-1").getOrThrow().first()

        assertEquals(listOf("cerca", "lejos"), lista.map { elemento -> elemento.punto.id })
        assertFalse(lista[0].superaDistanciaRecomendada)
        assertTrue(lista[1].superaDistanciaRecomendada)
        assertTrue((lista[1].distanciaAlTrazadoMetros ?: 0.0) > DISTANCIA_MAXIMA_RECOMENDADA_METROS)
    }

    @Test
    fun `observar con distancia sin recorrido deja la distancia desconocida`() = runTest {
        senderos.senderos.value = listOf(crearSenderoDePrueba(id = "sendero-1"))
        val puntos = FalsoPuntosCapturadosRepositorio(listOf(crearPuntoCapturadoDePrueba(id = "p1")))

        val lista = ObservarPuntosCapturadosConDistanciaUseCase(senderos, puntos, autenticacion)("sendero-1").getOrThrow().first()

        assertNull(lista.single().distanciaAlTrazadoMetros)
        assertFalse(lista.single().superaDistanciaRecomendada)
    }

    @Test
    fun `observar con distancia sin sesion activa falla`() {
        autenticacion.usuarioActual = null

        val resultado = ObservarPuntosCapturadosConDistanciaUseCase(senderos, FalsoPuntosCapturadosRepositorio(), autenticacion)("sendero-1")

        assertTrue(resultado.isFailure)
    }
}
