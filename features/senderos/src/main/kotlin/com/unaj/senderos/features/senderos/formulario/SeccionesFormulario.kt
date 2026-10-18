package com.unaj.senderos.features.senderos.formulario

import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.IconoDeBoton
import com.unaj.senderos.core.ui.comun.formatearKilometros
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.unaj.senderos.core.ui.componentes.accionable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.unaj.senderos.core.domain.configuracion.ProvinciasArgentinas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.model.CampoSendero
import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.Dificultad
import com.unaj.senderos.core.domain.model.Etapa
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.ui.componentes.FotoSendero
import com.unaj.senderos.core.ui.componentes.TituloDeSeccion
import com.unaj.senderos.core.ui.tema.obtenerColorLineaBioma
import com.unaj.senderos.core.ui.componentes.IndicadorDificultad
import com.unaj.senderos.core.ui.componentes.PuntoBioma
import com.unaj.senderos.core.ui.componentes.obtenerTextoBioma
import com.unaj.senderos.core.ui.componentes.obtenerTextoDificultad
import com.unaj.senderos.core.ui.componentes.obtenerTextoTipoRecorrido
import com.unaj.senderos.core.ui.componentes.mapa.MapaRecorrido

@Composable
internal fun SeccionFoto(
    estadoVisual: FormularioSenderoEstadoVisual,
    alElegirFoto: () -> Unit,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    val forma = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(forma)
    ) {
        if (estadoVisual.foto.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma)
                    .then(
                        if (estadoVisual.estaProcesandoFoto) Modifier else Modifier.accionable("Agregar foto", alTocar = alElegirFoto)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Agregar foto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Opcional. Sin foto se muestra la ilustración.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            FotoSendero(foto = estadoVisual.foto, bioma = estadoVisual.bioma, modifier = Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = alElegirFoto, enabled = !estadoVisual.estaProcesandoFoto) {
                    Text("Cambiar")
                }
                FilledTonalButton(
                    onClick = { alProcesarEvento(FormularioSenderoEvento.AlQuitarFoto) },
                    enabled = !estadoVisual.estaProcesandoFoto
                ) {
                    Text("Quitar")
                }
            }
        }

        if (estadoVisual.estaProcesandoFoto) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
internal fun SeccionDatosGenerales(
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
    CampoConSugerencias(
        valor = estadoVisual.region,
        etiqueta = "Región *",
        opciones = ProvinciasArgentinas.NOMBRES,
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
internal fun SeccionDatosTecnicos(
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
    AyudaDistanciaDelMapa(estadoVisual, alProcesarEvento)
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
                alSeleccionar = { alProcesarEvento(FormularioSenderoEvento.AlSeleccionarDificultad(dificultad)) },
                iconoInicial = { IndicadorDificultad(dificultad, mostrarTexto = false) }
            )
        }
    }

    Text(text = "Bioma", style = MaterialTheme.typography.bodyMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Bioma.entries.forEach { bioma ->
            FichaSeleccionable(
                texto = obtenerTextoBioma(bioma),
                estaSeleccionada = estadoVisual.bioma == bioma,
                alSeleccionar = { alProcesarEvento(FormularioSenderoEvento.AlSeleccionarBioma(bioma)) },
                iconoInicial = { PuntoBioma(bioma) }
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
internal fun SeccionRecorrido(
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
                    usarModoTextura = true,
                    colorLinea = obtenerColorLineaBioma(estadoVisual.bioma, MaterialTheme.colorScheme.primary)
                )
            }
        }
        Text(
            text = "${estadoVisual.recorrido.size} puntos · ${formatearKilometros(estadoVisual.distanciaCalculadaKilometros)} km " +
                "según el mapa",
            style = MaterialTheme.typography.bodyMedium
        )
        PerfilEnFormulario(estadoVisual, alProcesarEvento)
    }
    if (CampoSendero.RECORRIDO in estadoVisual.camposConError) {
        Text(
            text = "El recorrido tiene puntos con coordenadas inválidas",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
    }
    BotonSecundario(
        texto = if (estadoVisual.recorrido.isEmpty()) "Marcar recorrido en el mapa" else "Editar en el mapa",
        alTocar = { alProcesarEvento(FormularioSenderoEvento.AlAbrirEditorMapa) },
        icono = { IconoDeBoton(Icons.Filled.Map) },
        modifier = Modifier.fillMaxWidth()
    )
    ImportarTrazado(estadoVisual, alProcesarEvento)
}

@Composable
internal fun SeccionPlanificacion(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    TituloDeSeccion("Planificación")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoTexto(
            valor = estadoVisual.duracionEstimada,
            etiqueta = "Duración estimada",
            alCambiar = { texto -> alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DURACION, texto)) },
            modifier = Modifier.weight(1f)
        )
        // Estimar con la distancia y el desnivel (regla de Naismith); el resultado se puede corregir a mano
        TextButton(
            onClick = { alProcesarEvento(FormularioSenderoEvento.AlCalcularDuracion) },
            modifier = Modifier.semantics { contentDescription = "Calcular la duración estimada" }
        ) {
            Text("Calcular")
        }
    }
    CampoTexto(
        valor = estadoVisual.puntoInicio,
        etiqueta = "Punto de inicio",
        alCambiar = { texto ->
            alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.PUNTO_INICIO, texto))
        }
    )
    CampoConSugerencias(
        valor = estadoVisual.mejorEpoca,
        etiqueta = "Mejor época",
        opciones = EPOCAS_SUGERIDAS,
        alCambiar = { texto ->
            alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.MEJOR_EPOCA, texto))
        },
        accionTeclado = ImeAction.Done
    )
}

@Composable
internal fun SeccionRecomendaciones(
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
    ChipsDeSugerencias(
        sugerencias = RECOMENDACIONES_SUGERIDAS,
        yaAgregadas = estadoVisual.recomendaciones,
        alAgregar = { texto -> alProcesarEvento(FormularioSenderoEvento.AlAgregarRecomendacionSugerida(texto)) }
    )
}

@Composable
internal fun SeccionServicios(
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
    ChipsDeSugerencias(
        sugerencias = SERVICIOS_SUGERIDOS,
        yaAgregadas = estadoVisual.servicios,
        alAgregar = { texto -> alProcesarEvento(FormularioSenderoEvento.AlAgregarServicioSugerido(texto)) }
    )
}

@Composable
internal fun SeccionEtapas(
    estadoVisual: FormularioSenderoEstadoVisual,
    alProcesarEvento: (FormularioSenderoEvento) -> Unit
) {
    TituloDeSeccion("Etapas")
    estadoVisual.etapas.forEachIndexed { indice, etapa ->
        FilaElementoQuitable(
            texto = "Etapa ${etapa.numero}: ${etapa.nombre} — ${formatearKilometros(etapa.distanciaKilometros)} km",
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
            etiqueta = "Distancia (km)",
            alCambiar = { texto ->
                alProcesarEvento(FormularioSenderoEvento.AlCambiarTexto(CampoFormulario.DISTANCIA_NUEVA_ETAPA, texto))
            },
            modifier = Modifier.width(144.dp),
            tipoTeclado = KeyboardType.Decimal,
            accionTeclado = ImeAction.Done,
            alTerminar = { alProcesarEvento(FormularioSenderoEvento.AlAgregarEtapa) }
        )
        IconButton(onClick = { alProcesarEvento(FormularioSenderoEvento.AlAgregarEtapa) }) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Agregar etapa")
        }
    }
}
