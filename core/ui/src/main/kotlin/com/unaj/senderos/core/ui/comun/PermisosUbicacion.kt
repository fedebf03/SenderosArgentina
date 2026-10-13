package com.unaj.senderos.presentation.comun

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

val PERMISOS_UBICACION: Array<String> = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

fun tienePermisoDeUbicacion(contexto: Context): Boolean {
    return PERMISOS_UBICACION.any { permiso ->
        ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED
    }
}

// Es el permiso que exige el seguimiento del recorrido
fun tienePermisoDeUbicacionPrecisa(contexto: Context): Boolean {
    return ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
}
