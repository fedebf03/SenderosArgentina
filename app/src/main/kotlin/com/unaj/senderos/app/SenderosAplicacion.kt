package com.unaj.senderos.app

import android.app.Application
import com.unaj.senderos.di.ContenedorDependencias

class SenderosAplicacion : Application() {
    val contenedorDependencias: ContenedorDependencias by lazy { ContenedorDependencias(this) }
}
