package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.recorrido.ConversorCaminata
import com.unaj.senderos.core.domain.recorrido.TrazadoImportado
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.PuntosCapturadosRepositorio
import com.unaj.senderos.core.domain.repository.RecorridoRepositorio
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val MENSAJE_SIN_SESION = "No hay una sesión activa"
private const val MENSAJE_CAMINATA_NO_ENCONTRADA = "No se encontró la caminata"
// Al finalizar, el recorrido se escribe en segundo plano: se espera un momento por si el formulario se abre antes de que termine
private const val PLAZO_ESPERA_RECORRIDO_MILISEGUNDOS = 5_000L
private val FORMATO_FECHA_NOMBRE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// Borrador de sendero armado desde una caminata libre, con los ids de los puntos capturados que ya van dentro del trazado
data class BorradorDeCaminata(val trazado: TrazadoImportado, val idsPuntosCapturados: Set<String>)

class ConvertirCaminataEnBorradorUseCase @Inject constructor(
    private val recorridoRepositorio: RecorridoRepositorio,
    private val puntosCapturadosRepositorio: PuntosCapturadosRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Buscar el recorrido guardado de la caminata y sus puntos capturados pendientes, y convertirlos en el borrador del sendero nuevo
    suspend operator fun invoke(idCaminata: String): Result<BorradorDeCaminata> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        val recorrido = withTimeoutOrNull(PLAZO_ESPERA_RECORRIDO_MILISEGUNDOS) {
            recorridoRepositorio.observarRecorridos(usuario.id)
                .mapNotNull { lista -> lista.firstOrNull { candidato -> candidato.idSendero == idCaminata } }
                .first()
        } ?: return Result.failure(IllegalStateException(MENSAJE_CAMINATA_NO_ENCONTRADA))
        val capturados = puntosCapturadosRepositorio.observarPendientes(usuario.id, idCaminata).first()
        val nombre = "Caminata del " + FORMATO_FECHA_NOMBRE.format(
            Instant.ofEpochMilli(recorrido.inicioMilisegundos).atZone(ZoneId.systemDefault())
        )
        return ConversorCaminata.aTrazado(recorrido.trazadoCaminado, capturados, nombre)
            .map { trazado -> BorradorDeCaminata(trazado, capturados.map { punto -> punto.id }.toSet()) }
    }
}

class RetirarPuntosIncorporadosUseCase @Inject constructor(
    private val puntosCapturadosRepositorio: PuntosCapturadosRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio
) {
    // Retirar de los pendientes los puntos que ya quedaron dentro del sendero nuevo, sin borrar sus fotos
    suspend operator fun invoke(ids: Set<String>): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual()
            ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        return puntosCapturadosRepositorio.retirarIncorporados(usuario.id, ids)
    }
}
