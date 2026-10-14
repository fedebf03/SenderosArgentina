package com.unaj.senderos.core.data.sincronizacion

import android.util.Log
import com.unaj.senderos.core.database.EjecutorTransacciones
import com.unaj.senderos.core.database.SenderoDao
import com.unaj.senderos.core.database.SenderoEntidad
import com.unaj.senderos.core.data.mapeo.aEntidad
import com.unaj.senderos.core.data.mapeo.aRed
import com.unaj.senderos.core.network.SenderoFuenteRemota
import com.unaj.senderos.core.network.SenderoRed
import com.unaj.senderos.core.network.SubidorFotos
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

// La base local es la fuente de verdad frente a Firestore
class SincronizadorSenderos(
    private val senderoDao: SenderoDao,
    private val fuenteRemota: SenderoFuenteRemota,
    private val ejecutorTransacciones: EjecutorTransacciones,
    private val subidorFotos: SubidorFotos,
    private val existeArchivo: (String) -> Boolean = { ruta -> File(ruta).exists() },
    private val calculadorPerfiles: CalcularPerfilElevacionUseCase? = null,
    private val reloj: () -> Long = System::currentTimeMillis
) {
    // Pedidos de perfil que el servicio rechazó para un trazado concreto; se recuerdan mientras viva el proceso
    private val perfilesRechazados: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // Sincronizar al usuario con Firestore y devolver el resumen de lo subido, o failure si falla la lectura remota
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

    // Ejecutar la fusión y la subida sin capturar excepciones, que captura el llamador
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

    // Completar los perfiles de elevación después de subir los cambios del usuario y volver a subir solo los senderos con
    // perfil nuevo. Esa segunda subida suma a los senderos subidos que no se contaron antes, pero no toca el conteo de
    // guardados sin conexión; un fallo de este paso nunca invalida lo ya subido
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
            val error = subirEntidad(idUsuario, entidad)
            if (error != null) {
                senderosConError++
                if (primerError == null) primerError = error
            } else if (idSendero !in idsSubidos) {
                senderosSubidos++
            }
        }
        return resumen.copy(senderosSubidos = senderosSubidos, senderosConError = senderosConError, primerError = primerError)
    }

    // Calcular el perfil de hasta cinco senderos que lo necesitan y devolver los ids de los que lo obtuvieron. Se corta
    // ante falta de conexión o límite del servicio; un rechazo definitivo se recuerda para no repetirlo mientras viva el proceso
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

    // Identificar un sendero con su trazado actual, para recordar los pedidos de perfil que el servicio rechazó
    private fun claveDePerfil(idUsuario: String, sendero: Sendero): String {
        return "$idUsuario/${sendero.id}/${CalculadoraPerfil.firmaTrazado(sendero.recorrido, sendero.tipoRecorrido)}"
    }

    private suspend fun subirPendientes(idUsuario: String, idsSubidos: MutableSet<String>): ResumenSincronizacion {
        val instanteInicio = reloj()
        val idsFallidos = mutableSetOf<String>()
        var primerError: Throwable? = null
        var senderosSubidos = 0
        var senderosGuardadosSinConexion = 0
        repeat(MAXIMO_PASADAS_SUBIDA) {
            val pendientes = senderoDao.obtenerPendientes(idUsuario).filter { entidad -> entidad.id !in idsFallidos }
            if (pendientes.isEmpty()) {
                return ResumenSincronizacion(
                    senderosSubidos, senderosGuardadosSinConexion, idsFallidos.size, primerError
                )
            }
            pendientes.forEach { entidad ->
                val error = subirEntidad(idUsuario, entidad)
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

    private suspend fun subirEntidad(idUsuario: String, entidad: SenderoEntidad): Throwable? {
        return try {
            val entidadConFotoRemota = subirFotosSiSonLocales(idUsuario, entidad)
            fuenteRemota.guardarSendero(idUsuario, entidadConFotoRemota.aRed())
            if (entidad.estaEliminado) {
                senderoDao.borrarEliminadoSiNoCambio(idUsuario, entidad.id, entidad.fechaModificacion)
            } else {
                senderoDao.actualizarEstadoSiNoCambio(
                    idUsuario,
                    entidad.id,
                    entidad.fechaModificacion,
                    EstadoSincronizacion.SINCRONIZADO.name
                )
            }
            null
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
            error
        }
    }

    // Subir la foto local antes que el documento, porque Firestore solo debe guardar URLs que otros dispositivos puedan abrir
    private suspend fun subirFotoSiEsLocal(idUsuario: String, entidad: SenderoEntidad): SenderoEntidad {
        if (entidad.estaEliminado || !esFotoLocal(entidad.foto)) return entidad
        val urlRemota = subidorFotos.subir(entidad.foto)
        Log.d(TAG, "Foto del sendero ${entidad.id} subida a $urlRemota")
        senderoDao.actualizarFotoSiNoCambio(idUsuario, entidad.id, entidad.fechaModificacion, urlRemota)
        return entidad.copy(foto = urlRemota)
    }
    private suspend fun subirFotosSiSonLocales(idUsuario: String, entidad: SenderoEntidad): SenderoEntidad {
        val conFotoPrincipal = subirFotoSiEsLocal(idUsuario, entidad)
        return subirFotosDePuntosSiSonLocales(idUsuario, conFotoPrincipal)
    }

    // Subir en orden las fotos locales de los puntos del recorrido y guardar cada URL con la transición condicional,
    // para no repetir la subida si una posterior falla; la excepción de una subida fallida sube al llamador
    private suspend fun subirFotosDePuntosSiSonLocales(idUsuario: String, entidad: SenderoEntidad): SenderoEntidad {
        if (entidad.estaEliminado || entidad.recorrido.none { punto -> esFotoLocal(punto.foto) }) return entidad
        val recorrido = entidad.recorrido.toMutableList()
        for (indice in entidad.recorrido.indices) {
            val punto = entidad.recorrido[indice]
            if (!esFotoLocal(punto.foto)) continue
            // Si el archivo ya no existe (borrado o limpiado), la foto se limpia para no dejar el sendero en CON_ERROR para siempre
            val fotoNueva = if (existeArchivo(punto.foto)) {
                subidorFotos.subir(punto.foto).also { urlRemota -> Log.d(TAG, "Foto del punto $indice del sendero ${entidad.id} subida a $urlRemota") }
            } else {
                Log.e(TAG, "Falta el archivo local de la foto del punto $indice del sendero ${entidad.id}; se limpia la foto")
                ""
            }
            recorrido[indice] = punto.copy(foto = fotoNueva)
            senderoDao.actualizarRecorridoSiNoCambio(idUsuario, entidad.id, entidad.fechaModificacion, recorrido.toList())
        }
        return entidad.copy(recorrido = recorrido)
    }
}
