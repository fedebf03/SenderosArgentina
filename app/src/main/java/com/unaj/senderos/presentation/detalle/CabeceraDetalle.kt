package com.unaj.senderos.presentation.detalle

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unaj.senderos.domain.model.Sendero
import com.unaj.senderos.presentation.componentes.FotoSendero

// Renderizar la cabecera del detalle con la foto, las acciones y el nombre del sendero
@Composable
internal fun CabeceraDetalle(
    estadoVisual: DetalleSenderoEstadoVisual,
    sendero: Sendero,
    alProcesarEvento: (DetalleSenderoEvento) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        FotoSendero(foto = sendero.foto, modifier = Modifier.fillMaxSize())

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
                    MenuOpciones(estadoVisual.esMenuOpcionesVisible, alProcesarEvento)
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = sendero.region,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// Renderizar el menú con las opciones de editar y eliminar el sendero
@Composable
internal fun MenuOpciones(
    esVisible: Boolean,
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

// Renderizar un botón de ícono circular que se apoya sobre la foto
@Composable
internal fun BotonCircular(
    icono: ImageVector,
    descripcion: String,
    alTocar: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    ) {
        IconButton(onClick = alTocar) {
            Icon(
                imageVector = icono,
                contentDescription = descripcion,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
