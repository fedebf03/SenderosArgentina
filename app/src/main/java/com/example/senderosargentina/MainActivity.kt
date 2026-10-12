package com.example.senderosargentina

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import com.unaj.senderos.presentation.componentes.SeccionPrincipal
import com.unaj.senderos.presentation.detalle.DetalleSenderoPantalla
import com.unaj.senderos.presentation.detalle.DetalleSenderoViewModel
import com.unaj.senderos.presentation.favoritos.FavoritosPantalla
import com.unaj.senderos.presentation.favoritos.FavoritosViewModel
import com.unaj.senderos.presentation.formulario.FormularioSenderoPantalla
import com.unaj.senderos.presentation.formulario.FormularioSenderoViewModel
import com.unaj.senderos.presentation.login.LoginPantalla
import com.unaj.senderos.presentation.login.LoginViewModel
import com.unaj.senderos.presentation.mapasenderos.MapaSenderosPantalla
import com.unaj.senderos.presentation.mapasenderos.MapaSenderosViewModel
import com.unaj.senderos.presentation.perfil.PerfilPantalla
import com.unaj.senderos.presentation.perfil.PerfilViewModel
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                PerfilViewModel(dependencias.obtenerUsuarioActualUseCase, dependencias.cerrarSesionUseCase)
                            },
                            alNavegarALogin = {
                                pantallaActual = DestinoPantalla.Login
                                listOf("inicio", "mapa", "favoritos", "perfil").forEach { seccion ->
                                    almacenDestinos.liberarDestino("$seccion-$idUsuario")
                                }
                            },
                            alNavegarASeccion = navegarASeccion
                        )
                        is DestinoPantalla.Detalle -> {
                            val clave = "detalle-$idUsuario-${destino.idSendero}"
                            DetalleSenderoPantalla(
                                viewModel = viewModelDeDestino(clave) {
                                    DetalleSenderoViewModel(
                                        idSendero = destino.idSendero,
                                        observarDetalleSenderoUseCase = dependencias.observarDetalleSenderoUseCase,
                                        alternarFavoritoUseCase = dependencias.alternarFavoritoUseCase,
                                        eliminarSenderoUseCase = dependencias.eliminarSenderoUseCase
                                    )
                                },
                                alVolverAlListado = {
                                    pantallaActual = listadoDeOrigen
                                    almacenDestinos.liberarDestino(clave)
                                },
                                alNavegarAEdicion = { idSendero -> pantallaActual = DestinoPantalla.Formulario(idSendero) }
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

    @Composable
    private inline fun <reified VM : ViewModel> viewModelDeDestino(clave: String, crossinline crear: () -> VM): VM {
        return viewModel(
            viewModelStoreOwner = almacenDestinos.obtenerPropietario(clave),
            factory = viewModelFactory { initializer { crear() } }
        )
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
            else -> null
        }
    }
)
