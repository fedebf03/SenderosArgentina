package com.example.senderosargentina

import android.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.unaj.senderos.presentation.detalle.DetalleSenderoPantalla
import com.unaj.senderos.presentation.detalle.DetalleSenderoViewModel
import com.unaj.senderos.presentation.formulario.FormularioSenderoPantalla
import com.unaj.senderos.presentation.formulario.FormularioSenderoViewModel
import com.unaj.senderos.presentation.login.LoginPantalla
import com.unaj.senderos.presentation.login.LoginViewModel
import com.unaj.senderos.presentation.registro.RegistroPantalla
import com.unaj.senderos.presentation.registro.RegistroViewModel
import com.unaj.senderos.presentation.senderos.SenderosPantalla
import com.unaj.senderos.presentation.senderos.SenderosViewModel

sealed interface DestinoPantalla {
    data object Login : DestinoPantalla
    data object Registro : DestinoPantalla
    data object Inicio : DestinoPantalla
    data class Detalle(val idSendero: String) : DestinoPantalla
    data class Formulario(val idSendero: String?) : DestinoPantalla
}

class MainActivity : ComponentActivity() {
    // Inicializar la actividad principal de la aplicación y configurar la navegación
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar el fondo blanco a nivel de Window antes de renderizar la vista
        window.setBackgroundDrawableResource(R.color.white)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val dependencias = (application as SenderosAplicacion).contenedorDependencias
        val almacenDestinos = ViewModelProvider(this)[AlmacenDestinosViewModel::class.java]
        val casoDeUsoObtenerUsuario = dependencias.obtenerUsuarioActualUseCase

        val fabrica = object : ViewModelProvider.Factory {
            // Crear las instancias de los ViewModels de alcance Activity (Login y Registro)
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return when {
                    modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                        LoginViewModel(
                            dependencias.iniciarSesionUseCase,
                            dependencias.iniciarSesionConGoogleUseCase
                        ) as T
                    }
                    modelClass.isAssignableFrom(RegistroViewModel::class.java) -> {
                        RegistroViewModel(dependencias.registrarUsuarioUseCase) as T
                    }
                    else -> throw IllegalArgumentException("Clase de ViewModel desconocida: ${modelClass.name}")
                }
            }
        }

        val loginViewModel = ViewModelProvider(this, fabrica)[LoginViewModel::class.java]
        val registroViewModel = ViewModelProvider(this, fabrica)[RegistroViewModel::class.java]

        setContent {
            // El destino se guarda como texto con rememberSaveable para que sobreviva al giro de pantalla
            var pantallaActual by rememberSaveable(stateSaver = guardadorDeDestino) {
                val usuarioActivo = casoDeUsoObtenerUsuario()
                mutableStateOf<DestinoPantalla>(
                    if (usuarioActivo != null) DestinoPantalla.Inicio else DestinoPantalla.Login
                )
            }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF2B6CA3),
                    primaryContainer = Color(0xFF74ACDF),
                    onPrimaryContainer = Color(0xFF0B2A4A),
                    background = Color(0xFFF4F9FD),
                    surface = Color(0xFFF4F9FD),
                    onBackground = Color(0xFF1E293B),
                    onSurface = Color(0xFF1E293B)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val destinoActual = pantallaActual
                    when (destinoActual) {
                        is DestinoPantalla.Login -> {
                            LoginPantalla(
                                viewModel = loginViewModel,
                                alNavegarAInicio = {
                                    pantallaActual = DestinoPantalla.Inicio
                                },
                                alNavegarARegistro = { pantallaActual = DestinoPantalla.Registro }
                            )
                        }
                        is DestinoPantalla.Registro -> {
                            RegistroPantalla(
                                viewModel = registroViewModel,
                                alNavegarAInicio = {
                                    pantallaActual = DestinoPantalla.Inicio
                                },
                                alVolverALogin = { pantallaActual = DestinoPantalla.Login }
                            )
                        }
                        is DestinoPantalla.Inicio -> {
                            val idUsuario = casoDeUsoObtenerUsuario()?.id.orEmpty()
                            val senderosViewModel: SenderosViewModel = viewModel(
                                viewModelStoreOwner = almacenDestinos.obtenerPropietario("inicio-$idUsuario"),
                                factory = viewModelFactory {
                                    initializer {
                                        SenderosViewModel(
                                            dependencias.observarSenderosDelUsuarioUseCase,
                                            dependencias.cerrarSesionUseCase
                                        )
                                    }
                                }
                            )
                            SenderosPantalla(
                                viewModel = senderosViewModel,
                                alNavegarALogin = {
                                    pantallaActual = DestinoPantalla.Login
                                    almacenDestinos.liberarDestino("inicio-$idUsuario")
                                },
                                alNavegarADetalle = { idSendero ->
                                    pantallaActual = DestinoPantalla.Detalle(idSendero)
                                },
                                alNavegarACreacion = {
                                    pantallaActual = DestinoPantalla.Formulario(null)
                                }
                            )
                        }
                        is DestinoPantalla.Detalle -> {
                            val idUsuario = casoDeUsoObtenerUsuario()?.id.orEmpty()
                            val claveDetalle = "detalle-$idUsuario-${destinoActual.idSendero}"
                            val detalleViewModel: DetalleSenderoViewModel = viewModel(
                                viewModelStoreOwner = almacenDestinos.obtenerPropietario(claveDetalle),
                                factory = viewModelFactory {
                                    initializer {
                                        DetalleSenderoViewModel(
                                            idSendero = destinoActual.idSendero,
                                            observarDetalleSenderoUseCase = dependencias.observarDetalleSenderoUseCase,
                                            alternarFavoritoUseCase = dependencias.alternarFavoritoUseCase,
                                            eliminarSenderoUseCase = dependencias.eliminarSenderoUseCase
                                        )
                                    }
                                }
                            )
                            DetalleSenderoPantalla(
                                viewModel = detalleViewModel,
                                alVolverAlListado = {
                                    pantallaActual = DestinoPantalla.Inicio
                                    almacenDestinos.liberarDestino(claveDetalle)
                                },
                                alNavegarAEdicion = { idSendero ->
                                    pantallaActual = DestinoPantalla.Formulario(idSendero)
                                }
                            )
                        }
                        is DestinoPantalla.Formulario -> {
                            val idUsuario = casoDeUsoObtenerUsuario()?.id.orEmpty()
                            val idSenderoEditado = destinoActual.idSendero
                            val claveFormulario = "formulario-$idUsuario-${idSenderoEditado ?: "nuevo"}"
                            val formularioViewModel: FormularioSenderoViewModel = viewModel(
                                viewModelStoreOwner = almacenDestinos.obtenerPropietario(claveFormulario),
                                factory = viewModelFactory {
                                    initializer {
                                        FormularioSenderoViewModel(
                                            idSenderoEditado,
                                            dependencias.observarDetalleSenderoUseCase,
                                            dependencias.guardarSenderoUseCase
                                        )
                                    }
                                }
                            )
                            FormularioSenderoPantalla(
                                viewModel = formularioViewModel,
                                alVolver = {
                                    pantallaActual = if (idSenderoEditado != null) {
                                        DestinoPantalla.Detalle(idSenderoEditado)
                                    } else {
                                        DestinoPantalla.Inicio
                                    }
                                    almacenDestinos.liberarDestino(claveFormulario)
                                },
                                alGuardar = { idSenderoGuardado ->
                                    pantallaActual = if (idSenderoEditado != null) {
                                        DestinoPantalla.Detalle(idSenderoGuardado)
                                    } else {
                                        DestinoPantalla.Inicio
                                    }
                                    almacenDestinos.liberarDestino(claveFormulario)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Convertir el destino a texto y de vuelta para conservarlo al girar la pantalla
private val guardadorDeDestino = Saver<DestinoPantalla, String>(
    save = { destino ->
        when (destino) {
            is DestinoPantalla.Login -> "login"
            is DestinoPantalla.Registro -> "registro"
            is DestinoPantalla.Inicio -> "inicio"
            is DestinoPantalla.Detalle -> "detalle:${destino.idSendero}"
            is DestinoPantalla.Formulario -> "formulario:${destino.idSendero.orEmpty()}"
        }
    },
    restore = { texto ->
        when {
            texto == "login" -> DestinoPantalla.Login
            texto == "registro" -> DestinoPantalla.Registro
            texto == "inicio" -> DestinoPantalla.Inicio
            texto.startsWith("detalle:") -> DestinoPantalla.Detalle(texto.removePrefix("detalle:"))
            texto.startsWith("formulario:") -> {
                DestinoPantalla.Formulario(texto.removePrefix("formulario:").ifEmpty { null })
            }
            else -> null
        }
    }
)
