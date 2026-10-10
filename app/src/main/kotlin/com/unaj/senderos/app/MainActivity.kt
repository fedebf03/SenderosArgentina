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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unaj.senderos.data.repository.AutenticacionRepositorioImpl
import com.unaj.senderos.data.repository.SenderoRepositorioMock
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionConGoogleUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionUseCase
import com.unaj.senderos.domain.usecase.ObtenerSenderosDelUsuarioUseCase
import com.unaj.senderos.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.domain.usecase.RegistrarUsuarioUseCase
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

        val repositorio = AutenticacionRepositorioImpl()
        val repositorioSenderos = SenderoRepositorioMock()
        val casoDeUsoLogin = IniciarSesionUseCase(repositorio)
        val casoDeUsoGoogle = IniciarSesionConGoogleUseCase(repositorio)
        val casoDeUsoRegistro = RegistrarUsuarioUseCase(repositorio)
        val casoDeUsoObtenerUsuario = ObtenerUsuarioActualUseCase(repositorio)
        val casoDeUsoCerrarSesion = CerrarSesionUseCase(repositorio)
        val casoDeUsoObtenerSenderos = ObtenerSenderosDelUsuarioUseCase(repositorioSenderos, repositorio)

        val fabrica = object : ViewModelProvider.Factory {
            // Crear las instancias de los ViewModels requeridos por la arquitectura
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return when {
                    modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                        LoginViewModel(casoDeUsoLogin, casoDeUsoGoogle) as T
                    }
                    modelClass.isAssignableFrom(RegistroViewModel::class.java) -> {
                        RegistroViewModel(casoDeUsoRegistro) as T
                    }
                    modelClass.isAssignableFrom(SenderosViewModel::class.java) -> {
                        SenderosViewModel(
                            casoDeUsoObtenerSenderos,
                            casoDeUsoCerrarSesion
                        ) as T
                    }
                    else -> throw IllegalArgumentException("Clase de ViewModel desconocida: ${modelClass.name}")
                }
            }
        }

        val loginViewModel = ViewModelProvider(this, fabrica)[LoginViewModel::class.java]
        val registroViewModel = ViewModelProvider(this, fabrica)[RegistroViewModel::class.java]

        setContent {
            var pantallaActual by remember {
                val usuarioActivo = casoDeUsoObtenerUsuario()
                mutableStateOf<DestinoPantalla>(
                    if (usuarioActivo != null) DestinoPantalla.Inicio else DestinoPantalla.Login
                )
            }

            // Cada inicio de sesión crea un SenderosViewModel nuevo al incrementar este número;
            // rememberSaveable lo conserva al girar la pantalla para reutilizar el mismo ViewModel
            var numeroSesion by rememberSaveable { mutableIntStateOf(0) }

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
                    when (pantallaActual) {
                        is DestinoPantalla.Login -> {
                            LoginPantalla(
                                viewModel = loginViewModel,
                                alNavegarAInicio = {
                                    numeroSesion++
                                    pantallaActual = DestinoPantalla.Inicio
                                },
                                alNavegarARegistro = { pantallaActual = DestinoPantalla.Registro }
                            )
                        }
                        is DestinoPantalla.Registro -> {
                            RegistroPantalla(
                                viewModel = registroViewModel,
                                alNavegarAInicio = {
                                    numeroSesion++
                                    pantallaActual = DestinoPantalla.Inicio
                                },
                                alVolverALogin = { pantallaActual = DestinoPantalla.Login }
                            )
                        }
                        is DestinoPantalla.Inicio -> {
                            val idUsuario = casoDeUsoObtenerUsuario()?.id.orEmpty()
                            val senderosViewModel: SenderosViewModel = viewModel(
                                key = "senderos-$idUsuario-$numeroSesion",
                                factory = fabrica
                            )
                            SenderosPantalla(
                                viewModel = senderosViewModel,
                                alNavegarALogin = { pantallaActual = DestinoPantalla.Login }
                            )
                        }
                    }
                }
            }
        }
    }
}
