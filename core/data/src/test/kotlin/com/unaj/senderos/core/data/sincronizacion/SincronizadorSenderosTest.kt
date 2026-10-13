package com.unaj.senderos.data.sincronizacion

import com.unaj.senderos.falsos.FalsoEjecutorTransacciones
import com.unaj.senderos.falsos.FalsoSenderoDao
import com.unaj.senderos.falsos.FalsoSenderoFuenteRemota
import com.unaj.senderos.falsos.FalsoSubidorFotos
import com.unaj.senderos.falsos.crearEntidadDePrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SincronizadorSenderosTest {

    private val idUsuario = "usuario-1"
    private val ahora = 1_000_000L
    private val fuenteRemota = FalsoSenderoFuenteRemota()

    private fun crearSincronizador(dao: FalsoSenderoDao): SincronizadorSenderos {
        return SincronizadorSenderos(dao, fuenteRemota, FalsoEjecutorTransacciones(), FalsoSubidorFotos()) { ahora }
    }

    @Test
    fun `un sendero subido poco despues de modificarse no cuenta como guardado sin conexion`() = runTest {
        val dao = FalsoSenderoDao(listOf(crearEntidadDePrueba(fechaModificacion = ahora - 10_000L)))

        val resumen = crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, resumen.senderosSubidos)
        assertEquals(0, resumen.senderosGuardadosSinConexion)
        assertEquals(0, resumen.senderosConError)
        assertNull(resumen.primerError)
    }

    @Test
    fun `un sendero subido mucho despues de modificarse cuenta como guardado sin conexion`() = runTest {
        val dao = FalsoSenderoDao(listOf(crearEntidadDePrueba(fechaModificacion = ahora - 5 * 60_000L)))

        val resumen = crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, resumen.senderosSubidos)
        assertEquals(1, resumen.senderosGuardadosSinConexion)
    }

    @Test
    fun `una demora igual al umbral cuenta como guardado sin conexion`() = runTest {
        val dao = FalsoSenderoDao(listOf(crearEntidadDePrueba(fechaModificacion = ahora - 60_000L)))

        val resumen = crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, resumen.senderosGuardadosSinConexion)
    }

    @Test
    fun `una demora menor al umbral por un milisegundo no cuenta como guardado sin conexion`() = runTest {
        val dao = FalsoSenderoDao(listOf(crearEntidadDePrueba(fechaModificacion = ahora - 59_999L)))

        val resumen = crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, resumen.senderosSubidos)
        assertEquals(0, resumen.senderosGuardadosSinConexion)
    }

    @Test
    fun `un error de subida queda en el resumen sin hacer fallar la sincronizacion`() = runTest {
        val dao = FalsoSenderoDao(listOf(crearEntidadDePrueba(id = "falla"), crearEntidadDePrueba(id = "bien")))
        fuenteRemota.idsConErrorAlGuardar.add("falla")

        val resultado = crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(resultado.isSuccess)
        val resumen = resultado.getOrThrow()
        assertEquals(1, resumen.senderosSubidos)
        assertEquals(1, resumen.senderosConError)
        assertNotNull(resumen.primerError)
    }

    @Test
    fun `un sendero eliminado no cuenta como guardado sin conexion`() = runTest {
        val dao = FalsoSenderoDao(
            listOf(crearEntidadDePrueba(fechaModificacion = ahora - 5 * 60_000L, estaEliminado = true))
        )

        val resumen = crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(0, resumen.senderosGuardadosSinConexion)
        assertEquals(0, resumen.senderosConError)
    }

    @Test
    fun `los contadores acumulan senderos recientes antiguos y con error`() = runTest {
        val dao = FalsoSenderoDao(
            listOf(
                crearEntidadDePrueba(id = "reciente", fechaModificacion = ahora - 1_000L),
                crearEntidadDePrueba(id = "antiguo-1", fechaModificacion = ahora - 120_000L),
                crearEntidadDePrueba(id = "antiguo-2", fechaModificacion = ahora - 900_000L),
                crearEntidadDePrueba(id = "falla", fechaModificacion = ahora - 900_000L)
            )
        )
        fuenteRemota.idsConErrorAlGuardar.add("falla")

        val resumen = crearSincronizador(dao).sincronizar(idUsuario).getOrThrow()

        assertEquals(3, resumen.senderosSubidos)
        assertEquals(2, resumen.senderosGuardadosSinConexion)
        assertEquals(1, resumen.senderosConError)
    }

    @Test
    fun `un fallo en la lectura remota devuelve failure`() = runTest {
        fuenteRemota.errorAlObtener = IllegalStateException("sin red")
        val dao = FalsoSenderoDao(listOf(crearEntidadDePrueba()))

        val resultado = crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(resultado.isFailure)
    }

    @Test
    fun `el umbral se mide contra el instante de inicio de la subida y no contra el final`() = runTest {
        var instanteActual = ahora
        val dao = FalsoSenderoDao(listOf(crearEntidadDePrueba(fechaModificacion = ahora - 30_000L)))
        fuenteRemota.alGuardarSendero = { instanteActual += 40_000L }
        val sincronizador = SincronizadorSenderos(
            dao, fuenteRemota, FalsoEjecutorTransacciones(), FalsoSubidorFotos()
        ) { instanteActual }

        val resumen = sincronizador.sincronizar(idUsuario).getOrThrow()

        assertEquals(1, resumen.senderosSubidos)
        assertEquals(0, resumen.senderosGuardadosSinConexion)
    }
}
