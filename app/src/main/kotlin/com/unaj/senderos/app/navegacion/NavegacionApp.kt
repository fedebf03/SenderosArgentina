package com.unaj.senderos.app.navegacion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.unaj.senderos.R
import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
import com.unaj.senderos.core.ui.componentes.SeccionPrincipal
import com.unaj.senderos.features.auth.login.LoginPantalla
import com.unaj.senderos.features.auth.login.LoginViewModel
import com.unaj.senderos.features.auth.registro.RegistroPantalla
import com.unaj.senderos.features.auth.registro.RegistroViewModel
import com.unaj.senderos.features.mapa.MapaSenderosPantalla
import com.unaj.senderos.features.mapa.MapaSenderosViewModel
import com.unaj.senderos.features.perfil.PerfilPantalla
import com.unaj.senderos.features.perfil.PerfilViewModel
import com.unaj.senderos.features.perfil.editar.EditarPerfilPantalla
import com.unaj.senderos.features.perfil.editar.EditarPerfilViewModel
import com.unaj.senderos.features.perfil.historial.HistorialDetallePantalla
import com.unaj.senderos.features.perfil.historial.HistorialDetalleViewModel
import com.unaj.senderos.features.recorrido.RecorridoPantalla
import com.unaj.senderos.features.recorrido.RecorridoViewModel
import com.unaj.senderos.features.senderos.detalle.DetalleSenderoPantalla
import com.unaj.senderos.features.senderos.detalle.DetalleSenderoViewModel
import com.unaj.senderos.features.senderos.detallepublico.DetallePublicoPantalla
import com.unaj.senderos.features.senderos.detallepublico.DetallePublicoViewModel
import com.unaj.senderos.features.senderos.formulario.FormularioSenderoPantalla
import com.unaj.senderos.features.senderos.formulario.FormularioSenderoViewModel
import com.unaj.senderos.features.senderos.listado.ModoListado
import com.unaj.senderos.features.senderos.listado.SenderosPantalla
import com.unaj.senderos.features.senderos.listado.SenderosViewModel

@Composable
fun NavegacionApp(
    navController: NavHostController,
    destinoInicial: DestinoPantalla,
    loginViewModel: LoginViewModel,
    registroViewModel: RegistroViewModel,
    solicitarCancelacionRecorrido: Boolean,
    alAtenderSolicitudCancelacion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navegarASeccion: (SeccionPrincipal) -> Unit = { seccion ->
        navController.irASeccion(
            when (seccion) {
                SeccionPrincipal.EXPLORAR -> DestinoPantalla.Explorar
                SeccionPrincipal.MAPA -> DestinoPantalla.Mapa
                SeccionPrincipal.MIS_SENDEROS -> DestinoPantalla.MisSenderos
                SeccionPrincipal.PERFIL -> DestinoPantalla.Perfil
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = destinoInicial,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        composable<DestinoPantalla.Login> {
            LoginPantalla(
                viewModel = loginViewModel,
                idClienteWebGoogle = stringResource(R.string.default_web_client_id),
                alNavegarAInicio = { navController.reiniciarEn(DestinoPantalla.Explorar) },
                alNavegarARegistro = { navController.navigate(DestinoPantalla.Registro) { launchSingleTop = true } }
            )
        }
        composable<DestinoPantalla.Registro> {
            RegistroPantalla(
                viewModel = registroViewModel,
                alNavegarAInicio = { navController.reiniciarEn(DestinoPantalla.Explorar) },
                alVolverALogin = { navController.reemplazarPor(DestinoPantalla.Login) }
            )
        }
        composable<DestinoPantalla.Explorar> {
            SenderosPantalla(
                viewModel = hiltViewModel<SenderosViewModel, SenderosViewModel.Fabrica>(
                    creationCallback = { fabrica -> fabrica.crear(ModoListado.EXPLORAR) }
                ),
                alNavegarADetalle = { idSendero -> navController.navigate(DestinoPantalla.Detalle(idSendero)) },
                alNavegarADetallePublico = { idSendero -> navController.navigate(DestinoPantalla.DetallePublico(idSendero)) },
                alNavegarACaminata = { idCaminata -> navController.navigate(DestinoPantalla.Recorrido(idCaminata)) },
                alNavegarAHistorialDetalle = { idRecorrido -> navController.navigate(DestinoPantalla.HistorialDetalle(idRecorrido)) },
                alNavegarASeccion = navegarASeccion
            )
        }
        composable<DestinoPantalla.Mapa> {
            MapaSenderosPantalla(
                viewModel = hiltViewModel<MapaSenderosViewModel>(),
                alNavegarADetalle = { idSendero -> navController.navigate(DestinoPantalla.Detalle(idSendero)) },
                alNavegarADetallePublico = { idSendero -> navController.navigate(DestinoPantalla.DetallePublico(idSendero)) },
                alNavegarASeccion = navegarASeccion
            )
        }
        composable<DestinoPantalla.MisSenderos> {
            SenderosPantalla(
                viewModel = hiltViewModel<SenderosViewModel, SenderosViewModel.Fabrica>(
                    creationCallback = { fabrica -> fabrica.crear(ModoListado.MIS_SENDEROS) }
                ),
                alNavegarADetalle = { idSendero -> navController.navigate(DestinoPantalla.Detalle(idSendero)) },
                alNavegarACreacion = { navController.navigate(DestinoPantalla.Formulario(null)) },
                alNavegarASeccion = navegarASeccion
            )
        }
        composable<DestinoPantalla.Perfil> {
            PerfilPantalla(
                viewModel = hiltViewModel<PerfilViewModel>(),
                alNavegarALogin = {
                    // Al cerrar sesión no queda nada del usuario anterior, ni siquiera las secciones guardadas
                    navController.descartarSeccionesGuardadas()
                    navController.reiniciarEn(DestinoPantalla.Login)
                },
                alNavegarASeccion = navegarASeccion,
                alNavegarAHistorialDetalle = { idRecorrido -> navController.navigate(DestinoPantalla.HistorialDetalle(idRecorrido)) },
                alNavegarAEditarPerfil = { navController.navigate(DestinoPantalla.EditarPerfil) }
            )
        }
        composable<DestinoPantalla.EditarPerfil> {
            // Se rearman las pantallas con el nombre o la foto, para que muestren el perfil nuevo. El mapa no los muestra
            val salirAlPerfil = {
                navController.clearBackStack(DestinoPantalla.MisSenderos)
                navController.reiniciarEn(DestinoPantalla.Explorar)
                navController.irASeccion(DestinoPantalla.Perfil)
            }
            EditarPerfilPantalla(
                viewModel = hiltViewModel<EditarPerfilViewModel>(),
                alVolver = salirAlPerfil,
                alGuardar = salirAlPerfil
            )
        }
        composable<DestinoPantalla.HistorialDetalle> { entrada ->
            val destino = entrada.toRoute<DestinoPantalla.HistorialDetalle>()
            HistorialDetallePantalla(
                viewModel = hiltViewModel<HistorialDetalleViewModel, HistorialDetalleViewModel.Fabrica>(
                    creationCallback = { fabrica -> fabrica.crear(destino.idRecorrido) }
                ),
                alVolverAlPerfil = { navController.irASeccion(DestinoPantalla.Perfil) },
                alNavegarAFormularioDesdeCaminata = { idCaminata ->
                    navController.irASeccion(DestinoPantalla.Perfil)
                    navController.navigate(DestinoPantalla.Formulario(null, idCaminata))
                }
            )
        }
        composable<DestinoPantalla.Detalle> { entrada ->
            val destino = entrada.toRoute<DestinoPantalla.Detalle>()
            DetalleSenderoPantalla(
                viewModel = hiltViewModel<DetalleSenderoViewModel, DetalleSenderoViewModel.Fabrica>(
                    creationCallback = { fabrica -> fabrica.crear(destino.idSendero) }
                ),
                alVolverAlListado = { navController.volverASeccionActual() },
                alNavegarAEdicion = { idSendero -> navController.navigate(DestinoPantalla.Formulario(idSendero)) },
                alNavegarARecorrido = { idSendero -> navController.navigate(DestinoPantalla.Recorrido(idSendero)) }
            )
        }
        composable<DestinoPantalla.DetallePublico> { entrada ->
            val destino = entrada.toRoute<DestinoPantalla.DetallePublico>()
            DetallePublicoPantalla(
                viewModel = hiltViewModel<DetallePublicoViewModel, DetallePublicoViewModel.Fabrica>(
                    creationCallback = { fabrica -> fabrica.crear(destino.idSendero) }
                ),
                alVolverAExplorar = { navController.irASeccion(DestinoPantalla.Explorar) },
                // La copia es un sendero propio, así que al volver de su detalle se llega a Mis senderos
                alNavegarAMiSendero = { idCopia ->
                    navController.irASeccion(DestinoPantalla.MisSenderos)
                    navController.navigate(DestinoPantalla.Detalle(idCopia))
                }
            )
        }
        composable<DestinoPantalla.Recorrido> { entrada ->
            val destino = entrada.toRoute<DestinoPantalla.Recorrido>()
            RecorridoPantalla(
                solicitarCancelacion = solicitarCancelacionRecorrido,
                alAtenderSolicitudCancelacion = alAtenderSolicitudCancelacion,
                viewModel = hiltViewModel<RecorridoViewModel, RecorridoViewModel.Fabrica>(
                    creationCallback = { fabrica -> fabrica.crear(destino.idSendero) }
                ),
                // Volver no detiene el seguimiento, que sigue en segundo plano con su notificación
                alVolverAlDetalle = {
                    // Una caminata libre no tiene detalle, por eso se vuelve a la sección de origen
                    if (IdentificadorCaminataLibre.esDeCaminataLibre(destino.idSendero)) {
                        navController.volverASeccionActual()
                    } else {
                        navController.reemplazarPor(DestinoPantalla.Detalle(destino.idSendero))
                    }
                },
                alNavegarAFormularioDesdeCaminata = { idCaminata ->
                    navController.reemplazarPor(DestinoPantalla.Formulario(null, idCaminata))
                }
            )
        }
        composable<DestinoPantalla.Formulario> { entrada ->
            val destino = entrada.toRoute<DestinoPantalla.Formulario>()
            val idEditado = destino.idSendero
            val idCaminata = destino.idCaminata
            // Al editar o al armar desde una caminata se termina en el detalle del sendero. Si no, en la sección de origen
            val salir = { idSendero: String? ->
                if ((idEditado != null || idCaminata != null) && idSendero != null) {
                    navController.reemplazarPor(DestinoPantalla.Detalle(idSendero))
                } else {
                    navController.volverASeccionActual()
                }
            }
            FormularioSenderoPantalla(
                viewModel = hiltViewModel<FormularioSenderoViewModel, FormularioSenderoViewModel.Fabrica>(
                    creationCallback = { fabrica -> fabrica.crear(idEditado, idCaminata) }
                ),
                alVolver = { salir(idEditado) },
                alGuardar = { idGuardado -> salir(idGuardado) },
                alEditarSenderoExistente = { idExistente ->
                    navController.reemplazarPor(DestinoPantalla.Formulario(idExistente, null))
                }
            )
        }
    }
}

// Cambia de sección desde la botonera inferior. popUpTo con saveState y restoreState hace que cada sección
// conserve su estado y sus ViewModels al ir y volver entre ellas
internal fun NavController.irASeccion(seccion: DestinoPantalla) {
    // Las pantallas abiertas encima se descartan antes, para que no queden guardadas junto con la sección
    volverASeccionActual()
    navigate(seccion) {
        popUpTo<DestinoPantalla.Explorar> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

internal fun NavController.volverASeccionActual() {
    while (currentDestination?.esSeccionPrincipal() == false && previousBackStackEntry != null) {
        popBackStack()
    }
}

// Reemplaza la pantalla actual por el destino. Si el destino ya estaba justo debajo, vuelve a él y conserva su estado
internal inline fun <reified T : DestinoPantalla> NavController.reemplazarPor(destino: T) {
    popBackStack()
    val anterior = currentBackStackEntry
    val yaEstabaDebajo = anterior != null && anterior.destination.hasRoute<T>() && anterior.toRoute<T>() == destino
    if (!yaEstabaDebajo) navigate(destino)
}

internal fun NavController.reiniciarEn(destino: DestinoPantalla) {
    navigate(destino) {
        popUpTo(graph.id) { inclusive = true }
    }
}

// Las secciones que se dejaron con saveState no están en el back stack, así que reiniciarEn no las alcanza
internal fun NavController.descartarSeccionesGuardadas() {
    listOf(DestinoPantalla.Mapa, DestinoPantalla.MisSenderos, DestinoPantalla.Perfil).forEach { seccion ->
        clearBackStack(seccion)
    }
}
