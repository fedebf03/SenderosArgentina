package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.domain.model.CampoSendero
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.recorrido.EdicionRecorrido
import com.unaj.senderos.core.domain.recorrido.TrazadoImportado

internal fun FormularioSenderoEstadoVisual.conSenderoCargado(sendero: Sendero): FormularioSenderoEstadoVisual = copy(
    estaCargando = false,
    mensajeError = null,
    nombre = sendero.nombre,
    region = sendero.region,
    descripcion = sendero.descripcion,
    duracionEstimada = sendero.duracionEstimada,
    puntoInicio = sendero.puntoInicio,
    mejorEpoca = sendero.mejorEpoca,
    dificultad = sendero.dificultad,
    tipoRecorrido = sendero.tipoRecorrido,
    bioma = sendero.bioma,
    recomendaciones = sendero.recomendaciones,
    servicios = sendero.servicios,
    etapas = sendero.etapas,
    recorrido = sendero.recorrido,
    perfilElevacion = sendero.perfilElevacion,
    foto = sendero.foto,
    hayCambiosSinGuardar = false
)

internal fun FormularioSenderoEstadoVisual.conTexto(campo: CampoFormulario, texto: String): FormularioSenderoEstadoVisual {
    val estadoConTexto = when (campo) {
        CampoFormulario.NOMBRE -> copy(nombre = texto)
        CampoFormulario.REGION -> copy(region = texto)
        CampoFormulario.DESCRIPCION -> copy(descripcion = texto)
        CampoFormulario.DURACION -> copy(duracionEstimada = texto)
        CampoFormulario.PUNTO_INICIO -> copy(puntoInicio = texto)
        CampoFormulario.MEJOR_EPOCA -> copy(mejorEpoca = texto)
        CampoFormulario.NUEVA_RECOMENDACION -> copy(textoNuevaRecomendacion = texto)
        CampoFormulario.NUEVO_SERVICIO -> copy(textoNuevoServicio = texto)
        CampoFormulario.NOMBRE_NUEVA_ETAPA -> copy(nombreNuevaEtapa = texto)
        CampoFormulario.DISTANCIA_NUEVA_ETAPA -> copy(distanciaNuevaEtapa = texto)
    }
    val campoDelSendero = when (campo) {
        CampoFormulario.NOMBRE -> CampoSendero.NOMBRE
        CampoFormulario.REGION -> CampoSendero.REGION
        CampoFormulario.DESCRIPCION -> CampoSendero.DESCRIPCION
        else -> null
    }
    val camposRestantes = if (campoDelSendero != null) {
        estadoConTexto.camposConError - campoDelSendero
    } else {
        estadoConTexto.camposConError
    }
    val modificaDatos = campo != CampoFormulario.NUEVA_RECOMENDACION &&
        campo != CampoFormulario.NUEVO_SERVICIO &&
        campo != CampoFormulario.NOMBRE_NUEVA_ETAPA &&
        campo != CampoFormulario.DISTANCIA_NUEVA_ETAPA
    return estadoConTexto.copy(
        camposConError = camposRestantes,
        hayCambiosSinGuardar = estadoConTexto.hayCambiosSinGuardar || modificaDatos
    )
}

internal fun FormularioSenderoEstadoVisual.conRecomendacionNueva(): FormularioSenderoEstadoVisual {
    val texto = textoNuevaRecomendacion.trim()
    if (texto.isEmpty()) return this
    return copy(recomendaciones = recomendaciones + texto, textoNuevaRecomendacion = "", hayCambiosSinGuardar = true)
}

internal fun FormularioSenderoEstadoVisual.conRecomendacionSugerida(texto: String): FormularioSenderoEstadoVisual =
    recomendaciones.agregarSiFalta(texto)?.let { lista -> copy(recomendaciones = lista, hayCambiosSinGuardar = true) } ?: this

internal fun FormularioSenderoEstadoVisual.sinRecomendacion(indice: Int): FormularioSenderoEstadoVisual =
    copy(recomendaciones = recomendaciones.filterIndexed { posicion, _ -> posicion != indice }, hayCambiosSinGuardar = true)

internal fun FormularioSenderoEstadoVisual.conServicioNuevo(): FormularioSenderoEstadoVisual {
    val texto = textoNuevoServicio.trim()
    if (texto.isEmpty()) return this
    return copy(servicios = servicios + texto, textoNuevoServicio = "", hayCambiosSinGuardar = true)
}

internal fun FormularioSenderoEstadoVisual.conServicioSugerido(texto: String): FormularioSenderoEstadoVisual =
    servicios.agregarSiFalta(texto)?.let { lista -> copy(servicios = lista, hayCambiosSinGuardar = true) } ?: this

internal fun FormularioSenderoEstadoVisual.sinServicio(indice: Int): FormularioSenderoEstadoVisual =
    copy(servicios = servicios.filterIndexed { posicion, _ -> posicion != indice }, hayCambiosSinGuardar = true)

internal fun FormularioSenderoEstadoVisual.conEtapaNueva(): FormularioSenderoEstadoVisual? {
    val nombre = nombreNuevaEtapa.trim()
    val distancia = interpretarDecimal(distanciaNuevaEtapa)
    if (nombre.isEmpty() || distancia == null || distancia <= 0.0) return null
    return copy(
        etapas = etapas + Etapa(numero = etapas.size + 1, nombre = nombre, distanciaKilometros = distancia),
        nombreNuevaEtapa = "",
        distanciaNuevaEtapa = "",
        hayCambiosSinGuardar = true
    )
}

internal fun FormularioSenderoEstadoVisual.sinEtapa(indice: Int): FormularioSenderoEstadoVisual {
    val etapasRestantes = etapas
        .filterIndexed { posicion, _ -> posicion != indice }
        .mapIndexed { posicion, etapa -> etapa.copy(numero = posicion + 1) }
    return copy(etapas = etapasRestantes, hayCambiosSinGuardar = true)
}

// Una operación que no cambia nada (índice fuera de rango, invertir un solo punto) no cuenta como cambio
internal fun FormularioSenderoEstadoVisual.conRecorridoEditado(
    operacion: (EdicionRecorrido) -> EdicionRecorrido
): FormularioSenderoEstadoVisual {
    val actual = EdicionRecorrido(recorrido, indicePuntoSeleccionado)
    val editado = operacion(actual)
    if (editado == actual) return this
    return copy(
        recorrido = editado.puntos,
        indicePuntoSeleccionado = editado.seleccion,
        camposConError = camposConError - CampoSendero.RECORRIDO,
        hayCambiosSinGuardar = true
    )
}

// El nombre del archivo se usa solo si el formulario no tenía uno. Si el archivo trae alturas, también trae el perfil
internal fun FormularioSenderoEstadoVisual.conTrazadoImportado(trazado: TrazadoImportado): FormularioSenderoEstadoVisual {
    val conTrazado = copy(
        recorrido = trazado.recorrido,
        tipoRecorrido = trazado.tipoRecorrido,
        nombre = nombre.ifBlank { trazado.nombre },
        indicePuntoSeleccionado = null,
        trazadoImportadoPendiente = null,
        camposConError = camposConError - CampoSendero.RECORRIDO - CampoSendero.NOMBRE,
        hayCambiosSinGuardar = true
    )
    return trazado.perfil?.let { perfil -> conTrazado.conPerfil(perfil) } ?: conTrazado
}

internal fun FormularioSenderoEstadoVisual.conPerfil(perfil: PerfilElevacion): FormularioSenderoEstadoVisual = copy(
    perfilElevacion = perfil,
    estaCalculandoPerfil = false,
    avisoPerfil = null,
    hayCambiosSinGuardar = true
)

internal fun List<String>.agregarSiFalta(texto: String): List<String>? {
    val limpio = texto.trim()
    if (limpio.isEmpty() || any { elemento -> elemento.equals(limpio, ignoreCase = true) }) return null
    return this + limpio
}

internal fun interpretarDecimal(texto: String): Double? {
    val valor = texto.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return valor.takeIf { numero -> numero.isFinite() }
}
