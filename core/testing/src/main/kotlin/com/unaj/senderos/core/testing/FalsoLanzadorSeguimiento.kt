package com.unaj.senderos.falsos

import com.unaj.senderos.domain.port.LanzadorSeguimiento

class FalsoLanzadorSeguimiento(private val registro: MutableList<String> = mutableListOf()) : LanzadorSeguimiento {

    val idsIniciados: MutableList<String> = mutableListOf()
    val iniciosSimulados: MutableList<Boolean> = mutableListOf()
    var cantidadDetenciones: Int = 0
        private set

    // Registrar el sendero cuyo seguimiento se pidió iniciar
    override fun iniciar(idSendero: String, esSimulado: Boolean) {
        idsIniciados.add(idSendero)
        iniciosSimulados.add(esSimulado)
    }

    var cantidadCancelaciones: Int = 0
        private set

    // Contar cada pedido de detener el seguimiento
    override fun detener() {
        cantidadDetenciones++
        registro.add("lanzador.detener")
    }

    // Contar cada pedido de cancelar el seguimiento
    override fun cancelar() {
        cantidadCancelaciones++
        registro.add("lanzador.cancelar")
    }
}
