package com.unaj.senderos.core.data.sincronizacion

import com.unaj.senderos.core.database.sendero.PuntoRecorridoEntidad
import com.unaj.senderos.core.network.fotos.SubidorFotos
import com.unaj.senderos.core.data.falsos.FalsoEjecutorTransacciones
import com.unaj.senderos.core.data.falsos.FalsoSenderoDao
import com.unaj.senderos.core.data.falsos.FalsoSenderoFuenteRemota
import com.unaj.senderos.core.data.falsos.FalsoSubidorFotos
import com.unaj.senderos.core.data.falsos.crearEntidadDePrueba
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SincronizadorFotosPuntosTest {

    private val idUsuario = "usuario-1"
    private val ahora = 1_000_000L
    private val fuenteRemota = FalsoSenderoFuenteRemota()
    private val subidor = FalsoSubidorFotos()

    private fun punto(nombre: String, foto: String): PuntoRecorridoEntidad {
        return PuntoRecorridoEntidad(-39.95, -71.07, "MIRADOR", nombre, foto)
    }

    private fun crearSincronizador(
        dao: FalsoSenderoDao,
        subidorFotos: SubidorFotos = subidor,
        existeArchivo: (String) -> Boolean = { true }
    ): SincronizadorSenderos {
        return SincronizadorSenderos(dao, fuenteRemota, FalsoEjecutorTransacciones(), subidorFotos, existeArchivo) { ahora }
    }

    @Test
    fun `las fotos locales de los puntos se suben y se reemplazan por la URL`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(recorrido = listOf(punto("uno", "/fotos/a.jpg"), punto("dos", "/fotos/b.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))

        crearSincronizador(dao).sincronizar(idUsuario)

        assertEquals(listOf("/fotos/a.jpg", "/fotos/b.jpg"), subidor.rutasSubidas)
        val guardada = dao.entidades.value.single()
        assertEquals(
            listOf("https://imagenes.ejemplo//fotos/a.jpg", "https://imagenes.ejemplo//fotos/b.jpg"),
            guardada.recorrido.map { puntoGuardado -> puntoGuardado.foto }
        )
        assertEquals("SINCRONIZADO", guardada.estadoSincronizacion)
    }

    @Test
    fun `el documento remoto lleva las URL de las fotos de los puntos`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(recorrido = listOf(punto("uno", "/fotos/a.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))

        crearSincronizador(dao).sincronizar(idUsuario)

        val subido = fuenteRemota.senderosGuardados.single().second
        assertEquals("https://imagenes.ejemplo//fotos/a.jpg", subido.recorrido.single().foto)
    }

    @Test
    fun `los puntos sin foto o con URL no se suben`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L).copy(
            recorrido = listOf(punto("sin", ""), punto("remota", "https://imagenes.ejemplo/x.jpg"))
        )
        val dao = FalsoSenderoDao(listOf(entidad))

        crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(subidor.rutasSubidas.isEmpty())
        assertEquals("SINCRONIZADO", dao.entidades.value.single().estadoSincronizacion)
    }

    @Test
    fun `si falla la foto de un punto el sendero se sube igual sin esa foto y queda pendiente`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(recorrido = listOf(punto("uno", "/fotos/a.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))
        val subidorQueFalla = object : SubidorFotos {
            override suspend fun subir(rutaLocal: String): String = throw IllegalStateException("Cloudinary rechazó la foto")
        }

        val resumen = crearSincronizador(dao, subidorQueFalla).sincronizar(idUsuario).getOrThrow()

        assertEquals(0, resumen.senderosConError)
        assertEquals("", fuenteRemota.senderosGuardados.single().second.recorrido.single().foto)
        val local = dao.entidades.value.single()
        assertEquals("PENDIENTE", local.estadoSincronizacion)
        assertEquals("/fotos/a.jpg", local.recorrido.single().foto)
    }

    @Test
    fun `si falla la foto principal el sendero se sube igual sin foto y la conserva local`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L).copy(foto = "/fotos/portada.jpg")
        val dao = FalsoSenderoDao(listOf(entidad))
        val subidorQueFalla = object : SubidorFotos {
            override suspend fun subir(rutaLocal: String): String = throw IllegalStateException("Cloudinary no está configurado")
        }

        val resumen = crearSincronizador(dao, subidorQueFalla).sincronizar(idUsuario).getOrThrow()

        assertEquals(0, resumen.senderosConError)
        assertEquals("", fuenteRemota.senderosGuardados.single().second.foto)
        val local = dao.entidades.value.single()
        assertEquals("PENDIENTE", local.estadoSincronizacion)
        assertEquals("/fotos/portada.jpg", local.foto)
    }

    @Test
    fun `lo subido antes de un fallo queda guardado como URL`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(recorrido = listOf(punto("uno", "/fotos/a.jpg"), punto("dos", "/fotos/b.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))
        val subidorQueFallaEnLaSegunda = object : SubidorFotos {
            override suspend fun subir(rutaLocal: String): String {
                if (rutaLocal.endsWith("b.jpg")) throw IllegalStateException("sin red")
                return "https://imagenes.ejemplo/subida.jpg"
            }
        }

        crearSincronizador(dao, subidorQueFallaEnLaSegunda).sincronizar(idUsuario)

        val fotos = dao.entidades.value.single().recorrido.map { puntoGuardado -> puntoGuardado.foto }
        assertEquals(listOf("https://imagenes.ejemplo/subida.jpg", "/fotos/b.jpg"), fotos)
    }

    @Test
    fun `se sube la foto principal y las de los puntos`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(foto = "/fotos/principal.jpg", recorrido = listOf(punto("uno", "/fotos/a.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))

        crearSincronizador(dao).sincronizar(idUsuario)

        assertEquals(listOf("/fotos/principal.jpg", "/fotos/a.jpg"), subidor.rutasSubidas)
    }

    @Test
    fun `un sendero eliminado no sube las fotos de sus puntos`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L, estaEliminado = true)
            .copy(recorrido = listOf(punto("uno", "/fotos/a.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))

        crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(subidor.rutasSubidas.isEmpty())
    }

    @Test
    fun `si falta el archivo de la foto de un punto se limpia y el sendero se sincroniza`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(recorrido = listOf(punto("falta", "/fotos/falta.jpg"), punto("esta", "/fotos/esta.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))

        val resumen = crearSincronizador(dao, existeArchivo = { ruta -> ruta != "/fotos/falta.jpg" }).sincronizar(idUsuario).getOrThrow()

        assertEquals(0, resumen.senderosConError)
        assertEquals(listOf("/fotos/esta.jpg"), subidor.rutasSubidas)
        val guardada = dao.entidades.value.single()
        assertEquals(listOf("", "https://imagenes.ejemplo//fotos/esta.jpg"), guardada.recorrido.map { puntoGuardado -> puntoGuardado.foto })
        assertEquals("SINCRONIZADO", guardada.estadoSincronizacion)
        assertEquals("", fuenteRemota.senderosGuardados.single().second.recorrido.first().foto)
    }

    @Test
    fun `la URL de una foto no pisa una edicion hecha durante la subida`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(recorrido = listOf(punto("original", "/fotos/a.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))
        val subidorQueEdita = object : SubidorFotos {
            override suspend fun subir(rutaLocal: String): String {
                dao.entidades.update { lista ->
                    lista.map { existente ->
                        existente.copy(fechaModificacion = existente.fechaModificacion + 1, recorrido = listOf(punto("editado", "")))
                    }
                }
                return "https://imagenes.ejemplo/subida.jpg"
            }
        }

        crearSincronizador(dao, subidorQueEdita).sincronizar(idUsuario)

        val guardada = dao.entidades.value.single()
        assertEquals(listOf("editado"), guardada.recorrido.map { puntoGuardado -> puntoGuardado.nombre })
        assertEquals(listOf(""), guardada.recorrido.map { puntoGuardado -> puntoGuardado.foto })
        assertEquals(ahora - 999L, guardada.fechaModificacion)
    }

    @Test
    fun `un sendero con error previo por la foto pasa a pendiente cuando sube sin ella`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L).copy(foto = "/fotos/portada.jpg", estadoSincronizacion = "CON_ERROR")
        val dao = FalsoSenderoDao(listOf(entidad))
        val subidorQueFalla = object : SubidorFotos {
            override suspend fun subir(rutaLocal: String): String = throw IllegalStateException("Cloudinary no está configurado")
        }

        crearSincronizador(dao, subidorQueFalla).sincronizar(idUsuario).getOrThrow()

        assertEquals("PENDIENTE", dao.entidades.value.single().estadoSincronizacion)
    }
}
