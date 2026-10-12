package com.unaj.senderos.data.ubicacion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.repository.ProveedorUbicacion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "UBICACION"

// Obtener la ubicación del dispositivo con el proveedor combinado de Google Play Services
class ProveedorUbicacionCombinada(private val contexto: Context) : ProveedorUbicacion {

    // Obtener la ubicación actual; éxito con null si falta el permiso o no hay ubicación disponible
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun obtenerUbicacionActual(): Result<Coordenada?> {
        if (!tienePermisoDeUbicacion()) {
            Log.d(TAG, "Ubicación omitida: falta el permiso de ubicación")
            return Result.success(null)
        }
        return try {
            val cliente = LocationServices.getFusedLocationProviderClient(contexto)
            val fuenteCancelacion = CancellationTokenSource()
            val ubicacion: Location? = cliente
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, fuenteCancelacion.token)
                .await(fuenteCancelacion)
                ?: cliente.lastLocation.await()
            Log.d(TAG, "Ubicación obtenida: ${ubicacion != null}")
            Result.success(ubicacion?.let { posicion -> Coordenada(posicion.latitude, posicion.longitude) })
        } catch (excepcion: CancellationException) {
            throw excepcion
        } catch (error: SecurityException) {
            Log.e(TAG, "Permiso de ubicación revocado durante la consulta", error)
            Result.failure(error)
        } catch (error: Exception) {
            Log.e(TAG, "Fallo al obtener la ubicación", error)
            Result.failure(error)
        }
    }

    // Verificar si hay permiso de ubicación precisa o aproximada
    private fun tienePermisoDeUbicacion(): Boolean {
        val permisos = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        return permisos.any { permiso ->
            ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED
        }
    }
}
