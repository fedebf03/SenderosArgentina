package com.unaj.senderos.core.data.repository

import android.util.Log
import com.unaj.senderos.core.database.EjecutorTransacciones
import com.unaj.senderos.core.database.PuntoCapturadoDao
import com.unaj.senderos.core.database.SenderoDao
import com.unaj.senderos.core.data.mapeo.aDominio
import com.unaj.senderos.core.data.mapeo.aEntidad
import com.unaj.senderos.core.data.sincronizacion.PlanificadorSincronizacion
import com.unaj.senderos.core.domain.recorrido.CalculadoraInsercionPuntos
import com.unaj.senderos.core.domain.recorrido.CalculadoraRecorrido
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.PuntoCapturado
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.port.AlmacenFotos
import com.unaj.senderos.core.domain.repository.PuntosCapturadosRepositorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.roundToLong

private const val TAG = "PUNTOS_CAPTURADOS"
private const val FACTOR_UN_DECIMAL = 10.0
private const val TOLERANCIA_DISTANCIA = 1e-9

// Repositorio offline-first de puntos capturados: todo en Room. Los puntos no se sincronizan solos; pasan a la nube al incorporarse
// al sendero, que queda PENDIENTE. Al cerrar sesión se conservan para el mismo usuario (aislados por idUsuario).
// Incorporar corre en un alcance de proceso (no se cancela con la pantalla) y en una única transacción de Room
class PuntosCapturadosRepositorioOfflineFirst(
    private val puntoCapturadoDao: PuntoCapturadoDao,
    private val senderoDao: SenderoDao,
    private val ejecutorTransacciones: EjecutorTransacciones,
    private val planificadorSincronizacion: PlanificadorSincronizacion,
    private val almacenFotos: AlmacenFotos,
    private val alcanceDeProceso: CoroutineScope,
    private val reloj: () -> Long = System::currentTimeMillis
) : PuntosCapturadosRepositorio {

    // Guardar el punto en Room al instante para que no se pierda si muere el proceso
    override suspend fun guardar(punto: PuntoCapturado): Result<Unit> {
        return try {
            puntoCapturadoDao.guardar(punto.aEntidad())
            Log.d(TAG, "Punto ${punto.id} guardado en el sendero ${punto.idSendero}")
            Result.success(Unit)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo al guardar el punto ${punto.id}", error)
            Result.failure(IllegalStateException("No se pudo guardar el punto"))
        }
    }

    // Observar los puntos pendientes del sendero; un error de lectura se registra y el flujo emite una lista vacía
    override fun observarPendientes(idUsuario: String, idSendero: String): Flow<List<PuntoCapturado>> {
        return puntoCapturadoDao.observarPendientes(idUsuario, idSendero)
            .map { entidades -> entidades.map { entidad -> entidad.aDominio() } }
            .catch { error ->
                Log.e(TAG, "Fallo al leer los puntos del sendero $idSendero", error)
                emit(emptyList())
            }
    }

    // Borrar los puntos de Room y, solo si salió bien, los archivos de sus fotos
    override suspend fun descartar(idUsuario: String, ids: Collection<String>): Result<Unit> {
        return try {
            val listaIds = ids.toList()
            val fotos = if (listaIds.isEmpty()) emptyList() else puntoCapturadoDao.obtenerPorIds(idUsuario, listaIds).map { entidad -> entidad.foto }
            if (listaIds.isNotEmpty()) puntoCapturadoDao.borrar(idUsuario, listaIds)
            fotos.filter { foto -> foto.isNotEmpty() }.forEach { foto -> almacenFotos.borrar(foto) }
            Log.d(TAG, "Puntos descartados: ${listaIds.size}")
            Result.success(Unit)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo al descartar puntos", error)
            Result.failure(IllegalStateException("No se pudieron descartar los puntos"))
        }
    }

    // Incorporar los puntos elegidos en un alcance de proceso: si la pantalla se cancela, la operación termina igual
    override suspend fun incorporarAlSendero(idUsuario: String, idSendero: String, idsElegidos: Set<String>): Result<Sendero> {
        return try {
            val incorporacion = alcanceDeProceso.async { incorporarEnTransaccion(idUsuario, idSendero, idsElegidos) }.await()
                ?: return Result.failure(IllegalStateException("No se encontró el sendero"))
            if (incorporacion.second) planificadorSincronizacion.solicitarSincronizacion(idUsuario)
            Result.success(incorporacion.first)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Fallo al incorporar puntos al sendero $idSendero", error)
            Result.failure(IllegalStateException("No se pudieron incorporar los puntos al sendero"))
        }
    }

    // Leer, insertar y escribir el sendero y borrar los capturados en una sola transacción; devuelve null si el sendero no existe
    // y, si no, el sendero con un indicador de si hubo cambios. Leer dentro de la transacción evita pisar una fusión de la sincronización
    private suspend fun incorporarEnTransaccion(idUsuario: String, idSendero: String, idsElegidos: Set<String>): Pair<Sendero, Boolean>? {
        return ejecutorTransacciones.ejecutar {
            val entidad = senderoDao.obtenerPorId(idUsuario, idSendero)
            if (entidad == null || entidad.estaEliminado) return@ejecutar null
            val sendero = entidad.aDominio()
            val elegidos = puntoCapturadoDao.obtenerPendientes(idUsuario, idSendero)
                .filter { punto -> punto.id in idsElegidos }
                .map { punto -> punto.aDominio() }
            if (elegidos.isEmpty()) return@ejecutar sendero to false
            val recorridoNuevo = CalculadoraInsercionPuntos.insertar(
                sendero.recorrido,
                sendero.tipoRecorrido,
                elegidos.map { punto -> convertirEnPuntoDelRecorrido(punto) }
            )
            val senderoActualizado = sendero.copy(
                recorrido = recorridoNuevo,
                distanciaKilometros = recalcularDistanciaSiCorresponde(sendero, recorridoNuevo)
            )
            senderoDao.guardar(
                senderoActualizado.aEntidad(
                    fechaModificacion = reloj(),
                    estadoSincronizacion = EstadoSincronizacion.PENDIENTE,
                    estaEliminado = false
                )
            )
            puntoCapturadoDao.borrar(idUsuario, elegidos.map { punto -> punto.id })
            senderoActualizado to true
        }
    }

    // El nombre es la nota completa recortada de espacios
    private fun convertirEnPuntoDelRecorrido(punto: PuntoCapturado): PuntoRecorrido {
        return PuntoRecorrido(
            latitud = punto.latitud,
            longitud = punto.longitud,
            tipo = punto.tipo,
            nombre = punto.nota.trim(),
            foto = punto.foto
        )
    }

    // Recalcular la distancia solo si el sendero la tenía igual a la calculada del mapa (redondeada a un decimal); si fue escrita a mano se conserva
    private fun recalcularDistanciaSiCorresponde(sendero: Sendero, recorridoNuevo: List<PuntoRecorrido>): Double {
        if (sendero.recorrido.size < 2) return sendero.distanciaKilometros
        val calculadaAntes = redondearAUnDecimal(CalculadoraRecorrido.distanciaKilometros(sendero.recorrido, sendero.tipoRecorrido))
        if (abs(sendero.distanciaKilometros - calculadaAntes) > TOLERANCIA_DISTANCIA) return sendero.distanciaKilometros
        return redondearAUnDecimal(CalculadoraRecorrido.distanciaKilometros(recorridoNuevo, sendero.tipoRecorrido))
    }

    // Redondear una distancia en kilómetros a un decimal, como lo hace el formulario al copiar la distancia del mapa
    private fun redondearAUnDecimal(kilometros: Double): Double {
        return (kilometros * FACTOR_UN_DECIMAL).roundToLong() / FACTOR_UN_DECIMAL
    }
}
