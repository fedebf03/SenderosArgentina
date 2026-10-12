package com.unaj.senderos.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.unaj.senderos.domain.repository.AlmacenFotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

private const val LADO_MAXIMO_PIXELES = 1600
private const val CALIDAD_JPEG = 85

class AlmacenFotosLocal(contexto: Context) : AlmacenFotos {

    private val contexto = contexto.applicationContext
    private val carpetaFotos = File(this.contexto.filesDir, "fotos_senderos")

    // Guardar una copia achicada de la imagen elegida en la carpeta privada y devolver su ruta
    override suspend fun guardarCopia(uriOrigen: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val imagen = decodificarAchicada(Uri.parse(uriOrigen))
            carpetaFotos.mkdirs()
            val destino = File(carpetaFotos, "${UUID.randomUUID()}.jpg")
            destino.outputStream().use { salida -> imagen.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida) }
            imagen.recycle()
            Result.success(destino.absolutePath)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // Decodificar la imagen limitando su lado mayor para no guardar ni subir archivos de varios megas
    private fun decodificarAchicada(uri: Uri): Bitmap {
        // ImageDecoder respeta la orientación EXIF de las fotos de cámara; BitmapFactory no
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val fuente = ImageDecoder.createSource(contexto.contentResolver, uri)
            return ImageDecoder.decodeBitmap(fuente) { decodificador, informacion, _ ->
                val ancho = informacion.size.width
                val alto = informacion.size.height
                val escala = LADO_MAXIMO_PIXELES.toFloat() / max(ancho, alto)
                if (escala < 1f) {
                    decodificador.setTargetSize((ancho * escala).roundToInt(), (alto * escala).roundToInt())
                }
                decodificador.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }

        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contexto.contentResolver.openInputStream(uri).use { entrada -> BitmapFactory.decodeStream(entrada, null, limites) }
        var muestreo = 1
        while (max(limites.outWidth, limites.outHeight) / (muestreo * 2) >= LADO_MAXIMO_PIXELES) {
            muestreo *= 2
        }
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        val imagen = contexto.contentResolver.openInputStream(uri).use { entrada ->
            BitmapFactory.decodeStream(entrada, null, opciones)
        } ?: error("No se pudo leer la imagen elegida")
        return rotarSegunExif(imagen, uri)
    }

    // Rotar o espejar el mapa de bits según la orientación EXIF, que BitmapFactory ignora
    private fun rotarSegunExif(imagen: Bitmap, uri: Uri): Bitmap {
        val orientacion = contexto.contentResolver.openInputStream(uri).use { entrada ->
            if (entrada == null) ExifInterface.ORIENTATION_NORMAL else ExifInterface(entrada).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
        val matriz = Matrix()
        when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matriz.postRotate(90f)
                matriz.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matriz.postRotate(270f)
                matriz.postScale(-1f, 1f)
            }
            else -> return imagen
        }
        val girada = Bitmap.createBitmap(imagen, 0, 0, imagen.width, imagen.height, matriz, true)
        if (girada !== imagen) imagen.recycle()
        return girada
    }
}
