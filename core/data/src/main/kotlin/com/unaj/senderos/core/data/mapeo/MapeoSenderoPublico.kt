package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.network.senderopublico.SenderoPublicoRed
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.model.SenderoPublico

fun SenderoPublico.aRed(): SenderoPublicoRed {
    val documento = sendero.copy(esFavorito = false)
        .aEntidad(
            fechaModificacion = fechaPublicacion,
            estadoSincronizacion = EstadoSincronizacion.SINCRONIZADO,
            estaEliminado = false
        )
        .aRed()
    return SenderoPublicoRed(
        idAutor = idAutor,
        nombreAutor = nombreAutor,
        fechaPublicacion = fechaPublicacion,
        sendero = documento
    )
}

fun SenderoPublicoRed.aDominio(): SenderoPublico {
    return SenderoPublico(
        sendero = sendero.aEntidad(idAutor).aDominio(),
        nombreAutor = nombreAutor,
        fechaPublicacion = fechaPublicacion
    )
}
