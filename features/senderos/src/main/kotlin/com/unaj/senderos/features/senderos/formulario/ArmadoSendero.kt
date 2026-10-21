package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil

internal fun FormularioSenderoEstadoVisual.armarSendero(senderoOriginal: Sendero?, idCaminata: String?): Sendero? {
    // La distancia sale siempre del recorrido. Un sendero viejo sin trazado conserva la que ya tenía
    val distancia = if (recorrido.size >= 2) {
        Math.round(distanciaCalculadaKilometros * 10) / 10.0
    } else {
        senderoOriginal?.distanciaKilometros?.takeIf { valor -> valor > 0.0 }
    } ?: return null

    val senderoBase = senderoOriginal ?: Sendero(
        id = idCaminata?.let { id -> IdentificadorCaminataLibre.idSenderoDesde(id) }.orEmpty(),
        idUsuario = "",
        nombre = "",
        region = "",
        distanciaKilometros = 0.0,
        dificultad = dificultad
    )
    val senderoSinPerfil = senderoBase.copy(
        nombre = nombre,
        region = region,
        descripcion = descripcion,
        distanciaKilometros = distancia,
        duracionEstimada = duracionEstimada,
        puntoInicio = puntoInicio,
        mejorEpoca = mejorEpoca,
        dificultad = dificultad,
        tipoRecorrido = tipoRecorrido,
        bioma = bioma,
        recomendaciones = recomendaciones,
        servicios = servicios,
        etapas = etapas,
        recorrido = recorrido,
        foto = foto
    )
    // El perfil viaja aunque sea de otro trazado, porque su firma distinta hace que la sincronización lo recalcule
    return perfilElevacion
        ?.let { perfil -> CalculadoraPerfil.aplicarAlSendero(senderoSinPerfil, perfil) }
        ?: senderoSinPerfil
}
