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

// Perfil tal como queda en el dispositivo: la foto local y la remota se guardan por separado para saber si falta subirla
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
    // La foto que se muestra es la del dispositivo si hay, que se ve sin conexión, y si no la de la nube
    val fotoEfectiva: String
        get() = fotoLocal.ifBlank { fotoRemota }

    fun aPerfil() = PerfilUsuario(nombre, pais, provincia, localidad, fotoEfectiva)
}

// Guardar el perfil de cada usuario en un archivo propio de la carpeta privada de la app, con una copia en memoria: el perfil se lee
// seguido (cada vez que se pide el usuario actual) y el archivo es chico. Se escribe en un temporal y se renombra para no dejar un archivo a medias
@Singleton
class AlmacenPerfilLocal(private val carpeta: File) {

    @Inject
    constructor(@ApplicationContext contexto: Context) : this(File(contexto.applicationContext.filesDir, CARPETA_PERFILES))

    private val json = Json { ignoreUnknownKeys = true }
    private val enMemoria = mutableMapOf<String, PerfilGuardado?>()

    // Leer el perfil del usuario, de memoria o del archivo; nulo si nunca se guardó uno
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

    // Guardar el perfil; devuelve falso si no se pudo escribir, y en ese caso la copia en memoria no cambia
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
