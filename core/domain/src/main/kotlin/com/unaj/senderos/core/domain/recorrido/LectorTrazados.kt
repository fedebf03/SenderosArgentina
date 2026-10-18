package com.unaj.senderos.core.domain.recorrido

// Punto leído de un archivo de otra aplicación; la altura es null si el archivo no la trae
data class PuntoLeido(val latitud: Double, val longitud: Double, val altitudMetros: Double?)

// Punto marcado en el archivo (waypoint de un GPX), con su nombre
data class PuntoDeInteresLeido(val latitud: Double, val longitud: Double, val nombre: String)

// Contenido de un archivo de trazado: nombre si lo trae, puntos en orden y puntos marcados
data class TrazadoLeido(val nombre: String?, val puntos: List<PuntoLeido>, val puntosDeInteres: List<PuntoDeInteresLeido>)

private val NOMBRES_LATITUD = setOf("lat", "latitude", "latitud")
private val NOMBRES_LONGITUD = setOf("lon", "lng", "long", "longitude", "longitud")
private val NOMBRES_ALTURA = setOf("ele", "elevation", "altitude", "altitud", "altura", "alt")

// Solo la etiqueta de apertura: [^>]* no puede retroceder más allá del próximo ">", así que el costo es lineal
private val PATRON_APERTURA_PUNTO_GPX = Regex("<(trkpt|rtept|wpt)\\b([^>]*)>")
private val PATRON_APERTURA_TRAZA_GPX = Regex("<(?:trk|rte|metadata)\\b[^>]*>")
private val PATRON_ATRIBUTO_LATITUD = Regex("\\blat\\s*=\\s*[\"']([^\"']+)[\"']")
private val PATRON_ATRIBUTO_LONGITUD = Regex("\\blon\\s*=\\s*[\"']([^\"']+)[\"']")
private val PATRON_ALTURA_GPX = Regex("<ele>\\s*([^<]+?)\\s*</ele>")
private val PATRON_NOMBRE_GPX = Regex("<name>\\s*([^<]*?)\\s*</name>")

// Un trazado de sendero, aun de varios días con un punto por segundo, queda muy por debajo; más es un archivo equivocado
const val MAXIMO_PUNTOS_LEIDOS = 100_000
private const val MAXIMO_COLUMNAS_CSV = 64
private const val LARGO_MAXIMO_CONTENIDO_PUNTO = 4_096
private const val LARGO_MAXIMO_ENCABEZADO_TRAZA = 2_048

private const val MENSAJE_SIN_PUNTOS = "El archivo no tiene puntos de recorrido válidos"
private const val MENSAJE_DEMASIADOS_PUNTOS = "El archivo tiene demasiados puntos para un sendero (más de 100.000)"

object LectorTrazados {

    // Leer un trazado GPX o CSV: se reconoce por el contenido (un GPX empieza con una etiqueta XML), no por la extensión,
    // porque las aplicaciones de archivos suelen entregar los GPX sin un tipo claro. Falla si no hay ningún punto válido
    // o si hay más de 100.000
    fun leer(texto: String, nombreArchivo: String): Result<TrazadoLeido> {
        val limpio = texto.removePrefix("\uFEFF").trim()
        val leido = (if (limpio.startsWith("<")) leerGpx(limpio) else leerCsv(limpio))
            ?: return Result.failure(IllegalArgumentException(MENSAJE_DEMASIADOS_PUNTOS))
        if (leido.puntos.isEmpty()) return Result.failure(IllegalArgumentException(MENSAJE_SIN_PUNTOS))
        return Result.success(leido)
    }

    // Leer un GPX sin un analizador XML (así no se resuelven entidades externas): busca cada etiqueta de punto y toma su
    // contenido hasta el cierre, sin pasar de la próxima etiqueta de punto. Usa los puntos de la traza o, si no hay, los de la
    // ruta; los waypoints pasan a ser puntos de interés. Devuelve null si hay demasiados puntos
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

    // Tomar el texto entre la apertura y su cierre, sin pasar de la próxima apertura ni de 4 KB; vacío si no se cierra
    private fun contenidoHastaElCierre(texto: String, apertura: MatchResult, siguiente: MatchResult?, etiqueta: String): String {
        val desde = apertura.range.last + 1
        val limite = minOf(siguiente?.range?.first ?: texto.length, desde + LARGO_MAXIMO_CONTENIDO_PUNTO, texto.length)
        val cierre = texto.indexOf("</$etiqueta", desde)
        return if (cierre in desde until limite) texto.substring(desde, cierre) else ""
    }

    // Interpretar las coordenadas y la altura o el nombre de un punto del GPX y sumarlo a la lista que le corresponde
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

    // Buscar el nombre de la primera traza, ruta o de los metadatos, mirando solo el principio de esa sección
    private fun leerNombreDelGpx(texto: String): String? {
        val seccion = PATRON_APERTURA_TRAZA_GPX.find(texto) ?: return null
        val desde = seccion.range.last + 1
        val encabezado = texto.substring(desde, minOf(texto.length, desde + LARGO_MAXIMO_ENCABEZADO_TRAZA))
        val antesDeLosPuntos = encabezado.substringBefore("<trkpt").substringBefore("<rtept").substringBefore("<trkseg")
        return PATRON_NOMBRE_GPX.find(antesDeLosPuntos)?.groupValues?.get(1)?.let(::decodificarEntidades)?.takeIf { valor -> valor.isNotBlank() }
    }

    // Leer un CSV con coma o punto y coma como separador. Si la primera línea es un encabezado, ubica las columnas por nombre
    // (en inglés o castellano); si no, toma latitud, longitud y altura en ese orden. Saltea las líneas que no son un punto válido.
    // Recorre las líneas de a una y parte cada una solo hasta la última columna que necesita, para no llenar la memoria.
    // Devuelve null si hay demasiados puntos
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

    // Interpretar un número; con punto y coma como separador, la coma puede ser la coma decimal
    private fun interpretarNumero(celda: String, separador: Char): Double? {
        val normalizado = if (separador == ';') celda.replace(',', '.') else celda
        return normalizado.toDoubleOrNull()?.takeIf { valor -> valor.isFinite() }
    }

    // Indicar si las coordenadas están dentro del rango geográfico
    private fun sonCoordenadasValidas(latitud: Double, longitud: Double): Boolean =
        latitud in -90.0..90.0 && longitud in -180.0..180.0

    // Reemplazar las entidades XML más comunes de los nombres
    private fun decodificarEntidades(texto: String): String = texto
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
}
