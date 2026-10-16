package com.unaj.senderos.core.network

import com.unaj.senderos.core.network.di.NombreNubeCloudinary
import com.unaj.senderos.core.network.di.PresetSubidaCloudinary
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val CARPETA_CLOUDINARY = "senderos"
private const val SALTO = "\r\n"

// Subir fotos a Cloudinary con un upload preset sin firma, así la app no necesita guardar el secreto de la API
class SubidorFotosCloudinary @Inject constructor(
    @param:NombreNubeCloudinary private val nombreNube: String,
    @param:PresetSubidaCloudinary private val presetSubida: String
) : SubidorFotos {

    override suspend fun subir(rutaLocal: String): String = withContext(Dispatchers.IO) {
        check(nombreNube.isNotBlank() && presetSubida.isNotBlank()) {
            "Cloudinary no está configurado: completá cloudinary.nombreNube y cloudinary.presetSubida en local.properties"
        }
        val archivo = File(rutaLocal)
        check(archivo.exists()) { "No existe la foto local $rutaLocal" }

        val separador = "----SenderosArgentina${UUID.randomUUID()}"
        val conexion = URL("https://api.cloudinary.com/v1_1/$nombreNube/image/upload").openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "POST"
            conexion.doOutput = true
            conexion.connectTimeout = 15_000
            conexion.readTimeout = 60_000
            conexion.setRequestProperty("Content-Type", "multipart/form-data; boundary=$separador")

            DataOutputStream(conexion.outputStream).use { salida ->
                escribirCampo(salida, separador, "upload_preset", presetSubida)
                escribirCampo(salida, separador, "folder", CARPETA_CLOUDINARY)
                salida.writeBytes("--$separador$SALTO")
                salida.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${archivo.name}\"$SALTO")
                salida.writeBytes("Content-Type: image/jpeg$SALTO$SALTO")
                archivo.inputStream().use { entrada -> entrada.copyTo(salida) }
                salida.writeBytes("$SALTO--$separador--$SALTO")
            }

            val codigo = conexion.responseCode
            val cuerpo = (if (codigo in 200..299) conexion.inputStream else conexion.errorStream)
                ?.bufferedReader()?.use { lector -> lector.readText() }
                .orEmpty()
            if (codigo !in 200..299) {
                throw IOException("Cloudinary rechazó la foto ($codigo): $cuerpo")
            }
            JSONObject(cuerpo).getString("secure_url")
        } finally {
            conexion.disconnect()
        }
    }

    private fun escribirCampo(salida: DataOutputStream, separador: String, nombre: String, valor: String) {
        salida.writeBytes("--$separador$SALTO")
        salida.writeBytes("Content-Disposition: form-data; name=\"$nombre\"$SALTO$SALTO")
        salida.writeBytes("$valor$SALTO")
    }
}
