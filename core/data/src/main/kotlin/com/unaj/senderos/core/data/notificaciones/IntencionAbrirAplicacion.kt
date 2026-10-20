package com.unaj.senderos.core.data.notificaciones

import android.content.Context
import android.content.Intent

// La capa de datos no conoce MainActivity. Por lo tanto se abre la actividad lanzadora que declara :app
internal fun Context.intencionAbrirAplicacion(): Intent {
    val actividadLanzadora = requireNotNull(packageManager.getLaunchIntentForPackage(packageName)?.component) {
        "La aplicación no declara una actividad lanzadora"
    }
    return Intent().setComponent(actividadLanzadora)
}
