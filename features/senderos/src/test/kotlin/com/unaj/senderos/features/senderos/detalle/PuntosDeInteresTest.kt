package com.unaj.senderos.presentation.detalle

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.falsos.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuntosDeInteresTest {

    private val recorrido = listOf(
        PuntoRecorrido(latitud = 0.0, longitud = 0.0, tipo = TipoPunto.REFUGIO, nombre = "Base"),
        PuntoRecorrido(latitud = 0.0, longitud = 1.0),
        PuntoRecorrido(latitud = 0.0, longitud = 2.0, tipo = TipoPunto.AGUA),
        PuntoRecorrido(latitud = 0.0, longitud = 3.0, tipo = TipoPunto.MIRADOR, nombre = "Cima")
    )

    @Test
    fun `los puntos de interes omiten los pasos y traen su distancia acumulada`() {
        val puntos = obtenerPuntosDeInteres(crearSenderoDePrueba(recorrido = recorrido))

        assertEquals(listOf(0, 2, 3), puntos.map { punto -> punto.indice })
        assertEquals(listOf(TipoPunto.REFUGIO, TipoPunto.AGUA, TipoPunto.MIRADOR), puntos.map { punto -> punto.tipo })
        assertEquals(0.0, puntos[0].distanciaDesdeInicioKilometros, 0.0)
        assertEquals(222.39, puntos[1].distanciaDesdeInicioKilometros, 0.1)
        assertEquals(333.58, puntos[2].distanciaDesdeInicioKilometros, 0.2)
    }

    @Test
    fun `sin puntos de interes la lista es vacia`() {
        val soloPasos = listOf(PuntoRecorrido(0.0, 0.0), PuntoRecorrido(0.0, 1.0))

        assertTrue(obtenerPuntosDeInteres(null).isEmpty())
        assertTrue(obtenerPuntosDeInteres(crearSenderoDePrueba(recorrido = soloPasos)).isEmpty())
    }

    @Test
    fun `los puntos con coordenadas invalidas se omiten sin romper la distancia`() {
        val conInvalidos = listOf(
            PuntoRecorrido(latitud = 0.0, longitud = 0.0),
            PuntoRecorrido(latitud = 200.0, longitud = 0.0, tipo = TipoPunto.AGUA),
            PuntoRecorrido(latitud = Double.NaN, longitud = 1.0, tipo = TipoPunto.PELIGRO),
            PuntoRecorrido(latitud = 0.0, longitud = 1.0, tipo = TipoPunto.MIRADOR)
        )

        val puntos = obtenerPuntosDeInteres(crearSenderoDePrueba(recorrido = conInvalidos))

        assertEquals(listOf(3), puntos.map { punto -> punto.indice })
        assertEquals(111.19, puntos[0].distanciaDesdeInicioKilometros, 0.1)
    }

    @Test
    fun `el texto une tipo y nombre o muestra solo el tipo`() {
        val conNombre = PuntoDeInteres(0, TipoPunto.AGUA, "Arroyo Chico", 2.3)
        val sinNombre = PuntoDeInteres(1, TipoPunto.MIRADOR, "  ", 0.0)

        assertEquals("Agua · Arroyo Chico", conNombre.texto())
        assertEquals("Mirador", sinNombre.texto())
    }

    @Test
    fun `la descripcion accesible dice tipo nombre y kilometro desde el inicio`() {
        val conNombre = PuntoDeInteres(0, TipoPunto.AGUA, "Arroyo Chico", 2.34)
        val sinNombre = PuntoDeInteres(1, TipoPunto.MIRADOR, "  ", 12.0)

        assertEquals("Agua, Arroyo Chico, kilómetro 2,3 desde el inicio", conNombre.descripcionAccesible())
        assertEquals("Mirador, kilómetro 12 desde el inicio", sinNombre.descripcionAccesible())
    }

    @Test
    fun `los kilometros se muestran con coma y un decimal`() {
        assertEquals("km 2,3", formatearKilometrosDesdeInicio(2.34))
        assertEquals("km 0,0", formatearKilometrosDesdeInicio(0.0))
        assertEquals("km 12,0", formatearKilometrosDesdeInicio(12.0))
    }

    @Test
    fun `la coordenada del punto de interes se obtiene por su indice`() {
        val estado = DetalleSenderoEstadoVisual(sendero = crearSenderoDePrueba(recorrido = recorrido))

        assertEquals(Coordenada(0.0, 2.0), estado.coordenadaDelPunto(2))
        assertEquals(null, estado.coordenadaDelPunto(9))
    }

    @Test
    fun `el punto de interes trae la foto del punto del recorrido`() {
        val conFoto = listOf(
            PuntoRecorrido(latitud = 0.0, longitud = 0.0, tipo = TipoPunto.MIRADOR, nombre = "Cima", foto = "/fotos/cima.jpg"),
            PuntoRecorrido(latitud = 0.0, longitud = 1.0, tipo = TipoPunto.AGUA)
        )

        val puntos = obtenerPuntosDeInteres(crearSenderoDePrueba(recorrido = conFoto))

        assertEquals(listOf("/fotos/cima.jpg", ""), puntos.map { punto -> punto.foto })
    }
}
