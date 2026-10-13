package com.unaj.senderos.presentation.componentes.mapa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun ControlesDeZoom(
    alAcercar: () -> Unit,
    alAlejar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonDeMapa(Icons.Filled.Add, "Acercar", alAcercar)
        BotonDeMapa(Icons.Filled.Remove, "Alejar", alAlejar)
    }
}

@Composable
internal fun BotonDeMapa(icono: ImageVector, descripcion: String, alTocar: () -> Unit) {
    SmallFloatingActionButton(
        onClick = alTocar,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(imageVector = icono, contentDescription = descripcion)
    }
}
