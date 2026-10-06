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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.unaj.senderos.data.repository.AutenticacionRepositorioImpl
import com.unaj.senderos.domain.usecase.CerrarSesionUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionConGoogleUseCase
import com.unaj.senderos.domain.usecase.IniciarSesionUseCase
import com.unaj.senderos.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.presentation.explorar.ExplorarPantalla
import com.unaj.senderos.presentation.explorar.ExplorarViewModel
import com.unaj.senderos.presentation.login.LoginPantalla
import com.unaj.senderos.presentation.login.LoginViewModel

sealed interface DestinoPantalla {
    data object Login : DestinoPantalla
    data object Explorar : DestinoPantalla
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
        val casoDeUsoLogin = IniciarSesionUseCase(repositorio)
        val casoDeUsoGoogle = IniciarSesionConGoogleUseCase(repositorio)
        val casoDeUsoObtenerUsuario = ObtenerUsuarioActualUseCase(repositorio)
        val casoDeUsoCerrarSesion = CerrarSesionUseCase(repositorio)

        val fabrica = object : ViewModelProvider.Factory {
            // Crear las instancias de los ViewModels requeridos por la arquitectura
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return when {
                    modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                        LoginViewModel(casoDeUsoLogin, casoDeUsoGoogle) as T
                    }
                    modelClass.isAssignableFrom(ExplorarViewModel::class.java) -> {
                        ExplorarViewModel(casoDeUsoObtenerUsuario, casoDeUsoCerrarSesion) as T
                    }
                    else -> throw IllegalArgumentException("Clase de ViewModel desconocida: ${modelClass.name}")
                }
            }
        }

        val loginViewModel = ViewModelProvider(this, fabrica)[LoginViewModel::class.java]
        val explorarViewModel = ViewModelProvider(this, fabrica)[ExplorarViewModel::class.java]

        setContent {
            var pantallaActual by remember {
                val usuarioActivo = casoDeUsoObtenerUsuario()
                mutableStateOf<DestinoPantalla>(
                    if (usuarioActivo != null) DestinoPantalla.Explorar else DestinoPantalla.Login
                )
            }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF0D6077),
                    background = Color.White,
                    surface = Color.White,
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
                                    explorarViewModel.cargarDatosUsuario()
                                    pantallaActual = DestinoPantalla.Explorar
                                }
                            )
                        }
                        is DestinoPantalla.Explorar -> {
                            ExplorarPantalla(
                                viewModel = explorarViewModel,
                                alCerrarSesion = {
                                    pantallaActual = DestinoPantalla.Login
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
