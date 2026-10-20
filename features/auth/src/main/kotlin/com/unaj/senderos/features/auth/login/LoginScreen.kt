package com.unaj.senderos.features.auth.login

import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.core.ui.R
import com.unaj.senderos.core.ui.tema.TemaSenderos

@Composable
fun LoginPantalla(
    viewModel: LoginViewModel,
    idClienteWebGoogle: String,
    alNavegarAInicio: () -> Unit = {},
    alNavegarARegistro: () -> Unit = {}
) {
    val contexto = LocalContext.current
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is LoginEfecto.NavegarAInicio -> alNavegarAInicio()
                is LoginEfecto.NavegarARegistro -> alNavegarARegistro()
                is LoginEfecto.MostrarAviso -> snackbarHostState.showSnackbar(efecto.mensaje)
                is LoginEfecto.SolicitarGoogleSignIn -> pedirTokenDeGoogle(
                    contexto = contexto,
                    idClienteWeb = idClienteWebGoogle,
                    alObtenerToken = { idToken ->
                        viewModel.procesarEvento(LoginEvento.AlContinuarConGoogle(idToken))
                    },
                    alMostrarMensaje = { mensaje -> snackbarHostState.showSnackbar(mensaje) }
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LoginContenido(
            estadoVisual = estadoVisual,
            alProcesarEvento = viewModel::procesarEvento,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun LoginContenido(
    estadoVisual: LoginEstadoVisual,
    alProcesarEvento: (LoginEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 480.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo_senderos),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )

        Text(
            text = "SenderosArgentina",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() }
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = estadoVisual.email,
            onValueChange = { alProcesarEvento(LoginEvento.AlCambiarEmail(it)) },
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = estadoVisual.mensajeError != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = estadoVisual.clave,
            onValueChange = { alProcesarEvento(LoginEvento.AlCambiarClave(it)) },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = estadoVisual.mensajeError != null,
            visualTransformation = if (estadoVisual.esClaveVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { alProcesarEvento(LoginEvento.AlAlternarVisibilidadClave) }) {
                    Icon(
                        imageVector = if (estadoVisual.esClaveVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = if (estadoVisual.esClaveVisible) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        }
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { alProcesarEvento(LoginEvento.AlIniciarSesion) }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (estadoVisual.mensajeError != null) {
            Text(
                text = estadoVisual.mensajeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        TextButton(
            onClick = { alProcesarEvento(LoginEvento.AlOlvidarClave) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("¿Olvidaste tu contraseña?")
        }

        BotonPrincipal(
            texto = "Iniciar sesión",
            alTocar = { alProcesarEvento(LoginEvento.AlIniciarSesion) },
            estaCargando = estadoVisual.estaCargando,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "o",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        BotonSecundario(
            texto = "Continuar con Google",
            alTocar = { alProcesarEvento(LoginEvento.AlSolicitarGoogleSignIn) },
            estaHabilitado = !estadoVisual.estaCargando,
            icono = {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¿No tenés cuenta?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { alProcesarEvento(LoginEvento.AlIrARegistro) }) {
                Text("Registrate")
            }
        }
    }

    if (estadoVisual.esDialogoRecuperacionVisible) {
        DialogoRecuperarClave(estadoVisual, alProcesarEvento)
    }
}

@Composable
private fun DialogoRecuperarClave(
    estadoVisual: LoginEstadoVisual,
    alProcesarEvento: (LoginEvento) -> Unit
) {
    AlertDialog(
        onDismissRequest = { alProcesarEvento(LoginEvento.AlCerrarDialogoRecuperacion) },
        title = { Text("Recuperar contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Te enviamos un enlace para que elijas una contraseña nueva.")
                OutlinedTextField(
                    value = estadoVisual.emailRecuperacion,
                    onValueChange = { texto -> alProcesarEvento(LoginEvento.AlCambiarEmailRecuperacion(texto)) },
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    isError = estadoVisual.mensajeErrorRecuperacion != null,
                    supportingText = estadoVisual.mensajeErrorRecuperacion?.let { mensaje -> { Text(mensaje) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { alProcesarEvento(LoginEvento.AlEnviarEnlaceRecuperacion) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { alProcesarEvento(LoginEvento.AlEnviarEnlaceRecuperacion) },
                enabled = !estadoVisual.estaEnviandoRecuperacion
            ) {
                Text(if (estadoVisual.estaEnviandoRecuperacion) "Enviando…" else "Enviar enlace")
            }
        },
        dismissButton = {
            TextButton(onClick = { alProcesarEvento(LoginEvento.AlCerrarDialogoRecuperacion) }) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PrevisualizacionLogin() {
    TemaSenderos {
        LoginContenido(
            estadoVisual = LoginEstadoVisual(email = "usuario@unaj.edu.ar"),
            alProcesarEvento = {}
        )
    }
}
