package com.unaj.senderos.core.data.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.unaj.senderos.core.data.sincronizacion.PlanificadorSincronizacion
import com.unaj.senderos.core.domain.port.Notificador
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

private const val TAG = "NOTIFICACIONES"

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DependenciasReintento {
    fun notificador(): Notificador
    fun obtenerUsuarioActual(): ObtenerUsuarioActualUseCase
    fun planificadorSincronizacion(): PlanificadorSincronizacion
}

// Atiende el botón "Reintentar" del aviso de error. Es un receptor interno (no exportado en el manifiesto)
class ReintentoSincronizacionReceptor : BroadcastReceiver() {

    override fun onReceive(contexto: Context, intencion: Intent) {
        val dependencias = EntryPointAccessors.fromApplication(contexto, DependenciasReintento::class.java)
        dependencias.notificador().cancelarAvisoDeError()
        val usuario = dependencias.obtenerUsuarioActual()()
        if (usuario == null) {
            Log.d(TAG, "Reintento omitido: no hay sesión activa")
            return
        }
        dependencias.planificadorSincronizacion().solicitarSincronizacion(usuario.id)
        Log.d(TAG, "Reintento de sincronización pedido desde la notificación")
    }
}
