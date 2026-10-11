package com.unaj.senderos.presentation.login

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.senderosargentina.R
import com.unaj.senderos.presentation.tema.TemaSenderos

@Composable
fun LoginPantalla(
    viewModel: LoginViewModel,
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
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo_senderos),
            contentDescription = "Logo de SenderosArgentina",
            modifier = Modifier.size(120.dp)
        )

        Text(
            text = "SenderosArgentina",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
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

        Button(
            onClick = { alProcesarEvento(LoginEvento.AlIniciarSesion) },
            enabled = !estadoVisual.estaCargando,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (estadoVisual.estaCargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Iniciar sesión")
            }
        }

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

        OutlinedButton(
            onClick = { alProcesarEvento(LoginEvento.AlSolicitarGoogleSignIn) },
            enabled = !estadoVisual.estaCargando,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_google),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Continuar con Google")
        }

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
