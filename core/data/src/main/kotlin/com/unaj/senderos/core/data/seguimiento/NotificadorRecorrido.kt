package com.unaj.senderos.core.data.seguimiento

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.unaj.senderos.core.data.R
import com.unaj.senderos.core.domain.model.AvisoRecorrido
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.notificaciones.ContenidoNotificacionRecorrido
import com.unaj.senderos.core.domain.notificaciones.RedactorNotificacionRecorrido
import com.unaj.senderos.core.domain.notificaciones.TipoAvisoVisual
import com.unaj.senderos.core.data.notificaciones.intencionAbrirAplicacion

private const val TAG = "NOTIFICACION_RECORRIDO"
private const val ID_CANAL_EN_CURSO = "recorrido_en_curso"
private const val NOMBRE_CANAL_EN_CURSO = "Recorrido en curso"
private const val ID_CANAL_AVISOS = "recorrido_avisos"
private const val ID_CANAL_AVISOS_ANTERIOR = "recorrido"
private const val NOMBRE_CANAL_AVISOS = "Avisos del recorrido"
private const val ID_NOTIFICACION_EN_CURSO = 2000
private const val ID_NOTIFICACION_DESVIO = 2001
private const val ID_NOTIFICACION_LLEGADA = 2002
private const val BASE_ID_NOTIFICACION_CLIMA = 2010
private const val BASE_ID_NOTIFICACION_PRESION = 2020
private const val CANTIDAD_ID_NOTIFICACION_AMBIENTE = 10
private const val BASE_ID_NOTIFICACION_PUNTO = 2100
private const val CODIGO_PEDIDO_ABRIR = 20
private const val CODIGO_PEDIDO_FINALIZAR = 21
private const val CODIGO_PEDIDO_CANCELAR = 22
private const val PROGRESO_MAXIMO = 100
private const val LADO_ICONO_GRANDE_PIXELES = 128
private const val MARGEN_GLIFO_PIXELES = 28

const val EXTRA_ID_SENDERO_RECORRIDO = "idSenderoRecorrido"

// Extra que pide abrir la pantalla del recorrido con el diálogo de confirmación de cancelación abierto
const val EXTRA_SOLICITAR_CANCELACION = "solicitarCancelacion"

class NotificadorRecorrido(private val contexto: Context) {

    private val administrador: NotificationManager =
        contexto.getSystemService(NotificationManager::class.java)
    private var seCrearonCanales = false
    private var accionesCacheadas: AccionesRecorrido? = null

    fun construirNotificacionEnCurso(idSendero: String, estado: EstadoSeguimiento?, esSimulado: Boolean): Notification {
        return construirNotificacionConContenido(idSendero, RedactorNotificacionRecorrido.redactarPersistente(estado, esSimulado))
    }

    private fun construirNotificacionConContenido(idSendero: String, contenido: ContenidoNotificacionRecorrido): Notification {
        crearCanales()
        val acciones = obtenerAcciones(idSendero)
        val constructor = NotificationCompat.Builder(contexto, ID_CANAL_EN_CURSO)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(ContextCompat.getColor(contexto, R.color.color_notificacion))
            .setContentTitle(contenido.titulo)
            .setContentText(contenido.textoCorto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contenido.textoExpandido))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setContentIntent(acciones.abrir)
            .addAction(0, "Ver en el mapa", acciones.abrir)
            .addAction(0, "Finalizar", acciones.finalizar)
            .addAction(0, "Cancelar", acciones.cancelar)
        val instanteInicio = contenido.instanteInicioMilisegundos
        if (instanteInicio != null) {
            // El cronómetro del sistema muestra el tiempo sin que haya que republicar la notificación cada segundo
            constructor.setUsesChronometer(true).setWhen(instanteInicio).setShowWhen(true)
        } else {
            constructor.setShowWhen(false)
        }
        val progreso = contenido.progresoPorcentaje
        if (!contenido.tieneProgreso) {
            constructor.setProgress(0, 0, false)
        } else if (progreso != null) {
            constructor.setProgress(PROGRESO_MAXIMO, progreso, false)
        } else {
            constructor.setProgress(0, 0, true)
        }
        return constructor.build()
    }

    fun actualizarNotificacionEnCurso(idSendero: String, contenido: ContenidoNotificacionRecorrido) {
        administrador.notify(ID_NOTIFICACION_EN_CURSO, construirNotificacionConContenido(idSendero, contenido))
    }

    fun identificadorNotificacionEnCurso(): Int = ID_NOTIFICACION_EN_CURSO

    // RF7 - Notificaciones: durante el recorrido avisamos la cercanía a un punto, el desvío y la llegada al final
    fun notificarAviso(idSendero: String, aviso: AvisoRecorrido, estado: EstadoSeguimiento?) {
        if (!tienePermisoDeNotificaciones()) {
            Log.d(TAG, "Aviso omitido: falta el permiso POST_NOTIFICATIONS")
            return
        }
        crearCanales()
        val acciones = obtenerAcciones(idSendero)
        val contenido = RedactorNotificacionRecorrido.redactarAviso(aviso, estado)
        val identificador = when (aviso) {
            is AvisoRecorrido.CercaDePunto -> BASE_ID_NOTIFICACION_PUNTO + aviso.indice
            AvisoRecorrido.Desvio -> ID_NOTIFICACION_DESVIO
            AvisoRecorrido.LlegadaAlFinal -> ID_NOTIFICACION_LLEGADA
        }
        val constructor = NotificationCompat.Builder(contexto, ID_CANAL_AVISOS)
            .setSmallIcon(elegirIconoPequeno(contenido.tipoVisual))
            .setLargeIcon(crearIconoGrande(contenido.tipoVisual))
            .setColor(ContextCompat.getColor(contexto, elegirColor(contenido.tipoVisual)))
            .setContentTitle(contenido.titulo)
            .setContentText(contenido.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contenido.texto))
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setContentIntent(acciones.abrir)
            .setAutoCancel(true)
        if (contenido.permiteFinalizar) {
            constructor.addAction(0, "Finalizar", acciones.finalizar)
        }
        try {
            administrador.notify(identificador, constructor.build())
            Log.d(TAG, "Aviso mostrado: ${contenido.titulo}")
        } catch (error: SecurityException) {
            Log.e(TAG, "El sistema rechazó el aviso del recorrido", error)
        }
    }

    // Cada tipo de aviso del ambiente tiene su identificador, así uno nuevo reemplaza al anterior del mismo tipo
    fun notificarAvisoAmbiente(idSendero: String, aviso: AvisoAmbiente) {
        if (!tienePermisoDeNotificaciones()) {
            Log.d(TAG, "Aviso del ambiente omitido: falta el permiso POST_NOTIFICATIONS")
            return
        }
        crearCanales()
        val (identificador, titulo, texto) = when (aviso) {
            is AvisoAmbiente.Clima -> Triple(BASE_ID_NOTIFICACION_CLIMA + aviso.alerta.ordinal, "Aviso de clima: ${aviso.alerta.titulo}", aviso.alerta.detalle)
            is AvisoAmbiente.Presion -> Triple(BASE_ID_NOTIFICACION_PRESION + aviso.aviso.ordinal, aviso.aviso.titulo, aviso.aviso.detalle)
        }
        val constructor = NotificationCompat.Builder(contexto, ID_CANAL_AVISOS)
            .setSmallIcon(elegirIconoPequeno(TipoAvisoVisual.PELIGRO))
            .setLargeIcon(crearIconoGrande(TipoAvisoVisual.PELIGRO))
            .setColor(ContextCompat.getColor(contexto, elegirColor(TipoAvisoVisual.PELIGRO)))
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(obtenerAcciones(idSendero).abrir)
            .setAutoCancel(true)
        try {
            administrador.notify(identificador, constructor.build())
            Log.d(TAG, "Aviso del ambiente mostrado: $titulo")
        } catch (error: SecurityException) {
            Log.e(TAG, "El sistema rechazó el aviso del ambiente", error)
        }
    }

    @DrawableRes
    private fun elegirIconoPequeno(tipo: TipoAvisoVisual): Int {
        return when (tipo) {
            TipoAvisoVisual.AGUA -> R.drawable.ic_aviso_agua
            TipoAvisoVisual.PELIGRO -> R.drawable.ic_aviso_peligro
            TipoAvisoVisual.MIRADOR -> R.drawable.ic_aviso_mirador
            TipoAvisoVisual.REFUGIO -> R.drawable.ic_aviso_refugio
            TipoAvisoVisual.CAMPING -> R.drawable.ic_aviso_camping
            TipoAvisoVisual.PASO -> R.drawable.ic_notificacion
            TipoAvisoVisual.DESVIO -> R.drawable.ic_aviso_desvio
            TipoAvisoVisual.LLEGADA -> R.drawable.ic_aviso_llegada
        }
    }

    @ColorRes
    private fun elegirColor(tipo: TipoAvisoVisual): Int {
        return when (tipo) {
            TipoAvisoVisual.AGUA -> R.color.aviso_agua
            TipoAvisoVisual.PELIGRO -> R.color.aviso_peligro
            TipoAvisoVisual.MIRADOR -> R.color.aviso_mirador
            TipoAvisoVisual.REFUGIO -> R.color.aviso_refugio
            TipoAvisoVisual.CAMPING -> R.color.aviso_camping
            TipoAvisoVisual.PASO -> R.color.color_notificacion
            TipoAvisoVisual.DESVIO -> R.color.aviso_desvio
            TipoAvisoVisual.LLEGADA -> R.color.aviso_llegada
        }
    }

    private fun crearIconoGrande(tipo: TipoAvisoVisual): Bitmap {
        val mapaDeBits = Bitmap.createBitmap(LADO_ICONO_GRANDE_PIXELES, LADO_ICONO_GRANDE_PIXELES, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(mapaDeBits)
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(contexto, elegirColor(tipo)) }
        val radio = LADO_ICONO_GRANDE_PIXELES / 2f
        lienzo.drawCircle(radio, radio, radio, pincel)
        ContextCompat.getDrawable(contexto, elegirIconoPequeno(tipo))?.mutate()?.let { glifo ->
            glifo.setBounds(
                MARGEN_GLIFO_PIXELES,
                MARGEN_GLIFO_PIXELES,
                LADO_ICONO_GRANDE_PIXELES - MARGEN_GLIFO_PIXELES,
                LADO_ICONO_GRANDE_PIXELES - MARGEN_GLIFO_PIXELES
            )
            glifo.draw(lienzo)
        }
        return mapaDeBits
    }

    fun cancelarAvisos() {
        administrador.cancel(ID_NOTIFICACION_DESVIO)
        administrador.cancel(ID_NOTIFICACION_LLEGADA)
        for (desplazamiento in 0 until CANTIDAD_ID_NOTIFICACION_AMBIENTE) {
            administrador.cancel(BASE_ID_NOTIFICACION_CLIMA + desplazamiento)
            administrador.cancel(BASE_ID_NOTIFICACION_PRESION + desplazamiento)
        }
        administrador.activeNotifications
            .filter { notificacion -> notificacion.id >= BASE_ID_NOTIFICACION_PUNTO }
            .forEach { notificacion -> administrador.cancel(notificacion.id) }
    }

    private fun tienePermisoDeNotificaciones(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Android ignora los cambios de un canal que ya existe, por eso se borra el canal anterior de avisos
    private fun crearCanales() {
        if (seCrearonCanales) return
        seCrearonCanales = true
        administrador.deleteNotificationChannel(ID_CANAL_AVISOS_ANTERIOR)
        administrador.createNotificationChannel(
            NotificationChannel(ID_CANAL_EN_CURSO, NOMBRE_CANAL_EN_CURSO, NotificationManager.IMPORTANCE_LOW)
        )
        val canalAvisos = NotificationChannel(ID_CANAL_AVISOS, NOMBRE_CANAL_AVISOS, NotificationManager.IMPORTANCE_HIGH)
        canalAvisos.enableVibration(true)
        administrador.createNotificationChannel(canalAvisos)
    }

    // Se recrean solo cuando cambia el sendero, para no rehacer los PendingIntent en cada actualización
    private fun obtenerAcciones(idSendero: String): AccionesRecorrido {
        accionesCacheadas?.takeIf { acciones -> acciones.idSendero == idSendero }?.let { acciones -> return acciones }
        val acciones = AccionesRecorrido(
            idSendero = idSendero,
            abrir = crearAccionAbrirRecorrido(idSendero, CODIGO_PEDIDO_ABRIR, solicitarCancelacion = false),
            cancelar = crearAccionAbrirRecorrido(idSendero, CODIGO_PEDIDO_CANCELAR, solicitarCancelacion = true),
            finalizar = crearAccionFinalizar()
        )
        accionesCacheadas = acciones
        return acciones
    }

    private fun crearAccionAbrirRecorrido(
        idSendero: String,
        codigoPedido: Int,
        solicitarCancelacion: Boolean
    ): PendingIntent {
        val intencion = contexto.intencionAbrirAplicacion()
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_ID_SENDERO_RECORRIDO, idSendero)
        if (solicitarCancelacion) intencion.putExtra(EXTRA_SOLICITAR_CANCELACION, true)
        return PendingIntent.getActivity(
            contexto,
            codigoPedido,
            intencion,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun crearAccionFinalizar(): PendingIntent {
        return PendingIntent.getService(
            contexto,
            CODIGO_PEDIDO_FINALIZAR,
            ServicioSeguimientoRecorrido.crearIntencionDetener(contexto),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private class AccionesRecorrido(
        val idSendero: String,
        val abrir: PendingIntent,
        val cancelar: PendingIntent,
        val finalizar: PendingIntent
    )
}
