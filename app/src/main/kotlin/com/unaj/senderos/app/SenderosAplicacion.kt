package com.example.senderosargentina

import android.app.Application

class SenderosAplicacion : Application() {
    val contenedorDependencias: ContenedorDependencias by lazy { ContenedorDependencias(this) }
}
