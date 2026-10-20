package com.unaj.senderos.core.domain.port

interface LanzadorSeguimiento {

    fun iniciar(idSendero: String, esSimulado: Boolean = false)

    // Arranca el servicio en primer plano que registra lo caminado, sin sendero que seguir
    fun iniciarCaminataLibre(idCaminata: String, idUsuario: String, nombre: String)

    fun detener()

    fun cancelar()
}
