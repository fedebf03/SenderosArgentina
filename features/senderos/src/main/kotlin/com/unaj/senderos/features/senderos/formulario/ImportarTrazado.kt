package com.unaj.senderos.features.senderos.formulario

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.unaj.senderos.core.domain.model.TipoRecorrido
import com.unaj.senderos.core.ui.componentes.BotonSecundario
import com.unaj.senderos.core.ui.componentes.DialogoConfirmacion
import com.unaj.senderos.core.ui.componentes.IconoDeBoton

// Los GPX suelen llegar sin un tipo claro (o como octet-stream), así que se aceptan todos y se reconocen por el contenido
private val TIPOS_DE_ARCHIVO_ACEPTADOS = arrayOf("*/*")

// Mostrar el botón para importar un trazado de otra aplicación (GPX o CSV) con el selector de archivos del sistema,
// y la confirmación cuando el trazado importado reemplazaría el recorrido ya marcado
@Composable
internal fun ImportarTrazado(estadoVisual: FormularioSenderoEstadoVisual, alProcesarEvento: (FormularioSenderoEvento) -> Unit) {
    val selectorArchivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) alProcesarEvento(FormularioSenderoEvento.AlImportarTrazado(uri.toString()))
    }
    BotonSecundario(
        texto = if (estadoVisual.estaImportandoTrazado) "Importando…" else "Importar GPX o CSV",
        alTocar = { selectorArchivo.launch(TIPOS_DE_ARCHIVO_ACEPTADOS) },
        icono = { IconoDeBoton(Icons.Outlined.FileOpen) },
        estaHabilitado = !estadoVisual.estaImportandoTrazado,
        modifier = Modifier.fillMaxWidth()
    )
    estadoVisual.trazadoImportadoPendiente?.let { trazado ->
        DialogoConfirmacion(
            titulo = "Reemplazar el recorrido",
            texto = "El archivo trae un recorrido ${describirTipo(trazado.tipoRecorrido)} de ${trazado.recorrido.size} puntos. " +
                "Reemplaza al que marcaste en el mapa.",
            textoConfirmar = "Reemplazar",
            alConfirmar = { alProcesarEvento(FormularioSenderoEvento.AlConfirmarReemplazoTrazado) },
            textoDescartar = "Cancelar",
            alDescartar = { alProcesarEvento(FormularioSenderoEvento.AlCancelarReemplazoTrazado) },
            esDestructivo = true
        )
    }
}

// Nombrar el tipo de recorrido detectado para el diálogo
private fun describirTipo(tipo: TipoRecorrido): String = when (tipo) {
    TipoRecorrido.LINEAL -> "lineal"
    TipoRecorrido.CIRCULAR -> "circular"
    TipoRecorrido.IDA_Y_VUELTA -> "de ida y vuelta"
}
