package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.domain.port.FormatoCopiaSeguridad
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val NOMBRE_APLICACION = "SenderosArgentina"
private const val VERSION_FORMATO = 1
private const val MENSAJE_NO_ES_COPIA = "El archivo no es una copia de seguridad de Senderos"
// Solo se aceptan las fotos subidas por la app; una copia armada a mano no puede meter rutas del teléfono ni sitios de terceros
private const val PREFIJO_FOTOS_EN_LA_NUBE = "https://res.cloudinary.com/"
private const val LARGO_MAXIMO_ID = 128
private val FORMATO_ID_VALIDO = Regex("[A-Za-z0-9_-]+")

// Archivo de copia de seguridad: identifica la aplicación y la versión para poder leer copias de versiones futuras
@Serializable
private data class CopiaSeguridadArchivo(
    val aplicacion: String,
    val version: Int,
    val fechaExportacionMilisegundos: Long = 0L,
    val senderos: List<SenderoCopia>
)

// Datos que el usuario carga en un sendero. No viajan el usuario, el estado de sincronización ni el perfil (se recalcula)
@Serializable
private data class SenderoCopia(
    val id: String,
    val nombre: String,
    val region: String = "",
    val distanciaKilometros: Double = 0.0,
    val dificultad: String = Dificultad.MEDIA.name,
    val descripcion: String = "",
    val desnivelMetros: Int = 0,
    val duracionEstimada: String = "",
    val alturaMaximaMetros: Int = 0,
    val alturaMinimaMetros: Int = 0,
    val tipoRecorrido: String = TipoRecorrido.LINEAL.name,
    val puntoInicio: String = "",
    val mejorEpoca: String = "",
    val recomendaciones: List<String> = emptyList(),
    val servicios: List<String> = emptyList(),
    val etapas: List<EtapaCopia> = emptyList(),
    val esFavorito: Boolean = false,
    val recorrido: List<PuntoCopia> = emptyList(),
    val foto: String = "",
    val bioma: String = Bioma.SIN_DEFINIR.name
)

@Serializable
private data class EtapaCopia(val numero: Int, val nombre: String, val distanciaKilometros: Double)

@Serializable
private data class PuntoCopia(
    val latitud: Double,
    val longitud: Double,
    val tipo: String = TipoPunto.PASO.name,
    val nombre: String = "",
    val foto: String = ""
)

class CopiaSeguridadJson @Inject constructor() : FormatoCopiaSeguridad {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Convertir los senderos al JSON de la copia, legible por una persona
    override fun escribir(senderos: List<Sendero>, instanteMilisegundos: Long): String {
        val archivo = CopiaSeguridadArchivo(
            aplicacion = NOMBRE_APLICACION,
            version = VERSION_FORMATO,
            fechaExportacionMilisegundos = instanteMilisegundos,
            senderos = senderos.map(::aCopia)
        )
        return json.encodeToString(CopiaSeguridadArchivo.serializer(), archivo)
    }

    // Leer los senderos de la copia; un JSON de otra aplicación, uno roto o uno sin los campos obligatorios falla con un mensaje claro
    override fun leer(texto: String): Result<List<Sendero>> {
        val archivo = try {
            json.decodeFromString(CopiaSeguridadArchivo.serializer(), texto)
        } catch (error: SerializationException) {
            return Result.failure(IllegalArgumentException(MENSAJE_NO_ES_COPIA, error))
        } catch (error: IllegalArgumentException) {
            return Result.failure(IllegalArgumentException(MENSAJE_NO_ES_COPIA, error))
        }
        if (archivo.aplicacion != NOMBRE_APLICACION) return Result.failure(IllegalArgumentException(MENSAJE_NO_ES_COPIA))
        return Result.success(archivo.senderos.map(::aDominio))
    }

    // Pasar un sendero al formato de la copia, dejando afuera las fotos que solo existen en este teléfono
    private fun aCopia(sendero: Sendero): SenderoCopia = SenderoCopia(
        id = sendero.id,
        nombre = sendero.nombre,
        region = sendero.region,
        distanciaKilometros = sendero.distanciaKilometros,
        dificultad = sendero.dificultad.name,
        descripcion = sendero.descripcion,
        desnivelMetros = sendero.desnivelMetros,
        duracionEstimada = sendero.duracionEstimada,
        alturaMaximaMetros = sendero.alturaMaximaMetros,
        alturaMinimaMetros = sendero.alturaMinimaMetros,
        tipoRecorrido = sendero.tipoRecorrido.name,
        puntoInicio = sendero.puntoInicio,
        mejorEpoca = sendero.mejorEpoca,
        recomendaciones = sendero.recomendaciones,
        servicios = sendero.servicios,
        etapas = sendero.etapas.map { etapa -> EtapaCopia(etapa.numero, etapa.nombre, etapa.distanciaKilometros) },
        esFavorito = sendero.esFavorito,
        recorrido = sendero.recorrido.map { punto ->
            PuntoCopia(punto.latitud, punto.longitud, punto.tipo.name, punto.nombre, soloFotoEnLaNube(punto.foto))
        },
        foto = soloFotoEnLaNube(sendero.foto),
        bioma = sendero.bioma.name
    )

    // Pasar un sendero de la copia al dominio; los valores desconocidos (de una versión más nueva) toman el valor por defecto
    private fun aDominio(copia: SenderoCopia): Sendero = Sendero(
        // Un id con un formato inesperado (por ejemplo con "/") rompería la ruta en la nube: se guarda como sendero nuevo
        id = copia.id.takeIf { id -> id.length <= LARGO_MAXIMO_ID && FORMATO_ID_VALIDO.matches(id) }.orEmpty(),
        idUsuario = "",
        nombre = copia.nombre,
        region = copia.region,
        distanciaKilometros = copia.distanciaKilometros,
        dificultad = enumeradoODefecto(copia.dificultad, Dificultad.MEDIA),
        descripcion = copia.descripcion,
        desnivelMetros = copia.desnivelMetros,
        duracionEstimada = copia.duracionEstimada,
        alturaMaximaMetros = copia.alturaMaximaMetros,
        alturaMinimaMetros = copia.alturaMinimaMetros,
        tipoRecorrido = enumeradoODefecto(copia.tipoRecorrido, TipoRecorrido.LINEAL),
        puntoInicio = copia.puntoInicio,
        mejorEpoca = copia.mejorEpoca,
        recomendaciones = copia.recomendaciones,
        servicios = copia.servicios,
        etapas = copia.etapas.map { etapa -> Etapa(etapa.numero, etapa.nombre, etapa.distanciaKilometros) },
        esFavorito = copia.esFavorito,
        recorrido = copia.recorrido.map { punto ->
            PuntoRecorrido(punto.latitud, punto.longitud, enumeradoODefecto(punto.tipo, TipoPunto.PASO), punto.nombre, soloFotoEnLaNube(punto.foto))
        },
        foto = soloFotoEnLaNube(copia.foto),
        bioma = enumeradoODefecto(copia.bioma, Bioma.SIN_DEFINIR)
    )

    // Conservar la foto solo si está subida a la nube de la app; una ruta local no sirve en otro teléfono
    private fun soloFotoEnLaNube(foto: String): String = if (foto.startsWith(PREFIJO_FOTOS_EN_LA_NUBE)) foto else ""
}

// Convertir el nombre al valor del enumerado o devolver el valor por defecto si no existe
private inline fun <reified T : Enum<T>> enumeradoODefecto(nombre: String, porDefecto: T): T =
    enumValues<T>().firstOrNull { valor -> valor.name == nombre } ?: porDefecto
