package com.unaj.senderos.data.repository

import com.unaj.senderos.data.local.PuntoRecorridoEntidad
import com.unaj.senderos.data.mapeo.aEntidad
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.falsos.FalsoAlmacenFotos
import com.unaj.senderos.falsos.FalsoEjecutorTransaccionesReversible
import com.unaj.senderos.falsos.FalsoPlanificadorSincronizacion
import com.unaj.senderos.falsos.FalsoPuntoCapturadoDao
import com.unaj.senderos.falsos.FalsoSenderoDao
import com.unaj.senderos.falsos.crearPuntoCapturadoDePrueba
import com.unaj.senderos.falsos.crearSenderoDePrueba
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07
private const val AHORA = 77_000L

@OptIn(ExperimentalCoroutinesApi::class)
class PuntosCapturadosRepositorioOfflineFirstTest {

    private val puntoDao = FalsoPuntoCapturadoDao()
    private val senderoDao = FalsoSenderoDao()
    private val planificador = FalsoPlanificadorSincronizacion()
    private val almacenFotos = FalsoAlmacenFotos()
    private val recorridoBase = listOf(
        PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE, TipoPunto.PASO, "A"),
        PuntoRecorrido(LATITUD_BASE - 0.004, LONGITUD_BASE, TipoPunto.PASO, "B"),
        PuntoRecorrido(LATITUD_BASE - 0.008, LONGITUD_BASE, TipoPunto.PASO, "C")
    )

    // Construir el repositorio con el ejecutor reversible y el alcance indicado
    private fun TestScope.crearRepositorio(alcance: CoroutineScope = backgroundScope + SupervisorJob()): PuntosCapturadosRepositorioOfflineFirst {
        return PuntosCapturadosRepositorioOfflineFirst(
            puntoDao,
            senderoDao,
            FalsoEjecutorTransaccionesReversible(senderoDao, puntoDao),
            planificador,
            almacenFotos,
            alcance
        ) { AHORA }
    }

    // Guardar en la tabla falsa un sendero con el recorrido, tipo y distancia indicados
    private fun guardarSendero(
        recorrido: List<PuntoRecorrido> = recorridoBase,
        tipo: TipoRecorrido = TipoRecorrido.LINEAL,
        distanciaKilometros: Double = 12.0
    ) {
        val sendero = crearSenderoDePrueba(id = "sendero-1", recorrido = recorrido)
            .copy(tipoRecorrido = tipo, distanciaKilometros = distanciaKilometros)
        senderoDao.entidades.value = listOf(sendero.aEntidad(1_000L, EstadoSincronizacion.SINCRONIZADO, false))
    }

    // Guardar en la tabla falsa un punto capturado cerca del segundo tramo
    private fun guardarCapturado(
        id: String,
        nota: String,
        gradosAlSur: Double = 0.006,
        instante: Long = 1_000L,
        foto: String = "",
        gradosAlEste: Double = 0.0001
    ) {
        val punto = crearPuntoCapturadoDePrueba(
            id = id,
            latitud = LATITUD_BASE - gradosAlSur,
            longitud = LONGITUD_BASE + gradosAlEste,
            nota = nota,
            foto = foto,
            instanteMilisegundos = instante
        )
        puntoDao.entidades.value = puntoDao.entidades.value + punto.aEntidad()
    }

    // Obtener los nombres del recorrido guardado del sendero
    private fun nombresGuardados(): List<String> {
        return senderoDao.entidades.value.single().recorrido.map { punto -> punto.nombre }
    }

    // Verificar que un punto guardado se observa solo para su usuario y su sendero
    @Test
    fun `un punto guardado se observa por usuario y sendero`() = runTest {
        val repositorio = crearRepositorio()
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "a"))
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "b", idSendero = "otro"))
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "c", idUsuario = "otro-usuario"))

        val observados = repositorio.observarPendientes("usuario-1", "sendero-1").first()

        assertEquals(listOf("a"), observados.map { punto -> punto.id })
    }

    // Verificar que los puntos salen en orden de captura
    @Test
    fun `los puntos se observan en orden de captura`() = runTest {
        val repositorio = crearRepositorio()
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "tarde", instanteMilisegundos = 9_000L))
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "temprano", instanteMilisegundos = 1_000L))

        val observados = repositorio.observarPendientes("usuario-1", "sendero-1").first()

        assertEquals(listOf("temprano", "tarde"), observados.map { punto -> punto.id })
    }

    // Verificar que un error del DAO se devuelve como failure con mensaje propio en español
    @Test
    fun `un error al guardar se devuelve como failure con mensaje propio`() = runTest {
        puntoDao.errorAlGuardar = IllegalStateException("SQLITE_FULL")

        val resultado = crearRepositorio().guardar(crearPuntoCapturadoDePrueba())

        assertTrue(resultado.isFailure)
        assertEquals("No se pudo guardar el punto", resultado.exceptionOrNull()?.message)
    }

    // Verificar que descartar borra solo los identificadores indicados
    @Test
    fun `descartar borra solo los puntos indicados`() = runTest {
        val repositorio = crearRepositorio()
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "a"))
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "b"))

        val resultado = repositorio.descartar("usuario-1", listOf("a"))

        assertTrue(resultado.isSuccess)
        assertEquals(listOf("b"), repositorio.observarPendientes("usuario-1", "sendero-1").first().map { punto -> punto.id })
    }

    // Verificar que descartar borra los archivos de foto de los puntos descartados después de borrar en la base
    @Test
    fun `descartar borra los archivos de foto de los descartados`() = runTest {
        val repositorio = crearRepositorio()
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "a", foto = "/fotos/a.jpg"))
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "b", foto = "/fotos/b.jpg"))
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "c", foto = ""))

        repositorio.descartar("usuario-1", listOf("a", "c"))

        assertEquals(listOf("/fotos/a.jpg"), almacenFotos.rutasBorradas)
    }

    // Verificar que si falla el borrado en la base no se borra ningún archivo y el mensaje es propio
    @Test
    fun `descartar no borra archivos si falla el borrado en la base`() = runTest {
        val repositorio = crearRepositorio()
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "a", foto = "/fotos/a.jpg"))
        puntoDao.errorAlBorrar = IllegalStateException("SQLITE_BUSY")

        val resultado = repositorio.descartar("usuario-1", listOf("a"))

        assertTrue(resultado.isFailure)
        assertTrue(almacenFotos.rutasBorradas.isEmpty())
        assertEquals("No se pudieron descartar los puntos", resultado.exceptionOrNull()?.message)
    }

    // Verificar que los puntos se aislan por usuario y se conservan para el mismo usuario
    @Test
    fun `los puntos se aislan por usuario y se conservan para el mismo usuario`() = runTest {
        val repositorio = crearRepositorio()
        repositorio.guardar(crearPuntoCapturadoDePrueba(id = "a", idUsuario = "ana"))

        assertEquals(1, repositorio.observarPendientes("ana", "sendero-1").first().size)
        assertEquals(0, repositorio.observarPendientes("beto", "sendero-1").first().size)
    }

    // Verificar que incorporar inserta el punto en el tramo cercano con tipo, nombre y foto, y guarda el sendero como pendiente
    @Test
    fun `incorporar inserta el punto en el tramo cercano con tipo nombre y foto`() = runTest {
        guardarSendero()
        guardarCapturado("p1", "Mirador al lago", foto = "/fotos/a.jpg")
        puntoDao.entidades.value = puntoDao.entidades.value.map { entidad -> entidad.copy(tipo = "MIRADOR") }

        val resultado = crearRepositorio().incorporarAlSendero("usuario-1", "sendero-1", setOf("p1"))

        assertTrue(resultado.isSuccess)
        val guardado = senderoDao.entidades.value.single()
        assertEquals(listOf("A", "B", "Mirador al lago", "C"), nombresGuardados())
        assertEquals(
            PuntoRecorridoEntidad(LATITUD_BASE - 0.006, LONGITUD_BASE + 0.0001, "MIRADOR", "Mirador al lago", "/fotos/a.jpg"),
            guardado.recorrido[2]
        )
        assertEquals("PENDIENTE", guardado.estadoSincronizacion)
        assertEquals(AHORA, guardado.fechaModificacion)
        assertEquals(listOf("usuario-1"), planificador.solicitudes)
    }

    // Verificar que incorporar borra solo los puntos elegidos y no sus archivos de foto
    @Test
    fun `incorporar borra solo los elegidos y conserva sus fotos`() = runTest {
        guardarSendero()
        guardarCapturado("p1", "uno", foto = "/fotos/a.jpg")
        guardarCapturado("p2", "dos", gradosAlSur = 0.007)

        crearRepositorio().incorporarAlSendero("usuario-1", "sendero-1", setOf("p1"))

        assertEquals(listOf("p2"), puntoDao.entidades.value.map { entidad -> entidad.id })
        assertTrue(almacenFotos.rutasBorradas.isEmpty())
    }

    // Verificar que varios puntos del mismo tramo quedan ordenados a lo largo del tramo
    @Test
    fun `incorporar varios puntos del mismo tramo los ordena a lo largo del tramo`() = runTest {
        guardarSendero()
        guardarCapturado("p1", "lejos", gradosAlSur = 0.007, instante = 1_000L)
        guardarCapturado("p2", "cerca", gradosAlSur = 0.005, instante = 2_000L)

        crearRepositorio().incorporarAlSendero("usuario-1", "sendero-1", setOf("p1", "p2"))

        assertEquals(listOf("A", "B", "cerca", "lejos", "C"), nombresGuardados())
    }

    // Verificar que un circular recibe un punto cercano al cierre al final del recorrido
    @Test
    fun `incorporar en un circular agrega al final un punto cercano al cierre`() = runTest {
        val cuadrado = listOf(
            PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE, nombre = "P0"),
            PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE + 0.01, nombre = "P1"),
            PuntoRecorrido(LATITUD_BASE - 0.01, LONGITUD_BASE + 0.01, nombre = "P2"),
            PuntoRecorrido(LATITUD_BASE - 0.01, LONGITUD_BASE, nombre = "P3")
        )
        guardarSendero(cuadrado, TipoRecorrido.CIRCULAR)
        guardarCapturado("p1", "cierre", gradosAlSur = 0.005, gradosAlEste = -0.0001)

        crearRepositorio().incorporarAlSendero("usuario-1", "sendero-1", setOf("p1"))

        assertEquals(listOf("P0", "P1", "P2", "P3", "cierre"), nombresGuardados())
    }

    // Verificar que un sendero sin recorrido recibe los puntos en orden de captura
    @Test
    fun `incorporar en un sendero sin recorrido agrega los puntos en orden de captura`() = runTest {
        guardarSendero(recorrido = emptyList())
        guardarCapturado("p2", "segundo", instante = 2_000L)
        guardarCapturado("p1", "primero", instante = 1_000L)

        crearRepositorio().incorporarAlSendero("usuario-1", "sendero-1", setOf("p1", "p2"))

        assertEquals(listOf("primero", "segundo"), nombresGuardados())
    }

    // Verificar que la nota completa se conserva como nombre, solo recortada de espacios
    @Test
    fun `incorporar conserva la nota completa recortada de espacios`() = runTest {
        guardarSendero()
        val notaLarga = "a".repeat(150)
        guardarCapturado("p1", "  $notaLarga  ")

        crearRepositorio().incorporarAlSendero("usuario-1", "sendero-1", setOf("p1"))

        assertEquals(notaLarga, senderoDao.entidades.value.single().recorrido[2].nombre)
    }

    // Verificar que si la distancia coincidía con la calculada del mapa se recalcula con los puntos nuevos
    @Test
    fun `incorporar recalcula la distancia si coincidia con la calculada del mapa`() = runTest {
        val calculadaAntes = Math.round(CalculadoraRecorrido.distanciaKilometros(recorridoBase, TipoRecorrido.LINEAL) * 10) / 10.0
        guardarSendero(distanciaKilometros = calculadaAntes)
        guardarCapturado("p1", "desvio", gradosAlEste = 0.01)

        crearRepositorio().incorporarAlSendero("usuario-1", "sendero-1", setOf("p1"))

        assertTrue(senderoDao.entidades.value.single().distanciaKilometros > calculadaAntes)
    }

    // Verificar que si la distancia fue escrita a mano no se recalcula
    @Test
    fun `incorporar no recalcula la distancia escrita a mano`() = runTest {
        guardarSendero(distanciaKilometros = 12.0)
        guardarCapturado("p1", "desvio", gradosAlEste = 0.01)

        crearRepositorio().incorporarAlSendero("usuario-1", "sendero-1", setOf("p1"))

        assertEquals(12.0, senderoDao.entidades.value.single().distanciaKilometros, 0.0)
    }

    // Verificar que es todo o nada: si falla el borrado de los capturados, el sendero queda sin modificar
    @Test
    fun `incorporar es atomico y si falla el borrado el sendero no cambia`() = runTest {
        guardarSendero()
        guardarCapturado("p1", "uno")
        val senderoAntes = senderoDao.entidades.value
        puntoDao.errorAlBorrar = IllegalStateException("SQLITE_BUSY")

        val resultado = crearRepositorio().incorporarAlSendero("usuario-1", "sendero-1", setOf("p1"))

        assertTrue(resultado.isFailure)
        assertEquals(senderoAntes, senderoDao.entidades.value)
        assertEquals(1, puntoDao.entidades.value.size)
        assertTrue(planificador.solicitudes.isEmpty())
        assertEquals("No se pudieron incorporar los puntos al sendero", resultado.exceptionOrNull()?.message)
    }

    // Verificar que un sendero inexistente falla con mensaje en español sin tocar nada
    @Test
    fun `incorporar en un sendero inexistente falla sin tocar nada`() = runTest {
        guardarCapturado("p1", "uno")

        val resultado = crearRepositorio().incorporarAlSendero("usuario-1", "no-existe", setOf("p1"))

        assertTrue(resultado.isFailure)
        assertEquals("No se encontró el sendero", resultado.exceptionOrNull()?.message)
        assertEquals(1, puntoDao.entidades.value.size)
    }

    // Verificar que una selección vacía no modifica nada ni pide sincronizar
    @Test
    fun `incorporar sin puntos elegidos no modifica nada`() = runTest {
        guardarSendero()
        guardarCapturado("p1", "uno")
        val senderoAntes = senderoDao.entidades.value

        val resultado = crearRepositorio().incorporarAlSendero("usuario-1", "sendero-1", emptySet())

        assertTrue(resultado.isSuccess)
        assertEquals(senderoAntes, senderoDao.entidades.value)
        assertTrue(planificador.solicitudes.isEmpty())
    }

    // Verificar que la incorporación se completa aunque se cancele la corrutina de la pantalla
    @Test
    fun `incorporar no se cancela con la pantalla`() = runTest {
        guardarSendero()
        guardarCapturado("p1", "uno")
        val repositorio = crearRepositorio(backgroundScope + SupervisorJob() + StandardTestDispatcher(testScheduler))

        val trabajoDePantalla = launch(start = CoroutineStart.UNDISPATCHED) {
            repositorio.incorporarAlSendero("usuario-1", "sendero-1", setOf("p1"))
        }
        trabajoDePantalla.cancel()
        advanceUntilIdle()

        assertEquals(listOf("A", "B", "uno", "C"), nombresGuardados())
        assertTrue(puntoDao.entidades.value.isEmpty())
    }
}
