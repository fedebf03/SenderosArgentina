package com.unaj.senderos.core.data.archivos

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.unaj.senderos.core.domain.port.ArchivoLeido
import com.unaj.senderos.core.domain.port.LectorArchivos
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "LECTOR_ARCHIVOS"
// Un GPX de varios días con un punto por segundo ronda los 10 MB. Un archivo más grande no es un trazado de sendero
private const val TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024
private const val MENSAJE_ARCHIVO_GRANDE = "El archivo es demasiado grande para un trazado (más de 10 MB)"
private const val MENSAJE_NO_SE_PUDO_LEER = "No se pudo leer el archivo"

class LectorArchivosAndroid @Inject constructor(@ApplicationContext contexto: Context) : LectorArchivos {

    private val contexto = contexto.applicationContext

    // RF4 - Importar GPX: leemos el archivo elegido y controlamos el tamaño antes de cargarlo en memoria
    override suspend fun leerTexto(uri: String): Result<ArchivoLeido> = withContext(Dispatchers.IO) {
        try {
            val direccion = Uri.parse(uri)
            val (nombre, tamano) = leerNombreYTamano(direccion)
            if (tamano != null && tamano > TAMANO_MAXIMO_BYTES) return@withContext Result.failure(IllegalArgumentException(MENSAJE_ARCHIVO_GRANDE))
            val texto = contexto.contentResolver.openInputStream(direccion)?.use { flujo ->
                val bytes = flujo.readNBytesHasta(TAMANO_MAXIMO_BYTES + 1)
                if (bytes.size > TAMANO_MAXIMO_BYTES) return@withContext Result.failure(IllegalArgumentException(MENSAJE_ARCHIVO_GRANDE))
                String(bytes, Charsets.UTF_8)
            } ?: return@withContext Result.failure(IllegalStateException(MENSAJE_NO_SE_PUDO_LEER))
            Log.d(TAG, "Archivo leído: $nombre (${texto.length} caracteres)")
            Result.success(ArchivoLeido(nombre, texto))
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: IOException) {
            Log.e(TAG, "No se pudo leer el archivo", error)
            Result.failure(IllegalStateException(MENSAJE_NO_SE_PUDO_LEER, error))
        } catch (error: RuntimeException) {
            Log.e(TAG, "El proveedor del archivo falló", error)
            Result.failure(IllegalStateException(MENSAJE_NO_SE_PUDO_LEER, error))
        }
    }

    private fun leerNombreYTamano(direccion: Uri): Pair<String, Long?> {
        contexto.contentResolver.query(direccion, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val indiceNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val indiceTamano = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val nombre = if (indiceNombre >= 0) cursor.getString(indiceNombre) else null
                    val tamano = if (indiceTamano >= 0 && !cursor.isNull(indiceTamano)) cursor.getLong(indiceTamano) else null
                    return (nombre ?: direccion.lastPathSegment.orEmpty()) to tamano
                }
            }
        return direccion.lastPathSegment.orEmpty() to null
    }
}

// Se lee como máximo esa cantidad de bytes para no cargar en memoria un archivo enorme que no informó su tamaño
private fun java.io.InputStream.readNBytesHasta(maximo: Long): ByteArray {
    val salida = java.io.ByteArrayOutputStream()
    val bloque = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (total < maximo) {
        val leidos = read(bloque, 0, minOf(bloque.size.toLong(), maximo - total).toInt())
        if (leidos < 0) break
        salida.write(bloque, 0, leidos)
        total += leidos
    }
    return salida.toByteArray()
}
