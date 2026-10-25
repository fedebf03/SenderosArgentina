package com.unaj.senderos.core.data.preferencias

import android.content.Context
import android.util.Log
import com.unaj.senderos.core.domain.port.PreferenciasAplicacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "PREFERENCIAS"
private const val NOMBRE_ARCHIVO = "preferencias_aplicacion"
private const val CLAVE_MODO_DEMOSTRACION = "modo_demostracion"
private const val CLAVE_MODO_OSCURO = "modo_oscuro"

class PreferenciasAplicacionLocal @Inject constructor(@ApplicationContext contexto: Context) : PreferenciasAplicacion {

    private val preferencias by lazy { contexto.applicationContext.getSharedPreferences(NOMBRE_ARCHIVO, Context.MODE_PRIVATE) }
    private val modoDemostracion by lazy { MutableStateFlow(preferencias.getBoolean(CLAVE_MODO_DEMOSTRACION, false)) }
    private val modoOscuro by lazy { MutableStateFlow(preferencias.getBoolean(CLAVE_MODO_OSCURO, false)) }

    // Las preferencias se leen recién en el primer acceso y no mientras se construye el grafo de dependencias
    override val esModoDemostracion: StateFlow<Boolean> get() = modoDemostracion

    override val esModoOscuro: StateFlow<Boolean> get() = modoOscuro

    override fun cambiarModoDemostracion(activo: Boolean) {
        preferencias.edit().putBoolean(CLAVE_MODO_DEMOSTRACION, activo).apply()
        modoDemostracion.value = activo
        Log.d(TAG, "Modo demostración: $activo")
    }

    override fun cambiarModoOscuro(activo: Boolean) {
        preferencias.edit().putBoolean(CLAVE_MODO_OSCURO, activo).apply()
        modoOscuro.value = activo
        Log.d(TAG, "Modo oscuro: $activo")
    }
}
