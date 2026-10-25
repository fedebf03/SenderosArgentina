package com.unaj.senderos.features.senderos.detalle

import com.unaj.senderos.core.ui.sendero.obtenerIconoTipoPunto
import com.unaj.senderos.core.ui.sendero.CeldaDato
import com.unaj.senderos.core.ui.sendero.DatoRecorrido
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.PerfilElevacion
import com.unaj.senderos.core.domain.model.PuntoPerfil
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.TituloDeSeccion
import com.unaj.senderos.core.ui.componentes.perfilelevacion.GraficoPerfilElevacion
import com.unaj.senderos.core.ui.componentes.perfilelevacion.MarcaDelPerfil
import com.unaj.senderos.core.ui.componentes.perfilelevacion.TEXTO_PERFIL_DESACTUALIZADO
import com.unaj.senderos.core.ui.componentes.perfilelevacion.TEXTO_PERFIL_SIN_CALCULAR
import com.unaj.senderos.core.ui.componentes.perfilelevacion.TITULO_SECCION_PERFIL
import com.unaj.senderos.core.ui.comun.formatearAlturaMetros
import com.unaj.senderos.core.ui.comun.formatearPorcentaje
import com.unaj.senderos.core.ui.tema.TemaSenderos

private const val TEXTO_CALCULANDO_PERFIL = "Calculando el perfil…"
private const val COLUMNAS_DATOS_PERFIL = 3

@Composable
internal fun SeccionPerfilElevacion(
    estadoVisual: DetalleSenderoEstadoVisual,
    sendero: Sendero,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit,
    modifier: Modifier = Modifier
) {
    val perfil = sendero.perfilElevacion
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloDeSeccion(TITULO_SECCION_PERFIL)
        if (perfil == null) {
            InvitacionACalcularPerfil(
                mensaje = TEXTO_PERFIL_SIN_CALCULAR,
                textoBoton = "Calcular perfil",
                estaCalculando = estadoVisual.estaCalculandoPerfil,
                alCalcular = { alProcesarEvento(DetalleSenderoEvento.AlCalcularPerfil) }
            )
        } else {
            if (estadoVisual.perfilEstaDesactualizado) {
                InvitacionACalcularPerfil(
                    mensaje = TEXTO_PERFIL_DESACTUALIZADO,
                    textoBoton = "Recalcular perfil",
                    estaCalculando = estadoVisual.estaCalculandoPerfil,
                    alCalcular = { alProcesarEvento(DetalleSenderoEvento.AlCalcularPerfil) }
                )
            }
            OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                GraficoPerfilElevacion(
                    perfil = perfil,
                    bioma = sendero.bioma,
                    marcasDeInteres = estadoVisual.puntosDeInteres.map { punto ->
                        MarcaDelPerfil(punto.distanciaDesdeInicioKilometros, obtenerIconoTipoPunto(punto.tipo))
                    },
                    alSeleccionarKilometro = { kilometros ->
                        alProcesarEvento(DetalleSenderoEvento.AlSeleccionarKilometroDelPerfil(kilometros))
                    },
                    modifier = Modifier.padding(12.dp)
                )
            }
            DatosDelPerfil(perfil)
        }
    }
}

@Composable
private fun InvitacionACalcularPerfil(
    mensaje: String,
    textoBoton: String,
    estaCalculando: Boolean,
    alCalcular: () -> Unit
) {
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = mensaje, style = MaterialTheme.typography.bodyMedium)
            if (estaCalculando) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.semantics { stateDescription = TEXTO_CALCULANDO_PERFIL }
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(text = TEXTO_CALCULANDO_PERFIL, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                BotonSecundario(texto = textoBoton, alTocar = alCalcular, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DatosDelPerfil(perfil: PerfilElevacion) {
    val datos = listOf(
        DatoRecorrido(Icons.AutoMirrored.Filled.TrendingUp, formatearAlturaMetros(perfil.desnivelPositivoMetros), "Desnivel positivo"),
        DatoRecorrido(Icons.AutoMirrored.Filled.TrendingDown, formatearAlturaMetros(perfil.desnivelNegativoMetros), "Desnivel negativo"),
        DatoRecorrido(Icons.Outlined.Speed, formatearPorcentaje(perfil.pendienteMaximaPorcentaje), "Pendiente máxima"),
        DatoRecorrido(Icons.Filled.ArrowUpward, formatearAlturaMetros(perfil.alturaMaximaMetros), "Altura máxima"),
        DatoRecorrido(Icons.Filled.ArrowDownward, formatearAlturaMetros(perfil.alturaMinimaMetros), "Altura mínima")
    )
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        datos.chunked(COLUMNAS_DATOS_PERFIL).forEachIndexed { indiceFila, filaDeDatos ->
            if (indiceFila > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                filaDeDatos.forEachIndexed { indiceDato, dato ->
                    if (indiceDato > 0) {
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    CeldaDato(dato, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SeccionPerfilElevacionPreview() {
    val puntos = (0..40).map { indice ->
        PuntoPerfil(indice * 0.25, 1400.0 + 500.0 * kotlin.math.sin(indice / 8.0) + indice * 6.0)
    }
    val perfil = PerfilElevacion(
        puntos = puntos,
        desnivelPositivoMetros = 820,
        desnivelNegativoMetros = 410,
        alturaMaximaMetros = 2010,
        alturaMinimaMetros = 1400,
        pendienteMaximaPorcentaje = 14.3,
        firmaTrazado = "vista-previa"
    )
    TemaSenderos {
        SeccionPerfilElevacion(
            estadoVisual = DetalleSenderoEstadoVisual(estaCargando = false),
            sendero = Sendero(
                id = "vista-previa",
                idUsuario = "usuario-1",
                nombre = "Vista previa",
                region = "Mendoza",
                distanciaKilometros = 10.0,
                dificultad = com.unaj.senderos.core.domain.model.Dificultad.MEDIA,
                descripcion = "Vista previa",
                desnivelMetros = 820,
                duracionEstimada = "4 horas",
                alturaMaximaMetros = 2010,
                alturaMinimaMetros = 1400,
                tipoRecorrido = com.unaj.senderos.core.domain.model.TipoRecorrido.LINEAL,
                puntoInicio = "Base",
                mejorEpoca = "Verano",
                recomendaciones = emptyList(),
                servicios = emptyList(),
                etapas = emptyList(),
                bioma = Bioma.ALTA_MONTANIA,
                perfilElevacion = perfil
            ),
            alProcesarEvento = {},
            modifier = Modifier.padding(24.dp)
        )
    }
}
