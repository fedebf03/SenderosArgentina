package com.unaj.senderos.presentation.comun

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

val PERMISOS_UBICACION: Array<String> = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

// Verificar si la aplicación tiene al menos un permiso de ubicación (precisa o aproximada)
fun tienePermisoDeUbicacion(contexto: Context): Boolean {
    return PERMISOS_UBICACION.any { permiso ->
        ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED
    }
}
