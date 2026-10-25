package com.unaj.senderos.core.ui.comun

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val TAG = "CAPTURA_FOTO"
private const val UNA_HORA_EN_MILISEGUNDOS = 3_600_000L

class AccionesDeFoto(val sacarFoto: () -> Unit, val elegirDeGaleria: () -> Unit)

// RF6 - Cámara: la foto se toma sobre un archivo temporal que compartimos con nuestro FileProvider
@Composable
fun recordarAccionesDeFoto(
    alSeleccionarFoto: (String) -> Unit,
    alMostrarAviso: suspend (String) -> Unit
): AccionesDeFoto {
    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()
    val alSeleccionarFotoActual by rememberUpdatedState(alSeleccionarFoto)
    val alMostrarAvisoActual by rememberUpdatedState(alMostrarAviso)
    var rutaFotoPendiente by rememberSaveable { mutableStateOf<String?>(null) }

    val selectorGaleria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) alSeleccionarFotoActual(uri.toString())
    }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { seTomoFoto ->
        val rutaPendiente = rutaFotoPendiente
        rutaFotoPendiente = null
        alcanceCorrutina.launch {
            if (rutaPendiente != null && seTomoFoto) {
                try {
                    val uri = obtenerUriDeFoto(contexto, File(rutaPendiente))
                    alSeleccionarFotoActual(uri.toString())
                } catch (error: IllegalArgumentException) {
                    Log.e(TAG, "No se pudo compartir la foto de la cámara", error)
                    alMostrarAvisoActual("No se pudo leer la foto tomada")
                }
            } else {
                if (rutaPendiente != null) borrarArchivo(rutaPendiente)
                Log.d(TAG, "Captura de foto cancelada")
            }
        }
    }

    return remember(contexto, alcanceCorrutina, selectorGaleria, camara) {
        AccionesDeFoto(
            sacarFoto = {
                alcanceCorrutina.launch {
                    try {
                        val archivo = withContext(Dispatchers.IO) { crearArchivoTemporalDeFoto(contexto) }
                        rutaFotoPendiente = archivo.absolutePath
                        camara.launch(obtenerUriDeFoto(contexto, archivo))
                    } catch (excepcion: ActivityNotFoundException) {
                        Log.e(TAG, "No hay una aplicación de cámara disponible", excepcion)
                        rutaFotoPendiente?.let { ruta -> borrarArchivo(ruta) }
                        rutaFotoPendiente = null
                        alMostrarAvisoActual("No hay una aplicación de cámara disponible")
                    } catch (error: IOException) {
                        Log.e(TAG, "No se pudo preparar el archivo de la foto", error)
                        alMostrarAvisoActual("No se pudo preparar la cámara")
                    } catch (error: IllegalArgumentException) {
                        Log.e(TAG, "No se pudo compartir el archivo de la foto", error)
                        alMostrarAvisoActual("No se pudo preparar la cámara")
                    }
                }
            },
            elegirDeGaleria = {
                selectorGaleria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }
}

// Se llama fuera del hilo principal porque crea el archivo en disco
private fun crearArchivoTemporalDeFoto(contexto: Context): File {
    val carpeta = File(contexto.cacheDir, "fotos_camara")
    carpeta.mkdirs()
    return File.createTempFile("foto_", ".jpg", carpeta)
}

suspend fun limpiarFotosTemporalesViejas(contexto: Context) {
    withContext(Dispatchers.IO) {
        val limite = System.currentTimeMillis() - UNA_HORA_EN_MILISEGUNDOS
        File(contexto.cacheDir, "fotos_camara").listFiles()
            ?.filter { archivo -> archivo.lastModified() < limite }
            ?.forEach { archivo -> archivo.delete() }
    }
}

private fun obtenerUriDeFoto(contexto: Context, archivo: File): Uri {
    return FileProvider.getUriForFile(contexto, "${contexto.packageName}.proveedorarchivos", archivo)
}

private suspend fun borrarArchivo(ruta: String) {
    withContext(Dispatchers.IO) { File(ruta).delete() }
}
