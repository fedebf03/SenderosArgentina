package com.unaj.senderos.core.data.archivos

import android.content.Context
import android.util.Log
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.RecorridoRealizado
import com.unaj.senderos.core.domain.port.AlmacenBorradorRecorrido
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "BORRADOR_RECORRIDO"
private const val NOMBRE_ARCHIVO = "borrador_recorrido.json"
private const val SUFIJO_TEMPORAL = ".tmp"

@Serializable
private data class CoordenadaBorrador(val latitud: Double, val longitud: Double)

@Serializable
private data class BorradorRecorridoJson(
    val id: String,
    val idUsuario: String,
    val idSendero: String,
    val nombreSendero: String,
    val inicioMilisegundos: Long,
    val finMilisegundos: Long,
    val duracionMilisegundos: Long,
    val distanciaKilometros: Double,
    val velocidadPromedioKilometrosPorHora: Double,
    val velocidadMaximaKilometrosPorHora: Double,
    val pasos: Int? = null,
    val ritmoPasosPorMinuto: Double? = null,
    val trazadoCaminado: List<CoordenadaBorrador>,
    val esSimulado: Boolean = false
)

// El borrador se escribe primero en un temporal y después se renombra. Por lo tanto un corte a mitad de la escritura
// nunca deja el archivo a medias
class AlmacenBorradorRecorridoArchivo(private val archivo: File) : AlmacenBorradorRecorrido {

    @Inject
    constructor(@ApplicationContext contexto: Context) : this(File(contexto.applicationContext.filesDir, NOMBRE_ARCHIVO))

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun guardar(borrador: RecorridoRealizado): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val temporal = File(archivo.parentFile, archivo.name + SUFIJO_TEMPORAL)
            temporal.writeText(json.encodeToString(BorradorRecorridoJson.serializer(), borrador.aJson()))
            if (!temporal.renameTo(archivo)) {
                temporal.copyTo(archivo, overwrite = true)
                temporal.delete()
            }
            Result.success(Unit)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "No se pudo guardar el borrador del recorrido", error)
            Result.failure(IllegalStateException("No se pudo guardar el borrador del recorrido"))
        }
    }

    override suspend fun leer(): RecorridoRealizado? = withContext(Dispatchers.IO) {
        try {
            if (!archivo.exists()) return@withContext null
            json.decodeFromString(BorradorRecorridoJson.serializer(), archivo.readText()).aDominio()
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "El borrador del recorrido está dañado y se ignora", error)
            null
        }
    }

    override suspend fun borrar() {
        withContext(Dispatchers.IO) {
            try {
                archivo.delete()
                File(archivo.parentFile, archivo.name + SUFIJO_TEMPORAL).delete()
            } catch (error: Exception) {
                Log.e(TAG, "No se pudo borrar el borrador del recorrido", error)
            }
        }
    }
}

private fun RecorridoRealizado.aJson() = BorradorRecorridoJson(
    id = id,
    idUsuario = idUsuario,
    idSendero = idSendero,
    nombreSendero = nombreSendero,
    inicioMilisegundos = inicioMilisegundos,
    finMilisegundos = finMilisegundos,
    duracionMilisegundos = duracionMilisegundos,
    distanciaKilometros = distanciaKilometros,
    velocidadPromedioKilometrosPorHora = velocidadPromedioKilometrosPorHora,
    velocidadMaximaKilometrosPorHora = velocidadMaximaKilometrosPorHora,
    pasos = pasos,
    ritmoPasosPorMinuto = ritmoPasosPorMinuto,
    trazadoCaminado = trazadoCaminado.map { coordenada -> CoordenadaBorrador(coordenada.latitud, coordenada.longitud) },
    esSimulado = esSimulado
)

private fun BorradorRecorridoJson.aDominio() = RecorridoRealizado(
    id = id,
    idUsuario = idUsuario,
    idSendero = idSendero,
    nombreSendero = nombreSendero,
    inicioMilisegundos = inicioMilisegundos,
    finMilisegundos = finMilisegundos,
    duracionMilisegundos = duracionMilisegundos,
    distanciaKilometros = distanciaKilometros,
    velocidadPromedioKilometrosPorHora = velocidadPromedioKilometrosPorHora,
    velocidadMaximaKilometrosPorHora = velocidadMaximaKilometrosPorHora,
    pasos = pasos,
    ritmoPasosPorMinuto = ritmoPasosPorMinuto,
    trazadoCaminado = trazadoCaminado.map { coordenada -> Coordenada(coordenada.latitud, coordenada.longitud) },
    esSimulado = esSimulado,
    estadoSincronizacion = EstadoSincronizacion.PENDIENTE
)
