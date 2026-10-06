package com.unaj.senderos.presentation.login

import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.senderosargentina.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val TAG = "AUTH"

private val ColorAzulPetroleo = Color(0xFF0D6077)
private val ColorNaranjaAccento = Color(0xFFE85D04)
private val ColorTextoSecundario = Color(0xFF64748B)
private val ColorBordeBoton = Color(0xFFCBD5E1)
private val ColorFondoCampo = Color(0xFFF8FAFC)

@Composable
// Coordinar el estado y los efectos de la pantalla de inicio de sesión
fun LoginPantalla(
    viewModel: LoginViewModel,
    alNavegarAInicio: () -> Unit = {},
    alNavegarARegistro: () -> Unit = {}
) {
    val contexto = LocalContext.current
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val hostEstadoSnackbar = remember { SnackbarHostState() }

    SideEffect {
        Log.d(TAG, "LoginScreen renderizada / rotación detectada")
    }

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is LoginEfecto.NavegarAInicio -> alNavegarAInicio()
                is LoginEfecto.NavegarARegistro -> alNavegarARegistro()
                is LoginEfecto.MostrarAviso -> {
                    hostEstadoSnackbar.showSnackbar(efecto.mensaje)
                }
                is LoginEfecto.SolicitarGoogleSignIn -> {
                    iniciarFlujoGoogle(
                        contexto = contexto,
                        alObtenerToken = { idToken ->
                            viewModel.procesarEvento(LoginEvento.AlContinuarConGoogle(idToken))
                        },
                        alMostrarMensaje = { mensaje ->
                            hostEstadoSnackbar.showSnackbar(mensaje)
                        }
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = hostEstadoSnackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValores ->
        LoginContenido(
            estadoVisual = estadoVisual,
            alProcesarEvento = viewModel::procesarEvento,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
        )
    }
}

@Composable
// Renderizar la interfaz gráfica de usuario para el inicio de sesión
fun LoginContenido(
    estadoVisual: LoginEstadoVisual,
    alProcesarEvento: (LoginEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    val estadoDesplazamiento = rememberScrollState()
    var esClaveVisibleLocal by rememberSaveable { mutableStateOf(estadoVisual.esClaveVisible) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(estadoDesplazamiento)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo circular con isotipo de montañas
                LogoSenderos()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SENDEROS DE ARGENTINA",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorAzulPetroleo,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Bienvenido a tu guía de aventuras",
                    fontSize = 13.sp,
                    color = ColorTextoSecundario,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Campo de Correo Electrónico
                EtiquetaCampoTextual(
                    textoIzquierda = "CORREO ELECTRÓNICO",
                    textoDerecha = "Requerido",
                    esEnlace = false
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = estadoVisual.email,
                    onValueChange = { nuevoEmail ->
                        alProcesarEvento(LoginEvento.AlCambiarEmail(nuevoEmail))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ejemplo@correo.com", color = ColorTextoSecundario) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = "Icono Correo",
                            tint = ColorAzulPetroleo
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorFondoCampo,
                        unfocusedContainerColor = ColorFondoCampo,
                        focusedBorderColor = ColorAzulPetroleo,
                        unfocusedBorderColor = ColorBordeBoton,
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Campo de Contraseña
                EtiquetaCampoTextual(
                    textoIzquierda = "CONTRASEÑA",
                    textoDerecha = "¿Olvidaste tu contraseña?",
                    esEnlace = true,
                    alHacerClicDerecha = { alProcesarEvento(LoginEvento.AlOlvidarClave) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                val visibilidadClaveFinal = esClaveVisibleLocal || estadoVisual.esClaveVisible

                OutlinedTextField(
                    value = estadoVisual.clave,
                    onValueChange = { nuevaClave ->
                        alProcesarEvento(LoginEvento.AlCambiarClave(nuevaClave))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("••••••••", color = ColorTextoSecundario) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Icono Candado",
                            tint = ColorAzulPetroleo
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                esClaveVisibleLocal = !esClaveVisibleLocal
                                alProcesarEvento(LoginEvento.AlAlternarVisibilidadClave)
                            }
                        ) {
                            Icon(
                                imageVector = if (visibilidadClaveFinal) {
                                    Icons.Outlined.Visibility
                                } else {
                                    Icons.Outlined.VisibilityOff
                                },
                                contentDescription = "Alternar visibilidad contraseña",
                                tint = ColorTextoSecundario
                            )
                        }
                    },
                    visualTransformation = if (visibilidadClaveFinal) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorFondoCampo,
                        unfocusedContainerColor = ColorFondoCampo,
                        focusedBorderColor = ColorAzulPetroleo,
                        unfocusedBorderColor = ColorBordeBoton,
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF1E293B)
                    )
                )

                // Mensaje de error
                if (estadoVisual.mensajeError != null) {
                    Text(
                        text = estadoVisual.mensajeError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Botón Principal de Inicio de Sesión
                Button(
                    onClick = { alProcesarEvento(LoginEvento.AlIniciarSesion) },
                    enabled = !estadoVisual.estaCargando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorAzulPetroleo,
                        contentColor = Color.White
                    )
                ) {
                    if (estadoVisual.estaCargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INICIAR SESIÓN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Separador "O CONÉCTATE VÍA"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = ColorBordeBoton
                    )
                    Text(
                        text = "O CONÉCTATE VÍA",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTextoSecundario,
                        letterSpacing = 0.5.sp
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = ColorBordeBoton
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botón de Google
                OutlinedButton(
                    onClick = { alProcesarEvento(LoginEvento.AlSolicitarGoogleSignIn) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(ColorBordeBoton)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconoGoogle()
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continuar con Google",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = ColorTextoSecundario,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Texto inferior para ir a Registro
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val textoAnotado = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = ColorTextoSecundario, fontSize = 14.sp)) {
                        append("¿Primera vez aquí? ")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = ColorNaranjaAccento,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Regístrate acá")
                    }
                }

                Text(
                    text = textoAnotado,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        alProcesarEvento(LoginEvento.AlIrARegistro)
                    }
                )
        }
    }
}

@Composable
// Dibujar el logo circular con el isotipo de montañas de Senderos de Argentina
private fun LogoSenderos() {
    Surface(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .border(2.dp, ColorAzulPetroleo.copy(alpha = 0.2f), CircleShape),
        color = ColorAzulPetroleo
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ancho = size.width
            val alto = size.height

            // Sol naciente en el fondo
            drawCircle(
                color = Color(0xFFFFB703),
                radius = ancho * 0.18f,
                center = Offset(ancho * 0.65f, alto * 0.40f)
            )

            // Montaña posterior (más clara)
            val caminoMontanaAtras = Path().apply {
                moveTo(ancho * 0.25f, alto * 0.72f)
                lineTo(ancho * 0.58f, alto * 0.32f)
                lineTo(ancho * 0.90f, alto * 0.72f)
                close()
            }
            drawPath(
                path = caminoMontanaAtras,
                color = Color(0xFF1D829B)
            )

            // Montaña frontal principal
            val caminoMontanaPrincipal = Path().apply {
                moveTo(ancho * 0.08f, alto * 0.72f)
                lineTo(ancho * 0.40f, alto * 0.25f)
                lineTo(ancho * 0.75f, alto * 0.72f)
                close()
            }
            drawPath(
                path = caminoMontanaPrincipal,
                color = Color(0xFF084152)
            )

            // Cumbre nevada de la montaña principal
            val caminoNieve = Path().apply {
                moveTo(ancho * 0.40f, alto * 0.25f)
                lineTo(ancho * 0.48f, alto * 0.38f)
                lineTo(ancho * 0.43f, alto * 0.36f)
                lineTo(ancho * 0.37f, alto * 0.40f)
                lineTo(ancho * 0.32f, alto * 0.37f)
                close()
            }
            drawPath(
                path = caminoNieve,
                color = Color.White
            )

            // Línea ondulada del sendero
            val caminoSendero = Path().apply {
                moveTo(ancho * 0.20f, alto * 0.78f)
                quadraticTo(
                    ancho * 0.45f, alto * 0.70f,
                    ancho * 0.50f, alto * 0.82f
                )
                quadraticTo(
                    ancho * 0.55f, alto * 0.90f,
                    ancho * 0.80f, alto * 0.80f
                )
            }
            drawPath(
                path = caminoSendero,
                color = ColorNaranjaAccento,
                style = Stroke(width = 4f)
            )
        }
    }
}

@Composable
// Renderizar la etiqueta superior de los campos de entrada de texto
private fun EtiquetaCampoTextual(
    textoIzquierda: String,
    textoDerecha: String,
    esEnlace: Boolean,
    alHacerClicDerecha: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = textoIzquierda,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF334155),
            letterSpacing = 0.5.sp
        )
        Text(
            text = textoDerecha,
            fontSize = 11.sp,
            fontWeight = if (esEnlace) FontWeight.Bold else FontWeight.Normal,
            color = if (esEnlace) ColorAzulPetroleo else ColorTextoSecundario,
            modifier = if (esEnlace) Modifier.clickable { alHacerClicDerecha() } else Modifier
        )
    }
}

@Composable
// Dibujar el icono vectorial de Google con los colores corporativos
private fun IconoGoogle() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val ancho = size.width
        val alto = size.height
        val grosor = 3.5.dp.toPx()
        val centro = Offset(ancho / 2f, alto / 2f)
        val tamanoArco = Size(ancho - grosor, alto - grosor)
        val topLeft = Offset(grosor / 2f, grosor / 2f)

        // Rojo (Arriba)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 200f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = topLeft,
            size = tamanoArco,
            style = Stroke(width = grosor)
        )
        // Amarillo (Izquierda)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 120f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = topLeft,
            size = tamanoArco,
            style = Stroke(width = grosor)
        )
        // Verde (Abajo)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 25f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = topLeft,
            size = tamanoArco,
            style = Stroke(width = grosor)
        )
        // Azul (Derecha y Barra horizontal)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 310f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = topLeft,
            size = tamanoArco,
            style = Stroke(width = grosor)
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(centro.x, centro.y),
            end = Offset(ancho - grosor / 2f, centro.y),
            strokeWidth = grosor
        )
    }
}

@Preview(showBackground = true)
@Composable
// Previsualizar la pantalla de inicio de sesión en modo claro
fun PrevisualizacionLogin() {
    MaterialTheme {
        LoginContenido(
            estadoVisual = LoginEstadoVisual(
                email = "usuario@unaj.edu.ar",
                clave = "123456"
            ),
            alProcesarEvento = {}
        )
    }
}

// Iniciar el flujo de selección de credenciales de Google utilizando Credential Manager
private suspend fun iniciarFlujoGoogle(
    contexto: Context,
    alObtenerToken: (String) -> Unit,
    alMostrarMensaje: suspend (String) -> Unit
) {
    val gestorCredenciales = CredentialManager.create(contexto)
    val serverClientId = try {
        contexto.getString(R.string.default_web_client_id)
    } catch (excepcionRecurso: Exception) {
        Log.e(TAG, "No se encontró default_web_client_id en resources: ${excepcionRecurso.message}")
        alMostrarMensaje("Error: Identificador web de Google no configurado en google-services.json")
        return
    }

    val opcionGoogleId = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(serverClientId)
        .setAutoSelectEnabled(false)
        .build()

    val peticion = GetCredentialRequest.Builder()
        .addCredentialOption(opcionGoogleId)
        .build()

    try {
        Log.d(TAG, "Ejecutando getCredential con CredentialManager")
        val respuesta = gestorCredenciales.getCredential(
            request = peticion,
            context = contexto
        )

        val credencial = respuesta.credential
        if (credencial is CustomCredential && credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val credencialGoogle = GoogleIdTokenCredential.createFrom(credencial.data)
            Log.d(TAG, "Token de Google obtenido con éxito")
            alObtenerToken(credencialGoogle.idToken)
        } else {
            Log.w(TAG, "Tipo de credencial recibido no esperado: ${credencial.type}")
            alMostrarMensaje("Tipo de credencial no compatible")
        }
    } catch (cancelacion: GetCredentialCancellationException) {
        Log.d(TAG, "Inicio de sesión cancelado por el usuario")
    } catch (sinCredenciales: androidx.credentials.exceptions.NoCredentialException) {
        Log.e(TAG, "No hay credenciales de Google disponibles: ${sinCredenciales.message}", sinCredenciales)
        alMostrarMensaje("No hay cuentas de Google en el dispositivo o falta registrar la huella SHA-1 en Firebase")
    } catch (errorCredencial: GetCredentialException) {
        Log.e(TAG, "Fallo en CredentialManager: ${errorCredencial.message}", errorCredencial)
        alMostrarMensaje("Error al iniciar con Google: ${errorCredencial.localizedMessage}")
    } catch (excepcion: Exception) {
        Log.e(TAG, "Excepción inesperada al iniciar con Google: ${excepcion.message}", excepcion)
        alMostrarMensaje("Error inesperado al conectar con Google")
    }
}
