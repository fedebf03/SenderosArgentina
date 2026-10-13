package com.unaj.senderos.presentation.componentes.mapa

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.unaj.senderos.domain.model.TipoPunto

data class PaletaMapa(
    val primario: Int,
    val sobrePrimario: Int,
    val contenedorPrimario: Int,
    val sobreContenedorPrimario: Int,
    val error: Int,
    val sobreError: Int,
    val caminado: Int
)

object NombresImagenMapa {
    const val FLECHA = "imagen-flecha"
    const val FLECHA_NAVEGACION = "imagen-flecha-navegacion"
    const val PREFIJO_TIPO = "imagen-tipo-"
    const val PREFIJO_ROL = "imagen-rol-"
}

object IconosMapa {
    private const val TAMANO_INSIGNIA_DP = 22f
    private const val TAMANO_FLECHA_DP = 14f
    private const val TAMANO_FLECHA_NAVEGACION_DP = 44f

    // Crear todas las imágenes que el mapa necesita, indexadas por nombre
    fun crearTodos(paleta: PaletaMapa, densidad: Float): Map<String, Bitmap> {
        val imagenes = mutableMapOf<String, Bitmap>()
        imagenes[NombresImagenMapa.FLECHA] = crearFlecha(paleta, densidad)
        imagenes[NombresImagenMapa.FLECHA_NAVEGACION] = crearFlechaNavegacion(paleta, densidad)
        TipoPunto.entries.filter { tipo -> tipo != TipoPunto.PASO }.forEach { tipo ->
            imagenes[NombresImagenMapa.PREFIJO_TIPO + tipo.name] = crearInsigniaDeTipo(tipo, paleta, densidad)
        }
        listOf(RolesPunto.INICIO, RolesPunto.FIN, RolesPunto.INICIO_Y_FIN).forEach { rol ->
            imagenes[NombresImagenMapa.PREFIJO_ROL + rol] = crearInsigniaDeRol(rol, paleta, densidad)
        }
        return imagenes
    }

    // Crear un mapa de bits cuadrado y su lienzo con el tamaño en dp indicado
    private fun crearLienzo(tamanoDp: Float, densidad: Float): Pair<Bitmap, Canvas> {
        val lado = (tamanoDp * densidad).toInt().coerceAtLeast(1)
        val mapaDeBits = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        mapaDeBits.density = (densidad * 160f).toInt()
        return mapaDeBits to Canvas(mapaDeBits)
    }

    // Crear un pincel con color, relleno o trazo y extremos redondeados
    private fun crearPincel(color: Int, esRelleno: Boolean = true, grosor: Float = 0f): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = if (esRelleno) Paint.Style.FILL else Paint.Style.STROKE
            strokeWidth = grosor
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    // Dibujar la flecha pequeña que marca el sentido de la línea del recorrido
    private fun crearFlecha(paleta: PaletaMapa, densidad: Float): Bitmap {
        val (mapaDeBits, lienzo) = crearLienzo(TAMANO_FLECHA_DP, densidad)
        val ancho = lienzo.width.toFloat()
        val contorno = Path().apply {
            moveTo(ancho * 0.25f, ancho * 0.18f)
            lineTo(ancho * 0.85f, ancho * 0.5f)
            lineTo(ancho * 0.25f, ancho * 0.82f)
            close()
        }
        lienzo.drawPath(contorno, crearPincel(paleta.sobrePrimario))
        return mapaDeBits
    }

    // Dibujar la flecha de navegación apuntando hacia arriba: relleno del color primario y borde claro
    private fun crearFlechaNavegacion(paleta: PaletaMapa, densidad: Float): Bitmap {
        val (mapaDeBits, lienzo) = crearLienzo(TAMANO_FLECHA_NAVEGACION_DP, densidad)
        val lado = lienzo.width.toFloat()
        val contorno = Path().apply {
            moveTo(lado * 0.5f, lado * 0.08f)
            lineTo(lado * 0.86f, lado * 0.9f)
            lineTo(lado * 0.5f, lado * 0.68f)
            lineTo(lado * 0.14f, lado * 0.9f)
            close()
        }
        lienzo.drawPath(contorno, crearPincel(paleta.primario))
        lienzo.drawPath(contorno, crearPincel(paleta.sobrePrimario, esRelleno = false, grosor = lado * 0.07f))
        return mapaDeBits
    }

    // Dibujar la insignia circular con el símbolo del tipo de punto
    private fun crearInsigniaDeTipo(tipo: TipoPunto, paleta: PaletaMapa, densidad: Float): Bitmap {
        val (mapaDeBits, lienzo) = crearLienzo(TAMANO_INSIGNIA_DP, densidad)
        val lado = lienzo.width.toFloat()
        val colorFondo = if (tipo == TipoPunto.PELIGRO) paleta.error else paleta.primario
        val colorSimbolo = if (tipo == TipoPunto.PELIGRO) paleta.sobreError else paleta.sobrePrimario
        val centro = lado / 2f
        lienzo.drawCircle(centro, centro, centro * 0.92f, crearPincel(colorFondo))
        lienzo.drawCircle(centro, centro, centro * 0.92f, crearPincel(colorSimbolo, esRelleno = false, grosor = lado * 0.06f))
        val relleno = crearPincel(colorSimbolo)
        when (tipo) {
            TipoPunto.MIRADOR -> dibujarMontana(lienzo, lado, relleno)
            TipoPunto.AGUA -> dibujarGota(lienzo, lado, relleno)
            TipoPunto.REFUGIO -> dibujarCasa(lienzo, lado, relleno)
            TipoPunto.CAMPING -> dibujarCarpa(
                lienzo,
                lado,
                relleno,
                crearPincel(colorFondo, esRelleno = false, grosor = lado * 0.07f)
            )
            TipoPunto.PELIGRO -> dibujarAdmiracion(lienzo, lado, relleno)
            TipoPunto.PASO -> lienzo.drawCircle(centro, centro, lado * 0.12f, relleno)
        }
        return mapaDeBits
    }

    // Dibujar el símbolo de montaña del mirador
    private fun dibujarMontana(lienzo: Canvas, lado: Float, pincel: Paint) {
        val contorno = Path().apply {
            moveTo(lado * 0.2f, lado * 0.72f)
            lineTo(lado * 0.42f, lado * 0.3f)
            lineTo(lado * 0.56f, lado * 0.55f)
            lineTo(lado * 0.66f, lado * 0.42f)
            lineTo(lado * 0.82f, lado * 0.72f)
            close()
        }
        lienzo.drawPath(contorno, pincel)
    }

    // Dibujar el símbolo de gota del agua
    private fun dibujarGota(lienzo: Canvas, lado: Float, pincel: Paint) {
        val contorno = Path().apply {
            moveTo(lado * 0.5f, lado * 0.2f)
            lineTo(lado * 0.72f, lado * 0.59f)
            arcTo(RectF(lado * 0.28f, lado * 0.38f, lado * 0.72f, lado * 0.8f), 0f, 180f)
            close()
        }
        lienzo.drawPath(contorno, pincel)
    }

    // Dibujar el símbolo de casa del refugio
    private fun dibujarCasa(lienzo: Canvas, lado: Float, pincel: Paint) {
        val techo = Path().apply {
            moveTo(lado * 0.2f, lado * 0.5f)
            lineTo(lado * 0.5f, lado * 0.24f)
            lineTo(lado * 0.8f, lado * 0.5f)
            close()
        }
        lienzo.drawPath(techo, pincel)
        lienzo.drawRect(lado * 0.3f, lado * 0.5f, lado * 0.7f, lado * 0.76f, pincel)
    }

    // Dibujar el símbolo de carpa del camping
    private fun dibujarCarpa(lienzo: Canvas, lado: Float, relleno: Paint, entrada: Paint) {
        val carpa = Path().apply {
            moveTo(lado * 0.5f, lado * 0.24f)
            lineTo(lado * 0.82f, lado * 0.76f)
            lineTo(lado * 0.18f, lado * 0.76f)
            close()
        }
        lienzo.drawPath(carpa, relleno)
        lienzo.drawLine(lado * 0.5f, lado * 0.42f, lado * 0.5f, lado * 0.76f, entrada)
    }

    // Dibujar el símbolo de admiración del peligro
    private fun dibujarAdmiracion(lienzo: Canvas, lado: Float, pincel: Paint) {
        lienzo.drawRoundRect(
            RectF(lado * 0.45f, lado * 0.2f, lado * 0.55f, lado * 0.58f),
            lado * 0.05f,
            lado * 0.05f,
            pincel
        )
        lienzo.drawCircle(lado * 0.5f, lado * 0.72f, lado * 0.07f, pincel)
    }

    // Dibujar la insignia de inicio, fin o inicio y fin del recorrido
    private fun crearInsigniaDeRol(rol: String, paleta: PaletaMapa, densidad: Float): Bitmap {
        val (mapaDeBits, lienzo) = crearLienzo(TAMANO_INSIGNIA_DP, densidad)
        val lado = lienzo.width.toFloat()
        val centro = lado / 2f
        val colorFondo = if (rol == RolesPunto.FIN) paleta.sobreContenedorPrimario else paleta.primario
        lienzo.drawCircle(centro, centro, centro * 0.92f, crearPincel(colorFondo))
        lienzo.drawCircle(
            centro,
            centro,
            centro * 0.92f,
            crearPincel(paleta.sobrePrimario, esRelleno = false, grosor = lado * 0.06f)
        )
        val simbolo = crearPincel(paleta.sobrePrimario)
        if (rol == RolesPunto.FIN) {
            lienzo.drawRect(lado * 0.32f, lado * 0.32f, lado * 0.68f, lado * 0.68f, simbolo)
        } else {
            val triangulo = Path().apply {
                moveTo(lado * 0.38f, lado * 0.28f)
                lineTo(lado * 0.72f, lado * 0.5f)
                lineTo(lado * 0.38f, lado * 0.72f)
                close()
            }
            lienzo.drawPath(triangulo, simbolo)
            if (rol == RolesPunto.INICIO_Y_FIN) {
                lienzo.drawCircle(
                    centro,
                    centro,
                    centro * 0.7f,
                    crearPincel(paleta.sobrePrimario, esRelleno = false, grosor = lado * 0.05f)
                )
            }
        }
        return mapaDeBits
    }
}
