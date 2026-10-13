package com.unaj.senderos.data.seguimiento

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.recorrido.CalculadoraRecorrido
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07

@OptIn(ExperimentalCoroutinesApi::class)
class ProveedorUbicacionSimuladaTest {

    private fun trazadoAlSur(grados: Double): List<PuntoRecorrido> {
        return listOf(PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE), PuntoRecorrido(LATITUD_BASE - grados, LONGITUD_BASE))
    }

    private fun metrosEntre(primera: LecturaUbicacion, segunda: LecturaUbicacion): Double {
        val destino = PuntoRecorrido(segunda.coordenada.latitud, segunda.coordenada.longitud)
        return CalculadoraRecorrido.distanciaEntre(primera.coordenada, destino) * 1000.0
    }

    private fun metrosAlTrazado(lectura: LecturaUbicacion): Double {
        val proyectado = PuntoRecorrido(lectura.coordenada.latitud, LONGITUD_BASE)
        return CalculadoraRecorrido.distanciaEntre(lectura.coordenada, proyectado) * 1000.0
    }

    @Test
    fun `avanza 1,4 metros por segundo con una lectura por segundo`() = runTest {
        val proveedor = ProveedorUbicacionSimulada(trazadoAlSur(0.01))

        val lecturas = proveedor.observarUbicaciones().take(4).toList()

        assertEquals(1.4, metrosEntre(lecturas[0], lecturas[1]), 0.05)
        assertEquals(1.4, metrosEntre(lecturas[2], lecturas[3]), 0.05)
        assertEquals(3_000L, currentTime)
    }

    @Test
    fun `el multiplicador acelera el avance`() = runTest {
        val proveedor = ProveedorUbicacionSimulada(trazadoAlSur(0.01))
        proveedor.cambiarMultiplicador(5)

        val lecturas = proveedor.observarUbicaciones().take(3).toList()

        assertEquals(7.0, metrosEntre(lecturas[0], lecturas[1]), 0.1)
        assertEquals(7.0, lecturas[1].velocidadMetrosPorSegundo?.toDouble() ?: -1.0, 0.01)
    }

    @Test
    fun `cambiar el multiplicador durante la emision afecta a las lecturas siguientes`() = runTest {
        val proveedor = ProveedorUbicacionSimulada(trazadoAlSur(0.01))
        val lecturas = mutableListOf<LecturaUbicacion>()

        proveedor.observarUbicaciones().take(4).collect { lectura ->
            lecturas.add(lectura)
            if (lecturas.size == 2) proveedor.cambiarMultiplicador(20)
        }

        assertEquals(1.4, metrosEntre(lecturas[0], lecturas[1]), 0.05)
        assertEquals(28.0, metrosEntre(lecturas[2], lecturas[3]), 0.2)
    }

    @Test
    fun `las lecturas traen el rumbo del tramo la velocidad y la precision`() = runTest {
        val proveedor = ProveedorUbicacionSimulada(trazadoAlSur(0.01))

        val lectura = proveedor.observarUbicaciones().take(2).toList()[1]

        assertEquals(180.0, lectura.rumboGrados?.toDouble() ?: -1.0, 0.5)
        assertEquals(1.4, lectura.velocidadMetrosPorSegundo?.toDouble() ?: -1.0, 0.01)
        assertEquals(5.0, lectura.precisionMetros?.toDouble() ?: -1.0, 0.01)
    }

    @Test
    fun `el rumbo cambia al entrar en el siguiente tramo`() = runTest {
        val trazado = listOf(
            PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE),
            PuntoRecorrido(LATITUD_BASE - 0.0002, LONGITUD_BASE),
            PuntoRecorrido(LATITUD_BASE - 0.0002, LONGITUD_BASE + 0.001)
        )

        val lecturas = ProveedorUbicacionSimulada(trazado).observarUbicaciones().take(40).toList()

        assertEquals(180.0, lecturas[2].rumboGrados?.toDouble() ?: -1.0, 0.5)
        assertEquals(90.0, lecturas[39].rumboGrados?.toDouble() ?: -1.0, 0.5)
    }

    @Test
    fun `termina al llegar al final del trazado`() = runTest {
        val proveedor = ProveedorUbicacionSimulada(trazadoAlSur(0.0001))

        val lecturas = proveedor.observarUbicaciones().toList()

        assertTrue(lecturas.size in 3..10)
        assertEquals(LATITUD_BASE - 0.0001, lecturas.last().coordenada.latitud, 0.000001)
    }

    @Test
    fun `el desvio supera cien metros durante diez lecturas y luego vuelve`() = runTest {
        val proveedor = ProveedorUbicacionSimulada(trazadoAlSur(0.02))
        val lecturas = mutableListOf<LecturaUbicacion>()

        proveedor.observarUbicaciones().take(16).collect { lectura ->
            lecturas.add(lectura)
            if (lecturas.size == 2) proveedor.simularDesvio()
        }

        assertTrue(metrosAlTrazado(lecturas[1]) < 1.0)
        (2..11).forEach { indice -> assertTrue("lectura $indice", metrosAlTrazado(lecturas[indice]) > 100.0) }
        (12..15).forEach { indice -> assertTrue("lectura $indice", metrosAlTrazado(lecturas[indice]) < 1.0) }
    }

    @Test
    fun `la ubicacion actual es el primer punto del trazado`() = runTest {
        val proveedor = ProveedorUbicacionSimulada(trazadoAlSur(0.01))

        assertEquals(Result.success<Coordenada?>(Coordenada(LATITUD_BASE, LONGITUD_BASE)), proveedor.obtenerUbicacionActual())
    }

    @Test
    fun `un desvio pendiente al llegar al final termina sobre el trazado`() = runTest {
        val proveedor = ProveedorUbicacionSimulada(trazadoAlSur(0.0003))
        val lecturas = mutableListOf<LecturaUbicacion>()

        proveedor.observarUbicaciones().collect { lectura ->
            lecturas.add(lectura)
            if (lecturas.size == 22) proveedor.simularDesvio()
        }

        assertTrue(metrosAlTrazado(lecturas.last()) < 1.0)
        assertEquals(LATITUD_BASE - 0.0003, lecturas.last().coordenada.latitud, 0.000001)
    }

    @Test
    fun `un trazado de largo cero emite el primer punto y termina`() = runTest {
        val punto = PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE)

        val lecturas = ProveedorUbicacionSimulada(listOf(punto, punto)).observarUbicaciones().toList()

        assertEquals(1, lecturas.size)
        assertEquals(LATITUD_BASE, lecturas[0].coordenada.latitud, 0.0)
        assertEquals(LONGITUD_BASE, lecturas[0].coordenada.longitud, 0.0)
        assertTrue(!lecturas[0].coordenada.latitud.isNaN() && !lecturas[0].coordenada.longitud.isNaN())
    }
}
