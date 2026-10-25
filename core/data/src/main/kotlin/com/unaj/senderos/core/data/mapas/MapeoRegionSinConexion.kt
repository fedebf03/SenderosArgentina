package com.unaj.senderos.core.data.mapas

import com.unaj.senderos.core.domain.model.EstadoMapaSinConexion
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

const val MENSAJE_DESCARGA_INCOMPLETA = "La descarga quedó incompleta. Volvé a intentarla."

const val MAXIMO_ERRORES_SEGUIDOS = 5

private val formatoJson = Json { ignoreUnknownKeys = true }

@Serializable
data class MetadatosRegion(
    val idUsuario: String,
    val idSendero: String,
    val firma: String
)

data class ProgresoRegion(
    val recursosCompletados: Long,
    val recursosRequeridos: Long,
    val bytesCompletados: Long,
    val estaCompleta: Boolean,
    val estaDescargando: Boolean
)

fun MetadatosRegion.aBytes(): ByteArray {
    return formatoJson.encodeToString(MetadatosRegion.serializer(), this).toByteArray(Charsets.UTF_8)
}

fun leerMetadatosRegion(bytes: ByteArray?): MetadatosRegion? {
    if (bytes == null) return null
    return try {
        formatoJson.decodeFromString(MetadatosRegion.serializer(), bytes.toString(Charsets.UTF_8))
    } catch (error: SerializationException) {
        null
    } catch (error: IllegalArgumentException) {
        null
    }
}

fun calcularPorcentaje(recursosCompletados: Long, recursosRequeridos: Long): Int {
    if (recursosRequeridos <= 0L) return 0
    val porcentaje = recursosCompletados * 100L / recursosRequeridos
    return porcentaje.coerceIn(0L, 100L).toInt()
}

fun crearClaveRegion(idUsuario: String, idSendero: String): String = "$idUsuario/$idSendero"

fun determinarEstado(
    progreso: ProgresoRegion?,
    firmaRegion: String?,
    firmaActual: String,
    mensajeError: String?,
    esperandoConexion: Boolean = false
): EstadoMapaSinConexion {
    if (mensajeError != null) return EstadoMapaSinConexion.ConError(mensajeError)
    if (firmaRegion == null) return EstadoMapaSinConexion.NoDescargado
    if (progreso == null) return EstadoMapaSinConexion.Descargando(0, 0)
    if (progreso.estaCompleta) {
        return if (firmaRegion == firmaActual) {
            EstadoMapaSinConexion.Disponible(progreso.bytesCompletados)
        } else {
            EstadoMapaSinConexion.Desactualizado(progreso.bytesCompletados)
        }
    }
    val porcentaje = calcularPorcentaje(progreso.recursosCompletados, progreso.recursosRequeridos)
    if (esperandoConexion) return EstadoMapaSinConexion.EsperandoConexion(porcentaje, progreso.bytesCompletados)
    if (progreso.estaDescargando) return EstadoMapaSinConexion.Descargando(porcentaje, progreso.bytesCompletados)
    return EstadoMapaSinConexion.ConError(MENSAJE_DESCARGA_INCOMPLETA)
}

enum class CategoriaErrorRegion { CONEXION, SERVIDOR, NO_ENCONTRADO, OTRO }

enum class DecisionError { ESPERAR_CONEXION, DETENER }

fun decidirAnteError(categoria: CategoriaErrorRegion, erroresSeguidos: Int): DecisionError {
    val esRecuperable = categoria == CategoriaErrorRegion.CONEXION || categoria == CategoriaErrorRegion.SERVIDOR
    return if (esRecuperable && erroresSeguidos < MAXIMO_ERRORES_SEGUIDOS) {
        DecisionError.ESPERAR_CONEXION
    } else {
        DecisionError.DETENER
    }
}

fun mensajeErrorParaUsuario(categoria: CategoriaErrorRegion): String {
    return when (categoria) {
        CategoriaErrorRegion.CONEXION -> "Se perdió la conexión varias veces. Revisá tu señal y volvé a intentarlo."
        CategoriaErrorRegion.SERVIDOR -> "El servidor de mapas no responde. Probá de nuevo más tarde."
        CategoriaErrorRegion.NO_ENCONTRADO -> "El servidor de mapas no tiene los datos de esta zona."
        CategoriaErrorRegion.OTRO -> "No se pudo descargar el mapa. Volvé a intentarlo."
    }
}

enum class AccionDescarga { CREAR, YA_DISPONIBLE, REACTIVAR, CREAR_CONSERVANDO_ANTERIOR, CREAR_REEMPLAZANDO }

fun decidirAccionDescarga(firmaRegion: String?, progreso: ProgresoRegion?, firmaNueva: String): AccionDescarga {
    if (firmaRegion == null) return AccionDescarga.CREAR
    val estaCompleta = progreso?.estaCompleta == true
    return when {
        firmaRegion == firmaNueva && estaCompleta -> AccionDescarga.YA_DISPONIBLE
        firmaRegion == firmaNueva -> AccionDescarga.REACTIVAR
        estaCompleta -> AccionDescarga.CREAR_CONSERVANDO_ANTERIOR
        else -> AccionDescarga.CREAR_REEMPLAZANDO
    }
}

data class CandidataRegion(val id: Long, val estaCompleta: Boolean)

data class PlanRegiones(val principal: Long, val anterior: Long?, val sobrantes: List<Long>)

// De las regiones de un mismo sendero la vigente es la de mayor id. Si todavía no terminó de descargarse, también
// se conserva la última región completa para seguir teniendo mapa mientras tanto
fun planificarRegiones(candidatas: List<CandidataRegion>): PlanRegiones? {
    val ordenadas = candidatas.sortedByDescending { candidata -> candidata.id }
    val principal = ordenadas.firstOrNull() ?: return null
    val resto = ordenadas.drop(1)
    val anterior = if (principal.estaCompleta) null else resto.firstOrNull { candidata -> candidata.estaCompleta }
    val sobrantes = resto.filter { candidata -> candidata != anterior }.map { candidata -> candidata.id }
    return PlanRegiones(principal = principal.id, anterior = anterior?.id, sobrantes = sobrantes)
}
