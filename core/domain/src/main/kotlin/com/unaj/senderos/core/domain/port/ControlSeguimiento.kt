package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.AvisoRecorrido
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.core.domain.model.ResultadoCaptura
import com.unaj.senderos.core.domain.model.TipoPunto
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// Todos los métodos se llaman desde el hilo principal (el estado vive en Dispatchers.Main.immediate), por eso no hace
// falta sincronización adicional
interface ControlSeguimiento {

    val estado: StateFlow<EstadoSeguimiento?>

    // Solo se informa cuando el fin no lo pidió el usuario (por ejemplo, un permiso revocado)
    val motivoFin: StateFlow<MotivoFinSeguimiento?>

    val avisos: SharedFlow<AvisoRecorrido>

    // Si había un recorrido en curso se finaliza antes de empezar el nuevo
    suspend fun iniciar(idSendero: String, esSimulado: Boolean = false)

    // No hay sendero que seguir. Se registra el trazado caminado y se pueden marcar puntos
    suspend fun iniciarCaminataLibre(idCaminata: String, idUsuario: String, nombre: String)
    // Acepta 1, 5 o 20 y no hace nada si el recorrido no es simulado
    fun cambiarMultiplicadorSimulacion(valor: Int)

    // Saca la posición simulada del trazado unos segundos. No hace nada si el recorrido no es simulado
    fun simularDesvio()

    fun detener()

    // Se descarta lo recorrido y no se guarda nada en el historial
    fun cancelar()

    // Se usa la posición reservada al tocar el botón y, si no viene, la última válida del recorrido. Con la señal
    // débil el resultado informa la antigüedad de esa posición
    suspend fun capturarPunto(tipo: TipoPunto, nota: String, uriFoto: String?, posicion: Coordenada? = null): Result<ResultadoCaptura>
}
