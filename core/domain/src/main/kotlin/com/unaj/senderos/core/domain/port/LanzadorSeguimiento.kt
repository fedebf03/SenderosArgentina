package com.unaj.senderos.core.domain.port

interface LanzadorSeguimiento {

    fun iniciar(idSendero: String, esSimulado: Boolean = false)

    // Iniciar una caminata libre del usuario, sin sendero: arranca el servicio en primer plano que registra lo caminado
    fun iniciarCaminataLibre(idCaminata: String, idUsuario: String, nombre: String)

    fun detener()

    fun cancelar()
}
