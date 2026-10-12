package com.unaj.senderos.data.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.senderosargentina.SenderosAplicacion

private const val TAG = "NOTIFICACIONES"

// Receptor interno (no exportado) de la acción "Reintentar" de la notificación de error: encola de nuevo la sincronización del usuario
class ReintentoSincronizacionReceptor : BroadcastReceiver() {

    // Quitar el aviso de error y, si hay sesión activa, encolar la sincronización con el planificador compartido del contenedor
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
