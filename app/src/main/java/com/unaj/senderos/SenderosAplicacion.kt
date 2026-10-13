package com.unaj.senderos

import android.app.Application

class SenderosAplicacion : Application() {
    val contenedorDependencias: ContenedorDependencias by lazy { ContenedorDependencias(this) }
}
