package com.unaj.senderos.data.sincronizacion

import com.unaj.senderos.data.local.PuntoRecorridoEntidad
import com.unaj.senderos.data.remote.PuntoRecorridoRed
import com.unaj.senderos.falsos.FalsoEjecutorTransacciones
import com.unaj.senderos.falsos.FalsoSenderoDao
import com.unaj.senderos.falsos.FalsoSenderoFuenteRemota
import com.unaj.senderos.falsos.FalsoSubidorFotos
import com.unaj.senderos.falsos.crearEntidadDePrueba
import com.unaj.senderos.falsos.crearRedDePrueba
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SincronizadorSenderosTest {

    private val idUsuario = "usuario-1"
    private val falsoDao = FalsoSenderoDao()
    private val falsaFuenteRemota = FalsoSenderoFuenteRemota()
    private val falsoEjecutor = FalsoEjecutorTransacciones()
    private val falsoSubidorFotos = FalsoSubidorFotos()

    private fun crearSincronizador(): SincronizadorSenderos {
        return SincronizadorSenderos(
            senderoDao = falsoDao,
            fuenteRemota = falsaFuenteRemota,
            ejecutorTransacciones = falsoEjecutor,
            subidorFotos = falsoSubidorFotos
        )
    }

    @Test
    fun `un remoto eliminado sin copia local no se crea`() = runTest {
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "a", fechaModificacion = 2_000L, eliminado = true))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertTrue(falsoDao.entidades.value.isEmpty())
    }

    @Test
    fun `pendientes se suben y pasan a SINCRONIZADO`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", nombre = "Alfa", estadoSincronizacion = "PENDIENTE", fechaModificacion = 5_000L)
        )

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertEquals("SINCRONIZADO", falsoDao.entidades.value.single().estadoSincronizacion)
        val subido = falsaFuenteRemota.senderosGuardados.single().second
        assertEquals("a", subido.id)
        assertEquals("Alfa", subido.nombre)
        assertEquals(5_000L, subido.fechaModificacion)
    }

    @Test
    fun `eliminado pendiente se sube como eliminado y se borra del local`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", estadoSincronizacion = "PENDIENTE", estaEliminado = true)
        )

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertTrue(falsaFuenteRemota.senderosGuardados.single().second.eliminado)
        assertNull(falsoDao.obtenerPorId(idUsuario, "a"))
    }

    @Test
    fun `remoto mas nuevo pisa al local`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", nombre = "Local", fechaModificacion = 1_000L)
        )
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "a", nombre = "Remoto", fechaModificacion = 2_000L))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        val fila = falsoDao.obtenerPorId(idUsuario, "a")
        assertEquals("Remoto", fila?.nombre)
        assertEquals(2_000L, fila?.fechaModificacion)
        assertEquals("SINCRONIZADO", fila?.estadoSincronizacion)
    }

    @Test
    fun `local mas nuevo no es pisado y se sube`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", nombre = "Local", fechaModificacion = 3_000L, estadoSincronizacion = "PENDIENTE")
        )
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "a", nombre = "Remoto", fechaModificacion = 2_000L))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertEquals("Local", falsoDao.obtenerPorId(idUsuario, "a")?.nombre)
        assertEquals("SINCRONIZADO", falsoDao.obtenerPorId(idUsuario, "a")?.estadoSincronizacion)
        assertEquals("Local", falsaFuenteRemota.documentosPorUsuario[idUsuario]?.get("a")?.nombre)
    }

    @Test
    fun `remoto eliminado mas nuevo borra el local`() = runTest {
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a", fechaModificacion = 1_000L))
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "a", fechaModificacion = 2_000L, eliminado = true))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertNull(falsoDao.obtenerPorId(idUsuario, "a"))
    }

    @Test
    fun `remoto inexistente en local se guarda SINCRONIZADO`() = runTest {
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "b", nombre = "Beta"))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        val fila = falsoDao.obtenerPorId(idUsuario, "b")
        assertEquals("Beta", fila?.nombre)
        assertEquals("SINCRONIZADO", fila?.estadoSincronizacion)
    }

    @Test
    fun `fallo al obtener remotos devuelve failure sin tocar el local`() = runTest {
        falsaFuenteRemota.errorAlObtener = IllegalStateException("Sin conexión")
        val pendiente = crearEntidadDePrueba(id = "a", estadoSincronizacion = "PENDIENTE")
        falsoDao.entidades.value = listOf(pendiente)

        val resultado = crearSincronizador().sincronizar(idUsuario)

        assertTrue(resultado.isFailure)
        assertEquals(listOf(pendiente), falsoDao.entidades.value)
        assertTrue(falsaFuenteRemota.senderosGuardados.isEmpty())
    }

    @Test
    fun `fallo al subir un elemento lo deja CON_ERROR y sincroniza los demas`() = runTest {
        falsaFuenteRemota.idsConErrorAlGuardar.add("malo")
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "bueno-1", estadoSincronizacion = "PENDIENTE"),
            crearEntidadDePrueba(id = "malo", estadoSincronizacion = "PENDIENTE"),
            crearEntidadDePrueba(id = "bueno-2", estadoSincronizacion = "PENDIENTE")
        )

        val resultado = crearSincronizador().sincronizar(idUsuario)

        assertTrue(resultado.isFailure)
        assertEquals("CON_ERROR", falsoDao.obtenerPorId(idUsuario, "malo")?.estadoSincronizacion)
        assertEquals("SINCRONIZADO", falsoDao.obtenerPorId(idUsuario, "bueno-1")?.estadoSincronizacion)
        assertEquals("SINCRONIZADO", falsoDao.obtenerPorId(idUsuario, "bueno-2")?.estadoSincronizacion)
    }

    @Test
    fun `elemento CON_ERROR se reintenta en la siguiente sincronizacion`() = runTest {
        falsaFuenteRemota.idsConErrorAlGuardar.add("a")
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a", estadoSincronizacion = "PENDIENTE"))
        val sincronizador = crearSincronizador()

        val primerResultado = sincronizador.sincronizar(idUsuario)
        assertTrue(primerResultado.isFailure)
        assertEquals("CON_ERROR", falsoDao.obtenerPorId(idUsuario, "a")?.estadoSincronizacion)

        falsaFuenteRemota.idsConErrorAlGuardar.clear()
        sincronizador.sincronizar(idUsuario).getOrThrow()

        assertEquals("SINCRONIZADO", falsoDao.obtenerPorId(idUsuario, "a")?.estadoSincronizacion)
        assertEquals("a", falsaFuenteRemota.documentosPorUsuario[idUsuario]?.get("a")?.id)
    }

    @Test
    fun `no toca las filas de otros usuarios`() = runTest {
        val ajena = crearEntidadDePrueba(id = "ajena", idUsuario = "usuario-2", estadoSincronizacion = "PENDIENTE")
        falsoDao.entidades.value = listOf(ajena)

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertEquals(listOf(ajena), falsoDao.entidades.value)
        assertTrue(falsaFuenteRemota.senderosGuardados.isEmpty())
    }

    @Test
    fun `edicion durante la subida queda PENDIENTE con sus datos`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", nombre = "Alfa", fechaModificacion = 5_000L, estadoSincronizacion = "PENDIENTE")
        )
        var edicionesRealizadas = 0
        falsaFuenteRemota.alGuardarSendero = {
            edicionesRealizadas++
            falsoDao.guardar(
                crearEntidadDePrueba(
                    id = "a",
                    nombre = "Editado",
                    fechaModificacion = 9_000L + edicionesRealizadas,
                    estadoSincronizacion = "PENDIENTE"
                )
            )
        }

        crearSincronizador().sincronizar(idUsuario)

        val fila = falsoDao.obtenerPorId(idUsuario, "a")
        assertEquals("Editado", fila?.nombre)
        assertEquals("PENDIENTE", fila?.estadoSincronizacion)
    }

    @Test
    fun `eliminacion logica durante la subida no resucita el sendero`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", fechaModificacion = 5_000L, estadoSincronizacion = "PENDIENTE")
        )
        var yaEliminado = false
        falsaFuenteRemota.alGuardarSendero = {
            if (!yaEliminado) {
                yaEliminado = true
                falsoDao.guardar(
                    crearEntidadDePrueba(
                        id = "a",
                        fechaModificacion = 9_000L,
                        estadoSincronizacion = "PENDIENTE",
                        estaEliminado = true
                    )
                )
            }
        }

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertNull(falsoDao.obtenerPorId(idUsuario, "a"))
        assertTrue(falsaFuenteRemota.documentosPorUsuario[idUsuario]?.get("a")?.eliminado == true)
    }

    @Test
    fun `favorito durante la subida se conserva y se sube`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", fechaModificacion = 5_000L, estadoSincronizacion = "PENDIENTE")
        )
        var yaCambiado = false
        falsaFuenteRemota.alGuardarSendero = {
            if (!yaCambiado) {
                yaCambiado = true
                falsoDao.guardar(
                    crearEntidadDePrueba(
                        id = "a",
                        esFavorito = true,
                        fechaModificacion = 9_000L,
                        estadoSincronizacion = "PENDIENTE"
                    )
                )
            }
        }

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertTrue(falsoDao.obtenerPorId(idUsuario, "a")?.esFavorito == true)
        assertTrue(falsaFuenteRemota.documentosPorUsuario[idUsuario]?.get("a")?.esFavorito == true)
        assertEquals("SINCRONIZADO", falsoDao.obtenerPorId(idUsuario, "a")?.estadoSincronizacion)
    }

    @Test
    fun `edicion durante una subida fallida conserva los datos nuevos`() = runTest {
        falsaFuenteRemota.erroresPorIdAlGuardar["a"] = IllegalStateException("Sin red")
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", nombre = "Alfa", fechaModificacion = 5_000L, estadoSincronizacion = "PENDIENTE")
        )
        falsaFuenteRemota.alGuardarSendero = {
            falsoDao.guardar(
                crearEntidadDePrueba(
                    id = "a",
                    nombre = "Editado",
                    fechaModificacion = 9_000L,
                    estadoSincronizacion = "PENDIENTE"
                )
            )
        }

        val resultado = crearSincronizador().sincronizar(idUsuario)

        assertTrue(resultado.isFailure)
        val fila = falsoDao.obtenerPorId(idUsuario, "a")
        assertEquals("Editado", fila?.nombre)
        assertEquals("PENDIENTE", fila?.estadoSincronizacion)
    }

    @Test
    fun `pendiente agregado durante la subida se sube en la misma sincronizacion`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", fechaModificacion = 5_000L, estadoSincronizacion = "PENDIENTE")
        )
        var yaAgregado = false
        falsaFuenteRemota.alGuardarSendero = {
            if (!yaAgregado) {
                yaAgregado = true
                falsoDao.guardar(
                    crearEntidadDePrueba(id = "b", fechaModificacion = 9_000L, estadoSincronizacion = "PENDIENTE")
                )
            }
        }

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertEquals("SINCRONIZADO", falsoDao.obtenerPorId(idUsuario, "b")?.estadoSincronizacion)
        assertEquals(setOf("a", "b"), falsaFuenteRemota.documentosPorUsuario[idUsuario]?.keys)
    }

    @Test
    fun `la subida se repite como maximo tres pasadas`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", fechaModificacion = 5_000L, estadoSincronizacion = "PENDIENTE")
        )
        var contador = 0
        falsaFuenteRemota.alGuardarSendero = {
            contador++
            falsoDao.guardar(
                crearEntidadDePrueba(id = "nuevo-$contador", fechaModificacion = 9_000L, estadoSincronizacion = "PENDIENTE")
            )
        }

        crearSincronizador().sincronizar(idUsuario)

        assertEquals(3, falsaFuenteRemota.senderosGuardados.size)
        assertEquals("PENDIENTE", falsoDao.obtenerPorId(idUsuario, "nuevo-3")?.estadoSincronizacion)
    }

    @Test
    fun `un elemento fallido no se reintenta dentro de la misma ejecucion`() = runTest {
        falsaFuenteRemota.erroresPorIdAlGuardar["malo"] = IllegalStateException("Sin red")
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "malo", estadoSincronizacion = "PENDIENTE"),
            crearEntidadDePrueba(id = "bueno", estadoSincronizacion = "PENDIENTE")
        )
        var yaAgregado = false
        falsaFuenteRemota.alGuardarSendero = { sendero ->
            if (sendero.id == "bueno" && !yaAgregado) {
                yaAgregado = true
                falsoDao.guardar(crearEntidadDePrueba(id = "extra", estadoSincronizacion = "PENDIENTE"))
            }
        }

        crearSincronizador().sincronizar(idUsuario)

        assertEquals(1, falsaFuenteRemota.senderosGuardados.count { (_, sendero) -> sendero.id == "malo" })
        assertEquals("SINCRONIZADO", falsoDao.obtenerPorId(idUsuario, "extra")?.estadoSincronizacion)
    }

    @Test
    fun `la fusion de cada remoto pasa por el ejecutor de transacciones`() = runTest {
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "a"))
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "b"))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertEquals(2, falsoEjecutor.cantidadLlamadas)
    }

    @Test
    fun `el failure conserva la primera excepcion original de subida`() = runTest {
        val primerError = IllegalStateException("primero")
        falsaFuenteRemota.erroresPorIdAlGuardar["a"] = primerError
        falsaFuenteRemota.erroresPorIdAlGuardar["b"] = IllegalStateException("segundo")
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", estadoSincronizacion = "PENDIENTE"),
            crearEntidadDePrueba(id = "b", estadoSincronizacion = "PENDIENTE")
        )

        val resultado = crearSincronizador().sincronizar(idUsuario)

        assertSame(primerError, resultado.exceptionOrNull())
    }

    @Test
    fun `la cancelacion al obtener remotos se propaga`() = runTest {
        falsaFuenteRemota.errorAlObtener = CancellationException("cancelado")

        val resultado = runCatching { crearSincronizador().sincronizar(idUsuario) }

        assertTrue(resultado.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `la cancelacion al subir se propaga y no marca CON_ERROR`() = runTest {
        falsaFuenteRemota.erroresPorIdAlGuardar["a"] = CancellationException("cancelado")
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a", estadoSincronizacion = "PENDIENTE"))

        val resultado = runCatching { crearSincronizador().sincronizar(idUsuario) }

        assertTrue(resultado.exceptionOrNull() is CancellationException)
        assertEquals("PENDIENTE", falsoDao.obtenerPorId(idUsuario, "a")?.estadoSincronizacion)
    }

    @Test
    fun `la cancelacion durante la fusion se propaga`() = runTest {
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "a"))
        falsoDao.errorAlGuardar = CancellationException("cancelado")

        val resultado = runCatching { crearSincronizador().sincronizar(idUsuario) }

        assertTrue(resultado.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `sendero pendiente con recorrido sube los puntos sin perderlos`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(
                id = "a",
                estadoSincronizacion = "PENDIENTE",
                recorrido = listOf(
                    PuntoRecorridoEntidad(-39.2, -71.1, "REFUGIO", "Refugio"),
                    PuntoRecorridoEntidad(-39.3, -71.2, "PASO", "")
                )
            )
        )

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        val subido = falsaFuenteRemota.senderosGuardados.single().second
        assertEquals(
            listOf(PuntoRecorridoRed(-39.2, -71.1, "REFUGIO", "Refugio"), PuntoRecorridoRed(-39.3, -71.2, "PASO", "")),
            subido.recorrido
        )
    }

    @Test
    fun `documento remoto con recorrido baja los puntos sin perderlos`() = runTest {
        falsaFuenteRemota.precargar(
            idUsuario,
            crearRedDePrueba(
                id = "a",
                recorrido = listOf(
                    PuntoRecorridoRed(-39.2, -71.1, "MIRADOR", "Cumbre"),
                    PuntoRecorridoRed(-39.3, -71.2, "AGUA", "Arroyo")
                )
            )
        )

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertEquals(
            listOf(
                PuntoRecorridoEntidad(-39.2, -71.1, "MIRADOR", "Cumbre"),
                PuntoRecorridoEntidad(-39.3, -71.2, "AGUA", "Arroyo")
            ),
            falsoDao.obtenerPorId(idUsuario, "a")?.recorrido
        )
    }

    @Test
    fun `foto local se sube y el documento remoto guarda la URL`() = runTest {
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", estadoSincronizacion = "PENDIENTE").copy(foto = "/data/fotos_senderos/a.jpg")
        )

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        val urlEsperada = "https://res.cloudinary.com/prueba/a.jpg"
        assertEquals(listOf("/data/fotos_senderos/a.jpg"), falsoSubidorFotos.rutasSubidas)
        assertEquals(urlEsperada, falsaFuenteRemota.senderosGuardados.single().second.foto)
        val local = falsoDao.entidades.value.single()
        assertEquals(urlEsperada, local.foto)
        assertEquals("SINCRONIZADO", local.estadoSincronizacion)
    }

    @Test
    fun `foto remota no se vuelve a subir`() = runTest {
        val url = "https://res.cloudinary.com/prueba/a.jpg"
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a", estadoSincronizacion = "PENDIENTE").copy(foto = url))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertTrue(falsoSubidorFotos.rutasSubidas.isEmpty())
        assertEquals(url, falsaFuenteRemota.senderosGuardados.single().second.foto)
    }

    @Test
    fun `fallo al subir la foto deja el sendero CON_ERROR sin subir el documento`() = runTest {
        falsoSubidorFotos.errorAlSubir = IllegalStateException("Sin conexión con Cloudinary")
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", estadoSincronizacion = "PENDIENTE").copy(foto = "/data/fotos_senderos/a.jpg")
        )

        val resultado = crearSincronizador().sincronizar(idUsuario)

        assertTrue(resultado.isFailure)
        assertTrue(falsaFuenteRemota.senderosGuardados.isEmpty())
        val local = falsoDao.entidades.value.single()
        assertEquals("CON_ERROR", local.estadoSincronizacion)
        assertEquals("/data/fotos_senderos/a.jpg", local.foto)
    }
}
