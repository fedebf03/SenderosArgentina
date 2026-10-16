package com.unaj.senderos.core.ui.componentes

import com.unaj.senderos.core.domain.model.Bioma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextosBiomaTest {

    @Test
    fun `cada bioma tiene su nombre en espanol`() {
        assertEquals("Alta montaña", obtenerTextoBioma(Bioma.ALTA_MONTANIA))
        assertEquals("Bosque andino patagónico", obtenerTextoBioma(Bioma.BOSQUE_ANDINO_PATAGONICO))
        assertEquals("Estepa patagónica", obtenerTextoBioma(Bioma.ESTEPA_PATAGONICA))
        assertEquals("Puna", obtenerTextoBioma(Bioma.PUNA))
        assertEquals("Yungas", obtenerTextoBioma(Bioma.YUNGAS))
        assertEquals("Monte", obtenerTextoBioma(Bioma.MONTE))
        assertEquals("Sierras", obtenerTextoBioma(Bioma.SIERRAS))
        assertEquals("Litoral", obtenerTextoBioma(Bioma.LITORAL))
        assertEquals("Costa marítima", obtenerTextoBioma(Bioma.COSTA_MARITIMA))
        assertEquals("Sin definir", obtenerTextoBioma(Bioma.SIN_DEFINIR))
    }

    @Test
    fun `los nombres de los biomas no se repiten ni quedan vacios`() {
        val nombres = Bioma.entries.map { bioma -> obtenerTextoBioma(bioma) }
        assertEquals(Bioma.entries.size, nombres.toSet().size)
        assertTrue(nombres.all { nombre -> nombre.isNotBlank() })
    }

    @Test
    fun `la descripcion de accesibilidad del bioma menciona su nombre`() {
        assertEquals("Bioma: puna", obtenerDescripcionBioma(Bioma.PUNA))
        assertEquals("Bioma: alta montaña", obtenerDescripcionBioma(Bioma.ALTA_MONTANIA))
    }
}
