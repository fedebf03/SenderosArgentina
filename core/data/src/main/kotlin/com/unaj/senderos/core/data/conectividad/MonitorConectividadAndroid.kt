package com.unaj.senderos.core.data.conectividad

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.unaj.senderos.core.domain.port.MonitorConectividad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.unaj.senderos.core.data.di.AlcanceEscrituras

private const val TAG = "CONECTIVIDAD"
private const val ESPERA_SIN_SUSCRIPTORES_MILISEGUNDOS = 5_000L

// Todos los suscriptores comparten una sola escucha de red en un alcance que vive tanto como la aplicación.
// Con WhileSubscribed la escucha se registra con el primero y se libera 5 segundos después del último
class MonitorConectividadAndroid @Inject constructor(
    @ApplicationContext contexto: Context,
    @AlcanceEscrituras alcance: CoroutineScope
) : MonitorConectividad {
    private val administradorConectividad =
        contexto.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val conexionCompartida: StateFlow<Boolean> = crearFlujoDeConexion().stateIn(
        scope = alcance,
        started = SharingStarted.WhileSubscribed(ESPERA_SIN_SUSCRIPTORES_MILISEGUNDOS),
        initialValue = hayConexion()
    )

    override fun hayConexion(): Boolean {
        return try {
            val red = administradorConectividad?.activeNetwork ?: return false
            val capacidades = administradorConectividad.getNetworkCapabilities(red) ?: return false
            tieneInternetValidado(capacidades)
        } catch (error: SecurityException) {
            Log.e(TAG, "Sin permiso para consultar el estado de la red", error)
            false
        }
    }

    override fun observarConexion(): StateFlow<Boolean> = conexionCompartida

    private fun tieneInternetValidado(capacidades: NetworkCapabilities): Boolean {
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun crearFlujoDeConexion(): Flow<Boolean> = callbackFlow {
        val escucha = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(tieneInternetValidado(networkCapabilities))
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        trySend(hayConexion())
        try {
            administradorConectividad?.registerDefaultNetworkCallback(escucha)
        } catch (error: SecurityException) {
            Log.e(TAG, "Sin permiso para observar el estado de la red", error)
        }
        awaitClose {
            try {
                administradorConectividad?.unregisterNetworkCallback(escucha)
            } catch (error: IllegalArgumentException) {
                Log.e(TAG, "La escucha de red ya no estaba registrada", error)
            }
        }
    }.distinctUntilChanged()
}
