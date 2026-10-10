package com.unaj.senderos.data.sincronizacion

import com.unaj.senderos.data.local.PuntoRecorridoEntidad
import com.unaj.senderos.data.remote.PuntoRecorridoRed
import com.unaj.senderos.falsos.FalsoEjecutorTransacciones
import com.unaj.senderos.falsos.FalsoSenderoDao
import com.unaj.senderos.falsos.FalsoSenderoFuenteRemota
import com.unaj.senderos.falsos.crearEntidadDePrueba
import com.unaj.senderos.falsos.crearRedDePrueba
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SincronizadorSenderosTest {

    private val instanteFijo = 90_000L
    private val idUsuario = "usuario-1"
    private val falsoDao = FalsoSenderoDao()
    private val falsaFuenteRemota = FalsoSenderoFuenteRemota()
    private val falsoEjecutor = FalsoEjecutorTransacciones()

    // Crear el sincronizador con el reloj fijo y los falsos de fuente local y remota
    private fun crearSincronizador(): SincronizadorSenderos {
        return SincronizadorSenderos(
            senderoDao = falsoDao,
            fuenteRemota = falsaFuenteRemota,
            ejecutorTransacciones = falsoEjecutor,
            reloj = { instanteFijo }
        )
    }

    // Probar que el primer ingreso siembra los dos ejemplos, los sube y registra la marca
    @Test
    fun `primer ingreso siembra dos ejemplos los sube y registra la marca`() = runTest {
        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        val locales = falsoDao.entidades.value
        assertEquals(2, locales.size)
        assertTrue(locales.all { entidad -> entidad.estadoSincronizacion == "SINCRONIZADO" })
        assertEquals(
            setOf("Huella Andina", "Huella Neuquina"),
            locales.map { entidad -> entidad.nombre }.toSet()
        )
        assertEquals(2, falsaFuenteRemota.senderosGuardados.size)
        assertEquals(2, falsaFuenteRemota.documentosPorUsuario[idUsuario]?.size)
        assertEquals(listOf(idUsuario), falsaFuenteRemota.usuariosConMarcaRegistrada)
    }

    // Probar que con la marca registrada no se vuelven a sembrar los ejemplos
    @Test
    fun `con marca registrada no siembra ejemplos`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertTrue(falsoDao.entidades.value.isEmpty())
        assertTrue(falsaFuenteRemota.senderosGuardados.isEmpty())
    }

    // Probar que un usuario que borró todo y ya tiene la marca no recibe ejemplos nuevamente
    @Test
    fun `usuario que borro todo con marca no vuelve a sembrar`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "a", fechaModificacion = 2_000L, eliminado = true))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertTrue(falsoDao.entidades.value.isEmpty())
    }

    // Probar que no se siembra si ya hay senderos remotos aunque no exista la marca
    @Test
    fun `no siembra si hay senderos remotos aunque falte la marca`() = runTest {
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "remoto"))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertEquals(listOf("remoto"), falsoDao.entidades.value.map { entidad -> entidad.id })
        assertEquals(listOf(idUsuario), falsaFuenteRemota.usuariosConMarcaRegistrada)
    }

    // Probar que no se siembra si ya hay senderos locales aunque no exista la marca ni datos remotos
    @Test
    fun `no siembra si hay senderos locales aunque falte la marca`() = runTest {
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "local", estadoSincronizacion = "SINCRONIZADO"))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertEquals(listOf("local"), falsoDao.entidades.value.map { entidad -> entidad.id })
    }

    // Probar que los pendientes se suben y pasan a SINCRONIZADO
    @Test
    fun `pendientes se suben y pasan a SINCRONIZADO`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que un eliminado pendiente se sube con eliminado verdadero y se borra definitivamente del local
    @Test
    fun `eliminado pendiente se sube como eliminado y se borra del local`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", estadoSincronizacion = "PENDIENTE", estaEliminado = true)
        )

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertTrue(falsaFuenteRemota.senderosGuardados.single().second.eliminado)
        assertNull(falsoDao.obtenerPorId(idUsuario, "a"))
    }

    // Probar que el remoto más nuevo pisa al local sincronizado
    @Test
    fun `remoto mas nuevo pisa al local`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que el local más nuevo no es pisado por el remoto y se sube
    @Test
    fun `local mas nuevo no es pisado y se sube`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
        falsoDao.entidades.value = listOf(
            crearEntidadDePrueba(id = "a", nombre = "Local", fechaModificacion = 3_000L, estadoSincronizacion = "PENDIENTE")
        )
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "a", nombre = "Remoto", fechaModificacion = 2_000L))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertEquals("Local", falsoDao.obtenerPorId(idUsuario, "a")?.nombre)
        assertEquals("SINCRONIZADO", falsoDao.obtenerPorId(idUsuario, "a")?.estadoSincronizacion)
        assertEquals("Local", falsaFuenteRemota.documentosPorUsuario[idUsuario]?.get("a")?.nombre)
    }

    // Probar que un remoto eliminado más nuevo borra definitivamente el local
    @Test
    fun `remoto eliminado mas nuevo borra el local`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a", fechaModificacion = 1_000L))
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "a", fechaModificacion = 2_000L, eliminado = true))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertNull(falsoDao.obtenerPorId(idUsuario, "a"))
    }

    // Probar que un remoto nuevo que no existe en local se guarda como SINCRONIZADO
    @Test
    fun `remoto inexistente en local se guarda SINCRONIZADO`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "b", nombre = "Beta"))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        val fila = falsoDao.obtenerPorId(idUsuario, "b")
        assertEquals("Beta", fila?.nombre)
        assertEquals("SINCRONIZADO", fila?.estadoSincronizacion)
    }

    // Probar que un fallo al obtener remotos devuelve failure sin tocar el local
    @Test
    fun `fallo al obtener remotos devuelve failure sin tocar el local`() = runTest {
        falsaFuenteRemota.errorAlObtener = IllegalStateException("Sin conexión")
        val pendiente = crearEntidadDePrueba(id = "a", estadoSincronizacion = "PENDIENTE")
        falsoDao.entidades.value = listOf(pendiente)

        val resultado = crearSincronizador().sincronizar(idUsuario)

        assertTrue(resultado.isFailure)
        assertEquals(listOf(pendiente), falsoDao.entidades.value)
        assertTrue(falsaFuenteRemota.senderosGuardados.isEmpty())
        assertTrue(falsaFuenteRemota.usuariosConMarcaRegistrada.isEmpty())
    }

    // Probar que un fallo al consultar la marca devuelve failure sin sembrar nada
    @Test
    fun `fallo al consultar la marca devuelve failure sin sembrar`() = runTest {
        falsaFuenteRemota.errorAlObtenerMarca = IllegalStateException("Sin conexión")

        val resultado = crearSincronizador().sincronizar(idUsuario)

        assertTrue(resultado.isFailure)
        assertTrue(falsoDao.entidades.value.isEmpty())
    }

    // Probar que un fallo al subir un elemento lo deja CON_ERROR, los demás SINCRONIZADO y el resultado failure
    @Test
    fun `fallo al subir un elemento lo deja CON_ERROR y sincroniza los demas`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que si falla una subida en el primer ingreso no se registra la marca de ejemplos
    @Test
    fun `fallo de subida en el primer ingreso no registra la marca`() = runTest {
        falsaFuenteRemota.idsConErrorAlGuardar.add("huella-andina")

        val resultado = crearSincronizador().sincronizar(idUsuario)

        assertTrue(resultado.isFailure)
        assertTrue(falsaFuenteRemota.usuariosConMarcaRegistrada.isEmpty())
    }

    // Probar que un elemento CON_ERROR se reintenta en la siguiente sincronización
    @Test
    fun `elemento CON_ERROR se reintenta en la siguiente sincronizacion`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que las filas de otros usuarios no se suben ni se modifican
    @Test
    fun `no toca las filas de otros usuarios`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
        val ajena = crearEntidadDePrueba(id = "ajena", idUsuario = "usuario-2", estadoSincronizacion = "PENDIENTE")
        falsoDao.entidades.value = listOf(ajena)

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertEquals(listOf(ajena), falsoDao.entidades.value)
        assertTrue(falsaFuenteRemota.senderosGuardados.isEmpty())
    }

    // Probar que una edición con fecha nueva durante la subida queda PENDIENTE y conserva sus datos
    @Test
    fun `edicion durante la subida queda PENDIENTE con sus datos`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que crear y eliminar durante la subida no resucita el sendero y sube la baja
    @Test
    fun `eliminacion logica durante la subida no resucita el sendero`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que un cambio de favorito durante la subida se conserva y se sube después
    @Test
    fun `favorito durante la subida se conserva y se sube`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que una edición concurrente a un fallo de subida no se pisa con CON_ERROR y datos viejos
    @Test
    fun `edicion durante una subida fallida conserva los datos nuevos`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que un pendiente agregado durante la subida se sube en la misma sincronización
    @Test
    fun `pendiente agregado durante la subida se sube en la misma sincronizacion`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que la fase de subida se repite como máximo tres pasadas
    @Test
    fun `la subida se repite como maximo tres pasadas`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que un elemento que falló no se reintenta en las pasadas siguientes de la misma ejecución
    @Test
    fun `un elemento fallido no se reintenta dentro de la misma ejecucion`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que la fusión de cada remoto pasa por el ejecutor de transacciones
    @Test
    fun `la fusion de cada remoto pasa por el ejecutor de transacciones`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "a"))
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "b"))

        crearSincronizador().sincronizar(idUsuario).getOrThrow()

        assertEquals(2, falsoEjecutor.cantidadLlamadas)
    }

    // Probar que el resultado fallido conserva la primera excepción original ocurrida
    @Test
    fun `el failure conserva la primera excepcion original de subida`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que la cancelación al obtener los remotos se propaga y no se convierte en failure
    @Test
    fun `la cancelacion al obtener remotos se propaga`() = runTest {
        falsaFuenteRemota.errorAlObtener = CancellationException("cancelado")

        val resultado = runCatching { crearSincronizador().sincronizar(idUsuario) }

        assertTrue(resultado.exceptionOrNull() is CancellationException)
    }

    // Probar que la cancelación al subir se propaga y el elemento no se marca CON_ERROR
    @Test
    fun `la cancelacion al subir se propaga y no marca CON_ERROR`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
        falsaFuenteRemota.erroresPorIdAlGuardar["a"] = CancellationException("cancelado")
        falsoDao.entidades.value = listOf(crearEntidadDePrueba(id = "a", estadoSincronizacion = "PENDIENTE"))

        val resultado = runCatching { crearSincronizador().sincronizar(idUsuario) }

        assertTrue(resultado.exceptionOrNull() is CancellationException)
        assertEquals("PENDIENTE", falsoDao.obtenerPorId(idUsuario, "a")?.estadoSincronizacion)
        assertTrue(falsaFuenteRemota.usuariosConMarcaRegistrada.isEmpty())
    }

    // Probar que la cancelación durante la fusión local se propaga
    @Test
    fun `la cancelacion durante la fusion se propaga`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
        falsaFuenteRemota.precargar(idUsuario, crearRedDePrueba(id = "a"))
        falsoDao.errorAlGuardar = CancellationException("cancelado")

        val resultado = runCatching { crearSincronizador().sincronizar(idUsuario) }

        assertTrue(resultado.exceptionOrNull() is CancellationException)
    }

    // Probar que un sendero pendiente con recorrido sube todos sus puntos en el mismo orden
    @Test
    fun `sendero pendiente con recorrido sube los puntos sin perderlos`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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

    // Probar que un documento remoto con recorrido baja a la base local con todos sus puntos
    @Test
    fun `documento remoto con recorrido baja los puntos sin perderlos`() = runTest {
        falsaFuenteRemota.usuariosConMarcaEjemplos.add(idUsuario)
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
}
