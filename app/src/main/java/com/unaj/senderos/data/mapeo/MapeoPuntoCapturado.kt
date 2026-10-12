package com.unaj.senderos.data.mapeo

import com.unaj.senderos.data.local.PuntoCapturadoEntidad
import com.unaj.senderos.domain.model.PuntoCapturado
import com.unaj.senderos.domain.model.TipoPunto

// Convertir la fila de Room en punto capturado de dominio; un tipo desconocido se resuelve como PASO
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

// Convertir el punto capturado de dominio en fila de Room
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
