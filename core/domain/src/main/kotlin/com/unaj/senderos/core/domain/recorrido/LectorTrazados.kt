package com.unaj.senderos.core.domain.recorrido

data class PuntoLeido(val latitud: Double, val longitud: Double, val altitudMetros: Double?)

// Corresponde a un waypoint del GPX
data class PuntoDeInteresLeido(val latitud: Double, val longitud: Double, val nombre: String)

data class TrazadoLeido(val nombre: String?, val puntos: List<PuntoLeido>, val puntosDeInteres: List<PuntoDeInteresLeido>)

private val NOMBRES_LATITUD = setOf("lat", "latitude", "latitud")
private val NOMBRES_LONGITUD = setOf("lon", "lng", "long", "longitude", "longitud")
private val NOMBRES_ALTURA = setOf("ele", "elevation", "altitude", "altitud", "altura", "alt")

private val PATRON_APERTURA_PUNTO_GPX = Regex("<(trkpt|rtept|wpt)\\b([^>]*)>")
private val PATRON_APERTURA_TRAZA_GPX = Regex("<(?:trk|rte|metadata)\\b[^>]*>")
private val PATRON_ATRIBUTO_LATITUD = Regex("\\blat\\s*=\\s*[\"']([^\"']+)[\"']")
private val PATRON_ATRIBUTO_LONGITUD = Regex("\\blon\\s*=\\s*[\"']([^\"']+)[\"']")
private val PATRON_ALTURA_GPX = Regex("<ele>\\s*([^<]+?)\\s*</ele>")
private val PATRON_NOMBRE_GPX = Regex("<name>\\s*([^<]*?)\\s*</name>")

// Un trazado de varios días con un punto por segundo queda muy por debajo. Más que eso es un archivo equivocado
const val MAXIMO_PUNTOS_LEIDOS = 100_000
private const val MAXIMO_COLUMNAS_CSV = 64
private const val LARGO_MAXIMO_CONTENIDO_PUNTO = 4_096
private const val LARGO_MAXIMO_ENCABEZADO_TRAZA = 2_048

private const val MENSAJE_SIN_PUNTOS = "El archivo no tiene puntos de recorrido válidos"
private const val MENSAJE_DEMASIADOS_PUNTOS = "El archivo tiene demasiados puntos para un sendero (más de 100.000)"

object LectorTrazados {

    // El formato se reconoce por el contenido y no por la extensión, porque las aplicaciones de archivos suelen entregar
    // los GPX sin un tipo claro
    fun leer(texto: String, nombreArchivo: String): Result<TrazadoLeido> {
        val limpio = texto.removePrefix("\uFEFF").trim()
        val leido = (if (limpio.startsWith("<")) leerGpx(limpio) else leerCsv(limpio))
            ?: return Result.failure(IllegalArgumentException(MENSAJE_DEMASIADOS_PUNTOS))
        if (leido.puntos.isEmpty()) return Result.failure(IllegalArgumentException(MENSAJE_SIN_PUNTOS))
        return Result.success(leido)
    }

    // No usamos un analizador XML porque podría cargar entidades externas declaradas en el archivo (ataque XXE). Se toman
    // los puntos de la traza o, si no hay, los de la ruta, y los waypoints pasan a ser puntos de interés
    private fun leerGpx(texto: String): TrazadoLeido? {
        val trazas = mutableListOf<PuntoLeido>()
        val ruta = mutableListOf<PuntoLeido>()
        val puntosDeInteres = mutableListOf<PuntoDeInteresLeido>()
        var apertura = PATRON_APERTURA_PUNTO_GPX.find(texto)
        while (apertura != null) {
            if (trazas.size + ruta.size + puntosDeInteres.size >= MAXIMO_PUNTOS_LEIDOS) return null
            val siguiente = PATRON_APERTURA_PUNTO_GPX.find(texto, apertura.range.last + 1)
            val etiqueta = apertura.groupValues[1]
            val atributos = apertura.groupValues[2]
            val contenido = if (atributos.trimEnd().endsWith("/")) "" else contenidoHastaElCierre(texto, apertura, siguiente, etiqueta)
            agregarPunto(etiqueta, atributos, contenido, trazas, ruta, puntosDeInteres)
            apertura = siguiente
        }
        return TrazadoLeido(leerNombreDelGpx(texto), trazas.ifEmpty { ruta }, puntosDeInteres)
    }

    private fun contenidoHastaElCierre(texto: String, apertura: MatchResult, siguiente: MatchResult?, etiqueta: String): String {
        val desde = apertura.range.last + 1
        val limite = minOf(siguiente?.range?.first ?: texto.length, desde + LARGO_MAXIMO_CONTENIDO_PUNTO, texto.length)
        val marcaCierre = "</$etiqueta"
        val tramo = texto.substring(desde, minOf(texto.length, limite + marcaCierre.length - 1))
        val cierre = tramo.indexOf(marcaCierre)
        return if (cierre in 0 until limite - desde) tramo.substring(0, cierre) else ""
    }

    private fun agregarPunto(
        etiqueta: String,
        atributos: String,
        contenido: String,
        trazas: MutableList<PuntoLeido>,
        ruta: MutableList<PuntoLeido>,
        puntosDeInteres: MutableList<PuntoDeInteresLeido>
    ) {
        val latitud = PATRON_ATRIBUTO_LATITUD.find(atributos)?.groupValues?.get(1)?.trim()?.toDoubleOrNull()
        val longitud = PATRON_ATRIBUTO_LONGITUD.find(atributos)?.groupValues?.get(1)?.trim()?.toDoubleOrNull()
        if (latitud == null || longitud == null || !sonCoordenadasValidas(latitud, longitud)) return
        val altura = PATRON_ALTURA_GPX.find(contenido)?.groupValues?.get(1)?.toDoubleOrNull()?.takeIf { valor -> valor.isFinite() }
        when (etiqueta) {
            "trkpt" -> trazas.add(PuntoLeido(latitud, longitud, altura))
            "rtept" -> ruta.add(PuntoLeido(latitud, longitud, altura))
            else -> {
                val nombre = PATRON_NOMBRE_GPX.find(contenido)?.groupValues?.get(1)?.let(::decodificarEntidades).orEmpty()
                puntosDeInteres.add(PuntoDeInteresLeido(latitud, longitud, nombre))
            }
        }
    }

    // Se mira solo el principio de la sección, antes de los puntos, para no tomar el nombre de alguno de ellos
    private fun leerNombreDelGpx(texto: String): String? {
        val seccion = PATRON_APERTURA_TRAZA_GPX.find(texto) ?: return null
        val desde = seccion.range.last + 1
        val encabezado = texto.substring(desde, minOf(texto.length, desde + LARGO_MAXIMO_ENCABEZADO_TRAZA))
        val antesDeLosPuntos = encabezado.substringBefore("<trkpt").substringBefore("<rtept").substringBefore("<trkseg")
        return PATRON_NOMBRE_GPX.find(antesDeLosPuntos)?.groupValues?.get(1)?.let(::decodificarEntidades)?.takeIf { valor -> valor.isNotBlank() }
    }

    // Si la primera línea es un encabezado, las columnas se ubican por nombre (en inglés o castellano). Cada línea
    // se parte solo hasta la última columna que hace falta, para no llenar la memoria
    private fun leerCsv(texto: String): TrazadoLeido? {
        val lineas = texto.lineSequence().map { linea -> linea.trim() }.filter { linea -> linea.isNotEmpty() }.iterator()
        if (!lineas.hasNext()) return TrazadoLeido(null, emptyList(), emptyList())
        val primeraLinea = lineas.next()
        val separador = if (primeraLinea.contains(';')) ';' else ','
        val primera = primeraLinea.split(separador, limit = MAXIMO_COLUMNAS_CSV).map { celda -> celda.trim().trim('"').lowercase() }
        val esEncabezado = primera.none { celda -> interpretarNumero(celda, separador) != null }
        val columnaLatitud = if (esEncabezado) primera.indexOfFirst { celda -> celda in NOMBRES_LATITUD } else 0
        val columnaLongitud = if (esEncabezado) primera.indexOfFirst { celda -> celda in NOMBRES_LONGITUD } else 1
        val columnaAltura = if (esEncabezado) primera.indexOfFirst { celda -> celda in NOMBRES_ALTURA } else 2
        if (columnaLatitud < 0 || columnaLongitud < 0) return TrazadoLeido(null, emptyList(), emptyList())
        val columnasNecesarias = maxOf(columnaLatitud, columnaLongitud, columnaAltura) + 2
        val puntos = mutableListOf<PuntoLeido>()
        val lineasDeDatos = if (esEncabezado) lineas.asSequence() else sequenceOf(primeraLinea) + lineas.asSequence()
        for (linea in lineasDeDatos) {
            val celdas = linea.split(separador, limit = columnasNecesarias).map { celda -> celda.trim().trim('"') }
            val latitud = celdas.getOrNull(columnaLatitud)?.let { celda -> interpretarNumero(celda, separador) }
            val longitud = celdas.getOrNull(columnaLongitud)?.let { celda -> interpretarNumero(celda, separador) }
            if (latitud == null || longitud == null || !sonCoordenadasValidas(latitud, longitud)) continue
            if (puntos.size >= MAXIMO_PUNTOS_LEIDOS) return null
            val altura = celdas.getOrNull(columnaAltura)?.let { celda -> interpretarNumero(celda.substringBefore(separador), separador) }
            puntos.add(PuntoLeido(latitud, longitud, altura))
        }
        return TrazadoLeido(null, puntos, emptyList())
    }

    // Con punto y coma como separador, la coma puede ser la coma decimal
    private fun interpretarNumero(celda: String, separador: Char): Double? {
        val normalizado = if (separador == ';') celda.replace(',', '.') else celda
        return normalizado.toDoubleOrNull()?.takeIf { valor -> valor.isFinite() }
    }

    private fun sonCoordenadasValidas(latitud: Double, longitud: Double): Boolean =
        latitud in -90.0..90.0 && longitud in -180.0..180.0

    private fun decodificarEntidades(texto: String): String = texto
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
}
