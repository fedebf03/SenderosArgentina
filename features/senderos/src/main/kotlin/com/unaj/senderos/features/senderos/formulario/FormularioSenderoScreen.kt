package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.ui.componentes.DialogoConfirmacion
import com.unaj.senderos.core.ui.componentes.DialogoOrigenFoto
import com.unaj.senderos.core.ui.componentes.BarraSuperior
import com.unaj.senderos.core.ui.componentes.BotonPrincipal
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.core.domain.model.CampoSendero
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.ui.comun.limpiarFotosTemporalesViejas
import com.unaj.senderos.core.ui.comun.recordarAccionesDeFoto
import com.unaj.senderos.core.ui.tema.TemaSenderos

private const val TAG = "FORMULARIO_SENDERO"

// Ancho máximo del contenido del formulario para que no se estire en tablets
private val ANCHO_MAXIMO_FORMULARIO = 480.dp


@Composable
fun FormularioSenderoPantalla(
    viewModel: FormularioSenderoViewModel,
    alVolver: () -> Unit,
    alGuardar: (String) -> Unit,
    alEditarSenderoExistente: (String) -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val volverActual by rememberUpdatedState(alVolver)
    val guardarActual by rememberUpdatedState(alGuardar)
    val editarExistenteActual by rememberUpdatedState(alEditarSenderoExistente)

    var mostrarDialogoOrigenFoto by rememberSaveable { mutableStateOf(false) }
    val contexto = LocalContext.current
    val accionesDeFoto = recordarAccionesDeFoto(
        alSeleccionarFoto = { uri -> viewModel.procesarEvento(FormularioSenderoEvento.AlSeleccionarFoto(uri)) },
        alMostrarAviso = { mensaje -> estadoAvisos.showSnackbar(mensaje) }
    )

    // Limpiar fuera del hilo principal los temporales viejos de la cámara al entrar al formulario
    LaunchedEffect(Unit) {
        Log.d(TAG, "Pantalla inicializada / rotación completada")
        limpiarFotosTemporalesViejas(contexto)
    }

    BackHandler { viewModel.procesarEvento(FormularioSenderoEvento.AlVolver) }

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is FormularioSenderoEfecto.VolverAtras -> volverActual()
                is FormularioSenderoEfecto.SenderoGuardado -> guardarActual(efecto.idSendero)
                is FormularioSenderoEfecto.EditarSenderoExistente -> editarExistenteActual(efecto.idSendero)
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
    estadoVisual.nombreSenderoYaConvertido?.let { nombre ->
        DialogoConfirmacion(
            titulo = "Esta caminata ya es un sendero",
            texto = "Ya la convertiste en «$nombre». Si la reemplazás se vuelve a crear desde el trazado y se pierden los cambios " +
                "que le hiciste y los puntos de interés que ya tenía. Si preferís, abrí ese sendero para editarlo.",
            textoConfirmar = "Reemplazar",
            alConfirmar = { viewModel.procesarEvento(FormularioSenderoEvento.AlReemplazarSenderoDeCaminata) },
            textoDescartar = "Editar el existente",
            alDescartar = { viewModel.procesarEvento(FormularioSenderoEvento.AlEditarSenderoDeCaminata) },
            esDestructivo = true
        )
    }

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
            topBar = {
                BarraSuperior(
                    titulo = if (estadoVisual.esEdicion) "Editar sendero" else "Nuevo sendero",
                    alVolver = { alProcesarEvento(FormularioSenderoEvento.AlVolver) },
                    acciones = {
                        TextButton(
                            onClick = { alProcesarEvento(FormularioSenderoEvento.AlGuardar) },
                            enabled = !estadoVisual.estaGuardando && !estadoVisual.estaProcesandoFoto
                        ) {
                            if (estadoVisual.estaGuardando) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .semantics {
                                            contentDescription = "Guardando"
                                            stateDescription = "Guardando"
                                        },
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Guardar")
                            }
                        }
                    }
                )
            },
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
                    modifier = Modifier.padding(espaciadoInterno).consumeWindowInsets(espaciadoInterno)
                )
            }
        }

        if (estadoVisual.esEditorMapaVisible) {
            EditorRecorridoMapa(estadoVisual = estadoVisual, alProcesarEvento = alProcesarEvento)
        }
    }

    if (estadoVisual.esDialogoDescartarVisible) {
        DialogoConfirmacion(
            titulo = "Descartar cambios",
            texto = "¿Querés descartar los cambios? Los datos que modificaste se van a perder.",
            textoConfirmar = "Descartar",
            alConfirmar = { alProcesarEvento(FormularioSenderoEvento.AlConfirmarDescartar) },
            textoDescartar = "Seguir editando",
            alDescartar = { alProcesarEvento(FormularioSenderoEvento.AlCancelarDescartar) },
            esDestructivo = true
        )
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
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = ANCHO_MAXIMO_FORMULARIO)
            .imePadding()
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
        Spacer(Modifier.height(8.dp))
        BotonPrincipal(
            texto = "Guardar",
            alTocar = { alProcesarEvento(FormularioSenderoEvento.AlGuardar) },
            icono = { IconoDeBoton(Icons.Filled.Save) },
            estaCargando = estadoVisual.estaGuardando,
            modifier = Modifier.fillMaxWidth()
        )
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
