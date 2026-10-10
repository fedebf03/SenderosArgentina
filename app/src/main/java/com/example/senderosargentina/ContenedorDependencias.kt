package com.example.senderosargentina

import com.unaj.senderos.data.repository.AutenticacionRepositorioImpl
import com.unaj.senderos.data.repository.SenderoRepositorioMock
import com.unaj.senderos.domain.usecase.AlternarFavoritoUseCase
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.EliminarSenderoUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionConGoogleUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionUseCase
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.domain.usecase.ObservarSenderosDelUsuarioUseCase
import com.unaj.senderos.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.domain.usecase.RegistrarUsuarioUseCase

// Construir una sola vez los repositorios y casos de uso que comparte toda la aplicación
class ContenedorDependencias {
    private val autenticacionRepositorio = AutenticacionRepositorioImpl()
    private val senderoRepositorio = SenderoRepositorioMock()

    val iniciarSesionUseCase = IniciarSesionUseCase(autenticacionRepositorio)
    val iniciarSesionConGoogleUseCase = IniciarSesionConGoogleUseCase(autenticacionRepositorio)
    val registrarUsuarioUseCase = RegistrarUsuarioUseCase(autenticacionRepositorio)
    val obtenerUsuarioActualUseCase = ObtenerUsuarioActualUseCase(autenticacionRepositorio)
    val cerrarSesionUseCase = CerrarSesionUseCase(autenticacionRepositorio)
    val observarSenderosDelUsuarioUseCase =
        ObservarSenderosDelUsuarioUseCase(senderoRepositorio, autenticacionRepositorio)
    val observarDetalleSenderoUseCase =
        ObservarDetalleSenderoUseCase(senderoRepositorio, autenticacionRepositorio)
    val alternarFavoritoUseCase = AlternarFavoritoUseCase(senderoRepositorio, autenticacionRepositorio)
    val eliminarSenderoUseCase = EliminarSenderoUseCase(senderoRepositorio, autenticacionRepositorio)
}
