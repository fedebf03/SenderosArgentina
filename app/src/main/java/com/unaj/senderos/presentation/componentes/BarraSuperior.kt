package com.unaj.senderos.presentation.componentes

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.unaj.senderos.presentation.tema.TemaSenderos

// Renderizar la barra superior de las pantallas de detalle: flecha para volver, título y acciones opcionales
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
    descripcionVolver: String = "Volver",
    acciones: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
        },
        navigationIcon = {
            IconButton(onClick = alVolver) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = descripcionVolver)
            }
        },
        actions = acciones,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = modifier
    )
}

// Previsualizar la barra superior con una acción
@Preview(showBackground = true)
@Composable
private fun BarraSuperiorPreview() {
    TemaSenderos {
        BarraSuperior(
            titulo = "Nuevo sendero",
            alVolver = {},
            acciones = { BotonSecundario(texto = "Guardar", alTocar = {}) }
        )
    }
}
