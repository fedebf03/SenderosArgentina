package com.unaj.senderos.data.sincronizacion

import com.unaj.senderos.data.local.PuntoRecorridoEntidad
import com.unaj.senderos.data.remote.SubidorFotos
import com.unaj.senderos.falsos.FalsoEjecutorTransacciones
import com.unaj.senderos.falsos.FalsoSenderoDao
import com.unaj.senderos.falsos.FalsoSenderoFuenteRemota
import com.unaj.senderos.falsos.FalsoSubidorFotos
import com.unaj.senderos.falsos.crearEntidadDePrueba
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

    // Construir un punto del recorrido con la foto indicada
    private fun punto(nombre: String, foto: String): PuntoRecorridoEntidad {
        return PuntoRecorridoEntidad(-39.95, -71.07, "MIRADOR", nombre, foto)
    }

    // Crear el sincronizador con el subidor y el verificador de archivos indicados y un reloj fijo
    private fun crearSincronizador(
        dao: FalsoSenderoDao,
        subidorFotos: SubidorFotos = subidor,
        existeArchivo: (String) -> Boolean = { true }
    ): SincronizadorSenderos {
        return SincronizadorSenderos(dao, fuenteRemota, FalsoEjecutorTransacciones(), subidorFotos, existeArchivo) { ahora }
    }

    // Verificar que las fotos locales de los puntos se suben y la entidad queda con las URL
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

    // Verificar que el documento remoto lleva las URL y no las rutas locales
    @Test
    fun `el documento remoto lleva las URL de las fotos de los puntos`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(recorrido = listOf(punto("uno", "/fotos/a.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))

        crearSincronizador(dao).sincronizar(idUsuario)

        val subido = fuenteRemota.senderosGuardados.single().second
        assertEquals("https://imagenes.ejemplo//fotos/a.jpg", subido.recorrido.single().foto)
    }

    // Verificar que los puntos sin foto o con URL no se suben
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

    // Verificar que si falla la foto de un punto el sendero queda con error y no se sube el documento
    @Test
    fun `si falla la foto de un punto el sendero queda con error`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(recorrido = listOf(punto("uno", "/fotos/a.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))
        val subidorQueFalla = object : SubidorFotos {
            // Lanzar siempre un error como si la nube rechazara la foto
            override suspend fun subir(rutaLocal: String): String = throw IllegalStateException("Cloudinary rechazó la foto")
        }

        val resumen = crearSincronizador(dao, subidorQueFalla).sincronizar(idUsuario).getOrThrow()

        assertEquals(1, resumen.senderosConError)
        assertEquals("CON_ERROR", dao.entidades.value.single().estadoSincronizacion)
        assertTrue(fuenteRemota.senderosGuardados.isEmpty())
    }

    // Verificar que lo ya subido antes del fallo queda guardado como URL para no repetir la subida
    @Test
    fun `lo subido antes de un fallo queda guardado como URL`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(recorrido = listOf(punto("uno", "/fotos/a.jpg"), punto("dos", "/fotos/b.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))
        val subidorQueFallaEnLaSegunda = object : SubidorFotos {
            // Subir la primera foto y fallar en la segunda como si se cortara la red
            override suspend fun subir(rutaLocal: String): String {
                if (rutaLocal.endsWith("b.jpg")) throw IllegalStateException("sin red")
                return "https://imagenes.ejemplo/subida.jpg"
            }
        }

        crearSincronizador(dao, subidorQueFallaEnLaSegunda).sincronizar(idUsuario)

        val fotos = dao.entidades.value.single().recorrido.map { puntoGuardado -> puntoGuardado.foto }
        assertEquals(listOf("https://imagenes.ejemplo/subida.jpg", "/fotos/b.jpg"), fotos)
    }

    // Verificar que la foto principal y las de los puntos se suben antes del documento
    @Test
    fun `se sube la foto principal y las de los puntos`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(foto = "/fotos/principal.jpg", recorrido = listOf(punto("uno", "/fotos/a.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))

        crearSincronizador(dao).sincronizar(idUsuario)

        assertEquals(listOf("/fotos/principal.jpg", "/fotos/a.jpg"), subidor.rutasSubidas)
    }

    // Verificar que un sendero eliminado lógico no sube fotos de puntos
    @Test
    fun `un sendero eliminado no sube las fotos de sus puntos`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L, estaEliminado = true)
            .copy(recorrido = listOf(punto("uno", "/fotos/a.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))

        crearSincronizador(dao).sincronizar(idUsuario)

        assertTrue(subidor.rutasSubidas.isEmpty())
    }

    // Verificar que si falta el archivo de la foto de un punto se limpia la foto y el sendero se sincroniza
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

    // Verificar que la actualización del recorrido no pisa una edición hecha durante la subida de la foto
    @Test
    fun `la URL de una foto no pisa una edicion hecha durante la subida`() = runTest {
        val entidad = crearEntidadDePrueba(fechaModificacion = ahora - 1_000L)
            .copy(recorrido = listOf(punto("original", "/fotos/a.jpg")))
        val dao = FalsoSenderoDao(listOf(entidad))
        val subidorQueEdita = object : SubidorFotos {
            // Simular que el usuario edita el sendero mientras se sube la foto
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
}
