package com.example.senderosargentina

import android.app.Application

// Aplicación que conserva el contenedor de dependencias durante todo el proceso
class SenderosAplicacion : Application() {
    val contenedorDependencias: ContenedorDependencias by lazy { ContenedorDependencias(this) }
}
