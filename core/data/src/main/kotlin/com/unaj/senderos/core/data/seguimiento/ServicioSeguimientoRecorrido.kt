package com.unaj.senderos.data.seguimiento

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.senderosargentina.SenderosAplicacion
import com.unaj.senderos.domain.repository.ControlSeguimiento
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "SERVICIO_RECORRIDO"
private const val ACCION_INICIAR = "com.example.senderosargentina.seguimiento.INICIAR"
private const val ACCION_DETENER = "com.example.senderosargentina.seguimiento.DETENER"
private const val EXTRA_ID_SENDERO = "idSendero"
private const val EXTRA_ES_SIMULADO = "esSimulado"

// Mantener el seguimiento del recorrido en primer plano con notificación persistente y avisos
class ServicioSeguimientoRecorrido : Service() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var control: ControlSeguimiento
    private lateinit var notificador: NotificadorRecorrido
    private var idSenderoActual: String = ""
    private var esSimuladoActual = false
    private var trabajoInicio: Job? = null
    private var seVioEstadoActivo = false

    companion object {

        // Crear la intención que inicia el seguimiento del sendero indicado
        fun crearIntencionIniciar(contexto: Context, idSendero: String, esSimulado: Boolean): Intent {
            return Intent(contexto, ServicioSeguimientoRecorrido::class.java)
                .setAction(ACCION_INICIAR)
                .putExtra(EXTRA_ID_SENDERO, idSendero)
                .putExtra(EXTRA_ES_SIMULADO, esSimulado)
        }

        // Crear la intención que detiene el seguimiento
        fun crearIntencionDetener(contexto: Context): Intent {
            return Intent(contexto, ServicioSeguimientoRecorrido::class.java).setAction(ACCION_DETENER)
        }
    }

    // Obtener las dependencias de proceso y observar el estado y los avisos del control
    override fun onCreate() {
        super.onCreate()
        val contenedor = (applicationContext as SenderosAplicacion).contenedorDependencias
        control = contenedor.controlSeguimiento
        notificador = NotificadorRecorrido(applicationContext)
        alcance.launch {
            control.estado.collect { estado ->
                if (estado != null) {
                    seVioEstadoActivo = true
                    notificador.actualizarNotificacionEnCurso(estado.idSendero, estado)
                } else if (seVioEstadoActivo) {
                    Log.d(TAG, "El seguimiento terminó; se detiene el servicio")
                    detenerServicio()
                }
            }
        }
        alcance.launch {
            control.avisos.collect { aviso -> notificador.notificarAviso(idSenderoActual, aviso) }
        }
        Log.d(TAG, "Servicio creado")
    }

    // Atender las acciones de iniciar y detener recibidas por intención
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACCION_DETENER) {
            control.detener()
            detenerServicio()
            return START_NOT_STICKY
        }
        val idSendero = intent?.getStringExtra(EXTRA_ID_SENDERO)
        val esSimulado = intent?.getBooleanExtra(EXTRA_ES_SIMULADO, false) ?: false
        esSimuladoActual = esSimulado
        if (!pasarAPrimerPlano(idSendero.orEmpty())) return START_NOT_STICKY
        if (idSendero.isNullOrBlank()) {
            Log.e(TAG, "Se inició el servicio sin identificador de sendero")
            detenerServicio()
            return START_NOT_STICKY
        }
        idSenderoActual = idSendero
        trabajoInicio?.cancel()
        trabajoInicio = alcance.launch {
            try {
                control.iniciar(idSendero, esSimulado)
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Log.e(TAG, "No se pudo iniciar el seguimiento", error)
                detenerServicio()
            }
        }
        return START_NOT_STICKY
    }

    // Detener el control y liberar el alcance al destruirse el servicio
    override fun onDestroy() {
        control.detener()
        notificador.cancelarAvisos()
        alcance.cancel()
        Log.d(TAG, "Servicio destruido")
        super.onDestroy()
    }

    // Rechazar el enlace: este servicio solo se usa mediante inicio y detención
    override fun onBind(intent: Intent?): IBinder? = null

    // Pasar a primer plano con el tipo de ubicación; devolver falso si el sistema lo rechaza
    private fun pasarAPrimerPlano(idSendero: String): Boolean {
        val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        return try {
            ServiceCompat.startForeground(
                this,
                notificador.identificadorNotificacionEnCurso(),
                notificador.construirNotificacionEnCurso(idSendero, control.estado.value, esSimuladoActual),
                tipo
            )
            true
        } catch (error: SecurityException) {
            Log.e(TAG, "El sistema rechazó el servicio en primer plano (¿falta el permiso de ubicación?)", error)
            detenerServicio()
            false
        } catch (error: IllegalStateException) {
            Log.e(TAG, "No se pudo pasar a primer plano", error)
            detenerServicio()
            false
        }
    }

    // Quitar la notificación persistente y detener el servicio
    private fun detenerServicio() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
