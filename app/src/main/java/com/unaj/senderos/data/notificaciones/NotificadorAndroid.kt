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
import com.unaj.senderos.domain.model.MensajeNotificacion
import com.unaj.senderos.domain.repository.Notificador

private const val TAG = "NOTIFICACIONES"
private const val ID_CANAL_SINCRONIZACION = "sincronizacion"
private const val NOMBRE_CANAL_SINCRONIZACION = "Sincronización"
private const val ID_NOTIFICACION_SUBIDOS = 1001
private const val ID_NOTIFICACION_ERROR = 1002

// Mostrar notificaciones del sistema Android sobre el resultado de la sincronización
class NotificadorAndroid(private val contexto: Context) : Notificador {

    // Mostrar la notificación del mensaje si hay permiso, registrando cualquier rechazo del sistema
    override fun notificar(mensaje: MensajeNotificacion) {
        if (!tienePermisoDeNotificaciones()) {
            Log.d(TAG, "Notificación omitida: falta el permiso POST_NOTIFICATIONS")
            return
        }
        crearCanalSiHaceFalta()
        val (identificador, titulo, cuerpo) = when (mensaje) {
            is MensajeNotificacion.SenderosSubidos -> Triple(
                ID_NOTIFICACION_SUBIDOS,
                "Senderos sincronizados",
                if (mensaje.cantidad == 1) {
                    "Tu sendero guardado sin señal ya está en la nube"
                } else {
                    "Tus ${mensaje.cantidad} senderos guardados sin señal ya están en la nube"
                }
            )
            is MensajeNotificacion.SenderosConError -> Triple(
                ID_NOTIFICACION_ERROR,
                "Error de sincronización",
                if (mensaje.cantidad == 1) {
                    "No se pudo sincronizar 1 sendero. Se reintentará al abrir la app"
                } else {
                    "No se pudieron sincronizar ${mensaje.cantidad} senderos. Se reintentará al abrir la app"
                }
            )
        }
        val notificacion = NotificationCompat.Builder(contexto, ID_CANAL_SINCRONIZACION)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setContentIntent(crearAccionAbrirAplicacion())
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(contexto).notify(identificador, notificacion)
            Log.d(TAG, "Notificación mostrada: $titulo")
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

    // Crear el intent pendiente que abre la pantalla principal al tocar la notificación
    private fun crearAccionAbrirAplicacion(): PendingIntent {
        val intent = Intent(contexto, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(contexto, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}
