package com.unaj.senderos.core.data.di

import com.unaj.senderos.core.data.conectividad.MonitorConectividadAndroid
import com.unaj.senderos.core.data.mapas.GestorMapasSinConexionMapLibre
import com.unaj.senderos.core.data.notificaciones.NotificadorAndroid
import com.unaj.senderos.core.data.preferencias.PreferenciasAplicacionLocal
import com.unaj.senderos.core.data.seguimiento.ControlSeguimientoEnMemoria
import com.unaj.senderos.core.data.seguimiento.LanzadorSeguimientoAndroid
import com.unaj.senderos.core.data.sensores.DetectorCapacidadesAndroid
import com.unaj.senderos.core.data.sensores.FuenteAcelerometroAndroid
import com.unaj.senderos.core.data.sensores.FuenteBrujulaAndroid
import com.unaj.senderos.core.data.sensores.ProveedorDeclinacionAndroid
import com.unaj.senderos.core.data.ubicacion.ProveedorUbicacionCombinada
import com.unaj.senderos.core.domain.model.CapacidadesDispositivo
import com.unaj.senderos.core.domain.port.AlmacenBorradorRecorrido
import com.unaj.senderos.core.domain.port.AlmacenFotos
import com.unaj.senderos.core.domain.port.ControlSeguimiento
import com.unaj.senderos.core.domain.port.DetectorCapacidades
import com.unaj.senderos.core.domain.port.FuenteAcelerometro
import com.unaj.senderos.core.domain.port.FuenteBrujula
import com.unaj.senderos.core.domain.port.GestorMapasSinConexion
import com.unaj.senderos.core.domain.port.LanzadorSeguimiento
import com.unaj.senderos.core.domain.port.MonitorConectividad
import com.unaj.senderos.core.domain.port.Notificador
import com.unaj.senderos.core.domain.port.PreferenciasAplicacion
import com.unaj.senderos.core.domain.port.ProveedorDeclinacion
import com.unaj.senderos.core.domain.port.ProveedorUbicacion
import com.unaj.senderos.core.domain.repository.PuntosCapturadosRepositorio
import com.unaj.senderos.core.domain.repository.RecorridoRepositorio
import com.unaj.senderos.core.domain.usecase.ObservarDetalleSenderoUseCase
import com.unaj.senderos.core.domain.usecase.ObtenerCapacidadesDispositivoUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import com.unaj.senderos.core.domain.port.Reloj
import com.unaj.senderos.core.domain.port.GeneradorIdentificador
import java.util.UUID

@Module
@InstallIn(SingletonComponent::class)
abstract class DispositivoModule {

    @Binds
    @Singleton
    abstract fun bindProveedorUbicacion(impl: ProveedorUbicacionCombinada): ProveedorUbicacion

    @Binds
    @Singleton
    abstract fun bindNotificador(impl: NotificadorAndroid): Notificador

    @Binds
    @Singleton
    abstract fun bindPreferenciasAplicacion(impl: PreferenciasAplicacionLocal): PreferenciasAplicacion

    @Binds
    @Singleton
    abstract fun bindLanzadorSeguimiento(impl: LanzadorSeguimientoAndroid): LanzadorSeguimiento

    @Binds
    @Singleton
    abstract fun bindMonitorConectividad(impl: MonitorConectividadAndroid): MonitorConectividad

    @Binds
    @Singleton
    abstract fun bindGestorMapasSinConexion(impl: GestorMapasSinConexionMapLibre): GestorMapasSinConexion

    @Binds
    @Singleton
    abstract fun bindDetectorCapacidades(impl: DetectorCapacidadesAndroid): DetectorCapacidades

    @Binds
    abstract fun bindFuenteAcelerometro(impl: FuenteAcelerometroAndroid): FuenteAcelerometro

    @Binds
    abstract fun bindFuenteBrujula(impl: FuenteBrujulaAndroid): FuenteBrujula

    @Binds
    abstract fun bindProveedorDeclinacion(impl: ProveedorDeclinacionAndroid): ProveedorDeclinacion

    companion object {

        @Provides
        @Singleton
        @AlcanceSeguimiento
        fun provideAlcanceSeguimiento(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        @Provides
        fun provideReloj(): Reloj = Reloj(System::currentTimeMillis)

        @Provides
        fun provideGeneradorIdentificador(): GeneradorIdentificador = GeneradorIdentificador { UUID.randomUUID().toString() }

        @Provides
        @Singleton
        fun provideCapacidadesDispositivo(obtenerCapacidades: ObtenerCapacidadesDispositivoUseCase): CapacidadesDispositivo =
            obtenerCapacidades()

        @Provides
        @Singleton
        fun provideControlSeguimiento(
            proveedorUbicacion: ProveedorUbicacion,
            observarDetalleSendero: ObservarDetalleSenderoUseCase,
            recorridoRepositorio: RecorridoRepositorio,
            puntosCapturadosRepositorio: PuntosCapturadosRepositorio,
            almacenFotos: AlmacenFotos,
            @AlcanceSeguimiento alcance: CoroutineScope,
            capacidades: CapacidadesDispositivo,
            fuenteAcelerometro: FuenteAcelerometro,
            fuenteBrujula: FuenteBrujula,
            proveedorDeclinacion: ProveedorDeclinacion,
            almacenBorrador: AlmacenBorradorRecorrido
        ): ControlSeguimiento = ControlSeguimientoEnMemoria(
            proveedorUbicacion,
            observarDetalleSendero,
            recorridoRepositorio,
            puntosCapturadosRepositorio,
            almacenFotos,
            alcance,
            capacidades,
            fuenteAcelerometro,
            fuenteBrujula,
            proveedorDeclinacion,
            almacenBorrador
        )
    }
}
