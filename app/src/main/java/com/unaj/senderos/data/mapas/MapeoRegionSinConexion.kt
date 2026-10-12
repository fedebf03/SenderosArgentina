package com.unaj.senderos.data.mapas

import com.unaj.senderos.domain.model.EstadoMapaSinConexion
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

const val MENSAJE_DESCARGA_INCOMPLETA = "La descarga quedó incompleta. Volvé a intentarla."

const val MAXIMO_ERRORES_SEGUIDOS = 5

private val formatoJson = Json { ignoreUnknownKeys = true }

// Metadatos guardados en cada región sin conexión para asociarla al usuario, al sendero y al trazado
@Serializable
data class MetadatosRegion(
    val idUsuario: String,
    val idSendero: String,
    val firma: String
)

// Avance de una región sin conexión, independiente de las clases de MapLibre
data class ProgresoRegion(
    val recursosCompletados: Long,
    val recursosRequeridos: Long,
    val bytesCompletados: Long,
    val estaCompleta: Boolean,
    val estaDescargando: Boolean
)

// Serializar los metadatos a los bytes JSON que guarda la región
fun MetadatosRegion.aBytes(): ByteArray {
    return formatoJson.encodeToString(MetadatosRegion.serializer(), this).toByteArray(Charsets.UTF_8)
}

// Leer los metadatos de una región; devolver nulo si faltan o no son legibles
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

// Calcular el porcentaje de recursos completados acotado entre 0 y 100
fun calcularPorcentaje(recursosCompletados: Long, recursosRequeridos: Long): Int {
    if (recursosRequeridos <= 0L) return 0
    val porcentaje = recursosCompletados * 100L / recursosRequeridos
    return porcentaje.coerceIn(0L, 100L).toInt()
}

// Crear la clave que identifica la región de un sendero de un usuario
fun crearClaveRegion(idUsuario: String, idSendero: String): String = "$idUsuario/$idSendero"

// Determinar el estado visible del mapa a partir del avance, la firma guardada y la firma vigente
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

// Categoría de un error reportado por MapLibre durante la descarga
enum class CategoriaErrorRegion { CONEXION, SERVIDOR, NO_ENCONTRADO, OTRO }

// Qué hacer con la descarga ante un error
enum class DecisionError { ESPERAR_CONEXION, DETENER }

// Decidir si esperar a que MapLibre reintente o detener la descarga, según la categoría y los errores seguidos
fun decidirAnteError(categoria: CategoriaErrorRegion, erroresSeguidos: Int): DecisionError {
    val esRecuperable = categoria == CategoriaErrorRegion.CONEXION || categoria == CategoriaErrorRegion.SERVIDOR
    return if (esRecuperable && erroresSeguidos < MAXIMO_ERRORES_SEGUIDOS) {
        DecisionError.ESPERAR_CONEXION
    } else {
        DecisionError.DETENER
    }
}

// Obtener el mensaje en español para el usuario según la categoría del error, sin detalles técnicos
fun mensajeErrorParaUsuario(categoria: CategoriaErrorRegion): String {
    return when (categoria) {
        CategoriaErrorRegion.CONEXION -> "Se perdió la conexión varias veces. Revisá tu señal y volvé a intentarlo."
        CategoriaErrorRegion.SERVIDOR -> "El servidor de mapas no responde. Probá de nuevo más tarde."
        CategoriaErrorRegion.NO_ENCONTRADO -> "El servidor de mapas no tiene los datos de esta zona."
        CategoriaErrorRegion.OTRO -> "No se pudo descargar el mapa. Volvé a intentarlo."
    }
}

// Acción a tomar cuando el usuario pide descargar el mapa de un sendero
enum class AccionDescarga { CREAR, YA_DISPONIBLE, REACTIVAR, CREAR_CONSERVANDO_ANTERIOR, CREAR_REEMPLAZANDO }

// Decidir cómo atender un pedido de descarga según la región existente y la firma del trazado vigente
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

// Región guardada en el dispositivo candidata a ser la de un sendero
data class CandidataRegion(val id: Long, val estaCompleta: Boolean)

// Reparto de las regiones de un sendero: la vigente, la anterior completa que se conserva y las que sobran
data class PlanRegiones(val principal: Long, val anterior: Long?, val sobrantes: List<Long>)

// Planificar qué regiones de un mismo sendero conservar: la de mayor id es la vigente y, si está incompleta, la completa más reciente queda como anterior
fun planificarRegiones(candidatas: List<CandidataRegion>): PlanRegiones? {
    val ordenadas = candidatas.sortedByDescending { candidata -> candidata.id }
    val principal = ordenadas.firstOrNull() ?: return null
    val resto = ordenadas.drop(1)
    val anterior = if (principal.estaCompleta) null else resto.firstOrNull { candidata -> candidata.estaCompleta }
    val sobrantes = resto.filter { candidata -> candidata != anterior }.map { candidata -> candidata.id }
    return PlanRegiones(principal = principal.id, anterior = anterior?.id, sobrantes = sobrantes)
}
