package com.unaj.senderos.core.data.seguimiento

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.unaj.senderos.core.domain.notificaciones.RedactorNotificacionRecorrido
import com.unaj.senderos.core.domain.notificaciones.ContenidoNotificacionRecorrido
import com.unaj.senderos.core.domain.port.ControlSeguimiento
import com.unaj.senderos.core.domain.port.FuentePresion
import com.unaj.senderos.core.domain.port.Reloj
import com.unaj.senderos.core.domain.usecase.ObtenerPronosticoUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "SERVICIO_RECORRIDO"
private const val ACCION_INICIAR = "com.example.senderosargentina.seguimiento.INICIAR"
private const val ACCION_INICIAR_CAMINATA_LIBRE = "com.example.senderosargentina.seguimiento.INICIAR_CAMINATA_LIBRE"
private const val ACCION_DETENER = "com.example.senderosargentina.seguimiento.DETENER"
private const val ACCION_CANCELAR = "com.example.senderosargentina.seguimiento.CANCELAR"
private const val EXTRA_ID_SENDERO = "idSendero"
private const val EXTRA_ES_SIMULADO = "esSimulado"
private const val EXTRA_ID_USUARIO = "idUsuario"
private const val EXTRA_NOMBRE = "nombre"
private const val ETIQUETA_BLOQUEO = "senderos:seguimiento"
private const val DURACION_MAXIMA_BLOQUEO_MILISEGUNDOS = 6L * 60L * 60L * 1000L
private const val INTERVALO_NOTIFICACION_MILISEGUNDOS = 1_000L

// Se reduce el estado a lo que muestra la notificación persistente para no republicarla por cambios que no se ven
private data class DatosNotificacion(
    val idSendero: String,
    val contenido: ContenidoNotificacionRecorrido
)

@AndroidEntryPoint
class ServicioSeguimientoRecorrido : Service() {

    @Inject
    lateinit var control: ControlSeguimiento

    @Inject
    lateinit var capacidadesDispositivo: CapacidadesDispositivo

    @Inject
    lateinit var fuentePresion: FuentePresion

    @Inject
    lateinit var obtenerPronosticoUseCase: ObtenerPronosticoUseCase

    @Inject
    lateinit var reloj: Reloj

    private var vigilanteAmbiente: VigilanteAmbienteRecorrido? = null

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var notificador: NotificadorRecorrido
    private var idSenderoActual: String = ""
    private var esSimuladoActual = false
    private var trabajoInicio: Job? = null
    private var seVioEstadoActivo = false
    private var bloqueoParcial: PowerManager.WakeLock? = null
    private var tieneAcelerometro = false

    companion object {

        fun crearIntencionIniciar(contexto: Context, idSendero: String, esSimulado: Boolean): Intent {
            return Intent(contexto, ServicioSeguimientoRecorrido::class.java)
                .setAction(ACCION_INICIAR)
                .putExtra(EXTRA_ID_SENDERO, idSendero)
                .putExtra(EXTRA_ES_SIMULADO, esSimulado)
        }

        fun crearIntencionIniciarCaminataLibre(contexto: Context, idCaminata: String, idUsuario: String, nombre: String): Intent {
            return Intent(contexto, ServicioSeguimientoRecorrido::class.java)
                .setAction(ACCION_INICIAR_CAMINATA_LIBRE)
                .putExtra(EXTRA_ID_SENDERO, idCaminata)
                .putExtra(EXTRA_ID_USUARIO, idUsuario)
                .putExtra(EXTRA_NOMBRE, nombre)
        }

        fun crearIntencionDetener(contexto: Context): Intent {
            return Intent(contexto, ServicioSeguimientoRecorrido::class.java).setAction(ACCION_DETENER)
        }

        fun crearIntencionCancelar(contexto: Context): Intent {
            return Intent(contexto, ServicioSeguimientoRecorrido::class.java).setAction(ACCION_CANCELAR)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificador = NotificadorRecorrido(applicationContext)
        tieneAcelerometro = capacidadesDispositivo.tieneAcelerometro
        alcance.launch {
            control.estado.map { estado -> estado != null }.distinctUntilChanged().collect { estaActivo ->
                if (estaActivo) {
                    seVioEstadoActivo = true
                } else if (seVioEstadoActivo) {
                    Log.d(TAG, "El seguimiento terminó; se detiene el servicio")
                    detenerServicio()
                }
            }
        }
        alcance.launch {
            control.estado
                .map { estado ->
                    estado?.let { estadoActual ->
                        DatosNotificacion(
                            estadoActual.idSendero,
                            RedactorNotificacionRecorrido.redactarPersistente(estadoActual, estadoActual.esSimulado)
                        )
                    }
                }
                .distinctUntilChanged()
                .conflate()
                .collect { datos ->
                    if (datos != null) {
                        notificador.actualizarNotificacionEnCurso(datos.idSendero, datos.contenido)
                        delay(INTERVALO_NOTIFICACION_MILISEGUNDOS)
                    }
                }
        }
        alcance.launch {
            control.avisos.collect { aviso -> notificador.notificarAviso(idSenderoActual, aviso, control.estado.value) }
        }
        vigilanteAmbiente = VigilanteAmbienteRecorrido(
            alcance = alcance,
            estado = control.estado,
            fuentePresion = fuentePresion,
            obtenerPronostico = { lugar -> obtenerPronosticoUseCase(lugar) },
            reloj = reloj,
            alAvisar = { aviso -> notificador.notificarAvisoAmbiente(idSenderoActual, aviso) }
        ).also { vigilante -> vigilante.iniciar() }
        Log.d(TAG, "Servicio creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACCION_CANCELAR) {
            Log.d(TAG, "Cancelación pedida: se descarta el recorrido sin guardarlo")
            trabajoInicio?.cancel()
            control.cancelar()
            detenerServicio()
            return START_NOT_STICKY
        }
        if (intent?.action == ACCION_DETENER) {
            control.detener()
            detenerServicio()
            return START_NOT_STICKY
        }
        if (intent?.action == ACCION_INICIAR_CAMINATA_LIBRE) {
            iniciarCaminataLibre(
                intent.getStringExtra(EXTRA_ID_SENDERO).orEmpty(),
                intent.getStringExtra(EXTRA_ID_USUARIO).orEmpty(),
                intent.getStringExtra(EXTRA_NOMBRE).orEmpty()
            )
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
        if (!esSimulado && tieneAcelerometro) adquirirBloqueoParcial()
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

    private fun iniciarCaminataLibre(idCaminata: String, idUsuario: String, nombre: String) {
        esSimuladoActual = false
        if (!pasarAPrimerPlano(idCaminata)) return
        if (idCaminata.isBlank() || idUsuario.isBlank()) {
            Log.e(TAG, "Se inició una caminata libre sin identificador o sin usuario")
            detenerServicio()
            return
        }
        idSenderoActual = idCaminata
        if (tieneAcelerometro) adquirirBloqueoParcial()
        trabajoInicio?.cancel()
        trabajoInicio = alcance.launch {
            try {
                control.iniciarCaminataLibre(idCaminata, idUsuario, nombre)
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (error: Exception) {
                Log.e(TAG, "No se pudo iniciar la caminata libre", error)
                detenerServicio()
            }
        }
    }

    override fun onDestroy() {
        control.detener()
        vigilanteAmbiente?.detener()
        liberarBloqueoParcial()
        notificador.cancelarAvisos()
        alcance.cancel()
        Log.d(TAG, "Servicio destruido")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Devuelve falso si el sistema rechaza pasar a primer plano
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

    private fun detenerServicio() {
        liberarBloqueoParcial()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // El WakeLock parcial (máximo 6 h) mantiene la CPU entregando muestras del acelerómetro con la pantalla apagada
    private fun adquirirBloqueoParcial() {
        val bloqueo = bloqueoParcial ?: (getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ETIQUETA_BLOQUEO)
            ?.also { nuevo ->
                nuevo.setReferenceCounted(false)
                bloqueoParcial = nuevo
            }) ?: return
        bloqueo.acquire(DURACION_MAXIMA_BLOQUEO_MILISEGUNDOS)
        Log.d(TAG, "WakeLock parcial adquirido")
    }

    private fun liberarBloqueoParcial() {
        val bloqueo = bloqueoParcial ?: return
        if (bloqueo.isHeld) {
            bloqueo.release()
            Log.d(TAG, "WakeLock parcial liberado")
        }
    }
}
