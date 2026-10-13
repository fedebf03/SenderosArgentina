package com.unaj.senderos.data.mapeo

import com.unaj.senderos.data.local.PuntoCapturadoEntidad
import com.unaj.senderos.domain.model.PuntoCapturado
import com.unaj.senderos.domain.model.TipoPunto

// Un tipo desconocido se resuelve como PASO
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
