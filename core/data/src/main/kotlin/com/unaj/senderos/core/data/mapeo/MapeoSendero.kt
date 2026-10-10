package com.unaj.senderos.data.mapeo

import com.unaj.senderos.data.local.EtapaEntidad
import com.unaj.senderos.data.local.SenderoEntidad
import com.unaj.senderos.data.remote.EtapaRed
import com.unaj.senderos.data.remote.SenderoRed
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoRecorrido

// Resolver una dificultad por nombre con valor por defecto MEDIA
private fun resolverDificultad(nombre: String): Dificultad {
    return Dificultad.entries.firstOrNull { valor -> valor.name == nombre } ?: Dificultad.MEDIA
}

// Resolver un tipo de recorrido por nombre con valor por defecto LINEAL
private fun resolverTipoRecorrido(nombre: String): TipoRecorrido {
    return TipoRecorrido.entries.firstOrNull { valor -> valor.name == nombre } ?: TipoRecorrido.LINEAL
}

// Resolver un estado de sincronización por nombre con valor por defecto SINCRONIZADO
private fun resolverEstadoSincronizacion(nombre: String): EstadoSincronizacion {
    return EstadoSincronizacion.entries.firstOrNull { valor -> valor.name == nombre }
        ?: EstadoSincronizacion.SINCRONIZADO
}

// Mapear la entidad local al modelo de dominio
fun SenderoEntidad.aDominio(): Sendero {
    return Sendero(
        id = id,
        idUsuario = idUsuario,
        nombre = nombre,
        region = region,
        distanciaKilometros = distanciaKilometros,
        dificultad = resolverDificultad(dificultad),
        descripcion = descripcion,
        desnivelMetros = desnivelMetros,
        duracionEstimada = duracionEstimada,
        alturaMaximaMetros = alturaMaximaMetros,
        alturaMinimaMetros = alturaMinimaMetros,
        tipoRecorrido = resolverTipoRecorrido(tipoRecorrido),
        puntoInicio = puntoInicio,
        mejorEpoca = mejorEpoca,
        recomendaciones = recomendaciones,
        servicios = servicios,
        etapas = etapas.map { etapa -> Etapa(etapa.numero, etapa.nombre, etapa.distanciaKilometros) },
        esFavorito = esFavorito,
        estadoSincronizacion = resolverEstadoSincronizacion(estadoSincronizacion)
    )
}

// Mapear el modelo de dominio a la entidad local con los metadatos de sincronización indicados
fun Sendero.aEntidad(
    fechaModificacion: Long,
    estadoSincronizacion: EstadoSincronizacion,
    estaEliminado: Boolean
): SenderoEntidad {
    return SenderoEntidad(
        id = id,
        idUsuario = idUsuario,
        nombre = nombre,
        region = region,
        distanciaKilometros = distanciaKilometros,
        dificultad = dificultad.name,
        descripcion = descripcion,
        desnivelMetros = desnivelMetros,
        duracionEstimada = duracionEstimada,
        alturaMaximaMetros = alturaMaximaMetros,
        alturaMinimaMetros = alturaMinimaMetros,
        tipoRecorrido = tipoRecorrido.name,
        puntoInicio = puntoInicio,
        mejorEpoca = mejorEpoca,
        recomendaciones = recomendaciones,
        servicios = servicios,
        etapas = etapas.map { etapa -> EtapaEntidad(etapa.numero, etapa.nombre, etapa.distanciaKilometros) },
        esFavorito = esFavorito,
        fechaModificacion = fechaModificacion,
        estadoSincronizacion = estadoSincronizacion.name,
        estaEliminado = estaEliminado
    )
}

// Mapear la entidad local al documento remoto
fun SenderoEntidad.aRed(): SenderoRed {
    return SenderoRed(
        id = id,
        nombre = nombre,
        region = region,
        distanciaKilometros = distanciaKilometros,
        dificultad = dificultad,
        descripcion = descripcion,
        desnivelMetros = desnivelMetros,
        duracionEstimada = duracionEstimada,
        alturaMaximaMetros = alturaMaximaMetros,
        alturaMinimaMetros = alturaMinimaMetros,
        tipoRecorrido = tipoRecorrido,
        puntoInicio = puntoInicio,
        mejorEpoca = mejorEpoca,
        recomendaciones = recomendaciones,
        servicios = servicios,
        etapas = etapas.map { etapa -> EtapaRed(etapa.numero, etapa.nombre, etapa.distanciaKilometros) },
        esFavorito = esFavorito,
        fechaModificacion = fechaModificacion,
        eliminado = estaEliminado
    )
}

// Mapear el documento remoto a una entidad local ya sincronizada del usuario indicado
fun SenderoRed.aEntidad(idUsuario: String): SenderoEntidad {
    return SenderoEntidad(
        id = id,
        idUsuario = idUsuario,
        nombre = nombre,
        region = region,
        distanciaKilometros = distanciaKilometros,
        dificultad = dificultad,
        descripcion = descripcion,
        desnivelMetros = desnivelMetros,
        duracionEstimada = duracionEstimada,
        alturaMaximaMetros = alturaMaximaMetros,
        alturaMinimaMetros = alturaMinimaMetros,
        tipoRecorrido = tipoRecorrido,
        puntoInicio = puntoInicio,
        mejorEpoca = mejorEpoca,
        recomendaciones = recomendaciones,
        servicios = servicios,
        etapas = etapas.map { etapa -> EtapaEntidad(etapa.numero, etapa.nombre, etapa.distanciaKilometros) },
        esFavorito = esFavorito,
        fechaModificacion = fechaModificacion,
        estadoSincronizacion = EstadoSincronizacion.SINCRONIZADO.name,
        estaEliminado = eliminado
    )
}
