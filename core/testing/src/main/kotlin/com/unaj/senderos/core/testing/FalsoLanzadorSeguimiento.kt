package com.unaj.senderos.falsos

import com.unaj.senderos.domain.repository.LanzadorSeguimiento

class FalsoLanzadorSeguimiento : LanzadorSeguimiento {

    val idsIniciados: MutableList<String> = mutableListOf()
    val iniciosSimulados: MutableList<Boolean> = mutableListOf()
    var cantidadDetenciones: Int = 0
        private set

    // Registrar el sendero cuyo seguimiento se pidió iniciar
    override fun iniciar(idSendero: String, esSimulado: Boolean) {
        idsIniciados.add(idSendero)
        iniciosSimulados.add(esSimulado)
    }

    // Contar cada pedido de detener el seguimiento
    override fun detener() {
        cantidadDetenciones++
    }
}
