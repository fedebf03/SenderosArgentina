package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.port.LanzadorSeguimiento

class FalsoLanzadorSeguimiento(private val registro: MutableList<String> = mutableListOf()) : LanzadorSeguimiento {

    val idsIniciados: MutableList<String> = mutableListOf()
    val iniciosSimulados: MutableList<Boolean> = mutableListOf()
    var cantidadDetenciones: Int = 0
        private set

    override fun iniciar(idSendero: String, esSimulado: Boolean) {
        idsIniciados.add(idSendero)
        iniciosSimulados.add(esSimulado)
    }

    val caminatasLibresIniciadas: MutableList<Triple<String, String, String>> = mutableListOf()

    override fun iniciarCaminataLibre(idCaminata: String, idUsuario: String, nombre: String) {
        caminatasLibresIniciadas.add(Triple(idCaminata, idUsuario, nombre))
    }

    var cantidadCancelaciones: Int = 0
        private set

    override fun detener() {
        cantidadDetenciones++
        registro.add("lanzador.detener")
    }

    override fun cancelar() {
        cantidadCancelaciones++
        registro.add("lanzador.cancelar")
    }
}
