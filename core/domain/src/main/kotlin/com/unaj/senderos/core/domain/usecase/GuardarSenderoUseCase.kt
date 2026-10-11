package com.unaj.senderos.domain.usecase

import com.unaj.senderos.domain.model.CampoSendero
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.domain.model.SenderoInvalidoException
import com.unaj.senderos.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.domain.repository.SenderoRepositorio
import java.util.UUID

class GuardarSenderoUseCase(
    private val senderoRepositorio: SenderoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio,
    private val generarIdentificador: () -> String = { UUID.randomUUID().toString() }
) {
    suspend operator fun invoke(sendero: Sendero): Result<Sendero> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException("No hay una sesión activa"))

        val normalizado = normalizar(sendero)
        val camposInvalidos = validar(normalizado)
        if (camposInvalidos.isNotEmpty()) {
            return Result.failure(SenderoInvalidoException(camposInvalidos))
        }

        val senderoAGuardar = normalizado.copy(
            id = normalizado.id.ifBlank { generarIdentificador() },
            idUsuario = usuario.id
        )
        return senderoRepositorio.guardarSendero(usuario.id, senderoAGuardar)
            .map { senderoAGuardar }
    }

    private fun normalizar(sendero: Sendero): Sendero {
        return sendero.copy(
            nombre = sendero.nombre.trim(),
            region = sendero.region.trim(),
            descripcion = sendero.descripcion.trim(),
            duracionEstimada = sendero.duracionEstimada.trim(),
            puntoInicio = sendero.puntoInicio.trim(),
            mejorEpoca = sendero.mejorEpoca.trim(),
            recomendaciones = sendero.recomendaciones.map { texto -> texto.trim() }.filter { texto -> texto.isNotEmpty() },
            servicios = sendero.servicios.map { texto -> texto.trim() }.filter { texto -> texto.isNotEmpty() },
            etapas = sendero.etapas.mapIndexed { indice, etapa -> etapa.copy(numero = indice + 1) },
            recorrido = sendero.recorrido.map { punto -> punto.copy(nombre = punto.nombre.trim()) }
        )
    }

    private fun validar(sendero: Sendero): Set<CampoSendero> {
        val camposInvalidos = mutableSetOf<CampoSendero>()
        if (sendero.nombre.isBlank()) camposInvalidos.add(CampoSendero.NOMBRE)
        if (sendero.region.isBlank()) camposInvalidos.add(CampoSendero.REGION)
        if (sendero.descripcion.isBlank()) camposInvalidos.add(CampoSendero.DESCRIPCION)
        if (sendero.distanciaKilometros <= 0.0) camposInvalidos.add(CampoSendero.DISTANCIA)
        if (sendero.desnivelMetros < 0) camposInvalidos.add(CampoSendero.DESNIVEL)
        if (sendero.alturaMinimaMetros < 0) camposInvalidos.add(CampoSendero.ALTURA_MINIMA)
        if (sendero.alturaMaximaMetros < 0 || sendero.alturaMaximaMetros < sendero.alturaMinimaMetros) {
            camposInvalidos.add(CampoSendero.ALTURA_MAXIMA)
        }
        val hayPuntoFueraDeRango = sendero.recorrido.any { punto ->
            punto.latitud !in -90.0..90.0 || punto.longitud !in -180.0..180.0
        }
        if (hayPuntoFueraDeRango) camposInvalidos.add(CampoSendero.RECORRIDO)
        return camposInvalidos
    }
}
