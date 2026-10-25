package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.PuntoCapturado
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoRecorrido

private const val MENSAJE_CAMINATA_CORTA = "La caminata necesita al menos dos posiciones distintas para ser un sendero"
private const val MENSAJE_CAMINATA_MUY_CORTA = "La caminata fue muy corta para convertirla en un sendero (menos de 50 m)"
private const val DISTANCIA_MINIMA_PARA_CONVERTIR_KILOMETROS = 0.05

object ConversorCaminata {

    fun esConvertible(distanciaKilometros: Double): Boolean = distanciaKilometros >= DISTANCIA_MINIMA_PARA_CONVERTIR_KILOMETROS

    // El trazado caminado se simplifica igual que al importar un archivo. El perfil queda nulo porque la caminata no mide
    // alturas y el formulario lo calcula al crear el sendero
    fun aTrazado(
        trazadoCaminado: List<Coordenada>,
        puntosCapturados: List<PuntoCapturado>,
        nombreSugerido: String
    ): Result<TrazadoImportado> {
        val leido = TrazadoLeido(
            nombre = nombreSugerido,
            puntos = trazadoCaminado.map { coordenada -> PuntoLeido(coordenada.latitud, coordenada.longitud, altitudMetros = null) },
            puntosDeInteres = emptyList()
        )
        val base = ImportadorTrazado.importar(leido, nombreSugerido)
            .getOrElse { return Result.failure(IllegalArgumentException(MENSAJE_CAMINATA_CORTA)) }
        val largoCaminado = CalculadoraRecorrido.distanciaKilometros(
            trazadoCaminado.map { coordenada -> PuntoRecorrido(coordenada.latitud, coordenada.longitud) },
            TipoRecorrido.LINEAL
        )
        if (!esConvertible(largoCaminado)) return Result.failure(IllegalArgumentException(MENSAJE_CAMINATA_MUY_CORTA))
        val puntosDeInteres = puntosCapturados.sortedBy { punto -> punto.instanteMilisegundos }.map { punto ->
            PuntoRecorrido(
                latitud = punto.latitud,
                longitud = punto.longitud,
                tipo = punto.tipo,
                nombre = punto.nota.trim(),
                foto = punto.foto
            )
        }
        return Result.success(
            base.copy(recorrido = CalculadoraInsercionPuntos.insertar(base.recorrido, base.tipoRecorrido, puntosDeInteres))
        )
    }
}
