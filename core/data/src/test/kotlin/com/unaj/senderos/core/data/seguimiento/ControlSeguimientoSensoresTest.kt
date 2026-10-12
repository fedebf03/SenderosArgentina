package com.unaj.senderos.data.seguimiento

import com.unaj.senderos.domain.model.CalidadSenal
import com.unaj.senderos.domain.model.CapacidadesDispositivo
import com.unaj.senderos.domain.model.Coordenada
import com.unaj.senderos.domain.model.LecturaUbicacion
import com.unaj.senderos.domain.model.MuestraAcelerometro
import com.unaj.senderos.domain.model.PuntoRecorrido
import com.unaj.senderos.domain.model.Usuario
import com.unaj.senderos.domain.model.TipoRecorrido
import com.unaj.senderos.domain.model.MotivoFinSeguimiento
import com.unaj.senderos.domain.repository.FuenteAcelerometro
import com.unaj.senderos.domain.repository.ProveedorDeclinacion
import com.unaj.senderos.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.falsos.FalsoAutenticacionRepositorio
import com.unaj.senderos.falsos.FalsoFuenteAcelerometro
import com.unaj.senderos.falsos.FalsoFuenteBrujula
import com.unaj.senderos.falsos.FalsoProveedorUbicacion
import com.unaj.senderos.falsos.FalsoRecorridoRepositorio
import com.unaj.senderos.falsos.FalsoSenderoRepositorio
import com.unaj.senderos.falsos.crearSenderoDePrueba
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

private const val LATITUD_BASE = -39.95
private const val LONGITUD_BASE = -71.07

@OptIn(ExperimentalCoroutinesApi::class)
class ControlSeguimientoSensoresTest {

    private val ubicaciones = MutableSharedFlow<LecturaUbicacion>(extraBufferCapacity = 16)
    private val proveedor = FalsoProveedorUbicacion(ubicaciones = ubicaciones)
    private val acelerometro = FalsoFuenteAcelerometro()
    private val brujula = FalsoFuenteBrujula()
    private val recorrido = listOf(
        PuntoRecorrido(LATITUD_BASE, LONGITUD_BASE),
        PuntoRecorrido(LATITUD_BASE - 0.002, LONGITUD_BASE),
        PuntoRecorrido(LATITUD_BASE - 0.004, LONGITUD_BASE)
    )
    private val repositorio = FalsoSenderoRepositorio(
        listOf(crearSenderoDePrueba(id = "s1", recorrido = recorrido).copy(tipoRecorrido = TipoRecorrido.LINEAL))
    )
    private val autenticacion = FalsoAutenticacionRepositorio(Usuario(id = "usuario-1", email = "ana@correo.com", nombre = "Ana"))
    private val repositorioRecorridos = FalsoRecorridoRepositorio()
    private var instante = 1_000L

    private val todasLasCapacidades = CapacidadesDispositivo(tieneGps = true, tieneAcelerometro = true, tieneBrujula = true, tieneCamara = true)

    // Construir el control con las capacidades y fuentes indicadas, un alcance inmediato y el reloj de la prueba
    private fun TestScope.crearControl(
        capacidades: CapacidadesDispositivo = todasLasCapacidades,
        fuenteAcelerometro: FuenteAcelerometro? = acelerometro,
        proveedorDeclinacion: ProveedorDeclinacion? = null
    ): ControlSeguimientoEnMemoria {
        return ControlSeguimientoEnMemoria(
            proveedor,
            ObservarDetalleSenderoUseCase(repositorio, autenticacion),
            repositorioRecorridos,
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
            capacidades = capacidades,
            fuenteAcelerometro = fuenteAcelerometro,
            fuenteBrujula = brujula,
            proveedorDeclinacion = proveedorDeclinacion
        ) { instante }
    }

    // Construir una lectura de GPS al sur del origen
    private fun lectura(gradosAlSur: Double, velocidad: Float? = null, precision: Float? = null): LecturaUbicacion {
        return LecturaUbicacion(Coordenada(LATITUD_BASE - gradosAlSur, LONGITUD_BASE), null, velocidad, precisionMetros = precision)
    }

    // Emitir una caminata sintética de 2 pasos por segundo avanzando el reloj del control con las muestras
    private suspend fun caminar(duracionMilisegundos: Long) {
        val inicio = instante
        var transcurrido = 0L
        while (transcurrido <= duracionMilisegundos) {
            instante = inicio + transcurrido
            val z = 9.81 + 3.0 * sin(2 * PI * 2.0 * transcurrido / 1000.0)
            acelerometro.muestras.emit(MuestraAcelerometro(0f, 0f, z.toFloat(), instante))
            transcurrido += 20L
        }
    }

    // Verificar que sin acelerómetro los pasos y el ritmo son null
    @Test
    fun `sin acelerometro los pasos y el ritmo son null`() = runTest {
        val control = crearControl(capacidades = todasLasCapacidades.copy(tieneAcelerometro = false))

        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0, velocidad = 1.0f))

        assertNull(control.estado.value?.pasos)
        assertNull(control.estado.value?.ritmoPasosPorMinuto)
        assertEquals(0, acelerometro.muestras.subscriptionCount.value)
    }

    // Verificar que con acelerómetro una caminata suma pasos y ritmo y no queda detenido
    @Test
    fun `una caminata suma pasos y ritmo`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        assertEquals(0, control.estado.value?.pasos)

        caminar(5_000L)

        val pasos = control.estado.value?.pasos ?: -1
        assertTrue("Pasos: $pasos", pasos in 8..11)
        val ritmo = control.estado.value?.ritmoPasosPorMinuto ?: -1.0
        assertTrue("Ritmo: $ritmo", ritmo in 100.0..140.0)
        assertEquals(false, control.estado.value?.estaDetenido)
    }

    // Dejar al usuario detenido: pasan más de 10 s sin pasos y el GPS informa velocidad nula
    private suspend fun quedarDetenido() {
        instante = 20_000L
        ubicaciones.emit(lectura(0.0, velocidad = 0.0f))
    }

    // Construir una declinación fija de ocho grados al oeste
    private fun declinacionAlOeste(): ProveedorDeclinacion {
        return object : ProveedorDeclinacion {
            // Devolver la declinación fija de la prueba
            override fun obtenerDeclinacionGrados(coordenada: Coordenada, instanteMilisegundos: Long): Float = -8f
        }
    }

    // Verificar que estando detenido la brújula actualiza el rumbo de brújula del estado
    @Test
    fun `detenido la brujula actualiza el rumbo de brujula`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        quedarDetenido()

        brujula.rumbos.emit(200f)

        assertEquals(200f, control.estado.value?.rumboBrujulaGrados)
    }

    // Verificar que la brújula solo está registrada mientras el usuario está detenido
    @Test
    fun `la brujula solo se registra mientras esta detenido`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        assertEquals(0, brujula.rumbos.subscriptionCount.value)

        quedarDetenido()
        assertEquals(1, brujula.rumbos.subscriptionCount.value)

        instante = 21_000L
        ubicaciones.emit(lectura(0.001, velocidad = 1.4f))
        assertEquals(0, brujula.rumbos.subscriptionCount.value)
    }

    // Verificar que el rumbo de la brújula se corrige con la declinación de la última posición conocida
    @Test
    fun `el rumbo de brujula se corrige con la declinacion magnetica`() = runTest {
        val control = crearControl(proveedorDeclinacion = declinacionAlOeste())
        control.iniciar("s1")
        quedarDetenido()

        brujula.rumbos.emit(200f)

        assertEquals(192f, control.estado.value?.rumboBrujulaGrados)
        assertEquals(192f, control.estado.value?.rumboEfectivoGrados)
    }

    // Verificar que la declinación normaliza el resultado al intervalo de 0 a 360
    @Test
    fun `la declinacion normaliza el rumbo`() = runTest {
        val control = crearControl(proveedorDeclinacion = declinacionAlOeste())
        control.iniciar("s1")
        quedarDetenido()

        brujula.rumbos.emit(4f)

        assertEquals(356f, control.estado.value?.rumboBrujulaGrados)
    }

    // Verificar que los pasos usan el instante de la muestra y no el reloj de llegada (entrega por lotes)
    @Test
    fun `el ritmo usa el instante de las muestras aunque lleguen juntas`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        val inicio = instante
        var transcurrido = 0L
        while (transcurrido <= 5_000L) {
            val z = 9.81 + 3.0 * sin(2 * PI * 2.0 * transcurrido / 1000.0)
            acelerometro.muestras.emit(MuestraAcelerometro(0f, 0f, z.toFloat(), inicio + transcurrido))
            transcurrido += 20L
        }
        instante = inicio + 5_000L

        ubicaciones.emit(lectura(0.0, velocidad = 1.0f))

        val ritmo = control.estado.value?.ritmoPasosPorMinuto ?: -1.0
        assertTrue("Ritmo: $ritmo", ritmo in 100.0..140.0)
        assertEquals(false, control.estado.value?.estaDetenido)
    }

    // Verificar que si el acelerómetro falla durante el recorrido los pasos y el ritmo pasan a null
    @Test
    fun `si el acelerometro falla en pleno recorrido pasos y ritmo pasan a null`() = runTest {
        val origen = MutableSharedFlow<MuestraAcelerometro>(extraBufferCapacity = 64)
        val fuente = object : FuenteAcelerometro {
            // Reenviar las muestras de la prueba y fallar cuando llega una con x negativa
            override fun observarMuestras(): Flow<MuestraAcelerometro> = flow {
                origen.collect { muestra ->
                    check(muestra.x >= 0f) { "Sensor caído" }
                    emit(muestra)
                }
            }
        }
        val control = crearControl(fuenteAcelerometro = fuente)
        control.iniciar("s1")
        assertEquals(0, control.estado.value?.pasos)

        origen.emit(MuestraAcelerometro(-1f, 0f, 0f, instante))
        ubicaciones.emit(lectura(0.0, velocidad = 1.0f))

        assertNotNull(control.estado.value)
        assertNull(control.estado.value?.pasos)
        assertNull(control.estado.value?.ritmoPasosPorMinuto)
    }

    // Verificar que el recorrido guardado tras un fallo del acelerómetro no tiene pasos
    @Test
    fun `tras un fallo del acelerometro el recorrido guardado no tiene pasos`() = runTest {
        val fuente = object : FuenteAcelerometro {
            // Fallar apenas se observa
            override fun observarMuestras(): Flow<MuestraAcelerometro> = flow { throw IllegalStateException("Sensor caído") }
        }
        val control = crearControl(fuenteAcelerometro = fuente)
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0, velocidad = 1.0f))

        control.detener()

        assertNull(repositorioRecorridos.guardados.single().pasos)
        assertNull(repositorioRecorridos.guardados.single().ritmoPasosPorMinuto)
    }

    // Verificar que un recorrido simulado guarda los pasos pero no el ritmo, que estaría multiplicado
    @Test
    fun `un recorrido simulado guarda ritmo null`() = runTest {
        val control = crearControl()
        control.iniciar("s1", esSimulado = true)
        advanceTimeBy(10_500L)

        control.detener()

        val guardado = repositorioRecorridos.guardados.single()
        assertNull(guardado.ritmoPasosPorMinuto)
        assertTrue((guardado.pasos ?: 0) > 0)
    }

    // Verificar que cancelar deja el motivo CANCELADO_POR_EL_USUARIO y que iniciar de nuevo lo limpia
    @Test
    fun `cancelar fija el motivo cancelado por el usuario`() = runTest {
        val control = crearControl()
        control.iniciar("s1")

        control.cancelar()

        assertEquals(MotivoFinSeguimiento.CANCELADO_POR_EL_USUARIO, control.motivoFin.value)
        control.iniciar("s1")
        assertNull(control.motivoFin.value)
    }

    // Verificar que detener no deja motivo de fin
    @Test
    fun `detener no deja motivo de fin`() = runTest {
        val control = crearControl()
        control.iniciar("s1")

        control.detener()

        assertNull(control.motivoFin.value)
    }

    // Verificar que detenido y con brújula el rumbo efectivo sale de la brújula
    @Test
    fun `detenido el rumbo efectivo sale de la brujula`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        quedarDetenido()
        brujula.rumbos.emit(200f)

        assertEquals(true, control.estado.value?.estaDetenido)
        assertEquals(200f, control.estado.value?.rumboEfectivoGrados)
    }

    // Verificar que sin brújula en el equipo no se suscribe a la fuente
    @Test
    fun `sin brujula no se suscribe a la fuente`() = runTest {
        val control = crearControl(capacidades = todasLasCapacidades.copy(tieneBrujula = false))

        control.iniciar("s1")

        assertEquals(0, brujula.rumbos.subscriptionCount.value)
        assertNull(control.estado.value?.rumboBrujulaGrados)
    }

    // Verificar que un fallo de la fuente de pasos no termina el seguimiento
    @Test
    fun `un fallo de la fuente de pasos no termina el seguimiento`() = runTest {
        val fuenteQueFalla = object : FuenteAcelerometro {
            // Fallar al observar para simular un sensor roto
            override fun observarMuestras(): Flow<MuestraAcelerometro> = flow { throw IllegalStateException("Sensor roto") }
        }
        val control = crearControl(fuenteAcelerometro = fuenteQueFalla)

        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0, velocidad = 1.0f))

        assertNotNull(control.estado.value)
        assertNull(control.estado.value?.pasos)
    }

    // Verificar que un recorrido simulado genera pasos coherentes con la distancia, sin brújula ni sensores reales
    @Test
    fun `la simulacion genera un paso cada 75 centimetros`() = runTest {
        val control = crearControl()

        control.iniciar("s1", esSimulado = true)
        advanceTimeBy(10_500L)

        val estado = control.estado.value
        val distanciaMetros = (estado?.distanciaRecorridaKilometros ?: 0.0) * 1000.0
        assertTrue(distanciaMetros > 5.0)
        assertEquals((distanciaMetros / 0.75).toInt(), estado?.pasos)
        assertTrue((estado?.ritmoPasosPorMinuto ?: 0.0) > 0.0)
        assertNull(estado?.rumboBrujulaGrados)
        assertEquals(0, acelerometro.muestras.subscriptionCount.value)
        assertEquals(0, brujula.rumbos.subscriptionCount.value)
    }

    // Verificar que detener guarda el recorrido con los pasos y el ritmo medios reales
    @Test
    fun `detener guarda pasos y ritmo reales`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0, velocidad = 1.0f))
        caminar(5_000L)
        val pasos = control.estado.value?.pasos ?: -1
        instante = 61_000L

        control.detener()

        val guardado = repositorioRecorridos.guardados.single()
        assertEquals(pasos, guardado.pasos)
        assertEquals(pasos / (60_000L / 60_000.0), guardado.ritmoPasosPorMinuto ?: -1.0, 0.0001)
    }

    // Verificar que sin acelerómetro el recorrido guardado no tiene pasos ni ritmo
    @Test
    fun `detener sin acelerometro guarda pasos y ritmo null`() = runTest {
        val control = crearControl(capacidades = todasLasCapacidades.copy(tieneAcelerometro = false))
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0))

        control.detener()

        val guardado = repositorioRecorridos.guardados.single()
        assertNull(guardado.pasos)
        assertNull(guardado.ritmoPasosPorMinuto)
    }

    // Verificar que cancelar no guarda nada, deja el estado en null y libera los sensores
    @Test
    fun `cancelar no guarda y libera los sensores`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0, velocidad = 1.0f))
        ubicaciones.emit(lectura(0.001, velocidad = 1.0f))

        control.cancelar()

        assertTrue(repositorioRecorridos.guardados.isEmpty())
        assertNull(control.estado.value)
        assertEquals(MotivoFinSeguimiento.CANCELADO_POR_EL_USUARIO, control.motivoFin.value)
        assertEquals(0, ubicaciones.subscriptionCount.value)
        assertEquals(0, acelerometro.muestras.subscriptionCount.value)
        assertEquals(0, brujula.rumbos.subscriptionCount.value)
    }

    // Verificar que detener con las mismas posiciones sí guarda
    @Test
    fun `detener con posiciones guarda un recorrido`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0, velocidad = 1.0f))
        ubicaciones.emit(lectura(0.001, velocidad = 1.0f))

        control.detener()

        assertEquals(1, repositorioRecorridos.guardados.size)
        assertNull(control.estado.value)
    }

    // Verificar que cancelar y luego detener no guarda el recorrido descartado
    @Test
    fun `cancelar y luego detener no guarda`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0))

        control.cancelar()
        control.detener()

        assertTrue(repositorioRecorridos.guardados.isEmpty())
    }

    // Verificar que cancelar sin recorrido activo no falla
    @Test
    fun `cancelar sin recorrido activo no falla`() = runTest {
        val control = crearControl()

        control.cancelar()

        assertNull(control.estado.value)
        assertTrue(repositorioRecorridos.guardados.isEmpty())
    }

    // Verificar que cancelar un recorrido simulado tampoco guarda
    @Test
    fun `cancelar un recorrido simulado no guarda`() = runTest {
        val control = crearControl()
        control.iniciar("s1", esSimulado = true)
        advanceTimeBy(5_500L)

        control.cancelar()

        assertTrue(repositorioRecorridos.guardados.isEmpty())
        assertNull(control.estado.value)
    }

    // Verificar que el temporizador degrada la señal a débil sin que lleguen lecturas
    @Test
    fun `el temporizador pasa la senal de buena a debil sin lecturas`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0, precision = 10f))
        assertEquals(CalidadSenal.BUENA, control.estado.value?.calidadSenal)

        instante += 20_000L
        advanceTimeBy(5_500L)

        assertEquals(CalidadSenal.DEBIL, control.estado.value?.calidadSenal)
    }

    // Verificar que sin ninguna lectura válida el estado busca y luego pasa a débil por el temporizador
    @Test
    fun `sin lecturas validas busca y luego es debil por el temporizador`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        assertEquals(CalidadSenal.BUSCANDO, control.estado.value?.calidadSenal)

        instante += 31_000L
        advanceTimeBy(5_500L)

        assertEquals(CalidadSenal.DEBIL, control.estado.value?.calidadSenal)
    }

    // Verificar que una lectura válida devuelve la señal a buena tras haber estado débil
    @Test
    fun `una lectura valida devuelve la senal a buena`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0, precision = 10f))
        instante += 20_000L
        advanceTimeBy(5_500L)
        assertEquals(CalidadSenal.DEBIL, control.estado.value?.calidadSenal)

        ubicaciones.emit(lectura(0.0001, precision = 10f))

        assertEquals(CalidadSenal.BUENA, control.estado.value?.calidadSenal)
    }

    // Verificar que las lecturas imprecisas no mueven la posición y dejan la señal débil pasados 15 s
    @Test
    fun `las lecturas imprecisas dejan la senal debil sin mover la posicion`() = runTest {
        val control = crearControl()
        control.iniciar("s1")
        ubicaciones.emit(lectura(0.0, precision = 10f))
        val posicion = control.estado.value?.posicionActual

        instante += 20_000L
        ubicaciones.emit(lectura(0.001, precision = 80f))

        assertEquals(posicion, control.estado.value?.posicionActual)
        assertEquals(CalidadSenal.DEBIL, control.estado.value?.calidadSenal)
    }

    // Verificar que el recorrido simulado tiene buena señal desde el inicio y sigue así
    @Test
    fun `la simulacion siempre tiene buena senal`() = runTest {
        val control = crearControl()

        control.iniciar("s1", esSimulado = true)
        assertEquals(CalidadSenal.BUENA, control.estado.value?.calidadSenal)
        instante += 600_000L
        advanceTimeBy(20_000L)

        assertEquals(CalidadSenal.BUENA, control.estado.value?.calidadSenal)
    }

    // Verificar que cancelar detiene el temporizador de calidad
    @Test
    fun `cancelar detiene el temporizador de calidad`() = runTest {
        val control = crearControl()
        control.iniciar("s1")

        control.cancelar()
        advanceTimeBy(20_000L)

        assertNull(control.estado.value)
    }
}
