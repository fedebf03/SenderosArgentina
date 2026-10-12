package com.example.senderosargentina

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.unaj.senderos.data.local.AlmacenFotosLocal
import com.unaj.senderos.data.local.BaseDatosSenderos
import com.unaj.senderos.data.local.EjecutorTransaccionesRoom
import com.unaj.senderos.data.remote.FirestoreRecorridoFuenteRemota
import com.unaj.senderos.data.remote.FirestoreSenderoFuenteRemota
import com.unaj.senderos.data.remote.SubidorFotosCloudinary
import com.unaj.senderos.data.repository.AutenticacionRepositorioImpl
import com.unaj.senderos.data.repository.RecorridoRepositorioOfflineFirst
import com.unaj.senderos.data.repository.SenderoRepositorioOfflineFirst
import com.unaj.senderos.data.sincronizacion.PlanificadorSincronizacionWorkManager
import com.unaj.senderos.data.sincronizacion.SincronizadorRecorridos
import com.unaj.senderos.data.sincronizacion.SincronizadorSenderos
import com.unaj.senderos.data.mapas.GestorMapasSinConexionMapLibre
import com.unaj.senderos.data.notificaciones.NotificadorAndroid
import com.unaj.senderos.data.conectividad.MonitorConectividadAndroid
import com.unaj.senderos.domain.repository.GestorMapasSinConexion
import com.unaj.senderos.domain.repository.MonitorConectividad
import com.unaj.senderos.domain.usecase.DescargarMapaSinConexionUseCase
import com.unaj.senderos.domain.usecase.EliminarMapaSinConexionUseCase
import com.unaj.senderos.domain.usecase.ObservarMapaSinConexionUseCase
import com.unaj.senderos.data.preferencias.PreferenciasAplicacionLocal
import com.unaj.senderos.data.seguimiento.ControlSeguimientoEnMemoria
import com.unaj.senderos.data.seguimiento.LanzadorSeguimientoAndroid
import com.unaj.senderos.data.ubicacion.ProveedorUbicacionCombinada
import com.unaj.senderos.domain.repository.ControlSeguimiento
import com.unaj.senderos.domain.repository.LanzadorSeguimiento
import com.unaj.senderos.domain.repository.Notificador
import com.unaj.senderos.domain.repository.PreferenciasAplicacion
import com.unaj.senderos.domain.repository.RecorridoRepositorio
import com.unaj.senderos.domain.repository.ProveedorUbicacion
import com.unaj.senderos.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionConGoogleUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarRecorridoUseCase
import com.unaj.senderos.domain.usecase.ObservarRecorridosUseCase
import com.unaj.senderos.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.domain.usecase.RegistrarUsuarioUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ContenedorDependencias(contexto: Context) {
    private val autenticacionRepositorio = AutenticacionRepositorioImpl()
    private val baseDatosSenderos = Room.databaseBuilder(
        contexto,
        BaseDatosSenderos::class.java,
        "senderos.db"
    ).build()
    private val fuenteRemota = FirestoreSenderoFuenteRemota(FirebaseFirestore.getInstance())
    private val ejecutorTransacciones = EjecutorTransaccionesRoom(baseDatosSenderos)
    private val planificadorSincronizacion =
        PlanificadorSincronizacionWorkManager(WorkManager.getInstance(contexto))
    private val senderoRepositorio =
        SenderoRepositorioOfflineFirst(
            baseDatosSenderos.senderoDao(),
            planificadorSincronizacion,
            ejecutorTransacciones
        )

    private val subidorFotos = SubidorFotosCloudinary(
        nombreNube = BuildConfig.CLOUDINARY_NOMBRE_NUBE,
        presetSubida = BuildConfig.CLOUDINARY_PRESET_SUBIDA
    )

    val sincronizadorSenderos =
        SincronizadorSenderos(baseDatosSenderos.senderoDao(), fuenteRemota, ejecutorTransacciones, subidorFotos)
    val senderoDao = baseDatosSenderos.senderoDao()
    val recorridoDao = baseDatosSenderos.recorridoDao()
    private val recorridoRepositorio: RecorridoRepositorio =
        RecorridoRepositorioOfflineFirst(recorridoDao, planificadorSincronizacion, autenticacionRepositorio)
    val sincronizadorRecorridos = SincronizadorRecorridos(
        recorridoDao,
        FirestoreRecorridoFuenteRemota(FirebaseFirestore.getInstance()),
        ejecutorTransacciones
    )
    val almacenFotos = AlmacenFotosLocal(contexto)

    val proveedorUbicacion: ProveedorUbicacion = ProveedorUbicacionCombinada(contexto.applicationContext)
    val notificador: Notificador = NotificadorAndroid(contexto.applicationContext)
    val preferenciasAplicacion: PreferenciasAplicacion = PreferenciasAplicacionLocal(contexto.applicationContext)
    val lanzadorSeguimiento: LanzadorSeguimiento = LanzadorSeguimientoAndroid(contexto.applicationContext)

    val iniciarSesionUseCase = IniciarSesionUseCase(autenticacionRepositorio)
    val iniciarSesionConGoogleUseCase = IniciarSesionConGoogleUseCase(autenticacionRepositorio)
    val registrarUsuarioUseCase = RegistrarUsuarioUseCase(autenticacionRepositorio)
    val obtenerUsuarioActualUseCase = ObtenerUsuarioActualUseCase(autenticacionRepositorio)
    val cerrarSesionUseCase = CerrarSesionUseCase(autenticacionRepositorio, senderoRepositorio, lanzadorSeguimiento)
    val observarSenderosDelUsuarioUseCase =
        ObservarSenderosDelUsuarioUseCase(senderoRepositorio, autenticacionRepositorio)
    val observarDetalleSenderoUseCase =
        ObservarDetalleSenderoUseCase(senderoRepositorio, autenticacionRepositorio)
    val alternarFavoritoUseCase = AlternarFavoritoUseCase(senderoRepositorio, autenticacionRepositorio)
    val eliminarSenderoUseCase = EliminarSenderoUseCase(senderoRepositorio, autenticacionRepositorio)
    val guardarSenderoUseCase = GuardarSenderoUseCase(senderoRepositorio, autenticacionRepositorio)
    val observarRecorridosUseCase = ObservarRecorridosUseCase(recorridoRepositorio, autenticacionRepositorio)
    val observarRecorridoUseCase = ObservarRecorridoUseCase(recorridoRepositorio, autenticacionRepositorio)

    val monitorConectividad: MonitorConectividad = MonitorConectividadAndroid(contexto.applicationContext)
    private val gestorMapasSinConexion: GestorMapasSinConexion =
        GestorMapasSinConexionMapLibre(contexto.applicationContext)
    val observarMapaSinConexionUseCase = ObservarMapaSinConexionUseCase(gestorMapasSinConexion, autenticacionRepositorio)
    val descargarMapaSinConexionUseCase = DescargarMapaSinConexionUseCase(gestorMapasSinConexion, autenticacionRepositorio)
    val eliminarMapaSinConexionUseCase = EliminarMapaSinConexionUseCase(gestorMapasSinConexion, autenticacionRepositorio)

    // Ámbito de proceso del seguimiento: vive tanto como la aplicación; el hilo principal serializa las actualizaciones del estado
    private val alcanceSeguimiento = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val controlSeguimiento: ControlSeguimiento =
        ControlSeguimientoEnMemoria(
            proveedorUbicacion,
            observarDetalleSenderoUseCase,
            recorridoRepositorio,
            alcanceSeguimiento
        )
}
