package com.unaj.senderos.core.data.perfil

import android.content.Context
import android.util.Log
import com.unaj.senderos.core.domain.model.PerfilUsuario
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "PERFIL_LOCAL"
private const val CARPETA_PERFILES = "perfiles"
private const val SUFIJO_TEMPORAL = ".tmp"

// La foto local y la remota se guardan por separado para saber si todavía falta subirla
@Serializable
data class PerfilGuardado(
    val nombre: String = "",
    val pais: String = "",
    val provincia: String = "",
    val localidad: String = "",
    val fotoLocal: String = "",
    val fotoRemota: String = "",
    val pendiente: Boolean = false
) {
    // Se muestra la foto del dispositivo si hay, porque se ve sin conexión. Si no, se muestra la de la nube
    val fotoEfectiva: String
        get() = fotoLocal.ifBlank { fotoRemota }

    fun aPerfil() = PerfilUsuario(nombre, pais, provincia, localidad, fotoEfectiva)
}

// Tenemos una copia en memoria porque el perfil se lee cada vez que se pide el usuario actual y el archivo es chico.
// Por otro lado se escribe en un temporal y se renombra para no dejar un archivo a medias
@Singleton
class AlmacenPerfilLocal(private val carpeta: File) {

    @Inject
    constructor(@ApplicationContext contexto: Context) : this(File(contexto.applicationContext.filesDir, CARPETA_PERFILES))

    private val json = Json { ignoreUnknownKeys = true }
    private val enMemoria = mutableMapOf<String, PerfilGuardado?>()

    @Synchronized
    fun leer(idUsuario: String): PerfilGuardado? {
        if (enMemoria.containsKey(idUsuario)) return enMemoria[idUsuario]
        val leido = try {
            val archivo = archivoDe(idUsuario)
            if (archivo.exists()) json.decodeFromString(PerfilGuardado.serializer(), archivo.readText()) else null
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "No se pudo leer el perfil guardado", error)
            null
        }
        enMemoria[idUsuario] = leido
        return leido
    }

    // Si no se pudo escribir el archivo devuelve falso y la copia en memoria queda como estaba
    @Synchronized
    fun guardar(idUsuario: String, perfil: PerfilGuardado): Boolean {
        return try {
            carpeta.mkdirs()
            val archivo = archivoDe(idUsuario)
            val temporal = File(carpeta, archivo.name + SUFIJO_TEMPORAL)
            temporal.writeText(json.encodeToString(PerfilGuardado.serializer(), perfil))
            if (!temporal.renameTo(archivo)) {
                temporal.copyTo(archivo, overwrite = true)
                temporal.delete()
            }
            enMemoria[idUsuario] = perfil
            true
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "No se pudo guardar el perfil", error)
            false
        }
    }

    private fun archivoDe(idUsuario: String) = File(carpeta, "perfil_$idUsuario.json")
}
