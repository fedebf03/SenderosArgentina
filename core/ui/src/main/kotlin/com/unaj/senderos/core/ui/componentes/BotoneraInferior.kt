package com.unaj.senderos.presentation.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
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

enum class SeccionPrincipal { INICIO, MAPA, FAVORITOS, PERFIL }

// Renderizar la botonera inferior con las cuatro secciones principales
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
            ItemBotonera(SeccionPrincipal.INICIO, "Inicio", Icons.Outlined.Home, seccionActual, coloresItem, alSeleccionarSeccion)
            ItemBotonera(SeccionPrincipal.MAPA, "Mapa", Icons.Outlined.Map, seccionActual, coloresItem, alSeleccionarSeccion)
            ItemBotonera(SeccionPrincipal.FAVORITOS, "Favoritos", Icons.Outlined.FavoriteBorder, seccionActual, coloresItem, alSeleccionarSeccion)
            ItemBotonera(SeccionPrincipal.PERFIL, "Perfil", Icons.Outlined.Person, seccionActual, coloresItem, alSeleccionarSeccion)
        }
    }
}

// Renderizar un destino de la botonera inferior con su ícono y su etiqueta
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
