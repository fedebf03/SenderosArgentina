package com.unaj.senderos.core.data.falsos

import com.unaj.senderos.core.database.recorrido.CoordenadaEntidad
import com.unaj.senderos.core.database.sendero.EtapaEntidad
import com.unaj.senderos.core.database.recorrido.RecorridoEntidad
import com.unaj.senderos.core.database.sendero.SenderoEntidad
import com.unaj.senderos.core.network.recorrido.CoordenadaRed
import com.unaj.senderos.core.network.recorrido.RecorridoRed
import com.unaj.senderos.core.database.sendero.PerfilElevacionEntidad
import com.unaj.senderos.core.database.sendero.PuntoRecorridoEntidad
fun crearEntidadDePrueba(
    id: String = "sendero-1",
    idUsuario: String = "usuario-1",
    nombre: String = "Cerro Catedral",
    fechaModificacion: Long = 1_000L,
    estadoSincronizacion: String = "PENDIENTE",
    estaEliminado: Boolean = false,
    bioma: String = "SIN_DEFINIR",
    recorrido: List<PuntoRecorridoEntidad> = emptyList(),
    tipoRecorrido: String = "CIRCULAR",
    desnivelMetros: Int = 600,
    alturaMaximaMetros: Int = 2000,
    alturaMinimaMetros: Int = 1400,
    perfilElevacion: PerfilElevacionEntidad? = null
): SenderoEntidad {
    return SenderoEntidad(
        id = id,
        idUsuario = idUsuario,
        nombre = nombre,
        region = "Bariloche",
        distanciaKilometros = 12.0,
        dificultad = "MEDIA",
        descripcion = "Ascenso con vistas al lago",
        desnivelMetros = desnivelMetros,
        duracionEstimada = "5 horas",
        alturaMaximaMetros = alturaMaximaMetros,
        alturaMinimaMetros = alturaMinimaMetros,
        tipoRecorrido = tipoRecorrido,
        puntoInicio = "Base del cerro",
        mejorEpoca = "Verano",
        recomendaciones = listOf("Llevar agua"),
        servicios = listOf("Refugio"),
        etapas = listOf(EtapaEntidad(numero = 1, nombre = "Base a cumbre", distanciaKilometros = 6.0)),
        esFavorito = false,
        fechaModificacion = fechaModificacion,
        estadoSincronizacion = estadoSincronizacion,
        estaEliminado = estaEliminado,
        recorrido = recorrido,
        bioma = bioma,
        perfilElevacion = perfilElevacion
    )
}

fun crearEntidadRecorridoDePrueba(
    id: String = "recorrido-1",
    idUsuario: String = "usuario-1",
    fechaModificacion: Long = 1_000L,
    estadoSincronizacion: String = "PENDIENTE",
    estaEliminado: Boolean = false
): RecorridoEntidad {
    return RecorridoEntidad(
        id = id,
        idUsuario = idUsuario,
        idSendero = "sendero-1",
        nombreSendero = "Cerro Catedral",
        inicioMilisegundos = 1_000L,
        finMilisegundos = 3_601_000L,
        duracionMilisegundos = 3_600_000L,
        distanciaKilometros = 5.0,
        velocidadPromedioKilometrosPorHora = 5.0,
        velocidadMaximaKilometrosPorHora = 7.2,
        pasos = null,
        ritmoPasosPorMinuto = null,
        trazadoCaminado = listOf(CoordenadaEntidad(-39.95, -71.07), CoordenadaEntidad(-39.96, -71.07)),
        esSimulado = false,
        estadoSincronizacion = estadoSincronizacion,
        fechaModificacion = fechaModificacion,
        estaEliminado = estaEliminado
    )
}

fun crearRecorridoRedDePrueba(
    id: String = "recorrido-1",
    fechaModificacion: Long = 1_000L
): RecorridoRed {
    return RecorridoRed(
        id = id,
        idSendero = "sendero-1",
        nombreSendero = "Cerro Catedral",
        inicioMilisegundos = 1_000L,
        finMilisegundos = 3_601_000L,
        duracionMilisegundos = 3_600_000L,
        distanciaKilometros = 5.0,
        velocidadPromedioKilometrosPorHora = 5.0,
        velocidadMaximaKilometrosPorHora = 7.2,
        pasos = null,
        ritmoPasosPorMinuto = null,
        trazadoCaminado = listOf(CoordenadaRed(-39.95, -71.07), CoordenadaRed(-39.96, -71.07)),
        esSimulado = false,
        fechaModificacion = fechaModificacion
    )
}
