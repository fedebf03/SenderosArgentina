package com.unaj.senderos.core.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class SeccionPrincipal { EXPLORAR, MAPA, MIS_SENDEROS, PERFIL }

@Composable
fun BotoneraInferior(
    seccionActual: SeccionPrincipal,
    alSeleccionarSeccion: (SeccionPrincipal) -> Unit
) {
    val coloresItem = NavigationBarItemDefaults.colors(
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary
    )

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            ItemBotonera(SeccionPrincipal.EXPLORAR, "Explorar", Icons.Outlined.Explore, seccionActual, coloresItem, alSeleccionarSeccion)
            ItemBotonera(SeccionPrincipal.MAPA, "Mapa", Icons.Outlined.Map, seccionActual, coloresItem, alSeleccionarSeccion)
            ItemBotonera(SeccionPrincipal.MIS_SENDEROS, "Mis senderos", Icons.Outlined.Hiking, seccionActual, coloresItem, alSeleccionarSeccion)
            ItemBotonera(SeccionPrincipal.PERFIL, "Perfil", Icons.Outlined.Person, seccionActual, coloresItem, alSeleccionarSeccion)
        }
    }
}

@Composable
private fun RowScope.ItemBotonera(
    seccion: SeccionPrincipal,
    etiqueta: String,
    icono: ImageVector,
    seccionActual: SeccionPrincipal,
    colores: NavigationBarItemColors,
    alSeleccionarSeccion: (SeccionPrincipal) -> Unit
) {
    NavigationBarItem(
        selected = seccionActual == seccion,
        onClick = { if (seccion != seccionActual) alSeleccionarSeccion(seccion) },
        icon = { Icon(imageVector = icono, contentDescription = null) },
        label = { Text(etiqueta) },
        colors = colores
    )
}
