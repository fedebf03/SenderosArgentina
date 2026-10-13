package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.PuntoCapturado
import com.unaj.senderos.domain.model.TipoPunto

fun crearPuntoCapturadoDePrueba(
    id: String = "punto-1",
    idUsuario: String = "usuario-1",
    idSendero: String = "sendero-1",
    latitud: Double = -39.95,
    longitud: Double = -71.07,
    tipo: TipoPunto = TipoPunto.MIRADOR,
    nota: String = "Vista al lago",
    foto: String = "",
    instanteMilisegundos: Long = 1_000L
): PuntoCapturado {
    return PuntoCapturado(
        id = id,
        idUsuario = idUsuario,
        idSendero = idSendero,
        latitud = latitud,
        longitud = longitud,
        tipo = tipo,
        nota = nota,
        foto = foto,
        instanteMilisegundos = instanteMilisegundos
    )
}
