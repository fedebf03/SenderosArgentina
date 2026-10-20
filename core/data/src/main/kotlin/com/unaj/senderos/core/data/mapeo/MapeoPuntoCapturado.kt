package com.unaj.senderos.core.data.mapeo

import com.unaj.senderos.core.database.puntocapturado.PuntoCapturadoEntidad
import com.unaj.senderos.core.domain.model.PuntoCapturado
import com.unaj.senderos.core.domain.model.TipoPunto

fun PuntoCapturadoEntidad.aDominio(): PuntoCapturado {
    return PuntoCapturado(
        id = id,
        idUsuario = idUsuario,
        idSendero = idSendero,
        latitud = latitud,
        longitud = longitud,
        tipo = TipoPunto.entries.firstOrNull { valor -> valor.name == tipo } ?: TipoPunto.PASO,
        nota = nota,
        foto = foto,
        instanteMilisegundos = instanteMilisegundos
    )
}

fun PuntoCapturado.aEntidad(): PuntoCapturadoEntidad {
    return PuntoCapturadoEntidad(
        id = id,
        idUsuario = idUsuario,
        idSendero = idSendero,
        latitud = latitud,
        longitud = longitud,
        tipo = tipo.name,
        nota = nota,
        foto = foto,
        instanteMilisegundos = instanteMilisegundos
    )
}
