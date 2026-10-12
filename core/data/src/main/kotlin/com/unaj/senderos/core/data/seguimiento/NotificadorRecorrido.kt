package com.unaj.senderos.data.seguimiento

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.senderosargentina.MainActivity
import com.example.senderosargentina.R
import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.TipoPunto
import java.util.Locale

private const val TAG = "NOTIFICACION_RECORRIDO"
private const val ID_CANAL_EN_CURSO = "recorrido_en_curso"
private const val NOMBRE_CANAL_EN_CURSO = "Recorrido en curso"
private const val ID_CANAL_AVISOS = "recorrido"
private const val NOMBRE_CANAL_AVISOS = "Recorrido"
private const val ID_NOTIFICACION_EN_CURSO = 2000
private const val ID_NOTIFICACION_DESVIO = 2001
private const val ID_NOTIFICACION_LLEGADA = 2002
private const val BASE_ID_NOTIFICACION_PUNTO = 2100
private const val CODIGO_PEDIDO_ABRIR = 20
private const val CODIGO_PEDIDO_FINALIZAR = 21

// Extra de la intención que lleva el identificador del sendero en recorrido hacia MainActivity
const val EXTRA_ID_SENDERO_RECORRIDO = "idSenderoRecorrido"

// Construir y mostrar las notificaciones del recorrido: la persistente del servicio y los avisos
class NotificadorRecorrido(private val contexto: Context) {

    private val administrador: NotificationManager =
        contexto.getSystemService(NotificationManager::class.java)

    // Construir la notificación persistente del servicio según el estado actual del recorrido
    fun construirNotificacionEnCurso(idSendero: String, estado: EstadoSeguimiento?, esSimulado: Boolean): Notification {
        crearCanales()
        val resumen = if (estado == null) {
            "Preparando el recorrido"
        } else {
            "${formatearKilometros(estado.distanciaRestanteKilometros)} km restantes · " +
                "${formatearKilometros(estado.distanciaRecorridaKilometros)} km recorridos"
        }
        return NotificationCompat.Builder(contexto, ID_CANAL_EN_CURSO)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(if (esSimulado) "SIMULACIÓN · Recorrido en curso" else "Recorrido en curso")
            .setContentText(resumen)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setContentIntent(crearAccionAbrirRecorrido(idSendero))
            .addAction(0, "Finalizar", crearAccionFinalizar())
            .build()
    }

    // Actualizar la notificación persistente ya publicada por el servicio
    fun actualizarNotificacionEnCurso(idSendero: String, estado: EstadoSeguimiento) {
        administrador.notify(ID_NOTIFICACION_EN_CURSO, construirNotificacionEnCurso(idSendero, estado, estado.esSimulado))
    }

    // Obtener el identificador de la notificación persistente para iniciar el servicio en primer plano
    fun identificadorNotificacionEnCurso(): Int = ID_NOTIFICACION_EN_CURSO

    // Mostrar un aviso de proximidad o de desvío por el canal de importancia alta
    fun notificarAviso(idSendero: String, aviso: AvisoRecorrido, nombreSendero: String) {
        if (!tienePermisoDeNotificaciones()) {
            Log.d(TAG, "Aviso omitido: falta el permiso POST_NOTIFICATIONS")
            return
        }
        crearCanales()
        val (identificador, titulo, cuerpo) = when (aviso) {
            is AvisoRecorrido.CercaDePunto -> Triple(
                BASE_ID_NOTIFICACION_PUNTO + aviso.indice,
                "Cerca de ${aviso.punto.nombre.ifBlank { describirTipo(aviso.punto.tipo) }}",
                "Estás a menos de 50 metros"
            )
            AvisoRecorrido.Desvio -> Triple(
                ID_NOTIFICACION_DESVIO,
                "Te alejaste del sendero",
                "Estás a más de 100 metros del trazado"
            )
            AvisoRecorrido.LlegadaAlFinal -> Triple(
                ID_NOTIFICACION_LLEGADA,
                "¡Llegaste al final!",
                "Terminaste ${nombreSendero.ifBlank { "el sendero" }}. Tocá Finalizar para guardar el recorrido"
            )
        }
        val constructor = NotificationCompat.Builder(contexto, ID_CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setContentIntent(crearAccionAbrirRecorrido(idSendero))
            .setAutoCancel(true)
        if (aviso == AvisoRecorrido.LlegadaAlFinal) {
            constructor.addAction(0, "Finalizar", crearAccionFinalizar())
        }
        val notificacion = constructor.build()
        try {
            administrador.notify(identificador, notificacion)
            Log.d(TAG, "Aviso mostrado: $titulo")
        } catch (error: SecurityException) {
            Log.e(TAG, "El sistema rechazó el aviso del recorrido", error)
        }
    }

    // Cancelar los avisos que el recorrido dejó visibles al finalizar
    fun cancelarAvisos() {
        administrador.cancel(ID_NOTIFICACION_DESVIO)
        administrador.cancel(ID_NOTIFICACION_LLEGADA)
        administrador.activeNotifications
            .filter { notificacion -> notificacion.id >= BASE_ID_NOTIFICACION_PUNTO }
            .forEach { notificacion -> administrador.cancel(notificacion.id) }
    }

    // Describir en español el tipo de un punto sin nombre
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

    // Dar formato de un decimal con coma a una distancia en kilómetros
    private fun formatearKilometros(kilometros: Double): String {
        return String.format(Locale.forLanguageTag("es-AR"), "%.1f", kilometros)
    }

    // Verificar si la aplicación puede publicar avisos (requiere permiso desde API 33)
    private fun tienePermisoDeNotificaciones(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Crear los canales: uno silencioso para la persistente y otro de importancia alta para los avisos
    private fun crearCanales() {
        administrador.createNotificationChannel(
            NotificationChannel(ID_CANAL_EN_CURSO, NOMBRE_CANAL_EN_CURSO, NotificationManager.IMPORTANCE_LOW)
        )
        administrador.createNotificationChannel(
            NotificationChannel(ID_CANAL_AVISOS, NOMBRE_CANAL_AVISOS, NotificationManager.IMPORTANCE_HIGH)
        )
    }

    // Crear la intención pendiente que abre la aplicación en el recorrido en curso
    private fun crearAccionAbrirRecorrido(idSendero: String): PendingIntent {
        val intencion = Intent(contexto, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_ID_SENDERO_RECORRIDO, idSendero)
        return PendingIntent.getActivity(
            contexto,
            CODIGO_PEDIDO_ABRIR,
            intencion,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // Crear la intención pendiente que pide al servicio finalizar el recorrido
    private fun crearAccionFinalizar(): PendingIntent {
        return PendingIntent.getService(
            contexto,
            CODIGO_PEDIDO_FINALIZAR,
            ServicioSeguimientoRecorrido.crearIntencionDetener(contexto),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
