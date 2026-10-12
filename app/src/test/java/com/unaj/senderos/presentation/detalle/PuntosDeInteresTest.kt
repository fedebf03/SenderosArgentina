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

    // Verificar que se omiten los pasos y se conserva el índice original con su distancia acumulada
    @Test
    fun `los puntos de interes omiten los pasos y traen su distancia acumulada`() {
        val puntos = obtenerPuntosDeInteres(crearSenderoDePrueba(recorrido = recorrido))

        assertEquals(listOf(0, 2, 3), puntos.map { punto -> punto.indice })
        assertEquals(listOf(TipoPunto.REFUGIO, TipoPunto.AGUA, TipoPunto.MIRADOR), puntos.map { punto -> punto.tipo })
        assertEquals(0.0, puntos[0].distanciaDesdeInicioKilometros, 0.0)
        assertEquals(222.39, puntos[1].distanciaDesdeInicioKilometros, 0.1)
        assertEquals(333.58, puntos[2].distanciaDesdeInicioKilometros, 0.2)
    }

    // Verificar que sin sendero o sin puntos distintos de paso no hay sección
    @Test
    fun `sin puntos de interes la lista es vacia`() {
        val soloPasos = listOf(PuntoRecorrido(0.0, 0.0), PuntoRecorrido(0.0, 1.0))

        assertTrue(obtenerPuntosDeInteres(null).isEmpty())
        assertTrue(obtenerPuntosDeInteres(crearSenderoDePrueba(recorrido = soloPasos)).isEmpty())
    }

    // Verificar que los puntos con coordenadas inválidas se omiten y no contaminan la distancia acumulada
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

    // Verificar el texto con tipo y nombre, o solo el tipo cuando no hay nombre
    @Test
    fun `el texto une tipo y nombre o muestra solo el tipo`() {
        val conNombre = PuntoDeInteres(0, TipoPunto.AGUA, "Arroyo Chico", 2.3)
        val sinNombre = PuntoDeInteres(1, TipoPunto.MIRADOR, "  ", 0.0)

        assertEquals("Agua · Arroyo Chico", conNombre.texto())
        assertEquals("Mirador", sinNombre.texto())
    }

    // Verificar el formato argentino de kilómetros con un decimal
    @Test
    fun `los kilometros se muestran con coma y un decimal`() {
        assertEquals("km 2,3", formatearKilometrosDesdeInicio(2.34))
        assertEquals("km 0,0", formatearKilometrosDesdeInicio(0.0))
        assertEquals("km 12,0", formatearKilometrosDesdeInicio(12.0))
    }

    // Verificar que la coordenada del punto elegido es la enfocada y se repite con otro número
    @Test
    fun `la coordenada del punto de interes se obtiene por su indice`() {
        val estado = DetalleSenderoEstadoVisual(sendero = crearSenderoDePrueba(recorrido = recorrido))

        assertEquals(Coordenada(0.0, 2.0), estado.coordenadaDelPunto(2))
        assertEquals(null, estado.coordenadaDelPunto(9))
    }
}
