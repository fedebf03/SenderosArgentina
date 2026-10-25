package com.unaj.senderos.core.domain.recorrido

import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoRecorrido
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.model.tieneCoordenadasValidas
import com.unaj.senderos.core.domain.model.Coordenada
import java.util.Locale

private const val ENCABEZADO_CSV = "Latitude,Longitude,Elevation"

object EscritorTrazados {

    // Escribimos la traza completa (con la vuelta o el cierre) y la altura solo si el perfil está al día. Así otra
    // aplicación la dibuja entera y, al volver a importarla, se detecta el mismo tipo de recorrido
    fun escribirGpx(sendero: Sendero): String {
        val puntosConAltura = trazadoConAlturas(sendero)
        val texto = StringBuilder()
        texto.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        texto.appendLine("<gpx version=\"1.1\" creator=\"SenderosArgentina\" xmlns=\"http://www.topografix.com/GPX/1/1\">")
        texto.appendLine("  <metadata><name>${escapar(sendero.nombre)}</name><desc>${escapar(sendero.descripcion)}</desc></metadata>")
        sendero.recorrido.filter { punto -> punto.tipo != TipoPunto.PASO && esValido(punto) }.forEach { punto ->
            val nombre = punto.nombre.ifBlank { nombrarTipo(punto.tipo) }
            texto.appendLine(
                "  <wpt lat=\"${punto.latitud}\" lon=\"${punto.longitud}\"><name>${escapar(nombre)}</name>" +
                    "<type>${nombrarTipo(punto.tipo)}</type></wpt>"
            )
        }
        texto.appendLine("  <trk><name>${escapar(sendero.nombre)}</name><trkseg>")
        puntosConAltura.forEach { (punto, altura) ->
            val elevacion = altura?.let { valor -> "<ele>${formatearAltura(valor)}</ele>" }.orEmpty()
            texto.appendLine("    <trkpt lat=\"${punto.latitud}\" lon=\"${punto.longitud}\">$elevacion</trkpt>")
        }
        texto.appendLine("  </trkseg></trk>")
        texto.appendLine("</gpx>")
        return texto.toString()
    }

    // Usamos el mismo encabezado que las aplicaciones de senderismo
    fun escribirCsv(sendero: Sendero): String {
        val filas = trazadoConAlturas(sendero).map { (punto, altura) ->
            "${punto.latitud},${punto.longitud},${altura?.let(::formatearAltura).orEmpty()}"
        }
        return (listOf(ENCABEZADO_CSV) + filas).joinToString(separator = "\n", postfix = "\n")
    }

    // Un perfil calculado con otro trazado no se usa, porque sus alturas ya no corresponden a estos puntos
    private fun trazadoConAlturas(sendero: Sendero): List<Pair<PuntoRecorrido, Double?>> {
        val validos = sendero.recorrido.filter(::esValido)
        val trazado = CalculadoraSeguimiento.construirTrazado(validos, sendero.tipoRecorrido)
        val perfil = sendero.perfilElevacion?.takeIf { perfil ->
            perfil.firmaTrazado == CalculadoraPerfil.firmaTrazado(sendero.recorrido, sendero.tipoRecorrido)
        }
        if (perfil == null || trazado.size < 2) return trazado.map { punto -> punto to null }
        val acumuladas = CalculadoraRecorrido.distanciasAcumuladasKilometros(trazado)
        return trazado.mapIndexed { indice, punto -> punto to alturaEnElPerfil(perfil, acumuladas[indice]) }
    }

    private fun alturaEnElPerfil(perfil: PerfilElevacion, kilometros: Double): Double? {
        val puntos = perfil.puntos
        if (puntos.isEmpty()) return null
        val siguiente = puntos.indexOfFirst { punto -> punto.distanciaKilometros >= kilometros }
        if (siguiente < 0) return puntos.last().altitudMetros
        if (siguiente == 0) return puntos.first().altitudMetros
        val anterior = puntos[siguiente - 1]
        val tramo = puntos[siguiente].distanciaKilometros - anterior.distanciaKilometros
        val fraccion = if (tramo <= 0.0) 0.0 else (kilometros - anterior.distanciaKilometros) / tramo
        return anterior.altitudMetros + (puntos[siguiente].altitudMetros - anterior.altitudMetros) * fraccion
    }

    // Con Locale.ROOT la altura lleva punto decimal, como esperan los archivos de intercambio
    private fun formatearAltura(metros: Double): String = String.format(Locale.ROOT, "%.1f", metros)

    private fun esValido(punto: PuntoRecorrido): Boolean = Coordenada(punto.latitud, punto.longitud).tieneCoordenadasValidas()

    private fun nombrarTipo(tipo: TipoPunto): String = when (tipo) {
        TipoPunto.PASO -> "Paso"
        TipoPunto.MIRADOR -> "Mirador"
        TipoPunto.AGUA -> "Agua"
        TipoPunto.REFUGIO -> "Refugio"
        TipoPunto.CAMPING -> "Camping"
        TipoPunto.PELIGRO -> "Peligro"
        TipoPunto.OTRO -> "Otro"
    }

    private fun escapar(texto: String): String = texto
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}
