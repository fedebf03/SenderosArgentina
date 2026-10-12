package com.example.senderosargentina

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.unaj.senderos.data.seguimiento.EXTRA_ID_SENDERO_RECORRIDO
import com.unaj.senderos.presentation.componentes.SeccionPrincipal
import com.unaj.senderos.presentation.detalle.DetalleSenderoPantalla
import com.unaj.senderos.presentation.detalle.DetalleSenderoViewModel
import com.unaj.senderos.presentation.favoritos.FavoritosPantalla
import com.unaj.senderos.presentation.favoritos.FavoritosViewModel
import com.unaj.senderos.presentation.formulario.FormularioSenderoPantalla
import com.unaj.senderos.presentation.formulario.FormularioSenderoViewModel
import com.unaj.senderos.presentation.historial.HistorialDetallePantalla
import com.unaj.senderos.presentation.historial.HistorialDetalleViewModel
import com.unaj.senderos.presentation.login.LoginPantalla
import com.unaj.senderos.presentation.login.LoginViewModel
import com.unaj.senderos.presentation.mapasenderos.MapaSenderosPantalla
import com.unaj.senderos.presentation.mapasenderos.MapaSenderosViewModel
import com.unaj.senderos.presentation.perfil.PerfilPantalla
import com.unaj.senderos.presentation.perfil.PerfilViewModel
import com.unaj.senderos.presentation.recorrido.RecorridoPantalla
import com.unaj.senderos.presentation.recorrido.RecorridoViewModel
import com.unaj.senderos.presentation.registro.RegistroPantalla
import com.unaj.senderos.presentation.registro.RegistroViewModel
import com.unaj.senderos.presentation.senderos.SenderosPantalla
import com.unaj.senderos.presentation.senderos.SenderosViewModel
import com.unaj.senderos.presentation.tema.TemaSenderos

sealed interface DestinoPantalla {
    data object Login : DestinoPantalla
    data object Registro : DestinoPantalla
    data object Inicio : DestinoPantalla
    data object Mapa : DestinoPantalla
    data object Favoritos : DestinoPantalla
    data object Perfil : DestinoPantalla
    data class Detalle(val idSendero: String) : DestinoPantalla
    data class Formulario(val idSendero: String?) : DestinoPantalla
    data class Recorrido(val idSendero: String) : DestinoPantalla
    data class HistorialDetalle(val idRecorrido: String) : DestinoPantalla
}

class MainActivity : ComponentActivity() {

    private val dependencias by lazy { (application as SenderosAplicacion).contenedorDependencias }

    // Cada destino guarda sus ViewModels aparte para poder descartarlos al salir de él
    private val almacenDestinos: AlmacenDestinosViewModel by viewModels()

    // Login y Registro viven mientras viva la Activity: así se conserva lo escrito al ir y volver entre ambas
    private val loginViewModel: LoginViewModel by viewModels {
        viewModelFactory {
            initializer { LoginViewModel(dependencias.iniciarSesionUseCase, dependencias.iniciarSesionConGoogleUseCase) }
        }
    }
    private val registroViewModel: RegistroViewModel by viewModels {
        viewModelFactory { initializer { RegistroViewModel(dependencias.registrarUsuarioUseCase) } }
    }

    // Sendero cuyo recorrido en curso pidió abrir la notificación; la composición lo consume y lo limpia
    private var idSenderoRecorridoPendiente by mutableStateOf<String?>(null)

    // Tomar el pedido de abrir el recorrido desde la intención recibida y quitarlo para no repetirlo
    private fun leerPedidoDeRecorrido(intencionRecibida: Intent?) {
        val idSendero = intencionRecibida?.getStringExtra(EXTRA_ID_SENDERO_RECORRIDO)
        if (idSendero.isNullOrBlank()) return
        intencionRecibida.removeExtra(EXTRA_ID_SENDERO_RECORRIDO)
        // Al reabrir la app desde el historial el extra es viejo: no debe volver a navegar
        val vieneDelHistorial = intencionRecibida.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (!vieneDelHistorial) idSenderoRecorridoPendiente = idSendero
    }

    // Atender la notificación del recorrido cuando la Activity ya estaba abierta (launchMode singleTop)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leerPedidoDeRecorrido(intent)
    }

    // Crear la Activity, aplicar el modo de borde a borde y componer la navegación por estado
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
                val hayUsuario = dependencias.obtenerUsuarioActualUseCase() != null
                mutableStateOf(if (hayUsuario) DestinoPantalla.Inicio else DestinoPantalla.Login)
            }
            // Para volver del detalle (o de crear un sendero) al mismo listado desde el que se entró
            var listadoDeOrigen by rememberSaveable(stateSaver = guardadorDeDestino) {
                mutableStateOf<DestinoPantalla>(DestinoPantalla.Inicio)
            }
            val navegarASeccion: (SeccionPrincipal) -> Unit = { seccion ->
                pantallaActual = when (seccion) {
                    SeccionPrincipal.INICIO -> DestinoPantalla.Inicio
                    SeccionPrincipal.MAPA -> DestinoPantalla.Mapa
                    SeccionPrincipal.FAVORITOS -> DestinoPantalla.Favoritos
                    SeccionPrincipal.PERFIL -> DestinoPantalla.Perfil
                }
            }
            val abrirDetalleDesde: (DestinoPantalla) -> (String) -> Unit = { origen ->
                { idSendero ->
                    listadoDeOrigen = origen
                    pantallaActual = DestinoPantalla.Detalle(idSendero)
                }
            }
            val idUsuario = dependencias.obtenerUsuarioActualUseCase()?.id.orEmpty()

            // Abrir el recorrido en curso al tocar la notificación; sin sesión activa se descarta el pedido
            val idRecorridoPedido = idSenderoRecorridoPendiente
            LaunchedEffect(idRecorridoPedido) {
                if (idRecorridoPedido != null) {
                    abrirRecorridoPedido(
                        idSendero = idRecorridoPedido,
                        idUsuario = idUsuario,
                        destinoActual = pantallaActual,
                        irARecorrido = { pantallaActual = DestinoPantalla.Recorrido(idRecorridoPedido) }
                    )
                    idSenderoRecorridoPendiente = null
                }
            }

            TemaSenderos {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (val destino = pantallaActual) {
                        is DestinoPantalla.Login -> LoginPantalla(
                            viewModel = loginViewModel,
                            alNavegarAInicio = { pantallaActual = DestinoPantalla.Inicio },
                            alNavegarARegistro = { pantallaActual = DestinoPantalla.Registro }
                        )
                        is DestinoPantalla.Registro -> RegistroPantalla(
                            viewModel = registroViewModel,
                            alNavegarAInicio = { pantallaActual = DestinoPantalla.Inicio },
                            alVolverALogin = { pantallaActual = DestinoPantalla.Login }
                        )
                        is DestinoPantalla.Inicio -> SenderosPantalla(
                            viewModel = viewModelDeDestino("inicio-$idUsuario") {
                                SenderosViewModel(dependencias.observarSenderosDelUsuarioUseCase, dependencias.proveedorUbicacion)
                            },
                            alNavegarADetalle = abrirDetalleDesde(DestinoPantalla.Inicio),
                            alNavegarACreacion = {
                                listadoDeOrigen = DestinoPantalla.Inicio
                                pantallaActual = DestinoPantalla.Formulario(null)
                            },
                            alNavegarASeccion = navegarASeccion
                        )
                        is DestinoPantalla.Mapa -> MapaSenderosPantalla(
                            viewModel = viewModelDeDestino("mapa-$idUsuario") {
                                MapaSenderosViewModel(dependencias.observarSenderosDelUsuarioUseCase)
                            },
                            alNavegarADetalle = abrirDetalleDesde(DestinoPantalla.Mapa),
                            alNavegarASeccion = navegarASeccion
                        )
                        is DestinoPantalla.Favoritos -> FavoritosPantalla(
                            viewModel = viewModelDeDestino("favoritos-$idUsuario") {
                                FavoritosViewModel(dependencias.observarSenderosDelUsuarioUseCase)
                            },
                            alNavegarADetalle = abrirDetalleDesde(DestinoPantalla.Favoritos),
                            alNavegarASeccion = navegarASeccion
                        )
                        is DestinoPantalla.Perfil -> PerfilPantalla(
                            viewModel = viewModelDeDestino("perfil-$idUsuario") {
                                PerfilViewModel(
                                    dependencias.obtenerUsuarioActualUseCase,
                                    dependencias.cerrarSesionUseCase,
                                    dependencias.preferenciasAplicacion,
                                    dependencias.observarRecorridosUseCase
                                )
                            },
                            alNavegarALogin = {
                                pantallaActual = DestinoPantalla.Login
                                listOf("inicio", "mapa", "favoritos", "perfil").forEach { seccion ->
                                    almacenDestinos.liberarDestino("$seccion-$idUsuario")
                                }
                            },
                            alNavegarASeccion = navegarASeccion,
                            alNavegarAHistorialDetalle = { idRecorrido ->
                                pantallaActual = DestinoPantalla.HistorialDetalle(idRecorrido)
                            }
                        )
                        is DestinoPantalla.HistorialDetalle -> {
                            val clave = "historial-$idUsuario-${destino.idRecorrido}"
                            HistorialDetallePantalla(
                                viewModel = viewModelDeDestino(clave) {
                                    HistorialDetalleViewModel(
                                        idRecorrido = destino.idRecorrido,
                                        observarRecorridoUseCase = dependencias.observarRecorridoUseCase,
                                        observarDetalleSenderoUseCase = dependencias.observarDetalleSenderoUseCase
                                    )
                                },
                                alVolverAlPerfil = {
                                    pantallaActual = DestinoPantalla.Perfil
                                    almacenDestinos.liberarDestino(clave)
                                }
                            )
                        }
                        is DestinoPantalla.Detalle -> {
                            val clave = "detalle-$idUsuario-${destino.idSendero}"
                            DetalleSenderoPantalla(
                                viewModel = viewModelDeDestino(clave) {
                                    DetalleSenderoViewModel(
                                        idSendero = destino.idSendero,
                                        observarDetalleSenderoUseCase = dependencias.observarDetalleSenderoUseCase,
                                        alternarFavoritoUseCase = dependencias.alternarFavoritoUseCase,
                                        eliminarSenderoUseCase = dependencias.eliminarSenderoUseCase,
                                        lanzadorSeguimiento = dependencias.lanzadorSeguimiento,
                                        controlSeguimiento = dependencias.controlSeguimiento,
                                        preferenciasAplicacion = dependencias.preferenciasAplicacion
                                    )
                                },
                                alVolverAlListado = {
                                    pantallaActual = listadoDeOrigen
                                    almacenDestinos.liberarDestino(clave)
                                },
                                alNavegarAEdicion = { idSendero -> pantallaActual = DestinoPantalla.Formulario(idSendero) },
                                alNavegarARecorrido = { idSendero -> pantallaActual = DestinoPantalla.Recorrido(idSendero) }
                            )
                        }
                        is DestinoPantalla.Recorrido -> {
                            val clave = "recorrido-$idUsuario-${destino.idSendero}"
                            RecorridoPantalla(
                                viewModel = viewModelDeDestino(clave) {
                                    RecorridoViewModel(
                                        idSendero = destino.idSendero,
                                        observarDetalleSenderoUseCase = dependencias.observarDetalleSenderoUseCase,
                                        controlSeguimiento = dependencias.controlSeguimiento,
                                        lanzadorSeguimiento = dependencias.lanzadorSeguimiento
                                    )
                                },
                                // Volver no detiene el seguimiento: sigue en segundo plano con su notificación
                                alVolverAlDetalle = {
                                    pantallaActual = DestinoPantalla.Detalle(destino.idSendero)
                                    almacenDestinos.liberarDestino(clave)
                                }
                            )
                        }
                        is DestinoPantalla.Formulario -> {
                            val idEditado = destino.idSendero
                            val clave = "formulario-$idUsuario-${idEditado ?: "nuevo"}"
                            // Al editar se vuelve al detalle del sendero; al crear, al listado de origen
                            val salir = { idSendero: String? ->
                                pantallaActual = if (idEditado != null && idSendero != null) {
                                    DestinoPantalla.Detalle(idSendero)
                                } else {
                                    listadoDeOrigen
                                }
                                almacenDestinos.liberarDestino(clave)
                            }
                            FormularioSenderoPantalla(
                                viewModel = viewModelDeDestino(clave) {
                                    FormularioSenderoViewModel(
                                        idEditado,
                                        dependencias.observarDetalleSenderoUseCase,
                                        dependencias.guardarSenderoUseCase,
                                        dependencias.almacenFotos
                                    )
                                },
                                alVolver = { salir(idEditado) },
                                alGuardar = { idGuardado -> salir(idGuardado) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Navegar al recorrido pedido liberando el ViewModel del destino que se deja; con un formulario abierto
    // se conserva y se avisa, porque podría tener cambios sin guardar y descartarlos en silencio perdería datos
    private fun abrirRecorridoPedido(
        idSendero: String,
        idUsuario: String,
        destinoActual: DestinoPantalla,
        irARecorrido: () -> Unit
    ) {
        when {
            idUsuario.isEmpty() -> Unit
            destinoActual is DestinoPantalla.Formulario -> {
                Toast.makeText(
                    this,
                    "Terminá o descartá el formulario para volver al recorrido en curso",
                    Toast.LENGTH_LONG
                ).show()
            }
            destinoActual is DestinoPantalla.Recorrido && destinoActual.idSendero == idSendero -> Unit
            else -> {
                claveDeAlmacen(destinoActual, idUsuario)?.let { clave -> almacenDestinos.liberarDestino(clave) }
                irARecorrido()
            }
        }
    }

    // Componer el ViewModel de un destino guardado en su propio almacén para poder descartarlo al salir
    @Composable
    private inline fun <reified VM : ViewModel> viewModelDeDestino(clave: String, crossinline crear: () -> VM): VM {
        return viewModel(
            viewModelStoreOwner = almacenDestinos.obtenerPropietario(clave),
            factory = viewModelFactory { initializer { crear() } }
        )
    }
}

// Obtener la clave del almacén de ViewModels de un destino; Login y Registro no usan almacén por destino
private fun claveDeAlmacen(destino: DestinoPantalla, idUsuario: String): String? {
    return when (destino) {
        is DestinoPantalla.Login, is DestinoPantalla.Registro -> null
        is DestinoPantalla.Inicio -> "inicio-$idUsuario"
        is DestinoPantalla.Mapa -> "mapa-$idUsuario"
        is DestinoPantalla.Favoritos -> "favoritos-$idUsuario"
        is DestinoPantalla.Perfil -> "perfil-$idUsuario"
        is DestinoPantalla.Detalle -> "detalle-$idUsuario-${destino.idSendero}"
        is DestinoPantalla.Formulario -> "formulario-$idUsuario-${destino.idSendero ?: "nuevo"}"
        is DestinoPantalla.Recorrido -> "recorrido-$idUsuario-${destino.idSendero}"
        is DestinoPantalla.HistorialDetalle -> "historial-$idUsuario-${destino.idRecorrido}"
    }
}

// rememberSaveable solo sabe guardar tipos simples, así que el destino viaja como texto al girar la pantalla
private val guardadorDeDestino = Saver<DestinoPantalla, String>(
    save = { destino ->
        when (destino) {
            is DestinoPantalla.Login -> "login"
            is DestinoPantalla.Registro -> "registro"
            is DestinoPantalla.Inicio -> "inicio"
            is DestinoPantalla.Mapa -> "mapa"
            is DestinoPantalla.Favoritos -> "favoritos"
            is DestinoPantalla.Perfil -> "perfil"
            is DestinoPantalla.Detalle -> "detalle:${destino.idSendero}"
            is DestinoPantalla.Formulario -> "formulario:${destino.idSendero.orEmpty()}"
            is DestinoPantalla.Recorrido -> "recorrido:${destino.idSendero}"
            is DestinoPantalla.HistorialDetalle -> "historial:${destino.idRecorrido}"
        }
    },
    restore = { texto ->
        when {
            texto == "login" -> DestinoPantalla.Login
            texto == "registro" -> DestinoPantalla.Registro
            texto == "inicio" -> DestinoPantalla.Inicio
            texto == "mapa" -> DestinoPantalla.Mapa
            texto == "favoritos" -> DestinoPantalla.Favoritos
            texto == "perfil" -> DestinoPantalla.Perfil
            texto.startsWith("detalle:") -> DestinoPantalla.Detalle(texto.removePrefix("detalle:"))
            texto.startsWith("formulario:") -> DestinoPantalla.Formulario(texto.removePrefix("formulario:").ifEmpty { null })
            texto.startsWith("recorrido:") -> DestinoPantalla.Recorrido(texto.removePrefix("recorrido:"))
            texto.startsWith("historial:") -> DestinoPantalla.HistorialDetalle(texto.removePrefix("historial:"))
            else -> null
        }
    }
)
