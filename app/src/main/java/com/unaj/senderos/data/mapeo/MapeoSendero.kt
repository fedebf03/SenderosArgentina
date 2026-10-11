package com.unaj.senderos.data.mapeo

import com.unaj.senderos.data.local.EtapaEntidad
import com.unaj.senderos.data.local.PuntoRecorridoEntidad
import com.unaj.senderos.data.local.SenderoEntidad
import com.unaj.senderos.data.remote.EtapaRed
import com.unaj.senderos.data.remote.PuntoRecorridoRed
import com.unaj.senderos.data.remote.SenderoRed
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.EstadoSincronizacion
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.TipoRecorrido

private fun resolverDificultad(nombre: String): Dificultad {
    return Dificultad.entries.firstOrNull { valor -> valor.name == nombre } ?: Dificultad.MEDIA
}

private fun resolverTipoRecorrido(nombre: String): TipoRecorrido {
    return TipoRecorrido.entries.firstOrNull { valor -> valor.name == nombre } ?: TipoRecorrido.LINEAL
}

private fun resolverTipoPunto(nombre: String): TipoPunto {
    return TipoPunto.entries.firstOrNull { valor -> valor.name == nombre } ?: TipoPunto.PASO
}

private fun resolverEstadoSincronizacion(nombre: String): EstadoSincronizacion {
    return EstadoSincronizacion.entries.firstOrNull { valor -> valor.name == nombre }
        ?: EstadoSincronizacion.SINCRONIZADO
}

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
        estadoSincronizacion = resolverEstadoSincronizacion(estadoSincronizacion),
        recorrido = recorrido.map { punto ->
            PuntoRecorrido(punto.latitud, punto.longitud, resolverTipoPunto(punto.tipo), punto.nombre)
        },
        foto = foto
    )
}

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
        estaEliminado = estaEliminado,
        recorrido = recorrido.map { punto ->
            PuntoRecorridoEntidad(punto.latitud, punto.longitud, punto.tipo.name, punto.nombre)
        },
        foto = foto
    )
}

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
        eliminado = estaEliminado,
        recorrido = recorrido.map { punto -> PuntoRecorridoRed(punto.latitud, punto.longitud, punto.tipo, punto.nombre) },
        foto = foto
    )
}

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
        estaEliminado = eliminado,
        recorrido = recorrido.map { punto ->
            PuntoRecorridoEntidad(punto.latitud, punto.longitud, punto.tipo, punto.nombre)
        },
        foto = foto
    )
}
