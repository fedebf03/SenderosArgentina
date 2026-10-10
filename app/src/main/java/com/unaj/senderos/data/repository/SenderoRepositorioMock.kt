package com.unaj.senderos.data.repository

import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.domain.repository.SenderoRepositorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Repositorio en memoria con estado reactivo por usuario (clave: idUsuario)
class SenderoRepositorioMock : SenderoRepositorio {

    private val senderosPorUsuario = MutableStateFlow<Map<String, List<Sendero>>>(emptyMap())

    // Observar la lista de senderos del usuario, sembrando los datos de ejemplo en el primer acceso
    override fun observarSenderosDelUsuario(idUsuario: String): Flow<List<Sendero>> {
        sembrarSiHaceFalta(idUsuario)
        return senderosPorUsuario.map { mapaActual -> mapaActual[idUsuario].orEmpty() }
    }

    // Observar un sendero del usuario; emite null si no existe o fue eliminado
    override fun observarSendero(idUsuario: String, idSendero: String): Flow<Sendero?> {
        sembrarSiHaceFalta(idUsuario)
        return senderosPorUsuario.map { mapaActual ->
            mapaActual[idUsuario].orEmpty().firstOrNull { sendero -> sendero.id == idSendero }
        }
    }

    // Invertir esFavorito del sendero indicado reemplazándolo por una copia, verificando y modificando de forma atómica
    override suspend fun alternarFavorito(idUsuario: String, idSendero: String): Result<Unit> {
        sembrarSiHaceFalta(idUsuario)
        var fueEncontrado = false
        senderosPorUsuario.update { mapaActual ->
            val listaActual = mapaActual[idUsuario].orEmpty()
            fueEncontrado = listaActual.any { sendero -> sendero.id == idSendero }
            mapaActual + (idUsuario to listaActual.map { sendero ->
                if (sendero.id == idSendero) sendero.copy(esFavorito = !sendero.esFavorito) else sendero
            })
        }
        return if (fueEncontrado) Result.success(Unit) else Result.failure(NoSuchElementException("No se encontró el sendero"))
    }

    // Quitar el sendero indicado de la lista del usuario, verificando y modificando de forma atómica
    override suspend fun eliminarSendero(idUsuario: String, idSendero: String): Result<Unit> {
        sembrarSiHaceFalta(idUsuario)
        var fueEncontrado = false
        senderosPorUsuario.update { mapaActual ->
            val listaActual = mapaActual[idUsuario].orEmpty()
            fueEncontrado = listaActual.any { sendero -> sendero.id == idSendero }
            mapaActual + (idUsuario to listaActual.filter { sendero -> sendero.id != idSendero })
        }
        return if (fueEncontrado) Result.success(Unit) else Result.failure(NoSuchElementException("No se encontró el sendero"))
    }

    // Sembrar los senderos de ejemplo la primera vez que se accede a un usuario
    private fun sembrarSiHaceFalta(idUsuario: String) {
        senderosPorUsuario.update { mapaActual ->
            if (mapaActual.containsKey(idUsuario)) {
                mapaActual
            } else {
                mapaActual + (idUsuario to crearSenderosDeEjemplo(idUsuario))
            }
        }
    }

    // Datos de ejemplo: reemplazar por datos reales al conectar Room + Firestore
    private fun crearSenderosDeEjemplo(idUsuario: String): List<Sendero> {
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
                )
            )
        )
    }
}
