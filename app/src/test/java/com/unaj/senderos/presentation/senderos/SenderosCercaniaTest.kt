package com.unaj.senderos.presentation.senderos

import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.falsos.crearSenderoDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SenderosCercaniaTest {

    private val usuario = Coordenada(latitud = 0.0, longitud = 0.0)
    private val kilometrosPorGradoEcuatorial = 111.195

    private val cercano = crearSenderoDePrueba(
        id = "cercano", nombre = "Cerro Cercano", region = "Norte",
        recorrido = listOf(PuntoRecorrido(latitud = 0.0, longitud = 1.0), PuntoRecorrido(latitud = 50.0, longitud = 50.0))
    )
    private val lejano = crearSenderoDePrueba(
        id = "lejano", nombre = "Cerro Lejano", region = "Norte",
        recorrido = listOf(PuntoRecorrido(latitud = 0.0, longitud = 3.0))
    )
    private val sinRecorrido = crearSenderoDePrueba(id = "sin-recorrido", nombre = "Cerro Sin Trazado", region = "Sur")

    private fun crearEstado(
        ubicacion: Coordenada? = usuario,
        ordenar: Boolean = false,
        texto: String = ""
    ): SenderosEstadoVisual {
        return SenderosEstadoVisual(
            senderos = listOf(sinRecorrido, lejano, cercano),
            textoBusqueda = texto,
            ubicacionActual = ubicacion,
            ordenarPorCercania = ordenar
        )
    }

    @Test
    fun `la calculadora mide la distancia desde una coordenada a un punto del recorrido`() {
        val distancia = CalculadoraRecorrido.distanciaEntre(usuario, PuntoRecorrido(latitud = 0.0, longitud = 1.0))

        assertEquals(kilometrosPorGradoEcuatorial, distancia, 0.05)
    }

    @Test
    fun `la calculadora mide la distancia en diagonal`() {
        val distancia = CalculadoraRecorrido.distanciaEntre(usuario, PuntoRecorrido(latitud = 1.0, longitud = 1.0))

        assertEquals(157.25, distancia, 0.1)
    }

    @Test
    fun `la distancia de un sendero se mide al primer punto de su recorrido`() {
        val distancia = crearEstado().distanciaKilometrosA(cercano)

        assertNotNull(distancia)
        assertEquals(kilometrosPorGradoEcuatorial, distancia ?: 0.0, 0.05)
    }

    @Test
    fun `un sendero sin recorrido no tiene distancia`() {
        assertNull(crearEstado().distanciaKilometrosA(sinRecorrido))
    }

    @Test
    fun `sin ubicacion ningun sendero tiene distancia`() {
        assertNull(crearEstado(ubicacion = null).distanciaKilometrosA(cercano))
    }

    @Test
    fun `la dificultad se combina con el orden por cercania`() {
        val facilLejano = crearSenderoDePrueba(
            id = "facil-lejano", dificultad = Dificultad.BAJA,
            recorrido = listOf(PuntoRecorrido(latitud = 0.0, longitud = 3.0))
        )
        val facilCercano = crearSenderoDePrueba(
            id = "facil-cercano", dificultad = Dificultad.BAJA,
            recorrido = listOf(PuntoRecorrido(latitud = 0.0, longitud = 1.0))
        )
        val exigenteCercano = crearSenderoDePrueba(
            id = "exigente-cercano", dificultad = Dificultad.ALTA,
            recorrido = listOf(PuntoRecorrido(latitud = 0.0, longitud = 0.5))
        )
        val estado = SenderosEstadoVisual(
            senderos = listOf(facilLejano, exigenteCercano, facilCercano),
            ubicacionActual = usuario,
            ordenarPorCercania = true,
            dificultadesSeleccionadas = setOf(Dificultad.BAJA)
        )

        assertEquals(listOf(facilCercano, facilLejano), estado.senderosFiltrados)
    }

    @Test
    fun `sin ordenar por cercania se conserva el orden original`() {
        val estado = crearEstado(ordenar = false)

        assertEquals(listOf(sinRecorrido, lejano, cercano), estado.senderosFiltrados)
    }

    @Test
    fun `al ordenar por cercania los senderos sin distancia quedan al final`() {
        val estado = crearEstado(ordenar = true)

        assertEquals(listOf(cercano, lejano, sinRecorrido), estado.senderosFiltrados)
    }

    @Test
    fun `ordenar por cercania sin ubicacion conserva el orden original`() {
        val estado = crearEstado(ubicacion = null, ordenar = true)

        assertEquals(listOf(sinRecorrido, lejano, cercano), estado.senderosFiltrados)
    }

    @Test
    fun `el orden por cercania se combina con el filtro de busqueda`() {
        val estado = crearEstado(ordenar = true, texto = "Norte")

        assertEquals(listOf(cercano, lejano), estado.senderosFiltrados)
    }

    @Test
    fun `el orden por cercania con busqueda sin coincidencias devuelve lista vacia`() {
        val estado = crearEstado(ordenar = true, texto = "Inexistente")

        assertEquals(emptyList<Any>(), estado.senderosFiltrados)
    }
}
