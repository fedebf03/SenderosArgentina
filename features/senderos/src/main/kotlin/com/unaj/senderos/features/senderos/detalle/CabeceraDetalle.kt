package com.unaj.senderos.features.senderos.detalle

import com.unaj.senderos.core.ui.componentes.BotonCircular
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unaj.senderos.core.domain.model.Bioma
import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.ui.componentes.EtiquetaBioma
import com.unaj.senderos.core.ui.tema.obtenerColoresDelBioma
import com.unaj.senderos.core.ui.componentes.FotoSendero

@Composable
internal fun CabeceraDetalle(
    estadoVisual: DetalleSenderoEstadoVisual,
    sendero: Sendero,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit
) {
    val tieneBioma = sendero.bioma != Bioma.SIN_DEFINIR
    val coloresBioma = obtenerColoresDelBioma(sendero.bioma)
    val colorFondoPanel = if (tieneBioma) coloresBioma.contenedor.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val colorTitulo = if (tieneBioma) coloresBioma.sobreContenedor else MaterialTheme.colorScheme.primary
    val colorTexto = if (tieneBioma) coloresBioma.sobreContenedor else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        FotoSendero(foto = sendero.foto, bioma = sendero.bioma, modifier = Modifier.fillMaxSize())
        if (tieneBioma) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(coloresBioma.principal)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonCircular(
                icono = Icons.AutoMirrored.Filled.ArrowBack,
                descripcion = "Volver",
                alTocar = { alProcesarEvento(DetalleSenderoEvento.AlVolver) }
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonCircular(
                    icono = if (sendero.esFavorito) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    descripcion = if (sendero.esFavorito) "Quitar de favoritos" else "Agregar a favoritos",
                    alTocar = { alProcesarEvento(DetalleSenderoEvento.AlAlternarFavorito) }
                )
                BotonCircular(
                    icono = Icons.Outlined.Share,
                    descripcion = "Compartir",
                    alTocar = { alProcesarEvento(DetalleSenderoEvento.AlCompartir) }
                )
                Box {
                    BotonCircular(
                        icono = Icons.Filled.MoreVert,
                        descripcion = "Más opciones",
                        alTocar = { alProcesarEvento(DetalleSenderoEvento.AlAbrirMenuOpciones) }
                    )
                    MenuOpciones(estadoVisual.esMenuOpcionesVisible, estadoVisual.estaPublicado, alProcesarEvento)
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            color = colorFondoPanel,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = sendero.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorTitulo,
                    modifier = Modifier.semantics { heading() }
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorTexto
                    )
                    Text(
                        text = sendero.region,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorTexto
                    )
                }
                if (tieneBioma) {
                    EtiquetaBioma(sendero.bioma, conFondo = false)
                }
            }
        }
    }
}

@Composable
internal fun MenuOpciones(
    esVisible: Boolean,
    estaPublicado: Boolean,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit
) {
    DropdownMenu(
        expanded = esVisible,
        onDismissRequest = { alProcesarEvento(DetalleSenderoEvento.AlCerrarMenuOpciones) },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        DropdownMenuItem(
            text = { Text("Editar") },
            leadingIcon = { Icon(imageVector = Icons.Outlined.Edit, contentDescription = null) },
            onClick = { alProcesarEvento(DetalleSenderoEvento.AlTocarEditar) }
        )
        DropdownMenuItem(
            text = { Text(if (estaPublicado) "Hacer privado" else "Publicar") },
            leadingIcon = {
                Icon(
                    imageVector = if (estaPublicado) Icons.Outlined.Lock else Icons.Outlined.Public,
                    contentDescription = null
                )
            },
            onClick = {
                alProcesarEvento(
                    if (estaPublicado) DetalleSenderoEvento.AlTocarHacerPrivado else DetalleSenderoEvento.AlTocarPublicar
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = { alProcesarEvento(DetalleSenderoEvento.AlTocarEliminar) }
        )
    }
}
