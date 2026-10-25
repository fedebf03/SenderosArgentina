package com.unaj.senderos.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.unaj.senderos.app.navegacion.DestinoPantalla
import com.unaj.senderos.app.navegacion.NavegacionApp
import com.unaj.senderos.app.navegacion.esSeccionPrincipal
import com.unaj.senderos.app.navegacion.reemplazarPor
import com.unaj.senderos.core.data.seguimiento.EXTRA_ID_SENDERO_RECORRIDO
import com.unaj.senderos.core.data.seguimiento.EXTRA_SOLICITAR_CANCELACION
import com.unaj.senderos.core.domain.usecase.ObtenerUsuarioActualUseCase
import com.unaj.senderos.core.ui.tema.TemaSenderos
import com.unaj.senderos.features.auth.login.LoginViewModel
import com.unaj.senderos.features.auth.registro.RegistroViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var obtenerUsuarioActualUseCase: ObtenerUsuarioActualUseCase

    // Login y Registro viven mientras viva la Activity. Así se conserva lo escrito al ir y volver entre ambas
    private val loginViewModel: LoginViewModel by viewModels()
    private val registroViewModel: RegistroViewModel by viewModels()

    // Lo carga la notificación del recorrido en curso. Después la composición navega al recorrido y lo vuelve a null
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
        // Tras recrear la Activity (giro o muerte del proceso) Navigation restaura el back stack solo
        if (savedInstanceState == null) leerPedidoDeRecorrido(intent)
        window.setBackgroundDrawableResource(android.R.color.white)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            val navController = rememberNavController()
            // Solo decide el back stack de arranque. Si la Activity se recrea, se restaura el que había
            val destinoInicial = remember {
                if (obtenerUsuarioActualUseCase() != null) DestinoPantalla.Explorar else DestinoPantalla.Login
            }
            val estadoAvisosGlobal = remember { SnackbarHostState() }
            val alcance = rememberCoroutineScope()

            val idRecorridoPedido = idSenderoRecorridoPendiente
            LaunchedEffect(idRecorridoPedido) {
                if (idRecorridoPedido != null) {
                    abrirRecorridoPedido(
                        navController = navController,
                        idSendero = idRecorridoPedido,
                        alAvisar = { mensaje -> alcance.launch { estadoAvisosGlobal.showSnackbar(mensaje) } }
                    )
                    if (navController.currentDestination?.hasRoute<DestinoPantalla.Recorrido>() != true) {
                        solicitudCancelacionPendiente = false
                    }
                    idSenderoRecorridoPendiente = null
                }
            }

            TemaSenderos {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NavegacionApp(
                        navController = navController,
                        destinoInicial = destinoInicial,
                        loginViewModel = loginViewModel,
                        registroViewModel = registroViewModel,
                        solicitarCancelacionRecorrido = solicitudCancelacionPendiente,
                        alAtenderSolicitudCancelacion = { solicitudCancelacionPendiente = false }
                    )
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

    // Con un formulario abierto no se navega y se avisa, porque descartar cambios sin guardar perdería datos.
    // Si no hay sesión activa, el pedido de la notificación se descarta
    private fun abrirRecorridoPedido(
        navController: NavController,
        idSendero: String,
        alAvisar: (String) -> Unit
    ) {
        val entradaActual = navController.currentBackStackEntry ?: return
        val destinoActual = entradaActual.destination
        when {
            obtenerUsuarioActualUseCase() == null -> Unit
            destinoActual.hasRoute<DestinoPantalla.Formulario>() -> {
                alAvisar("Terminá o descartá el formulario para volver al recorrido en curso")
            }
            destinoActual.hasRoute<DestinoPantalla.Recorrido>() &&
                entradaActual.toRoute<DestinoPantalla.Recorrido>().idSendero == idSendero -> Unit
            // Sobre una sección el recorrido se abre encima, así al volver de él la sección sigue ahí
            destinoActual.esSeccionPrincipal() -> navController.navigate(DestinoPantalla.Recorrido(idSendero))
            else -> navController.reemplazarPor(DestinoPantalla.Recorrido(idSendero))
        }
    }
}
