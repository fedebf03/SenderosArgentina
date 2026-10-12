package com.unaj.senderos.data.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.senderosargentina.MainActivity
import com.example.senderosargentina.R
import com.unaj.senderos.domain.RedactorNotificacionSincronizacion
import com.unaj.senderos.domain.model.MensajeNotificacion
import com.unaj.senderos.domain.repository.Notificador

private const val TAG = "NOTIFICACIONES"
private const val ID_CANAL_SINCRONIZACION = "sincronizacion"
private const val NOMBRE_CANAL_SINCRONIZACION = "Sincronización"
private const val ID_NOTIFICACION_SUBIDOS = 1001
private const val ID_NOTIFICACION_ERROR = 1002
private const val CODIGO_PEDIDO_REINTENTAR = 1

// Mostrar notificaciones del sistema Android sobre el resultado de la sincronización
class NotificadorAndroid(private val contexto: Context) : Notificador {

    // Mostrar la notificación del mensaje si hay permiso, registrando cualquier rechazo del sistema
    override fun notificar(mensaje: MensajeNotificacion) {
        if (!tienePermisoDeNotificaciones()) {
            Log.d(TAG, "Notificación omitida: falta el permiso POST_NOTIFICATIONS")
            return
        }
        crearCanalSiHaceFalta()
        val contenido = RedactorNotificacionSincronizacion.redactar(mensaje)
        val identificador = when (mensaje) {
            is MensajeNotificacion.SenderosSubidos -> ID_NOTIFICACION_SUBIDOS
            is MensajeNotificacion.SenderosConError -> ID_NOTIFICACION_ERROR
        }
        val constructor = NotificationCompat.Builder(contexto, ID_CANAL_SINCRONIZACION)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(ContextCompat.getColor(contexto, R.color.color_notificacion))
            .setContentTitle(contenido.titulo)
            .setContentText(contenido.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contenido.texto))
            .setContentIntent(crearAccionAbrirAplicacion())
            .setAutoCancel(true)
        if (contenido.permiteReintentar) {
            constructor.addAction(0, "Reintentar", crearAccionReintentar())
        }
        try {
            NotificationManagerCompat.from(contexto).notify(identificador, constructor.build())
            Log.d(TAG, "Notificación mostrada: ${contenido.titulo}")
        } catch (excepcion: SecurityException) {
            Log.e(TAG, "El sistema rechazó la notificación", excepcion)
        }
    }

    // Cancelar la notificación de error de sincronización mostrada antes
    override fun cancelarAvisoDeError() {
        NotificationManagerCompat.from(contexto).cancel(ID_NOTIFICACION_ERROR)
    }

    // Verificar si la aplicación puede publicar notificaciones (requiere permiso en API 33 o superior)
    private fun tienePermisoDeNotificaciones(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Crear el canal de sincronización; repetirlo es inofensivo: Android actualiza el nombre y la descripción de un canal existente e ignora el cambio de importancia
    private fun crearCanalSiHaceFalta() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            ID_CANAL_SINCRONIZACION,
            NOMBRE_CANAL_SINCRONIZACION,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        NotificationManagerCompat.from(contexto).createNotificationChannel(canal)
    }

    // Crear la intención pendiente, inmutable y explícito, que dirige "Reintentar" al receptor interno de la aplicación
    private fun crearAccionReintentar(): PendingIntent {
        val intencion = Intent(contexto, ReintentoSincronizacionReceptor::class.java)
        return PendingIntent.getBroadcast(contexto, CODIGO_PEDIDO_REINTENTAR, intencion, PendingIntent.FLAG_IMMUTABLE)
    }

    // Crear la intención pendiente que abre la pantalla principal al tocar la notificación
    private fun crearAccionAbrirAplicacion(): PendingIntent {
        val intencion = Intent(contexto, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(contexto, 0, intencion, PendingIntent.FLAG_IMMUTABLE)
    }
}
