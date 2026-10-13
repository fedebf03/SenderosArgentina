package com.unaj.senderos.falsos

import com.unaj.senderos.domain.model.AvisoRecorrido
import com.unaj.senderos.domain.model.EstadoSeguimiento
import com.unaj.senderos.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.domain.model.ResultadoCaptura
import com.unaj.senderos.domain.model.TipoPunto
import com.unaj.senderos.domain.port.ControlSeguimiento
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class FalsoControlSeguimiento(private val registro: MutableList<String> = mutableListOf()) : ControlSeguimiento {

    // Estado que la prueba modifica para simular lo que publica el servicio
    val estadoEmitido = MutableStateFlow<EstadoSeguimiento?>(null)

    // Avisos que la prueba emite para simular proximidad y desvío
    val avisosEmitidos = MutableSharedFlow<AvisoRecorrido>(extraBufferCapacity = 16)

    override val estado: StateFlow<EstadoSeguimiento?> = estadoEmitido
    val motivoFinEmitido = MutableStateFlow<MotivoFinSeguimiento?>(null)
    override val motivoFin: StateFlow<MotivoFinSeguimiento?> = motivoFinEmitido
    override val avisos: SharedFlow<AvisoRecorrido> = avisosEmitidos

    // Guardar el estado inicial del sendero indicado
    override suspend fun iniciar(idSendero: String, esSimulado: Boolean) {
        estadoEmitido.value = EstadoSeguimiento(idSendero = idSendero, trazado = emptyList(), esSimulado = esSimulado)
    }

    val multiplicadoresCambiados: MutableList<Int> = mutableListOf()
    var cantidadDesviosSimulados: Int = 0
        private set

    // Registrar el multiplicador pedido
    override fun cambiarMultiplicadorSimulacion(valor: Int) {
        multiplicadoresCambiados.add(valor)
    }

    // Contar los desvíos pedidos
    override fun simularDesvio() {
        cantidadDesviosSimulados++
    }

    var cantidadCancelaciones: Int = 0
        private set

    // Dejar el estado en null como lo hace el control real
    override fun detener() {
        estadoEmitido.value = null
    }

    // Contar la cancelación, fijar el motivo y dejar el estado en null como lo hace el control real
    override fun cancelar() {
        cantidadCancelaciones++
        registro.add("control.cancelar")
        motivoFinEmitido.value = MotivoFinSeguimiento.CANCELADO_POR_EL_USUARIO
        estadoEmitido.value = null
    }

    var resultadoCapturarPunto: Result<ResultadoCaptura> = Result.failure(IllegalStateException("Sin configurar"))
    val capturasPedidas: MutableList<Triple<TipoPunto, String, String?>> = mutableListOf()

    // Registrar el pedido de captura y devolver el resultado configurado
    override suspend fun capturarPunto(tipo: TipoPunto, nota: String, uriFoto: String?): Result<ResultadoCaptura> {
        capturasPedidas.add(Triple(tipo, nota, uriFoto))
        return resultadoCapturarPunto
    }
}
