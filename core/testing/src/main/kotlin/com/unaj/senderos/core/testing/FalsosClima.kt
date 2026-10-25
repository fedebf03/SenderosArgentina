package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.clima.ClimaActual
import com.unaj.senderos.core.domain.clima.CondicionClima
import com.unaj.senderos.core.domain.clima.PronosticoClima
import com.unaj.senderos.core.domain.clima.PronosticoDia
import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.port.AlmacenPronosticos
import com.unaj.senderos.core.domain.port.FuentePronostico

fun crearPronosticoDePrueba(
    lugar: Coordenada = Coordenada(-41.13, -71.30),
    obtenidoMilisegundos: Long = 1_000L,
    actual: ClimaActual = ClimaActual(12.0, 10.0, 15.0, 25.0, CondicionClima.DESPEJADO, esDeDia = true),
    dias: List<PronosticoDia> = listOf(
        PronosticoDia("2026-10-19", 18.0, 6.0, 10, 0.0, 15.0, 25.0, CondicionClima.DESPEJADO),
        PronosticoDia("2026-10-20", 17.0, 5.0, 20, 0.5, 18.0, 30.0, CondicionClima.POCO_NUBLADO)
    )
) = PronosticoClima(lugar, obtenidoMilisegundos, actual, dias)

class FalsaFuentePronostico : FuentePronostico {

    var resultado: Result<PronosticoClima> = Result.success(crearPronosticoDePrueba())
    val lugaresPedidos: MutableList<Coordenada> = mutableListOf()

    override suspend fun obtener(lugar: Coordenada): Result<PronosticoClima> {
        lugaresPedidos.add(lugar)
        return resultado
    }
}

class FalsoAlmacenPronosticos : AlmacenPronosticos {

    val guardados: MutableMap<String, PronosticoClima> = mutableMapOf()
    var resultadoGuardar: Result<Unit> = Result.success(Unit)

    override suspend fun guardar(clave: String, pronostico: PronosticoClima): Result<Unit> {
        if (resultadoGuardar.isSuccess) guardados[clave] = pronostico
        return resultadoGuardar
    }

    override suspend fun leer(clave: String): PronosticoClima? = guardados[clave]
}
