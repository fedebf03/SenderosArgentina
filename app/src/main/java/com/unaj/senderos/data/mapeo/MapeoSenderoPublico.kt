package com.unaj.senderos.data.mapeo

import com.unaj.senderos.data.remote.SenderoPublicoRed
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.SenderoPublico

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
