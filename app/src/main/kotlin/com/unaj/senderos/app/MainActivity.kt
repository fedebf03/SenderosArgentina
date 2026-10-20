package com.unaj.senderos.app

import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Box
import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import com.unaj.senderos.core.data.seguimiento.EXTRA_ID_SENDERO_RECORRIDO
import com.unaj.senderos.core.data.seguimiento.EXTRA_SOLICITAR_CANCELACION
import com.unaj.senderos.core.ui.componentes.SeccionPrincipal
import com.unaj.senderos.features.senderos.detalle.DetalleSenderoPantalla
import com.unaj.senderos.features.senderos.detalle.DetalleSenderoViewModel
import com.unaj.senderos.features.senderos.detallepublico.DetallePublicoPantalla
import com.unaj.senderos.features.senderos.detallepublico.DetallePublicoViewModel
import com.unaj.senderos.features.senderos.formulario.FormularioSenderoPantalla
import com.unaj.senderos.features.senderos.formulario.FormularioSenderoViewModel
import com.unaj.senderos.features.perfil.historial.HistorialDetallePantalla
import com.unaj.senderos.features.perfil.historial.HistorialDetalleViewModel
import com.unaj.senderos.features.auth.login.LoginPantalla
import com.unaj.senderos.features.auth.login.LoginViewModel
import com.unaj.senderos.features.mapa.MapaSenderosPantalla
import com.unaj.senderos.features.mapa.MapaSenderosViewModel
import com.unaj.senderos.features.perfil.PerfilPantalla
import com.unaj.senderos.features.perfil.editar.EditarPerfilPantalla
import com.unaj.senderos.features.perfil.editar.EditarPerfilViewModel
import com.unaj.senderos.features.perfil.PerfilViewModel
import com.unaj.senderos.features.recorrido.RecorridoPantalla
import com.unaj.senderos.features.recorrido.RecorridoViewModel
import com.unaj.senderos.features.auth.registro.RegistroPantalla
import com.unaj.senderos.features.auth.registro.RegistroViewModel
import com.unaj.senderos.features.senderos.listado.ModoListado
import com.unaj.senderos.features.senderos.listado.SenderosPantalla
import com.unaj.senderos.features.senderos.listado.SenderosViewModel
import com.unaj.senderos.core.ui.tema.TemaSenderos
import com.unaj.senderos.app.navegacion.AlmacenDestinosViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.unaj.senderos.R
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.unaj.senderos.core.domain.model.IdentificadorCaminataLibre
import com.unaj.senderos.app.navegacion.DestinoPantalla
import com.unaj.senderos.app.navegacion.claveDeAlmacen
import com.unaj.senderos.app.navegacion.guardadorDeDestino
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var obtenerUsuarioActualUseCase: ObtenerUsuarioActualUseCase

    // Cada destino guarda sus ViewModels aparte para poder descartarlos al salir de él
    private val almacenDestinos: AlmacenDestinosViewModel by viewModels()

    // Login y Registro viven mientras viva la Activity. Así se conserva lo escrito al ir y volver entre ambas
    private val loginViewModel: LoginViewModel by viewModels()
    private val registroViewModel: RegistroViewModel by viewModels()

    // Lo deja la notificación del recorrido en curso y la composición lo consume y lo limpia
    private var idSenderoRecorridoPendiente by mutableStateOf<String?>(null)

    private var solicitudCancelacionPendiente by mutableStateOf(false)

    // RF7 - Notificaciones: al tocar la notificación del recorrido llega el sendero para volver a su seguimiento
    private fun leerPedidoDeRecorrido(intencionRecibida: Intent?) {
        val idSendero = intencionRecibida?.getStringExtra(EXTRA_ID_SENDERO_RECORRIDO)
        if (idSendero.isNullOrBlank()) return
        val solicitaCancelacion = intencionRecibida.getBooleanExtra(EXTRA_SOLICITAR_CANCELACION, false)
        intencionRecibida.removeExtra(EXTRA_ID_SENDERO_RECORRIDO)
        intencionRecibida.removeExtra(EXTRA_SOLICITAR_CANCELACION)
        // Al reabrir la app desde el historial el extra es viejo y no debe volver a navegar
        val vieneDelHistorial = intencionRecibida.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (!vieneDelHistorial) {
            solicitudCancelacionPendiente = solicitaCancelacion
            idSenderoRecorridoPendiente = idSendero
        }
    }

    // Con launchMode singleTop y la Activity ya abierta, la notificación llega por aquí
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leerPedidoDeRecorrido(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tras recrear la Activity (giro o muerte del proceso) el destino se restaura solo
        if (savedInstanceState == null) leerPedidoDeRecorrido(intent)
        window.setBackgroundDrawableResource(android.R.color.white)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            var pantallaActual by rememberSaveable(stateSaver = guardadorDeDestino) {
                val hayUsuario = obtenerUsuarioActualUseCase() != null
                mutableStateOf(if (hayUsuario) DestinoPantalla.Explorar else DestinoPantalla.Login)
            }
            var listadoDeOrigen by rememberSaveable(stateSaver = guardadorDeDestino) {
                mutableStateOf<DestinoPantalla>(DestinoPantalla.Explorar)
            }
            val navegarASeccion: (SeccionPrincipal) -> Unit = { seccion ->
                pantallaActual = when (seccion) {
                    SeccionPrincipal.EXPLORAR -> DestinoPantalla.Explorar
                    SeccionPrincipal.MAPA -> DestinoPantalla.Mapa
                    SeccionPrincipal.MIS_SENDEROS -> DestinoPantalla.MisSenderos
                    SeccionPrincipal.PERFIL -> DestinoPantalla.Perfil
                }
            }
            val abrirDetalleDesde: (DestinoPantalla) -> (String) -> Unit = { origen ->
                { idSendero ->
                    listadoDeOrigen = origen
                    pantallaActual = DestinoPantalla.Detalle(idSendero)
                }
            }
            val idUsuario = obtenerUsuarioActualUseCase()?.id.orEmpty()
            val estadoAvisosGlobal = remember { SnackbarHostState() }
            val alcance = rememberCoroutineScope()

            // Si no hay sesión activa, el pedido de la notificación se descarta
            val idRecorridoPedido = idSenderoRecorridoPendiente
            LaunchedEffect(idRecorridoPedido) {
                if (idRecorridoPedido != null) {
                    abrirRecorridoPedido(
                        idSendero = idRecorridoPedido,
                        idUsuario = idUsuario,
                        destinoActual = pantallaActual,
                        alAvisar = { mensaje -> alcance.launch { estadoAvisosGlobal.showSnackbar(mensaje) } },
                        irARecorrido = { pantallaActual = DestinoPantalla.Recorrido(idRecorridoPedido) }
                    )
                    if (pantallaActual !is DestinoPantalla.Recorrido) solicitudCancelacionPendiente = false
                    idSenderoRecorridoPendiente = null
                }
            }

            TemaSenderos {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (val destino = pantallaActual) {
                        is DestinoPantalla.Login -> LoginPantalla(
                            viewModel = loginViewModel,
                            idClienteWebGoogle = getString(R.string.default_web_client_id),
                            alNavegarAInicio = { pantallaActual = DestinoPantalla.Explorar },
                            alNavegarARegistro = { pantallaActual = DestinoPantalla.Registro }
                        )
                        is DestinoPantalla.Registro -> RegistroPantalla(
                            viewModel = registroViewModel,
                            alNavegarAInicio = { pantallaActual = DestinoPantalla.Explorar },
                            alVolverALogin = { pantallaActual = DestinoPantalla.Login }
                        )
                        is DestinoPantalla.Explorar -> SenderosPantalla(
                            viewModel = viewModelAsistidoDeDestino<SenderosViewModel, SenderosViewModel.Fabrica>("explorar-$idUsuario") { fabrica ->
                                fabrica.crear(ModoListado.EXPLORAR)
                            },
                            alNavegarADetalle = abrirDetalleDesde(DestinoPantalla.Explorar),
                            alNavegarADetallePublico = { idSendero -> pantallaActual = DestinoPantalla.DetallePublico(idSendero) },
                            alNavegarACaminata = { idCaminata ->
                                listadoDeOrigen = DestinoPantalla.Explorar
                                pantallaActual = DestinoPantalla.Recorrido(idCaminata)
                            },
                            alNavegarAHistorialDetalle = { idRecorrido -> pantallaActual = DestinoPantalla.HistorialDetalle(idRecorrido) },
                            alNavegarASeccion = navegarASeccion
                        )
                        is DestinoPantalla.Mapa -> MapaSenderosPantalla(
                            viewModel = viewModelDeDestino<MapaSenderosViewModel>("mapa-$idUsuario"),
                            alNavegarADetalle = abrirDetalleDesde(DestinoPantalla.Mapa),
                            alNavegarADetallePublico = { idSendero -> pantallaActual = DestinoPantalla.DetallePublico(idSendero) },
                            alNavegarASeccion = navegarASeccion
                        )
                        is DestinoPantalla.MisSenderos -> SenderosPantalla(
                            viewModel = viewModelAsistidoDeDestino<SenderosViewModel, SenderosViewModel.Fabrica>("mis-senderos-$idUsuario") { fabrica ->
                                fabrica.crear(ModoListado.MIS_SENDEROS)
                            },
                            alNavegarADetalle = abrirDetalleDesde(DestinoPantalla.MisSenderos),
                            alNavegarACreacion = {
                                listadoDeOrigen = DestinoPantalla.MisSenderos
                                pantallaActual = DestinoPantalla.Formulario(null)
                            },
                            alNavegarASeccion = navegarASeccion
                        )
                        is DestinoPantalla.Perfil -> PerfilPantalla(
                            viewModel = viewModelDeDestino<PerfilViewModel>("perfil-$idUsuario"),
                            alNavegarALogin = {
                                pantallaActual = DestinoPantalla.Login
                                listOf("explorar", "mis-senderos", "mapa", "perfil").forEach { seccion ->
                                    almacenDestinos.liberarDestino("$seccion-$idUsuario")
                                }
                            },
                            alNavegarASeccion = navegarASeccion,
                            alNavegarAHistorialDetalle = { idRecorrido ->
                                pantallaActual = DestinoPantalla.HistorialDetalle(idRecorrido)
                            },
                            alNavegarAEditarPerfil = { pantallaActual = DestinoPantalla.EditarPerfil }
                        )
                        is DestinoPantalla.EditarPerfil -> {
                            val clave = "editar-perfil-$idUsuario"
                            // Se liberan las pantallas con el nombre o la foto, para que se armen de nuevo con el perfil nuevo
                            val salirAlPerfil = {
                                pantallaActual = DestinoPantalla.Perfil
                                almacenDestinos.liberarDestino(clave)
                                listOf("explorar", "mis-senderos", "perfil").forEach { seccion ->
                                    almacenDestinos.liberarDestino("$seccion-$idUsuario")
                                }
                            }
                            EditarPerfilPantalla(
                                viewModel = viewModelDeDestino<EditarPerfilViewModel>(clave),
                                alVolver = salirAlPerfil,
                                alGuardar = salirAlPerfil
                            )
                        }
                        is DestinoPantalla.HistorialDetalle -> {
                            val clave = "historial-$idUsuario-${destino.idRecorrido}"
                            HistorialDetallePantalla(
                                viewModel = viewModelAsistidoDeDestino<HistorialDetalleViewModel, HistorialDetalleViewModel.Fabrica>(clave) { fabrica ->
                                    fabrica.crear(destino.idRecorrido)
                                },
                                alVolverAlPerfil = {
                                    pantallaActual = DestinoPantalla.Perfil
                                    almacenDestinos.liberarDestino(clave)
                                },
                                alNavegarAFormularioDesdeCaminata = { idCaminata ->
                                    listadoDeOrigen = DestinoPantalla.Perfil
                                    pantallaActual = DestinoPantalla.Formulario(null, idCaminata)
                                    almacenDestinos.liberarDestino(clave)
                                }
                            )
                        }
                        is DestinoPantalla.Detalle -> {
                            val clave = "detalle-$idUsuario-${destino.idSendero}"
                            DetalleSenderoPantalla(
                                viewModel = viewModelAsistidoDeDestino<DetalleSenderoViewModel, DetalleSenderoViewModel.Fabrica>(clave) { fabrica ->
                                    fabrica.crear(destino.idSendero)
                                },
                                alVolverAlListado = {
                                    pantallaActual = listadoDeOrigen
                                    almacenDestinos.liberarDestino(clave)
                                },
                                alNavegarAEdicion = { idSendero -> pantallaActual = DestinoPantalla.Formulario(idSendero) },
                                alNavegarARecorrido = { idSendero -> pantallaActual = DestinoPantalla.Recorrido(idSendero) }
                            )
                        }
                        is DestinoPantalla.DetallePublico -> {
                            val clave = "detallepublico-$idUsuario-${destino.idSendero}"
                            DetallePublicoPantalla(
                                viewModel = viewModelAsistidoDeDestino<DetallePublicoViewModel, DetallePublicoViewModel.Fabrica>(clave) { fabrica ->
                                    fabrica.crear(destino.idSendero)
                                },
                                alVolverAExplorar = {
                                    pantallaActual = DestinoPantalla.Explorar
                                    almacenDestinos.liberarDestino(clave)
                                },
                                alNavegarAMiSendero = { idCopia ->
                                    listadoDeOrigen = DestinoPantalla.MisSenderos
                                    pantallaActual = DestinoPantalla.Detalle(idCopia)
                                    almacenDestinos.liberarDestino(clave)
                                }
                            )
                        }
                        is DestinoPantalla.Recorrido -> {
                            val clave = "recorrido-$idUsuario-${destino.idSendero}"
                            RecorridoPantalla(
                                solicitarCancelacion = solicitudCancelacionPendiente,
                                alAtenderSolicitudCancelacion = { solicitudCancelacionPendiente = false },
                                viewModel = viewModelAsistidoDeDestino<RecorridoViewModel, RecorridoViewModel.Fabrica>(clave) { fabrica ->
                                    fabrica.crear(destino.idSendero)
                                },
                                // Volver no detiene el seguimiento, que sigue en segundo plano con su notificación
                                alVolverAlDetalle = {
                                    // Una caminata libre no tiene detalle, por eso se vuelve al listado de origen
                                    pantallaActual = if (IdentificadorCaminataLibre.esDeCaminataLibre(destino.idSendero)) {
                                        listadoDeOrigen
                                    } else {
                                        DestinoPantalla.Detalle(destino.idSendero)
                                    }
                                    almacenDestinos.liberarDestino(clave)
                                },
                                alNavegarAFormularioDesdeCaminata = { idCaminata ->
                                    pantallaActual = DestinoPantalla.Formulario(null, idCaminata)
                                    almacenDestinos.liberarDestino(clave)
                                }
                            )
                        }
                        is DestinoPantalla.Formulario -> {
                            val idEditado = destino.idSendero
                            val idCaminata = destino.idCaminata
                            val clave = "formulario-$idUsuario-${idEditado ?: idCaminata ?: "nuevo"}"
                            val salir = { idSendero: String? ->
                                pantallaActual = if ((idEditado != null || idCaminata != null) && idSendero != null) {
                                    DestinoPantalla.Detalle(idSendero)
                                } else {
                                    listadoDeOrigen
                                }
                                almacenDestinos.liberarDestino(clave)
                            }
                            FormularioSenderoPantalla(
                                viewModel = viewModelAsistidoDeDestino<FormularioSenderoViewModel, FormularioSenderoViewModel.Fabrica>(clave) { fabrica ->
                                    fabrica.crear(idEditado, idCaminata)
                                },
                                alVolver = { salir(idEditado) },
                                alGuardar = { idGuardado -> salir(idGuardado) },
                                alEditarSenderoExistente = { idExistente ->
                                    pantallaActual = DestinoPantalla.Formulario(idExistente, null)
                                    almacenDestinos.liberarDestino(clave)
                                }
                            )
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        SnackbarHost(
                            hostState = estadoAvisosGlobal,
                            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }

    // Con un formulario abierto no se navega y se avisa, porque descartar cambios sin guardar perdería datos
    private fun abrirRecorridoPedido(
        idSendero: String,
        idUsuario: String,
        destinoActual: DestinoPantalla,
        alAvisar: (String) -> Unit,
        irARecorrido: () -> Unit
    ) {
        when {
            idUsuario.isEmpty() -> Unit
            destinoActual is DestinoPantalla.Formulario -> {
                alAvisar("Terminá o descartá el formulario para volver al recorrido en curso")
            }
            destinoActual is DestinoPantalla.Recorrido && destinoActual.idSendero == idSendero -> Unit
            else -> {
                claveDeAlmacen(destinoActual, idUsuario)?.let { clave -> almacenDestinos.liberarDestino(clave) }
                irARecorrido()
            }
        }
    }

    @Composable
    private inline fun <reified VM : ViewModel> viewModelDeDestino(clave: String): VM {
        return hiltViewModel(viewModelStoreOwner = almacenDestinos.obtenerPropietario(clave, this))
    }

    @Composable
    private inline fun <reified VM : ViewModel, reified F> viewModelAsistidoDeDestino(
        clave: String,
        noinline crear: (F) -> VM
    ): VM {
        return hiltViewModel<VM, F>(
            viewModelStoreOwner = almacenDestinos.obtenerPropietario(clave, this),
            creationCallback = crear
        )
    }
}
