package com.unaj.senderos.core.domain.port

interface LanzadorSeguimiento {

    fun iniciar(idSendero: String, esSimulado: Boolean = false)

    fun iniciarCaminataLibre(idCaminata: String, idUsuario: String, nombre: String)

    fun detener()

    fun cancelar()
}
