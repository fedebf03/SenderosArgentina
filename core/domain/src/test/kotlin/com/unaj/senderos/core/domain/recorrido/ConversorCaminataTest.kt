package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.PuntoCapturado
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Convertir una caminata libre (trazado caminado y puntos capturados) en el borrador de un sendero nuevo
class ConversorCaminataTest {

    // Generar una caminata casi recta hacia el este, con un zigzag de 1 m, de unos 17 m por paso
    private fun caminataRecta(cantidad: Int = 60): List<Coordenada> = List(cantidad) { indice ->
        Coordenada(
            latitud = -41.13 + (if (indice % 2 == 0) 0.0 else 0.00001),
            longitud = -71.30 + indice * 0.0002
        )
    }

    // Generar un cuadrado caminado de a pasos de 11 m que termina donde empezó
    private fun caminataCircular(): List<Coordenada> {
        val lado = 30
        val paso = 0.0001
        val inicio = Coordenada(-41.13, -71.30)
        val norte = List(lado) { indice -> Coordenada(inicio.latitud + indice * paso, inicio.longitud) }
        val este = List(lado) { indice -> Coordenada(inicio.latitud + lado * paso, inicio.longitud + indice * paso) }
        val sur = List(lado) { indice -> Coordenada(inicio.latitud + (lado - indice) * paso, inicio.longitud + lado * paso) }
        val oeste = List(lado) { indice -> Coordenada(inicio.latitud, inicio.longitud + (lado - indice) * paso) }
        return norte + este + sur + oeste + inicio
    }

    // Armar un punto capturado de prueba en la posición indicada
    private fun punto(
        latitud: Double,
        longitud: Double,
        tipo: TipoPunto = TipoPunto.MIRADOR,
        nota: String = "",
        foto: String = ""
    ) = PuntoCapturado(
        id = "punto-$latitud-$longitud",
        idUsuario = "usuario-1",
        idSendero = "libre-1",
        latitud = latitud,
        longitud = longitud,
        tipo = tipo,
        nota = nota,
        foto = foto,
        instanteMilisegundos = 1_000L
    )

    @Test
    fun `una caminata recta se convierte en un sendero lineal simplificado con el mismo inicio y final`() {
        val caminata = caminataRecta()

        val trazado = ConversorCaminata.aTrazado(caminata, emptyList(), "Caminata del domingo").getOrThrow()

        assertEquals(TipoRecorrido.LINEAL, trazado.tipoRecorrido)
        assertTrue("debe simplificar", trazado.recorrido.size < caminata.size)
        assertEquals(caminata.first().latitud, trazado.recorrido.first().latitud, 1e-9)
        assertEquals(caminata.last().longitud, trazado.recorrido.last().longitud, 1e-9)
    }

    @Test
    fun `conserva el nombre sugerido y no trae perfil de elevacion`() {
        val trazado = ConversorCaminata.aTrazado(caminataRecta(), emptyList(), "Caminata del domingo").getOrThrow()

        assertEquals("Caminata del domingo", trazado.nombre)
        assertNull(trazado.perfil)
    }

    @Test
    fun `una caminata que termina donde empezo se detecta como circular y no repite el punto de cierre`() {
        val trazado = ConversorCaminata.aTrazado(caminataCircular(), emptyList(), "Vuelta a la manzana").getOrThrow()

        assertEquals(TipoRecorrido.CIRCULAR, trazado.tipoRecorrido)
        val primero = trazado.recorrido.first()
        val ultimo = trazado.recorrido.last()
        assertTrue("el cierre lo hace el tipo circular", primero.latitud != ultimo.latitud || primero.longitud != ultimo.longitud)
    }

    @Test
    fun `los puntos capturados se incorporan con tipo, nota como nombre y foto, en orden a lo largo del camino`() {
        val caminata = caminataRecta()
        val tardio = punto(-41.13, caminata[40].longitud, TipoPunto.AGUA, nota = "  Arroyo  ", foto = "/fotos/arroyo.jpg")
        val temprano = punto(-41.13, caminata[10].longitud, TipoPunto.MIRADOR, nota = "Vista al lago")

        val trazado = ConversorCaminata.aTrazado(caminata, listOf(tardio, temprano), "Caminata").getOrThrow()

        val interes = trazado.recorrido.filter { punto -> punto.tipo != TipoPunto.PASO }
        assertEquals(listOf("Vista al lago", "Arroyo"), interes.map { punto -> punto.nombre })
        assertEquals(listOf(TipoPunto.MIRADOR, TipoPunto.AGUA), interes.map { punto -> punto.tipo })
        assertEquals("/fotos/arroyo.jpg", interes[1].foto)
        assertEquals("", interes[0].foto)
    }

    @Test
    fun `los puntos capturados no cambian el inicio ni el final del sendero`() {
        val caminata = caminataRecta()
        val alPrincipio = punto(-41.13, caminata.first().longitud - 0.0003, TipoPunto.CAMPING)
        val alFinal = punto(-41.13, caminata.last().longitud + 0.0003, TipoPunto.REFUGIO)

        val trazado = ConversorCaminata.aTrazado(caminata, listOf(alPrincipio, alFinal), "Caminata").getOrThrow()

        assertEquals(TipoPunto.PASO, trazado.recorrido.first().tipo)
        assertEquals(TipoPunto.PASO, trazado.recorrido.last().tipo)
        assertEquals(2, trazado.recorrido.count { punto -> punto.tipo != TipoPunto.PASO })
    }

    @Test
    fun `una caminata de menos de 50 metros es muy corta para ser un sendero`() {
        val corta = List(5) { indice -> Coordenada(-41.13, -71.30 + indice * 0.00005) }

        val resultado = ConversorCaminata.aTrazado(corta, emptyList(), "Caminata")

        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()?.message.orEmpty().contains("muy corta"))
    }

    @Test
    fun `la distancia minima para convertir es de 50 metros`() {
        assertTrue(!ConversorCaminata.esConvertible(0.0))
        assertTrue(!ConversorCaminata.esConvertible(0.049))
        assertTrue(ConversorCaminata.esConvertible(0.05))
        assertTrue(ConversorCaminata.esConvertible(3.2))
    }

    @Test
    fun `una caminata sin dos posiciones distintas no se puede convertir`() {
        val quieta = List(5) { Coordenada(-41.13, -71.30) }

        assertTrue(ConversorCaminata.aTrazado(quieta, emptyList(), "Caminata").isFailure)
        assertTrue(ConversorCaminata.aTrazado(emptyList(), emptyList(), "Caminata").isFailure)
    }

    @Test
    fun `la distancia del sendero convertido se parece a la distancia caminada`() {
        val caminata = caminataRecta()

        val trazado = ConversorCaminata.aTrazado(caminata, emptyList(), "Caminata").getOrThrow()

        val caminada = CalculadoraRecorrido.distanciaKilometros(
            caminata.map { coordenada -> com.unaj.senderos.core.domain.model.PuntoRecorrido(coordenada.latitud, coordenada.longitud) },
            TipoRecorrido.LINEAL
        )
        val convertida = CalculadoraRecorrido.distanciaKilometros(trazado.recorrido, trazado.tipoRecorrido)
        assertEquals(caminada, convertida, caminada * 0.05)
    }
}
