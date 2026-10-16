package com.unaj.senderos.core.testing

import com.unaj.senderos.core.domain.model.AvisoRecorrido
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.core.domain.model.ResultadoCaptura
import com.unaj.senderos.core.domain.model.TipoPunto
import com.unaj.senderos.core.domain.port.ControlSeguimiento
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class FalsoControlSeguimiento(private val registro: MutableList<String> = mutableListOf()) : ControlSeguimiento {

    val estadoEmitido = MutableStateFlow<EstadoSeguimiento?>(null)

    val avisosEmitidos = MutableSharedFlow<AvisoRecorrido>(extraBufferCapacity = 16)

    override val estado: StateFlow<EstadoSeguimiento?> = estadoEmitido
    val motivoFinEmitido = MutableStateFlow<MotivoFinSeguimiento?>(null)
    override val motivoFin: StateFlow<MotivoFinSeguimiento?> = motivoFinEmitido
    override val avisos: SharedFlow<AvisoRecorrido> = avisosEmitidos

    override suspend fun iniciar(idSendero: String, esSimulado: Boolean) {
        estadoEmitido.value = EstadoSeguimiento(idSendero = idSendero, trazado = emptyList(), esSimulado = esSimulado)
    }

    val multiplicadoresCambiados: MutableList<Int> = mutableListOf()
    var cantidadDesviosSimulados: Int = 0
        private set

    override fun cambiarMultiplicadorSimulacion(valor: Int) {
        multiplicadoresCambiados.add(valor)
    }

    override fun simularDesvio() {
        cantidadDesviosSimulados++
    }

    var cantidadCancelaciones: Int = 0
        private set

    override fun detener() {
        estadoEmitido.value = null
    }

    override fun cancelar() {
        cantidadCancelaciones++
        registro.add("control.cancelar")
        motivoFinEmitido.value = MotivoFinSeguimiento.CANCELADO_POR_EL_USUARIO
        estadoEmitido.value = null
    }

    var resultadoCapturarPunto: Result<ResultadoCaptura> = Result.failure(IllegalStateException("Sin configurar"))
    val capturasPedidas: MutableList<Triple<TipoPunto, String, String?>> = mutableListOf()

    override suspend fun capturarPunto(tipo: TipoPunto, nota: String, uriFoto: String?): Result<ResultadoCaptura> {
        capturasPedidas.add(Triple(tipo, nota, uriFoto))
        return resultadoCapturarPunto
    }
}
