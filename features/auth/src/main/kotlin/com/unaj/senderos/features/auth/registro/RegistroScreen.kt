package com.unaj.senderos.presentation.registro

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import com.unaj.senderos.presentation.componentes.BotonPrincipal
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun RegistroPantalla(
    viewModel: RegistroViewModel,
    alNavegarAInicio: () -> Unit,
    alVolverALogin: () -> Unit
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()

    BackHandler(onBack = alVolverALogin)

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is RegistroEfecto.NavegarAInicio -> alNavegarAInicio()
            }
        }
    }

    Scaffold { padding ->
        RegistroContenido(
            estadoVisual = estadoVisual,
            alProcesarEvento = viewModel::procesarEvento,
            alVolverALogin = alVolverALogin,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun RegistroContenido(
    estadoVisual: RegistroEstadoVisual,
    alProcesarEvento: (RegistroEvento) -> Unit,
    alVolverALogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hayError = estadoVisual.mensajeError != null
    val transformacionClave = if (estadoVisual.esClaveVisible) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }

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
            modifier = Modifier.size(96.dp)
        )

        Text(
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = estadoVisual.email,
            onValueChange = { alProcesarEvento(RegistroEvento.AlCambiarEmail(it)) },
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = hayError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = estadoVisual.clave,
            onValueChange = { alProcesarEvento(RegistroEvento.AlCambiarClave(it)) },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = hayError,
            visualTransformation = transformacionClave,
            trailingIcon = {
                IconButton(onClick = { alProcesarEvento(RegistroEvento.AlAlternarVisibilidadClave) }) {
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
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = estadoVisual.confirmacion,
            onValueChange = { alProcesarEvento(RegistroEvento.AlCambiarConfirmacion(it)) },
            label = { Text("Repetir contraseña") },
            singleLine = true,
            isError = hayError,
            visualTransformation = transformacionClave,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { alProcesarEvento(RegistroEvento.AlRegistrarse) }
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

        Spacer(modifier = Modifier.height(24.dp))

        BotonPrincipal(
            texto = "Crear cuenta",
            alTocar = { alProcesarEvento(RegistroEvento.AlRegistrarse) },
            estaCargando = estadoVisual.estaCargando,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¿Ya tenés cuenta?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = alVolverALogin) {
                Text("Iniciá sesión")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrevisualizacionRegistro() {
    TemaSenderos {
        RegistroContenido(
            estadoVisual = RegistroEstadoVisual(email = "usuario@unaj.edu.ar"),
            alProcesarEvento = {},
            alVolverALogin = {}
        )
    }
}
