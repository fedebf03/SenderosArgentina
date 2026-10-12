package com.unaj.senderos.falsos

import com.unaj.senderos.data.local.EjecutorTransacciones

class FalsoEjecutorTransaccionesReversible(
    private val senderoDao: FalsoSenderoDao,
    private val puntoCapturadoDao: FalsoPuntoCapturadoDao
) : EjecutorTransacciones {

    // Ejecutar el bloque y, si lanza una excepción, restaurar las dos tablas falsas como lo hace la reversión de Room
    override suspend fun <T> ejecutar(bloque: suspend () -> T): T {
        val senderosAntes = senderoDao.entidades.value
        val puntosAntes = puntoCapturadoDao.entidades.value
        try {
            return bloque()
        } catch (error: Throwable) {
            senderoDao.entidades.value = senderosAntes
            puntoCapturadoDao.entidades.value = puntosAntes
            throw error
        }
    }
}
