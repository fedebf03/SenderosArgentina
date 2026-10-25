package com.unaj.senderos.core.data.archivos

import android.content.Context
import android.util.Log
import com.unaj.senderos.core.domain.clima.ClimaActual
import com.unaj.senderos.core.domain.clima.CondicionClima
import com.unaj.senderos.core.domain.clima.PronosticoClima
import com.unaj.senderos.core.domain.clima.PronosticoDia
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.port.AlmacenPronosticos
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "PRONOSTICOS"
private const val NOMBRE_CARPETA = "pronosticos"
private const val SUFIJO_TEMPORAL = ".tmp"
private val CLAVE_VALIDA = Regex("[A-Za-z0-9_-]+")

@Serializable
private data class ClimaActualJson(
    val temperaturaCelsius: Double,
    val sensacionTermicaCelsius: Double? = null,
    val vientoKilometrosPorHora: Double,
    val rafagasKilometrosPorHora: Double? = null,
    val condicion: String,
    val esDeDia: Boolean
)

@Serializable
private data class PronosticoDiaJson(
    val fecha: String,
    val temperaturaMaximaCelsius: Double,
    val temperaturaMinimaCelsius: Double,
    val probabilidadLluviaPorcentaje: Int? = null,
    val precipitacionMilimetros: Double,
    val vientoMaximoKilometrosPorHora: Double,
    val rafagasMaximasKilometrosPorHora: Double,
    val condicion: String
)

@Serializable
private data class PronosticoClimaJson(
    val latitud: Double,
    val longitud: Double,
    val obtenidoMilisegundos: Long,
    val actual: ClimaActualJson,
    val dias: List<PronosticoDiaJson>
)

// Cada pronóstico se escribe en un temporal y después se renombra para no dejar nunca un archivo a medias.
// Por otro lado la clave solo admite letras, números y guiones. Así el nombre no puede apuntar fuera de la carpeta (con "../")
class AlmacenPronosticosArchivo(private val carpeta: File) : AlmacenPronosticos {

    @Inject
    constructor(@ApplicationContext contexto: Context) : this(File(contexto.applicationContext.filesDir, NOMBRE_CARPETA))

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun guardar(clave: String, pronostico: PronosticoClima): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(CLAVE_VALIDA.matches(clave)) { "Clave de pronóstico inválida" }
            carpeta.mkdirs()
            val archivo = File(carpeta, "$clave.json")
            val temporal = File(carpeta, "$clave.json$SUFIJO_TEMPORAL")
            temporal.writeText(json.encodeToString(PronosticoClimaJson.serializer(), pronostico.aJson()))
            if (!temporal.renameTo(archivo)) {
                temporal.copyTo(archivo, overwrite = true)
                temporal.delete()
            }
            Result.success(Unit)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "No se pudo guardar el pronóstico", error)
            Result.failure(IllegalStateException("No se pudo guardar el pronóstico"))
        }
    }

    override suspend fun leer(clave: String): PronosticoClima? = withContext(Dispatchers.IO) {
        try {
            if (!CLAVE_VALIDA.matches(clave)) return@withContext null
            val archivo = File(carpeta, "$clave.json")
            if (!archivo.exists()) return@withContext null
            json.decodeFromString(PronosticoClimaJson.serializer(), archivo.readText()).aDominio()
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "El pronóstico guardado está dañado y se ignora", error)
            null
        }
    }
}

private fun PronosticoClima.aJson() = PronosticoClimaJson(
    latitud = lugar.latitud,
    longitud = lugar.longitud,
    obtenidoMilisegundos = obtenidoMilisegundos,
    actual = ClimaActualJson(
        temperaturaCelsius = actual.temperaturaCelsius,
        sensacionTermicaCelsius = actual.sensacionTermicaCelsius,
        vientoKilometrosPorHora = actual.vientoKilometrosPorHora,
        rafagasKilometrosPorHora = actual.rafagasKilometrosPorHora,
        condicion = actual.condicion.name,
        esDeDia = actual.esDeDia
    ),
    dias = dias.map { dia ->
        PronosticoDiaJson(
            fecha = dia.fecha,
            temperaturaMaximaCelsius = dia.temperaturaMaximaCelsius,
            temperaturaMinimaCelsius = dia.temperaturaMinimaCelsius,
            probabilidadLluviaPorcentaje = dia.probabilidadLluviaPorcentaje,
            precipitacionMilimetros = dia.precipitacionMilimetros,
            vientoMaximoKilometrosPorHora = dia.vientoMaximoKilometrosPorHora,
            rafagasMaximasKilometrosPorHora = dia.rafagasMaximasKilometrosPorHora,
            condicion = dia.condicion.name
        )
    }
)

// Una condición que esta versión no conoce se lee como variable en lugar de descartar todo el pronóstico
private fun String.aCondicion(): CondicionClima = CondicionClima.entries.firstOrNull { condicion -> condicion.name == this } ?: CondicionClima.VARIABLE

private fun PronosticoClimaJson.aDominio() = PronosticoClima(
    lugar = Coordenada(latitud, longitud),
    obtenidoMilisegundos = obtenidoMilisegundos,
    actual = ClimaActual(
        temperaturaCelsius = actual.temperaturaCelsius,
        sensacionTermicaCelsius = actual.sensacionTermicaCelsius,
        vientoKilometrosPorHora = actual.vientoKilometrosPorHora,
        rafagasKilometrosPorHora = actual.rafagasKilometrosPorHora,
        condicion = actual.condicion.aCondicion(),
        esDeDia = actual.esDeDia
    ),
    dias = dias.map { dia ->
        PronosticoDia(
            fecha = dia.fecha,
            temperaturaMaximaCelsius = dia.temperaturaMaximaCelsius,
            temperaturaMinimaCelsius = dia.temperaturaMinimaCelsius,
            probabilidadLluviaPorcentaje = dia.probabilidadLluviaPorcentaje,
            precipitacionMilimetros = dia.precipitacionMilimetros,
            vientoMaximoKilometrosPorHora = dia.vientoMaximoKilometrosPorHora,
            rafagasMaximasKilometrosPorHora = dia.rafagasMaximasKilometrosPorHora,
            condicion = dia.condicion.aCondicion()
        )
    }
)
