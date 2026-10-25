package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EdicionRecorridoTest {

    private val inicio = PuntoRecorrido(latitud = 0.0, longitud = 0.0)
    private val medio = PuntoRecorrido(latitud = 0.0, longitud = 0.01)
    private val fin = PuntoRecorrido(latitud = 0.0, longitud = 0.02)

    @Test
    fun `insertar antes agrega el punto medio del tramo anterior`() {
        val edicion = EdicionRecorrido(listOf(inicio, fin), seleccion = null)

        val resultado = edicion.insertarAntes(1)

        assertEquals(3, resultado.puntos.size)
        assertEquals(0.0, resultado.puntos[1].latitud, 1e-9)
        assertEquals(0.01, resultado.puntos[1].longitud, 1e-9)
        assertEquals(TipoPunto.PASO, resultado.puntos[1].tipo)
    }

    @Test
    fun `insertar antes del primer punto no cambia nada`() {
        val edicion = EdicionRecorrido(listOf(inicio, fin), seleccion = 0)

        assertEquals(edicion, edicion.insertarAntes(0))
    }

    // El punto elegido sigue siendo el mismo aunque se corra una posición
    @Test
    fun `insertar antes corre la seleccion al mismo punto`() {
        val edicion = EdicionRecorrido(listOf(inicio, fin), seleccion = 1)

        val resultado = edicion.insertarAntes(1)

        assertEquals(2, resultado.seleccion)
        assertEquals(fin, resultado.puntos[2])
    }

    @Test
    fun `un punto de interes se inserta en el tramo mas cercano y no al final`() {
        val edicion = EdicionRecorrido(listOf(inicio, medio, fin), seleccion = null)
        val mirador = PuntoRecorrido(latitud = 0.0005, longitud = 0.004, tipo = TipoPunto.MIRADOR, nombre = "Vista")

        val resultado = edicion.insertarPuntoDeInteres(mirador, TipoRecorrido.LINEAL)

        assertEquals(listOf(inicio, mirador, medio, fin), resultado.puntos)
    }

    // En un lineal el inicio y el final no cambian aunque el punto quede más allá del final
    @Test
    fun `un punto de interes despues del final queda como anteultimo`() {
        val edicion = EdicionRecorrido(listOf(inicio, fin), seleccion = null)
        val refugio = PuntoRecorrido(latitud = 0.0, longitud = 0.03, tipo = TipoPunto.REFUGIO)

        val resultado = edicion.insertarPuntoDeInteres(refugio, TipoRecorrido.LINEAL)

        assertEquals(listOf(inicio, refugio, fin), resultado.puntos)
        assertNull(resultado.seleccion)
    }

    // En un circular el último tramo es el de cierre (del último punto al primero)
    @Test
    fun `en un circular un punto de interes cerca del tramo de cierre queda al final`() {
        val vuelta = PuntoRecorrido(latitud = 0.01, longitud = 0.02)
        val edicion = EdicionRecorrido(listOf(inicio, fin, vuelta), seleccion = null)
        val mirador = PuntoRecorrido(latitud = 0.0055, longitud = 0.0095, tipo = TipoPunto.MIRADOR)

        val resultado = edicion.insertarPuntoDeInteres(mirador, TipoRecorrido.CIRCULAR)

        assertEquals(listOf(inicio, fin, vuelta, mirador), resultado.puntos)
    }

    @Test
    fun `con un solo punto el punto de interes se agrega al final`() {
        val edicion = EdicionRecorrido(listOf(inicio), seleccion = null)
        val agua = PuntoRecorrido(latitud = 0.0, longitud = 0.01, tipo = TipoPunto.AGUA)

        assertEquals(listOf(inicio, agua), edicion.insertarPuntoDeInteres(agua, TipoRecorrido.LINEAL).puntos)
    }
}
