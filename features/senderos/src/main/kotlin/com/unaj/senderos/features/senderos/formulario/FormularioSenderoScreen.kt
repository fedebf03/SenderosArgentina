package com.unaj.senderos.presentation.formulario

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.senderos.domain.model.CampoSendero
import com.unaj.senderos.domain.model.Dificultad
import com.unaj.senderos.domain.model.Etapa
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.presentation.mapa.MapaRecorrido

private const val TAG = "FORMULARIO_SENDERO_PANTALLA"

@Composable
// Coordinar el estado y los efectos de la pantalla del formulario de sendero
fun FormularioSenderoPantalla(
    viewModel: FormularioSenderoViewModel,
    alVolver: () -> Unit,
    alGuardar: (String) -> Unit
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val estadoAvisos = remember { SnackbarHostState() }
    val volverActual by rememberUpdatedState(alVolver)
    val guardarActual by rememberUpdatedState(alGuardar)

    BackHandler { viewModel.procesarEvento(FormularioSenderoEvento.AlVolver) }

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            Log.d(TAG, "Efecto recibido: $efecto")
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
        estadoAvisos = estadoAvisos
    )
}

@Composable
// Dibujar el formulario de creación o edición con barra superior, campos y botón de guardado
fun FormularioSenderoContenido(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit,
    modifier: Modifier = Modifier,
    estadoAvisos: SnackbarHostState = remember { SnackbarHostState() }
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
// Dibujar la barra superior con el botón de volver y el título según el modo
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
// Dibujar el botón principal fijo al pie para guardar el sendero
private fun BotonGuardar(
    estaGuardando: Boolean,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Button(
            onClick = { alProcesarEvento(FormularioSenderoEvento.AlGuardar) },
            enabled = !estaGuardando,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
// Dibujar el cuerpo desplazable con todas las secciones del formulario
private fun CuerpoFormulario(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

@Composable
// Dibujar un título de sección con el estilo primario de la app
private fun TituloDeSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
// Dibujar un campo de texto controlado con etiqueta, error y mensaje de apoyo opcionales
private fun CampoTexto(
    valor: String,
    etiqueta: String,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier,
    tieneError: Boolean = false,
    mensajeError: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    accionTeclado: ImeAction = ImeAction.Next,
    esUnaLinea: Boolean = true,
    alTerminar: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = esUnaLinea,
        minLines = if (esUnaLinea) 1 else 3,
        isError = tieneError,
        supportingText = if (tieneError && mensajeError != null) {
            { Text(text = mensajeError, style = MaterialTheme.typography.bodySmall) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, imeAction = accionTeclado),
        keyboardActions = KeyboardActions(onDone = { alTerminar?.invoke() }),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
// Dibujar la sección de datos generales: nombre, región y descripción
private fun SeccionDatosGenerales(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    TituloDeSeccion("Datos generales")
    CampoTexto(
        valor = estadoVisual.nombre,
        etiqueta = "Nombre *",
        alCambiar = { texto -> alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE, texto)) },
        tieneError = CampoSendero.NOMBRE in estadoVisual.camposConError,
        mensajeError = "Ingresá un nombre"
    )
    CampoTexto(
        valor = estadoVisual.region,
        etiqueta = "Región *",
        alCambiar = { texto -> alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.REGION, texto)) },
        tieneError = CampoSendero.REGION in estadoVisual.camposConError,
        mensajeError = "Ingresá una región"
    )
    CampoTexto(
        valor = estadoVisual.descripcion,
        etiqueta = "Descripción *",
        alCambiar = { texto -> alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESCRIPCION, texto)) },
        tieneError = CampoSendero.DESCRIPCION in estadoVisual.camposConError,
        mensajeError = "Ingresá una descripción",
        esUnaLinea = false
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
// Dibujar la sección de datos técnicos: medidas, dificultad y tipo de recorrido
private fun SeccionDatosTecnicos(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    TituloDeSeccion("Datos técnicos")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        CampoTexto(
            valor = estadoVisual.distancia,
            etiqueta = "Distancia (km) *",
            alCambiar = { texto ->
                alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA, texto))
            },
            modifier = Modifier.weight(1f),
            tieneError = CampoSendero.DISTANCIA in estadoVisual.camposConError,
            mensajeError = "Ingresá una distancia mayor a 0",
            tipoTeclado = KeyboardType.Decimal
        )
        CampoTexto(
            valor = estadoVisual.desnivel,
            etiqueta = "Desnivel (m) *",
            alCambiar = { texto ->
                alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DESNIVEL, texto))
            },
            modifier = Modifier.weight(1f),
            tieneError = CampoSendero.DESNIVEL in estadoVisual.camposConError,
            mensajeError = "Ingresá un número entero mayor o igual a 0",
            tipoTeclado = KeyboardType.Number
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        CampoTexto(
            valor = estadoVisual.alturaMaxima,
            etiqueta = "Altura máxima (m) *",
            alCambiar = { texto ->
                alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.ALTURA_MAXIMA, texto))
            },
            modifier = Modifier.weight(1f),
            tieneError = CampoSendero.ALTURA_MAXIMA in estadoVisual.camposConError,
            mensajeError = "Debe ser mayor o igual a la altura mínima",
            tipoTeclado = KeyboardType.Number
        )
        CampoTexto(
            valor = estadoVisual.alturaMinima,
            etiqueta = "Altura mínima (m) *",
            alCambiar = { texto ->
                alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.ALTURA_MINIMA, texto))
            },
            modifier = Modifier.weight(1f),
            tieneError = CampoSendero.ALTURA_MINIMA in estadoVisual.camposConError,
            mensajeError = "Ingresá un número entero mayor o igual a 0",
            tipoTeclado = KeyboardType.Number
        )
    }

    Text(text = "Dificultad", style = MaterialTheme.typography.bodyMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Dificultad.entries.forEach { dificultad ->
            FichaSeleccionable(
                texto = obtenerTextoDificultad(dificultad),
                estaSeleccionada = estadoVisual.dificultad == dificultad,
                alSeleccionar = { alProcesarEvento(FormularioSenderoEvento.AlSeleccionarDificultad(dificultad)) }
            )
        }
    }

    Text(text = "Tipo de recorrido", style = MaterialTheme.typography.bodyMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TipoRecorrido.entries.forEach { tipoRecorrido ->
            FichaSeleccionable(
                texto = obtenerTextoTipoRecorrido(tipoRecorrido),
                estaSeleccionada = estadoVisual.tipoRecorrido == tipoRecorrido,
                alSeleccionar = {
                    alProcesarEvento(FormularioSenderoEvento.AlSeleccionarTipoRecorrido(tipoRecorrido))
                }
            )
        }
    }
}

@Composable
// Dibujar la sección del recorrido con una vista previa del mapa, la distancia calculada y el acceso al editor
private fun SeccionRecorrido(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    TituloDeSeccion("Recorrido")
    if (estadoVisual.recorrido.isEmpty()) {
        Text(
            text = "Todavía no marcaste el recorrido en el mapa.",
            style = MaterialTheme.typography.bodyMedium
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            // Con el editor abierto se evita tener dos mapas a la vez
            if (!estadoVisual.esEditorMapaVisible) {
                MapaRecorrido(
                    puntos = estadoVisual.recorrido,
                    tipoRecorrido = estadoVisual.tipoRecorrido,
                    modifier = Modifier.fillMaxSize(),
                    sonGestosHabilitados = false,
                    usarModoTextura = true
                )
            }
        }
        Text(
            text = "${estadoVisual.recorrido.size} puntos · ${formatearKilometros(estadoVisual.distanciaCalculadaKilometros)} km " +
                "según el mapa",
            style = MaterialTheme.typography.bodyMedium
        )
    }
    if (CampoSendero.RECORRIDO in estadoVisual.camposConError) {
        Text(
            text = "El recorrido tiene puntos con coordenadas inválidas",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
    }
    OutlinedButton(
        onClick = { alProcesarEvento(FormularioSenderoEvento.AlAbrirEditorMapa) },
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Icon(imageVector = Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(if (estadoVisual.recorrido.isEmpty()) "Marcar recorrido en el mapa" else "Editar en el mapa")
    }
}

@Composable
// Dibujar una ficha de selección única con el color de contenedor primario al estar elegida
private fun FichaSeleccionable(
    texto: String,
    estaSeleccionada: Boolean,
    alSeleccionar: () -> Unit
) {
    FilterChip(
        selected = estaSeleccionada,
        onClick = alSeleccionar,
        label = { Text(texto) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
// Dibujar la sección de planificación: duración, punto de inicio y mejor época
private fun SeccionPlanificacion(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    TituloDeSeccion("Planificación")
    CampoTexto(
        valor = estadoVisual.duracionEstimada,
        etiqueta = "Duración estimada",
        alCambiar = { texto -> alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DURACION, texto)) }
    )
    CampoTexto(
        valor = estadoVisual.puntoInicio,
        etiqueta = "Punto de inicio",
        alCambiar = { texto ->
            alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.PUNTO_INICIO, texto))
        }
    )
    CampoTexto(
        valor = estadoVisual.mejorEpoca,
        etiqueta = "Mejor época",
        alCambiar = { texto ->
            alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.MEJOR_EPOCA, texto))
        },
        accionTeclado = ImeAction.Done
    )
}

@Composable
// Dibujar una fila de lista con el texto del elemento y el botón para quitarlo
private fun FilaElementoQuitable(
    texto: String,
    descripcionQuitar: String,
    alQuitar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = alQuitar) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = descripcionQuitar)
        }
    }
}

@Composable
// Dibujar un campo de texto con botón para agregar su contenido a una lista
private fun CampoConBotonAgregar(
    valor: String,
    etiqueta: String,
    descripcionAgregar: String,
    alCambiar: (String) -> Unit,
    alAgregar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CampoTexto(
            valor = valor,
            etiqueta = etiqueta,
            alCambiar = alCambiar,
            modifier = Modifier.weight(1f),
            accionTeclado = ImeAction.Done,
            alTerminar = alAgregar
        )
        IconButton(onClick = alAgregar) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = descripcionAgregar)
        }
    }
}

@Composable
// Dibujar la sección de recomendaciones con su lista y el campo para agregar
private fun SeccionRecomendaciones(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    TituloDeSeccion("Recomendaciones")
    estadoVisual.recomendaciones.forEachIndexed { indice, recomendacion ->
        FilaElementoQuitable(
            texto = recomendacion,
            descripcionQuitar = "Quitar recomendación",
            alQuitar = { alProcesarEvento(FormularioSenderoEvento.AlQuitarRecomendacion(indice)) }
        )
    }
    CampoConBotonAgregar(
        valor = estadoVisual.textoNuevaRecomendacion,
        etiqueta = "Nueva recomendación",
        descripcionAgregar = "Agregar recomendación",
        alCambiar = { texto ->
            alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVA_RECOMENDACION, texto))
        },
        alAgregar = { alProcesarEvento(FormularioSenderoEvento.AlAgregarRecomendacion) }
    )
}

@Composable
// Dibujar la sección de servicios en el camino con su lista y el campo para agregar
private fun SeccionServicios(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    TituloDeSeccion("Servicios en el camino")
    estadoVisual.servicios.forEachIndexed { indice, servicio ->
        FilaElementoQuitable(
            texto = servicio,
            descripcionQuitar = "Quitar servicio",
            alQuitar = { alProcesarEvento(FormularioSenderoEvento.AlQuitarServicio(indice)) }
        )
    }
    CampoConBotonAgregar(
        valor = estadoVisual.textoNuevoServicio,
        etiqueta = "Nuevo servicio",
        descripcionAgregar = "Agregar servicio",
        alCambiar = { texto ->
            alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NUEVO_SERVICIO, texto))
        },
        alAgregar = { alProcesarEvento(FormularioSenderoEvento.AlAgregarServicio) }
    )
}

@Composable
// Dibujar la sección de etapas con su lista y los campos para agregar una nueva
private fun SeccionEtapas(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    TituloDeSeccion("Etapas")
    estadoVisual.etapas.forEachIndexed { indice, etapa ->
        FilaElementoQuitable(
            texto = "Etapa ${etapa.numero}: ${etapa.nombre} — ${etapa.distanciaKilometros.toString().removeSuffix(".0")} km",
            descripcionQuitar = "Quitar etapa",
            alQuitar = { alProcesarEvento(FormularioSenderoEvento.AlQuitarEtapa(indice)) }
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CampoTexto(
            valor = estadoVisual.nombreNuevaEtapa,
            etiqueta = "Nombre de la etapa",
            alCambiar = { texto ->
                alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.NOMBRE_NUEVA_ETAPA, texto))
            },
            modifier = Modifier.weight(1f)
        )
        CampoTexto(
            valor = estadoVisual.distanciaNuevaEtapa,
            etiqueta = "Km",
            alCambiar = { texto ->
                alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA_NUEVA_ETAPA, texto))
            },
            modifier = Modifier.width(96.dp),
            tipoTeclado = KeyboardType.Decimal,
            accionTeclado = ImeAction.Done,
            alTerminar = { alProcesarEvento(FormularioSenderoEvento.AlAgregarEtapa) }
        )
        IconButton(onClick = { alProcesarEvento(FormularioSenderoEvento.AlAgregarEtapa) }) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Agregar etapa")
        }
    }
}

// Obtener el texto visible de una dificultad
private fun obtenerTextoDificultad(dificultad: Dificultad): String {
    return when (dificultad) {
        Dificultad.BAJA -> "Baja"
        Dificultad.MEDIA -> "Media"
        Dificultad.ALTA -> "Alta"
    }
}

// Obtener el texto visible de un tipo de recorrido
private fun obtenerTextoTipoRecorrido(tipoRecorrido: TipoRecorrido): String {
    return when (tipoRecorrido) {
        TipoRecorrido.CIRCULAR -> "Circular"
        TipoRecorrido.IDA_Y_VUELTA -> "Ida y vuelta"
        TipoRecorrido.LINEAL -> "Lineal"
    }
}

@Preview(showBackground = true)
@Composable
// Previsualizar el formulario de creación con datos de ejemplo
private fun FormularioSenderoContenidoPreview() {
    MaterialTheme {
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
