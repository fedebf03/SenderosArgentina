package com.unaj.senderos.data.mapas

import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapeoRegionSinConexionTest {

    private val completa = ProgresoRegion(
        recursosCompletados = 10,
        recursosRequeridos = 10,
        bytesCompletados = 2048,
        estaCompleta = true,
        estaDescargando = false
    )

    // Verificar que los metadatos se escriben y se leen sin pérdida
    @Test
    fun `los metadatos hacen viaje de ida y vuelta`() {
        val metadatos = MetadatosRegion(idUsuario = "u1", idSendero = "s1", firma = "abc")

        assertEquals(metadatos, leerMetadatosRegion(metadatos.aBytes()))
    }

    // Verificar que unos metadatos ilegibles o ausentes se leen como nulos
    @Test
    fun `los metadatos invalidos se leen como nulos`() {
        assertNull(leerMetadatosRegion(null))
        assertNull(leerMetadatosRegion("no es json".toByteArray()))
        assertNull(leerMetadatosRegion("{\"otro\":1}".toByteArray()))
    }

    // Verificar que el porcentaje se calcula y se acota entre 0 y 100
    @Test
    fun `el porcentaje se acota`() {
        assertEquals(0, calcularPorcentaje(0, 0))
        assertEquals(50, calcularPorcentaje(5, 10))
        assertEquals(100, calcularPorcentaje(12, 10))
        assertEquals(0, calcularPorcentaje(-3, 10))
    }

    // Verificar que la clave combina usuario y sendero
    @Test
    fun `la clave combina usuario y sendero`() {
        assertEquals("u1/s1", crearClaveRegion("u1", "s1"))
    }

    // Verificar que sin región el estado es no descargado
    @Test
    fun `sin region el estado es no descargado`() {
        assertEquals(EstadoMapaSinConexion.NoDescargado, determinarEstado(null, null, "firma", null))
    }

    // Verificar que una región completa con la misma firma está disponible con su tamaño
    @Test
    fun `region completa con la misma firma esta disponible`() {
        assertEquals(EstadoMapaSinConexion.Disponible(2048), determinarEstado(completa, "firma", "firma", null))
    }

    // Verificar que una región completa con otra firma está desactualizada
    @Test
    fun `region completa con otra firma esta desactualizada`() {
        assertEquals(EstadoMapaSinConexion.Desactualizado(2048), determinarEstado(completa, "vieja", "firma", null))
    }

    // Verificar que una región en descarga informa el porcentaje y los bytes
    @Test
    fun `region en descarga informa el porcentaje y los bytes`() {
        val progreso = ProgresoRegion(3, 12, 100, estaCompleta = false, estaDescargando = true)

        assertEquals(EstadoMapaSinConexion.Descargando(25, 100), determinarEstado(progreso, "firma", "firma", null))
    }

    // Verificar que una región sin progreso conocido se muestra descargando al 0 por ciento
    @Test
    fun `region sin progreso se muestra descargando en cero`() {
        assertEquals(EstadoMapaSinConexion.Descargando(0, 0), determinarEstado(null, "firma", "firma", null))
    }

    // Verificar que una región incompleta y detenida es un error
    @Test
    fun `region incompleta y detenida es un error`() {
        val progreso = ProgresoRegion(3, 12, 100, estaCompleta = false, estaDescargando = false)

        assertEquals(
            EstadoMapaSinConexion.ConError(MENSAJE_DESCARGA_INCOMPLETA),
            determinarEstado(progreso, "firma", "firma", null)
        )
    }

    // Verificar que un mensaje de error tiene prioridad sobre el resto del estado
    @Test
    fun `el mensaje de error tiene prioridad`() {
        assertEquals(EstadoMapaSinConexion.ConError("falló"), determinarEstado(completa, "firma", "firma", "falló"))
    }

    // Verificar que una descarga activa que espera conexión informa porcentaje y bytes
    @Test
    fun `una descarga que espera conexion informa el avance`() {
        val progreso = ProgresoRegion(6, 12, 700, estaCompleta = false, estaDescargando = true)

        assertEquals(
            EstadoMapaSinConexion.EsperandoConexion(50, 700),
            determinarEstado(progreso, "firma", "firma", null, esperandoConexion = true)
        )
    }

    // Verificar que una región completa no queda esperando conexión
    @Test
    fun `una region completa ignora la espera de conexion`() {
        assertEquals(
            EstadoMapaSinConexion.Disponible(2048),
            determinarEstado(completa, "firma", "firma", null, esperandoConexion = true)
        )
    }

    // Verificar que los errores de conexión y de servidor son recuperables hasta el quinto seguido
    @Test
    fun `los errores de conexion y servidor esperan hasta el quinto seguido`() {
        listOf(CategoriaErrorRegion.CONEXION, CategoriaErrorRegion.SERVIDOR).forEach { categoria ->
            assertEquals(DecisionError.ESPERAR_CONEXION, decidirAnteError(categoria, 1))
            assertEquals(DecisionError.ESPERAR_CONEXION, decidirAnteError(categoria, 4))
            assertEquals(DecisionError.DETENER, decidirAnteError(categoria, 5))
        }
    }

    // Verificar que los errores no recuperables detienen la descarga de inmediato
    @Test
    fun `los errores no recuperables detienen la descarga`() {
        assertEquals(DecisionError.DETENER, decidirAnteError(CategoriaErrorRegion.NO_ENCONTRADO, 1))
        assertEquals(DecisionError.DETENER, decidirAnteError(CategoriaErrorRegion.OTRO, 1))
    }

    // Verificar que los mensajes para el usuario están en español y no exponen detalles técnicos
    @Test
    fun `los mensajes de error son distintos y en espanol`() {
        val mensajes = CategoriaErrorRegion.entries.map { categoria -> mensajeErrorParaUsuario(categoria) }

        assertEquals(mensajes.size, mensajes.toSet().size)
        assertTrue(mensajes.all { mensaje -> mensaje.isNotBlank() && !mensaje.contains("REASON") })
    }

    // Verificar que sin región se crea una nueva
    @Test
    fun `sin region se crea una nueva`() {
        assertEquals(AccionDescarga.CREAR, decidirAccionDescarga(null, null, "firma"))
    }

    // Verificar que una región completa con la misma firma no se vuelve a descargar
    @Test
    fun `region completa con la misma firma ya esta disponible`() {
        assertEquals(AccionDescarga.YA_DISPONIBLE, decidirAccionDescarga("firma", completa, "firma"))
    }

    // Verificar que una región incompleta con la misma firma se reactiva sin borrarla
    @Test
    fun `region incompleta con la misma firma se reactiva`() {
        val incompleta = ProgresoRegion(3, 12, 100, estaCompleta = false, estaDescargando = false)

        assertEquals(AccionDescarga.REACTIVAR, decidirAccionDescarga("firma", incompleta, "firma"))
        assertEquals(AccionDescarga.REACTIVAR, decidirAccionDescarga("firma", null, "firma"))
    }

    // Verificar que una región completa de otro trazado se conserva hasta que termine la nueva
    @Test
    fun `region completa de otro trazado se conserva como anterior`() {
        assertEquals(AccionDescarga.CREAR_CONSERVANDO_ANTERIOR, decidirAccionDescarga("vieja", completa, "firma"))
    }

    // Verificar que una región incompleta de otro trazado se reemplaza
    @Test
    fun `region incompleta de otro trazado se reemplaza`() {
        val incompleta = ProgresoRegion(3, 12, 100, estaCompleta = false, estaDescargando = false)

        assertEquals(AccionDescarga.CREAR_REEMPLAZANDO, decidirAccionDescarga("vieja", incompleta, "firma"))
    }

    // Verificar que con una sola región candidata se elige esa y no hay sobrantes
    @Test
    fun `una sola candidata es la principal`() {
        val plan = planificarRegiones(listOf(CandidataRegion(7, estaCompleta = false)))

        assertEquals(PlanRegiones(principal = 7, anterior = null, sobrantes = emptyList()), plan)
    }

    // Verificar que sin candidatas no hay plan
    @Test
    fun `sin candidatas no hay plan`() {
        assertNull(planificarRegiones(emptyList()))
    }

    // Verificar que si la más reciente está completa las demás sobran
    @Test
    fun `si la mas reciente esta completa las demas sobran`() {
        val plan = planificarRegiones(
            listOf(CandidataRegion(1, true), CandidataRegion(3, true), CandidataRegion(2, false))
        )

        assertEquals(PlanRegiones(principal = 3, anterior = null, sobrantes = listOf(2, 1)), plan)
    }

    // Verificar que si la más reciente está incompleta se conserva la completa más reciente como anterior
    @Test
    fun `si la mas reciente esta incompleta se conserva la completa anterior`() {
        val plan = planificarRegiones(
            listOf(CandidataRegion(1, true), CandidataRegion(2, true), CandidataRegion(4, false), CandidataRegion(3, false))
        )

        assertEquals(PlanRegiones(principal = 4, anterior = 2, sobrantes = listOf(3, 1)), plan)
        assertNotEquals(null, plan)
    }
}
