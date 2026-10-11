package com.unaj.senderos.presentation.formulario

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.domain.model.CampoSendero
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.presentation.tema.TemaSenderos

@Composable
fun FormularioSenderoPantalla(
    viewModel: FormularioSenderoViewModel,
    alVolver: () -> Unit,
    alGuardar: (String) -> Unit
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val volverActual by rememberUpdatedState(alVolver)
    val guardarActual by rememberUpdatedState(alGuardar)

    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFoto(uri.toString()))
    }

    BackHandler { viewModel.procesarEvento(FormularioSenderoEvento.AlVolver) }

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is FormularioSenderoEfecto.VolverAtras -> volverActual()
                is FormularioSenderoEfecto.SenderoGuardado -> guardarActual(efecto.idSendero)
                is FormularioSenderoEfecto.MostrarAviso -> estadoAvisos.showSnackbar(efecto.mensaje)
            }
        }
    }

    FormularioSenderoContenido(
        estadoVisual = estadoVisual,
        alProcesarEvento = viewModel::procesarEvento,
        estadoAvisos = estadoAvisos,
        alElegirFoto = {
            selectorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    )
}

@Composable
fun FormularioSenderoContenido(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() },
    alElegirFoto: () -> Unit = {}
) {
    Box(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { BarraSuperiorFormulario(estadoVisual.esEdicion, alProcesarEvento) },
            bottomBar = { BotonGuardar(estadoVisual.estaGuardando, alProcesarEvento) },
            snackbarHost = { SnackbarHost(hostState = estadoAvisos) }
        ) { espaciadoInterno ->
            when {
                estadoVisual.estaCargando -> Box(
                    modifier = Modifier.fillMaxSize().padding(espaciadoInterno),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                estadoVisual.mensajeError != null -> Column(
                    modifier = Modifier.fillMaxSize().padding(espaciadoInterno).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = estadoVisual.mensajeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { alProcesarEvento(FormularioSenderoEvento.AlVolver) }) {
                        Text("Volver")
                    }
                }
                else -> CuerpoFormulario(
                    estadoVisual = estadoVisual,
                    alProcesarEvento = alProcesarEvento,
                    alElegirFoto = alElegirFoto,
                    modifier = Modifier.padding(espaciadoInterno)
                )
            }
        }

        if (estadoVisual.esEditorMapaVisible) {
            EditorRecorridoMapa(estadoVisual = estadoVisual, alProcesarEvento = alProcesarEvento)
        }
    }

    if (estadoVisual.esDialogoDescartarVisible) {
        AlertDialog(
            onDismissRequest = { alProcesarEvento(FormularioSenderoEvento.AlCancelarDescartar) },
            title = { Text("¿Descartar los cambios?") },
            text = { Text("Los datos que modificaste se van a perder.") },
            confirmButton = {
                TextButton(onClick = { alProcesarEvento(FormularioSenderoEvento.AlConfirmarDescartar) }) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { alProcesarEvento(FormularioSenderoEvento.AlCancelarDescartar) }) {
                    Text("Seguir editando")
                }
            }
        )
    }
}

@Composable
private fun BarraSuperiorFormulario(
    esEdicion: Boolean,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = { alProcesarEvento(FormularioSenderoEvento.AlVolver) }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Text(
            text = if (esEdicion) "Editar sendero" else "Nuevo sendero",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BotonGuardar(
    estaGuardando: Boolean,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Button(
            onClick = { alProcesarEvento(FormularioSenderoEvento.AlGuardar) },
            enabled = !estaGuardando,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .height(50.dp)
        ) {
            if (estaGuardando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text("Guardar")
            }
        }
    }
}

@Composable
private fun CuerpoFormulario(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit,
    alElegirFoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SeccionFoto(estadoVisual, alElegirFoto, alProcesarEvento)
        Spacer(Modifier.height(4.dp))
        SeccionDatosGenerales(estadoVisual, alProcesarEvento)
        Spacer(Modifier.height(4.dp))
        SeccionDatosTecnicos(estadoVisual, alProcesarEvento)
        Spacer(Modifier.height(4.dp))
        SeccionRecorrido(estadoVisual, alProcesarEvento)
        Spacer(Modifier.height(4.dp))
        SeccionPlanificacion(estadoVisual, alProcesarEvento)
        Spacer(Modifier.height(4.dp))
        SeccionRecomendaciones(estadoVisual, alProcesarEvento)
        Spacer(Modifier.height(4.dp))
        SeccionServicios(estadoVisual, alProcesarEvento)
        Spacer(Modifier.height(4.dp))
        SeccionEtapas(estadoVisual, alProcesarEvento)
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun FormularioSenderoContenidoPreview() {
    TemaSenderos {
        FormularioSenderoContenido(
            estadoVisual = FormularioSenderoEstadoVisual(
                nombre = "Cerro Catedral",
                region = "Bariloche",
                distancia = "12",
                camposConError = setOf(CampoSendero.DESCRIPCION),
                recomendaciones = listOf("Llevar agua"),
                etapas = listOf(Etapa(numero = 1, nombre = "Base al refugio", distanciaKilometros = 3.5))
            ),
            alProcesarEvento = {}
        )
    }
}
