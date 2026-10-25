package com.unaj.senderos.core.data.repository

import android.util.Log
import com.unaj.senderos.core.domain.model.PerfilUsuario
import com.unaj.senderos.core.domain.model.ResultadoGuardadoPerfil
import com.unaj.senderos.core.domain.model.esFotoLocal
import com.unaj.senderos.core.domain.repository.PerfilRepositorio
import com.unaj.senderos.core.network.auth.FuenteAutenticacion
import com.unaj.senderos.core.network.perfil.PerfilFuenteRemota
import com.unaj.senderos.core.network.perfil.PerfilRed
import com.unaj.senderos.core.network.fotos.SubidorFotos
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import com.unaj.senderos.core.data.perfil.AlmacenPerfilLocal
import com.unaj.senderos.core.data.perfil.PerfilGuardado

private const val TAG = "PERFIL"

// El perfil se guarda siempre primero en el dispositivo como pendiente y después se intenta subir.
// Si no hay conexión o la foto no se puede subir, queda pendiente y se reintenta al abrir el Perfil
class PerfilRepositorioImpl @Inject constructor(
    private val almacenLocal: AlmacenPerfilLocal,
    private val fuentePerfil: PerfilFuenteRemota,
    private val fuenteAutenticacion: FuenteAutenticacion,
    private val subidorFotos: SubidorFotos
) : PerfilRepositorio {

    override suspend fun obtener(idUsuario: String): PerfilUsuario? {
        almacenLocal.leer(idUsuario)?.let { guardado -> return guardado.aPerfil() }
        val remoto = try {
            fuentePerfil.obtenerPerfil(idUsuario)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.d(TAG, "Sin perfil en la nube por ahora: ${error.message}")
            null
        } ?: return null
        val guardado = PerfilGuardado(remoto.nombre, remoto.pais, remoto.provincia, remoto.localidad, fotoRemota = remoto.foto)
        almacenLocal.guardar(idUsuario, guardado)
        return guardado.aPerfil()
    }

    override suspend fun guardar(idUsuario: String, perfil: PerfilUsuario): Result<ResultadoGuardadoPerfil> {
        val anterior = almacenLocal.leer(idUsuario)
        val nuevo = PerfilGuardado(
            nombre = perfil.nombre,
            pais = perfil.pais,
            provincia = perfil.provincia,
            localidad = perfil.localidad,
            fotoLocal = if (esFotoLocal(perfil.foto)) perfil.foto else "",
            // La URL de la nube sirve mientras la foto sea la misma. Una foto local nueva se tiene que volver a subir
            fotoRemota = when {
                perfil.foto.isBlank() -> ""
                esFotoLocal(perfil.foto) -> if (anterior?.fotoLocal == perfil.foto) anterior.fotoRemota else ""
                else -> perfil.foto
            },
            pendiente = true
        )
        if (!almacenLocal.guardar(idUsuario, nuevo)) {
            return Result.failure(IllegalStateException("No se pudo guardar el perfil en el dispositivo"))
        }
        return Result.success(ResultadoGuardadoPerfil(estaEnLaNube = subirALaNube(idUsuario, nuevo)))
    }

    override suspend fun sincronizarPendiente(idUsuario: String) {
        val guardado = almacenLocal.leer(idUsuario) ?: return
        if (guardado.pendiente) subirALaNube(idUsuario, guardado)
    }

    // Devuelve verdadero solo si no quedó nada pendiente de subir
    private suspend fun subirALaNube(idUsuario: String, guardado: PerfilGuardado): Boolean {
        return try {
            val conFoto = subirFotoSiFalta(idUsuario, guardado)
            fuentePerfil.guardarPerfil(
                idUsuario,
                PerfilRed(conFoto.nombre, conFoto.pais, conFoto.provincia, conFoto.localidad, conFoto.fotoRemota)
            )
            fuenteAutenticacion.actualizarNombre(conFoto.nombre).onFailure { error ->
                Log.e(TAG, "No se pudo actualizar el nombre de la cuenta", error)
            }
            val estaCompleto = conFoto.fotoLocal.isBlank() || conFoto.fotoRemota.isNotBlank()
            if (estaCompleto) almacenLocal.guardar(idUsuario, conFoto.copy(pendiente = false))
            estaCompleto
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "El perfil queda pendiente de subir", error)
            false
        }
    }

    // Si la foto no se puede subir (sin red o Cloudinary sin configurar), el perfil se sube igual sin ella
    private suspend fun subirFotoSiFalta(idUsuario: String, guardado: PerfilGuardado): PerfilGuardado {
        if (guardado.fotoLocal.isBlank() || guardado.fotoRemota.isNotBlank() || !File(guardado.fotoLocal).exists()) return guardado
        return try {
            val url = subidorFotos.subir(guardado.fotoLocal)
            guardado.copy(fotoRemota = url).also { conUrl -> almacenLocal.guardar(idUsuario, conUrl) }
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "No se pudo subir la foto del perfil; se reintentará", error)
            guardado
        }
    }
}
