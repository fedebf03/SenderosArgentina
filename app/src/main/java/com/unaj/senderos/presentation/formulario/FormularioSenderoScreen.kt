package com.unaj.senderos.presentation.formulario

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.util.Log
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.domain.model.CampoSendero
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.presentation.tema.TemaSenderos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val TAG = "FORMULARIO_SENDERO"
private const val UNA_HORA_EN_MILISEGUNDOS = 3_600_000L

// Renderizar el formulario de sendero conectando el ViewModel, la cámara y la galería
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

    var mostrarDialogoOrigenFoto by rememberSaveable { mutableStateOf(false) }
    var rutaFotoPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { seTomoFoto ->
        val rutaPendiente = rutaFotoPendiente
        rutaFotoPendiente = null
        alcanceCorrutina.launch {
            if (rutaPendiente != null && seTomoFoto) {
                try {
                    val uri = obtenerUriDeFoto(contexto, File(rutaPendiente))
                    viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFoto(uri.toString()))
                } catch (error: IllegalArgumentException) {
                    Log.e(TAG, "No se pudo compartir la foto de la cámara", error)
                    estadoAvisos.showSnackbar("No se pudo leer la foto tomada")
                }
            } else {
                if (rutaPendiente != null) borrarArchivo(rutaPendiente)
                Log.d(TAG, "Captura de foto cancelada")
            }
        }
    }

    // Limpiar fuera del hilo principal los temporales viejos de la cámara al entrar al formulario
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { limpiarFotosTemporalesViejas(contexto) }
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
        alElegirFoto = { mostrarDialogoOrigenFoto = true }
    )

    if (mostrarDialogoOrigenFoto) {
        DialogoOrigenFoto(
            alSacarFoto = {
                mostrarDialogoOrigenFoto = false
                alcanceCorrutina.launch {
                    try {
                        val archivo = withContext(Dispatchers.IO) { crearArchivoTemporalDeFoto(contexto) }
                        rutaFotoPendiente = archivo.absolutePath
                        camara.launch(obtenerUriDeFoto(contexto, archivo))
                    } catch (excepcion: ActivityNotFoundException) {
                        Log.e(TAG, "No hay una aplicación de cámara disponible", excepcion)
                        rutaFotoPendiente?.let { ruta -> borrarArchivo(ruta) }
                        rutaFotoPendiente = null
                        estadoAvisos.showSnackbar("No hay una aplicación de cámara disponible")
                    } catch (error: IOException) {
                        Log.e(TAG, "No se pudo preparar el archivo de la foto", error)
                        estadoAvisos.showSnackbar("No se pudo preparar la cámara")
                    } catch (error: IllegalArgumentException) {
                        Log.e(TAG, "No se pudo compartir el archivo de la foto", error)
                        estadoAvisos.showSnackbar("No se pudo preparar la cámara")
                    }
                }
            },
            alElegirDeGaleria = {
                mostrarDialogoOrigenFoto = false
                selectorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            alCerrar = { mostrarDialogoOrigenFoto = false }
        )
    }
}

// Crear un archivo temporal en la caché para la foto de la cámara (debe llamarse fuera del hilo principal)
private fun crearArchivoTemporalDeFoto(contexto: Context): File {
    val carpeta = File(contexto.cacheDir, "fotos_camara")
    carpeta.mkdirs()
    return File.createTempFile("foto_", ".jpg", carpeta)
}

// Borrar los temporales de la cámara con más de una hora de antigüedad (debe llamarse fuera del hilo principal)
private fun limpiarFotosTemporalesViejas(contexto: Context) {
    val limite = System.currentTimeMillis() - UNA_HORA_EN_MILISEGUNDOS
    File(contexto.cacheDir, "fotos_camara").listFiles()
        ?.filter { archivo -> archivo.lastModified() < limite }
        ?.forEach { archivo -> archivo.delete() }
}

// Obtener la Uri de contenido del proveedor de archivos propio para un temporal de la cámara
private fun obtenerUriDeFoto(contexto: Context, archivo: File): Uri {
    return FileProvider.getUriForFile(contexto, "${contexto.packageName}.proveedorarchivos", archivo)
}

// Borrar un archivo temporal por su ruta fuera del hilo principal
private suspend fun borrarArchivo(ruta: String) {
    withContext(Dispatchers.IO) { File(ruta).delete() }
}

// Renderizar el contenido del formulario a partir del estado visual
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

// Renderizar la barra superior del formulario con el botón de volver
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

// Renderizar el botón de guardar del formulario con su indicador de progreso
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

// Renderizar las secciones desplazables del formulario
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

// Previsualizar el formulario con datos de ejemplo
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
