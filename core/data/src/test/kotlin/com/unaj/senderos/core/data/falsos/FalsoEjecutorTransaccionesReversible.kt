package com.unaj.senderos.core.data.falsos

import com.unaj.senderos.core.database.EjecutorTransacciones

class FalsoEjecutorTransaccionesReversible(
    private val senderoDao: FalsoSenderoDao,
    private val puntoCapturadoDao: FalsoPuntoCapturadoDao
) : EjecutorTransacciones {

    // Emula la reversión de Room: si el bloque lanza una excepción, restaura las dos tablas falsas
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
