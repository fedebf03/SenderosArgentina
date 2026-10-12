package com.unaj.senderos.presentation.detalle

import org.junit.Assert.assertEquals
import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import org.junit.Test

class TextosMapaSinConexionTest {

    // Verificar que los tamaños menores a un megabyte se muestran en KB con coma decimal
    @Test
    fun `los tamanos chicos se muestran en kilobytes`() {
        assertEquals("0,0 KB", formatearTamanoMapa(0L))
        assertEquals("1,5 KB", formatearTamanoMapa(1_536L))
    }

    // Verificar que desde un megabyte se muestran en MB con coma decimal
    @Test
    fun `los tamanos grandes se muestran en megabytes`() {
        assertEquals("1,0 MB", formatearTamanoMapa(1_048_576L))
        assertEquals("12,5 MB", formatearTamanoMapa(13_107_200L))
    }

    // Verificar el texto de progreso con porcentaje y tamaño descargado
    @Test
    fun `el progreso muestra porcentaje y tamano`() {
        assertEquals("Descargando… 45 % · 3,0 MB", describirDescarga(EstadoMapaSinConexion.Descargando(45, 3_145_728L)))
        assertEquals(
            "Esperando conexión… 45 % · 3,0 MB",
            describirDescarga(EstadoMapaSinConexion.EsperandoConexion(45, 3_145_728L))
        )
    }
}
