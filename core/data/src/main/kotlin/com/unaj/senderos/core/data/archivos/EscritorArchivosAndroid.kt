package com.unaj.senderos.core.data.archivos

import android.content.Context
import android.net.Uri
import android.util.Log
import com.unaj.senderos.core.domain.port.EscritorArchivos
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "ESCRITOR_ARCHIVOS"
private const val MENSAJE_NO_SE_PUDO_ESCRIBIR = "No se pudo guardar el archivo"

class EscritorArchivosAndroid @Inject constructor(@ApplicationContext contexto: Context) : EscritorArchivos {

    private val contexto = contexto.applicationContext

    // El modo "wt" reemplaza el contenido si el archivo elegido ya existía
    override suspend fun escribirTexto(uri: String, texto: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val flujo = contexto.contentResolver.openOutputStream(Uri.parse(uri), "wt")
                ?: return@withContext Result.failure(IllegalStateException(MENSAJE_NO_SE_PUDO_ESCRIBIR))
            flujo.use { salida -> salida.write(texto.toByteArray(Charsets.UTF_8)) }
            Log.d(TAG, "Archivo guardado (${texto.length} caracteres)")
            Result.success(Unit)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: IOException) {
            Log.e(TAG, "No se pudo escribir el archivo", error)
            Result.failure(IllegalStateException(MENSAJE_NO_SE_PUDO_ESCRIBIR, error))
        } catch (error: RuntimeException) {
            Log.e(TAG, "El proveedor del archivo falló", error)
            Result.failure(IllegalStateException(MENSAJE_NO_SE_PUDO_ESCRIBIR, error))
        }
    }
}
