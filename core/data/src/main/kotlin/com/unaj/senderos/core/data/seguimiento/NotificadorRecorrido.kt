package com.unaj.senderos.data.seguimiento

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
import com.example.senderosargentina.MainActivity
import com.example.senderosargentina.R
import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.notificaciones.ContenidoNotificacionRecorrido
import com.unaj.senderos.domain.notificaciones.RedactorNotificacionRecorrido
import com.unaj.senderos.domain.notificaciones.TipoAvisoVisual

private const val TAG = "NOTIFICACION_RECORRIDO"
private const val ID_CANAL_EN_CURSO = "recorrido_en_curso"
private const val NOMBRE_CANAL_EN_CURSO = "Recorrido en curso"
private const val ID_CANAL_AVISOS = "recorrido_avisos"
private const val ID_CANAL_AVISOS_ANTERIOR = "recorrido"
private const val NOMBRE_CANAL_AVISOS = "Avisos del recorrido"
private const val ID_NOTIFICACION_EN_CURSO = 2000
private const val ID_NOTIFICACION_DESVIO = 2001
private const val ID_NOTIFICACION_LLEGADA = 2002
private const val BASE_ID_NOTIFICACION_PUNTO = 2100
private const val CODIGO_PEDIDO_ABRIR = 20
private const val CODIGO_PEDIDO_FINALIZAR = 21
private const val CODIGO_PEDIDO_CANCELAR = 22
private const val PROGRESO_MAXIMO = 100
private const val LADO_ICONO_GRANDE_PIXELES = 128
private const val MARGEN_GLIFO_PIXELES = 28

// Extra de la intención que lleva el identificador del sendero en recorrido hacia MainActivity
const val EXTRA_ID_SENDERO_RECORRIDO = "idSenderoRecorrido"

// Extra que pide abrir la pantalla del recorrido con el diálogo de confirmación de cancelación abierto
const val EXTRA_SOLICITAR_CANCELACION = "solicitarCancelacion"

// Construir y mostrar las notificaciones del recorrido: la persistente del servicio y los avisos
class NotificadorRecorrido(private val contexto: Context) {

    private val administrador: NotificationManager =
        contexto.getSystemService(NotificationManager::class.java)
    private var seCrearonCanales = false
    private var accionesCacheadas: AccionesRecorrido? = null

    // Construir la notificación persistente del servicio según el estado actual del recorrido
    fun construirNotificacionEnCurso(idSendero: String, estado: EstadoSeguimiento?, esSimulado: Boolean): Notification {
        return construirNotificacionConContenido(idSendero, RedactorNotificacionRecorrido.redactarPersistente(estado, esSimulado))
    }

    // Construir la notificación persistente con barra de progreso, texto expandido y acciones
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
        if (progreso != null) {
            constructor.setProgress(PROGRESO_MAXIMO, progreso, false)
        } else {
            constructor.setProgress(0, 0, true)
        }
        return constructor.build()
    }

    // Actualizar la notificación persistente ya publicada por el servicio
    fun actualizarNotificacionEnCurso(idSendero: String, contenido: ContenidoNotificacionRecorrido) {
        administrador.notify(ID_NOTIFICACION_EN_CURSO, construirNotificacionConContenido(idSendero, contenido))
    }

    // Obtener el identificador de la notificación persistente para iniciar el servicio en primer plano
    fun identificadorNotificacionEnCurso(): Int = ID_NOTIFICACION_EN_CURSO

    // Mostrar un aviso de proximidad, desvío o llegada por el canal de importancia alta
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

    // Elegir el ícono pequeño monocromo del aviso según su tipo
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

    // Elegir el color del aviso según su tipo
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

    // Crear el ícono grande del aviso: un círculo del color del tipo con el glifo en blanco
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

    // Cancelar los avisos que el recorrido dejó visibles al finalizar
    fun cancelarAvisos() {
        administrador.cancel(ID_NOTIFICACION_DESVIO)
        administrador.cancel(ID_NOTIFICACION_LLEGADA)
        administrador.activeNotifications
            .filter { notificacion -> notificacion.id >= BASE_ID_NOTIFICACION_PUNTO }
            .forEach { notificacion -> administrador.cancel(notificacion.id) }
    }

    // Verificar si la aplicación puede publicar avisos (requiere permiso desde API 33)
    private fun tienePermisoDeNotificaciones(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Crear los canales: uno silencioso para la persistente y otro de importancia alta, con vibración, para los avisos;
    // borra el canal anterior de avisos (Android ignora los cambios de un canal existente) y repetirlo es inofensivo
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

    // Obtener las acciones del sendero, creándolas solo cuando cambia el sendero (evita rehacer PendingIntent en cada actualización)
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

    // Crear la intención pendiente (inmutable y explícita) que abre el recorrido en curso, con la confirmación de cancelación si se pide
    private fun crearAccionAbrirRecorrido(
        idSendero: String,
        codigoPedido: Int,
        solicitarCancelacion: Boolean
    ): PendingIntent {
        val intencion = Intent(contexto, MainActivity::class.java)
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

    // Crear la intención pendiente que pide al servicio finalizar el recorrido guardándolo
    private fun crearAccionFinalizar(): PendingIntent {
        return PendingIntent.getService(
            contexto,
            CODIGO_PEDIDO_FINALIZAR,
            ServicioSeguimientoRecorrido.crearIntencionDetener(contexto),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // Acciones de las notificaciones de un sendero en recorrido, reutilizadas entre actualizaciones
    private class AccionesRecorrido(
        val idSendero: String,
        val abrir: PendingIntent,
        val cancelar: PendingIntent,
        val finalizar: PendingIntent
    )
}
