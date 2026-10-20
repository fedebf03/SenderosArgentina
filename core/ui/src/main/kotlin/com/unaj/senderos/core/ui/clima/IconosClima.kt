package com.unaj.senderos.core.ui.clima

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.unaj.senderos.core.domain.clima.CondicionClima

fun elegirIconoClima(condicion: CondicionClima, esDeDia: Boolean): ImageVector = when (condicion) {
    CondicionClima.DESPEJADO, CondicionClima.POCO_NUBLADO -> if (esDeDia) Icons.Filled.WbSunny else Icons.Filled.NightsStay
    CondicionClima.PARCIALMENTE_NUBLADO -> Icons.Filled.WbCloudy
    CondicionClima.NUBLADO, CondicionClima.VARIABLE -> Icons.Filled.Cloud
    CondicionClima.NIEBLA -> Icons.Filled.Dehaze
    CondicionClima.LLOVIZNA, CondicionClima.LLUVIA, CondicionClima.LLUVIA_FUERTE, CondicionClima.CHUBASCOS -> Icons.Filled.Grain
    CondicionClima.NIEVE -> Icons.Filled.AcUnit
    CondicionClima.TORMENTA, CondicionClima.TORMENTA_CON_GRANIZO -> Icons.Filled.Thunderstorm
}
