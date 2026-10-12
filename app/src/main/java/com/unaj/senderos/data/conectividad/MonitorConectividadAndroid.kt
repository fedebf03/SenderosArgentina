package com.unaj.senderos.data.conectividad

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.unaj.senderos.domain.repository.MonitorConectividad

private const val TAG = "CONECTIVIDAD"

class MonitorConectividadAndroid(contexto: Context) : MonitorConectividad {
    private val administradorConectividad =
        contexto.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    // Indicar si la red activa declara salida a Internet; no valida que el servidor sea alcanzable
    override fun hayConexion(): Boolean {
        return try {
            val red = administradorConectividad?.activeNetwork ?: return false
            val capacidades = administradorConectividad.getNetworkCapabilities(red) ?: return false
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (error: SecurityException) {
            Log.e(TAG, "Sin permiso para consultar el estado de la red", error)
            false
        }
    }
}
