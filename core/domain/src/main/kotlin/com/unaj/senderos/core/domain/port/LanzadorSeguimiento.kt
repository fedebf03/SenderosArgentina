package com.unaj.senderos.core.domain.port

interface LanzadorSeguimiento {

    fun iniciar(idSendero: String, esSimulado: Boolean = false)

    fun detener()

    fun cancelar()
}
