package com.unaj.senderos.presentation.tema

import androidx.compose.ui.graphics.Color
import com.unaj.senderos.domain.model.Bioma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletaBiomaTest {

    private val biomasConColor = Bioma.entries.filter { bioma -> bioma != Bioma.SIN_DEFINIR }
    private val fondoDeTarjeta = Color.White
    private val fondoOscuroDeReferencia = Color(0xFF1C1B1F)

    @Test
    fun `calcular el contraste entre blanco y negro da 21 a 1`() {
        assertEquals(21.0, calcularContraste(Color.White, Color.Black), 0.01)
    }

    @Test
    fun `calcular el contraste de un color consigo mismo da 1 a 1`() {
        assertEquals(1.0, calcularContraste(Color(0xFF546E7A), Color(0xFF546E7A)), 0.001)
    }

    @Test
    fun `calcular el contraste no depende del orden de los colores`() {
        val primero = Color(0xFF1B5E20)
        val segundo = Color(0xFFD3E8D3)
        assertEquals(calcularContraste(primero, segundo), calcularContraste(segundo, primero), 0.0001)
    }

    @Test
    fun `sin definir no tiene colores propios porque usa los del tema`() {
        assertNull(obtenerColoresBioma(Bioma.SIN_DEFINIR, esTemaOscuro = false))
        assertNull(obtenerColoresBioma(Bioma.SIN_DEFINIR, esTemaOscuro = true))
    }

    @Test
    fun `todos los biomas definidos tienen colores en tema claro y oscuro`() {
        biomasConColor.forEach { bioma ->
            assertNotNull("Falta el color claro de $bioma", obtenerColoresBioma(bioma, esTemaOscuro = false))
            assertNotNull("Falta el color oscuro de $bioma", obtenerColoresBioma(bioma, esTemaOscuro = true))
        }
    }

    @Test
    fun `los colores principales claros de los biomas son todos distintos`() {
        val principales = biomasConColor.mapNotNull { bioma -> obtenerColoresBioma(bioma, false)?.principal }
        assertEquals(biomasConColor.size, principales.toSet().size)
    }

    @Test
    fun `el texto sobre el contenedor claro tiene al menos 4,5 a 1 de contraste`() {
        biomasConColor.forEach { bioma ->
            val colores = requireNotNull(obtenerColoresBioma(bioma, esTemaOscuro = false))
            val contraste = calcularContraste(colores.sobreContenedor, colores.contenedor)
            assertTrue("Contraste insuficiente en $bioma: $contraste", contraste >= 4.5)
        }
    }

    @Test
    fun `el texto sobre el contenedor oscuro tiene al menos 4,5 a 1 de contraste`() {
        biomasConColor.forEach { bioma ->
            val colores = requireNotNull(obtenerColoresBioma(bioma, esTemaOscuro = true))
            val contraste = calcularContraste(colores.sobreContenedor, colores.contenedor)
            assertTrue("Contraste insuficiente en $bioma: $contraste", contraste >= 4.5)
        }
    }

    @Test
    fun `la linea del bioma se distingue del fondo del mapa con al menos 3 a 1`() {
        biomasConColor.forEach { bioma ->
            val colores = requireNotNull(obtenerColoresBioma(bioma, esTemaOscuro = false))
            val contraste = calcularContraste(colores.principal, FONDO_MAPA_REFERENCIA)
            assertTrue("La línea de $bioma se pierde sobre el mapa: $contraste", contraste >= 3.0)
        }
    }

    @Test
    fun `la franja del bioma se distingue de la tarjeta clara con al menos 3 a 1`() {
        biomasConColor.forEach { bioma ->
            val colores = requireNotNull(obtenerColoresBioma(bioma, esTemaOscuro = false))
            val contraste = calcularContraste(colores.principal, fondoDeTarjeta)
            assertTrue("La franja de $bioma se pierde sobre la tarjeta: $contraste", contraste >= 3.0)
        }
    }

    @Test
    fun `el color principal oscuro se distingue del fondo oscuro con al menos 3 a 1`() {
        biomasConColor.forEach { bioma ->
            val colores = requireNotNull(obtenerColoresBioma(bioma, esTemaOscuro = true))
            val contraste = calcularContraste(colores.principal, fondoOscuroDeReferencia)
            assertTrue("El color de $bioma se pierde sobre fondo oscuro: $contraste", contraste >= 3.0)
        }
    }
}
