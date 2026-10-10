package com.unaj.senderos.data.ejemplos

import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.model.TipoRecorrido

// Crear los senderos de ejemplo que se siembran la primera vez que ingresa un usuario
fun crearSenderosDeEjemplo(idUsuario: String): List<Sendero> {
    return listOf(
        Sendero(
            id = "huella-andina",
            idUsuario = idUsuario,
            nombre = "Huella Andina",
            region = "Neuquén, Río Negro y Chubut",
            distanciaKilometros = 570.0,
            dificultad = Dificultad.MEDIA,
            descripcion = "Travesía de largo recorrido que une lagos, bosques andino-patagónicos y pasos de montaña a lo largo de la cordillera.",
            desnivelMetros = 1200,
            duracionEstimada = "30 días",
            alturaMaximaMetros = 1900,
            alturaMinimaMetros = 700,
            tipoRecorrido = TipoRecorrido.LINEAL,
            puntoInicio = "Lago Aluminé, Neuquén",
            mejorEpoca = "Diciembre a marzo",
            recomendaciones = listOf(
                "Llevar agua y filtro potabilizador",
                "Abrigo e impermeable aunque sea verano",
                "Registrarse en guardaparques antes de salir"
            ),
            servicios = listOf(
                "Campings organizados",
                "Refugios de montaña",
                "Señal de celular solo en localidades"
            ),
            etapas = listOf(
                Etapa(1, "Lago Aluminé - Lago Ruca Choroi", 21.0),
                Etapa(2, "Lago Ruca Choroi - Lago Quillén", 25.0),
                Etapa(3, "Lago Quillén - Lago Tromen", 30.0)
            ),
            // Coordenadas aproximadas de ejemplo, no son el trazado real
            recorrido = listOf(
                PuntoRecorrido(-39.24, -70.92, TipoPunto.PASO, "Inicio en Aluminé"),
                PuntoRecorrido(-39.30, -71.05, TipoPunto.AGUA, "Arroyo"),
                PuntoRecorrido(-39.38, -71.12, TipoPunto.MIRADOR, "Mirador del lago"),
                PuntoRecorrido(-39.47, -71.20, TipoPunto.CAMPING, "Camping agreste"),
                PuntoRecorrido(-39.55, -71.30, TipoPunto.REFUGIO, "Refugio")
            )
        ),
        Sendero(
            id = "huella-neuquina",
            idUsuario = idUsuario,
            nombre = "Huella Neuquina",
            region = "Neuquén",
            distanciaKilometros = 120.0,
            dificultad = Dificultad.MEDIA,
            descripcion = "Recorrido por la estepa y el bosque neuquino con vistas a volcanes y lagunas de altura.",
            desnivelMetros = 800,
            duracionEstimada = "6 días",
            alturaMaximaMetros = 1700,
            alturaMinimaMetros = 900,
            tipoRecorrido = TipoRecorrido.IDA_Y_VUELTA,
            puntoInicio = "Junín de los Andes, Neuquén",
            mejorEpoca = "Noviembre a abril",
            recomendaciones = listOf(
                "Protector solar y anteojos",
                "Bastones para los descensos"
            ),
            servicios = listOf(
                "Campings agrestes",
                "Fuentes de agua en arroyos"
            ),
            etapas = listOf(
                Etapa(1, "Junín de los Andes - Laguna Verde", 18.0),
                Etapa(2, "Laguna Verde - Volcán Lanín (base)", 22.0)
            ),
            // Coordenadas aproximadas de ejemplo, no son el trazado real
            recorrido = listOf(
                PuntoRecorrido(-39.95, -71.07, TipoPunto.PASO, "Inicio en Junín de los Andes"),
                PuntoRecorrido(-39.92, -71.14, TipoPunto.AGUA, "Arroyo"),
                PuntoRecorrido(-39.88, -71.22, TipoPunto.MIRADOR, "Mirador del volcán"),
                PuntoRecorrido(-39.84, -71.30, TipoPunto.CAMPING, "Camping agreste"),
                PuntoRecorrido(-39.80, -71.38, TipoPunto.PELIGRO, "Tramo expuesto")
            )
        )
    )
}
