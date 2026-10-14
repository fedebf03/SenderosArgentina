package com.unaj.senderos.core.data.di

import android.content.Context
import androidx.work.WorkManager
import com.unaj.senderos.core.data.fotos.AlmacenFotosLocal
import com.unaj.senderos.core.data.repository.AutenticacionRepositorioImpl
import com.unaj.senderos.core.data.repository.PuntosCapturadosRepositorioOfflineFirst
import com.unaj.senderos.core.data.repository.RecorridoRepositorioOfflineFirst
import com.unaj.senderos.core.data.repository.SenderoPublicoRepositorioImpl
import com.unaj.senderos.core.data.repository.SenderoRepositorioOfflineFirst
import com.unaj.senderos.core.data.sincronizacion.PlanificadorSincronizacion
import com.unaj.senderos.core.data.sincronizacion.PlanificadorSincronizacionWorkManager
import com.unaj.senderos.core.data.sincronizacion.SincronizadorSenderos
import com.unaj.senderos.core.database.EjecutorTransacciones
import com.unaj.senderos.core.database.PuntoCapturadoDao
import com.unaj.senderos.core.database.RecorridoDao
import com.unaj.senderos.core.database.SenderoDao
import com.unaj.senderos.core.domain.port.AlmacenFotos
import com.unaj.senderos.core.domain.repository.AutenticacionRepositorio
import com.unaj.senderos.core.domain.repository.PuntosCapturadosRepositorio
import com.unaj.senderos.core.domain.repository.RecorridoRepositorio
import com.unaj.senderos.core.domain.repository.SenderoPublicoRepositorio
import com.unaj.senderos.core.domain.repository.SenderoRepositorio
import com.unaj.senderos.core.network.SenderoFuenteRemota
import com.unaj.senderos.core.network.SenderoPublicoFuenteRemota
import com.unaj.senderos.core.network.SubidorFotos
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import com.unaj.senderos.core.data.elevacion.FuenteElevacionOpenMeteo
import com.unaj.senderos.core.domain.port.FuenteElevacion
import com.unaj.senderos.core.domain.usecase.CalcularPerfilElevacionUseCase

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAutenticacionRepositorio(impl: AutenticacionRepositorioImpl): AutenticacionRepositorio

    @Binds
    @Singleton
    abstract fun bindAlmacenFotos(impl: AlmacenFotosLocal): AlmacenFotos

    @Binds
    abstract fun bindFuenteElevacion(impl: FuenteElevacionOpenMeteo): FuenteElevacion

    @Binds
    @Singleton
    abstract fun bindPlanificadorSincronizacion(impl: PlanificadorSincronizacionWorkManager): PlanificadorSincronizacion

    companion object {

        @Provides
        @Singleton
        @AlcanceEscrituras
        fun provideAlcanceEscrituras(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext contexto: Context): WorkManager = WorkManager.getInstance(contexto)

        @Provides
        @Singleton
        fun provideSenderoRepositorio(
            senderoDao: SenderoDao,
            planificadorSincronizacion: PlanificadorSincronizacion,
            ejecutorTransacciones: EjecutorTransacciones
        ): SenderoRepositorio = SenderoRepositorioOfflineFirst(senderoDao, planificadorSincronizacion, ejecutorTransacciones)

        @Provides
        @Singleton
        fun provideRecorridoRepositorio(
            recorridoDao: RecorridoDao,
            planificadorSincronizacion: PlanificadorSincronizacion,
            autenticacionRepositorio: AutenticacionRepositorio
        ): RecorridoRepositorio = RecorridoRepositorioOfflineFirst(recorridoDao, planificadorSincronizacion, autenticacionRepositorio)

        @Provides
        @Singleton
        fun providePuntosCapturadosRepositorio(
            puntoCapturadoDao: PuntoCapturadoDao,
            senderoDao: SenderoDao,
            ejecutorTransacciones: EjecutorTransacciones,
            planificadorSincronizacion: PlanificadorSincronizacion,
            almacenFotos: AlmacenFotos,
            @AlcanceEscrituras alcanceEscrituras: CoroutineScope
        ): PuntosCapturadosRepositorio = PuntosCapturadosRepositorioOfflineFirst(
            puntoCapturadoDao,
            senderoDao,
            ejecutorTransacciones,
            planificadorSincronizacion,
            almacenFotos,
            alcanceEscrituras
        )

        @Provides
        @Singleton
        fun provideSenderoPublicoRepositorio(fuenteRemota: SenderoPublicoFuenteRemota): SenderoPublicoRepositorio =
            SenderoPublicoRepositorioImpl(fuenteRemota)

        @Provides
        fun provideSincronizadorSenderos(
            senderoDao: SenderoDao,
            fuenteRemota: SenderoFuenteRemota,
            ejecutorTransacciones: EjecutorTransacciones,
            subidorFotos: SubidorFotos,
            calcularPerfilElevacion: CalcularPerfilElevacionUseCase
        ): SincronizadorSenderos = SincronizadorSenderos(
            senderoDao,
            fuenteRemota,
            ejecutorTransacciones,
            subidorFotos,
            calculadorPerfiles = calcularPerfilElevacion
        )
    }
}
