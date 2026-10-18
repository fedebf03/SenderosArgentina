package com.unaj.senderos.core.domain.port

import com.unaj.senderos.core.domain.model.Coordenada
import com.unaj.senderos.core.domain.model.AvisoRecorrido
import com.unaj.senderos.core.domain.model.EstadoSeguimiento
import com.unaj.senderos.core.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.core.domain.model.ResultadoCaptura
import com.unaj.senderos.core.domain.model.TipoPunto
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// Todos los métodos se invocan desde el hilo principal (el control confina su estado a Dispatchers.Main.immediate), por eso no usa sincronización adicional
interface ControlSeguimiento {

    val estado: StateFlow<EstadoSeguimiento?>

    // Motivo del último fin no pedido por el usuario (p. ej. permiso revocado); null si no hubo o si se inició otro recorrido
    val motivoFin: StateFlow<MotivoFinSeguimiento?>

    val avisos: SharedFlow<AvisoRecorrido>

    // Iniciar el seguimiento de un sendero, real o simulado; finaliza el recorrido anterior si había uno
    suspend fun iniciar(idSendero: String, esSimulado: Boolean = false)

    // Iniciar una caminata libre del usuario con el identificador que le dio quien la pidió (ver IdentificadorCaminataLibre), sin sendero que seguir:
    // registra el trazado caminado y deja marcar puntos; finaliza el recorrido anterior si había uno
    suspend fun iniciarCaminataLibre(idCaminata: String, idUsuario: String, nombre: String)
    // Cambiar la velocidad de avance de la simulación (1, 5 o 20); no hace nada si el recorrido no es simulado
    fun cambiarMultiplicadorSimulacion(valor: Int)

    // Desplazar la posición simulada fuera del trazado unos segundos; no hace nada si el recorrido no es simulado
    fun simularDesvio()

    fun detener()

    // Cancelar el seguimiento activo descartando lo recorrido: no se guarda nada en el historial
    fun cancelar()

    // Capturar un punto de interés en la posición reservada al tocar el botón o, si no viene, en la última posición válida del recorrido activo,
    // en simulación también. Copia la foto (Uri) a la carpeta privada si viene y guarda el punto al instante en la base local; falla con un mensaje
    // claro si no hay recorrido o posición; con la señal débil el resultado informa la antigüedad de la posición usada
    suspend fun capturarPunto(tipo: TipoPunto, nota: String, uriFoto: String?, posicion: Coordenada? = null): Result<ResultadoCaptura>
}
