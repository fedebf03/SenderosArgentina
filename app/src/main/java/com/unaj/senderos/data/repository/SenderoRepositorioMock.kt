package com.unaj.senderos.data.repository

import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.repository.SenderoRepositorio

// Datos de ejemplo hasta conectar Firestore en /usuarios/{idUsuario}/senderos
class SenderoRepositorioMock : SenderoRepositorio {
    // Devolver dos senderos de ejemplo asociados al usuario indicado
    override suspend fun obtenerSenderosDelUsuario(idUsuario: String): Result<List<Sendero>> {
        return Result.success(
            listOf(
                Sendero(
                    id = "huella-andina",
                    idUsuario = idUsuario,
                    nombre = "Huella Andina",
                    region = "Neuquén, Río Negro y Chubut",
                    distanciaKilometros = 570.0,
                    dificultad = Dificultad.MEDIA
                ),
                Sendero(
                    id = "huella-neuquina",
                    idUsuario = idUsuario,
                    nombre = "Huella Neuquina",
                    region = "Neuquén",
                    distanciaKilometros = 120.0,
                    dificultad = Dificultad.MEDIA
                )
            )
        )
    }
}
