package com.unaj.senderos.core.domain.usecase

import com.unaj.senderos.core.domain.model.Sendero
import com.unaj.senderos.core.domain.model.EstadoSincronizacion
import com.unaj.senderos.core.domain.recorrido.CalculadoraPerfil
import com.unaj.senderos.core.domain.port.EscritorArchivos
import com.unaj.senderos.core.domain.port.FormatoCopiaSeguridad
import com.unaj.senderos.core.domain.port.LectorArchivos
import com.unaj.senderos.core.domain.recorrido.EscritorTrazados
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.SenderoRepositorio
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val MENSAJE_SIN_SESION = "No hay una sesión activa"
private const val MENSAJE_SENDERO_NO_ENCONTRADO = "No se encontró el sendero"
private const val SUFIJO_COPIA = " (copia)"

// Formatos para exportar un sendero, con la extensión y el tipo de archivo que espera el selector del sistema
enum class FormatoExportacion(val extension: String, val tipoMime: String) {
    GPX("gpx", "application/gpx+xml"),
    CSV("csv", "text/csv"),
    JSON("json", "application/json")
}

// Qué hacer con los senderos de la copia que ya existen en la cuenta
enum class ModoRestauracion { REEMPLAZAR_EXISTENTES, AGREGAR_COMO_COPIAS, SOLO_NUEVOS }

// Senderos de una copia de seguridad, separados en los que no están en la cuenta y los que ya están (mismo id), con cuántos de
// estos últimos tienen cambios en el teléfono que todavía no se subieron (reemplazarlos los perdería)
data class AnalisisCopiaSeguridad(
    val nuevos: List<Sendero>,
    val existentes: List<Sendero>,
    val existentesConCambiosSinSubir: Int = 0
)

// Exporta un sendero del usuario en GPX o CSV (para otras aplicaciones) o en JSON (copia completa de un solo sendero)
class ExportarSenderoUseCase(
    private val senderoRepositorio: SenderoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio,
    private val escritorArchivos: EscritorArchivos,
    private val formatoCopiaSeguridad: FormatoCopiaSeguridad,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val despachadorCalculo: CoroutineDispatcher = Dispatchers.Default
) {
    // Construir el caso de uso para Hilt con el reloj y el despachador por defecto
    @Inject
    constructor(
        senderoRepositorio: SenderoRepositorio,
        autenticacionRepositorio: AutenticacionRepositorio,
        escritorArchivos: EscritorArchivos,
        formatoCopiaSeguridad: FormatoCopiaSeguridad
    ) : this(
        senderoRepositorio, autenticacionRepositorio, escritorArchivos, formatoCopiaSeguridad, System::currentTimeMillis, Dispatchers.Default
    )

    // Leer el sendero, armar el texto en el formato pedido (fuera del hilo principal) y escribirlo en el archivo elegido
    suspend operator fun invoke(idSendero: String, formato: FormatoExportacion, uri: String): Result<Unit> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual() ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        val sendero = leerPrimero { senderoRepositorio.observarSendero(usuario.id, idSendero).first() }
            .getOrElse { fallo -> return Result.failure(fallo) }
            ?: return Result.failure(NoSuchElementException(MENSAJE_SENDERO_NO_ENCONTRADO))
        val texto = withContext(despachadorCalculo) {
            when (formato) {
                FormatoExportacion.GPX -> EscritorTrazados.escribirGpx(sendero)
                FormatoExportacion.CSV -> EscritorTrazados.escribirCsv(sendero)
                FormatoExportacion.JSON -> formatoCopiaSeguridad.escribir(listOf(sendero), reloj())
            }
        }
        return escritorArchivos.escribirTexto(uri, texto)
    }
}

// Exporta todos los senderos del usuario en una sola copia de seguridad JSON y devuelve cuántos se guardaron
class ExportarCopiaSeguridadUseCase(
    private val senderoRepositorio: SenderoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio,
    private val escritorArchivos: EscritorArchivos,
    private val formatoCopiaSeguridad: FormatoCopiaSeguridad,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val despachadorCalculo: CoroutineDispatcher = Dispatchers.Default
) {
    // Construir el caso de uso para Hilt con el reloj y el despachador por defecto
    @Inject
    constructor(
        senderoRepositorio: SenderoRepositorio,
        autenticacionRepositorio: AutenticacionRepositorio,
        escritorArchivos: EscritorArchivos,
        formatoCopiaSeguridad: FormatoCopiaSeguridad
    ) : this(
        senderoRepositorio, autenticacionRepositorio, escritorArchivos, formatoCopiaSeguridad, System::currentTimeMillis, Dispatchers.Default
    )

    // Leer los senderos del usuario y escribir la copia (armada fuera del hilo principal) en el archivo elegido
    suspend operator fun invoke(uri: String): Result<Int> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual() ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        val senderos = leerPrimero { senderoRepositorio.observarSenderosDelUsuario(usuario.id).first() }
            .getOrElse { fallo -> return Result.failure(fallo) }
        val texto = withContext(despachadorCalculo) { formatoCopiaSeguridad.escribir(senderos, reloj()) }
        return escritorArchivos.escribirTexto(uri, texto).map { senderos.size }
    }
}

// Restaura una copia de seguridad: primero la analiza para preguntar qué hacer con los repetidos y después la aplica
class ImportarCopiaSeguridadUseCase(
    private val senderoRepositorio: SenderoRepositorio,
    private val autenticacionRepositorio: AutenticacionRepositorio,
    private val lectorArchivos: LectorArchivos,
    private val formatoCopiaSeguridad: FormatoCopiaSeguridad,
    private val guardarSenderoUseCase: GuardarSenderoUseCase,
    private val despachadorCalculo: CoroutineDispatcher = Dispatchers.Default
) {
    // Construir el caso de uso para Hilt con el despachador por defecto
    @Inject
    constructor(
        senderoRepositorio: SenderoRepositorio,
        autenticacionRepositorio: AutenticacionRepositorio,
        lectorArchivos: LectorArchivos,
        formatoCopiaSeguridad: FormatoCopiaSeguridad,
        guardarSenderoUseCase: GuardarSenderoUseCase
    ) : this(
        senderoRepositorio, autenticacionRepositorio, lectorArchivos, formatoCopiaSeguridad, guardarSenderoUseCase, Dispatchers.Default
    )

    // Leer la copia elegida y separar sus senderos en nuevos y ya existentes en la cuenta
    suspend fun analizar(uri: String): Result<AnalisisCopiaSeguridad> {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual() ?: return Result.failure(IllegalStateException(MENSAJE_SIN_SESION))
        val archivo = lectorArchivos.leerTexto(uri).getOrElse { fallo -> return Result.failure(fallo) }
        val senderosCopia = withContext(despachadorCalculo) { formatoCopiaSeguridad.leer(archivo.texto) }
            .getOrElse { fallo -> return Result.failure(fallo) }
        val locales = leerPrimero { senderoRepositorio.observarSenderosDelUsuario(usuario.id).first() }
            .getOrElse { fallo -> return Result.failure(fallo) }
            .associateBy { sendero -> sendero.id }
        // Un id repetido dentro de la misma copia se guardaría dos veces sobre el mismo sendero: queda el primero
        val unicos = senderosCopia.distinctBy { sendero -> sendero.id.ifBlank { sendero.hashCode().toString() } }
        val (existentes, nuevos) = unicos.partition { sendero -> sendero.id in locales }
        val conCambios = existentes.count { sendero -> locales[sendero.id]?.estadoSincronizacion != EstadoSincronizacion.SINCRONIZADO }
        return Result.success(AnalisisCopiaSeguridad(nuevos = nuevos, existentes = existentes, existentesConCambiosSinSubir = conCambios))
    }

    // Guardar los senderos de la copia según el modo elegido y devolver cuántos se guardaron; los que no pasan la validación se
    // omiten y se informan con alRechazar. Al reemplazar, se conservan del sendero local los datos que la copia no lleva (el perfil,
    // si sigue correspondiendo al trazado, y la marca de guardado desde Explorar). Como copias, los repetidos van con un id nuevo y "(copia)"
    suspend fun aplicar(
        analisis: AnalisisCopiaSeguridad,
        modo: ModoRestauracion,
        alRechazar: (Sendero, Throwable) -> Unit = { _, _ -> }
    ): Int {
        val usuario = autenticacionRepositorio.obtenerUsuarioActual() ?: return 0
        val locales = leerPrimero { senderoRepositorio.observarSenderosDelUsuario(usuario.id).first() }
            .getOrDefault(emptyList())
            .associateBy { sendero -> sendero.id }
        val aGuardar = analisis.nuevos + when (modo) {
            ModoRestauracion.REEMPLAZAR_EXISTENTES -> analisis.existentes.map { sendero ->
                locales[sendero.id]?.let { local -> conservarDatosLocales(sendero, local) } ?: sendero
            }
            ModoRestauracion.AGREGAR_COMO_COPIAS -> analisis.existentes.map { sendero -> sendero.copy(id = "", nombre = sendero.nombre + SUFIJO_COPIA) }
            ModoRestauracion.SOLO_NUEVOS -> emptyList()
        }
        return aGuardar.count { sendero ->
            guardarSenderoUseCase(sendero).onFailure { error -> alRechazar(sendero, error) }.isSuccess
        }
    }

    // Completar el sendero de la copia con lo que solo tiene el local: el perfil si corresponde al trazado de la copia y el origen
    private fun conservarDatosLocales(deLaCopia: Sendero, local: Sendero): Sendero {
        val firmaDeLaCopia = CalculadoraPerfil.firmaTrazado(deLaCopia.recorrido, deLaCopia.tipoRecorrido)
        return deLaCopia.copy(
            perfilElevacion = local.perfilElevacion?.takeIf { perfil -> perfil.firmaTrazado == firmaDeLaCopia },
            idOrigen = local.idOrigen
        )
    }
}

// Ejecutar una lectura del repositorio y convertir sus fallos en un resultado, sin atrapar la cancelación
private suspend fun <T> leerPrimero(lectura: suspend () -> T): Result<T> {
    return try {
        Result.success(lectura())
    } catch (excepcion: CancellationException) {
        throw excepcion
    } catch (error: Exception) {
        Result.failure(error)
    }
}
