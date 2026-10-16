package com.unaj.senderos.core.ui.componentes.mapa

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.maps.MapView

@Composable
internal fun VincularCicloDeVidaMapa(vistaMapa: MapView, alIniciar: () -> Unit, alLiberar: () -> Unit) {
    val contexto = LocalContext.current
    val propietarioDelCiclo = LocalLifecycleOwner.current
    DisposableEffect(propietarioDelCiclo, vistaMapa) {
        val ciclo = propietarioDelCiclo.lifecycle
        var estaCreado = false
        var estaIniciado = false
        var estaReanudado = false
        var estaDestruido = false
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_CREATE -> {
                    vistaMapa.onCreate(null)
                    estaCreado = true
                }
                Lifecycle.Event.ON_START -> {
                    vistaMapa.onStart()
                    estaIniciado = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    vistaMapa.onResume()
                    estaReanudado = true
                }
                Lifecycle.Event.ON_PAUSE -> {
                    vistaMapa.onPause()
                    estaReanudado = false
                }
                Lifecycle.Event.ON_STOP -> {
                    vistaMapa.onStop()
                    estaIniciado = false
                }
                Lifecycle.Event.ON_DESTROY -> {
                    vistaMapa.onDestroy()
                    estaDestruido = true
                }
                else -> Unit
            }
        }
        val llamadasDeMemoria = object : ComponentCallbacks2 {
            // Ignorar los recortes de memoria graduales; solo se atiende la memoria baja
            override fun onTrimMemory(nivel: Int) = Unit

            // Ignorar los cambios de configuración, que el MapView resuelve solo
            override fun onConfigurationChanged(configuracion: Configuration) = Unit

            override fun onLowMemory() {
                vistaMapa.onLowMemory()
            }
        }
        val contextoDeAplicacion = contexto.applicationContext
        contextoDeAplicacion.registerComponentCallbacks(llamadasDeMemoria)
        ciclo.addObserver(observador)
        alIniciar()
        onDispose {
            contextoDeAplicacion.unregisterComponentCallbacks(llamadasDeMemoria)
            ciclo.removeObserver(observador)
            alLiberar()
            if (estaReanudado) vistaMapa.onPause()
            if (estaIniciado) vistaMapa.onStop()
            if (estaCreado && !estaDestruido) vistaMapa.onDestroy()
        }
    }
}
