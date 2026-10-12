package com.example.senderosargentina

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.unaj.senderos.data.local.AlmacenFotosLocal
import com.unaj.senderos.data.local.BaseDatosSenderos
import com.unaj.senderos.data.local.EjecutorTransaccionesRoom
import com.unaj.senderos.data.remote.FirestoreSenderoFuenteRemota
import com.unaj.senderos.data.remote.SubidorFotosCloudinary
import com.unaj.senderos.data.repository.AutenticacionRepositorioImpl
import com.unaj.senderos.data.repository.SenderoRepositorioOfflineFirst
import com.unaj.senderos.data.sincronizacion.PlanificadorSincronizacionWorkManager
import com.unaj.senderos.data.sincronizacion.SincronizadorSenderos
import com.unaj.senderos.data.notificaciones.NotificadorAndroid
import com.unaj.senderos.data.ubicacion.ProveedorUbicacionCombinada
import com.unaj.senderos.domain.repository.Notificador
import com.unaj.senderos.domain.repository.ProveedorUbicacion
import com.unaj.senderos.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.domain.usecase.GuardarSenderoUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionConGoogleUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.domain.usecase.RegistrarUsuarioUseCase

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
    val almacenFotos = AlmacenFotosLocal(contexto)

    val proveedorUbicacion: ProveedorUbicacion = ProveedorUbicacionCombinada(contexto.applicationContext)
    val notificador: Notificador = NotificadorAndroid(contexto.applicationContext)

    val iniciarSesionUseCase = IniciarSesionUseCase(autenticacionRepositorio)
    val iniciarSesionConGoogleUseCase = IniciarSesionConGoogleUseCase(autenticacionRepositorio)
    val registrarUsuarioUseCase = RegistrarUsuarioUseCase(autenticacionRepositorio)
    val obtenerUsuarioActualUseCase = ObtenerUsuarioActualUseCase(autenticacionRepositorio)
    val cerrarSesionUseCase = CerrarSesionUseCase(autenticacionRepositorio, senderoRepositorio)
    val observarSenderosDelUsuarioUseCase =
        ObservarSenderosDelUsuarioUseCase(senderoRepositorio, autenticacionRepositorio)
    val observarDetalleSenderoUseCase =
        ObservarDetalleSenderoUseCase(senderoRepositorio, autenticacionRepositorio)
    val alternarFavoritoUseCase = AlternarFavoritoUseCase(senderoRepositorio, autenticacionRepositorio)
    val eliminarSenderoUseCase = EliminarSenderoUseCase(senderoRepositorio, autenticacionRepositorio)
    val guardarSenderoUseCase = GuardarSenderoUseCase(senderoRepositorio, autenticacionRepositorio)
}
