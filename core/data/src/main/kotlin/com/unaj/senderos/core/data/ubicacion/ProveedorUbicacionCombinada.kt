package com.unaj.senderos.data.ubicacion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.unaj.senderos.domain.CalculadoraRecorrido
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.repository.ProveedorUbicacion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "UBICACION"
private const val INTERVALO_UBICACION_MILISEGUNDOS = 5_000L
private const val PRECISION_MAXIMA_METROS = 30f

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

    // Observar las ubicaciones continuas de alta precisión (GPS primero) y quitar el escucha al cancelar la recolección
    override fun observarUbicaciones(): Flow<LecturaUbicacion> = callbackFlow {
        if (!tienePermisoDeUbicacionPrecisa()) {
            val error = SecurityException("Se requiere el permiso de ubicación precisa (ACCESS_FINE_LOCATION) para seguir el recorrido")
            Log.e(TAG, "No se puede observar la ubicación", error)
            close(error)
            return@callbackFlow
        }
        val cliente = LocationServices.getFusedLocationProviderClient(contexto)
        val peticion = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVALO_UBICACION_MILISEGUNDOS).build()
        val escucha = object : LocationCallback() {
            // Emitir la última posición recibida como coordenada de dominio
            override fun onLocationResult(resultado: LocationResult) {
                resultado.lastLocation?.let { posicion ->
                    if (posicion.hasAccuracy() && posicion.accuracy > PRECISION_MAXIMA_METROS) {
                        Log.d(TAG, "Lectura descartada por baja precisión: ${posicion.accuracy} m")
                    } else {
                        trySend(convertirALectura(posicion))
                    }
                }
            }
        }
        try {
            cliente.requestLocationUpdates(peticion, escucha, Looper.getMainLooper())
                .addOnFailureListener { error ->
                    Log.e(TAG, "El sistema rechazó las actualizaciones de ubicación", error)
                    close(error)
                }
            Log.d(TAG, "Actualizaciones de ubicación solicitadas")
        } catch (error: SecurityException) {
            Log.e(TAG, "Permiso de ubicación revocado al solicitar actualizaciones", error)
            close(error)
        }
        awaitClose {
            cliente.removeLocationUpdates(escucha)
            Log.d(TAG, "Actualizaciones de ubicación detenidas")
        }
    }

    // Convertir una ubicación del sistema en lectura de dominio; rumbo y velocidad solo si el sistema los informa
    private fun convertirALectura(posicion: Location): LecturaUbicacion {
        return LecturaUbicacion(
            coordenada = Coordenada(posicion.latitude, posicion.longitude),
            rumboGrados = if (posicion.hasBearing()) CalculadoraRecorrido.normalizarRumbo(posicion.bearing) else null,
            velocidadMetrosPorSegundo = if (posicion.hasSpeed()) posicion.speed else null,
            precisionMetros = if (posicion.hasAccuracy()) posicion.accuracy else null
        )
    }

    // Verificar si hay permiso de ubicación precisa, necesario para el seguimiento con GPS
    private fun tienePermisoDeUbicacionPrecisa(): Boolean {
        return ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    // Verificar si hay permiso de ubicación precisa o aproximada
    private fun tienePermisoDeUbicacion(): Boolean {
        val permisos = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        return permisos.any { permiso ->
            ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED
        }
    }
}
