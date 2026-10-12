package com.unaj.senderos.data.preferencias

import android.content.Context
import android.util.Log
import com.unaj.senderos.domain.repository.PreferenciasAplicacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "PREFERENCIAS"
private const val NOMBRE_ARCHIVO = "preferencias_aplicacion"
private const val CLAVE_MODO_DEMOSTRACION = "modo_demostracion"

// Guardar las preferencias de la aplicación solo en este dispositivo, sin sincronizarlas
class PreferenciasAplicacionLocal(contexto: Context) : PreferenciasAplicacion {

    private val preferencias by lazy { contexto.applicationContext.getSharedPreferences(NOMBRE_ARCHIVO, Context.MODE_PRIVATE) }
    private val modoDemostracion by lazy { MutableStateFlow(preferencias.getBoolean(CLAVE_MODO_DEMOSTRACION, false)) }

    // Leer las preferencias recién al primer acceso, fuera de la construcción del contenedor
    override val esModoDemostracion: StateFlow<Boolean> get() = modoDemostracion

    // Guardar el valor del modo demostración y publicarlo a los observadores
    override fun cambiarModoDemostracion(activo: Boolean) {
        preferencias.edit().putBoolean(CLAVE_MODO_DEMOSTRACION, activo).apply()
        modoDemostracion.value = activo
        Log.d(TAG, "Modo demostración: $activo")
    }
}
