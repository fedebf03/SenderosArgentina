package com.unaj.senderos.features.perfil.editar

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.core.domain.model.Usuario
import com.unaj.senderos.core.domain.usecase.CampoPerfil
import com.unaj.senderos.core.domain.usecase.LONGITUD_MAXIMA_DATO_PERFIL
import com.unaj.senderos.core.ui.componentes.AvatarUsuario
import com.unaj.senderos.core.ui.componentes.BarraSuperior
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.DialogoConfirmacion
import com.unaj.senderos.core.ui.componentes.DialogoOrigenFoto
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
import com.unaj.senderos.core.ui.comun.recordarAccionesDeFoto

private const val TAG = "EDITAR_PERFIL"

@Composable
fun EditarPerfilPantalla(
    viewModel: EditarPerfilViewModel,
    alVolver: () -> Unit,
    alGuardar: () -> Unit
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val volverActual by rememberUpdatedState(alVolver)
    val guardarActual by rememberUpdatedState(alGuardar)
    var mostrarDialogoOrigenFoto by rememberSaveable { mutableStateOf(false) }
    val accionesDeFoto = recordarAccionesDeFoto(
        alSeleccionarFoto = { uri -> viewModel.procesarEvento(EditarPerfilEvento.AlSeleccionarFoto(uri)) },
        alMostrarAviso = { mensaje -> estadoAvisos.showSnackbar(mensaje) }
    )

    LaunchedEffect(Unit) {
        Log.d(TAG, "Pantalla inicializada / rotación completada")
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is EditarPerfilEfecto.VolverAlPerfil -> volverActual()
                is EditarPerfilEfecto.PerfilGuardado -> {
                    estadoAvisos.showSnackbar(efecto.mensaje)
                    guardarActual()
                }
                is EditarPerfilEfecto.MostrarAviso -> estadoAvisos.showSnackbar(efecto.mensaje)
            }
        }
    }

    BackHandler { viewModel.procesarEvento(EditarPerfilEvento.AlVolver) }

    EditarPerfilContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = viewModel::procesarEvento,
        estadoAvisos = estadoAvisos,
        alElegirFoto = { mostrarDialogoOrigenFoto = true }
    )

    if (mostrarDialogoOrigenFoto) {
        DialogoOrigenFoto(
            alSacarFoto = {
                mostrarDialogoOrigenFoto = false
                accionesDeFoto.sacarFoto()
            },
            alElegirDeGaleria = {
                mostrarDialogoOrigenFoto = false
                accionesDeFoto.elegirDeGaleria()
            },
            alCerrar = { mostrarDialogoOrigenFoto = false }
        )
    }
}

@Composable
fun EditarPerfilContenido(
    estadoVisual: EditarPerfilEstadoVisual,
    alProcesarEvento: (EditarPerfilEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() },
    alElegirFoto: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = estadoAvisos) },
        topBar = { BarraSuperior(titulo = "Editar perfil", alVolver = { alProcesarEvento(EditarPerfilEvento.AlVolver) }) }
    ) { espaciadoInterno ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(espaciadoInterno),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SeccionFoto(estadoVisual, alProcesarEvento, alElegirFoto)
                SeccionDatos(estadoVisual, alProcesarEvento)
                BotonPrincipal(
                    texto = if (estadoVisual.estaGuardando) "Guardando…" else "Guardar cambios",
                    alTocar = { alProcesarEvento(EditarPerfilEvento.AlGuardar) },
                    estaHabilitado = estadoVisual.puedeGuardar && estadoVisual.hayCambiosSinGuardar,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SeccionContrasena(estadoVisual, alProcesarEvento)
            }
        }
    }

    if (estadoVisual.esDialogoDescartarVisible) {
        DialogoConfirmacion(
            titulo = "Descartar cambios",
            texto = "Tenés cambios sin guardar en tu perfil. Si volvés ahora se pierden.",
            textoConfirmar = "Descartar",
            alConfirmar = { alProcesarEvento(EditarPerfilEvento.AlConfirmarDescartar) },
            textoDescartar = "Seguir editando",
            alDescartar = { alProcesarEvento(EditarPerfilEvento.AlCancelarDescartar) },
            esDestructivo = true
        )
    }
}

// Mostrar la foto actual con los botones para cambiarla o quitarla
@Composable
private fun SeccionFoto(
    estadoVisual: EditarPerfilEstadoVisual,
    alProcesarEvento: (EditarPerfilEvento) -> Unit,
    alElegirFoto: () -> Unit
) {
    AvatarUsuario(
        usuario = Usuario(id = "", email = estadoVisual.email, nombre = estadoVisual.nombre, fotoUrl = estadoVisual.foto),
        tamano = 96.dp
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonSecundario(
            texto = if (estadoVisual.estaProcesandoFoto) "Procesando…" else "Cambiar foto",
            alTocar = alElegirFoto,
            icono = { IconoDeBoton(Icons.Outlined.AddAPhoto) },
            estaHabilitado = !estadoVisual.estaProcesandoFoto
        )
        if (estadoVisual.foto.isNotEmpty()) {
            BotonSecundario(
                texto = "Quitar",
                alTocar = { alProcesarEvento(EditarPerfilEvento.AlQuitarFoto) },
                icono = { IconoDeBoton(Icons.Outlined.DeleteOutline) },
                esDestructivo = true
            )
        }
    }
    Text(
        text = estadoVisual.email,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Campos de texto del perfil: nombre y de dónde sos
@Composable
private fun SeccionDatos(estadoVisual: EditarPerfilEstadoVisual, alProcesarEvento: (EditarPerfilEvento) -> Unit) {
    CampoDePerfil("Nombre", estadoVisual.nombre, CampoPerfil.NOMBRE in estadoVisual.camposConError, "Ingresá tu nombre") { texto ->
        alProcesarEvento(EditarPerfilEvento.AlCambiarTexto(CampoEdicionPerfil.NOMBRE, texto))
    }
    CampoDePerfil("País", estadoVisual.pais, CampoPerfil.PAIS in estadoVisual.camposConError, "Máximo $LONGITUD_MAXIMA_DATO_PERFIL caracteres") { texto ->
        alProcesarEvento(EditarPerfilEvento.AlCambiarTexto(CampoEdicionPerfil.PAIS, texto))
    }
    CampoDePerfil("Provincia", estadoVisual.provincia, CampoPerfil.PROVINCIA in estadoVisual.camposConError, "Máximo $LONGITUD_MAXIMA_DATO_PERFIL caracteres") { texto ->
        alProcesarEvento(EditarPerfilEvento.AlCambiarTexto(CampoEdicionPerfil.PROVINCIA, texto))
    }
    CampoDePerfil("Localidad", estadoVisual.localidad, CampoPerfil.LOCALIDAD in estadoVisual.camposConError, "Máximo $LONGITUD_MAXIMA_DATO_PERFIL caracteres") { texto ->
        alProcesarEvento(EditarPerfilEvento.AlCambiarTexto(CampoEdicionPerfil.LOCALIDAD, texto))
    }
}

@Composable
private fun CampoDePerfil(
    etiqueta: String,
    valor: String,
    tieneError: Boolean,
    textoDeError: String,
    alCambiar: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        isError = tieneError,
        supportingText = if (tieneError) ({ Text(textoDeError) }) else null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

// Cambiar la contraseña con la actual como confirmación; las cuentas de Google no tienen contraseña propia
@Composable
private fun SeccionContrasena(estadoVisual: EditarPerfilEstadoVisual, alProcesarEvento: (EditarPerfilEvento) -> Unit) {
    Text(
        text = "Contraseña",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth()
    )
    if (!estadoVisual.puedeCambiarContrasena) {
        Text(
            text = "Tu cuenta entra con Google: la contraseña se administra desde tu cuenta de Google.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }
    CampoDeClave("Contraseña actual", estadoVisual.claveActual) { texto ->
        alProcesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.ACTUAL, texto))
    }
    CampoDeClave("Contraseña nueva", estadoVisual.claveNueva) { texto ->
        alProcesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.NUEVA, texto))
    }
    CampoDeClave("Repetí la contraseña nueva", estadoVisual.claveRepetida) { texto ->
        alProcesarEvento(EditarPerfilEvento.AlCambiarClave(CampoContrasena.REPETIDA, texto))
    }
    estadoVisual.mensajeErrorContrasena?.let { mensaje ->
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth()
        )
    }
    BotonSecundario(
        texto = if (estadoVisual.estaCambiandoContrasena) "Cambiando…" else "Cambiar contraseña",
        alTocar = { alProcesarEvento(EditarPerfilEvento.AlCambiarContrasena) },
        estaHabilitado = !estadoVisual.estaCambiandoContrasena &&
            estadoVisual.claveActual.isNotEmpty() && estadoVisual.claveNueva.isNotEmpty(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CampoDeClave(etiqueta: String, valor: String, alCambiar: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}
