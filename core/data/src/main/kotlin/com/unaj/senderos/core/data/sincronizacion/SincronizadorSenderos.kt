package com.unaj.senderos.core.data.sincronizacion

import android.util.Log
import com.unaj.senderos.core.database.EjecutorTransacciones
import com.unaj.senderos.core.database.sendero.SenderoDao
import com.unaj.senderos.core.database.sendero.SenderoEntidad
import com.unaj.senderos.core.data.mapeo.aEntidad
import com.unaj.senderos.core.data.mapeo.aRed
import com.unaj.senderos.core.network.sendero.SenderoFuenteRemota
import com.unaj.senderos.core.network.sendero.SenderoRed
import com.unaj.senderos.core.network.fotos.SubidorFotos
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.ResumenSincronizacion
import com.unaj.senderos.core.domain.model.UMBRAL_DEMORA_MILISEGUNDOS
import com.unaj.senderos.core.domain.model.esFotoLocal
import com.unaj.senderos.core.data.mapeo.aDominio
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.domain.port.ErrorElevacionException
import com.unaj.senderos.core.domain.port.SinConexionElevacionException
import com.unaj.senderos.core.domain.usecase.CalcularPerfilElevacionUseCase
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "SINCRONIZACION"
private const val MAXIMO_PASADAS_SUBIDA = 3
private const val MAXIMO_PERFILES_POR_PASADA = 5

// RF7 - Sincronización: Room es la fuente de verdad. Fusionamos lo remoto más nuevo y después subimos los pendientes
class SincronizadorSenderos(
    private val senderoDao: SenderoDao,
    private val fuenteRemota: SenderoFuenteRemota,
    private val ejecutorTransacciones: EjecutorTransacciones,
    private val subidorFotos: SubidorFotos,
    private val existeArchivo: (String) -> Boolean = { ruta -> File(ruta).exists() },
    private val calculadorPerfiles: CalcularPerfilElevacionUseCase? = null,
    private val reloj: () -> Long = System::currentTimeMillis
) {
    // Pedidos de perfil que el servicio rechazó para un trazado concreto. Se recuerdan mientras viva el proceso
    private val perfilesRechazados: MutableSet<String> = ConcurrentHashMap.newKeySet()

    suspend fun sincronizar(idUsuario: String): Result<ResumenSincronizacion> {
        Log.d(TAG, "Comienza la sincronización del usuario $idUsuario")
        return try {
            val resultado = sincronizarSinCapturar(idUsuario)
            if (resultado.isSuccess) {
                Log.d(TAG, "Termina la sincronización del usuario $idUsuario")
            } else {
                Log.e(TAG, "La sincronización del usuario $idUsuario terminó con fallos", resultado.exceptionOrNull())
            }
            resultado
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo la sincronización del usuario $idUsuario", error)
            Result.failure(error)
        }
    }

    private suspend fun sincronizarSinCapturar(idUsuario: String): Result<ResumenSincronizacion> {
        fusionarRemotos(idUsuario, fuenteRemota.obtenerSenderos(idUsuario))
        val idsSubidos = mutableSetOf<String>()
        val resumen = subirPendientes(idUsuario, idsSubidos)
        return Result.success(completarPerfiles(idUsuario, resumen, idsSubidos))
    }

    private suspend fun fusionarRemotos(idUsuario: String, remotos: List<SenderoRed>) {
        remotos.forEach { remoto ->
            ejecutorTransacciones.ejecutar {
                val local = senderoDao.obtenerPorId(idUsuario, remoto.id)
                if (local == null || remoto.fechaModificacion > local.fechaModificacion) {
                    if (remoto.eliminado) {
                        senderoDao.borrarDefinitivamente(idUsuario, remoto.id)
                    } else {
                        senderoDao.guardar(remoto.aEntidad(idUsuario))
                    }
                }
            }
        }
    }

    // La segunda subida solo suma los senderos que no se contaron antes y no toca el conteo de guardados sin conexión.
    // Por otro lado un fallo en este paso nunca invalida lo que ya se subió
    private suspend fun completarPerfiles(
        idUsuario: String,
        resumen: ResumenSincronizacion,
        idsSubidos: Set<String>
    ): ResumenSincronizacion {
        val calculador = calculadorPerfiles ?: return resumen
        val idsConPerfilNuevo = calcularPerfilesFaltantes(idUsuario, calculador)
        var senderosSubidos = resumen.senderosSubidos
        var senderosConError = resumen.senderosConError
        var primerError = resumen.primerError
        for (idSendero in idsConPerfilNuevo) {
            val entidad = senderoDao.obtenerPorId(idUsuario, idSendero) ?: continue
            if (entidad.estaEliminado || entidad.estadoSincronizacion != EstadoSincronizacion.PENDIENTE.name) continue
            val error = subirEntidad(idUsuario, entidad).error
            if (error != null) {
                senderosConError++
                if (primerError == null) primerError = error
            } else if (idSendero !in idsSubidos) {
                senderosSubidos++
            }
        }
        return resumen.copy(senderosSubidos = senderosSubidos, senderosConError = senderosConError, primerError = primerError)
    }

    // Se calculan hasta cinco perfiles por pasada y se corta ante falta de conexión o límite del servicio.
    // Un rechazo definitivo se recuerda para no repetir el pedido mientras viva el proceso
    private suspend fun calcularPerfilesFaltantes(idUsuario: String, calculador: CalcularPerfilElevacionUseCase): List<String> {
        val idsConPerfilNuevo = mutableListOf<String>()
        try {
            val candidatos = senderoDao.obtenerActivos(idUsuario)
                .map { entidad -> entidad.aDominio() }
                .filter { sendero -> CalculadoraPerfil.necesitaPerfil(sendero) && claveDePerfil(idUsuario, sendero) !in perfilesRechazados }
                .take(MAXIMO_PERFILES_POR_PASADA)
            for (sendero in candidatos) {
                val error = calculador.calcularPara(idUsuario, sendero).exceptionOrNull()
                if (error == null) {
                    idsConPerfilNuevo.add(sendero.id)
                    continue
                }
                Log.e(TAG, "No se pudo calcular el perfil del sendero ${sendero.id}: ${error.message}", error)
                if (error is SinConexionElevacionException) break
                if (error is ErrorElevacionException && error.esLimitacionDelServicio) break
                if (error is ErrorElevacionException && error.esRechazoDefinitivo) perfilesRechazados.add(claveDePerfil(idUsuario, sendero))
            }
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo el cálculo de perfiles del usuario $idUsuario", error)
        }
        return idsConPerfilNuevo
    }

    // La clave incluye la firma del trazado, así un trazado editado vuelve a pedir su perfil
    private fun claveDePerfil(idUsuario: String, sendero: Sendero): String {
        return "$idUsuario/${sendero.id}/${CalculadoraPerfil.firmaTrazado(sendero.recorrido, sendero.tipoRecorrido)}"
    }

    private suspend fun subirPendientes(idUsuario: String, idsSubidos: MutableSet<String>): ResumenSincronizacion {
        val instanteInicio = reloj()
        val idsFallidos = mutableSetOf<String>()
        val idsConFotosPendientes = mutableSetOf<String>()
        var primerError: Throwable? = null
        var senderosSubidos = 0
        var senderosGuardadosSinConexion = 0
        repeat(MAXIMO_PASADAS_SUBIDA) {
            val pendientes = senderoDao.obtenerPendientes(idUsuario).filter { entidad -> entidad.id !in idsFallidos && entidad.id !in idsConFotosPendientes }
            if (pendientes.isEmpty()) {
                return ResumenSincronizacion(
                    senderosSubidos, senderosGuardadosSinConexion, idsFallidos.size, primerError
                )
            }
            pendientes.forEach { entidad ->
                val resultadoSubida = subirEntidad(idUsuario, entidad)
                val error = resultadoSubida.error
                if (resultadoSubida.hayFotosSinSubir) idsConFotosPendientes.add(entidad.id)
                if (error != null) {
                    idsFallidos.add(entidad.id)
                    if (primerError == null) primerError = error
                } else if (!entidad.estaEliminado) {
                    senderosSubidos++
                    idsSubidos.add(entidad.id)
                    if (instanteInicio - entidad.fechaModificacion >= UMBRAL_DEMORA_MILISEGUNDOS) {
                        senderosGuardadosSinConexion++
                    }
                }
            }
        }
        return ResumenSincronizacion(senderosSubidos, senderosGuardadosSinConexion, idsFallidos.size, primerError)
    }

    private class ResultadoSubida(val error: Throwable?, val hayFotosSinSubir: Boolean = false)

    // El trazado no espera a las fotos. Si una foto no se puede subir, el documento viaja sin ella y la fila local
    // conserva la ruta y queda PENDIENTE para volver a probar la próxima vez
    private suspend fun subirEntidad(idUsuario: String, entidad: SenderoEntidad): ResultadoSubida {
        return try {
            val subida = subirFotosSiSonLocales(idUsuario, entidad)
            fuenteRemota.guardarSendero(idUsuario, subida.entidadRemota.aRed())
            if (entidad.estaEliminado) {
                senderoDao.borrarEliminadoSiNoCambio(idUsuario, entidad.id, entidad.fechaModificacion)
            } else {
                // Con fotos sin subir queda PENDIENTE y no CON_ERROR, porque el trazado ya llegó y solo falta reintentar las fotos
                val estadoFinal = if (subida.hayFotosSinSubir) EstadoSincronizacion.PENDIENTE else EstadoSincronizacion.SINCRONIZADO
                senderoDao.actualizarEstadoSiNoCambio(idUsuario, entidad.id, entidad.fechaModificacion, estadoFinal.name)
            }
            ResultadoSubida(null, subida.hayFotosSinSubir)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo al subir el sendero ${entidad.id}", error)
            senderoDao.actualizarEstadoSiNoCambio(
                idUsuario,
                entidad.id,
                entidad.fechaModificacion,
                EstadoSincronizacion.CON_ERROR.name
            )
            ResultadoSubida(error)
        }
    }

    private class SubidaDeFotos(val entidadRemota: SenderoEntidad, val hayFotosSinSubir: Boolean)

    // La foto se sube antes que el documento porque Firestore solo debe guardar URLs que otros dispositivos puedan abrir
    private suspend fun subirFotoSiEsLocal(idUsuario: String, entidad: SenderoEntidad): SubidaDeFotos {
        if (entidad.estaEliminado || !esFotoLocal(entidad.foto)) return SubidaDeFotos(entidad, false)
        return try {
            val urlRemota = subidorFotos.subir(entidad.foto)
            Log.d(TAG, "Foto del sendero ${entidad.id} subida a $urlRemota")
            senderoDao.actualizarFotoSiNoCambio(idUsuario, entidad.id, entidad.fechaModificacion, urlRemota)
            SubidaDeFotos(entidad.copy(foto = urlRemota), false)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "No se pudo subir la foto del sendero ${entidad.id}; el sendero se sube sin foto", error)
            SubidaDeFotos(entidad.copy(foto = ""), true)
        }
    }

    private suspend fun subirFotosSiSonLocales(idUsuario: String, entidad: SenderoEntidad): SubidaDeFotos {
        val principal = subirFotoSiEsLocal(idUsuario, entidad)
        val puntos = subirFotosDePuntosSiSonLocales(idUsuario, principal.entidadRemota)
        return SubidaDeFotos(puntos.entidadRemota, principal.hayFotosSinSubir || puntos.hayFotosSinSubir)
    }

    // Cada URL se guarda apenas se sube, con la transición condicional, para no repetir la subida si una posterior falla.
    // La foto que no se pudo subir se omite del documento remoto y se conserva la local
    private suspend fun subirFotosDePuntosSiSonLocales(idUsuario: String, entidad: SenderoEntidad): SubidaDeFotos {
        if (entidad.estaEliminado || entidad.recorrido.none { punto -> esFotoLocal(punto.foto) }) return SubidaDeFotos(entidad, false)
        val recorridoLocal = entidad.recorrido.toMutableList()
        val recorridoRemoto = entidad.recorrido.toMutableList()
        var hayFotosSinSubir = false
        for (indice in entidad.recorrido.indices) {
            val punto = entidad.recorrido[indice]
            if (!esFotoLocal(punto.foto)) continue
            // Si el archivo ya no existe, la foto se limpia para no dejar el sendero pendiente para siempre
            val fotoNueva: String? = if (existeArchivo(punto.foto)) {
                try {
                    subidorFotos.subir(punto.foto).also { urlRemota -> Log.d(TAG, "Foto del punto $indice del sendero ${entidad.id} subida a $urlRemota") }
                } catch (excepcion: CancellationException) {
                    throw excepcion
                } catch (error: Exception) {
                    Log.e(TAG, "No se pudo subir la foto del punto $indice del sendero ${entidad.id}; el punto se sube sin foto", error)
                    null
                }
            } else {
                Log.e(TAG, "Falta el archivo local de la foto del punto $indice del sendero ${entidad.id}; se limpia la foto")
                ""
            }
            if (fotoNueva == null) {
                hayFotosSinSubir = true
                recorridoRemoto[indice] = punto.copy(foto = "")
            } else {
                recorridoLocal[indice] = punto.copy(foto = fotoNueva)
                recorridoRemoto[indice] = punto.copy(foto = fotoNueva)
                senderoDao.actualizarRecorridoSiNoCambio(idUsuario, entidad.id, entidad.fechaModificacion, recorridoLocal.toList())
            }
        }
        return SubidaDeFotos(entidad.copy(recorrido = recorridoRemoto), hayFotosSinSubir)
    }
}
