package com.unaj.senderos.data.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.unaj.senderos.SenderosAplicacion

private const val TAG = "NOTIFICACIONES"

// Receptor interno (no exportado) de la acción "Reintentar" de la notificación de error
class ReintentoSincronizacionReceptor : BroadcastReceiver() {

    override fun onReceive(contexto: Context, intencion: Intent) {
        val contenedor = (contexto.applicationContext as SenderosAplicacion).contenedorDependencias
        contenedor.notificador.cancelarAvisoDeError()
        val usuario = contenedor.obtenerUsuarioActualUseCase()
        if (usuario == null) {
            Log.d(TAG, "Reintento omitido: no hay sesión activa")
            return
        }
        contenedor.planificadorSincronizacion.solicitarSincronizacion(usuario.id)
        Log.d(TAG, "Reintento de sincronización pedido desde la notificación")
    }
}
