package com.unaj.senderos.core.domain.model

// La foto queda como ruta local hasta que la sincronización la reemplaza por la URL remota
fun esFotoLocal(foto: String): Boolean {
    return foto.isNotEmpty() && !foto.startsWith("http://") && !foto.startsWith("https://")
}
