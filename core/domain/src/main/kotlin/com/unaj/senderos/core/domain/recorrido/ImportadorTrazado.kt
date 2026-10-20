package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.DISTANCIA_MAXIMA_RECOMENDADA_METROS
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.TipoRecorrido

data class TrazadoImportado(
    val nombre: String,
    val recorrido: List<PuntoRecorrido>,
    val tipoRecorrido: TipoRecorrido,
    val perfil: PerfilElevacion?
)

private const val TOLERANCIA_INICIAL_METROS = 5.0
private const val MAXIMO_PUNTOS_IMPORTADOS = 300
private const val DISTANCIA_CIERRE_METROS = 100.0
private const val DISTANCIA_VUELTA_AL_CAMINO_METROS = 25.0
private const val FRACCION_MINIMA_SOBRE_EL_CAMINO = 0.9
private const val FRACCION_MINIMA_CON_ALTURA = 0.8
private const val MAXIMO_PUNTOS_PARA_DETECTAR = 3000
private const val METROS_POR_KILOMETRO = 1000.0
private const val MAXIMO_PUNTOS_DE_INTERES_IMPORTADOS = 50

object ImportadorTrazado {

    // RF4 - Importar GPX: detectamos el tipo de recorrido, simplificamos el trazado y armamos el perfil del archivo
    // De un ida y vuelta se queda solo la ida, porque la vuelta la arma la aplicación
    fun importar(leido: TrazadoLeido, nombreArchivo: String): Result<TrazadoImportado> {
        val puntos = leido.puntos.filterIndexed { indice, punto ->
            indice == 0 || punto.latitud != leido.puntos[indice - 1].latitud || punto.longitud != leido.puntos[indice - 1].longitud
        }
        if (puntos.size < 2) return Result.failure(IllegalArgumentException("El archivo necesita al menos dos puntos distintos"))
        val (tipoRecorrido, referencia) = detectarTipo(puntos)
        var recorrido = simplificar(referencia).map { punto -> PuntoRecorrido(latitud = punto.latitud, longitud = punto.longitud) }
        if (tipoRecorrido == TipoRecorrido.CIRCULAR && recorrido.size > 3) {
            // El recorrido circular ya cierra solo del último punto al primero
            recorrido = recorrido.dropLast(1)
        }
        val puntosDeInteres = elegirPuntosDeInteres(leido.puntosDeInteres, recorrido, tipoRecorrido)
        recorrido = CalculadoraInsercionPuntos.insertar(recorrido, tipoRecorrido, puntosDeInteres)
        return Result.success(
            TrazadoImportado(
                nombre = leido.nombre ?: nombreDesdeArchivo(nombreArchivo),
                recorrido = recorrido,
                tipoRecorrido = tipoRecorrido,
                perfil = armarPerfil(referencia, recorrido, tipoRecorrido)
            )
        )
    }

    // Solo entran los waypoints a 100 m o menos del camino (un GPX puede traer marcas de toda una zona) y a lo sumo 50,
    // para no inflar el trazado ni el documento del sendero
    private fun elegirPuntosDeInteres(
        marcados: List<PuntoDeInteresLeido>,
        recorrido: List<PuntoRecorrido>,
        tipoRecorrido: TipoRecorrido
    ): List<PuntoRecorrido> {
        val candidatos = marcados.map { marcado ->
            PuntoRecorrido(latitud = marcado.latitud, longitud = marcado.longitud, tipo = TipoPunto.OTRO, nombre = marcado.nombre)
        }
        val distancias = CalculadoraInsercionPuntos.distanciasAlTrazadoMetros(recorrido, tipoRecorrido, candidatos)
        return candidatos.filterIndexed { indice, _ ->
            val distancia = distancias[indice]
            distancia != null && distancia <= DISTANCIA_MAXIMA_RECOMENDADA_METROS
        }.take(MAXIMO_PUNTOS_DE_INTERES_IMPORTADOS)
    }

    // Es ida y vuelta si termina cerca del inicio y el 90 % de la vuelta pasa a 25 m o menos de la ida. Es circular
    // si termina a 100 m o menos del inicio por otro camino, y lineal en cualquier otro caso
    private fun detectarTipo(puntos: List<PuntoLeido>): Pair<TipoRecorrido, List<PuntoLeido>> {
        val indicesMuestra = submuestrear(puntos.size)
        val muestra = indicesMuestra.map { indice -> puntos[indice] }
        val inicio = muestra.first()
        val terminaCerca = metrosEntre(inicio, muestra.last()) <= DISTANCIA_CIERRE_METROS
        if (!terminaCerca) return TipoRecorrido.LINEAL to puntos
        val indiceGiro = muestra.indices.maxBy { indice -> metrosEntre(inicio, muestra[indice]) }
        val ida = muestra.subList(0, indiceGiro + 1)
        val vuelta = muestra.subList(indiceGiro, muestra.size)
        if (ida.size >= 2 && vuelta.size >= 2) {
            val latitudReferencia = Math.toRadians(inicio.latitud)
            val idaPlana = ida.map { punto -> SimplificadorTrazado.proyectar(Coordenada(punto.latitud, punto.longitud), latitudReferencia) }
            val sobreElCamino = vuelta.count { punto ->
                val plano = SimplificadorTrazado.proyectar(Coordenada(punto.latitud, punto.longitud), latitudReferencia)
                (0 until idaPlana.lastIndex).any { tramo ->
                    SimplificadorTrazado.distanciaASegmento(plano, idaPlana[tramo], idaPlana[tramo + 1]) <= DISTANCIA_VUELTA_AL_CAMINO_METROS
                }
            }
            if (sobreElCamino >= FRACCION_MINIMA_SOBRE_EL_CAMINO * vuelta.size) {
                // Se usa el índice y no el valor porque un trazado puede pasar dos veces por el mismo punto
                val giroOriginal = indicesMuestra[indiceGiro]
                return TipoRecorrido.IDA_Y_VUELTA to puntos.subList(0, giroOriginal + 1)
            }
        }
        return TipoRecorrido.CIRCULAR to puntos
    }

    // Con 3000 puntos repartidos como máximo la detección no se vuelve lenta en trazados muy largos
    private fun submuestrear(cantidad: Int): List<Int> {
        if (cantidad <= MAXIMO_PUNTOS_PARA_DETECTAR) return List(cantidad) { indice -> indice }
        val paso = cantidad.toDouble() / MAXIMO_PUNTOS_PARA_DETECTAR
        return (List(MAXIMO_PUNTOS_PARA_DETECTAR) { indice -> (indice * paso).toInt() } + (cantidad - 1)).distinct()
    }

    // Se empieza con 5 m de tolerancia y se duplica hasta que queden 300 puntos o menos
    private fun simplificar(puntos: List<PuntoLeido>): List<PuntoLeido> {
        val coordenadas = puntos.map { punto -> Coordenada(punto.latitud, punto.longitud) }
        var tolerancia = TOLERANCIA_INICIAL_METROS
        var conservados = SimplificadorTrazado.simplificar(coordenadas, tolerancia)
        while (conservados.size > MAXIMO_PUNTOS_IMPORTADOS) {
            tolerancia *= 2
            conservados = SimplificadorTrazado.simplificar(coordenadas, tolerancia)
        }
        return conservados.map { indice -> puntos[indice] }
    }

    // Solo se arma si al menos el 80 % de los puntos trae altura. Cada muestra toma la altura del punto del archivo
    // que está a la misma fracción del camino (en ida y vuelta, la vuelta repite la ida al revés)
    private fun armarPerfil(referencia: List<PuntoLeido>, recorrido: List<PuntoRecorrido>, tipoRecorrido: TipoRecorrido): PerfilElevacion? {
        val conAltura = referencia.filter { punto -> punto.altitudMetros != null }
        if (conAltura.size < 2 || conAltura.size < FRACCION_MINIMA_CON_ALTURA * referencia.size) return null
        val acumuladas = distanciasAcumuladasMetros(conAltura)
        val largoReferencia = acumuladas.last()
        val muestras = CalculadoraPerfil.muestrearTrazado(recorrido, tipoRecorrido)
        if (muestras.isEmpty() || largoReferencia <= 0.0) return null
        val total = muestras.last().distanciaKilometros
        val esIdaYVuelta = tipoRecorrido == TipoRecorrido.IDA_Y_VUELTA
        val base = if (esIdaYVuelta) total / 2 else total
        val altitudes = muestras.map { muestra ->
            val distancia = if (esIdaYVuelta && muestra.distanciaKilometros > base) total - muestra.distanciaKilometros else muestra.distanciaKilometros
            interpolarAltura(conAltura, acumuladas, (distancia / base).coerceIn(0.0, 1.0) * largoReferencia)
        }
        return CalculadoraPerfil.calcular(muestras, altitudes, CalculadoraPerfil.firmaTrazado(recorrido, tipoRecorrido))
    }

    private fun distanciasAcumuladasMetros(puntos: List<PuntoLeido>): List<Double> {
        val acumuladas = MutableList(puntos.size) { 0.0 }
        for (indice in 1 until puntos.size) acumuladas[indice] = acumuladas[indice - 1] + metrosEntre(puntos[indice - 1], puntos[indice])
        return acumuladas
    }

    private fun interpolarAltura(puntos: List<PuntoLeido>, acumuladas: List<Double>, distancia: Double): Double {
        val indiceSiguiente = acumuladas.indexOfFirst { acumulada -> acumulada >= distancia }.let { indice -> if (indice < 0) puntos.lastIndex else indice }
        val alturaSiguiente = puntos[indiceSiguiente].altitudMetros ?: 0.0
        if (indiceSiguiente == 0) return alturaSiguiente
        val anterior = indiceSiguiente - 1
        val alturaAnterior = puntos[anterior].altitudMetros ?: alturaSiguiente
        val tramo = acumuladas[indiceSiguiente] - acumuladas[anterior]
        val fraccion = if (tramo <= 0.0) 0.0 else (distancia - acumuladas[anterior]) / tramo
        return alturaAnterior + (alturaSiguiente - alturaAnterior) * fraccion
    }

    private fun metrosEntre(desde: PuntoLeido, hasta: PuntoLeido): Double =
        CalculadoraRecorrido.distanciaEntre(Coordenada(desde.latitud, desde.longitud), PuntoRecorrido(hasta.latitud, hasta.longitud)) *
            METROS_POR_KILOMETRO

    private fun nombreDesdeArchivo(nombreArchivo: String): String =
        nombreArchivo.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').replace(Regex("\\s+"), " ").trim()
}
