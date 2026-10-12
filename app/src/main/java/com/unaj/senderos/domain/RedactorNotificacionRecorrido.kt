package com.unaj.senderos.domain

import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.CalidadSenal
import com.unaj.senderos.domain.model.ContenidoAvisoRecorrido
import com.unaj.senderos.domain.model.ContenidoNotificacionRecorrido
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.TipoAvisoVisual
import com.unaj.senderos.domain.model.TipoPunto
import java.util.Locale
import kotlin.math.roundToInt

private const val MENSAJE_SENAL_DEBIL = "Sin señal GPS · salí al aire libre"
private const val METROS_POR_KILOMETRO = 1000.0
private const val METROS_DE_REDONDEO = 10.0
private const val KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO = 3.6
private val LOCALIZACION_ARGENTINA: Locale = Locale.forLanguageTag("es-AR")

// Redactor puro (sin Android) del contenido de las notificaciones del recorrido: textos, progreso y tipo visual de los avisos
object RedactorNotificacionRecorrido {

    // Calcular el porcentaje avanzado como 1 menos lo que resta sobre el largo del trazado, acotado a [0, 100];
    // nulo (indeterminado) mientras se busca la señal. Usar lo restante evita que un desvío haga crecer el progreso
    fun calcularProgresoPorcentaje(estado: EstadoSeguimiento?): Int? {
        if (estado == null || estado.calidadSenal == CalidadSenal.BUSCANDO) return null
        val largoTrazado = CalculadoraRecorrido.distanciasAcumuladasKilometros(estado.trazado).lastOrNull() ?: 0.0
        if (largoTrazado <= 0.0) return 0
        return ((1 - estado.distanciaRestanteKilometros / largoTrazado) * 100).roundToInt().coerceIn(0, 100)
    }

    // Redactar el título, el texto corto, el texto expandido y el progreso de la notificación persistente
    fun redactarPersistente(estado: EstadoSeguimiento?, esSimulado: Boolean): ContenidoNotificacionRecorrido {
        val nombre = estado?.nombreSendero.orEmpty().ifBlank { "el sendero" }
        val prefijo = if (esSimulado) "SIMULACIÓN · " else ""
        val textoCorto = redactarTextoCorto(estado)
        return ContenidoNotificacionRecorrido(
            titulo = "${prefijo}Recorriendo $nombre",
            textoCorto = textoCorto,
            textoExpandido = redactarTextoExpandido(estado, textoCorto),
            progresoPorcentaje = calcularProgresoPorcentaje(estado),
            instanteInicioMilisegundos = estado?.instanteInicioMilisegundos?.takeIf { instante -> instante > 0L }
        )
    }

    // Redactar el texto corto: la señal débil tiene prioridad sobre las distancias
    private fun redactarTextoCorto(estado: EstadoSeguimiento?): String {
        return when {
            estado == null -> "Preparando el recorrido"
            estado.calidadSenal == CalidadSenal.DEBIL -> MENSAJE_SENAL_DEBIL
            estado.calidadSenal == CalidadSenal.BUSCANDO -> "Buscando señal GPS…"
            else -> redactarDistancias(estado)
        }
    }

    // Redactar las distancias restante y recorrida en kilómetros
    private fun redactarDistancias(estado: EstadoSeguimiento): String {
        return "${formatearDecimal(estado.distanciaRestanteKilometros)} km restantes · " +
            "${formatearDecimal(estado.distanciaRecorridaKilometros)} km recorridos"
    }

    // Redactar el texto expandido con el texto corto, el tiempo, la velocidad, los pasos y el próximo punto
    private fun redactarTextoExpandido(estado: EstadoSeguimiento?, textoCorto: String): String {
        if (estado == null || estado.calidadSenal == CalidadSenal.BUSCANDO) return textoCorto
        val lineas = mutableListOf(textoCorto)
        val kilometrosPorHora = estado.velocidadMetrosPorSegundo?.let { velocidad ->
            velocidad * KILOMETROS_POR_HORA_POR_METRO_POR_SEGUNDO
        } ?: estado.velocidadPromedioKilometrosPorHora
        lineas += "Velocidad: ${kilometrosPorHora.roundToInt()} km/h"
        estado.pasos?.let { pasos -> lineas += "Pasos: $pasos" }
        redactarProximoPunto(estado)?.let { proximo -> lineas += proximo }
        return lineas.joinToString("\n")
    }

    // Redactar la línea del próximo punto con su nombre (o tipo) y la distancia si se conoce la posición
    private fun redactarProximoPunto(estado: EstadoSeguimiento): String? {
        val punto = estado.trazado.getOrNull(estado.indiceProximoPunto) ?: return null
        val nombre = punto.nombre.ifBlank { describirTipoCorto(punto.tipo) }
        val distancia = estado.posicionActual?.let { posicion ->
            " (a ${formatearDistanciaAproximada(CalculadoraRecorrido.distanciaEntre(posicion, punto) * METROS_POR_KILOMETRO)})"
        }.orEmpty()
        return "Próximo punto: $nombre$distancia"
    }

    // Elegir la apariencia (ícono y color) del aviso según su tipo
    fun elegirTipoVisual(aviso: AvisoRecorrido): TipoAvisoVisual {
        return when (aviso) {
            is AvisoRecorrido.CercaDePunto -> when (aviso.punto.tipo) {
                TipoPunto.AGUA -> TipoAvisoVisual.AGUA
                TipoPunto.PELIGRO -> TipoAvisoVisual.PELIGRO
                TipoPunto.MIRADOR -> TipoAvisoVisual.MIRADOR
                TipoPunto.REFUGIO -> TipoAvisoVisual.REFUGIO
                TipoPunto.CAMPING -> TipoAvisoVisual.CAMPING
                TipoPunto.PASO -> TipoAvisoVisual.PASO
            }
            AvisoRecorrido.Desvio -> TipoAvisoVisual.DESVIO
            AvisoRecorrido.LlegadaAlFinal -> TipoAvisoVisual.LLEGADA
        }
    }

    // Redactar el título y el texto de un aviso del recorrido; el estado aporta la distancia y el nombre del sendero
    fun redactarAviso(aviso: AvisoRecorrido, estado: EstadoSeguimiento?): ContenidoAvisoRecorrido {
        val tipoVisual = elegirTipoVisual(aviso)
        return when (aviso) {
            is AvisoRecorrido.CercaDePunto -> {
                val nombre = aviso.punto.nombre
                val esPeligro = tipoVisual == TipoAvisoVisual.PELIGRO
                ContenidoAvisoRecorrido(
                    titulo = if (esPeligro) {
                        "¡Cuidado! ${nombre.ifBlank { "Zona de peligro" }} cerca"
                    } else {
                        "Cerca de ${nombre.ifBlank { describirTipo(aviso.punto.tipo) }}"
                    },
                    texto = redactarCercania(nombre, aviso.punto, estado),
                    tipoVisual = tipoVisual
                )
            }
            AvisoRecorrido.Desvio -> ContenidoAvisoRecorrido(
                titulo = "Te alejaste del sendero",
                texto = if (estado != null && estado.distanciaAlTrazadoMetros > 0.0) {
                    "Estás a unos ${formatearDistanciaMetros(estado.distanciaAlTrazadoMetros)} del trazado"
                } else {
                    "Estás a más de 100 metros del trazado"
                },
                tipoVisual = tipoVisual
            )
            AvisoRecorrido.LlegadaAlFinal -> ContenidoAvisoRecorrido(
                titulo = "¡Llegaste al final!",
                texto = "Terminaste ${estado?.nombreSendero.orEmpty().ifBlank { "el sendero" }}. Tocá Finalizar para guardar el recorrido",
                tipoVisual = tipoVisual,
                permiteFinalizar = true
            )
        }
    }

    // Redactar el texto de cercanía con el nombre del punto y la distancia (o el umbral del aviso si no hay posición)
    private fun redactarCercania(nombre: String, punto: PuntoRecorrido, estado: EstadoSeguimiento?): String {
        val distancia = estado?.posicionActual?.let { posicion ->
            formatearDistanciaMetros(CalculadoraRecorrido.distanciaEntre(posicion, punto) * METROS_POR_KILOMETRO)
        }
        return when {
            nombre.isNotBlank() -> "$nombre · a ${distancia ?: "menos de 50 m"}"
            distancia != null -> "Estás a $distancia"
            else -> "Estás a menos de 50 metros"
        }
    }

    // Formatear una distancia redondeando a 10 m bajo 1 km, para que el texto no cambie con cada lectura del GPS
    fun formatearDistanciaAproximada(metros: Double): String {
        return if (metros < METROS_POR_KILOMETRO) {
            "${(metros / METROS_DE_REDONDEO).roundToInt() * METROS_DE_REDONDEO.toInt()} m"
        } else {
            formatearDistanciaMetros(metros)
        }
    }

    // Formatear una distancia en metros: metros enteros bajo 1 km y kilómetros con un decimal desde allí
    fun formatearDistanciaMetros(metros: Double): String {
        return if (metros < METROS_POR_KILOMETRO) {
            "${metros.roundToInt()} m"
        } else {
            "${formatearDecimal(metros / METROS_POR_KILOMETRO)} km"
        }
    }

    // Dar formato de un decimal con coma a un número
    private fun formatearDecimal(valor: Double): String = String.format(LOCALIZACION_ARGENTINA, "%.1f", valor)

    // Describir en español el tipo de un punto sin nombre, con artículo
    private fun describirTipo(tipo: TipoPunto): String {
        return when (tipo) {
            TipoPunto.PASO -> "un punto de paso"
            TipoPunto.MIRADOR -> "un mirador"
            TipoPunto.AGUA -> "una fuente de agua"
            TipoPunto.REFUGIO -> "un refugio"
            TipoPunto.CAMPING -> "un camping"
            TipoPunto.PELIGRO -> "una zona de peligro"
        }
    }

    // Describir en español el tipo de un punto sin nombre, sin artículo
    private fun describirTipoCorto(tipo: TipoPunto): String {
        return when (tipo) {
            TipoPunto.PASO -> "punto de paso"
            TipoPunto.MIRADOR -> "mirador"
            TipoPunto.AGUA -> "fuente de agua"
            TipoPunto.REFUGIO -> "refugio"
            TipoPunto.CAMPING -> "camping"
            TipoPunto.PELIGRO -> "zona de peligro"
        }
    }
}
