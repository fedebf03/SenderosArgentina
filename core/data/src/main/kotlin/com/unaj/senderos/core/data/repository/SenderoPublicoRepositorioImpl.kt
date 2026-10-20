package com.unaj.senderos.core.data.repository

import android.util.Log
import com.unaj.senderos.core.data.mapeo.aDominio
import com.unaj.senderos.core.data.mapeo.aRed
import com.unaj.senderos.core.network.senderopublico.SenderoPublicoFuenteRemota
import com.unaj.senderos.core.domain.model.SenderoPublico
import com.unaj.senderos.core.domain.repository.SenderoPublicoRepositorio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import com.unaj.senderos.core.network.TipoErrorRemoto
import com.unaj.senderos.core.network.tipoErrorRemoto

private const val TAG = "SENDEROS_PUBLICOS"
private const val TIEMPO_MAXIMO_ESCRITURA_MILISEGUNDOS = 20_000L
private const val MENSAJE_SIN_RESPUESTA = "No se pudo completar. Probá de nuevo"
private const val MENSAJE_SIN_PERMISO = "No tenés permiso para hacer esto"

class SenderoPublicoRepositorioImpl(
    private val fuenteRemota: SenderoPublicoFuenteRemota,
    private val reloj: () -> Long = System::currentTimeMillis
) : SenderoPublicoRepositorio {

    override fun observarSenderosPublicos(): Flow<List<SenderoPublico>> {
        return fuenteRemota.observarTodos()
            .map { documentos -> documentos.map { documento -> documento.aDominio() } }
    }

    override fun observarSenderoPublico(idSendero: String): Flow<SenderoPublico?> {
        return fuenteRemota.observarUno(idSendero).map { documento -> documento?.aDominio() }
    }

    override suspend fun publicar(sendero: SenderoPublico): Result<Unit> {
        return ejecutar("publicar ${sendero.id}") {
            fuenteRemota.guardar(sendero.copy(fechaPublicacion = reloj()).aRed())
        }
    }

    override suspend fun despublicar(idSendero: String): Result<Unit> {
        return ejecutar("despublicar $idSendero") { fuenteRemota.eliminar(idSendero) }
    }

    private suspend fun ejecutar(accion: String, bloque: suspend () -> Unit): Result<Unit> {
        return try {
            withTimeoutOrNull(TIEMPO_MAXIMO_ESCRITURA_MILISEGUNDOS) { bloque() } ?: throw IllegalStateException(MENSAJE_SIN_RESPUESTA)
            Result.success(Unit)
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: Exception) {
            Log.e(TAG, "Falló al $accion", error)
            Result.failure(traducir(error))
        }
    }

    private fun traducir(error: Exception): Exception {
        return when (error.tipoErrorRemoto()) {
            TipoErrorRemoto.SIN_PERMISO -> IllegalStateException(MENSAJE_SIN_PERMISO, error)
            TipoErrorRemoto.SIN_RESPUESTA -> IllegalStateException(MENSAJE_SIN_RESPUESTA, error)
            else -> error
        }
    }
}
