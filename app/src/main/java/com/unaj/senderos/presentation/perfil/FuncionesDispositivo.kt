package com.unaj.senderos.presentation.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unaj.senderos.presentation.componentes.TituloDeSeccion
import com.unaj.senderos.presentation.tema.TemaSenderos

// Obtener el ícono que representa una función del equipo
private fun obtenerIconoFuncion(funcion: FuncionDispositivo): ImageVector {
    return when (funcion) {
        FuncionDispositivo.GPS -> Icons.Filled.GpsFixed
        FuncionDispositivo.ACELEROMETRO -> Icons.AutoMirrored.Filled.DirectionsWalk
        FuncionDispositivo.BRUJULA -> Icons.Filled.Explore
        FuncionDispositivo.CAMARA -> Icons.Filled.PhotoCamera
    }
}

// Mostrar las funciones del equipo con su disponibilidad y lo que se pierde si faltan
@Composable
fun SeccionFuncionesDispositivo(
    funciones: List<EstadoFuncion>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TituloDeSeccion("Funciones de tu dispositivo")
        funciones.forEach { estadoFuncion -> FilaFuncionDispositivo(estadoFuncion) }
    }
}

// Mostrar una función con su ícono, su estado y, si falta, la explicación
@Composable
private fun FilaFuncionDispositivo(estadoFuncion: EstadoFuncion) {
    val nombre = obtenerNombreFuncion(estadoFuncion.funcion)
    val estado = if (estadoFuncion.estaDisponible) "Disponible" else "No disponible"
    val colorIcono = if (estadoFuncion.estaDisponible) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = if (estadoFuncion.estaDisponible) {
                "$nombre: $estado"
            } else {
                "$nombre: $estado. ${explicarFuncionNoDisponible(estadoFuncion.funcion)}"
            }
        },
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = obtenerIconoFuncion(estadoFuncion.funcion),
            contentDescription = null,
            tint = colorIcono
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = nombre, style = MaterialTheme.typography.bodyMedium)
            Text(text = estado, style = MaterialTheme.typography.bodySmall, color = colorIcono)
            if (!estadoFuncion.estaDisponible) {
                Text(
                    text = explicarFuncionNoDisponible(estadoFuncion.funcion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Previsualizar las funciones de un equipo sin brújula ni acelerómetro
@Preview(showBackground = true)
@Composable
private fun SeccionFuncionesDispositivoPreview() {
    TemaSenderos {
        SeccionFuncionesDispositivo(
            funciones = listOf(
                EstadoFuncion(FuncionDispositivo.GPS, true),
                EstadoFuncion(FuncionDispositivo.ACELEROMETRO, false),
                EstadoFuncion(FuncionDispositivo.BRUJULA, false),
                EstadoFuncion(FuncionDispositivo.CAMARA, true)
            )
        )
    }
}
