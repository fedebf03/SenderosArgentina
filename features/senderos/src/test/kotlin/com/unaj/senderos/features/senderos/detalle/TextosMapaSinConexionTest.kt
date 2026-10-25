package com.unaj.senderos.features.senderos.detalle

import com.unaj.senderos.core.ui.comun.formatearTamanoMapa
import org.junit.Assert.assertEquals
import com.unaj.senderos.core.domain.model.EstadoMapaSinConexion
import org.junit.Test

class TextosMapaSinConexionTest {

    @Test
    fun `los tamanos chicos se muestran en kilobytes`() {
        assertEquals("0,0 KB", formatearTamanoMapa(0L))
        assertEquals("1,5 KB", formatearTamanoMapa(1_536L))
    }

    @Test
    fun `los tamanos grandes se muestran en megabytes`() {
        assertEquals("1,0 MB", formatearTamanoMapa(1_048_576L))
        assertEquals("12,5 MB", formatearTamanoMapa(13_107_200L))
    }

    @Test
    fun `el progreso muestra porcentaje y tamano`() {
        assertEquals("Descargando… 45 % · 3,0 MB", describirDescarga(EstadoMapaSinConexion.Descargando(45, 3_145_728L)))
        assertEquals(
            "Esperando conexión… 45 % · 3,0 MB",
            describirDescarga(EstadoMapaSinConexion.EsperandoConexion(45, 3_145_728L))
        )
    }
}
